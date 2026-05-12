package uk.gov.justice.laa.providerdata.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
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
import uk.gov.justice.laa.providerdata.entity.FirmType;
import uk.gov.justice.laa.providerdata.entity.LspProviderEntity;
import uk.gov.justice.laa.providerdata.entity.ProviderEntity;
import uk.gov.justice.laa.providerdata.exception.ItemNotFoundException;
import uk.gov.justice.laa.providerdata.mapper.ProviderMapper;
import uk.gov.justice.laa.providerdata.model.ProviderFirmTypeV2;
import uk.gov.justice.laa.providerdata.model.ProviderV2;
import uk.gov.justice.laa.providerdata.projection.ProviderFirmReadModel;
import uk.gov.justice.laa.providerdata.projection.ProviderFirmReadStore;

/**
 * Unit tests for {@link ProviderFirmQueryService}.
 *
 * <p>Covers cache-hit, cache-miss, cache-fallback, and list-search paths.
 */
@ExtendWith(MockitoExtension.class)
class ProviderFirmQueryServiceTest {

  @Mock private ProviderService providerService;
  @Mock private ProviderMapper providerMapper;
  @Mock private ProviderFirmReadStore readStore;

  @InjectMocks private ProviderFirmQueryService queryService;

  // ---- getProviderFirm — cache hit ----

  @Test
  void getProviderFirm_byGuid_cacheHit_returnsCachedModel() {
    UUID guid = UUID.randomUUID();
    ProviderFirmReadModel cached =
        new ProviderFirmReadModel(
            guid, "LSP-0001", "Cached Firm", FirmType.LEGAL_SERVICES_PROVIDER);
    when(readStore.findByGuid(guid)).thenReturn(Optional.of(cached));

    ProviderV2 result = queryService.getProviderFirm(guid.toString());

    assertThat(result.getGuid()).isEqualTo(guid);
    assertThat(result.getFirmNumber()).isEqualTo("LSP-0001");
    assertThat(result.getName()).isEqualTo("Cached Firm");
    assertThat(result.getFirmType()).isEqualTo(ProviderFirmTypeV2.LEGAL_SERVICES_PROVIDER);
    verify(providerService, never()).getProvider(any());
  }

  @Test
  void getProviderFirm_byFirmNumber_cacheHit_returnsCachedModel() {
    UUID guid = UUID.randomUUID();
    ProviderFirmReadModel cached =
        new ProviderFirmReadModel(guid, "CH-0001", "Chambers Cached", FirmType.CHAMBERS);
    when(readStore.findByFirmNumber("CH-0001")).thenReturn(Optional.of(cached));

    ProviderV2 result = queryService.getProviderFirm("CH-0001");

    assertThat(result.getFirmNumber()).isEqualTo("CH-0001");
    assertThat(result.getFirmType()).isEqualTo(ProviderFirmTypeV2.CHAMBERS);
    verify(providerService, never()).getProvider(any());
  }

  // ---- getProviderFirm — cache miss ----

  @Test
  void getProviderFirm_cacheMiss_loadsFromDatabase_andWarmsCache() {
    UUID guid = UUID.randomUUID();
    when(readStore.findByGuid(guid)).thenReturn(Optional.empty());

    LspProviderEntity entity = LspProviderEntity.builder().name("DB Firm").build();
    entity.setGuid(guid);
    entity.setFirmNumber("LSP-0001");
    when(providerService.getProvider(guid.toString())).thenReturn(entity);

    ProviderV2 expected =
        new ProviderV2()
            .guid(guid)
            .firmNumber("LSP-0001")
            .firmType(ProviderFirmTypeV2.LEGAL_SERVICES_PROVIDER)
            .name("DB Firm");
    when(providerMapper.toProviderV2(any(), any(), any(), any(), any())).thenReturn(expected);
    when(providerService.getLspHeadOffice(entity)).thenReturn(Optional.empty());
    when(providerService.getChambersHeadOffice(entity)).thenReturn(Optional.empty());
    when(providerService.getAdvocateOfficeLink(entity)).thenReturn(Optional.empty());
    when(providerService.getParentLinks(entity)).thenReturn(List.of());

    ProviderV2 result = queryService.getProviderFirm(guid.toString());

    assertThat(result.getName()).isEqualTo("DB Firm");
    verify(readStore).save(any(ProviderFirmReadModel.class));
  }

  @Test
  void getProviderFirm_notFound_throwsItemNotFoundException() {
    UUID guid = UUID.randomUUID();
    when(readStore.findByGuid(guid)).thenReturn(Optional.empty());
    when(providerService.getProvider(guid.toString()))
        .thenThrow(new ItemNotFoundException("Provider not found: " + guid));

    assertThatThrownBy(() -> queryService.getProviderFirm(guid.toString()))
        .isInstanceOf(ItemNotFoundException.class)
        .hasMessageContaining(guid.toString());
  }

  // ---- getProviderFirm — Redis failure resilience ----

  @Test
  void getProviderFirm_redisReadFails_fallsBackToDatabase() {
    UUID guid = UUID.randomUUID();
    when(readStore.findByGuid(guid)).thenThrow(new RuntimeException("Redis unavailable"));

    LspProviderEntity entity = LspProviderEntity.builder().name("Fallback Firm").build();
    entity.setGuid(guid);
    entity.setFirmNumber("LSP-0002");
    when(providerService.getProvider(guid.toString())).thenReturn(entity);

    ProviderV2 expected = new ProviderV2().guid(guid).firmNumber("LSP-0002").name("Fallback Firm");
    when(providerMapper.toProviderV2(any(), any(), any(), any(), any())).thenReturn(expected);
    when(providerService.getLspHeadOffice(entity)).thenReturn(Optional.empty());
    when(providerService.getChambersHeadOffice(entity)).thenReturn(Optional.empty());
    when(providerService.getAdvocateOfficeLink(entity)).thenReturn(Optional.empty());
    when(providerService.getParentLinks(entity)).thenReturn(List.of());

    ProviderV2 result = queryService.getProviderFirm(guid.toString());

    assertThat(result.getName()).isEqualTo("Fallback Firm");
  }

  @Test
  void getProviderFirm_cacheMissButCacheWarmFails_stillReturnsResult() {
    UUID guid = UUID.randomUUID();
    when(readStore.findByGuid(guid)).thenReturn(Optional.empty());

    LspProviderEntity entity = LspProviderEntity.builder().name("Firm").build();
    entity.setGuid(guid);
    entity.setFirmNumber("LSP-0003");
    when(providerService.getProvider(guid.toString())).thenReturn(entity);

    doThrow(new RuntimeException("Redis write failed")).when(readStore).save(any());

    ProviderV2 expected = new ProviderV2().guid(guid).firmNumber("LSP-0003").name("Firm");
    when(providerMapper.toProviderV2(any(), any(), any(), any(), any())).thenReturn(expected);
    when(providerService.getLspHeadOffice(entity)).thenReturn(Optional.empty());
    when(providerService.getChambersHeadOffice(entity)).thenReturn(Optional.empty());
    when(providerService.getAdvocateOfficeLink(entity)).thenReturn(Optional.empty());
    when(providerService.getParentLinks(entity)).thenReturn(List.of());

    ProviderV2 result = queryService.getProviderFirm(guid.toString());

    assertThat(result.getName()).isEqualTo("Firm");
  }

  // ---- searchProviderFirms — always hits database ----

  @Test
  void searchProviderFirms_alwaysQueriesDatabase() {
    UUID guid = UUID.randomUUID();
    ProviderEntity entity = LspProviderEntity.builder().name("LSP").build();
    entity.setGuid(guid);
    entity.setFirmNumber("LSP-0001");

    Page<ProviderEntity> dbPage = new PageImpl<>(List.of(entity), PageRequest.of(0, 20), 1);
    when(providerService.searchProviders(any(), any(), any(), any(), any(), any()))
        .thenReturn(dbPage);

    ProviderV2 mapped =
        new ProviderV2()
            .guid(guid)
            .firmNumber("LSP-0001")
            .firmType(ProviderFirmTypeV2.LEGAL_SERVICES_PROVIDER);
    when(providerMapper.toProviderV2(any(), any(), any(), any(), any())).thenReturn(mapped);
    when(providerService.getLspHeadOffice(entity)).thenReturn(Optional.empty());
    when(providerService.getChambersHeadOffice(entity)).thenReturn(Optional.empty());
    when(providerService.getAdvocateOfficeLink(entity)).thenReturn(Optional.empty());
    when(providerService.getParentLinks(entity)).thenReturn(List.of());

    Page<ProviderV2> result =
        queryService.searchProviderFirms(null, null, null, null, PageRequest.of(0, 20));

    assertThat(result.getContent()).hasSize(1);
    assertThat(result.getContent().get(0).getFirmNumber()).isEqualTo("LSP-0001");
    verify(readStore, never()).findByGuid(any());
    verify(providerService).searchProviders(any(), any(), any(), any(), any(), any());
  }

  @Test
  void searchProviderFirms_emptyResult_returnsEmptyPage() {
    when(providerService.searchProviders(any(), any(), any(), any(), any(), any()))
        .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

    Page<ProviderV2> result =
        queryService.searchProviderFirms(null, null, null, null, PageRequest.of(0, 20));

    assertThat(result.getContent()).isEmpty();
    assertThat(result.getTotalElements()).isZero();
  }

  // ---- null firmType in projection ----

  @Test
  void getProviderFirm_cacheHit_nullFirmType_returnsNullFirmType() {
    UUID guid = UUID.randomUUID();
    ProviderFirmReadModel cached = new ProviderFirmReadModel(guid, "X-001", "No Type Firm", null);
    when(readStore.findByGuid(guid)).thenReturn(Optional.of(cached));

    ProviderV2 result = queryService.getProviderFirm(guid.toString());

    assertThat(result.getFirmType()).isNull();
  }
}
