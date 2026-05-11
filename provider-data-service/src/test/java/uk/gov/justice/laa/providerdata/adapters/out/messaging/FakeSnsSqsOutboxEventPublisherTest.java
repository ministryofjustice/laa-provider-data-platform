package uk.gov.justice.laa.providerdata.adapters.out.messaging;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.justice.laa.providerdata.application.outbox.OutboxEventConsumerUseCase;
import uk.gov.justice.laa.providerdata.application.outbox.model.OutboxEventMessage;

@ExtendWith(MockitoExtension.class)
class FakeSnsSqsOutboxEventPublisherTest {

  @Mock private OutboxEventConsumerUseCase outboxEventConsumerUseCase;

  private FakeSnsSqsOutboxEventPublisher publisher;

  @BeforeEach
  void setUp() {
    publisher = new FakeSnsSqsOutboxEventPublisher(outboxEventConsumerUseCase);
  }

  @Test
  void publish_normalPayload_doesNotThrow() {
    OutboxEventMessage event =
        new OutboxEventMessage(
            UUID.randomUUID(),
            UUID.randomUUID(),
            "ProviderFirmUpdated",
            "100001",
            "payload",
            0,
            java.time.OffsetDateTime.now());

    assertThatCode(() -> publisher.publish(event)).doesNotThrowAnyException();
    verify(outboxEventConsumerUseCase).consume(event);
  }

  @Test
  void publish_failureFlagInPayload_throws() {
    OutboxEventMessage event =
        new OutboxEventMessage(
            UUID.randomUUID(),
            UUID.randomUUID(),
            "ProviderFirmUpdated",
            "100001",
            "simulate-publish-failure",
            0,
            java.time.OffsetDateTime.now());

    assertThatThrownBy(() -> publisher.publish(event))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("Simulated SNS/SQS publish failure");
    verifyNoInteractions(outboxEventConsumerUseCase);
  }
}

