package uk.gov.justice.laa.providerdata.application.outbox;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.justice.laa.providerdata.application.outbox.model.OutboxEventMessage;
import uk.gov.justice.laa.providerdata.application.outbox.port.out.OutboxEventPublisherPort;
import uk.gov.justice.laa.providerdata.application.outbox.port.out.OutboxEventStorePort;

@ExtendWith(MockitoExtension.class)
class DefaultOutboxDispatcherUseCaseTest {

  @Mock private OutboxEventStorePort outboxEventStorePort;
  @Mock private OutboxEventPublisherPort outboxEventPublisherPort;

  @InjectMocks private DefaultOutboxDispatcherUseCase useCase;

  @Test
  void dispatchPendingEvents_publishSuccess_marksSent() {
    UUID eventGuid = UUID.randomUUID();
    OutboxEventMessage event =
        new OutboxEventMessage(eventGuid, "ProviderFirmUpdated", "100001", "payload", 0);
    when(outboxEventStorePort.fetchPending(100)).thenReturn(List.of(event));

    useCase.dispatchPendingEvents(100);

    verify(outboxEventPublisherPort).publish(event);
    verify(outboxEventStorePort)
        .markSent(ArgumentMatchers.eq(eventGuid), ArgumentMatchers.eq(1), ArgumentMatchers.any(OffsetDateTime.class));
    verify(outboxEventStorePort, never()).markFailed(ArgumentMatchers.any(), ArgumentMatchers.anyInt(), ArgumentMatchers.any());
  }

  @Test
  void dispatchPendingEvents_publishFailure_marksFailed() {
    UUID eventGuid = UUID.randomUUID();
    OutboxEventMessage event =
        new OutboxEventMessage(eventGuid, "ProviderFirmUpdated", "100001", "payload", 1);
    when(outboxEventStorePort.fetchPending(50)).thenReturn(List.of(event));
    org.mockito.Mockito.doThrow(new IllegalStateException("Publish failed"))
        .when(outboxEventPublisherPort)
        .publish(event);

    useCase.dispatchPendingEvents(50);

    verify(outboxEventPublisherPort).publish(event);
    verify(outboxEventStorePort, never())
        .markSent(ArgumentMatchers.any(), ArgumentMatchers.anyInt(), ArgumentMatchers.any());
    verify(outboxEventStorePort)
        .markFailed(ArgumentMatchers.eq(eventGuid), ArgumentMatchers.eq(2), ArgumentMatchers.eq("Publish failed"));
  }
}

