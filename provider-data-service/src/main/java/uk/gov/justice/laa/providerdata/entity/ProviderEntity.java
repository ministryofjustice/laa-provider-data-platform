package uk.gov.justice.laa.providerdata.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/** Provider entity representing a legal services provider or individual practitioner. */
@SuperBuilder
@NoArgsConstructor
@Getter
@Setter
@EqualsAndHashCode(callSuper = false)
@Entity
@Table(name = "PROVIDER")
public class ProviderEntity extends AuditableEntity {

  @Column(name = "FIRM_NUMBER", nullable = false, unique = true)
  private String firmNumber;

  @Column(name = "FIRM_TYPE", nullable = false)
  private String firmType;

  @Column(name = "NAME", nullable = false)
  private String name;
}
