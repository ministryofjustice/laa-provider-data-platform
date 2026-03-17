package uk.gov.justice.laa.providerdata.e2e;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;

import org.junit.jupiter.api.Test;

@ReadOnlyTest
class ProviderFirmBankAccountsE2eTest {

  @Test
  void getProviderFirmBankAccounts_returns200WithExpectedAccount() {
    given()
        .pathParam("firmId", E2eConfig.lspFirmNumber())
        .when()
        .get("/provider-firms/{firmId}/bank-details")
        .then()
        .statusCode(200)
        .body("data.content", hasSize(greaterThanOrEqualTo(1)))
        .body("data.content[0].accountName", equalTo(E2eConfig.lspBankAccountName()))
        .body("data.content[0].sortCode", equalTo(E2eConfig.lspBankAccountSortCode()))
        .body("data.content[0].accountNumber", equalTo(E2eConfig.lspBankAccountNumber()))
        .body("data.metadata.pagination.totalItems", greaterThanOrEqualTo(1))
        .body("data.metadata.searchCriteria.criteria", empty());
  }

  @Test
  void getProviderFirmBankAccounts_filterByPartialAccountNumber_returnsMatchingAccount() {
    given()
        .pathParam("firmId", E2eConfig.lspFirmNumber())
        .queryParam("bankAccountNumber", E2eConfig.lspBankAccountPartialMatch())
        .when()
        .get("/provider-firms/{firmId}/bank-details")
        .then()
        .statusCode(200)
        .body("data.content", hasSize(greaterThanOrEqualTo(1)))
        .body("data.content[0].accountNumber", equalTo(E2eConfig.lspBankAccountNumber()))
        .body("data.metadata.searchCriteria.criteria[0].filter", equalTo("bankAccountNumber"))
        .body(
            "data.metadata.searchCriteria.criteria[0].values[0]",
            equalTo(E2eConfig.lspBankAccountPartialMatch()));
  }

  @Test
  void getProviderFirmBankAccounts_filterByNonMatchingNumber_returnsEmpty() {
    given()
        .pathParam("firmId", E2eConfig.lspFirmNumber())
        .queryParam("bankAccountNumber", E2eConfig.lspBankAccountNoMatch())
        .when()
        .get("/provider-firms/{firmId}/bank-details")
        .then()
        .statusCode(200)
        .body("data.content", hasSize(0))
        .body("data.metadata.pagination.totalItems", equalTo(0));
  }

  @Test
  void getProviderFirmBankAccounts_unknownFirm_returns404() {
    given()
        .pathParam("firmId", E2eConfig.invalidFirmNumber())
        .when()
        .get("/provider-firms/{firmId}/bank-details")
        .then()
        .statusCode(404)
        .body("error.errorCode", equalTo("P00NF"));
  }

  @Test
  void getProviderFirmOfficeBankAccounts_returns200WithPrimaryAccount() {
    given()
        .pathParam("firmId", E2eConfig.lspFirmNumber())
        .pathParam("officeCode", E2eConfig.lspOfficeCode())
        .when()
        .get("/provider-firms/{firmId}/offices/{officeCode}/bank-details")
        .then()
        .statusCode(200)
        .body("data.content", hasSize(greaterThanOrEqualTo(1)))
        .body("data.content[0].accountNumber", equalTo(E2eConfig.lspBankAccountNumber()))
        .body("data.content[0].sortCode", equalTo(E2eConfig.lspBankAccountSortCode()))
        .body("data.content[0].primaryFlag", equalTo(true))
        .body("data.metadata.pagination.totalItems", greaterThanOrEqualTo(1))
        .body("data.metadata.searchCriteria.criteria", empty());
  }

  @Test
  void getProviderFirmOfficeBankAccounts_unknownFirm_returns404() {
    given()
        .pathParam("firmId", E2eConfig.invalidFirmNumber())
        .pathParam("officeCode", E2eConfig.lspOfficeCode())
        .when()
        .get("/provider-firms/{firmId}/offices/{officeCode}/bank-details")
        .then()
        .statusCode(404)
        .body("error.errorCode", equalTo("P00NF"));
  }

  @Test
  void getProviderFirmOfficeBankAccounts_unknownOffice_returns404() {
    given()
        .pathParam("firmId", E2eConfig.lspFirmNumber())
        .pathParam("officeCode", E2eConfig.invalidOfficeCode())
        .when()
        .get("/provider-firms/{firmId}/offices/{officeCode}/bank-details")
        .then()
        .statusCode(404)
        .body("error.errorCode", equalTo("P00NF"));
  }
}
