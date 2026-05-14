package uk.gov.justice.laa.providerdata.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.justice.laa.providerdata.entity.CommandAuditLogEntity;
import uk.gov.justice.laa.providerdata.repository.CommandAuditLogRepository;

@ExtendWith(MockitoExtension.class)
class DefaultCommandAuditLogQueryServiceTest {

  @Mock private CommandAuditLogRepository auditLogRepository;

  @InjectMocks private DefaultCommandAuditLogQueryService queryService;

  @Test
  void getAuditLog_byGuid_returnsEntries() {
    UUID firmGuid = UUID.randomUUID();
    UUID entryGuid = UUID.randomUUID();
    OffsetDateTime now = OffsetDateTime.now();

    CommandAuditLogEntity entity = new CommandAuditLogEntity();
    entity.setProviderFirmGuid(firmGuid);
    entity.setFirmNumber("100001");
    entity.setCommandType("UpdateProviderFirm");
    entity.setOccurredAt(now);
    entity.setChangedFields("name");

    when(auditLogRepository.findByProviderFirmGuidOrderByOccurredAtAsc(firmGuid))
        .thenReturn(List.of(entity));

    List<CommandAuditLogEntry> result = queryService.getAuditLog(firmGuid.toString());

    assertThat(result).hasSize(1);
    CommandAuditLogEntry entry = result.getFirst();
    assertThat(entry.providerFirmGuid()).isEqualTo(firmGuid);
    assertThat(entry.firmNumber()).isEqualTo("100001");
    assertThat(entry.commandType()).isEqualTo("UpdateProviderFirm");
    assertThat(entry.occurredAt()).isEqualTo(now);
    assertThat(entry.changedFields()).isEqualTo("name");
  }

  @Test
  void getAuditLog_byFirmNumber_returnsEntries() {
    UUID firmGuid = UUID.randomUUID();
    OffsetDateTime now = OffsetDateTime.now();

    CommandAuditLogEntity entity = new CommandAuditLogEntity();
    entity.setProviderFirmGuid(firmGuid);
    entity.setFirmNumber("100002");
    entity.setCommandType("UpdateProviderFirm");
    entity.setOccurredAt(now);
    entity.setChangedFields("legalServicesProvider");

    when(auditLogRepository.findByFirmNumberOrderByOccurredAtAsc("100002"))
        .thenReturn(List.of(entity));

    List<CommandAuditLogEntry> result = queryService.getAuditLog("100002");

    assertThat(result).hasSize(1);
    assertThat(result.getFirst().firmNumber()).isEqualTo("100002");
  }

  @Test
  void getAuditLog_noRecords_returnsEmptyList() {
    UUID firmGuid = UUID.randomUUID();
    when(auditLogRepository.findByProviderFirmGuidOrderByOccurredAtAsc(firmGuid))
        .thenReturn(List.of());

    List<CommandAuditLogEntry> result = queryService.getAuditLog(firmGuid.toString());

    assertThat(result).isEmpty();
  }

  @Test
  void getAuditLog_multipleEntries_preservesOrder() {
    UUID firmGuid = UUID.randomUUID();
    OffsetDateTime t1 = OffsetDateTime.now().minusHours(1);

    CommandAuditLogEntity e1 = new CommandAuditLogEntity();
    e1.setProviderFirmGuid(firmGuid);
    e1.setFirmNumber("100003");
    e1.setCommandType("UpdateProviderFirm");
    e1.setOccurredAt(t1);

    CommandAuditLogEntity e2 = new CommandAuditLogEntity();
    e2.setProviderFirmGuid(firmGuid);
    e2.setFirmNumber("100003");
    e2.setCommandType("UpdateProviderFirm");
    OffsetDateTime t2 = OffsetDateTime.now();
    e2.setOccurredAt(t2);

    when(auditLogRepository.findByProviderFirmGuidOrderByOccurredAtAsc(firmGuid))
        .thenReturn(List.of(e1, e2));

    List<CommandAuditLogEntry> result = queryService.getAuditLog(firmGuid.toString());

    assertThat(result).hasSize(2);
    assertThat(result.get(0).occurredAt()).isEqualTo(t1);
    assertThat(result.get(1).occurredAt()).isEqualTo(t2);
  }
}
