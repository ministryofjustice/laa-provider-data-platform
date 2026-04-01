package uk.gov.justice.laa.providerdata.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
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

  /** PO.PO_VENDOR_SITES_ALL.ADDRESS_LINE1 VARCHAR2(240). */
  @Column(name = "ADDRESS_LINE1", nullable = false)
  private String addressLine1;

  /** PO.PO_VENDOR_SITES_ALL.ADDRESS_LINE2 VARCHAR2(240). */
  @Column(name = "ADDRESS_LINE2")
  private String addressLine2;

  /** PO.PO_VENDOR_SITES_ALL.ADDRESS_LINE3 VARCHAR2(240). */
  @Column(name = "ADDRESS_LINE3")
  private String addressLine3;

  /** PO.PO_VENDOR_SITES_ALL.ADDRESS_LINE4 VARCHAR2(240). */
  @Column(name = "ADDRESS_LINE4")
  private String addressLine4;

  /** PO.PO_VENDOR_SITES_ALL.CITY VARCHAR2(25). */
  @Column(name = "ADDRESS_TOWN_OR_CITY", nullable = false)
  private String addressTownOrCity;

  /** PO.PO_VENDOR_SITES_ALL.COUNTY VARCHAR2(150). */
  @Column(name = "ADDRESS_COUNTY")
  private String addressCounty;

  /** PO.PO_VENDOR_SITES_ALL.ZIP VARCHAR2(20). */
  @Column(name = "ADDRESS_POST_CODE", nullable = false)
  private String addressPostCode;

  /** PO.PO_VENDOR_SITES_ALL.AREA_CODE VARCHAR2(10) || PO.PO_VENDOR_SITES_ALL.PHONE VARCHAR2(15). */
  @Column(name = "TELEPHONE_NUMBER")
  private String telephoneNumber;

  /** PO.PO_VENDOR_SITES_ALL.EMAIL_ADDRESS VARCHAR2(2000). */
  @Column(name = "EMAIL_ADDRESS")
  private String emailAddress;

  /** PO.PO_VENDOR_SITES_ALL.ATTRIBUTE6 VARCHAR2(150). */
  @Column(name = "DX_DETAILS_NUMBER")
  private String dxDetailsNumber;

  /** PO.PO_VENDOR_SITES_ALL.ATTRIBUTE7 VARCHAR2(150). */
  @Column(name = "DX_DETAILS_CENTRE")
  private String dxDetailsCentre;
}
