package uk.gov.justice.laa.providerdata.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDate;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * Office Bank Account Link entity representing a link between office and bank accounts. Applicable
 * only for paymentMethod=EFT (Electronic). Currently active one is marked as primaryFlag=true
 * (others are historical records).
 */
@SuperBuilder
@NoArgsConstructor
@Getter
@Setter
@EqualsAndHashCode(callSuper = false)
@Entity
@Table(
    name = "OFFICE_BANK_ACCOUNT_LINK",
    uniqueConstraints =
        @UniqueConstraint(
            name = "UK_OFFICE_BA_LINK_PROVIDER_OFFICE_BANK_ACCOUNT",
            columnNames = {"PROVIDER_OFFICE_LINK_GUID", "BANK_ACCOUNT_GUID"}))
public class OfficeBankAccountLinkEntity extends AuditableEntity {

  @ManyToOne
  @JoinColumn(
      name = "BANK_ACCOUNT_GUID",
      nullable = false,
      foreignKey = @ForeignKey(name = "FK_OFFICE_BA_LINK_BANK_ACCOUNT"))
  private BankAccountEntity bankAccount;

  @ManyToOne
  @JoinColumn(
      name = "PROVIDER_OFFICE_LINK_GUID",
      nullable = false,
      foreignKey = @ForeignKey(name = "FK_OFFICE_BA_LINK_PROVIDER_OFFICE"))
  private ProviderOfficeLinkEntity providerOfficeLink;

  /** AP.AP_BANK_ACCCOUNT_USES_ALL.PRIMARY_FLAGE VARCHAR2(1) not null. */
  @Column(name = "PRIMARY_FLAG", nullable = false)
  private Boolean primaryFlag;

  /** AP.AP_BANK_ACCCOUNT_USES_ALL.START_DATE DATE. */
  @Column(name = "ACTIVE_DATE_FROM", nullable = false)
  private LocalDate activeDateFrom;

  /** AP.AP_BANK_ACCCOUNT_USES_ALL.END_DATE DATE. */
  @Column(name = "ACTIVE_DATE_TO")
  private LocalDate activeDateTo;
}
