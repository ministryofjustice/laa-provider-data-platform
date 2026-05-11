package uk.gov.justice.laa.providerdata.adapters.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.justice.laa.providerdata.entity.OutboxEventEntity;
import uk.gov.justice.laa.providerdata.entity.OutboxEventStatus;
import uk.gov.justice.laa.providerdata.repository.OutboxEventRepository;

@ExtendWith(MockitoExtension.class)
class OutboxEventStorePersistenceAdapterTest {

  @Mock private OutboxEventRepository outboxEventRepository;

  @InjectMocks private OutboxEventStorePersistenceAdapter adapter;

  @Test
  void fetchPending_mapsToApplicationMessages() {
    UUID guid = UUID.randomUUID();
    OutboxEventEntity entity =
        OutboxEventEntity.builder()
            .aggregateType("ProviderFirm")
            .aggregateId(UUID.randomUUID())
            .eventType("ProviderFirmUpdated")
            .firmNumber("100001")
            .eventPayload("payload")
            .status(OutboxEventStatus.PENDING)
            .attemptCount(0)
            .occurredAt(OffsetDateTime.now())
            .build();
    entity.setGuid(guid);

    when(outboxEventRepository.findTop100ByStatusOrderByOccurredAtAsc(OutboxEventStatus.PENDING))
        .thenReturn(List.of(entity));

    var result = adapter.fetchPending(10);

    assertThat(result).hasSize(1);
    assertThat(result.getFirst().guid()).isEqualTo(guid);
    assertThat(result.getFirst().eventType()).isEqualTo("ProviderFirmUpdated");
    assertThat(result.getFirst().firmNumber()).isEqualTo("100001");
  }

  @Test
  void markSent_updatesStatusAndTimestamps() {
    UUID guid = UUID.randomUUID();
    OutboxEventEntity entity =
        OutboxEventEntity.builder()
            .aggregateType("ProviderFirm")
            .aggregateId(UUID.randomUUID())
            .eventType("ProviderFirmUpdated")
            .firmNumber("100001")
            .eventPayload("payload")
            .status(OutboxEventStatus.PENDING)
            .attemptCount(0)
            .occurredAt(OffsetDateTime.now())
            .build();
    entity.setGuid(guid);
    when(outboxEventRepository.findById(guid)).thenReturn(Optional.of(entity));

    OffsetDateTime sentAt = OffsetDateTime.now();
    adapter.markSent(guid, 1, sentAt);

    ArgumentCaptor<OutboxEventEntity> captor = ArgumentCaptor.forClass(OutboxEventEntity.class);
    verify(outboxEventRepository).save(captor.capture());
    assertThat(captor.getValue().getStatus()).isEqualTo(OutboxEventStatus.SENT);
    assertThat(captor.getValue().getAttemptCount()).isEqualTo(1);
    assertThat(captor.getValue().getSentAt()).isEqualTo(sentAt);
  }

  @Test
  void markFailed_updatesStatusAndError() {
    UUID guid = UUID.randomUUID();
    OutboxEventEntity entity =
        OutboxEventEntity.builder()
            .aggregateType("ProviderFirm")
            .aggregateId(UUID.randomUUID())
            .eventType("ProviderFirmUpdated")
            .firmNumber("100001")
            .eventPayload("payload")
            .status(OutboxEventStatus.PENDING)
            .attemptCount(0)
            .occurredAt(OffsetDateTime.now())
            .build();
    entity.setGuid(guid);
    when(outboxEventRepository.findById(guid)).thenReturn(Optional.of(entity));

    adapter.markFailed(guid, 2, "boom");

    ArgumentCaptor<OutboxEventEntity> captor = ArgumentCaptor.forClass(OutboxEventEntity.class);
    verify(outboxEventRepository).save(captor.capture());
    assertThat(captor.getValue().getStatus()).isEqualTo(OutboxEventStatus.FAILED);
    assertThat(captor.getValue().getAttemptCount()).isEqualTo(2);
    assertThat(captor.getValue().getLastError()).isEqualTo("boom");
  }
}

