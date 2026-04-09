package uk.gov.justice.laa.providerdata.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.DiscriminatorFormula;

/**
 * Provider entity representing a legal services provider or individual practitioner. Base entity
 * for LSP, Chambers, and Advocate provider subtypes.
 */
@SuperBuilder
@NoArgsConstructor
@Getter
@Setter
@EqualsAndHashCode(callSuper = false)
@Entity
@Table(name = "PROVIDER")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorFormula(
    "CASE "
        + "WHEN FIRM_TYPE = 'Advocate' AND ADVOCATE_TYPE = 'Advocate' THEN 'Advocate.Advocate' "
        + "WHEN FIRM_TYPE = 'Advocate' AND ADVOCATE_TYPE = 'Barrister' THEN 'Advocate.Barrister' "
        + "ELSE FIRM_TYPE "
        + "END")
public class ProviderEntity extends AuditableEntity {

  /** PO.PO_VENDORS.SEGMENT1 VARCHAR2(30) not null. */
  @Column(name = "FIRM_NUMBER", nullable = false, unique = true, updatable = false)
  private String firmNumber;

  /** PO.PO_VENDORS.ATTRIBUTE4 VARCHAR2(150). */
  @Setter(AccessLevel.NONE)
  @Column(name = "FIRM_TYPE", nullable = false, updatable = false)
  private String firmType;

  /** PO.PO_VENDORS.VENDOR_NAME VARCHAR2(240) not null. */
  @Column(name = "NAME", nullable = false)
  private String name;

  /**
   * Ensures {@code FIRM_TYPE} is populated before insert. Each concrete subclass overrides {@link
   * #getFirmType()} to return its constant; the polymorphic call here sets the field so that
   * Hibernate includes it in the INSERT statement.
   */
  @PrePersist
  void initFirmType() {
    if (firmType == null) {
      firmType = getFirmType();
    }
  }
}
