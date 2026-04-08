package uk.gov.justice.laa.providerdata.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import uk.gov.justice.laa.providerdata.entity.AdvocatePractitionerEntity;
import uk.gov.justice.laa.providerdata.entity.AdvocateProviderOfficeLinkEntity;
import uk.gov.justice.laa.providerdata.entity.ChamberProviderOfficeLinkEntity;
import uk.gov.justice.laa.providerdata.entity.FirmType;
import uk.gov.justice.laa.providerdata.entity.LspProviderEntity;
import uk.gov.justice.laa.providerdata.entity.LspProviderOfficeLinkEntity;
import uk.gov.justice.laa.providerdata.entity.OfficeEntity;
import uk.gov.justice.laa.providerdata.entity.ProviderEntity;
import uk.gov.justice.laa.providerdata.entity.ProviderParentLinkEntity;
import uk.gov.justice.laa.providerdata.exception.ItemNotFoundException;
import uk.gov.justice.laa.providerdata.model.LSPDetailsConstitutionalStatusV2;
import uk.gov.justice.laa.providerdata.model.LSPDetailsPatchV2;
import uk.gov.justice.laa.providerdata.model.PractitionerDetailsAdvocateLevelV2;
import uk.gov.justice.laa.providerdata.model.PractitionerDetailsPatchV2;
import uk.gov.justice.laa.providerdata.model.ProviderFirmTypeV2;
import uk.gov.justice.laa.providerdata.model.ProviderPatchV2;
import uk.gov.justice.laa.providerdata.repository.AdvocateProviderOfficeLinkRepository;
import uk.gov.justice.laa.providerdata.repository.ChamberProviderOfficeLinkRepository;
import uk.gov.justice.laa.providerdata.repository.LspProviderOfficeLinkRepository;
import uk.gov.justice.laa.providerdata.repository.ProviderFirmRepository;
import uk.gov.justice.laa.providerdata.repository.ProviderParentLinkRepository;
import uk.gov.justice.laa.providerdata.repository.ProviderRepository;

@ExtendWith(MockitoExtension.class)
class ProviderServiceTest {

  @Mock private ProviderRepository providerRepository;
  @Mock private LspProviderOfficeLinkRepository lspProviderOfficeLinkRepository;
  @Mock private ChamberProviderOfficeLinkRepository chamberProviderOfficeLinkRepository;
  @Mock private AdvocateProviderOfficeLinkRepository advocateProviderOfficeLinkRepository;
  @Mock private ProviderParentLinkRepository providerParentLinkRepository;

  // ✅ NEW MOCK (minimal addition)
  @Mock private ProviderFirmRepository providerFirmRepository;

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
    ProviderEntity entity =
        ProviderEntity.builder().firmNumber("LSP-ABC123").name("My LSP").build();
    when(providerRepository.findByFirmNumber("LSP-ABC123")).thenReturn(Optional.of(entity));

    ProviderEntity result = service.getProvider("LSP-ABC123");

    assertThat(result.getFirmNumber()).isEqualTo("LSP-ABC123");
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
        LspProviderEntity.builder().firmNumber("LSP-0001").name("Old Name").build();
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
    assertThat(result.firmNumber()).isEqualTo("LSP-0001");
  }

  @Test
  void patchProvider_whenProviderNotLsp_rejectsLspPatch() {
    UUID guid = UUID.randomUUID();

    ProviderEntity existing =
        ProviderEntity.builder()
            .firmType(FirmType.CHAMBERS)
            .firmNumber("CH-0001")
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
  void patchProvider_headOfficeReassignmentRejected() {
    UUID guid = UUID.randomUUID();

    LspProviderEntity existing =
        LspProviderEntity.builder().firmNumber("LSP-0001").name("Old Name").build();
    existing.setGuid(guid);

    when(providerRepository.findById(guid)).thenReturn(Optional.of(existing));

    ProviderPatchV2 patch =
        new ProviderPatchV2()
            .legalServicesProvider(
                new LSPDetailsPatchV2()
                    .headOffice(
                        new uk.gov.justice.laa.providerdata.model.LSPHeadOfficeDetailsPatchV2()));

    assertThatThrownBy(() -> service.patchProvider(guid.toString(), patch))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Head office reassignment is not supported");
  }

  @Test
  void patchProvider_updatesAdvocatePractitionerFields() {
    UUID guid = UUID.randomUUID();

    AdvocatePractitionerEntity existing =
        AdvocatePractitionerEntity.builder().firmNumber("ADV-0001").name("Old Name").build();
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
    assertThat(result.firmNumber()).isEqualTo("ADV-0001");
  }

  @Test
  void patchProvider_rejectsBarristerFieldsForAdvocatePractitioner() {
    UUID guid = UUID.randomUUID();

    AdvocatePractitionerEntity existing =
        AdvocatePractitionerEntity.builder().firmNumber("ADV-0001").name("Old Name").build();
    existing.setGuid(guid);

    when(providerRepository.findById(guid)).thenReturn(Optional.of(existing));

    ProviderPatchV2 patch =
        new ProviderPatchV2()
            .practitioner(new PractitionerDetailsPatchV2().barCouncilRollNumber("BAR-123"));

    assertThatThrownBy(() -> service.patchProvider(guid.toString(), patch))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Barrister fields are only valid");
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
        ProviderEntity.builder().firmNumber("FRM001").name("Test Provider").build();

    Page<ProviderEntity> page = new PageImpl<>(List.of(provider));

    //noinspection unchecked
    when(providerFirmRepository.findAll(
            any(org.springframework.data.jpa.domain.Specification.class), eq(pageable)))
        .thenReturn(page);

    Page<ProviderEntity> result =
        service.searchProviders(
            List.of(UUID.randomUUID().toString()),
            List.of("FRM001"),
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
}
