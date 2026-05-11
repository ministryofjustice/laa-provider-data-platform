package uk.gov.justice.laa.providerdata.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * Durable outbox record written within the same transaction as the source command.
 */
@SuperBuilder
@NoArgsConstructor
@Getter
@Setter
@Entity
@Table(name = "OUTBOX_EVENT")
public class OutboxEventEntity extends AuditableEntity {

  @Column(name = "AGGREGATE_TYPE", nullable = false)
  private String aggregateType;

  @Column(name = "AGGREGATE_ID", nullable = false)
  private UUID aggregateId;

  @Column(name = "EVENT_TYPE", nullable = false)
  private String eventType;

  @Column(name = "FIRM_NUMBER", nullable = false)
  private String firmNumber;

  @Column(name = "EVENT_PAYLOAD", nullable = false, columnDefinition = "TEXT")
  private String eventPayload;

  @Enumerated(EnumType.STRING)
  @Column(name = "STATUS", nullable = false)
  private OutboxEventStatus status;

  @Column(name = "ATTEMPT_COUNT", nullable = false)
  private Integer attemptCount;

  @Column(name = "OCCURRED_AT", nullable = false)
  private OffsetDateTime occurredAt;

  @Column(name = "SENT_AT")
  private OffsetDateTime sentAt;

  @Column(name = "LAST_ERROR")
  private String lastError;
}

