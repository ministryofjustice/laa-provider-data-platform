package uk.gov.justice.laa.providerdata.command.event;

import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import uk.gov.justice.laa.providerdata.entity.CommandAuditLogEntity;
import uk.gov.justice.laa.providerdata.model.ProviderPatchV2;
import uk.gov.justice.laa.providerdata.repository.CommandAuditLogRepository;

/**
 * Handles {@link ProviderFirmUpdatedEvent} after the originating transaction commits.
 *
 * <p>Using {@link TransactionPhase#AFTER_COMMIT} guarantees the audit record is only written when
 * the command's database changes are durable. The listener runs in a fresh transaction ({@link
 * Propagation#REQUIRES_NEW}) so that an audit failure never rolls back the command.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CommandAuditEventListener {

  private final CommandAuditLogRepository auditLogRepository;

  /**
   * Persists a command audit record after the provider-firm update transaction commits.
   *
   * @param event provider-firm update event emitted by the command service
   */
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void onProviderFirmUpdated(ProviderFirmUpdatedEvent event) {
    log.debug(
        "Writing audit record for command={} providerFirmGUID={}",
        event.commandType(),
        event.providerFirmGUID());

    CommandAuditLogEntity record =
        CommandAuditLogEntity.builder()
            .providerFirmGuid(event.providerFirmGUID())
            .firmNumber(event.firmNumber())
            .commandType(event.commandType())
            .occurredAt(event.occurredAt())
            .changedFields(summariseChangedFields(event.patch()))
            .build();

    auditLogRepository.save(record);

    log.info(
        "Audit record saved: command={} providerFirmGUID={} changedFields={}",
        event.commandType(),
        event.providerFirmGUID(),
        record.getChangedFields());
  }

  /**
   * Derives a comma-separated summary of which top-level fields were present in the patch payload.
   * This gives a lightweight, query-friendly record of what changed without storing the full
   * before/after state.
   */
  static String summariseChangedFields(ProviderPatchV2 patch) {
    if (patch == null) {
      return null;
    }
    List<String> fields = new ArrayList<>();
    if (patch.getName() != null) {
      fields.add("name");
    }
    if (patch.getLegalServicesProvider() != null) {
      fields.add("legalServicesProvider");
    }
    if (patch.getPractitioner() != null) {
      fields.add("practitioner");
    }
    return fields.isEmpty() ? null : String.join(",", fields);
  }
}
