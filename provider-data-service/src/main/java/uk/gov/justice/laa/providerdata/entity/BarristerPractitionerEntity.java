package uk.gov.justice.laa.providerdata.entity;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/** Provider subtype for barrister practitioners (advocateType = "Barrister"). */
@SuperBuilder
@NoArgsConstructor
@Getter
@Setter
@EqualsAndHashCode(callSuper = false)
@Entity
@DiscriminatorValue("Advocate.Barrister")
public class BarristerPractitionerEntity extends PractitionerEntity {

  /** PO.PO_VENDORS.ATTRIBUTE10 VARCHAR2(150). */
  @Column(name = "BARRISTER_LEVEL")
  private String barristerLevel;

  /** PO.PO_VENDORS.ATTRIBUTE11 VARCHAR2(150). */
  @Column(name = "BAR_COUNCIL_ROLL_NUMBER")
  private String barCouncilRollNumber;

  /**
   * Returns the constant value for this subtype. The field itself is written by {@code @PrePersist}
   * before insert; this override ensures the correct value is visible on newly constructed
   * instances before that callback fires.
   */
  @Override
  public String getAdvocateType() {
    return AdvocateType.BARRISTER;
  }
}
