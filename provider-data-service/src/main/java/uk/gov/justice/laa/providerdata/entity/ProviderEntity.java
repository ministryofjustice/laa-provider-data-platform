package uk.gov.justice.laa.providerdata.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Provider entity representing a legal services provider or individual practitioner. */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "PROVIDER")
public class ProviderEntity extends AuditableEntity {

  @Column(name = "FIRM_NUMBER", nullable = false)
  private String firmNumber;

  @Column(name = "FIRM_TYPE")
  private String firmType;

  @Column(name = "NAME")
  private String name;
}
