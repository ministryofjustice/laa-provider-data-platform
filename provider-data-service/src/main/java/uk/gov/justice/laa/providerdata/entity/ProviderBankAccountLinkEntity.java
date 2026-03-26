package uk.gov.justice.laa.providerdata.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * Provider Bank Account Link entity representing a link between provider and bank accounts.
 * Available for LSP/Advocates. Not applicable for Chambers.
 */
@SuperBuilder
@NoArgsConstructor
@Getter
@Setter
@EqualsAndHashCode(callSuper = false)
@Entity
@Table(
    name = "PROVIDER_BANK_ACCOUNT_LINK",
    uniqueConstraints =
        @UniqueConstraint(
            name = "UK_PROVIDER_BA_LINK_PROVIDER_BANK_ACCOUNT",
            columnNames = {"PROVIDER_GUID", "BANK_ACCOUNT_GUID"}))
public class ProviderBankAccountLinkEntity extends AuditableEntity {

  @ManyToOne
  @JoinColumn(
      name = "BANK_ACCOUNT_GUID",
      nullable = false,
      foreignKey = @ForeignKey(name = "FK_PROVIDER_BA_LINK_BANK_ACCOUNT"))
  private BankAccountEntity bankAccount;

  @ManyToOne
  @JoinColumn(
      name = "PROVIDER_GUID",
      nullable = false,
      foreignKey = @ForeignKey(name = "FK_PROVIDER_BA_LINK_PROVIDER"))
  private ProviderEntity provider;
}
