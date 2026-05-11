package uk.gov.justice.laa.providerdata.application.outbox;

import uk.gov.justice.laa.providerdata.application.outbox.model.OutboxEventMessage;

/** Consumes published outbox events and applies downstream side effects. */
public interface OutboxEventConsumerUseCase {

  void consume(OutboxEventMessage event);
}

