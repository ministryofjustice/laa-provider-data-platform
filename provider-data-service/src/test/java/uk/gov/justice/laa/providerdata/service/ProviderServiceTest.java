package uk.gov.justice.laa.providerdata.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

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
import uk.gov.justice.laa.providerdata.entity.AdvocateProviderOfficeLinkEntity;
import uk.gov.justice.laa.providerdata.entity.ChamberProviderOfficeLinkEntity;
import uk.gov.justice.laa.providerdata.entity.FirmType;
import uk.gov.justice.laa.providerdata.entity.LspProviderOfficeLinkEntity;
import uk.gov.justice.laa.providerdata.entity.OfficeEntity;
import uk.gov.justice.laa.providerdata.entity.ProviderEntity;
import uk.gov.justice.laa.providerdata.entity.ProviderParentLinkEntity;
import uk.gov.justice.laa.providerdata.exception.ItemNotFoundException;
import uk.gov.justice.laa.providerdata.model.ProviderFirmTypeV2;
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
    when(providerParentLinkRepository.findByParentOrderByProviderNameAsc(chambers))
        .thenReturn(List.of(link));

    List<ProviderParentLinkEntity> result = service.getPractitionersByChambers(chambersId);

    assertThat(result).hasSize(1);
    assertThat(result.get(0).getProvider().getName()).isEqualTo("Practitioner");
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

    assertThatThrownBy(() -> service.getPractitionersByChambers(lspId))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Provider is not a Chambers");
  }
}
