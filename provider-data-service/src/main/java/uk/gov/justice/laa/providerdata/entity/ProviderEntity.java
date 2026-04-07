package uk.gov.justice.laa.providerdata.entity;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorType;
import jakarta.persistence.Entity;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.Table;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * Provider entity representing a legal services provider or individual practitioner. Base entity
 * for LSP, Chambers, and Advocate provider subtypes.
 */
@SuperBuilder
@NoArgsConstructor
@Getter
@Setter
@EqualsAndHashCode(callSuper = false)
@Entity
@Table(name = "PROVIDER")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "FIRM_TYPE", discriminatorType = DiscriminatorType.STRING)
public class ProviderEntity extends AuditableEntity {

  /** PO.PO_VENDORS.SEGMENT1 VARCHAR2(30) not null. */
  @Column(name = "FIRM_NUMBER", nullable = false, unique = true, updatable = false)
  private String firmNumber;

  /** PO.PO_VENDORS.ATTRIBUTE4 VARCHAR2(150). */
  @Column(name = "FIRM_TYPE", nullable = false, insertable = false, updatable = false)
  private String firmType;

  /** PO.PO_VENDORS.VENDOR_NAME VARCHAR2(240) not null. */
  @Column(name = "NAME", nullable = false)
  private String name;
}
