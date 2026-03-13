package uk.gov.justice.laa.providerdata.mapper;

import java.math.BigDecimal;
import org.mapstruct.BeanMapping;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import uk.gov.justice.laa.providerdata.entity.BankAccountEntity;
import uk.gov.justice.laa.providerdata.entity.OfficeBankAccountLinkEntity;
import uk.gov.justice.laa.providerdata.entity.ProviderBankAccountLinkEntity;
import uk.gov.justice.laa.providerdata.model.BankAccountProviderOfficeCreateV2;
import uk.gov.justice.laa.providerdata.model.BankAccountV2;
import uk.gov.justice.laa.providerdata.model.OfficeBankAccountV2;

/** MapStruct mapper for bank account request/entity/response conversions. */
@Mapper(componentModel = "spring")
public interface BankAccountMapper {

  /**
   * Maps a bank account creation request to a {@link BankAccountEntity} template.
   *
   * <p>GUID, version, and audit fields are populated automatically by JPA.
   */
  @BeanMapping(builder = @Builder(disableBuilder = true), ignoreByDefault = true)
  @Mapping(target = "accountName", source = "accountName")
  @Mapping(target = "sortCode", source = "sortCode")
  @Mapping(target = "accountNumber", source = "accountNumber")
  BankAccountEntity toBankAccountEntity(BankAccountProviderOfficeCreateV2 request);

  /**
   * Maps a {@link ProviderBankAccountLinkEntity} to a {@link BankAccountV2} response DTO.
   *
   * <p>The GUID and audit fields are taken from the {@code bankAccount} sub-entity.
   */
  default BankAccountV2 toBankAccountV2(ProviderBankAccountLinkEntity link) {
    BankAccountEntity account = link.getBankAccount();
    return new BankAccountV2()
        .guid(account.getGuid() != null ? account.getGuid().toString() : null)
        .version(account.getVersion() != null ? BigDecimal.valueOf(account.getVersion()) : null)
        .createdBy(account.getCreatedBy())
        .createdTimestamp(account.getCreatedTimestamp())
        .lastUpdatedBy(account.getLastUpdatedBy())
        .lastUpdatedTimestamp(account.getLastUpdatedTimestamp())
        .accountName(account.getAccountName())
        .sortCode(account.getSortCode())
        .accountNumber(account.getAccountNumber());
  }

  /**
   * Maps an {@link OfficeBankAccountLinkEntity} to an {@link OfficeBankAccountV2} response DTO.
   *
   * <p>Account fields come from the nested {@code bankAccount}; link-level fields ({@code
   * activeDateFrom}, {@code activeDateTo}, {@code primaryFlag}) come from the link itself.
   */
  default OfficeBankAccountV2 toOfficeBankAccountV2(OfficeBankAccountLinkEntity link) {
    BankAccountEntity account = link.getBankAccount();
    return new OfficeBankAccountV2()
        .guid(account.getGuid() != null ? account.getGuid().toString() : null)
        .version(account.getVersion() != null ? BigDecimal.valueOf(account.getVersion()) : null)
        .createdBy(account.getCreatedBy())
        .createdTimestamp(account.getCreatedTimestamp())
        .lastUpdatedBy(account.getLastUpdatedBy())
        .lastUpdatedTimestamp(account.getLastUpdatedTimestamp())
        .accountName(account.getAccountName())
        .sortCode(account.getSortCode())
        .accountNumber(account.getAccountNumber())
        .activeDateFrom(link.getActiveDateFrom())
        .activeDateTo(link.getActiveDateTo())
        .primaryFlag(link.getPrimaryFlag());
  }

  /**
   * Maps a {@link ProviderBankAccountLinkEntity} to an {@link OfficeBankAccountV2} response DTO.
   *
   * <p>Used when returning Advocate bank accounts in the context of a Chambers office query.
   * Office-level fields ({@code activeDateFrom}, {@code activeDateTo}, {@code primaryFlag}) are not
   * applicable to Advocate accounts and will be {@code null}.
   */
  default OfficeBankAccountV2 toOfficeBankAccountV2(ProviderBankAccountLinkEntity link) {
    BankAccountEntity account = link.getBankAccount();
    return new OfficeBankAccountV2()
        .guid(account.getGuid() != null ? account.getGuid().toString() : null)
        .version(account.getVersion() != null ? BigDecimal.valueOf(account.getVersion()) : null)
        .createdBy(account.getCreatedBy())
        .createdTimestamp(account.getCreatedTimestamp())
        .lastUpdatedBy(account.getLastUpdatedBy())
        .lastUpdatedTimestamp(account.getLastUpdatedTimestamp())
        .accountName(account.getAccountName())
        .sortCode(account.getSortCode())
        .accountNumber(account.getAccountNumber());
  }
}
