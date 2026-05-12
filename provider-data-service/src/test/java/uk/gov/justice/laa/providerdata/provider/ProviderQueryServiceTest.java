package uk.gov.justice.laa.providerdata.provider;

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
import uk.gov.justice.laa.providerdata.model.ProviderFirmTypeV2;
import uk.gov.justice.laa.providerdata.provider.repository.ProviderFirmRepository;
import uk.gov.justice.laa.providerdata.provider.repository.ProviderParentLinkRepository;
import uk.gov.justice.laa.providerdata.provider.repository.ProviderRepository;
import uk.gov.justice.laa.providerdata.shared.FirmType;
import uk.gov.justice.laa.providerdata.shared.ItemNotFoundException;

@ExtendWith(MockitoExtension.class)
class ProviderQueryServiceTest {

  @Mock private ProviderRepository providerRepository;
  @Mock private ProviderParentLinkRepository providerParentLinkRepository;
  @Mock private ProviderFirmRepository providerFirmRepository;

  @InjectMocks private ProviderQueryService service;

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
}
