package uk.gov.justice.laa.providerdata.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import uk.gov.justice.laa.providerdata.entity.AdvocatePractitionerEntity;
import uk.gov.justice.laa.providerdata.entity.AdvocateProviderOfficeLinkEntity;
import uk.gov.justice.laa.providerdata.entity.ChamberProviderEntity;
import uk.gov.justice.laa.providerdata.entity.ChamberProviderOfficeLinkEntity;
import uk.gov.justice.laa.providerdata.entity.FirmType;
import uk.gov.justice.laa.providerdata.entity.LiaisonManagerEntity;
import uk.gov.justice.laa.providerdata.entity.LspProviderEntity;
import uk.gov.justice.laa.providerdata.entity.LspProviderOfficeLinkEntity;
import uk.gov.justice.laa.providerdata.entity.OfficeEntity;
import uk.gov.justice.laa.providerdata.entity.OfficeLiaisonManagerLinkEntity;
import uk.gov.justice.laa.providerdata.entity.ProviderEntity;
import uk.gov.justice.laa.providerdata.entity.ProviderOfficeLinkEntity;
import uk.gov.justice.laa.providerdata.entity.ProviderParentLinkEntity;
import uk.gov.justice.laa.providerdata.exception.ItemNotFoundException;
import uk.gov.justice.laa.providerdata.model.AdvocateOfficeLiaisonManagerCreateOrLinkV2;
import uk.gov.justice.laa.providerdata.model.LSPDetailsConstitutionalStatusV2;
import uk.gov.justice.laa.providerdata.model.LSPDetailsPatchV2;
import uk.gov.justice.laa.providerdata.model.LiaisonManagerCreateV2;
import uk.gov.justice.laa.providerdata.model.LiaisonManagerLinkChambersV2;
import uk.gov.justice.laa.providerdata.model.PractitionerDetailsAdvocateLevelV2;
import uk.gov.justice.laa.providerdata.model.PractitionerDetailsParentUpdateV2OneOf;
import uk.gov.justice.laa.providerdata.model.PractitionerDetailsParentUpdateV2OneOf1;
import uk.gov.justice.laa.providerdata.model.PractitionerDetailsPatchV2;
import uk.gov.justice.laa.providerdata.model.ProviderFirmTypeV2;
import uk.gov.justice.laa.providerdata.model.ProviderPatchV2;
import uk.gov.justice.laa.providerdata.repository.AdvocateProviderOfficeLinkRepository;
import uk.gov.justice.laa.providerdata.repository.ChamberProviderOfficeLinkRepository;
import uk.gov.justice.laa.providerdata.repository.LiaisonManagerRepository;
import uk.gov.justice.laa.providerdata.repository.LspProviderOfficeLinkRepository;
import uk.gov.justice.laa.providerdata.repository.OfficeBankAccountLinkRepository;
import uk.gov.justice.laa.providerdata.repository.OfficeContractManagerLinkRepository;
import uk.gov.justice.laa.providerdata.repository.OfficeLiaisonManagerLinkRepository;
import uk.gov.justice.laa.providerdata.repository.ProviderFirmRepository;
import uk.gov.justice.laa.providerdata.repository.ProviderOfficeLinkRepository;
import uk.gov.justice.laa.providerdata.repository.ProviderParentLinkRepository;
import uk.gov.justice.laa.providerdata.repository.ProviderRepository;

@ExtendWith(MockitoExtension.class)
class ProviderServiceTest {

  @Mock private ProviderRepository providerRepository;
  @Mock private LspProviderOfficeLinkRepository lspProviderOfficeLinkRepository;
  @Mock private ChamberProviderOfficeLinkRepository chamberProviderOfficeLinkRepository;
  @Mock private AdvocateProviderOfficeLinkRepository advocateProviderOfficeLinkRepository;
  @Mock private ProviderParentLinkRepository providerParentLinkRepository;
  @Mock private ProviderFirmRepository providerFirmRepository;
  @Mock private ProviderOfficeLinkRepository providerOfficeLinkRepository;
  @Mock private OfficeLiaisonManagerLinkRepository officeLiaisonManagerLinkRepository;
  @Mock private LiaisonManagerRepository liaisonManagerRepository;
  @Mock private OfficeContractManagerLinkRepository officeContractManagerLinkRepository;
  @Mock private OfficeBankAccountLinkRepository officeBankAccountLinkRepository;

  @InjectMocks private ProviderService service;

  @Test
  void getProvider_byGuid_returnsEntity() {
    UUID guid = UUID.randomUUID();
    ProviderEntity entity = ProviderEntity.builder().name("My LSP").build();
    entity.setGuid(guid);
    when(providerRepository.findById(guid)).thenReturn(Optional.of(entity));

    ProviderEntity result = service.getProvider(guid.toString());

    assertThat(result.getName()).isEqualTo("My LSP");
  }

  @Test
  void getProvider_byFirmNumber_returnsEntity() {
    ProviderEntity entity = ProviderEntity.builder().firmNumber("100001").name("My LSP").build();
    when(providerRepository.findByFirmNumber("100001")).thenReturn(Optional.of(entity));

    ProviderEntity result = service.getProvider("100001");

    assertThat(result.getFirmNumber()).isEqualTo("100001");
  }

  @Test
  void getProvider_byGuid_notFound_throwsItemNotFoundException() {
    UUID guid = UUID.randomUUID();
    when(providerRepository.findById(guid)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.getProvider(guid.toString()))
        .isInstanceOf(ItemNotFoundException.class)
        .hasMessageContaining(guid.toString());
  }

  @Test
  void getProvider_byFirmNumber_notFound_throwsItemNotFoundException() {
    when(providerRepository.findByFirmNumber("UNKNOWN")).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.getProvider("UNKNOWN"))
        .isInstanceOf(ItemNotFoundException.class)
        .hasMessageContaining("UNKNOWN");
  }

  @Test
  void patchProvider_updatesLspNameAndBasicFields_andReturnsIdentifiers() {
    UUID guid = UUID.randomUUID();

    LspProviderEntity existing =
        LspProviderEntity.builder().firmNumber("100001").name("Old Name").build();
    existing.setGuid(guid);

    when(providerRepository.findById(guid)).thenReturn(Optional.of(existing));
    when(providerRepository.save(any(ProviderEntity.class))).thenAnswer(inv -> inv.getArgument(0));

    ProviderPatchV2 patch =
        new ProviderPatchV2()
            .name("New Name")
            .legalServicesProvider(
                new LSPDetailsPatchV2()
                    .constitutionalStatus(LSPDetailsConstitutionalStatusV2.PARTNERSHIP)
                    .indemnityReceivedDate(LocalDate.of(2024, 1, 2))
                    .companiesHouseNumber("12345678"));

    ProviderCreationResult result = service.patchProvider(guid.toString(), patch);

    assertThat(existing.getName()).isEqualTo("New Name");
    assertThat(existing.getConstitutionalStatus()).isEqualTo("Partnership");
    assertThat(existing.getIndemnityReceivedDate()).isEqualTo(LocalDate.of(2024, 1, 2));
    assertThat(existing.getCompaniesHouseNumber()).isEqualTo("12345678");

    assertThat(result.providerFirmGUID()).isEqualTo(guid);
    assertThat(result.firmNumber()).isEqualTo("100001");
  }

  @Test
  void patchProvider_whenProviderNotLsp_rejectsLspPatch() {
    UUID guid = UUID.randomUUID();

    ProviderEntity existing =
        ProviderEntity.builder()
            .firmType(FirmType.CHAMBERS)
            .firmNumber("100002")
            .name("Chambers")
            .build();
    existing.setGuid(guid);

    when(providerRepository.findById(guid)).thenReturn(Optional.of(existing));

    ProviderPatchV2 patch =
        new ProviderPatchV2()
            .name("New")
            .legalServicesProvider(new LSPDetailsPatchV2().companiesHouseNumber("X"));

    assertThatThrownBy(() -> service.patchProvider(guid.toString(), patch))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("legalServicesProvider updates require a Legal Services Provider");
  }

  @Test
  void patchProvider_lspHeadOfficeFieldsUpdated() {
    UUID guid = UUID.randomUUID();
    UUID headOfficeGuid = UUID.randomUUID();

    LspProviderEntity existing =
        LspProviderEntity.builder().firmNumber("100001").name("Old Name").build();
    existing.setGuid(guid);

    LspProviderOfficeLinkEntity headOfficeLink =
        LspProviderOfficeLinkEntity.builder()
            .guid(headOfficeGuid)
            .provider(existing)
            .headOfficeFlag(true)
            .build();

    when(providerRepository.findById(guid)).thenReturn(Optional.of(existing));
    when(lspProviderOfficeLinkRepository.findByProviderAndHeadOfficeFlagTrue(existing))
        .thenReturn(Optional.of(headOfficeLink));
    when(providerRepository.save(any(ProviderEntity.class))).thenAnswer(inv -> inv.getArgument(0));

    ProviderPatchV2 patch =
        new ProviderPatchV2()
            .legalServicesProvider(
                new LSPDetailsPatchV2()
                    .firmIntervenedFlag(true)
                    .firmIntervenedDate(LocalDate.now())
                    .holdAllPaymentsFlag(true)
                    .holdAllPaymentsReason("Financial review")
                    .referredToDebtRecoveryFlag(true));

    ProviderCreationResult result = service.patchProvider(guid.toString(), patch);

    assertThat(headOfficeLink.getIntervenedFlag()).isTrue();
    assertThat(headOfficeLink.getIntervenedChangeDate()).isEqualTo(LocalDate.now());
    assertThat(headOfficeLink.getPaymentHeldFlag()).isTrue();
    assertThat(headOfficeLink.getPaymentHeldReason()).isEqualTo("Financial review");
    assertThat(headOfficeLink.getDebtRecoveryFlag()).isTrue();
    assertThat(result.providerFirmGUID()).isEqualTo(guid);
    assertThat(result.firmNumber()).isEqualTo("100001");
  }

  @Test
  void patchProvider_updatesAdvocatePractitionerFields() {
    UUID guid = UUID.randomUUID();

    AdvocatePractitionerEntity existing =
        AdvocatePractitionerEntity.builder().firmNumber("100003").name("Old Name").build();
    existing.setGuid(guid);

    when(providerRepository.findById(guid)).thenReturn(Optional.of(existing));
    when(providerRepository.save(any(ProviderEntity.class))).thenAnswer(inv -> inv.getArgument(0));

    ProviderPatchV2 patch =
        new ProviderPatchV2()
            .practitioner(
                new PractitionerDetailsPatchV2()
                    .advocateLevel(PractitionerDetailsAdvocateLevelV2.KC)
                    .solicitorRegulationAuthorityRollNumber("SRA-123"));

    ProviderCreationResult result = service.patchProvider(guid.toString(), patch);

    assertThat(existing.getAdvocateLevel()).isEqualTo("KC");
    assertThat(existing.getSolicitorRegulationAuthorityRollNumber()).isEqualTo("SRA-123");
    assertThat(result.providerFirmGUID()).isEqualTo(guid);
    assertThat(result.firmNumber()).isEqualTo("100003");
  }

  @Test
  void patchProvider_updatesParentFirms() {
    UUID guid = UUID.randomUUID();
    UUID parentGuid = UUID.randomUUID();
    String parentFirmNumber = "200001";

    AdvocatePractitionerEntity existing =
        AdvocatePractitionerEntity.builder().firmNumber("100003").name("Practitioner").build();
    existing.setGuid(guid);

    ProviderEntity parentByGuid = ProviderEntity.builder().name("Parent 1").build();
    parentByGuid.setGuid(parentGuid);

    ProviderEntity parentByFirmNumber =
        ProviderEntity.builder().firmNumber(parentFirmNumber).name("Parent 2").build();
    parentByFirmNumber.setGuid(UUID.randomUUID());

    when(providerRepository.findById(guid)).thenReturn(Optional.of(existing));
    when(providerRepository.findById(parentGuid)).thenReturn(Optional.of(parentByGuid));
    when(providerRepository.findByFirmNumber(parentFirmNumber))
        .thenReturn(Optional.of(parentByFirmNumber));
    when(providerParentLinkRepository.findByProvider(existing)).thenReturn(List.of());
    when(providerRepository.save(any(ProviderEntity.class))).thenAnswer(inv -> inv.getArgument(0));

    ProviderPatchV2 patch =
        new ProviderPatchV2()
            .practitioner(
                new PractitionerDetailsPatchV2()
                    .parentFirms(
                        List.of(
                            new PractitionerDetailsParentUpdateV2OneOf(parentGuid),
                            new PractitionerDetailsParentUpdateV2OneOf1(parentFirmNumber))));

    service.patchProvider(guid.toString(), patch);

    ArgumentCaptor<ProviderParentLinkEntity> linkCaptor =
        org.mockito.ArgumentCaptor.forClass(ProviderParentLinkEntity.class);
    verify(providerParentLinkRepository, org.mockito.Mockito.times(2)).save(linkCaptor.capture());

    List<ProviderParentLinkEntity> savedLinks = linkCaptor.getAllValues();
    assertThat(savedLinks).hasSize(2);
    assertThat(savedLinks.get(0).getProvider()).isEqualTo(existing);
    assertThat(savedLinks.get(0).getParent()).isEqualTo(parentByGuid);
    assertThat(savedLinks.get(1).getProvider()).isEqualTo(existing);
    assertThat(savedLinks.get(1).getParent()).isEqualTo(parentByFirmNumber);
    verify(providerParentLinkRepository).deleteAll(any());
  }

  @Test
  void patchProvider_ignoresBarristerFieldsForAdvocatePractitioner() {
    UUID guid = UUID.randomUUID();

    AdvocatePractitionerEntity existing =
        AdvocatePractitionerEntity.builder().firmNumber("100003").name("Old Name").build();
    existing.setGuid(guid);

    when(providerRepository.findById(guid)).thenReturn(Optional.of(existing));
    when(providerRepository.save(any(ProviderEntity.class))).thenAnswer(inv -> inv.getArgument(0));

    ProviderPatchV2 patch =
        new ProviderPatchV2()
            .practitioner(new PractitionerDetailsPatchV2().barCouncilRollNumber("BAR-123"));

    ProviderCreationResult result = service.patchProvider(guid.toString(), patch);

    assertThat(result.providerFirmGUID()).isEqualTo(guid);
    assertThat(result.firmNumber()).isEqualTo("100003");
  }

  @Test
  void getLspHeadOffice_returnsLinkWhenPresent() {
    ProviderEntity provider = ProviderEntity.builder().name("My LSP").build();
    provider.setGuid(UUID.randomUUID());

    LspProviderOfficeLinkEntity link = new LspProviderOfficeLinkEntity();
    link.setHeadOfficeFlag(Boolean.TRUE);
    link.setOffice(new OfficeEntity());
    when(lspProviderOfficeLinkRepository.findByProviderAndHeadOfficeFlagTrue(provider))
        .thenReturn(Optional.of(link));

    assertThat(service.getLspHeadOffice(provider)).contains(link);
  }

  @Test
  void getLspHeadOffice_returnsEmptyWhenAbsent() {
    ProviderEntity provider = ProviderEntity.builder().name("My Chambers").build();
    provider.setGuid(UUID.randomUUID());
    when(lspProviderOfficeLinkRepository.findByProviderAndHeadOfficeFlagTrue(provider))
        .thenReturn(Optional.empty());

    assertThat(service.getLspHeadOffice(provider)).isEmpty();
  }

  @Test
  void getChambersHeadOffice_returnsLinkWhenPresent() {
    ProviderEntity provider = ProviderEntity.builder().name("My Chambers").build();
    provider.setGuid(UUID.randomUUID());

    ChamberProviderOfficeLinkEntity link = new ChamberProviderOfficeLinkEntity();
    link.setHeadOfficeFlag(Boolean.TRUE);
    link.setOffice(new OfficeEntity());
    when(chamberProviderOfficeLinkRepository.findByProviderAndHeadOfficeFlagTrue(provider))
        .thenReturn(Optional.of(link));

    assertThat(service.getChambersHeadOffice(provider)).contains(link);
  }

  @Test
  void getAdvocateOfficeLink_returnsLinkWhenPresent() {
    ProviderEntity provider =
        ProviderEntity.builder().firmType(FirmType.ADVOCATE).name("J. Smith").build();
    provider.setGuid(UUID.randomUUID());

    AdvocateProviderOfficeLinkEntity link = new AdvocateProviderOfficeLinkEntity();
    link.setHeadOfficeFlag(Boolean.TRUE);
    when(advocateProviderOfficeLinkRepository.findByProviderAndHeadOfficeFlagTrue(provider))
        .thenReturn(Optional.of(link));

    assertThat(service.getAdvocateOfficeLink(provider)).contains(link);
  }

  @Test
  void getParentLinks_returnsLinksForAdvocate() {
    ProviderEntity advocate =
        ProviderEntity.builder().firmType(FirmType.ADVOCATE).name("A.B.").build();
    advocate.setGuid(UUID.randomUUID());

    ProviderEntity parent = ProviderEntity.builder().firmType(FirmType.CHAMBERS).name("CH").build();
    parent.setGuid(UUID.randomUUID());

    ProviderParentLinkEntity parentLink =
        ProviderParentLinkEntity.builder().provider(advocate).parent(parent).build();
    when(providerParentLinkRepository.findByProvider(advocate)).thenReturn(List.of(parentLink));

    List<ProviderParentLinkEntity> result = service.getParentLinks(advocate);
    assertThat(result).hasSize(1);
    assertThat(result.getFirst().getParent().getName()).isEqualTo("CH");
  }

  @Test
  void getParentLinks_returnsEmptyForNonAdvocate() {
    ProviderEntity provider = ProviderEntity.builder().name("My LSP").build();
    provider.setGuid(UUID.randomUUID());
    when(providerParentLinkRepository.findByProvider(provider)).thenReturn(List.of());

    assertThat(service.getParentLinks(provider)).isEmpty();
  }

  // ============================================================
  // ✅ NEW TESTS FOR searchProviders (added at end)
  // ============================================================

  @Test
  void searchProviders_withFilters_returnsPagedResult() {
    Pageable pageable = PageRequest.of(0, 10);

    ProviderEntity provider =
        ProviderEntity.builder().firmNumber("100001").name("Test Provider").build();

    Page<ProviderEntity> page = new PageImpl<>(List.of(provider));

    //noinspection unchecked
    when(providerFirmRepository.findAll(
            any(org.springframework.data.jpa.domain.Specification.class), eq(pageable)))
        .thenReturn(page);

    Page<ProviderEntity> result =
        service.searchProviders(
            List.of(UUID.randomUUID().toString()),
            List.of("100001"),
            "Test",
            null,
            List.of(ProviderFirmTypeV2.ADVOCATE),
            pageable);

    assertThat(result.getContent()).hasSize(1);
    assertThat(result.getContent().getFirst().getName()).isEqualTo("Test Provider");
  }

  @Test
  void searchProviders_withNoFilters_returnsAllPagedResults() {
    Pageable pageable = PageRequest.of(0, 5);

    Page<ProviderEntity> emptyPage = Page.empty();

    //noinspection unchecked
    when(providerFirmRepository.findAll(
            any(org.springframework.data.jpa.domain.Specification.class), eq(pageable)))
        .thenReturn(emptyPage);

    Page<ProviderEntity> result = service.searchProviders(null, null, null, null, null, pageable);

    assertThat(result).isEmpty();
  }

  @Test
  void getPractitionersByChambers_returnsPractitionersForChambers() {
    String chambersId = UUID.randomUUID().toString();
    ProviderEntity chambers =
        ProviderEntity.builder()
            .guid(UUID.fromString(chambersId))
            .firmType(FirmType.CHAMBERS)
            .build();
    when(providerRepository.findById(UUID.fromString(chambersId)))
        .thenReturn(Optional.of(chambers));

    ProviderEntity practitioner = ProviderEntity.builder().name("Practitioner").build();
    ProviderParentLinkEntity link =
        ProviderParentLinkEntity.builder().provider(practitioner).build();
    PageRequest pageable = PageRequest.of(0, 20);
    when(providerParentLinkRepository.findByParentOrderByProviderNameAsc(chambers, pageable))
        .thenReturn(new PageImpl<>(List.of(link), pageable, 1));

    Page<ProviderParentLinkEntity> result =
        service.getPractitionersByChambers(chambersId, pageable);

    assertThat(result.getContent()).hasSize(1);
    assertThat(result.getContent().getFirst().getProvider().getName()).isEqualTo("Practitioner");
  }

  @Test
  void getPractitionersByChambers_notChambers_throwsIllegalArgumentException() {
    String lspId = UUID.randomUUID().toString();
    ProviderEntity lsp =
        ProviderEntity.builder()
            .guid(UUID.fromString(lspId))
            .firmType(FirmType.LEGAL_SERVICES_PROVIDER)
            .build();
    when(providerRepository.findById(UUID.fromString(lspId))).thenReturn(Optional.of(lsp));

    assertThatThrownBy(() -> service.getPractitionersByChambers(lspId, PageRequest.of(0, 20)))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Provider is not a Chambers");
  }

  // ============================================================
  // Tests for DSTEW-1647: Practitioner LM re-linking (UC5 option
  // 1/2/3)
  // ============================================================

  @Test
  void patchProvider_practitionerLiaisonManager_option1_linkChambers() {
    UUID advocateGuid = UUID.randomUUID();
    UUID advocateOfficeGuid = UUID.randomUUID();
    UUID chambersGuid = UUID.randomUUID();
    UUID chambersOfficeGuid = UUID.randomUUID();
    UUID chamberslmGuid = UUID.randomUUID();

    AdvocatePractitionerEntity advocate =
        AdvocatePractitionerEntity.builder().firmNumber("300001").name("Advocate Smith").build();
    advocate.setGuid(advocateGuid);

    final AdvocateProviderOfficeLinkEntity advocateOfficeLink =
        AdvocateProviderOfficeLinkEntity.builder().guid(advocateOfficeGuid).build();

    ChamberProviderEntity chambers =
        ChamberProviderEntity.builder().firmNumber("200001").name("Chambers X").build();
    chambers.setGuid(chambersGuid);

    final ProviderOfficeLinkEntity chambersOfficeLink =
        ProviderOfficeLinkEntity.builder().guid(chambersOfficeGuid).build();

    LiaisonManagerEntity chambersLm = new LiaisonManagerEntity();
    chambersLm.setGuid(chamberslmGuid);
    chambersLm.setFirstName("Jane");
    chambersLm.setLastName("Doe");

    ProviderParentLinkEntity parentLink =
        ProviderParentLinkEntity.builder().provider(advocate).parent(chambers).build();

    when(providerRepository.findById(advocateGuid)).thenReturn(Optional.of(advocate));
    when(advocateProviderOfficeLinkRepository.findByProviderAndHeadOfficeFlagTrue(advocate))
        .thenReturn(Optional.of(advocateOfficeLink));
    when(providerParentLinkRepository.findByProvider(advocate)).thenReturn(List.of(parentLink));
    when(providerOfficeLinkRepository.findByProviderAndHeadOfficeFlagTrue(chambers))
        .thenReturn(Optional.of(chambersOfficeLink));
    when(officeLiaisonManagerLinkRepository.findByOfficeLinkAndActiveDateToIsNull(
            chambersOfficeLink))
        .thenReturn(
            List.of(
                new OfficeLiaisonManagerLinkEntity() {
                  {
                    setLiaisonManager(chambersLm);
                    setActiveDateTo(null);
                  }
                })); // Chambers has active LM
    when(officeLiaisonManagerLinkRepository.findByOfficeLink_Guid(advocateOfficeLink.getGuid()))
        .thenReturn(List.of()); // No previous links to end-date
    when(providerRepository.save(any(ProviderEntity.class))).thenAnswer(inv -> inv.getArgument(0));
    when(officeLiaisonManagerLinkRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    LiaisonManagerLinkChambersV2 linkRequest = new LiaisonManagerLinkChambersV2();
    linkRequest.setUseChambersLiaisonManager(true);

    AdvocateOfficeLiaisonManagerCreateOrLinkV2 lmRequest = linkRequest;
    PractitionerDetailsPatchV2 practitionerPatch =
        new PractitionerDetailsPatchV2().liaisonManager(lmRequest);
    ProviderPatchV2 patch = new ProviderPatchV2().practitioner(practitionerPatch);

    ProviderCreationResult result = service.patchProvider(advocateGuid.toString(), patch);

    assertThat(result.providerFirmGUID()).isEqualTo(advocateGuid);
    assertThat(result.firmNumber()).isEqualTo("300001");

    ArgumentCaptor<OfficeLiaisonManagerLinkEntity> linkCaptor =
        ArgumentCaptor.forClass(OfficeLiaisonManagerLinkEntity.class);
    verify(officeLiaisonManagerLinkRepository).save(linkCaptor.capture());
    OfficeLiaisonManagerLinkEntity savedLink = linkCaptor.getValue();
    assertThat(savedLink.getLinkedFlag()).isTrue();
    assertThat(savedLink.getLiaisonManager().getGuid()).isEqualTo(chamberslmGuid);
  }

  @Test
  void patchProvider_practitionerLiaisonManager_option3_createNew() {
    UUID advocateGuid = UUID.randomUUID();
    UUID advocateOfficeGuid = UUID.randomUUID();

    AdvocatePractitionerEntity advocate =
        AdvocatePractitionerEntity.builder().firmNumber("300001").name("Advocate Smith").build();
    advocate.setGuid(advocateGuid);

    AdvocateProviderOfficeLinkEntity advocateOfficeLink =
        AdvocateProviderOfficeLinkEntity.builder().guid(advocateOfficeGuid).build();

    when(providerRepository.findById(advocateGuid)).thenReturn(Optional.of(advocate));
    when(advocateProviderOfficeLinkRepository.findByProviderAndHeadOfficeFlagTrue(advocate))
        .thenReturn(Optional.of(advocateOfficeLink));
    when(officeLiaisonManagerLinkRepository.findByOfficeLinkAndActiveDateToIsNull(
            advocateOfficeLink))
        .thenReturn(List.of()); // No existing active LM

    LiaisonManagerEntity newLm = new LiaisonManagerEntity();
    newLm.setGuid(UUID.randomUUID());
    newLm.setFirstName("John");
    newLm.setLastName("Lawyer");

    when(liaisonManagerRepository.save(any(LiaisonManagerEntity.class)))
        .thenAnswer(inv -> inv.getArgument(0));
    when(officeLiaisonManagerLinkRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    when(providerRepository.save(any(ProviderEntity.class))).thenAnswer(inv -> inv.getArgument(0));

    LiaisonManagerCreateV2 createRequest = new LiaisonManagerCreateV2();
    createRequest.setFirstName("John");
    createRequest.setLastName("Lawyer");
    createRequest.setEmailAddress("john@example.com");
    createRequest.setTelephoneNumber("0123456789");

    PractitionerDetailsPatchV2 practitionerPatch =
        new PractitionerDetailsPatchV2().liaisonManager(createRequest);
    ProviderPatchV2 patch = new ProviderPatchV2().practitioner(practitionerPatch);

    ProviderCreationResult result = service.patchProvider(advocateGuid.toString(), patch);

    assertThat(result.providerFirmGUID()).isEqualTo(advocateGuid);
    assertThat(result.firmNumber()).isEqualTo("300001");

    ArgumentCaptor<OfficeLiaisonManagerLinkEntity> linkCaptor =
        ArgumentCaptor.forClass(OfficeLiaisonManagerLinkEntity.class);
    verify(officeLiaisonManagerLinkRepository).save(linkCaptor.capture());
    OfficeLiaisonManagerLinkEntity savedLink = linkCaptor.getValue();
    assertThat(savedLink.getLinkedFlag()).isFalse();
    assertThat(savedLink.getActiveDateFrom()).isEqualTo(LocalDate.now());
    assertThat(savedLink.getActiveDateTo()).isNull();
  }

  @Test
  void patchProvider_practitionerLiaisonManager_option3_rejectsWhenActiveLmExists() {
    UUID advocateGuid = UUID.randomUUID();
    UUID advocateOfficeGuid = UUID.randomUUID();

    AdvocatePractitionerEntity advocate =
        AdvocatePractitionerEntity.builder().firmNumber("300001").name("Advocate Smith").build();
    advocate.setGuid(advocateGuid);

    AdvocateProviderOfficeLinkEntity advocateOfficeLink =
        AdvocateProviderOfficeLinkEntity.builder().guid(advocateOfficeGuid).build();

    LiaisonManagerEntity existingLm = new LiaisonManagerEntity();
    existingLm.setGuid(UUID.randomUUID());

    OfficeLiaisonManagerLinkEntity existingLink =
        OfficeLiaisonManagerLinkEntity.builder()
            .officeLink(advocateOfficeLink)
            .liaisonManager(existingLm)
            .activeDateTo(null)
            .build();

    when(providerRepository.findById(advocateGuid)).thenReturn(Optional.of(advocate));
    when(advocateProviderOfficeLinkRepository.findByProviderAndHeadOfficeFlagTrue(advocate))
        .thenReturn(Optional.of(advocateOfficeLink));
    when(officeLiaisonManagerLinkRepository.findByOfficeLinkAndActiveDateToIsNull(
            advocateOfficeLink))
        .thenReturn(List.of(existingLink)); // Office already has active LM

    LiaisonManagerCreateV2 createRequest = new LiaisonManagerCreateV2();
    createRequest.setFirstName("John");
    createRequest.setLastName("Lawyer");
    createRequest.setEmailAddress("john@example.com");
    createRequest.setTelephoneNumber("0123456789");

    PractitionerDetailsPatchV2 practitionerPatch =
        new PractitionerDetailsPatchV2().liaisonManager(createRequest);
    ProviderPatchV2 patch = new ProviderPatchV2().practitioner(practitionerPatch);

    assertThatThrownBy(() -> service.patchProvider(advocateGuid.toString(), patch))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Office already has an active liaison manager");
  }
}
