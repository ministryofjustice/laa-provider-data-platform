package uk.gov.justice.laa.providerdata.entity;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import java.time.LocalDate;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/** Provider subtype for Legal Services Providers. */
@SuperBuilder
@NoArgsConstructor
@Getter
@Setter
@EqualsAndHashCode(callSuper = false)
@Entity
@DiscriminatorValue(FirmType.LEGAL_SERVICES_PROVIDER)
public class LspProviderEntity extends ProviderEntity {

  /** PO.PO_VENDORS.ATTRIBUTE1 VARCHAR2(150). */
  @Column(name = "CONSTITUTIONAL_STATUS")
  private String constitutionalStatus;

  /** PO.PO_VENDORS.ATTRIBUTE3 VARCHAR2(150). */
  @Column(name = "NOT_FOR_PROFIT_ORGANISATION_FLAG")
  private Boolean notForProfitOrganisationFlag;

  /** PO.PO_VENDORS.ATTRIBUTE2 VARCHAR2(150). */
  @Column(name = "INDEMNITY_RECEIVED_DATE")
  private LocalDate indemnityReceivedDate;

  /** PO.PO_VENDORS.ATTRIBUTE11 VARCHAR2(150). */
  @Column(name = "COMPANIES_HOUSE_NUMBER")
  private String companiesHouseNumber;

  /**
   * Compatibility shim for newly-constructed instances before persistence populates the
   * discriminator-backed {@code firmType} field.
   */
  @Override
  public String getFirmType() {
    return FirmType.LEGAL_SERVICES_PROVIDER;
  }
}
