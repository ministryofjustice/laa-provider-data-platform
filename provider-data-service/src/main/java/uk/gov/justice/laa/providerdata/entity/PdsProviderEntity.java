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

/** Provider subtype for Public Defender Services. */
@SuperBuilder
@NoArgsConstructor
@Getter
@Setter
@EqualsAndHashCode(callSuper = false)
@Entity
@DiscriminatorValue(FirmType.PUBLIC_DEFENDER_SERVICE)
public final class PdsProviderEntity extends ProviderEntity {

  @Column(name = "CONSTITUTIONAL_STATUS")
  private String constitutionalStatus;

  @Column(name = "INDEMNITY_RECEIVED_DATE")
  private LocalDate indemnityReceivedDate;

  @Column(name = "COMPANIES_HOUSE_NUMBER")
  private String companiesHouseNumber;

  @Override
  public String getFirmType() {
    return FirmType.PUBLIC_DEFENDER_SERVICE;
  }
}
