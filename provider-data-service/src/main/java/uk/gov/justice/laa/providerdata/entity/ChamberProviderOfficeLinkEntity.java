package uk.gov.justice.laa.providerdata.entity;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * Chamber Provider Office Link entity representing office-specific attributes for Chambers. Extends
 * ProviderOfficeLinkEntity with Chamber-specific attributes (currently no additional fields).
 */
@SuperBuilder
@NoArgsConstructor
@Getter
@Setter
@EqualsAndHashCode(callSuper = false)
@Entity
@DiscriminatorValue(FirmType.CHAMBERS)
public class ChamberProviderOfficeLinkEntity extends ProviderOfficeLinkEntity {}
