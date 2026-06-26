package uk.gov.justice.laa.providerdata.e2e.modifying;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;

import io.restassured.http.ContentType;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import uk.gov.justice.laa.providerdata.e2e.E2eConfig;
import uk.gov.justice.laa.providerdata.e2e.ModifyingTest;

/**
 * Data-modifying e2e tests for {@code POST
 * /provider-firms/{firmId}/offices/{officeCode}/contract-managers}.
 *
 * <p>The contract manager GUID is looked up dynamically from the API at test startup. When no GUID
 * is supplied the service falls back to the system default contract manager seeded by the V4
 * migration.
 */
@ModifyingTest
class AssignOfficeContractManagerE2eTest {

  private static String contractManagerGuid;

  @BeforeAll
  static void lookUpGuids() {
    contractManagerGuid =
        given()
            .queryParam("contractManagerId", E2eConfig.contractManagerId())
            .when()
            .get("/provider-contract-managers")
            .then()
            .statusCode(200)
            .extract()
            .path("data.content[0].guid");
  }

  /**
   * DSTEW-1660/DSTEW-1661 AC1: verifies a valid contract manager GUID is accepted and the
   * assignment is recorded.
   *
   * <p>DSTEW-1660/DSTEW-1661 AC3: verifies the GET response contains exactly one contract manager
   * after the assignment, confirming only one assignment is held per office.
   */
  @Test
  void assignContractManager_forExistingOffice_returns201ThenGetReturnsAssignment() {
    Map<String, Object> body = Map.of("contractManagerGUID", contractManagerGuid);

    given()
        .contentType(ContentType.JSON)
        .pathParam("firmId", E2eConfig.lspFirmNumber())
        .pathParam("officeCode", E2eConfig.lspOfficeCode())
        .body(body)
        .when()
        .post("/provider-firms/{firmId}/offices/{officeCode}/contract-managers")
        .then()
        .statusCode(201)
        .body("data.officeGUID", notNullValue())
        .body("data.contractManagerId", equalTo(E2eConfig.contractManagerId()));

    // DSTEW-1660/DSTEW-1661 AC1: assignment appears in the GET response.
    // DSTEW-1660/DSTEW-1661 AC3: exactly one contract manager is assigned — the service enforces
    // at-most-one per office.
    given()
        .pathParam("firmId", E2eConfig.lspFirmNumber())
        .pathParam("officeCode", E2eConfig.lspOfficeCode())
        .when()
        .get("/provider-firms/{firmId}/offices/{officeCode}/contract-managers")
        .then()
        .statusCode(200)
        .body("data.content", hasSize(equalTo(1)))
        .body("data.content.contractManagerId", hasItem(E2eConfig.contractManagerId()));
  }

  /**
   * DSTEW-1660/DSTEW-1661 AC2: verifies that omitting {@code contractManagerGUID} from the request
   * body causes the service to assign the system default contract manager and returns HTTP 201.
   */
  @Test
  void assignContractManager_noGuidProvided_assignsDefaultContractManager() {
    given()
        .contentType(ContentType.JSON)
        .pathParam("firmId", E2eConfig.lspFirmNumber())
        .pathParam("officeCode", E2eConfig.lspOfficeCode())
        .body("{}")
        .when()
        .post("/provider-firms/{firmId}/offices/{officeCode}/contract-managers")
        .then()
        .statusCode(201)
        .body("data.officeGUID", notNullValue())
        .body("data.contractManagerId", equalTo(E2eConfig.defaultContractManagerId()));
  }

  /**
   * DSTEW-1660/DSTEW-1661 AC4: a syntactically valid UUID that does not correspond to any contract
   * manager must be rejected with 400 Bad Request — the service throws {@link
   * IllegalArgumentException} for an unknown GUID, which the global exception handler maps to 400.
   *
   * <p>Also confirms DSTEW-1660/DSTEW-1661 AC3 is not violated: the LSP office's existing contract
   * manager assignment is not mutated by the failed request.
   */
  @Test
  void assignContractManager_unknownContractManagerGuid_returns400() {
    // Snapshot the existing contract manager state before the failed attempt.
    List<String> originalContractManagerIds =
        given()
            .pathParam("firmId", E2eConfig.lspFirmNumber())
            .pathParam("officeCode", E2eConfig.lspOfficeCode())
            .when()
            .get("/provider-firms/{firmId}/offices/{officeCode}/contract-managers")
            .then()
            .statusCode(200)
            .extract()
            .path("data.content.contractManagerId");

    // A well-formed UUID that is guaranteed not to exist in the dataset
    Map<String, Object> body =
        Map.of("contractManagerGUID", "00000000-0000-0000-0000-000000000000");

    given()
        .contentType(ContentType.JSON)
        .pathParam("firmId", E2eConfig.lspFirmNumber())
        .pathParam("officeCode", E2eConfig.lspOfficeCode())
        .body(body)
        .when()
        .post("/provider-firms/{firmId}/offices/{officeCode}/contract-managers")
        .then()
        .statusCode(400);

    // Assert the office's contract manager state is identical to the pre-request snapshot —
    // the failed POST must not have mutated the existing assignment.
    given()
        .pathParam("firmId", E2eConfig.lspFirmNumber())
        .pathParam("officeCode", E2eConfig.lspOfficeCode())
        .when()
        .get("/provider-firms/{firmId}/offices/{officeCode}/contract-managers")
        .then()
        .statusCode(200)
        .body("data.content", hasSize(originalContractManagerIds.size()))
        .body("data.content.contractManagerId", equalTo(originalContractManagerIds));
  }

  /**
   * DSTEW-1660/DSTEW-1661 AC4: a value that is not a valid UUID must be rejected with 400 Bad
   * Request — Jackson fails to deserialise the field and Spring MVC returns a 400 before the
   * controller is invoked.
   */
  @Test
  void assignContractManager_malformedContractManagerGuid_returns400() {
    Map<String, Object> body = Map.of("contractManagerGUID", "not-a-valid-uuid");

    given()
        .contentType(ContentType.JSON)
        .pathParam("firmId", E2eConfig.lspFirmNumber())
        .pathParam("officeCode", E2eConfig.lspOfficeCode())
        .body(body)
        .when()
        .post("/provider-firms/{firmId}/offices/{officeCode}/contract-managers")
        .then()
        .statusCode(400);
  }

  // TODO: Add unknownOfficeCode_returns404 test once the OpenAPI spec defines 404 for this
  // endpoint.

}
