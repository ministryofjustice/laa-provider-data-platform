package uk.gov.justice.laa.providerdata.adapters.out.messaging;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.justice.laa.providerdata.application.outbox.model.OutboxEventMessage;
import uk.gov.justice.laa.providerdata.application.outbox.port.out.OutboxEventPublisherPort;

/**
 * Stub publisher used during local development before wiring a real SNS/SQS integration.
 */
@Slf4j
@Component
public class FakeSnsSqsOutboxEventPublisher implements OutboxEventPublisherPort {

  @Override
  public void publish(OutboxEventMessage event) {
    if (event.payload() != null && event.payload().contains("simulate-publish-failure")) {
      throw new IllegalStateException("Simulated SNS/SQS publish failure");
    }

    log.info(
        "Stub SNS/SQS publish. eventGuid={}, eventType={}, firmNumber={}",
        event.guid(),
        event.eventType(),
        event.firmNumber());
  }
}

