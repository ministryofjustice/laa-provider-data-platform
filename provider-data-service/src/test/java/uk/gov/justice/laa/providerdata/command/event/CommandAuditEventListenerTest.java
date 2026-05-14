package uk.gov.justice.laa.providerdata.command.event;

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
import uk.gov.justice.laa.providerdata.model.LSPDetailsPatchV2;
import uk.gov.justice.laa.providerdata.model.ProviderPatchV2;
import uk.gov.justice.laa.providerdata.repository.CommandAuditLogRepository;

@ExtendWith(MockitoExtension.class)
class CommandAuditEventListenerTest {

  @Mock private CommandAuditLogRepository auditLogRepository;

  @InjectMocks private CommandAuditEventListener listener;

  @Test
  void onProviderFirmUpdated_savesAuditRecord() {
    UUID providerGuid = UUID.randomUUID();
    ProviderPatchV2 patch = new ProviderPatchV2().name("New Name");
    ProviderFirmUpdatedEvent event =
        new ProviderFirmUpdatedEvent(
            providerGuid, "100001", "UpdateProviderFirm", patch, OffsetDateTime.now());

    listener.onProviderFirmUpdated(event);

    ArgumentCaptor<CommandAuditLogEntity> captor = forClass(CommandAuditLogEntity.class);
    verify(auditLogRepository).save(captor.capture());

    CommandAuditLogEntity saved = captor.getValue();
    assertThat(saved.getProviderFirmGuid()).isEqualTo(providerGuid);
    assertThat(saved.getFirmNumber()).isEqualTo("100001");
    assertThat(saved.getCommandType()).isEqualTo("UpdateProviderFirm");
    assertThat(saved.getOccurredAt()).isEqualTo(event.occurredAt());
    assertThat(saved.getChangedFields()).isEqualTo("name");
  }

  @Test
  void onProviderFirmUpdated_withLspPatch_recordsCorrectFields() {
    ProviderPatchV2 patch =
        new ProviderPatchV2()
            .name("LSP Updated")
            .legalServicesProvider(new LSPDetailsPatchV2().companiesHouseNumber("12345678"));
    ProviderFirmUpdatedEvent event =
        ProviderFirmUpdatedEvent.of(UUID.randomUUID(), "100002", patch);

    listener.onProviderFirmUpdated(event);

    ArgumentCaptor<CommandAuditLogEntity> captor = forClass(CommandAuditLogEntity.class);
    verify(auditLogRepository).save(captor.capture());
    assertThat(captor.getValue().getChangedFields()).isEqualTo("name,legalServicesProvider");
  }

  // --- summariseChangedFields unit tests ---

  @Test
  void summariseChangedFields_nullPatch_returnsNull() {
    assertThat(CommandAuditEventListener.summariseChangedFields(null)).isNull();
  }

  @Test
  void summariseChangedFields_emptyPatch_returnsNull() {
    assertThat(CommandAuditEventListener.summariseChangedFields(new ProviderPatchV2())).isNull();
  }

  @Test
  void summariseChangedFields_nameOnly_returnsName() {
    assertThat(CommandAuditEventListener.summariseChangedFields(new ProviderPatchV2().name("X")))
        .isEqualTo("name");
  }

  @Test
  void summariseChangedFields_multipleFields_returnsCommaSeparated() {
    ProviderPatchV2 patch =
        new ProviderPatchV2().name("X").legalServicesProvider(new LSPDetailsPatchV2());
    assertThat(CommandAuditEventListener.summariseChangedFields(patch))
        .isEqualTo("name,legalServicesProvider");
  }
}
