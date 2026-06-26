package uk.gov.justice.laa.providerdata.e2e;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

import org.junit.jupiter.api.Test;

@ReadOnlyTest
class ProviderFirmLiaisonManagersE2eTest {

  @Test
  void getOfficeLiaisonManagers_returns200WithContent() {
    given()
        .pathParam("firmId", E2eConfig.lspFirmNumber())
        .pathParam("officeCode", E2eConfig.lspOfficeCode())
        .when()
        .get("/provider-firms/{firmId}/offices/{officeCode}/liaison-managers")
        .then()
        .statusCode(200)
        .body("data.content", hasSize(greaterThanOrEqualTo(1)));
  }

  /**
   * DSTEW-1649 AC1 – View Liaison Manager for an entity.
   *
   * <p>DS_MAPD_FR_014 (DSTEW-1649): The active Liaison Manager entry must contain all required
   * fields: guid, firstName, lastName, emailAddress, telephoneNumber, and activeDateFrom. The
   * active entry must have no activeDateTo.
   */
  @Test
  void dstew1649_ac1_activeLiaisonManager_allRequiredFieldsPresent() {
    given()
        .pathParam("firmId", E2eConfig.lspFirmNumber())
        .pathParam("officeCode", E2eConfig.lspOfficeCode())
        .when()
        .get("/provider-firms/{firmId}/offices/{officeCode}/liaison-managers")
        .then()
        .statusCode(200)
        .body("data.content.find { it.activeDateTo == null }.guid", notNullValue())
        .body("data.content.find { it.activeDateTo == null }.firstName", notNullValue())
        .body("data.content.find { it.activeDateTo == null }.lastName", notNullValue())
        .body("data.content.find { it.activeDateTo == null }.emailAddress", notNullValue())
        .body("data.content.find { it.activeDateTo == null }.telephoneNumber", notNullValue())
        .body("data.content.find { it.activeDateTo == null }.activeDateFrom", notNullValue())
        // Tautological but explicit: DSTEW-1649 AC1 requires the active entry to have no
        // Inactive Date. The find predicate alone is not a sufficient assertion — the field
        // must be absent from the response object itself.
        .body("data.content.find { it.activeDateTo == null }.activeDateTo", nullValue());
  }

  /**
   * DSTEW-1649 AC2 – Liaison Manager not found: unknown firm.
   *
   * <p>DS_MAPD_FR_014 (DSTEW-1649): When the requested entity does not exist in PDP, an appropriate
   * error must be returned. Since every office is created with a Liaison Manager and LMs can only
   * be superseded (never removed), "entity not found" is the only reachable proxy for this AC.
   */
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

  /**
   * DSTEW-1649 AC2 – Liaison Manager not found: unknown office.
   *
   * <p>DS_MAPD_FR_014 (DSTEW-1649): When the requested entity does not exist in PDP, an appropriate
   * error must be returned. Since every office is created with a Liaison Manager and LMs can only
   * be superseded (never removed), "entity not found" is the only reachable proxy for this AC.
   */
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
