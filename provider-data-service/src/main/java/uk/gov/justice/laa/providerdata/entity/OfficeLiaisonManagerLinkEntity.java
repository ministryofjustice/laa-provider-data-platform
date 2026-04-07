package uk.gov.justice.laa.providerdata.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/** Office Liaison Manager Link entity representing a link between office and liaison manager. */
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@EqualsAndHashCode(callSuper = false)
@Entity
@Table(
    name = "OFFICE_LIAISON_MANAGER_LINK",
    uniqueConstraints =
        @UniqueConstraint(
            name = "UK_OFFICE_LIAISON_MGR_LINK_OFFICE_LIAISON_MGR",
            columnNames = {"OFFICE_GUID", "LIAISON_MANAGER_GUID"}))
public class OfficeLiaisonManagerLinkEntity extends AuditableEntity {

  @ManyToOne
  @JoinColumn(
      name = "LIAISON_MANAGER_GUID",
      nullable = false,
      foreignKey = @ForeignKey(name = "FK_OFFICE_LIAISON_MGR_LINK_LIAISON_MGR"))
  private LiaisonManagerEntity liaisonManager;

  /** OFFICE_GUID points to PROVIDER_OFFICE_LINK.GUID, not OFFICE.GUID. */
  @ManyToOne
  @JoinColumn(
      name = "OFFICE_GUID",
      nullable = false,
      foreignKey = @ForeignKey(name = "FK_OFFICE_LIAISON_MGR_LINK_OFFICE"))
  private ProviderOfficeLinkEntity officeLink;

  /** PO.PO_VENDOR_CONTACTS.CREATION_DATE DATE. */
  @Column(name = "ACTIVE_DATE_FROM", nullable = false)
  private LocalDate activeDateFrom;

  /** PO.PO_VENDOR_CONTACTS.INACTIVE_DATE DATE. */
  @Column(name = "ACTIVE_DATE_TO")
  private LocalDate activeDateTo;

  @Column(name = "LINKED_FLAG", nullable = false)
  private Boolean linkedFlag;
}
