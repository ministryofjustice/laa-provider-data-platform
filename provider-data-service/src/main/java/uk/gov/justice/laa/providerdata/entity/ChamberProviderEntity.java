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
   * Returns the constant value for this subtype. The field itself is written by {@code @PrePersist}
   * before insert; this override ensures the correct value is visible on newly constructed
   * instances before that callback fires.
   */
  @Override
  public String getFirmType() {
    return FirmType.CHAMBERS;
  }
}
