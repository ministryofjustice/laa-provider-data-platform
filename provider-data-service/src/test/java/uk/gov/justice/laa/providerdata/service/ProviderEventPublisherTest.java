package uk.gov.justice.laa.providerdata.service;

import static org.assertj.core.api.Assertions.assertThat;
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
import uk.gov.justice.laa.providerdata.entity.ProviderEntity;
import uk.gov.justice.laa.providerdata.event.EventContext;
import uk.gov.justice.laa.providerdata.event.ProviderFirmChangedSnapshotEvent;
import uk.gov.justice.laa.providerdata.model.ProviderFirmChangedSnapshotEventV2Payload;

@ExtendWith(MockitoExtension.class)
class ProviderEventPublisherTest {

  @Mock private ProviderSnapshotAssembler snapshotAssembler;
  @Mock private ApplicationEventPublisher applicationEventPublisher;

  @InjectMocks private ProviderEventPublisher publisher;

  @Test
  void publishAfterWrite_publishesEventWithCorrectFields() {
    ProviderEntity provider = ProviderEntity.builder().build();
    provider.setLastUpdatedBy("test-user");
    provider.setGuid(UUID.randomUUID());

    EventContext context = EventContext.of("corr-123", "trace-456");

    when(snapshotAssembler.assemble(provider))
        .thenReturn(new ProviderFirmChangedSnapshotEventV2Payload());

    publisher.publishAfterWrite(provider, context);

    ArgumentCaptor<ProviderFirmChangedSnapshotEvent> captor =
        ArgumentCaptor.forClass(ProviderFirmChangedSnapshotEvent.class);
    verify(applicationEventPublisher).publishEvent(captor.capture());
    ProviderFirmChangedSnapshotEvent event = captor.getValue();

    assertThat(event.eventGuid()).isNotNull();
    assertThat(event.correlationId()).isEqualTo("corr-123");
    assertThat(event.traceId()).isEqualTo("trace-456");
    assertThat(event.triggeredBy()).isEqualTo("test-user");
    assertThat(event.occurredAt()).isNotNull();
  }

  @Test
  void publishAfterWrite_usesSystemWhenLastUpdatedByIsNull() {
    ProviderEntity provider = ProviderEntity.builder().build();
    provider.setLastUpdatedBy(null);
    provider.setGuid(UUID.randomUUID());

    EventContext context = EventContext.empty();

    when(snapshotAssembler.assemble(provider))
        .thenReturn(new ProviderFirmChangedSnapshotEventV2Payload());

    publisher.publishAfterWrite(provider, context);

    ArgumentCaptor<ProviderFirmChangedSnapshotEvent> captor =
        ArgumentCaptor.forClass(ProviderFirmChangedSnapshotEvent.class);
    verify(applicationEventPublisher).publishEvent(captor.capture());

    assertThat(captor.getValue().triggeredBy()).isEqualTo("system");
  }
}
