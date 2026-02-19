package uk.gov.justice.laa.providerdata.entity;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorType;
import jakarta.persistence.Entity;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Provider Office Link entity representing a link between provider and office. There is always only
 * one head office. For Advocate type the office is the Chambers' office. Advocate can however have
 * an alternative bank account, intervention etc. Base entity for LSP ProviderOfficeLink and
 * Advocate ProviderOfficeLink subtypes.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "PROVIDER_OFFICE_LINK")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "FIRM_TYPE", discriminatorType = DiscriminatorType.STRING)
public class ProviderOfficeLinkEntity {

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

  @Column(name = "ACCOUNT_NUMBER", nullable = false)
  private String accountNumber;

  @ManyToOne
  @JoinColumn(
      name = "PROVIDER_GUID",
      nullable = false,
      foreignKey = @ForeignKey(name = "FK_PROVIDER_OFFICE_LINK_PROVIDER"))
  private ProviderEntity provider;

  @ManyToOne
  @JoinColumn(
      name = "OFFICE_GUID",
      nullable = false,
      foreignKey = @ForeignKey(name = "FK_PROVIDER_OFFICE_LINK_OFFICE"))
  private OfficeEntity office;

  @Column(name = "FIRM_TYPE", nullable = false, insertable = false, updatable = false)
  private String firmType;

  @Column(name = "HEAD_OFFICE_FLAG")
  private Boolean headOfficeFlag;

  @Column(name = "WEBSITE")
  private String website;

  @Column(name = "ACTIVE_DATE_TO")
  private LocalDate activeDateTo;
}
