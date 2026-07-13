package uk.gov.justice.laa.providerdata.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.justice.laa.providerdata.entity.AdvocatePractitionerEntity;
import uk.gov.justice.laa.providerdata.entity.AdvocateProviderOfficeLinkEntity;
import uk.gov.justice.laa.providerdata.entity.BarristerPractitionerEntity;
import uk.gov.justice.laa.providerdata.entity.ChambersProviderOfficeLinkEntity;
import uk.gov.justice.laa.providerdata.entity.FirmType;
import uk.gov.justice.laa.providerdata.entity.LiaisonManagerEntity;
import uk.gov.justice.laa.providerdata.entity.LspProviderEntity;
import uk.gov.justice.laa.providerdata.entity.LspProviderOfficeLinkEntity;
import uk.gov.justice.laa.providerdata.entity.OfficeBankAccountLinkEntity;
import uk.gov.justice.laa.providerdata.entity.OfficeContractManagerLinkEntity;
import uk.gov.justice.laa.providerdata.entity.OfficeLiaisonManagerLinkEntity;
import uk.gov.justice.laa.providerdata.entity.PractitionerEntity;
import uk.gov.justice.laa.providerdata.entity.ProviderEntity;
import uk.gov.justice.laa.providerdata.entity.ProviderOfficeLinkEntity;
import uk.gov.justice.laa.providerdata.entity.ProviderParentLinkEntity;
import uk.gov.justice.laa.providerdata.exception.ItemNotFoundException;
import uk.gov.justice.laa.providerdata.model.AdvocateOfficeLiaisonManagerCreateOrLinkV2;
import uk.gov.justice.laa.providerdata.model.LSPDetailsPatchV2;
import uk.gov.justice.laa.providerdata.model.LSPHeadOfficeDetailsPatchV2;
import uk.gov.justice.laa.providerdata.model.LiaisonManagerCreateV2;
import uk.gov.justice.laa.providerdata.model.LiaisonManagerLinkByGUIDV2;
import uk.gov.justice.laa.providerdata.model.LiaisonManagerLinkChambersV2;
import uk.gov.justice.laa.providerdata.model.PractitionerDetailsParentUpdateV2;
import uk.gov.justice.laa.providerdata.model.PractitionerDetailsParentUpdateV2OneOf;
import uk.gov.justice.laa.providerdata.model.PractitionerDetailsParentUpdateV2OneOf1;
import uk.gov.justice.laa.providerdata.model.PractitionerDetailsPatchV2;
import uk.gov.justice.laa.providerdata.model.ProviderFirmTypeV2;
import uk.gov.justice.laa.providerdata.model.ProviderPatchV2;
import uk.gov.justice.laa.providerdata.repository.AdvocateProviderOfficeLinkRepository;
import uk.gov.justice.laa.providerdata.repository.ChambersProviderOfficeLinkRepository;
import uk.gov.justice.laa.providerdata.repository.LiaisonManagerRepository;
import uk.gov.justice.laa.providerdata.repository.LspProviderOfficeLinkRepository;
import uk.gov.justice.laa.providerdata.repository.OfficeBankAccountLinkRepository;
import uk.gov.justice.laa.providerdata.repository.OfficeContractManagerLinkRepository;
import uk.gov.justice.laa.providerdata.repository.OfficeLiaisonManagerLinkRepository;
import uk.gov.justice.laa.providerdata.repository.ProviderFirmRepository;
import uk.gov.justice.laa.providerdata.repository.ProviderOfficeLinkRepository;
import uk.gov.justice.laa.providerdata.repository.ProviderParentLinkRepository;
import uk.gov.justice.laa.providerdata.repository.ProviderRepository;
import uk.gov.justice.laa.providerdata.repository.spec.ProviderSpecification;
import uk.gov.justice.laa.providerdata.util.UuidUtils;
import uk.gov.justice.laa.providerdata.validation.LspPatchValidator;

/** Service responsible for provider firm read operations. */
@Service
@Transactional(readOnly = true)
public class ProviderService {

  private final ProviderRepository providerRepository;
  private final LspProviderOfficeLinkRepository lspProviderOfficeLinkRepository;
  private final ChambersProviderOfficeLinkRepository chambersProviderOfficeLinkRepository;
  private final AdvocateProviderOfficeLinkRepository advocateProviderOfficeLinkRepository;
  private final ProviderParentLinkRepository providerParentLinkRepository;
  private final ProviderFirmRepository providerFirmRepository;
  private final ProviderOfficeLinkRepository providerOfficeLinkRepository;
  private final OfficeLiaisonManagerLinkRepository officeLiaisonManagerLinkRepository;
  private final OfficeContractManagerLinkRepository officeContractManagerLinkRepository;
  private final OfficeBankAccountLinkRepository officeBankAccountLinkRepository;
  private final LiaisonManagerRepository liaisonManagerRepository;

  /**
   * Inject dependencies.
   *
   * @param providerRepository to find provider firms
   * @param lspProviderOfficeLinkRepository to find LSP head office links
   * @param chambersProviderOfficeLinkRepository to find Chambers head office links
   * @param advocateProviderOfficeLinkRepository to find Advocate office links
   * @param providerParentLinkRepository to find Advocate parent links
   * @param providerOfficeLinkRepository to find general office links
   * @param liaisonManagerRepository to look up liaison manager entities by GUID
   */
  public ProviderService(
      ProviderRepository providerRepository,
      LspProviderOfficeLinkRepository lspProviderOfficeLinkRepository,
      ChambersProviderOfficeLinkRepository chambersProviderOfficeLinkRepository,
      AdvocateProviderOfficeLinkRepository advocateProviderOfficeLinkRepository,
      ProviderParentLinkRepository providerParentLinkRepository,
      ProviderFirmRepository providerFirmRepository,
      ProviderOfficeLinkRepository providerOfficeLinkRepository,
      OfficeLiaisonManagerLinkRepository officeLiaisonManagerLinkRepository,
      OfficeContractManagerLinkRepository officeContractManagerLinkRepository,
      OfficeBankAccountLinkRepository officeBankAccountLinkRepository,
      LiaisonManagerRepository liaisonManagerRepository) {
    this.providerRepository = providerRepository;
    this.lspProviderOfficeLinkRepository = lspProviderOfficeLinkRepository;
    this.chambersProviderOfficeLinkRepository = chambersProviderOfficeLinkRepository;
    this.advocateProviderOfficeLinkRepository = advocateProviderOfficeLinkRepository;
    this.providerParentLinkRepository = providerParentLinkRepository;
    this.providerFirmRepository = providerFirmRepository;
    this.providerOfficeLinkRepository = providerOfficeLinkRepository;
    this.officeLiaisonManagerLinkRepository = officeLiaisonManagerLinkRepository;
    this.officeContractManagerLinkRepository = officeContractManagerLinkRepository;
    this.officeBankAccountLinkRepository = officeBankAccountLinkRepository;
    this.liaisonManagerRepository = liaisonManagerRepository;
  }

  /**
   * Returns a single provider firm by GUID or firm number.
   *
   * @param providerFirmGUIDorFirmNumber UUID string (primary key) or firm number (unique key)
   * @return the matching {@link ProviderEntity}
   * @throws ItemNotFoundException if no provider matches the given identifier
   */
  public ProviderEntity getProvider(String providerFirmGUIDorFirmNumber) {
    var guid = UuidUtils.parseUuid(providerFirmGUIDorFirmNumber);
    return (guid.isPresent()
            ? providerRepository.findById(guid.get())
            : providerRepository.findByFirmNumber(providerFirmGUIDorFirmNumber))
        .orElseThrow(
            () -> new ItemNotFoundException("Provider not found: " + providerFirmGUIDorFirmNumber));
  }

  /** Applies supported provider PATCH fields for the resolved provider subtype. */
  @Transactional
  public ProviderCreationResult patchProvider(
      String providerFirmGUIDorFirmNumber, ProviderPatchV2 patch) {
    ProviderEntity provider = getProvider(providerFirmGUIDorFirmNumber);

    if (patch.getName() != null) {
      if (FirmType.ADVOCATE.equals(provider.getFirmType())) {
        throw new IllegalArgumentException("name must not be amended for practitioners");
      }
      if (patch.getName().isBlank()) {
        throw new IllegalArgumentException("name must not be blank");
      }
      provider.setName(patch.getName());
    }

    var lspPatch = patch.getLegalServicesProvider();
    if (lspPatch != null) {
      applyLspPatch(
          provider, providerFirmGUIDorFirmNumber, lspPatch, lspProviderOfficeLinkRepository);
    }

    var practitionerPatch = patch.getPractitioner();
    if (practitionerPatch != null) {
      applyPractitionerPatch(
          provider,
          providerFirmGUIDorFirmNumber,
          practitionerPatch,
          providerParentLinkRepository,
          advocateProviderOfficeLinkRepository,
          providerRepository,
          providerOfficeLinkRepository,
          officeLiaisonManagerLinkRepository,
          liaisonManagerRepository);
    }

    var saved = providerRepository.save(provider);

    return ProviderCreationResult.withoutOffice(saved.getGuid(), saved.getFirmNumber());
  }

  private static void applyLspPatch(
      ProviderEntity provider,
      String providerFirmGUIDorFirmNumber,
      LSPDetailsPatchV2 lspPatch,
      LspProviderOfficeLinkRepository lspProviderOfficeLinkRepository) {
    switch (provider) {
      case LspProviderEntity lspProvider -> {
        // Get the head office link for validation
        Optional<LspProviderOfficeLinkEntity> headOfficeLinkOpt =
            lspProviderOfficeLinkRepository.findByProviderAndHeadOfficeFlagTrue(lspProvider);

        // Validate the patch against business rules (DSTEW-1574)
        LspPatchValidator.validateLspPatch(lspProvider, lspPatch, headOfficeLinkOpt.orElse(null));

        // Apply provider-level fields
        if (lspPatch.getConstitutionalStatus() != null) {
          lspProvider.setConstitutionalStatus(lspPatch.getConstitutionalStatus().getValue());
        }
        if (lspPatch.getIndemnityReceivedDate() != null) {
          lspProvider.setIndemnityReceivedDate(lspPatch.getIndemnityReceivedDate());
        }
        if (lspPatch.getCompaniesHouseNumber() != null) {
          lspProvider.setCompaniesHouseNumber(lspPatch.getCompaniesHouseNumber());
        }

        // Apply head office link level fields
        if (headOfficeLinkOpt.isPresent()) {
          applyHeadOfficePatch(headOfficeLinkOpt.get(), lspPatch);
        }
      }
      default ->
          throw new IllegalArgumentException(
              "legalServicesProvider updates require a Legal Services Provider: "
                  + providerFirmGUIDorFirmNumber);
    }
  }

  private static void applyHeadOfficePatch(
      LspProviderOfficeLinkEntity headOfficeLink, LSPDetailsPatchV2 lspPatch) {
    // Apply office link level fields from LSPDetailsPatchV2

    // Handle activeDateTo (Inactive Date)
    if (lspPatch.getHeadOffice() != null) {
      LSPHeadOfficeDetailsPatchV2 headOfficePatch = lspPatch.getHeadOffice();
      if (Boolean.TRUE.equals(headOfficePatch.getClearActiveDateTo())) {
        headOfficeLink.setActiveDateTo(null);
      } else if (headOfficePatch.getActiveDateTo() != null) {
        headOfficeLink.setActiveDateTo(headOfficePatch.getActiveDateTo());
      }

      // Apply status flags
      if (headOfficePatch.getDebtRecoveryFlag() != null) {
        headOfficeLink.setDebtRecoveryFlag(headOfficePatch.getDebtRecoveryFlag());
      }

      if (headOfficePatch.getFalseBalanceFlag() != null) {
        // False balance flag would be stored in the office entity
        // For now, this is a placeholder for future implementation
      }

      // Apply intervened details
      if (headOfficePatch.getIntervened() != null) {
        headOfficeLink.setIntervenedFlag(headOfficePatch.getIntervened().getIntervenedFlag());
        headOfficeLink.setIntervenedChangeDate(
            headOfficePatch.getIntervened().getIntervenedChangeDate());
      }

      // Note: Address, payment, DX, VAT, contact details would require updating the Office entity
      // and possibly creating new bank account, payment, and DX records. These are handled
      // separately through the OfficeMapper and should be implemented in a future enhancement.
    }

    // Apply provider-level fields to the head office link
    if (lspPatch.getFirmIntervenedFlag() != null) {
      headOfficeLink.setIntervenedFlag(lspPatch.getFirmIntervenedFlag());
    }
    if (lspPatch.getFirmIntervenedDate() != null) {
      headOfficeLink.setIntervenedChangeDate(lspPatch.getFirmIntervenedDate());
    }

    // Apply hold all payments fields
    if (lspPatch.getHoldAllPaymentsFlag() != null) {
      headOfficeLink.setPaymentHeldFlag(lspPatch.getHoldAllPaymentsFlag());
    }
    if (lspPatch.getHoldAllPaymentsReason() != null) {
      headOfficeLink.setPaymentHeldReason(lspPatch.getHoldAllPaymentsReason());
    }

    // Apply referred to debt recovery flag
    if (lspPatch.getReferredToDebtRecoveryFlag() != null) {
      headOfficeLink.setDebtRecoveryFlag(lspPatch.getReferredToDebtRecoveryFlag());
    }
  }

  private static void applyParentFirmPatch(
      ProviderEntity provider,
      List<PractitionerDetailsParentUpdateV2> parentFirms,
      ProviderParentLinkRepository providerParentLinkRepository,
      AdvocateProviderOfficeLinkRepository advocateProviderOfficeLinkRepository,
      ProviderRepository providerRepository,
      ProviderOfficeLinkRepository providerOfficeLinkRepository) {
    // BR-31 (DS_MAPD_FR_025 AC4): A practitioner must be linked to exactly one Chamber when
    // amending parentFirms. This constraint may change in future to support movement between
    // Chambers; see DSTEW-1735 AC4 for context. `null` means "no parent change"; [] and 2+ are
    // invalid.
    if (parentFirms.size() != 1) {
      throw new IllegalArgumentException(
          "Exactly one parent Chamber must be provided when amending a practitioner");
    }

    ProviderEntity parent = resolveParent(parentFirms.get(0), providerRepository);
    ProviderOfficeLinkEntity parentOfficeLink =
        resolveAndValidateParentHeadOffice(parent, providerOfficeLinkRepository);

    if (provider instanceof PractitionerEntity) {
      List<ProviderParentLinkEntity> existingLinks =
          providerParentLinkRepository.findByProvider(provider);
      providerParentLinkRepository.deleteAll(existingLinks);

      providerParentLinkRepository.save(
          ProviderParentLinkEntity.builder().provider(provider).parent(parent).build());

      // BR-28 (DSTEW-1741 AC3): a practitioner's contact details are inherited from its parent
      // Chamber's office, so reassigning the parent must repoint the practitioner's office link
      // to the new parent's head office.
      AdvocateProviderOfficeLinkEntity practitionerOfficeLink =
          advocateProviderOfficeLinkRepository
              .findByProviderAndHeadOfficeFlagTrue(provider)
              .orElseThrow(
                  () ->
                      new ItemNotFoundException(
                          "Practitioner has no head office link: " + provider.getGuid()));
      practitionerOfficeLink.setOffice(parentOfficeLink.getOffice());
      advocateProviderOfficeLinkRepository.save(practitionerOfficeLink);
    } else {
      Optional<AdvocateProviderOfficeLinkEntity> existingLink =
          advocateProviderOfficeLinkRepository.findByProviderAndHeadOfficeFlagTrue(provider);
      existingLink.ifPresent(advocateProviderOfficeLinkRepository::delete);

      advocateProviderOfficeLinkRepository.save(
          AdvocateProviderOfficeLinkEntity.builder()
              .provider(provider)
              .office(parentOfficeLink.getOffice())
              .headOfficeFlag(true)
              .build());
    }
  }

  private static ProviderOfficeLinkEntity resolveAndValidateParentHeadOffice(
      ProviderEntity parent, ProviderOfficeLinkRepository providerOfficeLinkRepository) {
    if (!FirmType.CHAMBERS.equals(parent.getFirmType())) {
      throw new IllegalArgumentException("Parent firm must be Chambers");
    }

    ProviderOfficeLinkEntity parentOfficeLink =
        providerOfficeLinkRepository
            .findByProviderAndHeadOfficeFlagTrue(parent)
            .orElseThrow(() -> new IllegalArgumentException("Parent Chamber has no head office"));

    // BR-27 (DSTEW-1735 AC3): an amended practitioner must not be linked to an inactive Chamber.
    if (parentOfficeLink.getActiveDateTo() != null) {
      throw new IllegalArgumentException(
          "Parent Chamber is inactive; practitioner cannot be linked to an inactive Chamber");
    }
    return parentOfficeLink;
  }

  private static ProviderEntity resolveParent(
      PractitionerDetailsParentUpdateV2 parentUpdate, ProviderRepository providerRepository) {
    return switch (parentUpdate) {
      case PractitionerDetailsParentUpdateV2OneOf guidUpdate ->
          providerRepository
              .findById(guidUpdate.getParentGUID())
              .orElseThrow(
                  () ->
                      new ItemNotFoundException(
                          "Parent provider not found: " + guidUpdate.getParentGUID()));
      case PractitionerDetailsParentUpdateV2OneOf1 firmNumberUpdate ->
          providerRepository
              .findByFirmNumber(firmNumberUpdate.getParentFirmNumber())
              .orElseThrow(
                  () ->
                      new ItemNotFoundException(
                          "Parent provider not found: " + firmNumberUpdate.getParentFirmNumber()));
      default ->
          throw new IllegalArgumentException(
              "Unsupported parent firm update type: " + parentUpdate.getClass().getName());
    };
  }

  private static void applyPractitionerPatch(
      ProviderEntity provider,
      String providerFirmGUIDorFirmNumber,
      PractitionerDetailsPatchV2 practitionerPatch,
      ProviderParentLinkRepository providerParentLinkRepository,
      AdvocateProviderOfficeLinkRepository advocateProviderOfficeLinkRepository,
      ProviderRepository providerRepository,
      ProviderOfficeLinkRepository providerOfficeLinkRepository,
      OfficeLiaisonManagerLinkRepository officeLiaisonManagerLinkRepository,
      LiaisonManagerRepository liaisonManagerRepository) {
    if (!FirmType.ADVOCATE.equals(provider.getFirmType())) {
      throw new IllegalArgumentException(
          "practitioner updates require an Advocate provider: " + providerFirmGUIDorFirmNumber);
    }

    if (practitionerPatch.getParentFirms() != null
        && !practitionerPatch.getParentFirms().isEmpty()) {
      applyParentFirmPatch(
          provider,
          practitionerPatch.getParentFirms(),
          providerParentLinkRepository,
          advocateProviderOfficeLinkRepository,
          providerRepository,
          providerOfficeLinkRepository);
    } else if (practitionerPatch.getParentFirms() != null
        && practitionerPatch.getParentFirms().isEmpty()
        && hasNoOtherPractitionerPatchFields(practitionerPatch)) {
      throw new IllegalArgumentException(
          "Exactly one parent Chamber must be provided when amending a practitioner");
    }

    if (practitionerPatch.getLiaisonManager() != null) {
      applyPractitionerLiaisonManagerPatch(
          provider,
          practitionerPatch.getLiaisonManager(),
          advocateProviderOfficeLinkRepository,
          providerParentLinkRepository,
          officeLiaisonManagerLinkRepository,
          liaisonManagerRepository,
          providerOfficeLinkRepository);
    }

    switch (provider) {
      case AdvocatePractitionerEntity advocate -> {
        if (practitionerPatch.getAdvocateLevel() != null) {
          advocate.setAdvocateLevel(practitionerPatch.getAdvocateLevel().getValue());
        }
        if (practitionerPatch.getSolicitorRegulationAuthorityRollNumber() != null) {
          if (practitionerPatch.getSolicitorRegulationAuthorityRollNumber().isBlank()) {
            throw new IllegalArgumentException(
                "solicitorRegulationAuthorityRollNumber must not be blank");
          }
          advocate.setSolicitorRegulationAuthorityRollNumber(
              practitionerPatch.getSolicitorRegulationAuthorityRollNumber());
        }
      }

      case BarristerPractitionerEntity barrister -> {
        if (practitionerPatch.getBarristerLevel() != null) {
          barrister.setBarristerLevel(practitionerPatch.getBarristerLevel().getValue());
        }
        if (practitionerPatch.getBarCouncilRollNumber() != null) {
          if (practitionerPatch.getBarCouncilRollNumber().isBlank()) {
            throw new IllegalArgumentException("barCouncilRollNumber must not be blank");
          }
          barrister.setBarCouncilRollNumber(practitionerPatch.getBarCouncilRollNumber());
        }
      }
      default ->
          throw new IllegalArgumentException(
              "practitioner updates require an Advocate provider: " + providerFirmGUIDorFirmNumber);
    }
  }

  private static boolean hasNoOtherPractitionerPatchFields(
      PractitionerDetailsPatchV2 practitionerPatch) {
    return practitionerPatch.getLiaisonManager() == null
        && practitionerPatch.getAdvocateLevel() == null
        && practitionerPatch.getSolicitorRegulationAuthorityRollNumber() == null
        && practitionerPatch.getBarristerLevel() == null
        && practitionerPatch.getBarCouncilRollNumber() == null;
  }

  /**
   * Applies the liaison manager update for a practitioner (Advocate/Barrister) within a PATCH
   * /provider-firms operation.
   *
   * <p>Three options map to DSTEW-1647:
   *
   * <ul>
   *   <li>Option 1 – {@link LiaisonManagerLinkChambersV2}: end-date existing active link and create
   *       a new linked entry pointing to the current chambers LM ({@code linkedFlag=true}).
   *   <li>Option 2 – {@code null} liaison manager: keep existing LM unchanged.
   *   <li>Option 3 – {@link LiaisonManagerCreateV2}: create a new LM and assign it; rejected
   *       (AC4/BR-29) if the advocate's office already has an active liaison manager.
   * </ul>
   *
   * <p>AC3: {@code activeDateFrom} is always set to today's date.
   *
   * <p>AC6: {@code activeDateTo} is never set at creation.
   */
  private static void applyPractitionerLiaisonManagerPatch(
      ProviderEntity provider,
      AdvocateOfficeLiaisonManagerCreateOrLinkV2 lmRequest,
      AdvocateProviderOfficeLinkRepository advocateProviderOfficeLinkRepository,
      ProviderParentLinkRepository providerParentLinkRepository,
      OfficeLiaisonManagerLinkRepository officeLiaisonManagerLinkRepository,
      LiaisonManagerRepository liaisonManagerRepository,
      ProviderOfficeLinkRepository providerOfficeLinkRepository) {
    final ProviderOfficeLinkEntity advocateOfficeLink =
        advocateProviderOfficeLinkRepository.findByProvider(provider).stream()
            .findFirst()
            .orElseThrow(
                () ->
                    new ItemNotFoundException(
                        "No office link found for practitioner: " + provider.getGuid()));
    final LocalDate today = LocalDate.now();

    switch (lmRequest) {
      case LiaisonManagerCreateV2 create -> {
        // Option 3: DSTEW-1652 AC4 – end-date any existing active links before creating the new
        // one.
        var existingLinks =
            officeLiaisonManagerLinkRepository.findByOfficeLink_Guid(advocateOfficeLink.getGuid());
        for (OfficeLiaisonManagerLinkEntity existing : existingLinks) {
          if (existing.getActiveDateTo() == null || existing.getActiveDateTo().isAfter(today)) {
            existing.setActiveDateTo(today);
          }
        }
        LiaisonManagerEntity newLm = new LiaisonManagerEntity();
        newLm.setFirstName(create.getFirstName());
        newLm.setLastName(create.getLastName());
        newLm.setEmailAddress(create.getEmailAddress());
        newLm.setTelephoneNumber(create.getTelephoneNumber());
        final LiaisonManagerEntity savedLm = liaisonManagerRepository.save(newLm);
        final OfficeLiaisonManagerLinkEntity newLink = new OfficeLiaisonManagerLinkEntity();
        newLink.setOfficeLink(advocateOfficeLink);
        newLink.setLiaisonManager(savedLm);
        newLink.setActiveDateFrom(today);
        newLink.setActiveDateTo(null);
        newLink.setLinkedFlag(false);
        officeLiaisonManagerLinkRepository.save(newLink);
      }
      case LiaisonManagerLinkChambersV2 linkChambers -> {
        // Option 1: end-date existing active links then link to chambers LM.
        if (!Boolean.TRUE.equals(linkChambers.getUseChambersLiaisonManager())) {
          throw new IllegalArgumentException("useChambersLiaisonManager must be true");
        }
        final ProviderOfficeLinkEntity chambersOfficeLink =
            resolveChambersOfficeLinkForPractitioner(
                provider, providerParentLinkRepository, providerOfficeLinkRepository);
        final var activeLinksForOffice =
            officeLiaisonManagerLinkRepository.findByOfficeLinkAndActiveDateToIsNull(
                chambersOfficeLink);
        if (activeLinksForOffice.isEmpty()) {
          throw new ItemNotFoundException(
              "No active liaison manager found for chambers office: "
                  + chambersOfficeLink.getGuid());
        }
        final LiaisonManagerEntity chambersLm = activeLinksForOffice.getFirst().getLiaisonManager();
        var existingLinks =
            officeLiaisonManagerLinkRepository.findByOfficeLink_Guid(advocateOfficeLink.getGuid());
        for (OfficeLiaisonManagerLinkEntity existing : existingLinks) {
          if (existing.getActiveDateTo() == null || existing.getActiveDateTo().isAfter(today)) {
            existing.setActiveDateTo(today);
          }
        }
        final OfficeLiaisonManagerLinkEntity newLink = new OfficeLiaisonManagerLinkEntity();
        newLink.setOfficeLink(advocateOfficeLink);
        newLink.setLiaisonManager(chambersLm);
        newLink.setActiveDateFrom(today);
        newLink.setActiveDateTo(null);
        newLink.setLinkedFlag(true);
        officeLiaisonManagerLinkRepository.save(newLink);
      }
      case LiaisonManagerLinkByGUIDV2 lmLink -> {
        // DSTEW-1652: Link by GUID, end-dating existing active links.
        UUID guid = lmLink.getLiaisonManagerGUID();
        final LiaisonManagerEntity existingLmByGuid =
            liaisonManagerRepository
                .findById(guid)
                .orElseThrow(() -> new ItemNotFoundException("Liaison manager not found: " + guid));
        var existingLinks =
            officeLiaisonManagerLinkRepository.findByOfficeLink_Guid(advocateOfficeLink.getGuid());
        for (OfficeLiaisonManagerLinkEntity existing : existingLinks) {
          if (existing.getActiveDateTo() == null || existing.getActiveDateTo().isAfter(today)) {
            existing.setActiveDateTo(today);
          }
        }
        final OfficeLiaisonManagerLinkEntity newLink = new OfficeLiaisonManagerLinkEntity();
        newLink.setOfficeLink(advocateOfficeLink);
        newLink.setLiaisonManager(existingLmByGuid);
        newLink.setActiveDateFrom(today);
        newLink.setActiveDateTo(null);
        newLink.setLinkedFlag(false);
        officeLiaisonManagerLinkRepository.save(newLink);
      }
      default ->
          throw new IllegalArgumentException(
              "Unsupported liaison manager request type: " + lmRequest.getClass().getName());
    }
  }

  /**
   * Resolves the chambers head-office provider-office link for a practitioner by looking up their
   * parent firm (ProviderParentLink) and then finding that parent's head office link.
   */
  private static ProviderOfficeLinkEntity resolveChambersOfficeLinkForPractitioner(
      ProviderEntity provider,
      ProviderParentLinkRepository providerParentLinkRepository,
      ProviderOfficeLinkRepository providerOfficeLinkRepository) {
    List<ProviderParentLinkEntity> parents = providerParentLinkRepository.findByProvider(provider);
    if (parents.isEmpty()) {
      throw new ItemNotFoundException(
          "No parent firm found for practitioner: " + provider.getGuid());
    }
    final ProviderEntity chambersFirm = parents.getFirst().getParent();
    return providerOfficeLinkRepository
        .findByProviderAndHeadOfficeFlagTrue(chambersFirm)
        .orElseThrow(
            () ->
                new ItemNotFoundException(
                    "No head office found for chambers: " + chambersFirm.getGuid()));
  }

  /**
   * Searches provider firms using optional filter criteria with pagination support.
   *
   * <p>Applies filters such as GUID, firm number, name, active status, and provider type. Only
   * non-null filters are considered, and all conditions are combined using AND logic.
   *
   * @param guids list of provider firm GUIDs to filter
   * @param firmNumbers list of provider firm numbers to filter
   * @param name provider name (partial match)
   * @param activeStatus active status filter
   * @param types list of provider firm types to filter
   * @param pageable pagination information
   * @return a paginated list of matching {@link ProviderEntity}
   */
  public Page<ProviderEntity> searchProviders(
      List<String> guids,
      List<String> firmNumbers,
      String name,
      String activeStatus,
      List<ProviderFirmTypeV2> types,
      Pageable pageable) {

    return providerFirmRepository.findAll(
        ProviderSpecification.filter(guids, firmNumbers, name, types), pageable);
  }

  /** Returns the LSP head office link for the given provider, if one exists. */
  public Optional<LspProviderOfficeLinkEntity> getLspHeadOffice(ProviderEntity provider) {
    return lspProviderOfficeLinkRepository.findByProviderAndHeadOfficeFlagTrue(provider);
  }

  /** Returns the active liaison manager link for the office, if one exists. */
  public Optional<OfficeLiaisonManagerLinkEntity> getActiveLiaisonManager(
      ProviderOfficeLinkEntity officeLink) {
    return officeLiaisonManagerLinkRepository
        .findByOfficeLinkAndActiveDateToIsNull(officeLink)
        .stream()
        .findFirst();
  }

  /** Returns one contract manager link for the office, if one exists. */
  public Optional<OfficeContractManagerLinkEntity> getContractManager(
      ProviderOfficeLinkEntity officeLink) {
    return officeContractManagerLinkRepository
        .findByOfficeLink_Guid(officeLink.getGuid(), PageRequest.of(0, 1))
        .stream()
        .findFirst();
  }

  /** Returns the primary bank account link for the office, if one exists. */
  public Optional<OfficeBankAccountLinkEntity> getPrimaryOfficeBankAccount(
      ProviderOfficeLinkEntity officeLink) {
    return officeBankAccountLinkRepository.findByProviderOfficeLinkAndPrimaryFlagTrue(officeLink);
  }

  /** Returns the Chambers head office link for the given provider, if one exists. */
  public Optional<ChambersProviderOfficeLinkEntity> getChambersHeadOffice(ProviderEntity provider) {
    return chambersProviderOfficeLinkRepository.findByProviderAndHeadOfficeFlagTrue(provider);
  }

  /** Returns the Advocate office link for the given provider, if one exists. */
  public Optional<AdvocateProviderOfficeLinkEntity> getAdvocateOfficeLink(ProviderEntity provider) {
    return advocateProviderOfficeLinkRepository.findByProviderAndHeadOfficeFlagTrue(provider);
  }

  /** Returns the parent firm links for the given provider (Advocates only). */
  public List<ProviderParentLinkEntity> getParentLinks(ProviderEntity provider) {
    return providerParentLinkRepository.findByProvider(provider);
  }

  /**
   * Returns a page of practitioners (Advocates) assigned to the given Chambers.
   *
   * @param chambersGUIDorFirmNumber Chambers GUID or firm number
   * @param pageable pagination information
   * @return page of {@link ProviderParentLinkEntity} representing the practitioners
   * @throws IllegalArgumentException if the identifier does not correspond to a Chambers
   * @throws ItemNotFoundException if no provider matches the given identifier
   */
  public Page<ProviderParentLinkEntity> getPractitionersByChambers(
      String chambersGUIDorFirmNumber, Pageable pageable) {
    ProviderEntity provider = getProvider(chambersGUIDorFirmNumber);

    if (!FirmType.CHAMBERS.equals(provider.getFirmType())) {
      throw new IllegalArgumentException("Provider is not a Chambers: " + chambersGUIDorFirmNumber);
    }

    return providerParentLinkRepository.findByParentOrderByProviderNameAsc(provider, pageable);
  }
}
