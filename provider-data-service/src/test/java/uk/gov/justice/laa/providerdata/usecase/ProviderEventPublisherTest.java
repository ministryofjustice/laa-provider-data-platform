package uk.gov.justice.laa.providerdata.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import tools.jackson.databind.json.JsonMapper;
import uk.gov.justice.laa.providerdata.model.ProviderFirmChangedSnapshotEventV2Payload;
import uk.gov.justice.laa.providerdata.provider.ProviderEntity;
import uk.gov.justice.laa.providerdata.usecase.repository.ProviderEventRepository;

@ExtendWith(MockitoExtension.class)
class ProviderEventPublisherTest {

  @Mock private ProviderSnapshotAssembler snapshotAssembler;
  @Mock private ProviderEventRepository providerEventRepository;
  @Mock private ApplicationEventPublisher applicationEventPublisher;
  @Mock private JsonMapper objectMapper;

  @InjectMocks private ProviderEventPublisher publisher;

  @Test
  void publishAfterWrite_savesEntityWithCorrectFields() {
    ProviderEntity provider = ProviderEntity.builder().build();
    provider.setLastUpdatedBy("test-user");
    provider.setGuid(UUID.randomUUID());

    EventContext context = EventContext.of("corr-123", "trace-456");

    when(snapshotAssembler.assemble(provider))
        .thenReturn(new ProviderFirmChangedSnapshotEventV2Payload());
    when(objectMapper.writeValueAsString(any())).thenReturn("{}");

    publisher.publishAfterWrite(provider, context);

    ArgumentCaptor<ProviderEventEntity> captor = ArgumentCaptor.forClass(ProviderEventEntity.class);
    verify(providerEventRepository).save(captor.capture());
    ProviderEventEntity entity = captor.getValue();

    assertThat(entity.getEventType()).isEqualTo("ProviderFirmChangedSnapshotEvent");
    assertThat(entity.getEventSource()).isEqualTo("apiV2");
    assertThat(entity.getCorrelationId()).isEqualTo("corr-123");
    assertThat(entity.getTraceId()).isEqualTo("trace-456");
    assertThat(entity.getCreatedBy()).isEqualTo("test-user");
    assertThat(entity.getGuid()).isNotNull();
  }

  @Test
  void publishAfterWrite_publishesEventWithMatchingGuid() {
    ProviderEntity provider = ProviderEntity.builder().build();
    provider.setLastUpdatedBy("test-user");
    provider.setGuid(UUID.randomUUID());

    EventContext context = EventContext.of("corr-123", "trace-456");

    when(snapshotAssembler.assemble(provider))
        .thenReturn(new ProviderFirmChangedSnapshotEventV2Payload());
    when(objectMapper.writeValueAsString(any())).thenReturn("{}");

    publisher.publishAfterWrite(provider, context);

    ArgumentCaptor<ProviderEventEntity> entityCaptor =
        ArgumentCaptor.forClass(ProviderEventEntity.class);
    verify(providerEventRepository).save(entityCaptor.capture());
    ProviderEventEntity entity = entityCaptor.getValue();

    ArgumentCaptor<ProviderFirmChangedSnapshotEvent> eventCaptor =
        ArgumentCaptor.forClass(ProviderFirmChangedSnapshotEvent.class);
    verify(applicationEventPublisher).publishEvent(eventCaptor.capture());
    ProviderFirmChangedSnapshotEvent event = eventCaptor.getValue();

    assertThat(event.eventGuid()).isEqualTo(entity.getGuid());
    assertThat(event.correlationId()).isEqualTo("corr-123");
  }

  @Test
  void publishAfterWrite_usesSystemWhenLastUpdatedByIsNull() {
    ProviderEntity provider = ProviderEntity.builder().build();
    provider.setLastUpdatedBy(null);
    provider.setGuid(UUID.randomUUID());

    EventContext context = EventContext.of("corr-123", "trace-456");

    when(snapshotAssembler.assemble(provider))
        .thenReturn(new ProviderFirmChangedSnapshotEventV2Payload());
    when(objectMapper.writeValueAsString(any())).thenReturn("{}");

    publisher.publishAfterWrite(provider, context);

    ArgumentCaptor<ProviderEventEntity> captor = ArgumentCaptor.forClass(ProviderEventEntity.class);
    verify(providerEventRepository).save(captor.capture());
    ProviderEventEntity entity = captor.getValue();

    assertThat(entity.getCreatedBy()).isEqualTo("system");
  }
}
