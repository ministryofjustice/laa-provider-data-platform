package uk.gov.justice.laa.providerdata.entity;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Chamber Provider Office Link entity representing office-specific attributes for Chambers. Extends
 * ProviderOfficeLinkEntity with Chamber-specific attributes (currently no additional fields).
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@DiscriminatorValue("Chambers")
public class ChamberProviderOfficeLinkEntity extends ProviderOfficeLinkEntity {
  // Chambers-specific attributes to be added as needed
}
