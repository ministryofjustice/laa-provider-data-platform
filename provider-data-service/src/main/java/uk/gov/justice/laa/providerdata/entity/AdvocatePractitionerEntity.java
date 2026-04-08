package uk.gov.justice.laa.providerdata.entity;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/** Provider subtype for advocate practitioners (advocateType = "Advocate"). */
@SuperBuilder
@NoArgsConstructor
@Getter
@Setter
@EqualsAndHashCode(callSuper = false)
@Entity
@DiscriminatorValue("Advocate.Advocate")
public class AdvocatePractitionerEntity extends PractitionerEntity {

  /** PO.PO_VENDORS.ATTRIBUTE10 VARCHAR2(150). */
  @Column(name = "ADVOCATE_LEVEL")
  private String advocateLevel;

  /** PO.PO_VENDORS.ATTRIBUTE11 VARCHAR2(150). */
  @Column(name = "SOLICITOR_REGULATION_AUTHORITY_ROLL_NUMBER")
  private String solicitorRegulationAuthorityRollNumber;

  /**
   * Returns the constant value for this subtype. The field itself is written by {@code @PrePersist}
   * before insert; this override ensures the correct value is visible on newly constructed
   * instances before that callback fires.
   */
  @Override
  public String getAdvocateType() {
    return AdvocateType.ADVOCATE;
  }
}
