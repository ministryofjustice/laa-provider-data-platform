package uk.gov.justice.laa.providerdata.usecase;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Permanent record of a {@link ProviderFirmChangedSnapshotEvent}.
 *
 * <p>Written in the same transaction as the triggering write operation. Unlike the transient {@code
 * event_publication} outbox table (which Spring Modulith clears after delivery), rows in this table
 * are never deleted and back the {@code GET /provider-events} query API.
 *
 * <p>Does not extend {@link uk.gov.justice.laa.providerdata.shared.AuditableEntity} because the
 * audit fields are supplied explicitly from the write context rather than by JPA auditing
 * listeners, and the GUID is assigned by the caller rather than auto-generated.
 */
@Entity
@Table(name = "PROVIDER_EVENT")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProviderEventEntity {

  /** Externally assigned UUID; matches {@link ProviderFirmChangedSnapshotEvent#eventGuid()}. */
  @Id
  @Column(name = "GUID", columnDefinition = "uuid", updatable = false, nullable = false)
  private UUID guid;

  /** Always {@code 0}; events are immutable. */
  @Column(name = "VERSION", nullable = false)
  private Long version;

  /** Username of the authenticated user who triggered the write. */
  @Column(name = "CREATED_BY", updatable = false, nullable = false)
  private String createdBy;

  /** Timestamp of the write transaction. */
  @Column(name = "CREATED_TIMESTAMP", updatable = false, nullable = false)
  private OffsetDateTime createdTimestamp;

  /** Same as {@link #createdBy}; events do not change after creation. */
  @Column(name = "LAST_UPDATED_BY", nullable = false)
  private String lastUpdatedBy;

  /** Same as {@link #createdTimestamp}; events do not change after creation. */
  @Column(name = "LAST_UPDATED_TIMESTAMP", nullable = false)
  private OffsetDateTime lastUpdatedTimestamp;

  /** Always {@code "ProviderFirmChangedSnapshotEvent"}. */
  @Column(name = "EVENT_TYPE", nullable = false)
  private String eventType;

  /** Always {@code "apiV2"}. */
  @Column(name = "EVENT_SOURCE", nullable = false)
  private String eventSource;

  /** Value of the incoming {@code x-correlation-id} header; may be {@code null}. */
  @Column(name = "CORRELATION_ID")
  private String correlationId;

  /** Value of the incoming {@code traceparent} header; may be {@code null}. */
  @Column(name = "TRACE_ID")
  private String traceId;

  /** Serialised {@code ProviderFirmChangedSnapshotEventV2Payload} as a JSON string. */
  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "PAYLOAD", columnDefinition = "jsonb", nullable = false)
  private String payload;
}
