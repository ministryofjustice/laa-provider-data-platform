package uk.gov.justice.laa.providerdata.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.LocalDate;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * A Novation represents a provider change event such as a merger, acquisition, legal entity change,
 * partnership change or organisational restructure.
 *
 * <p>{@code decisionDate}/{@code decisionReason}/{@code decisionBy} record the operation that moves
 * a Novation out of {@code Proposed}. {@code rescindedDate}/{@code rescindedReason}/{@code
 * rescindedBy} record a later rescission without overwriting the original decision. Both {@code
 * decisionBy} and {@code rescindedBy} are populated from the authenticated user performing the
 * corresponding operation, not client-supplied.
 */
@SuperBuilder
@NoArgsConstructor
@Getter
@Setter
@EqualsAndHashCode(callSuper = false)
@Entity
@Table(name = "NOVATION")
public class NovationEntity extends AuditableEntity {

  @Column(name = "NOVATION_TYPE", nullable = false)
  private String novationType;

  @Column(name = "NOVATION_EFFECTIVE_DATE", nullable = false)
  private LocalDate novationEffectiveDate;

  /** One of {@link NovationStatus}, or {@code null} if not supplied on creation. */
  @Column(name = "NOVATION_STATUS")
  private String novationStatus;

  @Column(name = "DECISION_DATE")
  private LocalDate decisionDate;

  @Column(name = "DECISION_REASON")
  private String decisionReason;

  @Column(name = "DECISION_BY")
  private String decisionBy;

  @Column(name = "RESCINDED_DATE")
  private LocalDate rescindedDate;

  @Column(name = "RESCINDED_REASON")
  private String rescindedReason;

  @Column(name = "RESCINDED_BY")
  private String rescindedBy;

  @Column(name = "DRIVER_FOR_NOVATION")
  private String driverForNovation;

  @Column(name = "NOTES")
  private String notes;
}
