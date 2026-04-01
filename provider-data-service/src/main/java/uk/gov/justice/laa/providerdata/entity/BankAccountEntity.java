package uk.gov.justice.laa.providerdata.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/** Bank account entity representing a financial account for providers. */
@SuperBuilder
@NoArgsConstructor
@Getter
@Setter
@EqualsAndHashCode(callSuper = false)
@Entity
@Table(
    name = "BANK_ACCOUNT",
    uniqueConstraints =
        @UniqueConstraint(
            name = "UK_BANK_ACCOUNT_SORTCODE_ACCOUNTNUMBER",
            columnNames = {"SORT_CODE", "ACCOUNT_NUMBER"}))
public class BankAccountEntity extends AuditableEntity {

  /** AP.AP_BANK_ACCCOUNTS_ALL.BANK_ACCOUNT_NAME VARCHAR2(80) not null. */
  @Column(name = "ACCOUNT_NAME", nullable = false)
  private String accountName;

  /** AP.AP_BANK_BRANCHES.BANK_NUM VARCHAR2(25). */
  @Column(name = "SORT_CODE", nullable = false, updatable = false)
  private String sortCode;

  /** AP.AP_BANK_ACCCOUNTS_ALL.BANK_ACCOUNT_NUM VARCHAR2(30) not null. */
  @Column(name = "ACCOUNT_NUMBER", nullable = false, updatable = false)
  private String accountNumber;
}
