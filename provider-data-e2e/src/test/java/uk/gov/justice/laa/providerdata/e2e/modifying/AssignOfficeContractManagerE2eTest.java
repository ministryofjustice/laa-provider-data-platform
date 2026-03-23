package uk.gov.justice.laa.providerdata.e2e.modifying;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;

import io.restassured.http.ContentType;
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
 * /provider-firms/{firmId}/offices/{officeCode}/contract-managers}.
 *
 * <p>The MVP endpoint requires GUIDs for both office and contract manager identifiers. Tests verify
 * the assignment via GET and clean up in {@link #afterAll()}.
 */
@ModifyingTest
class AssignOfficeContractManagerE2eTest {

  private static final List<String> createdLinkOfficeGuids = new ArrayList<>();

  @AfterAll
  static void afterAll() throws SQLException {
    if (createdLinkOfficeGuids.isEmpty()) {
      return;
    }
    try (var conn =
            DriverManager.getConnection(
                E2eConfig.dbUrl(), E2eConfig.dbUsername(), E2eConfig.dbPassword());
        var stmt = conn.createStatement()) {
      for (String officeGuid : createdLinkOfficeGuids) {
        // Only delete links created by this test (matching both office and contract manager)
        stmt.execute(
            "DELETE FROM office_contract_manager_link WHERE office_guid = '"
                + officeGuid
                + "' AND contract_manager_guid = '"
                + E2eConfig.contractManagerGuid()
                + "'");
      }
    }
  }

  @Test
  void assignContractManager_forExistingOffice_returns201ThenGetReturnsAssignment() {
    Map<String, Object> body = Map.of("contractManagerGUID", E2eConfig.contractManagerGuid());

    given()
        .contentType(ContentType.JSON)
        .pathParam("firmId", E2eConfig.lspGuid())
        .pathParam("officeCode", E2eConfig.lspOfficeGuid())
        .body(body)
        .when()
        .post("/provider-firms/{firmId}/offices/{officeCode}/contract-managers")
        .then()
        .statusCode(201)
        .body("data.officeGUID", notNullValue())
        .body("data.contractManagerId", equalTo(E2eConfig.contractManagerId()));

    createdLinkOfficeGuids.add(E2eConfig.lspOfficeGuid());

    // Verify the assignment appears in the GET response
    given()
        .pathParam("firmId", E2eConfig.lspGuid())
        .pathParam("officeCode", E2eConfig.lspOfficeGuid())
        .when()
        .get("/provider-firms/{firmId}/offices/{officeCode}/contract-managers")
        .then()
        .statusCode(200)
        .body("data.content", hasSize(notNullValue()))
        .body("data.content.contractManagerId", hasItem(E2eConfig.contractManagerId()));
  }

  @Test
  void assignContractManager_missingContractManagerGuid_returns400() {
    Map<String, Object> body = Map.of();

    given()
        .contentType(ContentType.JSON)
        .pathParam("firmId", E2eConfig.lspGuid())
        .pathParam("officeCode", E2eConfig.lspOfficeGuid())
        .body(body)
        .when()
        .post("/provider-firms/{firmId}/offices/{officeCode}/contract-managers")
        .then()
        .statusCode(400);
  }

  @Test
  void assignContractManager_invalidOfficeGuid_returns400() {
    Map<String, Object> body = Map.of("contractManagerGUID", E2eConfig.contractManagerGuid());

    given()
        .contentType(ContentType.JSON)
        .pathParam("firmId", E2eConfig.lspGuid())
        .pathParam("officeCode", "not-a-uuid")
        .body(body)
        .when()
        .post("/provider-firms/{firmId}/offices/{officeCode}/contract-managers")
        .then()
        .statusCode(400);
  }
}
