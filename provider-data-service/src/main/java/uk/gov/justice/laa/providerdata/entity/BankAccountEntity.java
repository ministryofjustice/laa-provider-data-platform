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

  @Column(name = "ACCOUNT_NAME", nullable = false)
  private String accountName;

  @Column(name = "SORT_CODE", nullable = false)
  private String sortCode;

  @Column(name = "ACCOUNT_NUMBER", nullable = false)
  private String accountNumber;
}
