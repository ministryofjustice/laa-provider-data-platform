package uk.gov.justice.laa.providerdata.e2e;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;

import org.junit.jupiter.api.Test;

@ReadOnlyTest
class ProviderFirmE2eTest {

  @Test
  void getProviderFirm_lspByFirmNumber_returns200WithExpectedFields() {
    given()
        .pathParam("firmId", E2eConfig.lspFirmNumber())
        .when()
        .get("/provider-firms/{firmId}")
        .then()
        .statusCode(200)
        .body("data.firmNumber", equalTo(E2eConfig.lspFirmNumber()))
        .body("data.name", equalTo(E2eConfig.lspName()))
        .body("data.firmType", equalTo(E2eConfig.lspFirmType()))
        .body(
            "data.legalServicesProvider.headOffice.accountNumber",
            equalTo(E2eConfig.lspOfficeCode()))
        .body("data.legalServicesProvider.headOffice.activeDateTo", nullValue())
        // Chambers and practitioner sub-objects should be absent for an LSP
        .body("data.chambers", nullValue())
        .body("data.practitioner", nullValue());
  }

  @Test
  void getProviderFirm_chambersByFirmNumber_returns200WithExpectedFields() {
    given()
        .pathParam("firmId", E2eConfig.chambersFirmNumber())
        .when()
        .get("/provider-firms/{firmId}")
        .then()
        .statusCode(200)
        .body("data.firmNumber", equalTo(E2eConfig.chambersFirmNumber()))
        .body("data.name", equalTo(E2eConfig.chambersName()))
        .body("data.firmType", equalTo(E2eConfig.chambersFirmType()))
        .body("data.chambers.office.accountNumber", equalTo(E2eConfig.chambersOfficeCode()))
        // LSP and practitioner sub-objects should be absent for a Chambers firm
        .body("data.legalServicesProvider", nullValue())
        .body("data.practitioner", nullValue());
  }

  @Test
  void getProviderFirm_unknownFirmNumber_returns404WithErrorCode() {
    given()
        .pathParam("firmId", E2eConfig.invalidFirmNumber())
        .when()
        .get("/provider-firms/{firmId}")
        .then()
        .statusCode(404)
        .body("error.errorCode", equalTo("P00NF"));
  }
}
