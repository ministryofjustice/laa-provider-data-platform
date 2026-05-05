package uk.gov.justice.laa.providerdata.e2e;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Read-only e2e tests for DSTEW-1556: View Chambers record (DS_MAPD_FR_018).
 *
 * <p>Fixture notes:
 *
 * <ul>
 *   <li>The standard seeded Chambers (firmNumber 100002) has no DX fields; the DX-enabled Chambers
 *       (firmNumber 100004) is seeded by {@code LocalDataSeeder} with DX fields.
 *   <li>Both records are pre-existing; no setup or teardown is required.
 * </ul>
 *
 * <p>Test data is managed by {@code LocalDataSeeder}; reset the local database to re-seed.
 */
@ReadOnlyTest
@DisplayName("DSTEW-1556: View Chambers record (DS_MAPD_FR_018)")
class ViewChambersE2eTest {

  /**
   * AC1 - View an existing Chambers record. Verifies both the provider-level and office-level
   * endpoints return all available data for the seeded Chambers (firm 100002, office ACC002).
   */
  @Test
  void dstew1556_ac1_existingChambers_returnsProviderAndOfficeFields() {
    given()
        .pathParam("firmId", E2eConfig.chambersFirmNumber())
        .when()
        .get("/provider-firms/{firmId}")
        .then()
        .statusCode(200)
        .body("data.firmNumber", equalTo(E2eConfig.chambersFirmNumber()))
        .body("data.name", notNullValue())
        .body("data.firmType", equalTo("Chambers"))
        .body("data.chambers.office.accountNumber", equalTo(E2eConfig.chambersOfficeCode()));

    given()
        .pathParam("firmId", E2eConfig.chambersFirmNumber())
        .pathParam("officeCode", E2eConfig.chambersOfficeCode())
        .when()
        .get("/provider-firms/{firmId}/offices/{officeCode}")
        .then()
        .statusCode(200)
        .body("data.address.line1", notNullValue())
        .body("data.address.townOrCity", notNullValue())
        .body("data.address.postcode", notNullValue())
        .body("data.telephoneNumber", notNullValue())
        .body("data.emailAddress", notNullValue())
        .body("data.activeDateTo", nullValue())
        .body("data.dxDetails", nullValue());
  }

  /**
   * AC2 - Display optional DX fields when present. The DX Chambers (100004/ACC004) has both DX
   * Number and DX Centre populated; verifies they are returned in the office response.
   */
  @Test
  void dstew1556_ac2_withDxFields_dxFieldsDisplayed() {
    given()
        .pathParam("firmId", E2eConfig.chambersDxFirmNumber())
        .pathParam("officeCode", E2eConfig.chambersDxOfficeCode())
        .when()
        .get("/provider-firms/{firmId}/offices/{officeCode}")
        .then()
        .statusCode(200)
        .body("data.dxDetails.dxNumber", notNullValue())
        .body("data.dxDetails.dxCentre", notNullValue());
  }

  /**
   * AC2 - Omit optional DX fields when absent. The standard seeded Chambers (100002) has no DX
   * fields; verifies the office is returned successfully with dxDetails absent (null). See also
   * {@link #dstew1556_ac1_existingChambers_returnsProviderAndOfficeFields()} which also covers this
   * case.
   */
  @Test
  void dstew1556_ac2_withoutDxFields_dxFieldsOmittedWithoutError() {
    given()
        .pathParam("firmId", E2eConfig.chambersFirmNumber())
        .pathParam("officeCode", E2eConfig.chambersOfficeCode())
        .when()
        .get("/provider-firms/{firmId}/offices/{officeCode}")
        .then()
        .statusCode(200)
        .body("data.dxDetails", nullValue());
  }

  /** AC3 - Non-existent Chambers returns 404. */
  @Test
  void dstew1556_ac3_nonExistentChambers_returns404() {
    given()
        .pathParam("firmId", E2eConfig.invalidFirmNumber())
        .when()
        .get("/provider-firms/{firmId}")
        .then()
        .statusCode(404)
        .body("error.errorCode", equalTo("P00NF"));
  }
}
