package uk.gov.justice.laa.providerdata.service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.justice.laa.providerdata.entity.AdvocatePractitionerEntity;
import uk.gov.justice.laa.providerdata.entity.AdvocateProviderOfficeLinkEntity;
import uk.gov.justice.laa.providerdata.entity.BarristerPractitionerEntity;
import uk.gov.justice.laa.providerdata.entity.ChamberProviderOfficeLinkEntity;
import uk.gov.justice.laa.providerdata.entity.FirmType;
import uk.gov.justice.laa.providerdata.entity.LspProviderEntity;
import uk.gov.justice.laa.providerdata.entity.LspProviderOfficeLinkEntity;
import uk.gov.justice.laa.providerdata.entity.PractitionerEntity;
import uk.gov.justice.laa.providerdata.entity.ProviderEntity;
import uk.gov.justice.laa.providerdata.entity.ProviderParentLinkEntity;
import uk.gov.justice.laa.providerdata.exception.ItemNotFoundException;
import uk.gov.justice.laa.providerdata.model.LSPDetailsPatchV2;
import uk.gov.justice.laa.providerdata.model.PractitionerDetailsPatchV2;
import uk.gov.justice.laa.providerdata.model.ProviderFirmTypeV2;
import uk.gov.justice.laa.providerdata.model.ProviderPatchV2;
import uk.gov.justice.laa.providerdata.repository.AdvocateProviderOfficeLinkRepository;
import uk.gov.justice.laa.providerdata.repository.ChamberProviderOfficeLinkRepository;
import uk.gov.justice.laa.providerdata.repository.LspProviderOfficeLinkRepository;
import uk.gov.justice.laa.providerdata.repository.ProviderFirmRepository;
import uk.gov.justice.laa.providerdata.repository.ProviderParentLinkRepository;
import uk.gov.justice.laa.providerdata.repository.ProviderRepository;
import uk.gov.justice.laa.providerdata.repository.spec.ProviderSpecification;

/** Service responsible for provider firm read operations. */
@Service
@Transactional(readOnly = true)
public class ProviderService {

  private final ProviderRepository providerRepository;
  private final LspProviderOfficeLinkRepository lspProviderOfficeLinkRepository;
  private final ChamberProviderOfficeLinkRepository chamberProviderOfficeLinkRepository;
  private final AdvocateProviderOfficeLinkRepository advocateProviderOfficeLinkRepository;
  private final ProviderParentLinkRepository providerParentLinkRepository;
  private final ProviderFirmRepository providerFirmRepository;

  /**
   * Inject dependencies.
   *
   * @param providerRepository to find provider firms
   * @param lspProviderOfficeLinkRepository to find LSP head office links
   * @param chamberProviderOfficeLinkRepository to find Chambers head office links
   * @param advocateProviderOfficeLinkRepository to find Advocate office links
   * @param providerParentLinkRepository to find Advocate parent links
   */
  public ProviderService(
      ProviderRepository providerRepository,
      LspProviderOfficeLinkRepository lspProviderOfficeLinkRepository,
      ChamberProviderOfficeLinkRepository chamberProviderOfficeLinkRepository,
      AdvocateProviderOfficeLinkRepository advocateProviderOfficeLinkRepository,
      ProviderParentLinkRepository providerParentLinkRepository,
      ProviderFirmRepository providerFirmRepository) {
    this.providerRepository = providerRepository;
    this.lspProviderOfficeLinkRepository = lspProviderOfficeLinkRepository;
    this.chamberProviderOfficeLinkRepository = chamberProviderOfficeLinkRepository;
    this.advocateProviderOfficeLinkRepository = advocateProviderOfficeLinkRepository;
    this.providerParentLinkRepository = providerParentLinkRepository;
    this.providerFirmRepository = providerFirmRepository;
  }

  /**
   * Returns a single provider firm by GUID or firm number.
   *
   * @param providerFirmGUIDorFirmNumber UUID string (primary key) or firm number (unique key)
   * @return the matching {@link ProviderEntity}
   * @throws ItemNotFoundException if no provider matches the given identifier
   */
  public ProviderEntity getProvider(String providerFirmGUIDorFirmNumber) {
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

    LSPDetailsPatchV2 lspPatch = patch.getLegalServicesProvider();
    if (lspPatch != null) {
      applyLspPatch(provider, providerFirmGUIDorFirmNumber, lspPatch);
    }

    PractitionerDetailsPatchV2 practitionerPatch = patch.getPractitioner();
    if (practitionerPatch != null) {
      applyPractitionerPatch(provider, providerFirmGUIDorFirmNumber, practitionerPatch);
    }

    ProviderEntity saved = providerRepository.save(provider);

    return ProviderCreationResult.withoutOffice(saved.getGuid(), saved.getFirmNumber());
  }

  private static void applyLspPatch(
      ProviderEntity provider, String providerFirmGUIDorFirmNumber, LSPDetailsPatchV2 lspPatch) {
    if (!(provider instanceof LspProviderEntity lspProvider)) {
      throw new IllegalArgumentException(
          "legalServicesProvider updates require a Legal Services Provider: "
              + providerFirmGUIDorFirmNumber);
    }

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

  private static void applyPractitionerPatch(
      ProviderEntity provider,
      String providerFirmGUIDorFirmNumber,
      PractitionerDetailsPatchV2 practitionerPatch) {
    if (!(provider instanceof PractitionerEntity practitionerProvider)) {
      throw new IllegalArgumentException(
          "practitioner updates require an Advocate provider: " + providerFirmGUIDorFirmNumber);
    }

    if (practitionerPatch.getLiaisonManager() != null) {
      throw new IllegalArgumentException(
          "Practitioner liaison manager updates are not supported on this endpoint");
    }

    if (practitionerPatch.getParentFirms() != null
        && !practitionerPatch.getParentFirms().isEmpty()) {
      throw new IllegalArgumentException(
          "Practitioner parent-firm updates are not supported on this endpoint");
    }

    boolean hasAdvocateFields =
        practitionerPatch.getAdvocateLevel() != null
            || practitionerPatch.getSolicitorRegulationAuthorityRollNumber() != null;
    boolean hasBarristerFields =
        practitionerPatch.getBarristerLevel() != null
            || practitionerPatch.getBarCouncilRollNumber() != null;

    if (hasAdvocateFields && hasBarristerFields) {
      throw new IllegalArgumentException(
          "Practitioner patch must contain either advocate fields or barrister fields, not both");
    }

    if (hasAdvocateFields && !(practitionerProvider instanceof AdvocatePractitionerEntity)) {
      throw new IllegalArgumentException(
          "Advocate fields are only valid for practitioner advocateType=Advocate");
    }

    if (hasBarristerFields && !(practitionerProvider instanceof BarristerPractitionerEntity)) {
      throw new IllegalArgumentException(
          "Barrister fields are only valid for practitioner advocateType=Barrister");
    }

    if (practitionerPatch.getAdvocateLevel() != null) {
      ((AdvocatePractitionerEntity) practitionerProvider)
          .setAdvocateLevel(practitionerPatch.getAdvocateLevel().getValue());
    }

    if (practitionerPatch.getSolicitorRegulationAuthorityRollNumber() != null) {
      ((AdvocatePractitionerEntity) practitionerProvider)
          .setSolicitorRegulationAuthorityRollNumber(
              practitionerPatch.getSolicitorRegulationAuthorityRollNumber());
    }

    if (practitionerPatch.getBarristerLevel() != null) {
      ((BarristerPractitionerEntity) practitionerProvider)
          .setBarristerLevel(practitionerPatch.getBarristerLevel().getValue());
    }

    if (practitionerPatch.getBarCouncilRollNumber() != null) {
      ((BarristerPractitionerEntity) practitionerProvider)
          .setBarCouncilRollNumber(practitionerPatch.getBarCouncilRollNumber());
    }
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

  /** Returns the Chambers head office link for the given provider, if one exists. */
  public Optional<ChamberProviderOfficeLinkEntity> getChambersHeadOffice(ProviderEntity provider) {
    return chamberProviderOfficeLinkRepository.findByProviderAndHeadOfficeFlagTrue(provider);
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
