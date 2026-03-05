package uk.gov.justice.laa.providerdata.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/** Bank account entity representing a financial account for providers. */
@Data
@EqualsAndHashCode(callSuper = false)
@Builder
@NoArgsConstructor
@AllArgsConstructor
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
