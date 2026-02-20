package uk.gov.justice.laa.providerdata.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Office entity representing a provider's office location. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "OFFICE")
public class OfficeEntity {

  @Id
  @Column(name = "GUID", columnDefinition = "UUID")
  private UUID guid;

  @Column(name = "VERSION")
  private Long version;

  @Column(name = "CREATED_BY")
  private String createdBy;

  @Column(name = "CREATED_TIMESTAMP")
  private OffsetDateTime createdTimestamp;

  @Column(name = "LAST_UPDATED_BY")
  private String lastUpdatedBy;

  @Column(name = "LAST_UPDATED_TIMESTAMP")
  private OffsetDateTime lastUpdatedTimestamp;

  @Column(name = "ACTIVE_DATE_TO")
  private LocalDate activeDateTo;

  @Column(name = "ADDRESS_LINE1")
  private String addressLine1;

  @Column(name = "ADDRESS_LINE2")
  private String addressLine2;

  @Column(name = "ADDRESS_LINE3")
  private String addressLine3;

  @Column(name = "ADDRESS_LINE4")
  private String addressLine4;

  @Column(name = "ADDRESS_TOWN_OR_CITY")
  private String addressTownOrCity;

  @Column(name = "ADDRESS_COUNTY")
  private String addressCounty;

  @Column(name = "ADDRESS_POST_CODE")
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
