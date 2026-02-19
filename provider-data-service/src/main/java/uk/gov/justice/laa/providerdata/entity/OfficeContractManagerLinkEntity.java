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

/** Office Contract Manager Link entity representing a link between office and contract manager. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "OFFICE_CONTRACT_MANAGER_LINK")
public class OfficeContractManagerLinkEntity {

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
      name = "CONTRACT_MANAGER_GUID",
      nullable = false,
      foreignKey = @ForeignKey(name = "FK_OFFICE_CONTRACT_MGR_LINK_CONTRACT_MGR"))
  private ContractManagerEntity contractManager;

  @ManyToOne
  @JoinColumn(
      name = "OFFICE_GUID",
      nullable = false,
      foreignKey = @ForeignKey(name = "FK_OFFICE_CONTRACT_MGR_LINK_OFFICE"))
  private OfficeEntity office;
}
