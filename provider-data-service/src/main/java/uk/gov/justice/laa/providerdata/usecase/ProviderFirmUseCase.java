package uk.gov.justice.laa.providerdata.usecase;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.justice.laa.providerdata.bankaccount.BankAccountCommandService;
import uk.gov.justice.laa.providerdata.bankaccount.BankAccountEntity;
import uk.gov.justice.laa.providerdata.bankaccount.BankAccountMapper;
import uk.gov.justice.laa.providerdata.liaisonmanager.LiaisonManagerEntity;
import uk.gov.justice.laa.providerdata.liaisonmanager.OfficeLiaisonManagerCommandService;
import uk.gov.justice.laa.providerdata.liaisonmanager.OfficeLiaisonManagerLinkEntity;
import uk.gov.justice.laa.providerdata.model.BankAccountProviderOfficeCreateV2;
import uk.gov.justice.laa.providerdata.model.LSPDetailsPatchV2;
import uk.gov.justice.laa.providerdata.model.PaymentDetailsCreateV2;
import uk.gov.justice.laa.providerdata.model.PaymentDetailsPaymentMethodV2;
import uk.gov.justice.laa.providerdata.model.PractitionerDetailsParentUpdateV2;
import uk.gov.justice.laa.providerdata.model.PractitionerDetailsParentUpdateV2OneOf;
import uk.gov.justice.laa.providerdata.model.PractitionerDetailsParentUpdateV2OneOf1;
import uk.gov.justice.laa.providerdata.model.PractitionerDetailsPatchV2;
import uk.gov.justice.laa.providerdata.model.ProviderPatchV2;
import uk.gov.justice.laa.providerdata.office.AdvocateProviderOfficeLinkEntity;
import uk.gov.justice.laa.providerdata.office.ChamberProviderOfficeLinkEntity;
import uk.gov.justice.laa.providerdata.office.LspProviderOfficeLinkEntity;
import uk.gov.justice.laa.providerdata.office.OfficeCommandService;
import uk.gov.justice.laa.providerdata.office.OfficeEntity;
import uk.gov.justice.laa.providerdata.office.OfficeQueryService;
import uk.gov.justice.laa.providerdata.office.ProviderOfficeLinkEntity;
import uk.gov.justice.laa.providerdata.provider.AdvocatePractitionerEntity;
import uk.gov.justice.laa.providerdata.provider.BarristerPractitionerEntity;
import uk.gov.justice.laa.providerdata.provider.ChamberProviderEntity;
import uk.gov.justice.laa.providerdata.provider.LspProviderEntity;
import uk.gov.justice.laa.providerdata.provider.PractitionerEntity;
import uk.gov.justice.laa.providerdata.provider.ProviderCommandService;
import uk.gov.justice.laa.providerdata.provider.ProviderEntity;
import uk.gov.justice.laa.providerdata.provider.ProviderParentLinkEntity;
import uk.gov.justice.laa.providerdata.provider.ProviderQueryService;
import uk.gov.justice.laa.providerdata.shared.FirmType;
import uk.gov.justice.laa.providerdata.shared.ItemNotFoundException;
import uk.gov.justice.laa.providerdata.shared.ReferenceNumberUtils;

/**
 * Orchestrates atomic provider firm creation and mutation, including the head office where
 * applicable.
 *
 * <p>LSP and Chambers firms always have a head office created in the same transaction.
 * Practitioners have no head office.
 */
@Service
@RequiredArgsConstructor
public class ProviderFirmUseCase {

  private final ProviderCommandService providerPersistService;
  private final ProviderQueryService providerQueryService;
  private final OfficeCommandService officeCommandService;
  private final OfficeQueryService officeQueryService;
  private final OfficeLiaisonManagerCommandService officeLiaisonManagerCommandService;
  private final BankAccountCommandService bankDetailsService;
  private final BankAccountMapper bankAccountMapper;

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
      LspProviderEntity providerTemplate,
      OfficeEntity officeTemplate,
      LspProviderOfficeLinkEntity linkTemplate,
      @Nullable LiaisonManagerEntity lmTemplate,
      @Nullable OfficeLiaisonManagerLinkEntity lmLinkTemplate,
      @Nullable PaymentDetailsCreateV2 payment) {

    providerTemplate.setFirmNumber(
        ReferenceNumberUtils.generateFirmNumber(providerTemplate.getFirmType()));
    ProviderEntity savedProvider = providerPersistService.save(providerTemplate);

    OfficeEntity savedOffice = officeCommandService.save(officeTemplate);

    String accountNumber =
        ReferenceNumberUtils.generateAccountNumber(savedProvider.getFirmType(), null);
    linkTemplate.setProvider(savedProvider);
    linkTemplate.setOffice(savedOffice);
    linkTemplate.setAccountNumber(accountNumber);
    LspProviderOfficeLinkEntity savedLink = officeCommandService.saveLspOfficeLink(linkTemplate);

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
      ChamberProviderEntity providerTemplate,
      OfficeEntity officeTemplate,
      ChamberProviderOfficeLinkEntity linkTemplate,
      @Nullable LiaisonManagerEntity lmTemplate,
      @Nullable OfficeLiaisonManagerLinkEntity lmLinkTemplate) {

    providerTemplate.setFirmNumber(
        ReferenceNumberUtils.generateFirmNumber(providerTemplate.getFirmType()));
    ProviderEntity savedProvider = providerPersistService.save(providerTemplate);

    OfficeEntity savedOffice = officeCommandService.save(officeTemplate);

    String accountNumber =
        ReferenceNumberUtils.generateAccountNumber(savedProvider.getFirmType(), null);
    linkTemplate.setProvider(savedProvider);
    linkTemplate.setOffice(savedOffice);
    linkTemplate.setAccountNumber(accountNumber);
    ProviderOfficeLinkEntity savedLink = officeCommandService.saveChamberOfficeLink(linkTemplate);

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
    PractitionerEntity saved =
        (PractitionerEntity) providerPersistService.save(practitionerTemplate);

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

  /** Applies supported provider PATCH fields for the resolved provider subtype. */
  @Transactional
  public ProviderCreationResult patchProvider(
      String providerFirmGUIDorFirmNumber, ProviderPatchV2 patch) {
    ProviderEntity provider = getProvider(providerFirmGUIDorFirmNumber);

    if (patch.getName() != null) {
      if (patch.getName().isBlank()) {
        throw new IllegalArgumentException("name must not be blank");
      }
      provider.setName(patch.getName());
    }

    if (patch.getLegalServicesProvider() != null) {
      applyLspPatch(provider, providerFirmGUIDorFirmNumber, patch.getLegalServicesProvider());
    }

    if (patch.getPractitioner() != null) {
      applyPractitionerPatch(provider, providerFirmGUIDorFirmNumber, patch.getPractitioner());
    }

    var saved = providerPersistService.save(provider);
    return ProviderCreationResult.withoutOffice(saved.getGuid(), saved.getFirmNumber());
  }

  private ProviderEntity getProvider(String providerFirmGUIDorFirmNumber) {
    return providerQueryService.getProvider(providerFirmGUIDorFirmNumber);
  }

  private static void applyLspPatch(
      ProviderEntity provider, String providerFirmGUIDorFirmNumber, LSPDetailsPatchV2 lspPatch) {
    switch (provider) {
      case LspProviderEntity lspProvider -> {
        if (lspPatch.getHeadOffice() != null) {
          throw new IllegalArgumentException(
              "Head office reassignment is not supported on this endpoint");
        }
        if (lspPatch.getConstitutionalStatus() != null) {
          lspProvider.setConstitutionalStatus(lspPatch.getConstitutionalStatus().getValue());
        }
        if (lspPatch.getIndemnityReceivedDate() != null) {
          lspProvider.setIndemnityReceivedDate(lspPatch.getIndemnityReceivedDate());
        }
        if (lspPatch.getCompaniesHouseNumber() != null) {
          lspProvider.setCompaniesHouseNumber(lspPatch.getCompaniesHouseNumber());
        }
      }
      default ->
          throw new IllegalArgumentException(
              "legalServicesProvider updates require a Legal Services Provider: "
                  + providerFirmGUIDorFirmNumber);
    }
  }

  private void applyPractitionerPatch(
      ProviderEntity provider,
      String providerFirmGUIDorFirmNumber,
      PractitionerDetailsPatchV2 practitionerPatch) {
    if (!FirmType.ADVOCATE.equals(provider.getFirmType())) {
      throw new IllegalArgumentException(
          "practitioner updates require an Advocate provider: " + providerFirmGUIDorFirmNumber);
    }
    if (practitionerPatch.getLiaisonManager() != null) {
      throw new IllegalArgumentException(
          "Practitioner liaison manager updates are not supported on this endpoint");
    }
    if (practitionerPatch.getParentFirms() != null) {
      applyParentFirmPatch(provider, practitionerPatch.getParentFirms());
    }
    switch (provider) {
      case AdvocatePractitionerEntity advocate -> {
        if (practitionerPatch.getAdvocateLevel() != null) {
          advocate.setAdvocateLevel(practitionerPatch.getAdvocateLevel().getValue());
        }
        if (practitionerPatch.getSolicitorRegulationAuthorityRollNumber() != null) {
          advocate.setSolicitorRegulationAuthorityRollNumber(
              practitionerPatch.getSolicitorRegulationAuthorityRollNumber());
        }
      }
      case BarristerPractitionerEntity barrister -> {
        if (practitionerPatch.getBarristerLevel() != null) {
          barrister.setBarristerLevel(practitionerPatch.getBarristerLevel().getValue());
        }
        if (practitionerPatch.getBarCouncilRollNumber() != null) {
          barrister.setBarCouncilRollNumber(practitionerPatch.getBarCouncilRollNumber());
        }
      }
      default ->
          throw new IllegalArgumentException(
              "practitioner updates require an Advocate provider: " + providerFirmGUIDorFirmNumber);
    }
  }

  private void applyParentFirmPatch(
      ProviderEntity provider, List<PractitionerDetailsParentUpdateV2> parentFirms) {
    if (provider instanceof PractitionerEntity) {
      List<ProviderParentLinkEntity> newLinks = new ArrayList<>();
      for (PractitionerDetailsParentUpdateV2 parentUpdate : parentFirms) {
        newLinks.add(
            ProviderParentLinkEntity.builder()
                .provider(provider)
                .parent(resolveParent(parentUpdate))
                .build());
      }
      providerPersistService.replaceParentLinks(provider, newLinks);
    } else {
      Optional<AdvocateProviderOfficeLinkEntity> existingLink =
          officeQueryService.getAdvocateOfficeLink(provider);
      existingLink.ifPresent(officeCommandService::deleteAdvocateOfficeLink);
      if (!parentFirms.isEmpty()) {
        ProviderEntity parent = resolveParent(parentFirms.get(0));
        ProviderOfficeLinkEntity parentOfficeLink =
            officeQueryService
                .getProviderHeadOfficeLink(parent)
                .orElseThrow(
                    () ->
                        new ItemNotFoundException(
                            "Parent provider has no head office: " + parent.getGuid()));
        officeCommandService.saveAdvocateOfficeLink(
            AdvocateProviderOfficeLinkEntity.builder()
                .provider(provider)
                .office(parentOfficeLink.getOffice())
                .headOfficeFlag(true)
                .build());
      }
    }
  }

  private AdvocateProviderOfficeLinkEntity createAdvocateOfficeLink(
      PractitionerEntity practitioner, ProviderEntity parent) {
    ProviderOfficeLinkEntity parentHeadOffice =
        officeQueryService
            .getProviderHeadOfficeLink(parent)
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
    return officeCommandService.saveAdvocateOfficeLink(link);
  }

  private void persistParentLinks(
      List<PractitionerDetailsParentUpdateV2> parentFirms, PractitionerEntity practitioner) {
    for (PractitionerDetailsParentUpdateV2 parentFirm : parentFirms) {
      ProviderEntity parent = resolveParent(parentFirm);
      providerPersistService.saveParentLink(
          ProviderParentLinkEntity.builder().provider(practitioner).parent(parent).build());
    }
  }

  private ProviderEntity resolveParent(PractitionerDetailsParentUpdateV2 parentFirm) {
    return switch (parentFirm) {
      case PractitionerDetailsParentUpdateV2OneOf byGuid ->
          providerQueryService.getProvider(byGuid.getParentGuid().toString());
      case PractitionerDetailsParentUpdateV2OneOf1 byNumber ->
          providerQueryService.getProvider(byNumber.getParentFirmNumber());
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
      ProviderOfficeLinkEntity savedOfficeLink) {
    if (lmTemplate == null || lmLinkTemplate == null) {
      return;
    }
    lmLinkTemplate.setOfficeLink(savedOfficeLink);
    officeLiaisonManagerCommandService.createAndLink(lmTemplate, lmLinkTemplate);
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
