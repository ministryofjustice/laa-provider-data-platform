package uk.gov.justice.laa.providerdata.application.outbox;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.justice.laa.providerdata.application.outbox.model.OutboxEventMessage;
import uk.gov.justice.laa.providerdata.application.outbox.port.out.CommandAuditLogWritePort;

@ExtendWith(MockitoExtension.class)
class DefaultOutboxEventConsumerUseCaseTest {

  @Mock private CommandAuditLogWritePort commandAuditLogWritePort;

  @InjectMocks private DefaultOutboxEventConsumerUseCase useCase;

  @Test
  void consume_writesAuditRecordFromOutboxEvent() {
    UUID eventGuid = UUID.randomUUID();
    UUID providerGuid = UUID.randomUUID();
    OffsetDateTime occurredAt = OffsetDateTime.now();
    OutboxEventMessage event =
        new OutboxEventMessage(
            eventGuid,
            providerGuid,
            "ProviderFirmUpdated",
            "100001",
            "providerFirmGuid=" + providerGuid + ";firmNumber=100001;changedFields=name",
            0,
            occurredAt);

    useCase.consume(event);

    verify(commandAuditLogWritePort)
        .write(providerGuid, "100001", "UpdateProviderFirm", occurredAt, "name");
  }

  @Test
  void extractField_missingOrBlank_returnsNull() {
    assertThat(DefaultOutboxEventConsumerUseCase.extractField(null, "changedFields")).isNull();
    assertThat(DefaultOutboxEventConsumerUseCase.extractField("a=b", "missing")).isNull();
    assertThat(DefaultOutboxEventConsumerUseCase.extractField("changedFields=", "changedFields"))
        .isNull();
  }
}

