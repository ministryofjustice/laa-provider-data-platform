package uk.gov.justice.laa.providerdata.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.justice.laa.providerdata.entity.BankAccountEntity;
import uk.gov.justice.laa.providerdata.entity.LiaisonManagerEntity;
import uk.gov.justice.laa.providerdata.entity.LspProviderOfficeLinkEntity;
import uk.gov.justice.laa.providerdata.entity.OfficeEntity;
import uk.gov.justice.laa.providerdata.entity.OfficeLiaisonManagerLinkEntity;
import uk.gov.justice.laa.providerdata.entity.ProviderEntity;
import uk.gov.justice.laa.providerdata.exception.ItemNotFoundException;
import uk.gov.justice.laa.providerdata.mapper.BankAccountMapper;
import uk.gov.justice.laa.providerdata.model.BankAccountProviderOfficeCreateV2;
import uk.gov.justice.laa.providerdata.model.BankAccountProviderOfficeLinkV2;
import uk.gov.justice.laa.providerdata.model.PaymentDetailsCreateOrLinkV2;
import uk.gov.justice.laa.providerdata.model.PaymentDetailsPaymentMethodV2;
import uk.gov.justice.laa.providerdata.repository.LiaisonManagerRepository;
import uk.gov.justice.laa.providerdata.repository.LspProviderOfficeLinkRepository;
import uk.gov.justice.laa.providerdata.repository.OfficeLiaisonManagerLinkRepository;
import uk.gov.justice.laa.providerdata.repository.OfficeRepository;
import uk.gov.justice.laa.providerdata.repository.ProviderRepository;

/** Service responsible for provider firm office operations. */
@Service
@Transactional
public class OfficeService {

  private final ProviderRepository providerRepository;
  private final OfficeRepository officeRepository;
  private final LspProviderOfficeLinkRepository lspProviderOfficeLinkRepository;
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
   * @param liaisonManagerRepository to save liaison manager entities.
   * @param officeLiaisonManagerLinkRepository to save and query office liaison manager links.
   * @param bankDetailsService to create and link bank accounts.
   * @param bankAccountMapper to map bank account request DTOs to entities.
   */
  public OfficeService(
      ProviderRepository providerRepository,
      OfficeRepository officeRepository,
      LspProviderOfficeLinkRepository lspProviderOfficeLinkRepository,
      LiaisonManagerRepository liaisonManagerRepository,
      OfficeLiaisonManagerLinkRepository officeLiaisonManagerLinkRepository,
      BankDetailsService bankDetailsService,
      BankAccountMapper bankAccountMapper) {
    this.providerRepository = providerRepository;
    this.officeRepository = officeRepository;
    this.lspProviderOfficeLinkRepository = lspProviderOfficeLinkRepository;
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
      lmLinkTemplate.setLiaisonManager(savedLm);
      lmLinkTemplate.setOffice(savedOffice);
      officeLiaisonManagerLinkRepository.save(lmLinkTemplate);
    } else if (linkToHeadOfficeLiaisonManager) {
      linkHeadOfficeLiaisonManager(provider, savedOffice);
    }

    persistBankDetails(payment, provider, savedLink);

    return new OfficeCreationResult(
        provider.getGuid(), provider.getFirmNumber(), savedOffice.getGuid(), accountNumber);
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
   * Returns a single LSP office by GUID or account number.
   *
   * <p>The {@code officeGUIDorCode} parameter is first tried as a UUID; if that fails it is treated
   * as an account number.
   *
   * @param providerFirmGUIDorFirmNumber GUID or firm number identifying the parent provider
   * @param officeGUIDorCode office GUID or account number
   * @return the matching {@link LspProviderOfficeLinkEntity}
   * @throws ItemNotFoundException if no matching office is found
   */
  @Transactional(readOnly = true)
  public LspProviderOfficeLinkEntity getLspOffice(
      String providerFirmGUIDorFirmNumber, String officeGUIDorCode) {

    ProviderEntity provider = findProvider(providerFirmGUIDorFirmNumber);
    return findLink(provider, officeGUIDorCode)
        .orElseThrow(() -> new ItemNotFoundException("Office not found: " + officeGUIDorCode));
  }

  private Optional<LspProviderOfficeLinkEntity> findLink(
      ProviderEntity provider, String officeGUIDorCode) {
    try {
      UUID guid = UUID.fromString(officeGUIDorCode);
      return lspProviderOfficeLinkRepository.findByProviderAndOffice_Guid(provider, guid);
    } catch (IllegalArgumentException e) {
      return lspProviderOfficeLinkRepository.findByProviderAndAccountNumber(
          provider, officeGUIDorCode);
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

  private static String generateAccountNumber() {
    return UUID.randomUUID().toString().substring(0, 6).toUpperCase(Locale.UK);
  }

  private void linkHeadOfficeLiaisonManager(ProviderEntity provider, OfficeEntity newOffice) {
    LspProviderOfficeLinkEntity headOfficeLink =
        lspProviderOfficeLinkRepository
            .findByProviderAndHeadOfficeFlagTrue(provider)
            .orElseThrow(
                () ->
                    new ItemNotFoundException(
                        "Head office not found for provider: " + provider.getGuid()));

    OfficeEntity headOffice = headOfficeLink.getOffice();
    List<OfficeLiaisonManagerLinkEntity> activeLmLinks =
        officeLiaisonManagerLinkRepository.findByOfficeAndActiveDateToIsNull(headOffice);

    if (activeLmLinks.isEmpty()) {
      throw new ItemNotFoundException(
          "No active liaison manager found on head office for provider: " + provider.getGuid());
    }

    LiaisonManagerEntity headOfficeLm = activeLmLinks.getFirst().getLiaisonManager();
    OfficeLiaisonManagerLinkEntity link = new OfficeLiaisonManagerLinkEntity();
    link.setLiaisonManager(headOfficeLm);
    link.setOffice(newOffice);
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
          UUID.fromString(link.getBankAccountGUID()),
          provider,
          officeLink,
          link.getActiveDateFrom());
    }
  }
}
