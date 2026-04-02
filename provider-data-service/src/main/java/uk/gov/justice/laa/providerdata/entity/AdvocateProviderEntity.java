package uk.gov.justice.laa.providerdata.entity;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * Provider subtype for practitioner providers.
 *
 * <p>The current provider discriminator keeps all practitioner providers under {@code firmType =
 * "Advocate"}. The {@code advocateType} field distinguishes Advocate vs Barrister semantics within
 * this single provider subtype.
 */
@SuperBuilder
@NoArgsConstructor
@Getter
@Setter
@EqualsAndHashCode(callSuper = false)
@Entity
@DiscriminatorValue(FirmType.ADVOCATE)
public class AdvocateProviderEntity extends ProviderEntity {

  /** PO.PO_VENDORS.ATTRIBUTE9 VARCHAR2(150). */
  @Column(name = "ADVOCATE_TYPE")
  private String advocateType;

  /** PO.PO_VENDORS.ATTRIBUTE10 VARCHAR2(150). */
  @Column(name = "ADVOCATE_LEVEL")
  private String advocateLevel;

  /** PO.PO_VENDORS.ATTRIBUTE11 VARCHAR2(150). */
  @Column(name = "SOLICITOR_REGULATION_AUTHORITY_ROLL_NUMBER")
  private String solicitorRegulationAuthorityRollNumber;

  /** PO.PO_VENDORS.ATTRIBUTE10 VARCHAR2(150). */
  @Column(name = "BARRISTER_LEVEL")
  private String barristerLevel;

  /** PO.PO_VENDORS.ATTRIBUTE11 VARCHAR2(150). */
  @Column(name = "BAR_COUNCIL_ROLL_NUMBER")
  private String barCouncilRollNumber;

  /**
   * Compatibility shim for newly-constructed instances before persistence populates the
   * discriminator-backed {@code firmType} field.
   */
  @Override
  public String getFirmType() {
    return FirmType.ADVOCATE;
  }
}
