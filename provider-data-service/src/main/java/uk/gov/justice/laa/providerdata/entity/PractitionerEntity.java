package uk.gov.justice.laa.providerdata.entity;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.PrePersist;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * Abstract provider subtype for practitioner providers (firmType = "Advocate").
 *
 * <p>Concrete subtypes are {@link AdvocatePractitionerEntity} (advocateType = "Advocate") and
 * {@link BarristerPractitionerEntity} (advocateType = "Barrister"), discriminated by a compound
 * formula on {@code FIRM_TYPE} and {@code ADVOCATE_TYPE}.
 */
@SuperBuilder
@NoArgsConstructor
@Getter
@Setter
@EqualsAndHashCode(callSuper = false)
@Entity
@DiscriminatorValue(FirmType.ADVOCATE)
public abstract class PractitionerEntity extends ProviderEntity {

  /** PO.PO_VENDORS.ATTRIBUTE9 VARCHAR2(150). */
  @Setter(AccessLevel.NONE)
  @Column(name = "ADVOCATE_TYPE", updatable = false)
  private String advocateType;

  /**
   * Returns the constant value for this subtype. The field itself is written by {@code @PrePersist}
   * before insert; this override ensures the correct value is visible on newly constructed
   * instances before that callback fires.
   */
  @Override
  public String getFirmType() {
    return FirmType.ADVOCATE;
  }

  /**
   * Ensures {@code ADVOCATE_TYPE} is populated before insert. Each concrete subclass overrides
   * {@link #getAdvocateType()} to return its constant; the polymorphic call here sets the field so
   * that Hibernate includes it in the INSERT statement.
   */
  @PrePersist
  void initAdvocateType() {
    if (advocateType == null) {
      advocateType = getAdvocateType();
    }
  }
}
