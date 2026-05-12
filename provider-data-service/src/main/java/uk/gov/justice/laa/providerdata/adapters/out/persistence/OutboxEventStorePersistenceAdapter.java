package uk.gov.justice.laa.providerdata.adapters.out.persistence;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.justice.laa.providerdata.application.outbox.model.OutboxEventMessage;
import uk.gov.justice.laa.providerdata.application.outbox.port.out.OutboxEventStorePort;
import uk.gov.justice.laa.providerdata.entity.OutboxEventEntity;
import uk.gov.justice.laa.providerdata.entity.OutboxEventStatus;
import uk.gov.justice.laa.providerdata.repository.OutboxEventRepository;

/** Persistence adapter for reading and updating durable outbox records. */
@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxEventStorePersistenceAdapter implements OutboxEventStorePort {

  private final OutboxEventRepository outboxEventRepository;

  @Override
  @Transactional(readOnly = true)
  public List<OutboxEventMessage> fetchPending(int limit) {
    List<OutboxEventEntity> pending =
        outboxEventRepository.findTop100ByStatusOrderByOccurredAtAsc(OutboxEventStatus.PENDING);

    List<OutboxEventMessage> messages =
        pending.stream().limit(Math.max(limit, 0L)).map(this::toMessage).toList();
    log.info(
        "Fetched pending outbox events: requestedLimit={} returnedCount={}",
        limit,
        messages.size());
    return messages;
  }

  @Override
  @Transactional
  public void markSent(UUID eventGuid, int attemptCount, OffsetDateTime sentAt) {
    OutboxEventEntity event = getById(eventGuid);
    event.setStatus(OutboxEventStatus.SENT);
    event.setAttemptCount(attemptCount);
    event.setSentAt(sentAt);
    event.setLastError(null);
    outboxEventRepository.save(event);
    log.info(
        "Outbox state updated to SENT: eventGuid={} attemptCount={} sentAt={}",
        eventGuid,
        attemptCount,
        sentAt);
  }

  @Override
  @Transactional
  public void markFailed(UUID eventGuid, int attemptCount, String errorMessage) {
    OutboxEventEntity event = getById(eventGuid);
    event.setStatus(OutboxEventStatus.FAILED);
    event.setAttemptCount(attemptCount);
    event.setLastError(errorMessage);
    outboxEventRepository.save(event);
    log.info(
        "Outbox state updated to FAILED: eventGuid={} attemptCount={} error={}",
        eventGuid,
        attemptCount,
        errorMessage);
  }

  private OutboxEventEntity getById(UUID eventGuid) {
    return outboxEventRepository
        .findById(eventGuid)
        .orElseThrow(() -> new IllegalStateException("Outbox event not found: " + eventGuid));
  }

  private OutboxEventMessage toMessage(OutboxEventEntity event) {
    return new OutboxEventMessage(
        event.getGuid(),
        event.getAggregateId(),
        event.getEventType(),
        event.getFirmNumber(),
        event.getEventPayload(),
        event.getAttemptCount(),
        event.getOccurredAt());
  }
}
