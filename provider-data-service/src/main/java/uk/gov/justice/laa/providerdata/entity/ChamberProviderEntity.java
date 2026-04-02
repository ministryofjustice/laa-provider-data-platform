package uk.gov.justice.laa.providerdata.entity;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/** Provider subtype for Chambers. */
@SuperBuilder
@NoArgsConstructor
@Entity
@DiscriminatorValue(FirmType.CHAMBERS)
public class ChamberProviderEntity extends ProviderEntity {

  /**
   * Compatibility shim for newly-constructed instances before persistence populates the
   * discriminator-backed {@code firmType} field.
   */
  @Override
  public String getFirmType() {
    return FirmType.CHAMBERS;
  }
}
