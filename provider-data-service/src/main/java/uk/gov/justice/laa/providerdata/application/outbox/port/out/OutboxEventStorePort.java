package uk.gov.justice.laa.providerdata.application.outbox.port.out;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import uk.gov.justice.laa.providerdata.application.outbox.model.OutboxEventMessage;

/** Outbound port for reading and updating outbox records. */
public interface OutboxEventStorePort {

  List<OutboxEventMessage> fetchPending(int limit);

  void markSent(UUID eventGuid, int attemptCount, OffsetDateTime sentAt);

  void markFailed(UUID eventGuid, int attemptCount, String errorMessage);
}

