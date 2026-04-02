package uk.gov.justice.laa.providerdata.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/** Liaison manager entity representing a liaison manager for provider offices. */
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@EqualsAndHashCode(callSuper = false)
@Entity
@Table(name = "LIAISON_MANAGER")
public class LiaisonManagerEntity extends AuditableEntity {

  /** PO.PO_VENDOR_CONTACTS.FIRST_NAME VARCHAR2(15). */
  @Column(name = "FIRST_NAME", nullable = false)
  private String firstName;

  /** PO.PO_VENDOR_CONTACTS.FIRST_NAME VARCHAR2(20). */
  @Column(name = "LAST_NAME", nullable = false)
  private String lastName;

  /** PO.PO_VENDOR_CONTACTS.EMAIL_ADDRESS VARCHAR2(2000). */
  @Column(name = "EMAIL_ADDRESS", nullable = false)
  private String emailAddress;

  /** PO.PO_VENDOR_SITES_ALL.AREA_CODE VARCHAR2(10) || PO.PO_VENDOR_SITES_ALL.PHONE VARCHAR2(15). */
  @Column(name = "TELEPHONE_NUMBER")
  private String telephoneNumber;
}
