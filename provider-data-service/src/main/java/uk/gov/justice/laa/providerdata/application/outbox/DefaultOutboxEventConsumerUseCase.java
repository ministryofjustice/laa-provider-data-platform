package uk.gov.justice.laa.providerdata.application.outbox;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.justice.laa.providerdata.application.outbox.model.OutboxEventMessage;
import uk.gov.justice.laa.providerdata.application.outbox.port.out.CommandAuditLogWritePort;

/**
 * Consumer-side processing for outbox events.
 *
 * <p>In this phase the consumer writes audit records, mimicking an external queue consumer.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DefaultOutboxEventConsumerUseCase implements OutboxEventConsumerUseCase {

  private static final String UPDATE_PROVIDER_FIRM_COMMAND = "UpdateProviderFirm";

  private final CommandAuditLogWritePort commandAuditLogWritePort;

  @Override
  public void consume(OutboxEventMessage event) {
    String changedFields = extractField(event.payload(), "changedFields");

    log.info(
        "Consumer received outbox event: eventGuid={} aggregateId={} eventType={} "
            + "firmNumber={} changedFields={}",
        event.guid(),
        event.aggregateId(),
        event.eventType(),
        event.firmNumber(),
        changedFields);

    commandAuditLogWritePort.write(
        event.aggregateId(),
        event.firmNumber(),
        UPDATE_PROVIDER_FIRM_COMMAND,
        event.occurredAt(),
        changedFields);

    log.info(
        "Consumer wrote audit record: providerFirmGuid={} firmNumber={} commandType={}",
        event.aggregateId(),
        event.firmNumber(),
        UPDATE_PROVIDER_FIRM_COMMAND);
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
