package uk.gov.justice.laa.providerdata.e2e;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;

import org.junit.jupiter.api.Test;

@ReadOnlyTest
class ProviderFirmOfficesE2eTest {

  @Test
  void getProviderFirmOffices_returnsPaginatedList() {
    given()
        .pathParam("firmId", E2eConfig.lspFirmNumber())
        .when()
        .get("/provider-firms/{firmId}/offices")
        .then()
        .statusCode(200)
        .body("data.content", hasSize(greaterThanOrEqualTo(1)))
        .body("data.content[0].accountNumber", notNullValue())
        .body("data.content[0].firmType", notNullValue())
        .body("data.metadata.pagination.totalItems", greaterThanOrEqualTo(1))
        .body("data.metadata.searchCriteria.criteria", empty());
  }

  @Test
  void getProviderFirmOffices_unknownFirm_returns404() {
    given()
        .pathParam("firmId", E2eConfig.invalidFirmNumber())
        .when()
        .get("/provider-firms/{firmId}/offices")
        .then()
        .statusCode(404)
        .body("error.errorCode", equalTo("P00NF"));
  }

  @Test
  void getProviderFirmOfficeByCode_returns200WithDetails() {
    given()
        .pathParam("firmId", E2eConfig.lspFirmNumber())
        .pathParam("officeCode", E2eConfig.lspOfficeCode())
        .when()
        .get("/provider-firms/{firmId}/offices/{officeCode}")
        .then()
        .statusCode(200)
        .body("data.accountNumber", notNullValue())
        .body("data.firmType", notNullValue())
        .body("data.address.line1", notNullValue())
        .body("data.address.townOrCity", notNullValue())
        .body("data.address.postcode", notNullValue());
  }

  @Test
  void getProviderFirmOfficeByCode_unknownOfficeCode_returns404() {
    given()
        .pathParam("firmId", E2eConfig.lspFirmNumber())
        .pathParam("officeCode", E2eConfig.invalidOfficeCode())
        .when()
        .get("/provider-firms/{firmId}/offices/{officeCode}")
        .then()
        .statusCode(404)
        .body("error.errorCode", equalTo("P00NF"));
  }

  @Test
  void getProviderFirmOfficeByCode_unknownFirm_returns404() {
    given()
        .pathParam("firmId", E2eConfig.invalidFirmNumber())
        .pathParam("officeCode", E2eConfig.lspOfficeCode())
        .when()
        .get("/provider-firms/{firmId}/offices/{officeCode}")
        .then()
        .statusCode(404)
        .body("error.errorCode", equalTo("P00NF"));
  }
}
