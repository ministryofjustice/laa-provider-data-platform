package uk.gov.justice.laa.providerdata.entity;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/** Provider office link subtype for Public Defender Service head offices. */
@SuperBuilder
@NoArgsConstructor
@Getter
@Setter
@EqualsAndHashCode(callSuper = false)
@Entity
@DiscriminatorValue(FirmType.PUBLIC_DEFENDER_SERVICE)
public final class PdsProviderOfficeLinkEntity extends ProviderOfficeLinkEntity {

  @Column(name = "VAT_REGISTRATION_NUMBER")
  private String vatRegistrationNumber;
}
