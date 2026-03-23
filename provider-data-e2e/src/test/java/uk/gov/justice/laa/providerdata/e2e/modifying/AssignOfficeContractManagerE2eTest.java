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
 * /provider-firms/{firmId}/offices/{officeCode}/contract-managers}.
 *
 * <p>The MVP endpoint requires GUIDs for both office and contract manager identifiers. Tests verify
 * the assignment via GET. Cleanup is handled by {@code delete-test-data.sql} which removes contract
 * manager links for E2E contract managers.
 */
@ModifyingTest
class AssignOfficeContractManagerE2eTest {

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
