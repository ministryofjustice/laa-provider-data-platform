package uk.gov.justice.laa.providerdata.adapters.out.persistence;

import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.justice.laa.providerdata.application.outbox.port.out.CommandAuditLogWritePort;
import uk.gov.justice.laa.providerdata.entity.CommandAuditLogEntity;
import uk.gov.justice.laa.providerdata.repository.CommandAuditLogRepository;

/** Persistence adapter for writing command audit log rows. */
@Slf4j
@Component
@RequiredArgsConstructor
public class CommandAuditLogWritePersistenceAdapter implements CommandAuditLogWritePort {

  private final CommandAuditLogRepository commandAuditLogRepository;

  @Override
  @Transactional
  public void write(
      UUID providerFirmGuid,
      String firmNumber,
      String commandType,
      OffsetDateTime occurredAt,
      String changedFields) {
    CommandAuditLogEntity record =
        CommandAuditLogEntity.builder()
            .providerFirmGuid(providerFirmGuid)
            .firmNumber(firmNumber)
            .commandType(commandType)
            .occurredAt(occurredAt)
            .changedFields(changedFields)
            .build();

    commandAuditLogRepository.save(record);

    log.info(
        "Audit row written: providerFirmGuid={} firmNumber={} commandType={} changedFields={}",
        providerFirmGuid,
        firmNumber,
        commandType,
        changedFields);
  }
}
