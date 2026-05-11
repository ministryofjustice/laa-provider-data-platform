package uk.gov.justice.laa.providerdata.adapters.out.messaging;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import uk.gov.justice.laa.providerdata.application.outbox.model.OutboxEventMessage;

class FakeSnsSqsOutboxEventPublisherTest {

  private final FakeSnsSqsOutboxEventPublisher publisher = new FakeSnsSqsOutboxEventPublisher();

  @Test
  void publish_normalPayload_doesNotThrow() {
    OutboxEventMessage event =
        new OutboxEventMessage(UUID.randomUUID(), "ProviderFirmUpdated", "100001", "payload", 0);

    assertThatCode(() -> publisher.publish(event)).doesNotThrowAnyException();
  }

  @Test
  void publish_failureFlagInPayload_throws() {
    OutboxEventMessage event =
        new OutboxEventMessage(
            UUID.randomUUID(),
            "ProviderFirmUpdated",
            "100001",
            "simulate-publish-failure",
            0);

    assertThatThrownBy(() -> publisher.publish(event))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("Simulated SNS/SQS publish failure");
  }
}

