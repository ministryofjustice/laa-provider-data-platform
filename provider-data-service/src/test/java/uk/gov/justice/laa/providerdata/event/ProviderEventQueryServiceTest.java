package uk.gov.justice.laa.providerdata.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
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
import org.springframework.data.domain.Pageable;
import uk.gov.justice.laa.providerdata.entity.ProviderEventEntity;
import uk.gov.justice.laa.providerdata.exception.ItemNotFoundException;
import uk.gov.justice.laa.providerdata.model.EventTypeV2;
import uk.gov.justice.laa.providerdata.repository.ProviderEventRepository;

@ExtendWith(MockitoExtension.class)
class ProviderEventQueryServiceTest {

  @Mock private ProviderEventRepository providerEventRepository;

  @InjectMocks private ProviderEventQueryService providerEventQueryService;

  @Test
  void getEvents_noFilters_callsFindAll() {
    Pageable pageable = Pageable.unpaged();
    when(providerEventRepository.findAll(pageable)).thenReturn(Page.empty());

    providerEventQueryService.getEvents(null, null, pageable);

    verify(providerEventRepository).findAll(pageable);
    verifyNoMoreInteractions(providerEventRepository);
  }

  @Test
  void getEvents_typesOnly_callsFindByEventTypeIn() {
    Pageable pageable = Pageable.unpaged();
    List<EventTypeV2> types = List.of(EventTypeV2.PROVIDER_FIRM_CHANGED_SNAPSHOT_EVENT);
    when(providerEventRepository.findByEventTypeIn(any(), any())).thenReturn(Page.empty());

    providerEventQueryService.getEvents(types, null, pageable);

    verify(providerEventRepository)
        .findByEventTypeIn(List.of("ProviderFirmChangedSnapshotEvent"), pageable);
  }

  @Test
  void getEvents_correlationOnly_callsFindByCorrelationId() {
    Pageable pageable = Pageable.unpaged();
    when(providerEventRepository.findByCorrelationId("corr-123", pageable))
        .thenReturn(Page.empty());

    providerEventQueryService.getEvents(null, "corr-123", pageable);

    verify(providerEventRepository).findByCorrelationId("corr-123", pageable);
  }

  @Test
  void getEvents_bothFilters_callsFindByEventTypeInAndCorrelationId() {
    Pageable pageable = Pageable.unpaged();
    List<EventTypeV2> types = List.of(EventTypeV2.PROVIDER_FIRM_CHANGED_SNAPSHOT_EVENT);
    when(providerEventRepository.findByEventTypeInAndCorrelationId(any(), any(), any()))
        .thenReturn(Page.empty());

    providerEventQueryService.getEvents(types, "corr-123", pageable);

    verify(providerEventRepository)
        .findByEventTypeInAndCorrelationId(
            List.of("ProviderFirmChangedSnapshotEvent"), "corr-123", pageable);
  }

  @Test
  void getEvent_found_returnsEntity() {
    UUID guid = UUID.randomUUID();
    ProviderEventEntity entity = ProviderEventEntity.builder().guid(guid).build();
    when(providerEventRepository.findById(guid)).thenReturn(Optional.of(entity));

    ProviderEventEntity result = providerEventQueryService.getEvent(guid.toString());

    assertThat(result).isEqualTo(entity);
  }

  @Test
  void getEvent_invalidUuid_throwsIllegalArgumentException() {
    assertThatThrownBy(() -> providerEventQueryService.getEvent("not-a-uuid"))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void getEvent_notFound_throwsItemNotFoundException() {
    when(providerEventRepository.findById(any())).thenReturn(Optional.empty());

    assertThatThrownBy(() -> providerEventQueryService.getEvent(UUID.randomUUID().toString()))
        .isInstanceOf(ItemNotFoundException.class);
  }
}
