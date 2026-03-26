package uk.gov.justice.laa.providerdata.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * Provider Parent Link entity representing a link between provider and parent provider. Applicable
 * only for provider type=Advocate.
 */
@SuperBuilder
@NoArgsConstructor
@Getter
@Setter
@EqualsAndHashCode(callSuper = false)
@Entity
@Table(
    name = "PROVIDER_PARENT_LINK",
    uniqueConstraints =
        @UniqueConstraint(
            name = "UK_PROVIDER_PARENT_LINK_PROVIDER_PARENT",
            columnNames = {"PROVIDER_GUID", "PARENT_GUID"}))
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
