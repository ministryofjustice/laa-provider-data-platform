package uk.gov.justice.laa.providerdata.e2e.modifying;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;

import io.restassured.http.ContentType;
import io.restassured.response.Response;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import uk.gov.justice.laa.providerdata.e2e.E2eConfig;
import uk.gov.justice.laa.providerdata.e2e.ModifyingTest;

/**
 * Data-modifying e2e tests for {@code POST
 * /provider-firms/{firmId}/offices/{officeCode}/liaison-managers}.
 *
 * <p>Tests create new liaison manager records linked to the E2E office, verify via GET, and clean up
 * in {@link #afterAll()}.
 */
@ModifyingTest
class CreateOfficeLiaisonManagerE2eTest {

  private static final List<String> createdLiaisonManagerGuids = new ArrayList<>();

  @AfterAll
  static void afterAll() throws SQLException {
    if (createdLiaisonManagerGuids.isEmpty()) {
      return;
    }
    try (var conn =
            DriverManager.getConnection(
                E2eConfig.dbUrl(), E2eConfig.dbUsername(), E2eConfig.dbPassword());
        var stmt = conn.createStatement()) {
      for (String guid : createdLiaisonManagerGuids) {
        stmt.execute(
            "DELETE FROM office_liaison_manager_link WHERE liaison_manager_guid = '"
                + guid
                + "'");
        stmt.execute("DELETE FROM liaison_manager WHERE guid = '" + guid + "'");
      }
    }
  }

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

    Response response =
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
            .body("data.liaisonManagerGUID", notNullValue())
            .extract()
            .response();

    String liaisonManagerGuid = response.path("data.liaisonManagerGUID");
    createdLiaisonManagerGuids.add(liaisonManagerGuid);

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
