package uk.gov.justice.laa.providerdata.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * Persistent audit record written after each successful provider firm command.
 *
 * <p>Records are append-only: they are never updated or deleted. The table is written
 * by {@link uk.gov.justice.laa.providerdata.command.event.CommandAuditEventListener}
 * after the originating transaction commits, ensuring the log only contains records
 * for changes that were durably persisted.
 *
 * <p>The {@code changedFields} column is a free-text summary (comma-separated field names)
 * derived from the command payload. A full JSON diff can be added in a later phase.
 */
@SuperBuilder
@NoArgsConstructor
@Getter
@Setter
@Entity
@Table(name = "COMMAND_AUDIT_LOG")
public class CommandAuditLogEntity extends AuditableEntity {

  @Column(name = "PROVIDER_FIRM_GUID", nullable = false)
  private UUID providerFirmGuid;

  @Column(name = "FIRM_NUMBER", nullable = false)
  private String firmNumber;

  @Column(name = "COMMAND_TYPE", nullable = false)
  private String commandType;

  @Column(name = "OCCURRED_AT", nullable = false)
  private OffsetDateTime occurredAt;

  /**
   * Comma-separated list of top-level fields present in the patch payload, e.g.
   * {@code "name,legalServicesProvider"}.
   */
  @Column(name = "CHANGED_FIELDS")
  private String changedFields;
}

