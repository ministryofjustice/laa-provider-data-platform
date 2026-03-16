package uk.gov.justice.laa.providerdata.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * Provider Parent Link entity representing a link between provider and parent provider. Applicable
 * only for provider type=Advocate. There can only be one parent provider of type=Chamber and/or
 * provider of type=LSP (maximum of two parents). Parent link is defined as a separate table to
 * facilitate future capability of Advocates having multiple parents. Chamber link is automatically
 * created when a new Advocate record is added. When Advocate changes Chamber (parent), the link
 * should be updated.
 */
@SuperBuilder
@NoArgsConstructor
@Getter
@Setter
@EqualsAndHashCode(callSuper = false)
@Entity
@Table(name = "PROVIDER_PARENT_LINK")
public class ProviderParentLinkEntity extends AuditableEntity {

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
