package uk.gov.justice.laa.providerdata.application.outbox;

import java.time.OffsetDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.justice.laa.providerdata.application.outbox.model.OutboxEventMessage;
import uk.gov.justice.laa.providerdata.application.outbox.port.out.OutboxEventPublisherPort;
import uk.gov.justice.laa.providerdata.application.outbox.port.out.OutboxEventStorePort;

/** Default outbox dispatch implementation with simple success/failure status updates. */
@Slf4j
@Service
@RequiredArgsConstructor
public class DefaultOutboxDispatcherUseCase implements OutboxDispatcherUseCase {

  private final OutboxEventStorePort outboxEventStorePort;
  private final OutboxEventPublisherPort outboxEventPublisherPort;

  @Override
  public void dispatchPendingEvents(int batchSize) {
    var pendingEvents = outboxEventStorePort.fetchPending(batchSize);
    log.info(
        "Outbox dispatch cycle started: batchSize={} pendingCount={}",
        batchSize,
        pendingEvents.size());

    for (OutboxEventMessage event : pendingEvents) {
      int nextAttempt = event.attemptCount() + 1;
      try {
        log.info(
            "Dispatching outbox event: eventGuid={} eventType={} firmNumber={} attempt={}",
            event.guid(),
            event.eventType(),
            event.firmNumber(),
            nextAttempt);
        outboxEventPublisherPort.publish(event);
        outboxEventStorePort.markSent(event.guid(), nextAttempt, OffsetDateTime.now());
        log.info(
            "Outbox event marked SENT: eventGuid={} eventType={} attempt={}",
            event.guid(),
            event.eventType(),
            nextAttempt);
      } catch (Exception ex) {
        outboxEventStorePort.markFailed(
            event.guid(), nextAttempt, truncate(ex.getMessage(), 1000));
        log.warn(
            "Outbox event marked FAILED: eventGuid={} eventType={} attempt={} error={}",
            event.guid(),
            event.eventType(),
            nextAttempt,
            ex.getMessage(),
            ex);
      }
    }

    log.info("Outbox dispatch cycle completed: processedCount={}", pendingEvents.size());
  }

  private static String truncate(String value, int maxLength) {
    if (value == null) {
      return null;
    }
    return value.length() <= maxLength ? value : value.substring(0, maxLength);
  }
}
