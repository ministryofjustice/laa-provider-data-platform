package uk.gov.justice.laa.providerdata.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Provider Bank Account Link entity representing a link between provider and bank accounts.
 * Available for LSP/Advocates. Not applicable for Chambers. Defines an additional filter for bank
 * accounts available for offices.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "PROVIDER_BANK_ACCOUNT_LINK")
public class ProviderBankAccountLinkEntity {

  @Id
  @Column(name = "GUID", columnDefinition = "UUID")
  private UUID guid;

  @Column(name = "VERSION")
  private Long version;

  @Column(name = "CREATED_BY")
  private String createdBy;

  @Column(name = "CREATED_TIMESTAMP")
  private OffsetDateTime createdTimestamp;

  @Column(name = "LAST_UPDATED_BY")
  private String lastUpdatedBy;

  @Column(name = "LAST_UPDATED_TIMESTAMP")
  private OffsetDateTime lastUpdatedTimestamp;

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
