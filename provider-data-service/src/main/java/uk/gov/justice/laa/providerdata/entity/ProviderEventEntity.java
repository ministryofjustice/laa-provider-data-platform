package uk.gov.justice.laa.providerdata.entity;

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
 * Permanent record of a provider firm changed snapshot event.
 *
 * <p>Written in the same transaction as the triggering write operation. Does not extend {@link
 * AuditableEntity} because the audit fields are supplied explicitly from the write context.
 */
@Entity
@Table(name = "PROVIDER_EVENT")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProviderEventEntity {

  @Id
  @Column(name = "GUID", columnDefinition = "uuid", updatable = false, nullable = false)
  private UUID guid;

  @Column(name = "VERSION", nullable = false)
  private Long version;

  @Column(name = "CREATED_BY", updatable = false, nullable = false)
  private String createdBy;

  @Column(name = "CREATED_TIMESTAMP", updatable = false, nullable = false)
  private OffsetDateTime createdTimestamp;

  @Column(name = "LAST_UPDATED_BY", nullable = false)
  private String lastUpdatedBy;

  @Column(name = "LAST_UPDATED_TIMESTAMP", nullable = false)
  private OffsetDateTime lastUpdatedTimestamp;

  @Column(name = "EVENT_TYPE", nullable = false)
  private String eventType;

  @Column(name = "EVENT_SOURCE", nullable = false)
  private String eventSource;

  @Column(name = "CORRELATION_ID")
  private String correlationId;

  @Column(name = "TRACE_ID")
  private String traceId;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "PAYLOAD", columnDefinition = "jsonb", nullable = false)
  private String payload;
}
