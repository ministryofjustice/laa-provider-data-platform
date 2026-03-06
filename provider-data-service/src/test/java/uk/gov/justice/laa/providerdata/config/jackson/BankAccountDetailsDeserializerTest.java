package uk.gov.justice.laa.providerdata.config.jackson;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.module.SimpleModule;
import uk.gov.justice.laa.providerdata.model.BankAccountProviderOfficeCreateV2;
import uk.gov.justice.laa.providerdata.model.BankAccountProviderOfficeLinkV2;
import uk.gov.justice.laa.providerdata.model.PaymentDetailsCreateOrLinkV2BankAccountDetails;

class BankAccountDetailsDeserializerTest {

  private JsonMapper mapper;

  @BeforeEach
  void setUp() {
    mapper =
        JsonMapper.builder()
            .addModule(
                new SimpleModule()
                    .addDeserializer(
                        PaymentDetailsCreateOrLinkV2BankAccountDetails.class,
                        new BankAccountDetailsDeserializer()))
            .build();
  }

  @Test
  void deserializes_to_link_type_when_bankAccountGUID_present() throws Exception {
    var result =
        mapper.readValue(
            """
            {"bankAccountGUID": "82f8d845-a606-44ca-8971-cc1794b7d619"}
            """,
            PaymentDetailsCreateOrLinkV2BankAccountDetails.class);

    assertThat(result).isInstanceOf(BankAccountProviderOfficeLinkV2.class);
    assertThat(((BankAccountProviderOfficeLinkV2) result).getBankAccountGUID())
        .isEqualTo("82f8d845-a606-44ca-8971-cc1794b7d619");
  }

  @Test
  void deserializes_to_create_type_when_bankAccountGUID_absent() throws Exception {
    var result =
        mapper.readValue(
            """
            {
              "accountName": "Westgate Legal LLP Client A/C",
              "sortCode": "601111",
              "accountNumber": "06805333"
            }
            """,
            PaymentDetailsCreateOrLinkV2BankAccountDetails.class);

    assertThat(result).isInstanceOf(BankAccountProviderOfficeCreateV2.class);
    var create = (BankAccountProviderOfficeCreateV2) result;
    assertThat(create.getAccountName()).isEqualTo("Westgate Legal LLP Client A/C");
    assertThat(create.getSortCode()).isEqualTo("601111");
    assertThat(create.getAccountNumber()).isEqualTo("06805333");
  }
}
