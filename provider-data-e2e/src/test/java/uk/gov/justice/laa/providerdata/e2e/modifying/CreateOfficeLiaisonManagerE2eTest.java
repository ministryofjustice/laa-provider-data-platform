package uk.gov.justice.laa.providerdata.e2e.modifying;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;

import io.restassured.http.ContentType;
import java.util.Map;
import org.junit.jupiter.api.Test;
import uk.gov.justice.laa.providerdata.e2e.E2eConfig;
import uk.gov.justice.laa.providerdata.e2e.ModifyingTest;

/**
 * Data-modifying e2e tests for {@code POST
 * /provider-firms/{firmId}/offices/{officeCode}/liaison-managers}.
 *
 * <p>Tests create new liaison manager records linked to the E2E office and verify via GET. Cleanup
 * is handled by {@code delete-test-data.sql} which removes liaison manager links for E2E offices.
 */
@ModifyingTest
class CreateOfficeLiaisonManagerE2eTest {

  @Test
  void createLiaisonManager_forExistingOffice_returns201ThenGetReturnsCreatedManager() {
    String firstName = "New";
    String lastName = "Liaison";

    Map<String, Object> body =
        Map.of(
            "firstName", firstName,
            "lastName", lastName,
            "emailAddress", "new.liaison." + System.currentTimeMillis() + "@example.com",
            "telephoneNumber", "020 9999 8888");

    given()
        .contentType(ContentType.JSON)
        .pathParam("firmId", E2eConfig.lspFirmNumber())
        .pathParam("officeCode", E2eConfig.lspOfficeCode())
        .body(body)
        .when()
        .post("/provider-firms/{firmId}/offices/{officeCode}/liaison-managers")
        .then()
        .statusCode(201)
        .body("data.providerFirmGUID", notNullValue())
        .body("data.providerFirmNumber", equalTo(E2eConfig.lspFirmNumber()))
        .body("data.officeGUID", notNullValue())
        .body("data.officeCode", equalTo(E2eConfig.lspOfficeCode()))
        .body("data.liaisonManagerGUID", notNullValue());

    // Verify the created liaison manager appears in the GET response
    given()
        .pathParam("firmId", E2eConfig.lspFirmNumber())
        .pathParam("officeCode", E2eConfig.lspOfficeCode())
        .when()
        .get("/provider-firms/{firmId}/offices/{officeCode}/liaison-managers")
        .then()
        .statusCode(200)
        .body("data.content", hasSize(notNullValue()))
        .body("data.content.firstName", hasItem(firstName))
        .body("data.content.lastName", hasItem(lastName));
  }

  @Test
  void createLiaisonManager_unknownFirm_returns404() {
    Map<String, Object> body =
        Map.of(
            "firstName", "Test",
            "lastName", "Person",
            "emailAddress", "test@example.com",
            "telephoneNumber", "020 0000 0000");

    given()
        .contentType(ContentType.JSON)
        .pathParam("firmId", E2eConfig.invalidFirmNumber())
        .pathParam("officeCode", E2eConfig.lspOfficeCode())
        .body(body)
        .when()
        .post("/provider-firms/{firmId}/offices/{officeCode}/liaison-managers")
        .then()
        .statusCode(404);
  }

  @Test
  void createLiaisonManager_unknownOffice_returns404() {
    Map<String, Object> body =
        Map.of(
            "firstName", "Test",
            "lastName", "Person",
            "emailAddress", "test@example.com",
            "telephoneNumber", "020 0000 0000");

    given()
        .contentType(ContentType.JSON)
        .pathParam("firmId", E2eConfig.lspFirmNumber())
        .pathParam("officeCode", E2eConfig.invalidOfficeCode())
        .body(body)
        .when()
        .post("/provider-firms/{firmId}/offices/{officeCode}/liaison-managers")
        .then()
        .statusCode(404);
  }

  @Test
  void createLiaisonManager_missingRequiredFields_returns400() {
    Map<String, Object> body = Map.of("firstName", "Incomplete");

    given()
        .contentType(ContentType.JSON)
        .pathParam("firmId", E2eConfig.lspFirmNumber())
        .pathParam("officeCode", E2eConfig.lspOfficeCode())
        .body(body)
        .when()
        .post("/provider-firms/{firmId}/offices/{officeCode}/liaison-managers")
        .then()
        .statusCode(400);
  }
}
