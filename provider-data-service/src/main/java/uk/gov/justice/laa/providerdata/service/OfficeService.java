package uk.gov.justice.laa.providerdata.service;

import java.net.URI;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.justice.laa.providerdata.entity.BankAccountEntity;
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
import uk.gov.justice.laa.providerdata.model.PaymentDetailsPaymentMethodV2;
import uk.gov.justice.laa.providerdata.repository.LiaisonManagerRepository;
import uk.gov.justice.laa.providerdata.repository.LspProviderOfficeLinkRepository;
import uk.gov.justice.laa.providerdata.repository.OfficeLiaisonManagerLinkRepository;
import uk.gov.justice.laa.providerdata.repository.OfficeRepository;
import uk.gov.justice.laa.providerdata.repository.ProviderOfficeLinkRepository;
import uk.gov.justice.laa.providerdata.repository.ProviderRepository;

/** Service responsible for provider firm office operations. */
@Service
@Transactional
public class OfficeService {

  private final ProviderRepository providerRepository;
  private final OfficeRepository officeRepository;
  private final LspProviderOfficeLinkRepository lspProviderOfficeLinkRepository;
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
      ProviderOfficeLinkRepository providerOfficeLinkRepository,
      LiaisonManagerRepository liaisonManagerRepository,
      OfficeLiaisonManagerLinkRepository officeLiaisonManagerLinkRepository,
      BankDetailsService bankDetailsService,
      BankAccountMapper bankAccountMapper) {
    this.providerRepository = providerRepository;
    this.officeRepository = officeRepository;
    this.lspProviderOfficeLinkRepository = lspProviderOfficeLinkRepository;
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

    OfficeEntity savedOffice = officeRepository.save(officeTemplate);

    String accountNumber = generateAccountNumber();
    linkTemplate.setProvider(provider);
    linkTemplate.setOffice(savedOffice);
    linkTemplate.setAccountNumber(accountNumber);
    LspProviderOfficeLinkEntity savedLink = lspProviderOfficeLinkRepository.save(linkTemplate);

    if (lmTemplate != null && lmLinkTemplate != null) {
      LiaisonManagerEntity savedLm = liaisonManagerRepository.save(lmTemplate);

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
    return parseUuid(officeGUIDorCode)
        .flatMap(guid -> providerOfficeLinkRepository.findByProviderAndGuid(provider, guid))
        .or(
            () ->
                providerOfficeLinkRepository.findByProviderAndAccountNumber(
                    provider, officeGUIDorCode));
  }

  private Optional<LspProviderOfficeLinkEntity> findLspOfficeLink(
      ProviderEntity provider, String officeGUIDorCode) {
    return parseUuid(officeGUIDorCode)
        .flatMap(guid -> lspProviderOfficeLinkRepository.findByProviderAndGuid(provider, guid))
        .or(
            () ->
                lspProviderOfficeLinkRepository.findByProviderAndAccountNumber(
                    provider, officeGUIDorCode));
  }

  /**
   * Patches the contact details of an office.
   *
   * <p>Only fields present in the patch request (non-null) are applied. Address and DX details are
   * replaced as whole objects when provided. Website is stored on the {@link
   * ProviderOfficeLinkEntity}; all other fields are stored on the {@link OfficeEntity}.
   *
   * <p>Contact fields (address, telephone, email, website, DX) are supported for LSP and Chambers
   * offices. Advocate patch requests carry no contact fields and are treated as a no-op.
   *
   * @param providerFirmGUIDorFirmNumber GUID or firm number identifying the parent provider
   * @param officeGUIDorCode office-link GUID or account number identifying the office
   * @param patch the patch request
   * @return identifiers for the provider and office
   * @throws ItemNotFoundException if no provider or office matches the given identifiers
   */
  public OfficeCreationResult patchOffice(
      String providerFirmGUIDorFirmNumber, String officeGUIDorCode, OfficePatchV2 patch) {

    ProviderEntity provider = findProvider(providerFirmGUIDorFirmNumber);
    ProviderOfficeLinkEntity link =
        findProviderOfficeLink(provider, officeGUIDorCode)
            .orElseThrow(() -> new ItemNotFoundException("Office not found: " + officeGUIDorCode));

    switch (patch) {
      case LSPOfficePatchV2 lsp ->
          applyContactPatch(
              link.getOffice(),
              link,
              lsp.getAddress(),
              lsp.getTelephoneNumber(),
              lsp.getEmailAddress(),
              lsp.getWebsite(),
              lsp.getDxDetails());
      case ChambersOfficePatchV2 chambers ->
          applyContactPatch(
              link.getOffice(),
              link,
              chambers.getAddress(),
              chambers.getTelephoneNumber(),
              chambers.getEmailAddress(),
              chambers.getWebsite(),
              chambers.getDxDetails());
      case AdvocateOfficePatchV2 _ -> { // no contact fields on Advocate patch
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

  private ProviderEntity findProvider(String providerFirmGUIDorFirmNumber) {
    try {
      UUID guid = UUID.fromString(providerFirmGUIDorFirmNumber);
      return providerRepository
          .findById(guid)
          .orElseThrow(
              () ->
                  new ItemNotFoundException("Provider not found: " + providerFirmGUIDorFirmNumber));
    } catch (IllegalArgumentException e) {
      return providerRepository
          .findByFirmNumber(providerFirmGUIDorFirmNumber)
          .orElseThrow(
              () ->
                  new ItemNotFoundException("Provider not found: " + providerFirmGUIDorFirmNumber));
    }
  }

  private static Optional<UUID> parseUuid(String value) {
    try {
      return Optional.of(UUID.fromString(value));
    } catch (IllegalArgumentException e) {
      return Optional.empty();
    }
  }

  private static String generateAccountNumber() {
    return UUID.randomUUID().toString().substring(0, 6).toUpperCase(Locale.UK);
  }

  private void linkHeadOfficeLiaisonManager(
      ProviderEntity provider, ProviderOfficeLinkEntity newOfficeLink) {
    LspProviderOfficeLinkEntity headOfficeLink =
        lspProviderOfficeLinkRepository
            .findByProviderAndHeadOfficeFlagTrue(provider)
            .orElseThrow(
                () ->
                    new ItemNotFoundException(
                        "Head office not found for provider: " + provider.getGuid()));

    List<OfficeLiaisonManagerLinkEntity> activeLmLinks =
        officeLiaisonManagerLinkRepository.findByOfficeLinkAndActiveDateToIsNull(headOfficeLink);

    if (activeLmLinks.isEmpty()) {
      throw new ItemNotFoundException(
          "No active liaison manager found on head office for provider: " + provider.getGuid());
    }

    LiaisonManagerEntity headOfficeLm = activeLmLinks.getFirst().getLiaisonManager();
    OfficeLiaisonManagerLinkEntity link = new OfficeLiaisonManagerLinkEntity();
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
    if (payment.getBankAccountDetails() instanceof BankAccountProviderOfficeCreateV2 create) {
      BankAccountEntity template = bankAccountMapper.toBankAccountEntity(create);
      bankDetailsService.createAndLink(template, provider, officeLink, create.getActiveDateFrom());
    } else if (payment.getBankAccountDetails() instanceof BankAccountProviderOfficeLinkV2 link) {
      bankDetailsService.linkExisting(
          link.getBankAccountGUID(), provider, officeLink, link.getActiveDateFrom());
    }
  }
}
