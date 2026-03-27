package uk.gov.justice.laa.providerdata.service;

import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.justice.laa.providerdata.entity.AdvocateProviderOfficeLinkEntity;
import uk.gov.justice.laa.providerdata.entity.BankAccountEntity;
import uk.gov.justice.laa.providerdata.entity.ChamberProviderOfficeLinkEntity;
import uk.gov.justice.laa.providerdata.entity.FirmType;
import uk.gov.justice.laa.providerdata.entity.LiaisonManagerEntity;
import uk.gov.justice.laa.providerdata.entity.LspProviderOfficeLinkEntity;
import uk.gov.justice.laa.providerdata.entity.OfficeEntity;
import uk.gov.justice.laa.providerdata.entity.OfficeLiaisonManagerLinkEntity;
import uk.gov.justice.laa.providerdata.entity.ProviderEntity;
import uk.gov.justice.laa.providerdata.entity.ProviderOfficeLinkEntity;
import uk.gov.justice.laa.providerdata.entity.ProviderParentLinkEntity;
import uk.gov.justice.laa.providerdata.exception.ItemNotFoundException;
import uk.gov.justice.laa.providerdata.mapper.BankAccountMapper;
import uk.gov.justice.laa.providerdata.model.BankAccountProviderOfficeCreateV2;
import uk.gov.justice.laa.providerdata.model.PaymentDetailsCreateV2;
import uk.gov.justice.laa.providerdata.model.PaymentDetailsPaymentMethodV2;
import uk.gov.justice.laa.providerdata.model.PractitionerDetailsParentUpdateV2;
import uk.gov.justice.laa.providerdata.model.PractitionerDetailsParentUpdateV2OneOf;
import uk.gov.justice.laa.providerdata.model.PractitionerDetailsParentUpdateV2OneOf1;
import uk.gov.justice.laa.providerdata.repository.AdvocateProviderOfficeLinkRepository;
import uk.gov.justice.laa.providerdata.repository.ChamberProviderOfficeLinkRepository;
import uk.gov.justice.laa.providerdata.repository.LiaisonManagerRepository;
import uk.gov.justice.laa.providerdata.repository.LspProviderOfficeLinkRepository;
import uk.gov.justice.laa.providerdata.repository.OfficeLiaisonManagerLinkRepository;
import uk.gov.justice.laa.providerdata.repository.OfficeRepository;
import uk.gov.justice.laa.providerdata.repository.ProviderOfficeLinkRepository;
import uk.gov.justice.laa.providerdata.repository.ProviderParentLinkRepository;
import uk.gov.justice.laa.providerdata.repository.ProviderRepository;

/**
 * Orchestrates atomic provider firm creation, including the head office where applicable.
 *
 * <p>LSP and Chambers firms always have a head office created in the same transaction.
 * Practitioners have no head office.
 */
@Service
public class ProviderCreationService {

  private final ProviderRepository providerRepository;
  private final OfficeRepository officeRepository;
  private final LspProviderOfficeLinkRepository lspProviderOfficeLinkRepository;
  private final ChamberProviderOfficeLinkRepository chamberProviderOfficeLinkRepository;
  private final AdvocateProviderOfficeLinkRepository advocateProviderOfficeLinkRepository;
  private final ProviderOfficeLinkRepository providerOfficeLinkRepository;
  private final LiaisonManagerRepository liaisonManagerRepository;
  private final OfficeLiaisonManagerLinkRepository officeLiaisonManagerLinkRepository;
  private final ProviderParentLinkRepository providerParentLinkRepository;
  private final BankDetailsService bankDetailsService;
  private final BankAccountMapper bankAccountMapper;

  /**
   * Inject dependencies.
   *
   * @param providerRepository to save provider entities
   * @param officeRepository to save office entities
   * @param lspProviderOfficeLinkRepository to save LSP office links
   * @param chamberProviderOfficeLinkRepository to save Chambers office links
   * @param advocateProviderOfficeLinkRepository to save Advocate office links
   * @param providerOfficeLinkRepository to look up parent head offices generically
   * @param liaisonManagerRepository to save liaison manager entities
   * @param officeLiaisonManagerLinkRepository to save office liaison manager links
   * @param providerParentLinkRepository to save practitioner parent links
   * @param bankDetailsService to create and link bank accounts
   * @param bankAccountMapper to map bank account request DTOs to entities
   */
  public ProviderCreationService(
      ProviderRepository providerRepository,
      OfficeRepository officeRepository,
      LspProviderOfficeLinkRepository lspProviderOfficeLinkRepository,
      ChamberProviderOfficeLinkRepository chamberProviderOfficeLinkRepository,
      AdvocateProviderOfficeLinkRepository advocateProviderOfficeLinkRepository,
      ProviderOfficeLinkRepository providerOfficeLinkRepository,
      LiaisonManagerRepository liaisonManagerRepository,
      OfficeLiaisonManagerLinkRepository officeLiaisonManagerLinkRepository,
      ProviderParentLinkRepository providerParentLinkRepository,
      BankDetailsService bankDetailsService,
      BankAccountMapper bankAccountMapper) {
    this.providerRepository = providerRepository;
    this.officeRepository = officeRepository;
    this.lspProviderOfficeLinkRepository = lspProviderOfficeLinkRepository;
    this.chamberProviderOfficeLinkRepository = chamberProviderOfficeLinkRepository;
    this.advocateProviderOfficeLinkRepository = advocateProviderOfficeLinkRepository;
    this.providerOfficeLinkRepository = providerOfficeLinkRepository;
    this.liaisonManagerRepository = liaisonManagerRepository;
    this.officeLiaisonManagerLinkRepository = officeLiaisonManagerLinkRepository;
    this.providerParentLinkRepository = providerParentLinkRepository;
    this.bankDetailsService = bankDetailsService;
    this.bankAccountMapper = bankAccountMapper;
  }

  /**
   * Creates a new Legal Services Provider firm with its head office, and optionally a liaison
   * manager for that office and a bank account.
   *
   * @param providerTemplate partially-populated provider entity (firmType and name set)
   * @param officeTemplate partially-populated office entity (address and contact fields set)
   * @param linkTemplate partially-populated link entity (payment, VAT fields set; headOfficeFlag
   *     must be {@code true})
   * @param lmTemplate liaison manager entity to create, or {@code null} if none
   * @param lmLinkTemplate partially-populated LM link template with activeDateFrom set, or {@code
   *     null} if none
   * @param payment payment details from the request, or {@code null}
   * @return identifiers for the created provider and head office
   */
  @Transactional
  public ProviderCreationResult createLspFirm(
      ProviderEntity providerTemplate,
      OfficeEntity officeTemplate,
      LspProviderOfficeLinkEntity linkTemplate,
      @Nullable LiaisonManagerEntity lmTemplate,
      @Nullable OfficeLiaisonManagerLinkEntity lmLinkTemplate,
      @Nullable PaymentDetailsCreateV2 payment) {

    providerTemplate.setFirmNumber(generateFirmNumber(providerTemplate.getFirmType()));
    ProviderEntity savedProvider = providerRepository.save(providerTemplate);

    OfficeEntity savedOffice = officeRepository.save(officeTemplate);

    String accountNumber = generateAccountNumber();
    linkTemplate.setProvider(savedProvider);
    linkTemplate.setOffice(savedOffice);
    linkTemplate.setAccountNumber(accountNumber);
    LspProviderOfficeLinkEntity savedLink = lspProviderOfficeLinkRepository.save(linkTemplate);

    saveLiaisonManagerLink(lmTemplate, lmLinkTemplate, savedLink);

    persistBankDetailsForOffice(payment, savedProvider, savedLink);

    return new ProviderCreationResult(
        savedProvider.getGuid(), savedProvider.getFirmNumber(), savedLink.getGuid(), accountNumber);
  }

  /**
   * Creates a new Chambers firm with its head office, and optionally a liaison manager for that
   * office.
   *
   * @param providerTemplate partially-populated provider entity (firmType and name set)
   * @param officeTemplate partially-populated office entity (address and contact fields set)
   * @param linkTemplate partially-populated link entity (headOfficeFlag must be {@code true})
   * @param lmTemplate liaison manager entity to create, or {@code null} if none
   * @param lmLinkTemplate partially-populated LM link template with activeDateFrom set, or {@code
   *     null} if none
   * @return identifiers for the created provider and head office
   */
  @Transactional
  public ProviderCreationResult createChambersFirm(
      ProviderEntity providerTemplate,
      OfficeEntity officeTemplate,
      ChamberProviderOfficeLinkEntity linkTemplate,
      @Nullable LiaisonManagerEntity lmTemplate,
      @Nullable OfficeLiaisonManagerLinkEntity lmLinkTemplate) {

    providerTemplate.setFirmNumber(generateFirmNumber(providerTemplate.getFirmType()));
    ProviderEntity savedProvider = providerRepository.save(providerTemplate);

    OfficeEntity savedOffice = officeRepository.save(officeTemplate);

    String accountNumber = generateAccountNumber();
    linkTemplate.setProvider(savedProvider);
    linkTemplate.setOffice(savedOffice);
    linkTemplate.setAccountNumber(accountNumber);
    ProviderOfficeLinkEntity savedLink = chamberProviderOfficeLinkRepository.save(linkTemplate);

    saveLiaisonManagerLink(lmTemplate, lmLinkTemplate, savedLink);

    return new ProviderCreationResult(
        savedProvider.getGuid(), savedProvider.getFirmNumber(), savedLink.getGuid(), accountNumber);
  }

  /**
   * Creates a new Practitioner firm with a head office linked to the first parent's Chambers
   * office.
   *
   * <p>If {@code parentFirms} is provided, a {@link ProviderParentLinkEntity} is saved for each
   * entry and an {@link AdvocateProviderOfficeLinkEntity} is created pointing to the first parent's
   * head office. If {@code payment} specifies EFT with bank account details, a bank account is
   * created and linked to the provider and that office link.
   *
   * <p>If no parent firms are provided, no office link is created and bank account details (if any)
   * are linked to the provider only.
   *
   * @param providerTemplate partially-populated provider entity (firmType and name set)
   * @param parentFirms parent firm references from the request, or {@code null}/empty
   * @param payment payment details from the request, or {@code null}
   * @return identifiers for the created provider and head office (if created)
   */
  @Transactional
  public ProviderCreationResult createPractitionerFirm(
      ProviderEntity providerTemplate,
      @Nullable List<PractitionerDetailsParentUpdateV2> parentFirms,
      @Nullable PaymentDetailsCreateV2 payment) {
    providerTemplate.setFirmNumber(generateFirmNumber(providerTemplate.getFirmType()));
    ProviderEntity saved = providerRepository.save(providerTemplate);

    AdvocateProviderOfficeLinkEntity officeLink = null;
    if (parentFirms != null && !parentFirms.isEmpty()) {
      persistParentLinks(parentFirms, saved);
      ProviderEntity firstParent = resolveParent(parentFirms.get(0));
      officeLink = createAdvocateOfficeLink(saved, firstParent);
    }

    if (officeLink != null) {
      persistBankDetailsForOffice(payment, saved, officeLink);
      return new ProviderCreationResult(
          saved.getGuid(),
          saved.getFirmNumber(),
          officeLink.getGuid(),
          officeLink.getAccountNumber());
    }

    persistBankDetailsForPractitioner(payment, saved);
    return ProviderCreationResult.withoutOffice(saved.getGuid(), saved.getFirmNumber());
  }

  private AdvocateProviderOfficeLinkEntity createAdvocateOfficeLink(
      ProviderEntity advocate, ProviderEntity parent) {
    ProviderOfficeLinkEntity parentHeadOffice =
        providerOfficeLinkRepository
            .findByProviderAndHeadOfficeFlagTrue(parent)
            .orElseThrow(
                () ->
                    new ItemNotFoundException(
                        "Parent firm has no head office: " + parent.getGuid()));
    AdvocateProviderOfficeLinkEntity link = new AdvocateProviderOfficeLinkEntity();
    link.setProvider(advocate);
    link.setOffice(parentHeadOffice.getOffice());
    link.setAccountNumber(generateAccountNumber());
    link.setHeadOfficeFlag(Boolean.TRUE);
    return advocateProviderOfficeLinkRepository.save(link);
  }

  private static String generateFirmNumber(String firmType) {
    return firmPrefix(firmType)
        + "-"
        + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.UK);
  }

  private static String firmPrefix(String firmType) {
    if (firmType == null) {
      return "PF";
    }
    return switch (firmType) {
      case FirmType.LEGAL_SERVICES_PROVIDER -> "LSP";
      case FirmType.CHAMBERS -> "CH";
      case FirmType.ADVOCATE -> "ADV";
      default -> "PF";
    };
  }

  private static String generateAccountNumber() {
    return UUID.randomUUID().toString().substring(0, 6).toUpperCase(Locale.UK);
  }

  private void persistParentLinks(
      List<PractitionerDetailsParentUpdateV2> parentFirms, ProviderEntity advocate) {
    for (PractitionerDetailsParentUpdateV2 parentFirm : parentFirms) {
      ProviderEntity parent = resolveParent(parentFirm);
      providerParentLinkRepository.save(
          ProviderParentLinkEntity.builder().provider(advocate).parent(parent).build());
    }
  }

  private ProviderEntity resolveParent(PractitionerDetailsParentUpdateV2 parentFirm) {
    if (parentFirm instanceof PractitionerDetailsParentUpdateV2OneOf byGuid) {
      return providerRepository
          .findById(UUID.fromString(byGuid.getParentGuid()))
          .orElseThrow(
              () ->
                  new ItemNotFoundException(
                      "Parent provider not found: " + byGuid.getParentGuid()));
    }
    if (parentFirm instanceof PractitionerDetailsParentUpdateV2OneOf1 byNumber) {
      return providerRepository
          .findByFirmNumber(byNumber.getParentFirmNumber())
          .orElseThrow(
              () ->
                  new ItemNotFoundException(
                      "Parent provider not found: " + byNumber.getParentFirmNumber()));
    }
    throw new IllegalArgumentException("Unknown parentFirm type: " + parentFirm.getClass());
  }

  private void persistBankDetailsForPractitioner(
      @Nullable PaymentDetailsCreateV2 payment, ProviderEntity provider) {
    if (payment == null
        || !PaymentDetailsPaymentMethodV2.EFT.equals(payment.getPaymentMethod())
        || payment.getBankAccountDetails() == null) {
      return;
    }
    BankAccountProviderOfficeCreateV2 create =
        (BankAccountProviderOfficeCreateV2) payment.getBankAccountDetails();
    BankAccountEntity template = bankAccountMapper.toBankAccountEntity(create);
    bankDetailsService.createAndLinkToProvider(template, provider);
  }

  private void saveLiaisonManagerLink(
      @Nullable LiaisonManagerEntity lmTemplate,
      @Nullable OfficeLiaisonManagerLinkEntity lmLinkTemplate,
      ProviderOfficeLinkEntity savedOfficeLink) {
    if (lmTemplate == null || lmLinkTemplate == null) {
      return;
    }

    LiaisonManagerEntity savedLm = liaisonManagerRepository.save(lmTemplate);

    // Enforce NOT NULL constraints introduced on OfficeLiaisonManagerLinkEntity.
    if (lmLinkTemplate.getActiveDateFrom() == null) {
      lmLinkTemplate.setActiveDateFrom(java.time.LocalDate.now());
    }
    if (lmLinkTemplate.getLinkedFlag() == null) {
      lmLinkTemplate.setLinkedFlag(Boolean.FALSE);
    }

    lmLinkTemplate.setLiaisonManager(savedLm);
    lmLinkTemplate.setOfficeLink(savedOfficeLink);
    officeLiaisonManagerLinkRepository.save(lmLinkTemplate);
  }

  private void persistBankDetailsForOffice(
      @Nullable PaymentDetailsCreateV2 payment,
      ProviderEntity provider,
      ProviderOfficeLinkEntity officeLink) {
    if (payment == null
        || !PaymentDetailsPaymentMethodV2.EFT.equals(payment.getPaymentMethod())
        || payment.getBankAccountDetails() == null) {
      return;
    }
    BankAccountProviderOfficeCreateV2 create =
        (BankAccountProviderOfficeCreateV2) payment.getBankAccountDetails();
    BankAccountEntity template = bankAccountMapper.toBankAccountEntity(create);
    bankDetailsService.createAndLink(template, provider, officeLink, create.getActiveDateFrom());
  }
}
