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
    for (OutboxEventMessage event : outboxEventStorePort.fetchPending(batchSize)) {
      int nextAttempt = event.attemptCount() + 1;
      try {
        outboxEventPublisherPort.publish(event);
        outboxEventStorePort.markSent(event.guid(), nextAttempt, OffsetDateTime.now());
      } catch (Exception ex) {
        outboxEventStorePort.markFailed(event.guid(), nextAttempt, truncate(ex.getMessage(), 1000));
        log.warn("Failed to publish outbox event {} ({})", event.guid(), event.eventType(), ex);
      }
    }
  }

  private static String truncate(String value, int maxLength) {
    if (value == null) {
      return null;
    }
    return value.length() <= maxLength ? value : value.substring(0, maxLength);
  }
}

