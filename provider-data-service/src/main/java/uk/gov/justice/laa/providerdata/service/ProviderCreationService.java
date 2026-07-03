package uk.gov.justice.laa.providerdata.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Timer;
import java.util.List;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.justice.laa.providerdata.entity.AdvocateProviderOfficeLinkEntity;
import uk.gov.justice.laa.providerdata.entity.BankAccountEntity;
import uk.gov.justice.laa.providerdata.entity.ChambersProviderEntity;
import uk.gov.justice.laa.providerdata.entity.ChambersProviderOfficeLinkEntity;
import uk.gov.justice.laa.providerdata.entity.ContractManagerEntity;
import uk.gov.justice.laa.providerdata.entity.LiaisonManagerEntity;
import uk.gov.justice.laa.providerdata.entity.LspProviderEntity;
import uk.gov.justice.laa.providerdata.entity.LspProviderOfficeLinkEntity;
import uk.gov.justice.laa.providerdata.entity.OfficeContractManagerLinkEntity;
import uk.gov.justice.laa.providerdata.entity.OfficeEntity;
import uk.gov.justice.laa.providerdata.entity.OfficeLiaisonManagerLinkEntity;
import uk.gov.justice.laa.providerdata.entity.PractitionerEntity;
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
import uk.gov.justice.laa.providerdata.repository.ChambersProviderOfficeLinkRepository;
import uk.gov.justice.laa.providerdata.repository.ContractManagerRepository;
import uk.gov.justice.laa.providerdata.repository.LiaisonManagerRepository;
import uk.gov.justice.laa.providerdata.repository.LspProviderOfficeLinkRepository;
import uk.gov.justice.laa.providerdata.repository.OfficeContractManagerLinkRepository;
import uk.gov.justice.laa.providerdata.repository.OfficeLiaisonManagerLinkRepository;
import uk.gov.justice.laa.providerdata.repository.OfficeRepository;
import uk.gov.justice.laa.providerdata.repository.ProviderOfficeLinkRepository;
import uk.gov.justice.laa.providerdata.repository.ProviderParentLinkRepository;
import uk.gov.justice.laa.providerdata.repository.ProviderRepository;
import uk.gov.justice.laa.providerdata.util.ReferenceNumberUtils;

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
  private final ChambersProviderOfficeLinkRepository chambersProviderOfficeLinkRepository;
  private final AdvocateProviderOfficeLinkRepository advocateProviderOfficeLinkRepository;
  private final ProviderOfficeLinkRepository providerOfficeLinkRepository;
  private final LiaisonManagerRepository liaisonManagerRepository;
  private final OfficeLiaisonManagerLinkRepository officeLiaisonManagerLinkRepository;
  private final ProviderParentLinkRepository providerParentLinkRepository;
  private final ContractManagerRepository contractManagerRepository;
  private final OfficeContractManagerLinkRepository officeContractManagerLinkRepository;
  private final BankDetailsService bankDetailsService;
  private final BankAccountMapper bankAccountMapper;
  private final Counter lspFirmCreationCounter;
  private final Counter chambersFirmCreationCounter;
  private final Counter practitionerFirmCreationCounter;
  private final Timer lspFirmCreationTimer;
  private final Timer chambersFirmCreationTimer;
  private final Timer practitionerFirmCreationTimer;

  /**
   * Inject dependencies.
   *
   * @param providerRepository to save provider entities
   * @param officeRepository to save office entities
   * @param lspProviderOfficeLinkRepository to save LSP office links
   * @param chambersProviderOfficeLinkRepository to save Chambers office links
   * @param advocateProviderOfficeLinkRepository to save Advocate office links
   * @param providerOfficeLinkRepository to look up parent head offices generically
   * @param liaisonManagerRepository to save liaison manager entities
   * @param officeLiaisonManagerLinkRepository to save office liaison manager links
   * @param providerParentLinkRepository to save practitioner parent links
   * @param contractManagerRepository to look up contract managers
   * @param officeContractManagerLinkRepository to save office contract manager links
   * @param bankDetailsService to create and link bank accounts
   * @param bankAccountMapper to map bank account request DTOs to entities
   * @param lspFirmCreationCounter for tracking LSP firm creations
   * @param chambersFirmCreationCounter for tracking Chambers firm creations
   * @param practitionerFirmCreationCounter for tracking Practitioner firm creations
   * @param lspFirmCreationTimer for recording LSP firm creation latency
   * @param chambersFirmCreationTimer for recording Chambers firm creation latency
   * @param practitionerFirmCreationTimer for recording Practitioner firm creation latency
   */
  public ProviderCreationService(
      ProviderRepository providerRepository,
      OfficeRepository officeRepository,
      LspProviderOfficeLinkRepository lspProviderOfficeLinkRepository,
      ChambersProviderOfficeLinkRepository chambersProviderOfficeLinkRepository,
      AdvocateProviderOfficeLinkRepository advocateProviderOfficeLinkRepository,
      ProviderOfficeLinkRepository providerOfficeLinkRepository,
      LiaisonManagerRepository liaisonManagerRepository,
      OfficeLiaisonManagerLinkRepository officeLiaisonManagerLinkRepository,
      ProviderParentLinkRepository providerParentLinkRepository,
      ContractManagerRepository contractManagerRepository,
      OfficeContractManagerLinkRepository officeContractManagerLinkRepository,
      BankDetailsService bankDetailsService,
      BankAccountMapper bankAccountMapper,
      Counter lspFirmCreationCounter,
      Counter chambersFirmCreationCounter,
      Counter practitionerFirmCreationCounter,
      Timer lspFirmCreationTimer,
      Timer chambersFirmCreationTimer,
      Timer practitionerFirmCreationTimer) {
    this.providerRepository = providerRepository;
    this.officeRepository = officeRepository;
    this.lspProviderOfficeLinkRepository = lspProviderOfficeLinkRepository;
    this.chambersProviderOfficeLinkRepository = chambersProviderOfficeLinkRepository;
    this.advocateProviderOfficeLinkRepository = advocateProviderOfficeLinkRepository;
    this.providerOfficeLinkRepository = providerOfficeLinkRepository;
    this.liaisonManagerRepository = liaisonManagerRepository;
    this.officeLiaisonManagerLinkRepository = officeLiaisonManagerLinkRepository;
    this.providerParentLinkRepository = providerParentLinkRepository;
    this.contractManagerRepository = contractManagerRepository;
    this.officeContractManagerLinkRepository = officeContractManagerLinkRepository;
    this.bankDetailsService = bankDetailsService;
    this.bankAccountMapper = bankAccountMapper;
    this.lspFirmCreationCounter = lspFirmCreationCounter;
    this.chambersFirmCreationCounter = chambersFirmCreationCounter;
    this.practitionerFirmCreationCounter = practitionerFirmCreationCounter;
    this.lspFirmCreationTimer = lspFirmCreationTimer;
    this.chambersFirmCreationTimer = chambersFirmCreationTimer;
    this.practitionerFirmCreationTimer = practitionerFirmCreationTimer;
  }

  /**
   * Creates a new Legal Services Provider firm with its head office, and optionally a liaison
   * manager for that office and a bank account.
   *
   * <p>BR-21 requires every LSP to have a Contract Manager; the API layer validates that either
   * {@code contractManagerGuid} or {@code useDefaultContractManager} is supplied before this method
   * is called (see ProviderFirmController#validateLegalServicesProvider). This method is
   * responsible for actually persisting that link - previously the value was validated as present
   * but silently discarded, leaving every newly created LSP head office without a contract manager
   * (see DSTEW-1663).
   *
   * @param providerTemplate partially-populated provider entity (firmType and name set)
   * @param officeTemplate partially-populated office entity (address and contact fields set)
   * @param linkTemplate partially-populated link entity (payment, VAT fields set; headOfficeFlag
   *     must be {@code true})
   * @param lmTemplate liaison manager entity to create, or {@code null} if none
   * @param lmLinkTemplate partially-populated LM link template with activeDateFrom set, or {@code
   *     null} if none
   * @param payment payment details from the request, or {@code null}
   * @param contractManagerGuid GUID of the contract manager to assign to the head office, or {@code
   *     null}
   * @param useDefaultContractManager if {@code true}, link the system default contract manager
   *     instead of {@code contractManagerGuid}
   * @return identifiers for the created provider and head office
   * @throws IllegalArgumentException if {@code contractManagerGuid} is non-null and does not match
   *     any contract manager
   * @throws IllegalStateException if {@code useDefaultContractManager} is true and no default
   *     contract manager is configured
   */
  @Transactional
  public ProviderCreationResult createLspFirm(
      LspProviderEntity providerTemplate,
      OfficeEntity officeTemplate,
      LspProviderOfficeLinkEntity linkTemplate,
      @Nullable LiaisonManagerEntity lmTemplate,
      @Nullable OfficeLiaisonManagerLinkEntity lmLinkTemplate,
      @Nullable PaymentDetailsCreateV2 payment,
      @Nullable UUID contractManagerGuid,
      boolean useDefaultContractManager) {

    providerTemplate.setFirmNumber(
        ReferenceNumberUtils.generateFirmNumber(providerTemplate.getFirmType()));
    ProviderEntity savedProvider = providerRepository.save(providerTemplate);

    OfficeEntity savedOffice = officeRepository.save(officeTemplate);

    String accountNumber =
        ReferenceNumberUtils.generateAccountNumber(savedProvider.getFirmType(), null);
    linkTemplate.setProvider(savedProvider);
    linkTemplate.setOffice(savedOffice);
    linkTemplate.setAccountNumber(accountNumber);
    LspProviderOfficeLinkEntity savedLink = lspProviderOfficeLinkRepository.save(linkTemplate);

    saveLiaisonManagerLink(lmTemplate, lmLinkTemplate, null, savedLink);

    persistBankDetailsForOffice(payment, savedProvider, savedLink);

    if (useDefaultContractManager) {
      linkDefaultContractManager(savedLink);
    } else if (contractManagerGuid != null) {
      linkContractManager(savedLink, contractManagerGuid);
    }

    var sample = io.micrometer.core.instrument.Timer.start();
    sample.stop(lspFirmCreationTimer);
    lspFirmCreationCounter.increment();

    return new ProviderCreationResult(
        savedProvider.getGuid(), savedProvider.getFirmNumber(), savedLink.getGuid(), accountNumber);
  }

  private void linkContractManager(ProviderOfficeLinkEntity savedLink, UUID contractManagerGuid) {
    ContractManagerEntity manager =
        contractManagerRepository
            .findById(contractManagerGuid)
            .orElseThrow(
                () ->
                    new IllegalArgumentException(
                        "Unknown contractManagerGUID: " + contractManagerGuid));
    officeContractManagerLinkRepository.save(
        OfficeContractManagerLinkEntity.builder()
            .officeLink(savedLink)
            .contractManager(manager)
            .build());
  }

  private void linkDefaultContractManager(ProviderOfficeLinkEntity savedLink) {
    ContractManagerEntity manager =
        contractManagerRepository
            .findByDefaultContractManagerTrue()
            .orElseThrow(
                () -> new IllegalStateException("No default contract manager is configured"));
    officeContractManagerLinkRepository.save(
        OfficeContractManagerLinkEntity.builder()
            .officeLink(savedLink)
            .contractManager(manager)
            .build());
  }

  /**
   * Creates a new Chambers firm with its head office, and optionally a liaison manager for that
   * office.
   *
   * <p>Unlike LSPs, Chambers are not required by BR-21 to have a Contract Manager, so {@code
   * contractManagerGuid} and {@code useDefaultContractManager} are both optional; if neither is
   * supplied, no contract manager is linked.
   *
   * @param providerTemplate partially-populated provider entity (firmType and name set)
   * @param officeTemplate partially-populated office entity (address and contact fields set)
   * @param linkTemplate partially-populated link entity (headOfficeFlag must be {@code true})
   * @param lmTemplate liaison manager entity to create, or {@code null} if linking by GUID or none
   * @param lmLinkTemplate partially-populated LM link template with activeDateFrom set, or {@code
   *     null} if linking by GUID or none
   * @param existingLmGuid GUID of an existing liaison manager to link, or {@code null}
   * @param contractManagerGuid GUID of the contract manager to assign to the head office, or {@code
   *     null}
   * @param useDefaultContractManager if {@code true}, link the system default contract manager
   *     instead of {@code contractManagerGuid}
   * @return identifiers for the created provider and head office
   * @throws IllegalArgumentException if {@code contractManagerGuid} is non-null and does not match
   *     any contract manager
   * @throws IllegalStateException if {@code useDefaultContractManager} is true and no default
   *     contract manager is configured
   */
  @Transactional
  public ProviderCreationResult createChambersFirm(
      ChambersProviderEntity providerTemplate,
      OfficeEntity officeTemplate,
      ChambersProviderOfficeLinkEntity linkTemplate,
      @Nullable LiaisonManagerEntity lmTemplate,
      @Nullable OfficeLiaisonManagerLinkEntity lmLinkTemplate,
      @Nullable UUID existingLmGuid,
      @Nullable UUID contractManagerGuid,
      boolean useDefaultContractManager) {

    providerTemplate.setFirmNumber(
        ReferenceNumberUtils.generateFirmNumber(providerTemplate.getFirmType()));
    ProviderEntity savedProvider = providerRepository.save(providerTemplate);

    OfficeEntity savedOffice = officeRepository.save(officeTemplate);

    String accountNumber =
        ReferenceNumberUtils.generateAccountNumber(savedProvider.getFirmType(), null);
    linkTemplate.setProvider(savedProvider);
    linkTemplate.setOffice(savedOffice);
    linkTemplate.setAccountNumber(accountNumber);
    ProviderOfficeLinkEntity savedLink = chambersProviderOfficeLinkRepository.save(linkTemplate);

    saveLiaisonManagerLink(lmTemplate, lmLinkTemplate, existingLmGuid, savedLink);

    if (useDefaultContractManager) {
      linkDefaultContractManager(savedLink);
    } else if (contractManagerGuid != null) {
      linkContractManager(savedLink, contractManagerGuid);
    }

    var sample = io.micrometer.core.instrument.Timer.start();
    sample.stop(chambersFirmCreationTimer);
    chambersFirmCreationCounter.increment();

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
   * @param practitionerTemplate partially-populated provider entity (firmType and name set)
   * @param parentFirms parent firm references from the request, or {@code null}/empty
   * @param payment payment details from the request, or {@code null}
   * @return identifiers for the created provider and head office (if created)
   */
  @Transactional
  public ProviderCreationResult createPractitionerFirm(
      PractitionerEntity practitionerTemplate,
      @Nullable List<PractitionerDetailsParentUpdateV2> parentFirms,
      @Nullable PaymentDetailsCreateV2 payment) {
    practitionerTemplate.setFirmNumber(
        ReferenceNumberUtils.generateFirmNumber(
            practitionerTemplate.getFirmType(), practitionerTemplate.getAdvocateType()));
    PractitionerEntity saved = (PractitionerEntity) providerRepository.save(practitionerTemplate);

    AdvocateProviderOfficeLinkEntity officeLink = null;
    if (parentFirms != null && !parentFirms.isEmpty()) {
      persistParentLinks(parentFirms, saved);
      ProviderEntity firstParent = resolveParent(parentFirms.get(0));
      officeLink = createAdvocateOfficeLink(saved, firstParent);
    }

    if (officeLink != null) {
      persistBankDetailsForOffice(payment, saved, officeLink);
      var sample = io.micrometer.core.instrument.Timer.start();
      sample.stop(practitionerFirmCreationTimer);
      practitionerFirmCreationCounter.increment();
      return new ProviderCreationResult(
          saved.getGuid(),
          saved.getFirmNumber(),
          officeLink.getGuid(),
          officeLink.getAccountNumber());
    }

    persistBankDetailsForPractitioner(payment, saved);
    var sample = io.micrometer.core.instrument.Timer.start();
    sample.stop(practitionerFirmCreationTimer);
    practitionerFirmCreationCounter.increment();
    return ProviderCreationResult.withoutOffice(saved.getGuid(), saved.getFirmNumber());
  }

  private AdvocateProviderOfficeLinkEntity createAdvocateOfficeLink(
      PractitionerEntity practitioner, ProviderEntity parent) {
    ProviderOfficeLinkEntity parentHeadOffice =
        providerOfficeLinkRepository
            .findByProviderAndHeadOfficeFlagTrue(parent)
            .orElseThrow(
                () ->
                    new ItemNotFoundException(
                        "Parent firm has no head office: " + parent.getGuid()));
    AdvocateProviderOfficeLinkEntity link = new AdvocateProviderOfficeLinkEntity();
    link.setProvider(practitioner);
    link.setOffice(parentHeadOffice.getOffice());
    link.setAccountNumber(
        ReferenceNumberUtils.generateAccountNumber(
            practitioner.getFirmType(), practitioner.getAdvocateType()));
    link.setHeadOfficeFlag(Boolean.TRUE);
    return advocateProviderOfficeLinkRepository.save(link);
  }

  private void persistParentLinks(
      List<PractitionerDetailsParentUpdateV2> parentFirms, PractitionerEntity practitioner) {
    for (PractitionerDetailsParentUpdateV2 parentFirm : parentFirms) {
      ProviderEntity parent = resolveParent(parentFirm);
      providerParentLinkRepository.save(
          ProviderParentLinkEntity.builder().provider(practitioner).parent(parent).build());
    }
  }

  private ProviderEntity resolveParent(PractitionerDetailsParentUpdateV2 parentFirm) {
    return switch (parentFirm) {
      case PractitionerDetailsParentUpdateV2OneOf byGuid ->
          providerRepository
              .findById(byGuid.getParentGUID())
              .orElseThrow(
                  () ->
                      new ItemNotFoundException(
                          "Parent provider not found: " + byGuid.getParentGUID()));
      case PractitionerDetailsParentUpdateV2OneOf1 byNumber ->
          providerRepository
              .findByFirmNumber(byNumber.getParentFirmNumber())
              .orElseThrow(
                  () ->
                      new ItemNotFoundException(
                          "Parent provider not found: " + byNumber.getParentFirmNumber()));
      default ->
          throw new IllegalArgumentException("Unknown parentFirm type: " + parentFirm.getClass());
    };
  }

  private void persistBankDetailsForPractitioner(
      @Nullable PaymentDetailsCreateV2 payment, PractitionerEntity practitioner) {
    if (payment == null
        || !PaymentDetailsPaymentMethodV2.EFT.equals(payment.getPaymentMethod())
        || payment.getBankAccountDetails() == null) {
      return;
    }
    BankAccountProviderOfficeCreateV2 create =
        (BankAccountProviderOfficeCreateV2) payment.getBankAccountDetails();
    BankAccountEntity template = bankAccountMapper.toBankAccountEntity(create);
    bankDetailsService.createAndLinkToProvider(template, practitioner);
  }

  private void saveLiaisonManagerLink(
      @Nullable LiaisonManagerEntity lmTemplate,
      @Nullable OfficeLiaisonManagerLinkEntity lmLinkTemplate,
      @Nullable UUID existingLmGuid,
      ProviderOfficeLinkEntity savedOfficeLink) {
    LiaisonManagerEntity lm;
    if (existingLmGuid != null) {
      lm =
          liaisonManagerRepository
              .findById(existingLmGuid)
              .orElseThrow(
                  () -> new ItemNotFoundException("Liaison manager not found: " + existingLmGuid));
      OfficeLiaisonManagerLinkEntity newLink = new OfficeLiaisonManagerLinkEntity();
      newLink.setActiveDateFrom(java.time.LocalDate.now());
      newLink.setLinkedFlag(Boolean.FALSE);
      newLink.setLiaisonManager(lm);
      newLink.setOfficeLink(savedOfficeLink);
      officeLiaisonManagerLinkRepository.save(newLink);
      return;
    }
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
