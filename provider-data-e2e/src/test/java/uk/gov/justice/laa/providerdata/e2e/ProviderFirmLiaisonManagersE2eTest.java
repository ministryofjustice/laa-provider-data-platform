package uk.gov.justice.laa.providerdata.e2e;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;

import org.junit.jupiter.api.Test;

@ReadOnlyTest
class ProviderFirmLiaisonManagersE2eTest {

  @Test
  void getOfficeLiaisonManagers_returns200WithExpectedManager() {
    given()
        .pathParam("firmId", E2eConfig.lspFirmNumber())
        .pathParam("officeCode", E2eConfig.lspOfficeCode())
        .when()
        .get("/provider-firms/{firmId}/offices/{officeCode}/liaison-managers")
        .then()
        .statusCode(200)
        .body("data.content", hasSize(greaterThanOrEqualTo(1)))
        .body("data.content[0].firstName", equalTo(E2eConfig.lspLiaisonManagerFirstName()))
        .body("data.content[0].lastName", equalTo(E2eConfig.lspLiaisonManagerLastName()))
        .body("data.content[0].emailAddress", equalTo(E2eConfig.lspLiaisonManagerEmailAddress()))
        .body(
            "data.content[0].telephoneNumber",
            equalTo(E2eConfig.lspLiaisonManagerTelephoneNumber()));
  }

  @Test
  void getOfficeLiaisonManagers_unknownFirm_returns404() {
    given()
        .pathParam("firmId", E2eConfig.invalidFirmNumber())
        .pathParam("officeCode", E2eConfig.lspOfficeCode())
        .when()
        .get("/provider-firms/{firmId}/offices/{officeCode}/liaison-managers")
        .then()
        .statusCode(404)
        .body("error.errorCode", equalTo("P00NF"));
  }

  @Test
  void getOfficeLiaisonManagers_unknownOffice_returns404() {
    given()
        .pathParam("firmId", E2eConfig.lspFirmNumber())
        .pathParam("officeCode", E2eConfig.invalidOfficeCode())
        .when()
        .get("/provider-firms/{firmId}/offices/{officeCode}/liaison-managers")
        .then()
        .statusCode(404)
        .body("error.errorCode", equalTo("P00NF"));
  }
}
