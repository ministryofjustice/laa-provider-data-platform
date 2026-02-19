package uk.gov.justice.laa.providerdata.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Provider Parent Link entity representing a link between provider and parent provider. Applicable
 * only for provider type=Advocate. There can only be one parent provider of type=Chamber and/or
 * provider of type=LSP (maximum of two parents). Parent link is defined as a separate table to
 * facilitate future capability of Advocates having multiple parents. Chamber link is automatically
 * created when a new Advocate record is added. When Advocate changes Chamber (parent), the link
 * should be updated.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "PROVIDER_PARENT_LINK")
public class ProviderParentLinkEntity {

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

  @ManyToOne
  @JoinColumn(
      name = "PROVIDER_GUID",
      nullable = false,
      foreignKey = @ForeignKey(name = "FK_PROVIDER_PARENT_LINK_PROVIDER"))
  private ProviderEntity provider;

  @ManyToOne
  @JoinColumn(
      name = "PARENT_GUID",
      nullable = false,
      foreignKey = @ForeignKey(name = "FK_PROVIDER_PARENT_LINK_PARENT"))
  private ProviderEntity parent;
}
