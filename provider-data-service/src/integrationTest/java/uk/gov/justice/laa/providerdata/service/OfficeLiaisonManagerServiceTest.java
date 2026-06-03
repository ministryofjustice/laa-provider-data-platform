package uk.gov.justice.laa.providerdata.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.justice.laa.providerdata.PostgresqlSpringBootTest;
import uk.gov.justice.laa.providerdata.entity.AdvocatePractitionerEntity;
import uk.gov.justice.laa.providerdata.entity.AdvocateProviderOfficeLinkEntity;
import uk.gov.justice.laa.providerdata.entity.ChamberProviderEntity;
import uk.gov.justice.laa.providerdata.entity.ChamberProviderOfficeLinkEntity;
import uk.gov.justice.laa.providerdata.entity.LiaisonManagerEntity;
import uk.gov.justice.laa.providerdata.entity.LspProviderEntity;
import uk.gov.justice.laa.providerdata.entity.LspProviderOfficeLinkEntity;
import uk.gov.justice.laa.providerdata.entity.OfficeEntity;
import uk.gov.justice.laa.providerdata.entity.OfficeLiaisonManagerLinkEntity;
import uk.gov.justice.laa.providerdata.entity.ProviderEntity;
import uk.gov.justice.laa.providerdata.entity.ProviderOfficeLinkEntity;
import uk.gov.justice.laa.providerdata.model.LiaisonManagerCreateV2;
import uk.gov.justice.laa.providerdata.model.LiaisonManagerLinkChambersV2;
import uk.gov.justice.laa.providerdata.model.LiaisonManagerLinkHeadOfficeV2;
import uk.gov.justice.laa.providerdata.repository.LiaisonManagerRepository;
import uk.gov.justice.laa.providerdata.repository.OfficeLiaisonManagerLinkRepository;
import uk.gov.justice.laa.providerdata.repository.OfficeRepository;
import uk.gov.justice.laa.providerdata.repository.ProviderOfficeLinkRepository;
import uk.gov.justice.laa.providerdata.repository.ProviderRepository;

class OfficeLiaisonManagerServiceTest extends PostgresqlSpringBootTest {

  @Autowired private OfficeLiaisonManagerService service;

  @Autowired private ProviderRepository providerRepository;
  @Autowired private OfficeRepository officeRepository;
  @Autowired private ProviderOfficeLinkRepository providerOfficeLinkRepository;
  @Autowired private OfficeLiaisonManagerLinkRepository officeLiaisonManagerLinkRepository;
  @Autowired private LiaisonManagerRepository liaisonManagerRepository;

  // -- helper --

  private OfficeEntity savedOffice(OffsetDateTime now) {
    return officeRepository.save(
        OfficeEntity.builder()
            .addressLine1("1 Test Street")
            .addressTownOrCity("London")
            .addressPostCode("SW1A 1AA")
            .build());
  }

  private LspProviderOfficeLinkEntity savedLspOfficeLink(
      ProviderEntity provider, OfficeEntity office, String accountNumber, OffsetDateTime now) {
    return providerOfficeLinkRepository.save(
        LspProviderOfficeLinkEntity.builder()
            .provider(provider)
            .office(office)
            .accountNumber(accountNumber)
            .headOfficeFlag(true)
            .createdBy("test")
            .createdTimestamp(now)
            .lastUpdatedBy("test")
            .lastUpdatedTimestamp(now)
            .build());
  }

  private LiaisonManagerCreateV2 createRequest() {
    LiaisonManagerCreateV2 request = new LiaisonManagerCreateV2();
    request.setFirstName("Alice");
    request.setLastName("Jones");
    request.setEmailAddress("alice@example.com");
    request.setTelephoneNumber("0123456789");
    return request;
  }

  // -- UC1: create LM for LSP office (DSTEW-1647) --

  @Test
  void post_createsLiaisonManager_and_linksToOffice_byOfficeCode() {
    final OffsetDateTime now = OffsetDateTime.now();

    ProviderEntity provider =
        LspProviderEntity.builder().firmNumber("100100").name("Test Firm").build();
    provider = providerRepository.save(provider);
    final var providerGuid = provider.getGuid();

    OfficeEntity office = savedOffice(now);
    ProviderOfficeLinkEntity link = savedLspOfficeLink(provider, office, "0Q731M", now);
    final var officeLinkGuid = link.getGuid();

    var result = service.postOfficeLiaisonManager("100100", "0Q731M", createRequest());

    assertThat(result).isNotNull();
    assertThat(result.providerFirmGuid()).isEqualTo(providerGuid);
    assertThat(result.providerFirmNumber()).isEqualTo("100100");
    assertThat(result.officeGuid()).isEqualTo(officeLinkGuid);
    assertThat(result.officeCode()).isEqualTo("0Q731M");
    assertThat(result.liaisonManagerGuid()).isNotNull();

    var persistedLinks = officeLiaisonManagerLinkRepository.findByOfficeLink_Guid(officeLinkGuid);
    assertThat(persistedLinks).hasSize(1);
    assertThat(persistedLinks.getFirst().getLiaisonManager().getGuid())
        .isEqualTo(result.liaisonManagerGuid());
    assertThat(persistedLinks.getFirst().getActiveDateTo()).isNull();
    assertThat(persistedLinks.getFirst().getLinkedFlag()).isFalse();
  }

  // -- AC3: activeDateFrom must be system-derived (DSTEW-1647) --

  @Test
  void post_setsActiveDateFrom_toSystemDate() {
    final OffsetDateTime now = OffsetDateTime.now();

    ProviderEntity provider =
        LspProviderEntity.builder().firmNumber("100101").name("LSP Date Test").build();
    provider = providerRepository.save(provider);
    OfficeEntity office = savedOffice(now);
    savedLspOfficeLink(provider, office, "DATE01", now);

    var result = service.postOfficeLiaisonManager("100101", "DATE01", createRequest());

    var links =
        officeLiaisonManagerLinkRepository.findByOfficeLink_Guid(result.officeGuid());
    assertThat(links).hasSize(1);
    assertThat(links.getFirst().getActiveDateFrom()).isEqualTo(LocalDate.now());
  }

  // -- AC4: reject if office already has an active liaison manager (DSTEW-1647) --

  @Test
  void post_rejectsCreation_whenOfficeAlreadyHasActiveLiaisonManager() {
    final OffsetDateTime now = OffsetDateTime.now();

    ProviderEntity provider =
        LspProviderEntity.builder().firmNumber("100102").name("LSP Conflict Test").build();
    provider = providerRepository.save(provider);
    OfficeEntity office = savedOffice(now);
    final ProviderOfficeLinkEntity officeLink =
        savedLspOfficeLink(provider, office, "CONF01", now);

    LiaisonManagerEntity existing = new LiaisonManagerEntity();
    existing.setFirstName("Bob");
    existing.setLastName("Smith");
    existing.setEmailAddress("bob@example.com");
    existing.setTelephoneNumber("0999999999");
    existing = liaisonManagerRepository.save(existing);

    OfficeLiaisonManagerLinkEntity activeLink = new OfficeLiaisonManagerLinkEntity();
    activeLink.setOfficeLink(officeLink);
    activeLink.setLiaisonManager(existing);
    activeLink.setActiveDateFrom(LocalDate.now().minusDays(10));
    activeLink.setActiveDateTo(null);
    activeLink.setLinkedFlag(false);
    officeLiaisonManagerLinkRepository.save(activeLink);

    assertThatThrownBy(
            () -> service.postOfficeLiaisonManager("100102", "CONF01", createRequest()))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("already has an active liaison manager");

    var linksAfter =
        officeLiaisonManagerLinkRepository.findByOfficeLinkAndActiveDateToIsNull(officeLink);
    assertThat(linksAfter).hasSize(1);
    assertThat(linksAfter.getFirst().getLiaisonManager().getFirstName()).isEqualTo("Bob");
  }

  // -- UC3: create LM for Chambers office (DSTEW-1647) --

  @Test
  void post_createsLiaisonManager_forChambersOffice() {
    final OffsetDateTime now = OffsetDateTime.now();

    ProviderEntity provider =
        ChamberProviderEntity.builder().firmNumber("CH0001").name("Test Chambers").build();
    provider = providerRepository.save(provider);
    OfficeEntity office = savedOffice(now);

    ProviderOfficeLinkEntity officeLink =
        providerOfficeLinkRepository.save(
            ChamberProviderOfficeLinkEntity.builder()
                .provider(provider)
                .office(office)
                .accountNumber("CH001")
                .headOfficeFlag(true)
                .createdBy("test")
                .createdTimestamp(now)
                .lastUpdatedBy("test")
                .lastUpdatedTimestamp(now)
                .build());

    var result = service.postOfficeLiaisonManager("CH0001", "CH001", createRequest());

    assertThat(result.liaisonManagerGuid()).isNotNull();
    var links = officeLiaisonManagerLinkRepository.findByOfficeLink_Guid(officeLink.getGuid());
    assertThat(links).hasSize(1);
    assertThat(links.getFirst().getLinkedFlag()).isFalse();
    assertThat(links.getFirst().getActiveDateTo()).isNull();
  }

  // -- UC4: create LM for Advocate/Barrister office (DSTEW-1647) --

  @Test
  void post_createsLiaisonManager_forAdvocatePractitionerOffice() {
    final OffsetDateTime now = OffsetDateTime.now();

    ProviderEntity provider =
        AdvocatePractitionerEntity.builder().firmNumber("ADV001").name("Test Advocate").build();
    provider = providerRepository.save(provider);
    OfficeEntity office = savedOffice(now);

    ProviderOfficeLinkEntity officeLink =
        providerOfficeLinkRepository.save(
            AdvocateProviderOfficeLinkEntity.builder()
                .provider(provider)
                .office(office)
                .accountNumber("ADV001")
                .headOfficeFlag(true)
                .createdBy("test")
                .createdTimestamp(now)
                .lastUpdatedBy("test")
                .lastUpdatedTimestamp(now)
                .build());

    var result = service.postOfficeLiaisonManager("ADV001", "ADV001", createRequest());

    assertThat(result.liaisonManagerGuid()).isNotNull();
    var links = officeLiaisonManagerLinkRepository.findByOfficeLink_Guid(officeLink.getGuid());
    assertThat(links).hasSize(1);
    assertThat(links.getFirst().getLinkedFlag()).isFalse();
    assertThat(links.getFirst().getActiveDateTo()).isNull();
  }

  // -- UC2: link LSP child office to head office liaison manager --

  @Test
  void post_linksHeadOfficeLiaisonManager_toChildOffice_andSetsLinkedFlag() {
    final OffsetDateTime now = OffsetDateTime.now();

    ProviderEntity provider =
        LspProviderEntity.builder().firmNumber("LSP200").name("LSP Link Test").build();
    provider = providerRepository.save(provider);

    OfficeEntity headOfficeEntity = savedOffice(now);
    final ProviderOfficeLinkEntity headOfficeLink =
        providerOfficeLinkRepository.save(
            LspProviderOfficeLinkEntity.builder()
                .provider(provider)
                .office(headOfficeEntity)
                .accountNumber("HEAD01")
                .headOfficeFlag(true)
                .createdBy("test")
                .createdTimestamp(now)
                .lastUpdatedBy("test")
                .lastUpdatedTimestamp(now)
                .build());

    LiaisonManagerEntity headLm = new LiaisonManagerEntity();
    headLm.setFirstName("Head");
    headLm.setLastName("Manager");
    headLm.setEmailAddress("head@example.com");
    headLm.setTelephoneNumber("0111111111");
    final LiaisonManagerEntity savedHeadLm = liaisonManagerRepository.save(headLm);

    OfficeLiaisonManagerLinkEntity headActiveLink = new OfficeLiaisonManagerLinkEntity();
    headActiveLink.setOfficeLink(headOfficeLink);
    headActiveLink.setLiaisonManager(savedHeadLm);
    headActiveLink.setActiveDateFrom(LocalDate.now().minusDays(5));
    headActiveLink.setActiveDateTo(null);
    headActiveLink.setLinkedFlag(false);
    officeLiaisonManagerLinkRepository.save(headActiveLink);

    OfficeEntity childOfficeEntity = savedOffice(now);
    ProviderOfficeLinkEntity childOfficeLink =
        providerOfficeLinkRepository.save(
            LspProviderOfficeLinkEntity.builder()
                .provider(provider)
                .office(childOfficeEntity)
                .accountNumber("CHILD01")
                .headOfficeFlag(false)
                .createdBy("test")
                .createdTimestamp(now)
                .lastUpdatedBy("test")
                .lastUpdatedTimestamp(now)
                .build());

    LiaisonManagerLinkHeadOfficeV2 request = new LiaisonManagerLinkHeadOfficeV2();
    request.setUseHeadOfficeLiaisonManager(true);

    var result = service.postOfficeLiaisonManager("LSP200", "CHILD01", request);

    assertThat(result.liaisonManagerGuid()).isEqualTo(savedHeadLm.getGuid());

    var childLinks =
        officeLiaisonManagerLinkRepository.findByOfficeLink_Guid(childOfficeLink.getGuid());
    assertThat(childLinks).hasSize(1);
    assertThat(childLinks.getFirst().getLiaisonManager().getGuid())
        .isEqualTo(savedHeadLm.getGuid());
    assertThat(childLinks.getFirst().getLinkedFlag()).isTrue();
    assertThat(childLinks.getFirst().getActiveDateTo()).isNull();
  }

  @Test
  void post_linkHeadOffice_endDatesExistingActiveLink_onChildOffice() {
    final OffsetDateTime now = OffsetDateTime.now();

    ProviderEntity provider =
        LspProviderEntity.builder().firmNumber("LSP201").name("LSP End-Date Test").build();
    provider = providerRepository.save(provider);

    OfficeEntity headOfficeEntity = savedOffice(now);
    final ProviderOfficeLinkEntity headOfficeLink =
        providerOfficeLinkRepository.save(
            LspProviderOfficeLinkEntity.builder()
                .provider(provider)
                .office(headOfficeEntity)
                .accountNumber("HEAD02")
                .headOfficeFlag(true)
                .createdBy("test")
                .createdTimestamp(now)
                .lastUpdatedBy("test")
                .lastUpdatedTimestamp(now)
                .build());

    LiaisonManagerEntity headLm = new LiaisonManagerEntity();
    headLm.setFirstName("Head");
    headLm.setLastName("Mgr");
    headLm.setEmailAddress("head2@example.com");
    headLm.setTelephoneNumber("0222222222");
    final LiaisonManagerEntity savedHeadLm = liaisonManagerRepository.save(headLm);

    OfficeLiaisonManagerLinkEntity headActiveLink = new OfficeLiaisonManagerLinkEntity();
    headActiveLink.setOfficeLink(headOfficeLink);
    headActiveLink.setLiaisonManager(savedHeadLm);
    headActiveLink.setActiveDateFrom(LocalDate.now().minusDays(5));
    headActiveLink.setActiveDateTo(null);
    headActiveLink.setLinkedFlag(false);
    officeLiaisonManagerLinkRepository.save(headActiveLink);

    OfficeEntity childOfficeEntity = savedOffice(now);
    final ProviderOfficeLinkEntity childOfficeLink =
        providerOfficeLinkRepository.save(
            LspProviderOfficeLinkEntity.builder()
                .provider(provider)
                .office(childOfficeEntity)
                .accountNumber("CHILD02")
                .headOfficeFlag(false)
                .createdBy("test")
                .createdTimestamp(now)
                .lastUpdatedBy("test")
                .lastUpdatedTimestamp(now)
                .build());

    LiaisonManagerEntity existingChildLm = new LiaisonManagerEntity();
    existingChildLm.setFirstName("Old");
    existingChildLm.setLastName("Manager");
    existingChildLm.setEmailAddress("old@example.com");
    existingChildLm.setTelephoneNumber("0333333333");
    existingChildLm = liaisonManagerRepository.save(existingChildLm);

    OfficeLiaisonManagerLinkEntity existingChildLink = new OfficeLiaisonManagerLinkEntity();
    existingChildLink.setOfficeLink(childOfficeLink);
    existingChildLink.setLiaisonManager(existingChildLm);
    existingChildLink.setActiveDateFrom(LocalDate.now().minusDays(3));
    existingChildLink.setActiveDateTo(null);
    existingChildLink.setLinkedFlag(false);
    officeLiaisonManagerLinkRepository.save(existingChildLink);

    LiaisonManagerLinkHeadOfficeV2 request = new LiaisonManagerLinkHeadOfficeV2();
    request.setUseHeadOfficeLiaisonManager(true);

    service.postOfficeLiaisonManager("LSP201", "CHILD02", request);

    var childLinks =
        officeLiaisonManagerLinkRepository.findByOfficeLink_Guid(childOfficeLink.getGuid());
    assertThat(childLinks).hasSize(2);
    assertThat(childLinks)
        .anySatisfy(
            l -> {
              assertThat(l.getLiaisonManager().getGuid()).isEqualTo(savedHeadLm.getGuid());
              assertThat(l.getActiveDateTo()).isNull();
              assertThat(l.getLinkedFlag()).isTrue();
            });
    assertThat(childLinks)
        .anySatisfy(
            l -> {
              assertThat(l.getActiveDateTo()).isNotNull();
            });
  }

  // -- UC5: link Chambers child office to Chambers head office liaison manager --

  @Test
  void post_linksChambersLiaisonManager_toChambersChildOffice_andSetsLinkedFlag() {
    final OffsetDateTime now = OffsetDateTime.now();

    ProviderEntity provider =
        ChamberProviderEntity.builder().firmNumber("CH200").name("Chambers Link Test").build();
    provider = providerRepository.save(provider);

    OfficeEntity headOfficeEntity = savedOffice(now);
    final ProviderOfficeLinkEntity headOfficeLink =
        providerOfficeLinkRepository.save(
            ChamberProviderOfficeLinkEntity.builder()
                .provider(provider)
                .office(headOfficeEntity)
                .accountNumber("CHHEAD1")
                .headOfficeFlag(true)
                .createdBy("test")
                .createdTimestamp(now)
                .lastUpdatedBy("test")
                .lastUpdatedTimestamp(now)
                .build());

    LiaisonManagerEntity chambersLm = new LiaisonManagerEntity();
    chambersLm.setFirstName("Chambers");
    chambersLm.setLastName("Manager");
    chambersLm.setEmailAddress("ch@example.com");
    chambersLm.setTelephoneNumber("0444444444");
    final LiaisonManagerEntity savedChambersLm = liaisonManagerRepository.save(chambersLm);

    OfficeLiaisonManagerLinkEntity headActiveLink = new OfficeLiaisonManagerLinkEntity();
    headActiveLink.setOfficeLink(headOfficeLink);
    headActiveLink.setLiaisonManager(savedChambersLm);
    headActiveLink.setActiveDateFrom(LocalDate.now().minusDays(5));
    headActiveLink.setActiveDateTo(null);
    headActiveLink.setLinkedFlag(false);
    officeLiaisonManagerLinkRepository.save(headActiveLink);

    OfficeEntity childOfficeEntity = savedOffice(now);
    ProviderOfficeLinkEntity childOfficeLink =
        providerOfficeLinkRepository.save(
            ChamberProviderOfficeLinkEntity.builder()
                .provider(provider)
                .office(childOfficeEntity)
                .accountNumber("CHCHILD1")
                .headOfficeFlag(false)
                .createdBy("test")
                .createdTimestamp(now)
                .lastUpdatedBy("test")
                .lastUpdatedTimestamp(now)
                .build());

    LiaisonManagerLinkChambersV2 request = new LiaisonManagerLinkChambersV2();
    request.setUseChambersLiaisonManager(true);

    var result = service.postOfficeLiaisonManager("CH200", "CHCHILD1", request);

    assertThat(result.liaisonManagerGuid()).isEqualTo(savedChambersLm.getGuid());

    var childLinks =
        officeLiaisonManagerLinkRepository.findByOfficeLink_Guid(childOfficeLink.getGuid());
    assertThat(childLinks).hasSize(1);
    assertThat(childLinks.getFirst().getLiaisonManager().getGuid())
        .isEqualTo(savedChambersLm.getGuid());
    assertThat(childLinks.getFirst().getLinkedFlag()).isTrue();
    assertThat(childLinks.getFirst().getActiveDateTo()).isNull();
  }
}
