package uk.gov.justice.laa.providerdata.application.outbox;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.justice.laa.providerdata.application.outbox.model.OutboxEventMessage;
import uk.gov.justice.laa.providerdata.application.outbox.port.out.CommandAuditLogWritePort;

/**
 * Consumer-side processing for outbox events.
 *
 * <p>In this phase the consumer writes audit records, mimicking an external queue consumer.
 */
@Service
@RequiredArgsConstructor
public class DefaultOutboxEventConsumerUseCase implements OutboxEventConsumerUseCase {

  private static final String UPDATE_PROVIDER_FIRM_COMMAND = "UpdateProviderFirm";

  private final CommandAuditLogWritePort commandAuditLogWritePort;

  @Override
  public void consume(OutboxEventMessage event) {
    commandAuditLogWritePort.write(
        event.aggregateId(),
        event.firmNumber(),
        UPDATE_PROVIDER_FIRM_COMMAND,
        event.occurredAt(),
        extractField(event.payload(), "changedFields"));
  }

  static String extractField(String payload, String key) {
    if (payload == null || key == null || key.isBlank()) {
      return null;
    }

    String prefix = key + "=";
    for (String token : payload.split(";")) {
      if (token.startsWith(prefix)) {
        String value = token.substring(prefix.length());
        return value.isBlank() ? null : value;
      }
    }
    return null;
  }
}

