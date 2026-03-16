package uk.gov.justice.laa.providerdata.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
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
@Table(name = "BANK_ACCOUNT")
public class BankAccountEntity extends AuditableEntity {

  @Column(name = "ACCOUNT_NAME")
  private String accountName;

  @Column(name = "SORT_CODE")
  private String sortCode;

  @Column(name = "ACCOUNT_NUMBER")
  private String accountNumber;
}
