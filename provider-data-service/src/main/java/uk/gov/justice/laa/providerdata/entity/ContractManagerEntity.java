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

/** Contract manager entity representing a contract manager for office oversight. */
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@EqualsAndHashCode(callSuper = false)
@Entity
@Table(name = "CONTRACT_MANAGER")
public class ContractManagerEntity extends AuditableEntity {

  /** HR.PER_ALL_PEOPLE_F.PERSON_ID NUMBER(10) not null. */
  @Column(name = "CONTRACT_MANAGER_ID", nullable = false, unique = true, updatable = false)
  private String contractManagerId;

  /** HR.PER_ALL_PEOPLE_F.FIRST_NAME VARCHAR2(150). */
  @Column(name = "FIRST_NAME", nullable = false)
  private String firstName;

  /** HR.PER_ALL_PEOPLE_F.LAST_NAME VARCHAR2(150) not null. */
  @Column(name = "LAST_NAME", nullable = false)
  private String lastName;

  /** HR.PER_ALL_PEOPLE_F.EMAIL_ADDRESS VARCHAR2(240). */
  @Column(name = "EMAIL_ADDRESS")
  private String emailAddress;
}
