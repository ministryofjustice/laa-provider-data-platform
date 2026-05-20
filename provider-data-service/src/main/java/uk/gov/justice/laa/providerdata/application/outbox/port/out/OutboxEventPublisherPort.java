package uk.gov.justice.laa.providerdata.application.outbox.port.out;

import uk.gov.justice.laa.providerdata.application.outbox.model.OutboxEventMessage;

/** Outbound port for publishing outbox events to external messaging systems. */
public interface OutboxEventPublisherPort {

  void publish(OutboxEventMessage event);
}

