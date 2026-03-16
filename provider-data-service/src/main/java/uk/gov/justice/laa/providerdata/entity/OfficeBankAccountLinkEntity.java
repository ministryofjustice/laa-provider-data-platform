package uk.gov.justice.laa.providerdata.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
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
@Table(name = "OFFICE_BANK_ACCOUNT_LINK")
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

  @Column(name = "PRIMARY_FLAG")
  private Boolean primaryFlag;

  @Column(name = "ACTIVE_DATE_FROM")
  private LocalDate activeDateFrom;

  @Column(name = "ACTIVE_DATE_TO")
  private LocalDate activeDateTo;
}
