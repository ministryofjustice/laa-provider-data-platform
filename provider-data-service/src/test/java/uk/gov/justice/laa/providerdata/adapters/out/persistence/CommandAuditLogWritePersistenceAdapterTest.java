package uk.gov.justice.laa.providerdata.adapters.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.Mockito.verify;

import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.justice.laa.providerdata.entity.CommandAuditLogEntity;
import uk.gov.justice.laa.providerdata.repository.CommandAuditLogRepository;

@ExtendWith(MockitoExtension.class)
class CommandAuditLogWritePersistenceAdapterTest {

  @Mock private CommandAuditLogRepository commandAuditLogRepository;

  @InjectMocks private CommandAuditLogWritePersistenceAdapter adapter;

  @Test
  void write_persistsAuditRecord() {
    UUID providerGuid = UUID.randomUUID();
    OffsetDateTime occurredAt = OffsetDateTime.now();

    adapter.write(providerGuid, "100001", "UpdateProviderFirm", occurredAt, "name");

    ArgumentCaptor<CommandAuditLogEntity> captor = forClass(CommandAuditLogEntity.class);
    verify(commandAuditLogRepository).save(captor.capture());

    CommandAuditLogEntity saved = captor.getValue();
    assertThat(saved.getProviderFirmGuid()).isEqualTo(providerGuid);
    assertThat(saved.getFirmNumber()).isEqualTo("100001");
    assertThat(saved.getCommandType()).isEqualTo("UpdateProviderFirm");
    assertThat(saved.getOccurredAt()).isEqualTo(occurredAt);
    assertThat(saved.getChangedFields()).isEqualTo("name");
  }
}

