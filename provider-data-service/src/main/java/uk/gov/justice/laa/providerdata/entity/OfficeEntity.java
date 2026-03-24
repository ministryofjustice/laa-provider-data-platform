package uk.gov.justice.laa.providerdata.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.LocalDate;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/** Office entity representing a provider's office location. */
@SuperBuilder
@NoArgsConstructor
@Getter
@Setter
@EqualsAndHashCode(callSuper = false)
@Entity
@Table(name = "OFFICE")
public class OfficeEntity extends AuditableEntity {

  @Column(name = "ACTIVE_DATE_TO")
  private LocalDate activeDateTo;

  @Column(name = "ADDRESS_LINE1", nullable = false)
  private String addressLine1;

  @Column(name = "ADDRESS_LINE2")
  private String addressLine2;

  @Column(name = "ADDRESS_LINE3")
  private String addressLine3;

  @Column(name = "ADDRESS_LINE4")
  private String addressLine4;

  @Column(name = "ADDRESS_TOWN_OR_CITY", nullable = false)
  private String addressTownOrCity;

  @Column(name = "ADDRESS_COUNTY")
  private String addressCounty;

  @Column(name = "ADDRESS_POST_CODE", nullable = false)
  private String addressPostCode;

  @Column(name = "TELEPHONE_NUMBER")
  private String telephoneNumber;

  @Column(name = "EMAIL_ADDRESS")
  private String emailAddress;

  @Column(name = "DX_DETAILS_NUMBER")
  private String dxDetailsNumber;

  @Column(name = "DX_DETAILS_CENTRE")
  private String dxDetailsCentre;
}
