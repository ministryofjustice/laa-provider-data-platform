package uk.gov.justice.laa.providerdata.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.json.JsonMapper;
import uk.gov.justice.laa.providerdata.entity.ProviderEventEntity;
import uk.gov.justice.laa.providerdata.model.ProviderFirmChangedSnapshotEventV2Payload;
import uk.gov.justice.laa.providerdata.repository.ProviderEventRepository;

@ExtendWith(MockitoExtension.class)
class ProviderEventPersistenceListenerTest {

  @Mock private ProviderEventRepository providerEventRepository;
  @Mock private JsonMapper objectMapper;

  @InjectMocks private ProviderEventPersistenceListener listener;

  @Test
  void on_savesEntityWithFieldsFromEvent() {
    UUID guid = UUID.randomUUID();
    OffsetDateTime now = OffsetDateTime.now();
    ProviderFirmChangedSnapshotEventV2Payload payload =
        new ProviderFirmChangedSnapshotEventV2Payload();
    ProviderFirmChangedSnapshotEvent event =
        new ProviderFirmChangedSnapshotEvent(
            guid, "corr-123", "trace-456", "test-user", now, payload);

    when(objectMapper.writeValueAsString(any())).thenReturn("{}");

    listener.on(event);

    ArgumentCaptor<ProviderEventEntity> captor = ArgumentCaptor.forClass(ProviderEventEntity.class);
    verify(providerEventRepository).save(captor.capture());
    ProviderEventEntity entity = captor.getValue();

    assertThat(entity.getGuid()).isEqualTo(guid);
    assertThat(entity.getEventType()).isEqualTo("ProviderFirmChangedSnapshotEvent");
    assertThat(entity.getEventSource()).isEqualTo("apiV2");
    assertThat(entity.getCorrelationId()).isEqualTo("corr-123");
    assertThat(entity.getTraceId()).isEqualTo("trace-456");
    assertThat(entity.getCreatedBy()).isEqualTo("test-user");
    assertThat(entity.getCreatedTimestamp()).isEqualTo(now);
    assertThat(entity.getVersion()).isZero();
  }

  @Test
  void on_serialisesPayloadToJson() {
    ProviderFirmChangedSnapshotEventV2Payload payload =
        new ProviderFirmChangedSnapshotEventV2Payload();
    ProviderFirmChangedSnapshotEvent event =
        new ProviderFirmChangedSnapshotEvent(
            UUID.randomUUID(), null, null, "system", OffsetDateTime.now(), payload);

    when(objectMapper.writeValueAsString(payload)).thenReturn("{\"key\":\"value\"}");

    listener.on(event);

    ArgumentCaptor<ProviderEventEntity> captor = ArgumentCaptor.forClass(ProviderEventEntity.class);
    verify(providerEventRepository).save(captor.capture());

    assertThat(captor.getValue().getPayload()).isEqualTo("{\"key\":\"value\"}");
  }
}
