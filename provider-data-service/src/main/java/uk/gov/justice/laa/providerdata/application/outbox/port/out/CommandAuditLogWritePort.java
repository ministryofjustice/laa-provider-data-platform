package uk.gov.justice.laa.providerdata.application.outbox.port.out;

import java.time.OffsetDateTime;
import java.util.UUID;

/** Outbound port for writing command audit log records. */
public interface CommandAuditLogWritePort {

  void write(
      UUID providerFirmGuid,
      String firmNumber,
      String commandType,
      OffsetDateTime occurredAt,
      String changedFields);
}

