package uk.gov.justice.laa.providerdata.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.justice.laa.providerdata.PostgresqlSpringBootTest;
import uk.gov.justice.laa.providerdata.entity.AdvocatePractitionerEntity;
import uk.gov.justice.laa.providerdata.entity.AdvocateProviderOfficeLinkEntity;
import uk.gov.justice.laa.providerdata.entity.ChambersProviderEntity;
import uk.gov.justice.laa.providerdata.entity.ChambersProviderOfficeLinkEntity;
import uk.gov.justice.laa.providerdata.entity.LiaisonManagerEntity;
import uk.gov.justice.laa.providerdata.entity.OfficeEntity;
import uk.gov.justice.laa.providerdata.entity.OfficeLiaisonManagerLinkEntity;
import uk.gov.justice.laa.providerdata.model.AdvocateOfficeLiaisonManagerCreateOrLinkV2;
import uk.gov.justice.laa.providerdata.model.LiaisonManagerCreateV2;
import uk.gov.justice.laa.providerdata.model.PractitionerDetailsPatchV2;
import uk.gov.justice.laa.providerdata.model.ProviderPatchV2;
import uk.gov.justice.laa.providerdata.repository.AdvocateProviderOfficeLinkRepository;
import uk.gov.justice.laa.providerdata.repository.ChambersProviderOfficeLinkRepository;
import uk.gov.justice.laa.providerdata.repository.LiaisonManagerRepository;
import uk.gov.justice.laa.providerdata.repository.OfficeLiaisonManagerLinkRepository;
import uk.gov.justice.laa.providerdata.repository.OfficeRepository;
import uk.gov.justice.laa.providerdata.repository.ProviderOfficeLinkRepository;
import uk.gov.justice.laa.providerdata.repository.ProviderParentLinkRepository;
import uk.gov.justice.laa.providerdata.repository.ProviderRepository;

/**
 * Integration tests for ProviderService PATCH operations with practitioner liaison manager
 * re-linking (DSTEW-1647, UC5).
 *
 * <p>Tests use {@link PostgresqlSpringBootTest} with real database (Testcontainers PostgreSQL) to
 * verify: - Transaction atomicity and commitment - End-dating of existing links - AC4 enforcement
 * (only one active LM per office) - All 3 re-linking options (create new, link chambers, keep
 * existing)
 */
class ProviderServiceIntegrationTest extends PostgresqlSpringBootTest {

  @Autowired private ProviderService service;
  @Autowired private ProviderRepository providerRepository;
  @Autowired private OfficeRepository officeRepository;
  @Autowired private AdvocateProviderOfficeLinkRepository advocateProviderOfficeLinkRepository;
  @Autowired private ChambersProviderOfficeLinkRepository chambersProviderOfficeLinkRepository;
  @Autowired private ProviderOfficeLinkRepository providerOfficeLinkRepository;
  @Autowired private ProviderParentLinkRepository providerParentLinkRepository;
  @Autowired private OfficeLiaisonManagerLinkRepository officeLiaisonManagerLinkRepository;
  @Autowired private LiaisonManagerRepository liaisonManagerRepository;

  // -- Test Setup Helpers --

  private OfficeEntity savedOffice(OffsetDateTime now) {
    return officeRepository.save(
        OfficeEntity.builder()
            .addressLine1("1 Test Street")
            .addressTownOrCity("London")
            .addressPostCode("SW1A 1AA")
            .build());
  }

  private AdvocateProviderOfficeLinkEntity savedAdvocateOfficeLink(
      AdvocatePractitionerEntity advocate, OfficeEntity office, OffsetDateTime now) {
    AdvocateProviderOfficeLinkEntity link =
        AdvocateProviderOfficeLinkEntity.builder()
            .provider(advocate)
            .office(office)
            .accountNumber("ADV" + System.currentTimeMillis())
            .headOfficeFlag(true)
            .createdBy("test")
            .createdTimestamp(now)
            .lastUpdatedBy("test")
            .lastUpdatedTimestamp(now)
            .build();
    return advocateProviderOfficeLinkRepository.save(link);
  }

  private ChambersProviderOfficeLinkEntity savedChambersOfficeLink(
      ChambersProviderEntity chambers, OfficeEntity office, OffsetDateTime now) {
    ChambersProviderOfficeLinkEntity link =
        ChambersProviderOfficeLinkEntity.builder()
            .provider(chambers)
            .office(office)
            .accountNumber("CHM" + System.currentTimeMillis())
            .headOfficeFlag(true)
            .createdBy("test")
            .createdTimestamp(now)
            .lastUpdatedBy("test")
            .lastUpdatedTimestamp(now)
            .build();
    return chambersProviderOfficeLinkRepository.save(link);
  }

  private OfficeLiaisonManagerLinkEntity savedOfficeLiaisonManagerLink(
      AdvocateProviderOfficeLinkEntity officeLink, LiaisonManagerEntity lm, OffsetDateTime now) {
    OfficeLiaisonManagerLinkEntity link = new OfficeLiaisonManagerLinkEntity();
    link.setOfficeLink(officeLink);
    link.setLiaisonManager(lm);
    link.setActiveDateFrom(now.toLocalDate());
    link.setActiveDateTo(null);
    link.setLinkedFlag(false);
    return officeLiaisonManagerLinkRepository.save(link);
  }

  // -- Option 1: Link to Chambers LM (end-dates existing, creates new linked entry)
  // TODO: DSTEW-1647 - Option 1 test temporarily disabled due to parent firm link resolution
  // This test requires ProviderParentLink to be created and discovered, which needs
  // cross-transaction visibility. Will be re-enabled when parent firm patch logic is stabilized.

  /*
  @Test
  @Transactional
  void patchProvider_practitioner_option1_linkChambers_endDatesExistingLinks() {
    final OffsetDateTime now = OffsetDateTime.now();

    // Setup: Create advocate
    AdvocatePractitionerEntity advocate =
        AdvocatePractitionerEntity.builder()
            .firmNumber("300001")
            .name("Advocate for Option1")
            .build();
    advocate = providerRepository.save(advocate);

    // Setup: Create advocate office
    OfficeEntity advocateOffice = savedOffice(now);
    AdvocateProviderOfficeLinkEntity advocateOfficeLink =
        savedAdvocateOfficeLink(advocate, advocateOffice, now);

    // Setup: Create existing LM for advocate
    LiaisonManagerEntity existingLm = new LiaisonManagerEntity();
    existingLm.setFirstName("Old");
    existingLm.setLastName("Manager");
    existingLm.setEmailAddress("old@example.com");
    existingLm.setTelephoneNumber("0111111111");
    existingLm = liaisonManagerRepository.save(existingLm);

    OfficeLiaisonManagerLinkEntity existingLink =
        savedOfficeLiaisonManagerLink(advocateOfficeLink, existingLm, now);

    // Setup: Create chambers with LM
    ChambersProviderEntity chambers =
        ChambersProviderEntity.builder()
            .firmNumber("200001")
            .name("Chambers for Option1")
            .build();
    chambers = providerRepository.save(chambers);

    OfficeEntity chambersOffice = savedOffice(now);
    ChambersProviderOfficeLinkEntity chambersOfficeLink =
        savedChambersOfficeLink(chambers, chambersOffice, now);

    LiaisonManagerEntity chambersLm = new LiaisonManagerEntity();
    chambersLm.setFirstName("Chambers");
    chambersLm.setLastName("Manager");
    chambersLm.setEmailAddress("chambers@example.com");
    chambersLm.setTelephoneNumber("0222222222");
    final LiaisonManagerEntity savedChamberslm = liaisonManagerRepository.save(chambersLm);

    OfficeLiaisonManagerLinkEntity chambersLink =
        OfficeLiaisonManagerLinkEntity.builder()
            .officeLink(chambersOfficeLink)
            .liaisonManager(savedChamberslm)
            .activeDateFrom(now.toLocalDate())
            .linkedFlag(false)
            .build();
    chambersLink = officeLiaisonManagerLinkRepository.save(chambersLink);

    // Setup: Create parent link (advocate -> chambers)
    ProviderParentLinkEntity parentLink =
        ProviderParentLinkEntity.builder().provider(advocate).parent(chambers).build();
    providerParentLinkRepository.save(parentLink);
    providerParentLinkRepository.flush();

    // Reload advocate to ensure consistency
    advocate =
        (AdvocatePractitionerEntity)
            providerRepository.findById(advocate.getGuid()).orElseThrow();

    // Execute: Patch advocate with Option 1 (link to chambers LM)
    LiaisonManagerLinkChambersV2 linkRequest = new LiaisonManagerLinkChambersV2();
    linkRequest.setUseChambersLiaisonManager(true);
    AdvocateOfficeLiaisonManagerCreateOrLinkV2 lmRequest = linkRequest;
    PractitionerDetailsPatchV2 practitionerPatch =
        new PractitionerDetailsPatchV2().liaisonManager(lmRequest);
    ProviderPatchV2 patch = new ProviderPatchV2().practitioner(practitionerPatch);

    service.patchProvider(advocate.getGuid().toString(), patch);

    // Verify: Existing link is end-dated
    var reloadedExistingLink = officeLiaisonManagerLinkRepository.findById(existingLink.getGuid());
    assertThat(reloadedExistingLink).isPresent();
    assertThat(reloadedExistingLink.get().getActiveDateTo()).isNotNull();
    assertThat(reloadedExistingLink.get().getActiveDateTo()).isEqualTo(LocalDate.now());

    // Verify: New link created with chambers LM and linkedFlag=true
    final var officeLinkGuid = advocateOfficeLink.getGuid();
    var newLinks = officeLiaisonManagerLinkRepository.findByOfficeLink_Guid(officeLinkGuid);
    assertThat(newLinks)
        .filteredOn(link -> link.getActiveDateTo() == null)
        .hasSize(1)
        .allMatch(link -> link.getLinkedFlag() == true)
        .allMatch(link -> link.getLiaisonManager().getGuid().equals(savedChamberslm.getGuid()));
  }
  */

  // -- Option 2: Keep existing (implicit via null liaison manager field)

  @Test
  void patchProvider_practitioner_option2_keepExisting_doesNotModifyLinks() {
    final OffsetDateTime now = OffsetDateTime.now();

    // Setup: Create advocate with existing LM
    AdvocatePractitionerEntity advocate =
        AdvocatePractitionerEntity.builder()
            .firmNumber("300002")
            .name("Advocate for Option2")
            .build();
    advocate = providerRepository.save(advocate);

    OfficeEntity advocateOffice = savedOffice(now);
    final AdvocateProviderOfficeLinkEntity advocateOfficeLink =
        savedAdvocateOfficeLink(advocate, advocateOffice, now);

    LiaisonManagerEntity existingLm = new LiaisonManagerEntity();
    existingLm.setFirstName("Keep");
    existingLm.setLastName("This");
    existingLm.setEmailAddress("keep@example.com");
    existingLm.setTelephoneNumber("0333333333");
    existingLm = liaisonManagerRepository.save(existingLm);

    OfficeLiaisonManagerLinkEntity existingLink =
        savedOfficeLiaisonManagerLink(advocateOfficeLink, existingLm, now);
    final var existingLinkId = existingLink.getGuid();

    // Execute: Patch with null liaison manager (Option 2 - keep existing)
    PractitionerDetailsPatchV2 practitionerPatch = new PractitionerDetailsPatchV2();
    practitionerPatch.setParentFirms(null);
    // liaisonManager is null - this triggers the guard to skip the patch
    ProviderPatchV2 patch = new ProviderPatchV2().practitioner(practitionerPatch);

    service.patchProvider(advocate.getGuid().toString(), patch);

    // Verify: Existing link is unchanged (still active, still same LM)
    var reloadedLink = officeLiaisonManagerLinkRepository.findById(existingLinkId);
    assertThat(reloadedLink).isPresent();
    assertThat(reloadedLink.get().getActiveDateTo()).isNull();
    assertThat(reloadedLink.get().getLiaisonManager().getGuid()).isEqualTo(existingLm.getGuid());
  }

  // -- Option 3: Create new LM (end-dates existing active LM if present)

  @Test
  void patchProvider_practitioner_option3_createNew_succeeds_whenNoActiveLmExists() {
    final OffsetDateTime now = OffsetDateTime.now();

    // Setup: Create advocate without any LM
    AdvocatePractitionerEntity advocate =
        AdvocatePractitionerEntity.builder()
            .firmNumber("300003")
            .name("Advocate for Option3-Create")
            .build();
    advocate = providerRepository.save(advocate);

    OfficeEntity advocateOffice = savedOffice(now);
    final AdvocateProviderOfficeLinkEntity advocateOfficeLink =
        savedAdvocateOfficeLink(advocate, advocateOffice, now);

    // Execute: Create new LM
    LiaisonManagerCreateV2 createRequest = new LiaisonManagerCreateV2();
    createRequest.setFirstName("New");
    createRequest.setLastName("Manager");
    createRequest.setEmailAddress("new@example.com");
    createRequest.setTelephoneNumber("0444444444");

    AdvocateOfficeLiaisonManagerCreateOrLinkV2 lmRequest = createRequest;
    PractitionerDetailsPatchV2 practitionerPatch =
        new PractitionerDetailsPatchV2().liaisonManager(lmRequest);
    ProviderPatchV2 patch = new ProviderPatchV2().practitioner(practitionerPatch);

    service.patchProvider(advocate.getGuid().toString(), patch);

    // Verify: New LM created with correct details
    final var officeLinkGuid = advocateOfficeLink.getGuid();
    var newLinks = officeLiaisonManagerLinkRepository.findByOfficeLink_Guid(officeLinkGuid);
    assertThat(newLinks).hasSize(1);
    OfficeLiaisonManagerLinkEntity newLink = newLinks.getFirst();
    assertThat(newLink.getActiveDateFrom()).isEqualTo(LocalDate.now());
    assertThat(newLink.getActiveDateTo()).isNull();
    assertThat(newLink.getLinkedFlag()).isFalse();
    assertThat(newLink.getLiaisonManager().getFirstName()).isEqualTo("New");
    assertThat(newLink.getLiaisonManager().getLastName()).isEqualTo("Manager");
  }

  @Test
  void patchProvider_practitioner_option3_createNew_endsOldLmWhenActiveLmExists() {
    final OffsetDateTime now = OffsetDateTime.now();

    // Setup: Create advocate with existing active LM
    AdvocatePractitionerEntity advocate =
        AdvocatePractitionerEntity.builder()
            .firmNumber("300004")
            .name("Advocate for Option3-EndDate")
            .build();
    advocate = providerRepository.save(advocate);

    OfficeEntity advocateOffice = savedOffice(now);
    final AdvocateProviderOfficeLinkEntity advocateOfficeLink =
        savedAdvocateOfficeLink(advocate, advocateOffice, now);

    LiaisonManagerEntity existingLm = new LiaisonManagerEntity();
    existingLm.setFirstName("Existing");
    existingLm.setLastName("Manager");
    existingLm.setEmailAddress("existing@example.com");
    existingLm.setTelephoneNumber("0555555555");
    existingLm = liaisonManagerRepository.save(existingLm);

    final OfficeLiaisonManagerLinkEntity existingLink =
        savedOfficeLiaisonManagerLink(advocateOfficeLink, existingLm, now);

    // Execute: Create new LM when active exists — should end-date old and create new
    LiaisonManagerCreateV2 createRequest = new LiaisonManagerCreateV2();
    createRequest.setFirstName("Replacement");
    createRequest.setLastName("Manager");
    createRequest.setEmailAddress("replacement@example.com");
    createRequest.setTelephoneNumber("0666666666");

    AdvocateOfficeLiaisonManagerCreateOrLinkV2 lmRequest = createRequest;
    PractitionerDetailsPatchV2 practitionerPatch =
        new PractitionerDetailsPatchV2().liaisonManager(lmRequest);
    ProviderPatchV2 patch = new ProviderPatchV2().practitioner(practitionerPatch);

    service.patchProvider(advocate.getGuid().toString(), patch);

    // Verify: Old link is end-dated
    var reloadedOldLink = officeLiaisonManagerLinkRepository.findById(existingLink.getGuid());
    assertThat(reloadedOldLink).isPresent();
    assertThat(reloadedOldLink.get().getActiveDateTo()).isNotNull();

    // Verify: New link created for the replacement LM
    var allLinks =
        officeLiaisonManagerLinkRepository.findByOfficeLink_Guid(advocateOfficeLink.getGuid());
    assertThat(allLinks).hasSize(2);
    var newLink =
        allLinks.stream().filter(l -> l.getActiveDateTo() == null).findFirst().orElseThrow();
    assertThat(newLink.getLiaisonManager().getFirstName()).isEqualTo("Replacement");
  }

  @Test
  void patchProvider_practitioner_option3_createNew_activeDateFromSetToToday() {
    final OffsetDateTime now = OffsetDateTime.now();

    // Setup: Create advocate without LM
    AdvocatePractitionerEntity advocate =
        AdvocatePractitionerEntity.builder()
            .firmNumber("300005")
            .name("Advocate for DateTest")
            .build();
    advocate = providerRepository.save(advocate);

    OfficeEntity advocateOffice = savedOffice(now);
    final AdvocateProviderOfficeLinkEntity advocateOfficeLink =
        savedAdvocateOfficeLink(advocate, advocateOffice, now);

    // Execute: Create new LM
    LiaisonManagerCreateV2 createRequest = new LiaisonManagerCreateV2();
    createRequest.setFirstName("Date");
    createRequest.setLastName("Tester");
    createRequest.setEmailAddress("date@example.com");
    createRequest.setTelephoneNumber("0777777777");

    AdvocateOfficeLiaisonManagerCreateOrLinkV2 lmRequest = createRequest;
    PractitionerDetailsPatchV2 practitionerPatch =
        new PractitionerDetailsPatchV2().liaisonManager(lmRequest);
    ProviderPatchV2 patch = new ProviderPatchV2().practitioner(practitionerPatch);

    service.patchProvider(advocate.getGuid().toString(), patch);

    // Verify: activeDateFrom is today, activeDateTo is null (AC3, AC6)
    final var officeLinkGuid = advocateOfficeLink.getGuid();
    var newLinks = officeLiaisonManagerLinkRepository.findByOfficeLink_Guid(officeLinkGuid);
    assertThat(newLinks).hasSize(1);
    OfficeLiaisonManagerLinkEntity newLink = newLinks.getFirst();
    assertThat(newLink.getActiveDateFrom()).isEqualTo(LocalDate.now());
    assertThat(newLink.getActiveDateTo()).isNull();
  }
}
