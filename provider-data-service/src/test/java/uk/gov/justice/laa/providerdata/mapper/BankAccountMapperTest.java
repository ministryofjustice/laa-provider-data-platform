package uk.gov.justice.laa.providerdata.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import uk.gov.justice.laa.providerdata.entity.BankAccountEntity;
import uk.gov.justice.laa.providerdata.entity.OfficeBankAccountLinkEntity;
import uk.gov.justice.laa.providerdata.entity.ProviderBankAccountLinkEntity;
import uk.gov.justice.laa.providerdata.model.BankAccountProviderOfficeCreateV2;
import uk.gov.justice.laa.providerdata.model.BankAccountV2;
import uk.gov.justice.laa.providerdata.model.OfficeBankAccountV2;

class BankAccountMapperTest {

  private final BankAccountMapper mapper = new BankAccountMapperImpl();

  @Test
  void toBankAccountEntity_mapsAllFields() {
    var request =
        new BankAccountProviderOfficeCreateV2()
            .accountName("Test Account")
            .sortCode("12-34-56")
            .accountNumber("12345678")
            .activeDateFrom(LocalDate.of(2024, 1, 1));

    BankAccountEntity entity = mapper.toBankAccountEntity(request);

    assertThat(entity.getAccountName()).isEqualTo("Test Account");
    assertThat(entity.getSortCode()).isEqualTo("12-34-56");
    assertThat(entity.getAccountNumber()).isEqualTo("12345678");
  }

  @Test
  void toBankAccountV2_mapsAllFieldsFromBankAccountEntity() {
    UUID guid = UUID.randomUUID();
    OffsetDateTime now = OffsetDateTime.now();

    BankAccountEntity account = new BankAccountEntity();
    account.setGuid(guid);
    account.setVersion(3L);
    account.setCreatedBy("user1");
    account.setCreatedTimestamp(now);
    account.setLastUpdatedBy("user2");
    account.setLastUpdatedTimestamp(now);
    account.setAccountName("HSBC Account");
    account.setSortCode("40-47-84");
    account.setAccountNumber("87654321");

    ProviderBankAccountLinkEntity link = new ProviderBankAccountLinkEntity();
    link.setBankAccount(account);

    BankAccountV2 dto = mapper.toBankAccountV2(link);

    assertThat(dto.getGuid()).isEqualTo(guid);
    assertThat(dto.getVersion()).isEqualTo(3);
    assertThat(dto.getCreatedBy()).isEqualTo("user1");
    assertThat(dto.getCreatedTimestamp()).isEqualTo(now);
    assertThat(dto.getLastUpdatedBy()).isEqualTo("user2");
    assertThat(dto.getLastUpdatedTimestamp()).isEqualTo(now);
    assertThat(dto.getAccountName()).isEqualTo("HSBC Account");
    assertThat(dto.getSortCode()).isEqualTo("40-47-84");
    assertThat(dto.getAccountNumber()).isEqualTo("87654321");
  }

  @Test
  void toBankAccountV2_nullGuidAndVersion_returnNullInDto() {
    BankAccountEntity account = new BankAccountEntity();
    ProviderBankAccountLinkEntity link = new ProviderBankAccountLinkEntity();
    link.setBankAccount(account);

    BankAccountV2 dto = mapper.toBankAccountV2(link);

    assertThat(dto.getGuid()).isNull();
    assertThat(dto.getVersion()).isNull();
  }

  @Test
  void toOfficeBankAccountV2_mapsAccountAndLinkFields() {
    BankAccountEntity account = new BankAccountEntity();
    UUID guid = UUID.randomUUID();
    account.setGuid(guid);
    account.setVersion(1L);
    account.setAccountName("Barclays Account");
    account.setSortCode("20-00-00");
    account.setAccountNumber("11223344");

    OfficeBankAccountLinkEntity link = new OfficeBankAccountLinkEntity();
    LocalDate from = LocalDate.of(2024, 1, 1);
    LocalDate to = LocalDate.of(2025, 1, 1);
    link.setBankAccount(account);
    link.setActiveDateFrom(from);
    link.setActiveDateTo(to);
    link.setPrimaryFlag(Boolean.TRUE);

    OfficeBankAccountV2 dto = mapper.toOfficeBankAccountV2(link);

    assertThat(dto.getGuid()).isEqualTo(guid);
    assertThat(dto.getVersion()).isEqualTo(1);
    assertThat(dto.getAccountName()).isEqualTo("Barclays Account");
    assertThat(dto.getSortCode()).isEqualTo("20-00-00");
    assertThat(dto.getAccountNumber()).isEqualTo("11223344");
    assertThat(dto.getActiveDateFrom()).isEqualTo(from);
    assertThat(dto.getActiveDateTo()).isEqualTo(to);
    assertThat(dto.getPrimaryFlag()).isTrue();
  }
}
