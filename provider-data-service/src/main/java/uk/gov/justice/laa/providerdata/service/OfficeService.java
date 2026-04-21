package uk.gov.justice.laa.providerdata.service;

import java.net.URI;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.justice.laa.providerdata.entity.AdvocateProviderOfficeLinkEntity;
import uk.gov.justice.laa.providerdata.entity.ChamberProviderOfficeLinkEntity;
import uk.gov.justice.laa.providerdata.entity.LiaisonManagerEntity;
import uk.gov.justice.laa.providerdata.entity.LspProviderOfficeLinkEntity;
import uk.gov.justice.laa.providerdata.entity.OfficeEntity;
import uk.gov.justice.laa.providerdata.entity.OfficeLiaisonManagerLinkEntity;
import uk.gov.justice.laa.providerdata.entity.ProviderEntity;
import uk.gov.justice.laa.providerdata.entity.ProviderOfficeLinkEntity;
import uk.gov.justice.laa.providerdata.exception.ItemNotFoundException;
import uk.gov.justice.laa.providerdata.mapper.BankAccountMapper;
import uk.gov.justice.laa.providerdata.model.AdvocateOfficePatchV2;
import uk.gov.justice.laa.providerdata.model.BankAccountProviderOfficeCreateV2;
import uk.gov.justice.laa.providerdata.model.BankAccountProviderOfficeLinkV2;
import uk.gov.justice.laa.providerdata.model.ChambersOfficePatchV2;
import uk.gov.justice.laa.providerdata.model.DXPatchV2;
import uk.gov.justice.laa.providerdata.model.LSPOfficePatchV2;
import uk.gov.justice.laa.providerdata.model.OfficeAddressV2;
import uk.gov.justice.laa.providerdata.model.OfficePatchV2;
import uk.gov.justice.laa.providerdata.model.PaymentDetailsCreateOrLinkV2;
import uk.gov.justice.laa.providerdata.model.PaymentDetailsPatchOrLinkV2;
import uk.gov.justice.laa.providerdata.model.PaymentDetailsPaymentMethodV2;
import uk.gov.justice.laa.providerdata.repository.AdvocateProviderOfficeLinkRepository;
import uk.gov.justice.laa.providerdata.repository.LiaisonManagerRepository;
import uk.gov.justice.laa.providerdata.repository.LspProviderOfficeLinkRepository;
import uk.gov.justice.laa.providerdata.repository.OfficeLiaisonManagerLinkRepository;
import uk.gov.justice.laa.providerdata.repository.OfficeRepository;
import uk.gov.justice.laa.providerdata.repository.ProviderOfficeLinkRepository;
import uk.gov.justice.laa.providerdata.repository.ProviderParentLinkRepository;
import uk.gov.justice.laa.providerdata.repository.ProviderRepository;
import uk.gov.justice.laa.providerdata.util.ReferenceNumberUtils;
import uk.gov.justice.laa.providerdata.util.UuidUtils;

/** Service responsible for provider firm office operations. */
@Service
@Transactional
public class OfficeService {

  private final ProviderRepository providerRepository;
  private final OfficeRepository officeRepository;
  private final LspProviderOfficeLinkRepository lspProviderOfficeLinkRepository;
  private final AdvocateProviderOfficeLinkRepository advocateProviderOfficeLinkRepository;
  private final ProviderParentLinkRepository providerParentLinkRepository;
  private final ProviderOfficeLinkRepository providerOfficeLinkRepository;
  private final LiaisonManagerRepository liaisonManagerRepository;
  private final OfficeLiaisonManagerLinkRepository officeLiaisonManagerLinkRepository;
  private final BankDetailsService bankDetailsService;
  private final BankAccountMapper bankAccountMapper;

  /**
   * Inject dependencies.
   *
   * @param providerRepository to find firms.
   * @param officeRepository to save offices.
   * @param lspProviderOfficeLinkRepository to save and query LSP office links.
   * @param advocateProviderOfficeLinkRepository to save and query Advocate office links.
   * @param providerParentLinkRepository to resolve advocate membership for Chambers firms.
   * @param providerOfficeLinkRepository to look up offices generically across all firm types.
   * @param liaisonManagerRepository to save liaison manager entities.
   * @param officeLiaisonManagerLinkRepository to save and query office liaison manager links.
   * @param bankDetailsService to create and link bank accounts.
   * @param bankAccountMapper to map bank account request DTOs to entities.
   */
  public OfficeService(
      ProviderRepository providerRepository,
      OfficeRepository officeRepository,
      LspProviderOfficeLinkRepository lspProviderOfficeLinkRepository,
      AdvocateProviderOfficeLinkRepository advocateProviderOfficeLinkRepository,
      ProviderParentLinkRepository providerParentLinkRepository,
      ProviderOfficeLinkRepository providerOfficeLinkRepository,
      LiaisonManagerRepository liaisonManagerRepository,
      OfficeLiaisonManagerLinkRepository officeLiaisonManagerLinkRepository,
      BankDetailsService bankDetailsService,
      BankAccountMapper bankAccountMapper) {
    this.providerRepository = providerRepository;
    this.officeRepository = officeRepository;
    this.lspProviderOfficeLinkRepository = lspProviderOfficeLinkRepository;
    this.advocateProviderOfficeLinkRepository = advocateProviderOfficeLinkRepository;
    this.providerParentLinkRepository = providerParentLinkRepository;
    this.providerOfficeLinkRepository = providerOfficeLinkRepository;
    this.liaisonManagerRepository = liaisonManagerRepository;
    this.officeLiaisonManagerLinkRepository = officeLiaisonManagerLinkRepository;
    this.bankDetailsService = bankDetailsService;
    this.bankAccountMapper = bankAccountMapper;
  }

  /**
   * Creates a new office for an LSP provider firm, links it to the provider, and optionally creates
   * or links a liaison manager and bank account.
   *
   * <p>If {@code lmTemplate} and {@code lmLinkTemplate} are non-null, a new liaison manager is
   * created for the office. If {@code linkToHeadOfficeLiaisonManager} is {@code true}, the existing
   * active liaison manager from the provider's head office is linked to the new office instead. At
   * most one of these two options should be active per call.
   *
   * <p>If {@code payment} is non-null and its {@code paymentMethod} is {@code EFT}, a bank account
   * is created (or an existing one linked) and associated with the office.
   *
   * @param providerFirmGUIDorFirmNumber GUID or firm number identifying the parent provider
   * @param officeTemplate unpersisted office entity with address and contact fields populated
   * @param linkTemplate unpersisted link entity with payment and VAT fields populated
   * @param lmTemplate liaison manager entity to create, or {@code null}
   * @param lmLinkTemplate LM link template with activeDateFrom set, or {@code null}
   * @param linkToHeadOfficeLiaisonManager if {@code true}, link the head office's active LM to this
   *     office
   * @param payment payment details from the request, or {@code null}
   * @return identifiers for the created provider, office, and link
   * @throws ItemNotFoundException if no provider matches the given identifier, or if {@code
   *     linkToHeadOfficeLiaisonManager} is true but no head office or active LM is found
   */
  public OfficeCreationResult createLspOffice(
      String providerFirmGUIDorFirmNumber,
      OfficeEntity officeTemplate,
      LspProviderOfficeLinkEntity linkTemplate,
      @Nullable LiaisonManagerEntity lmTemplate,
      @Nullable OfficeLiaisonManagerLinkEntity lmLinkTemplate,
      boolean linkToHeadOfficeLiaisonManager,
      @Nullable PaymentDetailsCreateOrLinkV2 payment) {

    ProviderEntity provider = findProvider(providerFirmGUIDorFirmNumber);

    var savedOffice = officeRepository.save(officeTemplate);

    String accountNumber = ReferenceNumberUtils.generateAccountNumber(provider.getFirmType(), null);
    linkTemplate.setProvider(provider);
    linkTemplate.setOffice(savedOffice);
    linkTemplate.setAccountNumber(accountNumber);
    var savedLink = lspProviderOfficeLinkRepository.save(linkTemplate);

    if (lmTemplate != null && lmLinkTemplate != null) {
      var savedLm = liaisonManagerRepository.save(lmTemplate);

      // Enforce NOT NULL constraints introduced on OfficeLiaisonManagerLinkEntity.
      if (lmLinkTemplate.getActiveDateFrom() == null) {
        lmLinkTemplate.setActiveDateFrom(LocalDate.now());
      }
      if (lmLinkTemplate.getLinkedFlag() == null) {
        lmLinkTemplate.setLinkedFlag(Boolean.FALSE);
      }

      lmLinkTemplate.setLiaisonManager(savedLm);
      lmLinkTemplate.setOfficeLink(savedLink);
      officeLiaisonManagerLinkRepository.save(lmLinkTemplate);
    } else if (linkToHeadOfficeLiaisonManager) {
      linkHeadOfficeLiaisonManager(provider, savedLink);
    }

    persistBankDetails(payment, provider, savedLink);

    return new OfficeCreationResult(
        provider.getGuid(), provider.getFirmNumber(), savedLink.getGuid(), accountNumber);
  }

  /**
   * Convenience overload that creates an LSP office with no liaison manager.
   *
   * @param providerFirmGUIDorFirmNumber GUID or firm number identifying the parent provider
   * @param officeTemplate unpersisted office entity with address and contact fields populated
   * @param linkTemplate unpersisted link entity with payment and VAT fields populated
   * @return identifiers for the created provider, office, and link
   * @throws ItemNotFoundException if no provider matches the given identifier
   */
  public OfficeCreationResult createLspOffice(
      String providerFirmGUIDorFirmNumber,
      OfficeEntity officeTemplate,
      LspProviderOfficeLinkEntity linkTemplate) {
    return createLspOffice(
        providerFirmGUIDorFirmNumber, officeTemplate, linkTemplate, null, null, false, null);
  }

  /**
   * Returns a paginated page of LSP offices for the given provider.
   *
   * @param providerFirmGUIDorFirmNumber GUID or firm number identifying the parent provider.
   * @param pageable the page being requested.
   * @return page of {@link LspProviderOfficeLinkEntity} for the provider.
   * @throws ItemNotFoundException if no provider matches the given identifier.
   */
  @Transactional(readOnly = true)
  public Page<LspProviderOfficeLinkEntity> getLspOffices(
      String providerFirmGUIDorFirmNumber, Pageable pageable) {

    ProviderEntity provider = findProvider(providerFirmGUIDorFirmNumber);
    return lspProviderOfficeLinkRepository.findByProvider(provider, pageable);
  }

  /**
   * Returns a paginated page of offices for the given provider, across all firm types.
   *
   * @param providerFirmGUIDorFirmNumber GUID or firm number identifying the provider.
   * @param pageable the page being requested.
   * @return page of {@link ProviderOfficeLinkEntity} for the provider.
   * @throws ItemNotFoundException if no provider matches the given identifier.
   */
  @Transactional(readOnly = true)
  public Page<ProviderOfficeLinkEntity> getOffices(
      String providerFirmGUIDorFirmNumber, Pageable pageable) {

    ProviderEntity provider = findProvider(providerFirmGUIDorFirmNumber);
    return providerOfficeLinkRepository.findByProvider(provider, pageable);
  }

  /**
   * Returns a paginated page of offices across all providers, with optional filtering by office
   * GUID or account number.
   *
   * <p>When {@code officeGUIDs} and {@code officeCodes} are both empty, all offices are returned.
   * When either filter list is non-empty, only offices matching those GUIDs or codes are returned,
   * unless {@code allProviderOffices} is {@code true} — in which case all offices belonging to the
   * providers that own any matching office are returned instead.
   *
   * @param officeGUIDs list of office-link GUID strings to filter by; may be null or empty
   * @param officeCodes list of office account-number strings to filter by; may be null or empty
   * @param allProviderOffices when {@code true}, expand results to all offices of the matched
   *     providers
   * @param pageable the page being requested
   * @return a page of {@link ProviderOfficeLinkEntity}
   */
  @Transactional(readOnly = true)
  public Page<ProviderOfficeLinkEntity> getOfficesGlobal(
      @Nullable List<String> officeGUIDs,
      @Nullable List<String> officeCodes,
      @Nullable Boolean allProviderOffices,
      Pageable pageable) {

    List<UUID> guids =
        officeGUIDs != null ? officeGUIDs.stream().map(UUID::fromString).toList() : List.of();
    List<String> codes = officeCodes != null ? officeCodes : List.of();

    if (guids.isEmpty() && codes.isEmpty()) {
      return providerOfficeLinkRepository.findAll(pageable);
    }

    if (Boolean.TRUE.equals(allProviderOffices)) {
      Collection<ProviderOfficeLinkEntity> matching =
          providerOfficeLinkRepository.findByGuidInOrAccountNumberIn(guids, codes);
      if (matching.isEmpty()) {
        return Page.empty(pageable);
      }
      Set<ProviderEntity> providers =
          matching.stream().map(ProviderOfficeLinkEntity::getProvider).collect(Collectors.toSet());
      return providerOfficeLinkRepository.findByProviderIn(providers, pageable);
    }

    return providerOfficeLinkRepository.findByGuidInOrAccountNumberIn(guids, codes, pageable);
  }

  /**
   * Returns a single LSP office by the {@link LspProviderOfficeLinkEntity} GUID or account number.
   *
   * <p>The {@code officeGUIDorCode} parameter is first tried as a UUID (the {@code
   * ProviderOfficeLinkEntity.guid}); if that fails it is treated as an account number.
   *
   * @param providerFirmGUIDorFirmNumber GUID or firm number identifying the parent provider
   * @param officeGUIDorCode {@link LspProviderOfficeLinkEntity} GUID or account number
   * @return the matching {@link LspProviderOfficeLinkEntity}
   * @throws ItemNotFoundException if no matching office is found
   */
  @Transactional(readOnly = true)
  public LspProviderOfficeLinkEntity getLspOfficeLink(
      String providerFirmGUIDorFirmNumber, String officeGUIDorCode) {

    ProviderEntity provider = findProvider(providerFirmGUIDorFirmNumber);
    return findLspOfficeLink(provider, officeGUIDorCode)
        .orElseThrow(() -> new ItemNotFoundException("Office not found: " + officeGUIDorCode));
  }

  /**
   * Returns the office link for the given provider and office identifier, regardless of firm type.
   *
   * <p>The {@code officeGUIDorCode} parameter is first tried as a UUID (the {@code
   * ProviderOfficeLinkEntity.guid}); if that fails it is treated as an account number.
   *
   * @param provider the provider to look up the office for
   * @param officeGUIDorCode {@link ProviderOfficeLinkEntity} GUID or account number
   * @return the matching {@link ProviderOfficeLinkEntity}
   * @throws ItemNotFoundException if no matching office is found
   */
  @Transactional(readOnly = true)
  public ProviderOfficeLinkEntity getProviderOfficeLink(
      ProviderEntity provider, String officeGUIDorCode) {
    return findProviderOfficeLink(provider, officeGUIDorCode)
        .orElseThrow(() -> new ItemNotFoundException("Office not found: " + officeGUIDorCode));
  }

  private Optional<ProviderOfficeLinkEntity> findProviderOfficeLink(
      ProviderEntity provider, String officeGUIDorCode) {
    return UuidUtils.parseUuid(officeGUIDorCode)
        .flatMap(guid -> providerOfficeLinkRepository.findByProviderAndGuid(provider, guid))
        .or(
            () ->
                providerOfficeLinkRepository.findByProviderAndAccountNumber(
                    provider, officeGUIDorCode));
  }

  private Optional<LspProviderOfficeLinkEntity> findLspOfficeLink(
      ProviderEntity provider, String officeGUIDorCode) {
    return UuidUtils.parseUuid(officeGUIDorCode)
        .flatMap(guid -> lspProviderOfficeLinkRepository.findByProviderAndGuid(provider, guid))
        .or(
            () ->
                lspProviderOfficeLinkRepository.findByProviderAndAccountNumber(
                    provider, officeGUIDorCode));
  }

  /**
   * Patches the contact details, activation state, financial flags, and payment details of an
   * office.
   *
   * <p>Only fields present in the patch request (non-null) are applied. Address and DX details are
   * replaced as whole objects when provided. Website is stored on the {@link
   * ProviderOfficeLinkEntity}; all other fields are stored on the {@link OfficeEntity}.
   *
   * <p>Activation rules (LSP and Advocate offices only):
   *
   * <ul>
   *   <li>Setting {@code activeDateTo} deactivates the office and auto-resets {@code
   *       debtRecoveryFlag} to {@code false}.
   *   <li>Deactivating the LSP head office cascades the same {@code activeDateTo} to all active
   *       child offices.
   *   <li>Deactivating a Chambers office cascades to all active linked Advocate offices.
   *   <li>{@code debtRecoveryFlag} may only be set to {@code true} when the office is active (no
   *       {@code activeDateTo}).
   *   <li>{@code falseBalanceFlag} may only be set to {@code true} when the office is inactive
   *       ({@code activeDateTo} set).
   *   <li>Invalid flag combinations throw {@link IllegalArgumentException}, which is mapped to HTTP
   *       400 by {@link uk.gov.justice.laa.providerdata.exception.GlobalExceptionHandler}.
   * </ul>
   *
   * @param providerFirmGUIDorFirmNumber GUID or firm number identifying the parent provider
   * @param officeGUIDorCode office-link GUID or account number identifying the office
   * @param patch the patch request
   * @return identifiers for the provider and office
   * @throws ItemNotFoundException if no provider or office matches the given identifiers
   * @throws IllegalArgumentException if flag combinations conflict with the office activation state
   */
  public OfficeCreationResult patchOffice(
      String providerFirmGUIDorFirmNumber, String officeGUIDorCode, OfficePatchV2 patch) {

    ProviderEntity provider = findProvider(providerFirmGUIDorFirmNumber);
    ProviderOfficeLinkEntity link =
        findProviderOfficeLink(provider, officeGUIDorCode)
            .orElseThrow(() -> new ItemNotFoundException("Office not found: " + officeGUIDorCode));

    switch (patch) {
      case LSPOfficePatchV2 lsp -> {
        applyContactPatch(
            link.getOffice(),
            link,
            lsp.getAddress(),
            lsp.getTelephoneNumber(),
            lsp.getEmailAddress(),
            lsp.getWebsite(),
            lsp.getDxDetails());
        applyActivationPatchToLink(
            lsp.getActiveDateTo(),
            Boolean.TRUE.equals(lsp.getClearActiveDateTo()),
            lsp.getDebtRecoveryFlag(),
            lsp.getFalseBalanceFlag(),
            provider,
            link);
        applyPaymentPatchToLink(lsp.getPayment(), provider, link);
      }
      case ChambersOfficePatchV2 chambers -> {
        applyContactPatch(
            link.getOffice(),
            link,
            chambers.getAddress(),
            chambers.getTelephoneNumber(),
            chambers.getEmailAddress(),
            chambers.getWebsite(),
            chambers.getDxDetails());
        applyActivationPatchToLink(
            chambers.getActiveDateTo(),
            Boolean.TRUE.equals(chambers.getClearActiveDateTo()),
            null,
            null,
            provider,
            link);
      }
      case AdvocateOfficePatchV2 advocate -> {
        applyActivationPatchToLink(
            advocate.getActiveDateTo(),
            Boolean.TRUE.equals(advocate.getClearActiveDateTo()),
            advocate.getDebtRecoveryFlag(),
            advocate.getFalseBalanceFlag(),
            provider,
            link);
        applyPaymentPatchToLink(advocate.getPayment(), provider, link);
      }
      default ->
          throw new IllegalStateException(
              "Unhandled OfficePatchV2 subtype: " + patch.getClass().getSimpleName());
    }

    officeRepository.save(link.getOffice());
    providerOfficeLinkRepository.save(link);

    return new OfficeCreationResult(
        provider.getGuid(), provider.getFirmNumber(), link.getGuid(), link.getAccountNumber());
  }

  /**
   * Applies activation-related fields ({@code activeDateTo}, {@code debtRecoveryFlag}, {@code
   * falseBalanceFlag}) to the link entity, dispatching on the entity type. This handles the case
   * where the Jackson discriminator cannot distinguish LSP from Advocate patches and deserialises
   * an activation-only request as {@link ChambersOfficePatchV2} or {@link AdvocateOfficePatchV2}.
   *
   * <p>For LSP and Advocate links: validates and applies flag constraints; auto-resets {@code
   * debtRecoveryFlag} to {@code false} when the office is deactivated. For LSP head offices,
   * cascades the deactivation date to all active child offices. For Chambers links, cascades to all
   * active linked Advocate offices. No-op if all three arguments are {@code null}.
   *
   * @throws IllegalArgumentException if flag combinations conflict with the effective activation
   *     state
   */
  private void applyActivationPatchToLink(
      @Nullable LocalDate patchActiveDateTo,
      boolean clearActiveDateTo,
      @Nullable Boolean patchDebtRecoveryFlag,
      @Nullable Boolean patchFalseBalanceFlag,
      ProviderEntity provider,
      ProviderOfficeLinkEntity link) {
    if (patchActiveDateTo != null && clearActiveDateTo) {
      throw new IllegalArgumentException(
          "activeDateTo and clearActiveDateTo must not both be set in the same request");
    }
    switch (link) {
      case LspProviderOfficeLinkEntity lspLink ->
          applyActivationPatchToLspLink(
              patchActiveDateTo,
              clearActiveDateTo,
              patchDebtRecoveryFlag,
              patchFalseBalanceFlag,
              provider,
              lspLink);
      case ChamberProviderOfficeLinkEntity chamberLink -> {
        if (patchActiveDateTo != null) {
          chamberLink.setActiveDateTo(patchActiveDateTo);
          cascadeDeactivationToAdvocates(chamberLink.getProvider(), patchActiveDateTo);
        } else if (clearActiveDateTo) {
          chamberLink.setActiveDateTo(null);
        }
      }
      case AdvocateProviderOfficeLinkEntity advocateLink ->
          applyActivationPatchToAdvocateLink(
              patchActiveDateTo,
              clearActiveDateTo,
              patchDebtRecoveryFlag,
              patchFalseBalanceFlag,
              advocateLink);
      default ->
          throw new IllegalStateException(
              "Unhandled ProviderOfficeLinkEntity subtype: " + link.getClass().getSimpleName());
    }
  }

  /**
   * Validates and applies activation patch fields to an LSP office link.
   *
   * <p>Validates the effective post-patch state before mutating the entity. Auto-resets {@code
   * debtRecoveryFlag} to {@code false} when {@code activeDateTo} is set (deactivation). Cascades
   * the deactivation date to all active child offices when the patched link is the head office.
   *
   * @throws IllegalArgumentException if flag combinations conflict with the effective state
   */
  private void applyActivationPatchToLspLink(
      @Nullable LocalDate patchActiveDateTo,
      boolean clearActiveDateTo,
      @Nullable Boolean patchDebtRecoveryFlag,
      @Nullable Boolean patchFalseBalanceFlag,
      ProviderEntity provider,
      LspProviderOfficeLinkEntity link) {
    LocalDate effectiveActiveDateTo =
        patchActiveDateTo != null
            ? patchActiveDateTo
            : clearActiveDateTo ? null : link.getActiveDateTo();
    boolean willBeInactive = effectiveActiveDateTo != null;

    validateFlagCombinations(patchDebtRecoveryFlag, patchFalseBalanceFlag, willBeInactive);

    if (patchActiveDateTo != null) {
      link.setActiveDateTo(patchActiveDateTo);
      link.setDebtRecoveryFlag(Boolean.FALSE);
      if (Boolean.TRUE.equals(link.getHeadOfficeFlag())) {
        cascadeDeactivationToLspChildren(provider, patchActiveDateTo);
      }
    } else if (clearActiveDateTo) {
      applyReactivationToLspLink(link);
    }
    if (patchDebtRecoveryFlag != null) {
      link.setDebtRecoveryFlag(patchDebtRecoveryFlag);
    }
    if (patchFalseBalanceFlag != null) {
      link.setFalseBalanceFlag(patchFalseBalanceFlag);
    }
  }

  /**
   * Validates and applies activation patch fields to an Advocate office link.
   *
   * <p>Auto-resets {@code debtRecoveryFlag} to {@code false} when {@code activeDateTo} is set.
   *
   * @throws IllegalArgumentException if flag combinations conflict with the effective state
   */
  private static void applyActivationPatchToAdvocateLink(
      @Nullable LocalDate patchActiveDateTo,
      boolean clearActiveDateTo,
      @Nullable Boolean patchDebtRecoveryFlag,
      @Nullable Boolean patchFalseBalanceFlag,
      AdvocateProviderOfficeLinkEntity link) {
    LocalDate effectiveActiveDateTo =
        patchActiveDateTo != null
            ? patchActiveDateTo
            : clearActiveDateTo ? null : link.getActiveDateTo();
    boolean willBeInactive = effectiveActiveDateTo != null;

    validateFlagCombinations(patchDebtRecoveryFlag, patchFalseBalanceFlag, willBeInactive);

    if (patchActiveDateTo != null) {
      link.setActiveDateTo(patchActiveDateTo);
      link.setDebtRecoveryFlag(Boolean.FALSE);
    } else if (clearActiveDateTo) {
      applyReactivationToAdvocateLink(link);
    }
    if (patchDebtRecoveryFlag != null) {
      link.setDebtRecoveryFlag(patchDebtRecoveryFlag);
    }
    if (patchFalseBalanceFlag != null) {
      link.setFalseBalanceFlag(patchFalseBalanceFlag);
    }
  }

  private static void applyReactivationToLspLink(LspProviderOfficeLinkEntity link) {
    link.setActiveDateTo(null);
    link.setFalseBalanceFlag(Boolean.FALSE);
  }

  private static void applyReactivationToAdvocateLink(AdvocateProviderOfficeLinkEntity link) {
    link.setActiveDateTo(null);
    link.setFalseBalanceFlag(Boolean.FALSE);
  }

  /**
   * Validates that the requested flag values are consistent with the effective activation state.
   *
   * @param willBeInactive {@code true} if the office will be inactive after this patch
   * @throws IllegalArgumentException if a flag is set to {@code true} in a disallowed state
   */
  private static void validateFlagCombinations(
      @Nullable Boolean debtRecoveryFlag,
      @Nullable Boolean falseBalanceFlag,
      boolean willBeInactive) {
    if (Boolean.TRUE.equals(debtRecoveryFlag) && willBeInactive) {
      throw new IllegalArgumentException(
          "debtRecoveryFlag cannot be set to true for an inactive office");
    }
    if (Boolean.TRUE.equals(falseBalanceFlag) && !willBeInactive) {
      throw new IllegalArgumentException(
          "falseBalanceFlag cannot be set to true for an active office");
    }
  }

  /**
   * Sets {@code activeDateTo} on all active (non-head) LSP office links for the provider and
   * auto-resets their {@code debtRecoveryFlag} to {@code false}.
   */
  private void cascadeDeactivationToLspChildren(ProviderEntity provider, LocalDate activeDateTo) {
    lspProviderOfficeLinkRepository
        .findByProviderAndHeadOfficeFlagFalseAndActiveDateToIsNull(provider)
        .forEach(
            child -> {
              child.setActiveDateTo(activeDateTo);
              child.setDebtRecoveryFlag(Boolean.FALSE);
              lspProviderOfficeLinkRepository.save(child);
            });
  }

  /**
   * Sets {@code activeDateTo} on all active Advocate office links for practitioners belonging to
   * the given Chambers firm (resolved via {@code PROVIDER_PARENT_LINK}), and auto-resets their
   * {@code debtRecoveryFlag} to {@code false}.
   */
  private void cascadeDeactivationToAdvocates(ProviderEntity chambersFirm, LocalDate activeDateTo) {
    providerParentLinkRepository.findByParent(chambersFirm).stream()
        .flatMap(
            ppl ->
                advocateProviderOfficeLinkRepository
                    .findByProviderAndActiveDateToIsNull(ppl.getProvider())
                    .stream())
        .forEach(
            advocateLink -> {
              advocateLink.setActiveDateTo(activeDateTo);
              advocateLink.setDebtRecoveryFlag(Boolean.FALSE);
              advocateProviderOfficeLinkRepository.save(advocateLink);
            });
  }

  private static void applyContactPatch(
      OfficeEntity office,
      ProviderOfficeLinkEntity link,
      @Nullable OfficeAddressV2 address,
      @Nullable String telephoneNumber,
      @Nullable String emailAddress,
      @Nullable URI website,
      @Nullable DXPatchV2 dxDetails) {
    if (address != null) {
      office.setAddressLine1(address.getLine1());
      office.setAddressLine2(address.getLine2());
      office.setAddressLine3(address.getLine3());
      office.setAddressLine4(address.getLine4());
      office.setAddressTownOrCity(address.getTownOrCity());
      office.setAddressCounty(address.getCounty());
      office.setAddressPostCode(address.getPostcode());
    }
    if (telephoneNumber != null) {
      office.setTelephoneNumber(telephoneNumber);
    }
    if (emailAddress != null) {
      office.setEmailAddress(emailAddress);
    }
    if (website != null) {
      link.setWebsite(website.toString());
    }
    if (dxDetails != null) {
      office.setDxDetailsNumber(dxDetails.getDxNumber());
      office.setDxDetailsCentre(dxDetails.getDxCentre());
    }
  }

  /**
   * Applies payment fields and persists bank account changes for a patch request, dispatching on
   * the actual link entity type rather than the patch DTO subtype. This handles cases where the
   * {@link JacksonConfig} discriminator cannot distinguish LSP from Advocate patches when only
   * {@code payment} fields are present.
   *
   * <p>No-op if {@code payment} is null or the link type does not support payment fields.
   */
  private void applyPaymentPatchToLink(
      @Nullable PaymentDetailsPatchOrLinkV2 payment,
      ProviderEntity provider,
      ProviderOfficeLinkEntity link) {
    if (payment == null) {
      return;
    }
    switch (link) {
      case LspProviderOfficeLinkEntity lspLink -> {
        lspLink.setPaymentMethod(payment.getPaymentMethod().getValue());
        lspLink.setPaymentHeldFlag(payment.getPaymentHeldFlag());
        lspLink.setPaymentHeldReason(payment.getPaymentHeldReason());
        persistBankDetailsForPatch(payment, provider, link);
      }
      case AdvocateProviderOfficeLinkEntity advocateLink -> {
        advocateLink.setPaymentMethod(payment.getPaymentMethod().getValue());
        advocateLink.setPaymentHeldFlag(payment.getPaymentHeldFlag());
        advocateLink.setPaymentHeldReason(payment.getPaymentHeldReason());
        persistBankDetailsForPatch(payment, provider, link);
      }
      default -> { // Chambers offices have no payment details
      }
    }
  }

  /**
   * Creates or links a bank account from a patch payment request.
   *
   * <p>Only acts when {@code paymentMethod=EFT} and {@code bankAccountDetails} is non-null. The
   * existing primary {@link OfficeBankAccountLinkEntity} is end-dated by {@link
   * BankDetailsService#saveOfficeBankAccountLink} before the new one is saved.
   *
   * @throws ItemNotFoundException if the request links by GUID and no matching account exists
   */
  private void persistBankDetailsForPatch(
      PaymentDetailsPatchOrLinkV2 payment,
      ProviderEntity provider,
      ProviderOfficeLinkEntity officeLink) {
    if (!PaymentDetailsPaymentMethodV2.EFT.equals(payment.getPaymentMethod())
        || payment.getBankAccountDetails() == null) {
      return;
    }
    switch (payment.getBankAccountDetails()) {
      case BankAccountProviderOfficeCreateV2 create -> {
        var template = bankAccountMapper.toBankAccountEntity(create);
        bankDetailsService.createAndLink(
            template, provider, officeLink, create.getActiveDateFrom());
      }
      case BankAccountProviderOfficeLinkV2 link ->
          bankDetailsService.linkExisting(
              link.getBankAccountGUID(), provider, officeLink, link.getActiveDateFrom());
      default -> { // unknown bankAccountDetails subtype — no-op
      }
    }
  }

  private ProviderEntity findProvider(String providerFirmGUIDorFirmNumber) {
    var guid = UuidUtils.parseUuid(providerFirmGUIDorFirmNumber);
    return (guid.isPresent()
            ? providerRepository.findById(guid.get())
            : providerRepository.findByFirmNumber(providerFirmGUIDorFirmNumber))
        .orElseThrow(
            () -> new ItemNotFoundException("Provider not found: " + providerFirmGUIDorFirmNumber));
  }

  private void linkHeadOfficeLiaisonManager(
      ProviderEntity provider, LspProviderOfficeLinkEntity newOfficeLink) {
    var headOfficeLink =
        lspProviderOfficeLinkRepository
            .findByProviderAndHeadOfficeFlagTrue(provider)
            .orElseThrow(
                () ->
                    new ItemNotFoundException(
                        "Head office not found for provider: " + provider.getGuid()));

    var activeLmLinks =
        officeLiaisonManagerLinkRepository.findByOfficeLinkAndActiveDateToIsNull(headOfficeLink);

    if (activeLmLinks.isEmpty()) {
      throw new ItemNotFoundException(
          "No active liaison manager found on head office for provider: " + provider.getGuid());
    }

    var headOfficeLm = activeLmLinks.getFirst().getLiaisonManager();
    var link = new OfficeLiaisonManagerLinkEntity();
    link.setLiaisonManager(headOfficeLm);
    link.setOfficeLink(newOfficeLink);
    link.setActiveDateFrom(LocalDate.now());
    link.setLinkedFlag(Boolean.TRUE);
    officeLiaisonManagerLinkRepository.save(link);
  }

  private void persistBankDetails(
      @Nullable PaymentDetailsCreateOrLinkV2 payment,
      ProviderEntity provider,
      LspProviderOfficeLinkEntity officeLink) {
    if (payment == null
        || !PaymentDetailsPaymentMethodV2.EFT.equals(payment.getPaymentMethod())
        || payment.getBankAccountDetails() == null) {
      return;
    }
    switch (payment.getBankAccountDetails()) {
      case BankAccountProviderOfficeCreateV2 create -> {
        var template = bankAccountMapper.toBankAccountEntity(create);
        bankDetailsService.createAndLink(
            template, provider, officeLink, create.getActiveDateFrom());
      }
      case BankAccountProviderOfficeLinkV2 link ->
          bankDetailsService.linkExisting(
              link.getBankAccountGUID(), provider, officeLink, link.getActiveDateFrom());
      default -> { // unknown bankAccountDetails subtype — no-op
      }
    }
  }
}
