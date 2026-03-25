package uk.gov.justice.laa.providerdata.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/** Office Contract Manager Link entity representing a link between office and contract manager. */
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@EqualsAndHashCode(callSuper = false)
@Entity
@Table(name = "OFFICE_CONTRACT_MANAGER_LINK")
public class OfficeContractManagerLinkEntity extends AuditableEntity {

  @ManyToOne
  @JoinColumn(
      name = "CONTRACT_MANAGER_GUID",
      nullable = false,
      foreignKey = @ForeignKey(name = "FK_OFFICE_CONTRACT_MGR_LINK_CONTRACT_MGR"))
  private ContractManagerEntity contractManager;

  @ManyToOne
  @JoinColumn(
      name = "OFFICE_GUID",
      nullable = false,
      foreignKey = @ForeignKey(name = "FK_OFFICE_CONTRACT_MGR_LINK_OFFICE"))
  private ProviderOfficeLinkEntity officeLink;
}
