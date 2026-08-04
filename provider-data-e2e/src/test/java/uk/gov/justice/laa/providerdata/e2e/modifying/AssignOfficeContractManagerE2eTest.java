package uk.gov.justice.laa.providerdata.e2e.modifying;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;

import io.restassured.http.ContentType;
import io.restassured.response.ValidatableResponse;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import uk.gov.justice.laa.providerdata.e2e.E2eConfig;
import uk.gov.justice.laa.providerdata.e2e.ModifyingTest;

/**
 * Data-modifying e2e tests for {@code POST
 * /provider-firms/{firmId}/offices/{officeCode}/contract-managers}.
 *
 * <p>The contract manager GUID is looked up dynamically from the API at test startup. After
 * DSTEW-1924, every assignment request must explicitly state which contract manager to assign:
 * either a {@code contractManagerGUID}, {@code useDefaultContractManager: true}, or {@code
 * useHeadOfficeContractManager: true}.
 */
@ModifyingTest
class AssignOfficeContractManagerE2eTest {

  private static final String CONTRACT_MANAGERS_ENDPOINT =
      "/provider-firms/{firmId}/offices/{officeCode}/contract-managers";

  private static String contractManagerGuid;
  private static String defaultContractManagerGuid;

  /** POSTs the given assignment instruction body to the office's contract-managers endpoint. */
  private static ValidatableResponse assignContractManager(
      String firmNumber, String officeCode, Map<String, Object> body) {
    return given()
        .contentType(ContentType.JSON)
        .pathParam("firmId", firmNumber)
        .pathParam("officeCode", officeCode)
        .body(body)
        .when()
        .post(CONTRACT_MANAGERS_ENDPOINT)
        .then();
  }

  /** GETs the contract managers currently assigned to the office. */
  private static ValidatableResponse getContractManagers(String firmNumber, String officeCode) {
    return given()
        .pathParam("firmId", firmNumber)
        .pathParam("officeCode", officeCode)
        .when()
        .get(CONTRACT_MANAGERS_ENDPOINT)
        .then();
  }

  /** Snapshots the contract manager IDs currently assigned to the office, for later comparison. */
  private static List<String> snapshotContractManagerIds(String firmNumber, String officeCode) {
    return getContractManagers(firmNumber, officeCode)
        .statusCode(200)
        .extract()
        .path("data.content.contractManagerId");
  }

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

    // Default ("Mr Default") contract manager GUID — used as the second, distinct CM in the
    // replacement test below so we can prove the prior assignment was removed rather than added to.
    defaultContractManagerGuid =
        given()
            .queryParam("contractManagerId", E2eConfig.defaultContractManagerId())
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

    assignContractManager(E2eConfig.lspFirmNumber(), E2eConfig.lspOfficeCode(), body)
        .statusCode(201)
        .body("data.officeGUID", notNullValue())
        .body("data.contractManagerId", equalTo(E2eConfig.contractManagerId()));

    // DSTEW-1660/DSTEW-1661 AC1: assignment appears in the GET response.
    // DSTEW-1660/DSTEW-1661 AC3: exactly one contract manager is assigned — the service enforces
    // at-most-one per office.
    getContractManagers(E2eConfig.lspFirmNumber(), E2eConfig.lspOfficeCode())
        .statusCode(200)
        .body("data.content", hasSize(equalTo(1)))
        .body("data.content.contractManagerId", hasItem(E2eConfig.contractManagerId()));
  }

  /**
   * DSTEW-1924 AC1: verifies that a request with no contract manager instruction (empty body) is
   * rejected with 400. After DSTEW-1924, omitting the instruction is no longer silently defaulted.
   *
   * <p>Also confirms the existing contract manager assignment is not mutated by the failed request.
   */
  @Test
  void
      dstew1924_ac1_assignContractManager_noInstructionProvided_returns400AndAssignmentUnchanged() {
    // Snapshot the existing contract manager state before the failed attempt.
    List<String> originalContractManagerIds =
        snapshotContractManagerIds(E2eConfig.lspFirmNumber(), E2eConfig.lspOfficeCode());

    given()
        .contentType(ContentType.JSON)
        .pathParam("firmId", E2eConfig.lspFirmNumber())
        .pathParam("officeCode", E2eConfig.lspOfficeCode())
        .body("{}")
        .when()
        .post(CONTRACT_MANAGERS_ENDPOINT)
        .then()
        .statusCode(400);

    // Assert the office's contract manager state is identical to the pre-request snapshot.
    getContractManagers(E2eConfig.lspFirmNumber(), E2eConfig.lspOfficeCode())
        .statusCode(200)
        .body("data.content", hasSize(originalContractManagerIds.size()))
        .body("data.content.contractManagerId", equalTo(originalContractManagerIds));
  }

  /**
   * DSTEW-1660 AC2 / DSTEW-1661 AC2 / DSTEW-1924 AC2: verifies that {@code
   * useDefaultContractManager: true} assigns the system default contract manager ("Mr Default") and
   * returns 201.
   *
   * <p>DSTEW-1660 AC2 and DSTEW-1661 AC2 described this as "no CM provided → assign Mr Default";
   * DSTEW-1924 reinterprets that as an explicit {@code useDefaultContractManager: true}
   * instruction, which is what is tested here.
   */
  @Test
  void dstew1660_ac2_assignContractManager_useDefault_assignsDefaultContractManager() {
    assignContractManager(
            E2eConfig.lspFirmNumber(),
            E2eConfig.lspOfficeCode(),
            Map.of("useDefaultContractManager", true))
        .statusCode(201)
        .body("data.officeGUID", notNullValue())
        .body("data.contractManagerId", equalTo(E2eConfig.defaultContractManagerId()));

    getContractManagers(E2eConfig.lspFirmNumber(), E2eConfig.lspOfficeCode())
        .statusCode(200)
        .body("data.content", hasSize(equalTo(1)))
        .body("data.content[0].contractManagerId", equalTo(E2eConfig.defaultContractManagerId()));
  }

  /**
   * DSTEW-1661 AC2 / DSTEW-1661 AC3 / DSTEW-1924 AC2: verifies that an existing Contract Manager
   * assignment can be replaced by specifying {@code useDefaultContractManager: true}, and that
   * after the replacement exactly one Contract Manager remains assigned (the default).
   *
   * <p>DSTEW-1661 AC2 described this scenario as "remove the CM without providing a replacement";
   * DSTEW-1924 reinterprets that as an explicit {@code useDefaultContractManager: true}
   * instruction, which is what is tested here.
   */
  @Test
  void dstew1661_ac2_replaceExistingCm_withDefault_onlyDefaultRemains() {
    // Establish a known non-default CM on the office.
    assignContractManager(
            E2eConfig.lspFirmNumber(),
            E2eConfig.lspOfficeCode(),
            Map.of("contractManagerGUID", contractManagerGuid))
        .statusCode(201)
        .body("data.contractManagerId", equalTo(E2eConfig.contractManagerId()));

    // Replace it with the system default using an explicit instruction.
    assignContractManager(
            E2eConfig.lspFirmNumber(),
            E2eConfig.lspOfficeCode(),
            Map.of("useDefaultContractManager", true))
        .statusCode(201)
        .body("data.contractManagerId", equalTo(E2eConfig.defaultContractManagerId()));

    // DSTEW-1661 AC3: exactly one CM is assigned — the previous assignment was replaced.
    getContractManagers(E2eConfig.lspFirmNumber(), E2eConfig.lspOfficeCode())
        .statusCode(200)
        .body("data.content", hasSize(equalTo(1)))
        .body("data.content[0].contractManagerId", equalTo(E2eConfig.defaultContractManagerId()));
  }

  /**
   * DSTEW-1924 AC4: verifies that providing both {@code contractManagerGUID} and {@code
   * useDefaultContractManager: true} in the same request is rejected with 400.
   *
   * <p>Also confirms the existing contract manager assignment is not mutated by the failed request.
   */
  @Test
  void
      dstew1924_ac4_assignContractManager_conflictingInstructions_returns400AndAssignmentUnchanged() {
    // Snapshot the existing contract manager state before the failed attempt.
    List<String> originalContractManagerIds =
        snapshotContractManagerIds(E2eConfig.lspFirmNumber(), E2eConfig.lspOfficeCode());

    assignContractManager(
            E2eConfig.lspFirmNumber(),
            E2eConfig.lspOfficeCode(),
            Map.of("contractManagerGUID", contractManagerGuid, "useDefaultContractManager", true))
        .statusCode(400);

    // Assert the office's contract manager state is identical to the pre-request snapshot.
    getContractManagers(E2eConfig.lspFirmNumber(), E2eConfig.lspOfficeCode())
        .statusCode(200)
        .body("data.content", hasSize(originalContractManagerIds.size()))
        .body("data.content.contractManagerId", equalTo(originalContractManagerIds));
  }

  /**
   * DSTEW-1924 AC5: verifies that {@code useHeadOfficeContractManager: true} assigns the contract
   * manager currently linked to the LSP2 head office to the LSP2 child office, returning 201.
   *
   * <p>Sets up a known head-office CM state before exercising the child-office trickle-down.
   */
  @Test
  void dstew1924_ac5_assignContractManager_useHeadOffice_assignsHeadOfficeCm() {
    // Establish a known CM on the LSP2 head office.
    assignContractManager(
            E2eConfig.lsp2FirmNumber(),
            E2eConfig.lsp2HeadOfficeCode(),
            Map.of("contractManagerGUID", contractManagerGuid))
        .statusCode(201);

    // Assign via head-office trickle-down to the child office.
    assignContractManager(
            E2eConfig.lsp2FirmNumber(),
            E2eConfig.lsp2ChildOfficeCode(),
            Map.of("useHeadOfficeContractManager", true))
        .statusCode(201)
        .body("data.officeGUID", notNullValue())
        .body("data.contractManagerId", equalTo(E2eConfig.contractManagerId()));

    // Verify the assignment is persisted and exactly one CM is assigned to the child office.
    getContractManagers(E2eConfig.lsp2FirmNumber(), E2eConfig.lsp2ChildOfficeCode())
        .statusCode(200)
        .body("data.content", hasSize(equalTo(1)))
        .body("data.content[0].contractManagerId", equalTo(E2eConfig.contractManagerId()));
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
        snapshotContractManagerIds(E2eConfig.lspFirmNumber(), E2eConfig.lspOfficeCode());

    // A well-formed UUID that is guaranteed not to exist in the dataset
    Map<String, Object> body =
        Map.of("contractManagerGUID", "00000000-0000-0000-0000-000000000000");

    assignContractManager(E2eConfig.lspFirmNumber(), E2eConfig.lspOfficeCode(), body)
        .statusCode(400);

    // Assert the office's contract manager state is identical to the pre-request snapshot —
    // the failed POST must not have mutated the existing assignment.
    getContractManagers(E2eConfig.lspFirmNumber(), E2eConfig.lspOfficeCode())
        .statusCode(200)
        .body("data.content", hasSize(originalContractManagerIds.size()))
        .body("data.content.contractManagerId", equalTo(originalContractManagerIds));
  }

  /**
   * DSTEW-1660/DSTEW-1661 AC4: a value that is not a valid UUID must be rejected with 400 Bad
   * Request — Jackson fails to deserialise the field and Spring MVC returns a 400 before the
   * controller is invoked.
   *
   * <p>Also confirms the existing contract manager assignment is not mutated by the failed request.
   */
  @Test
  void assignContractManager_malformedContractManagerGuid_returns400AndAssignmentUnchanged() {
    // Snapshot the existing contract manager state before the failed attempt.
    List<String> originalContractManagerIds =
        snapshotContractManagerIds(E2eConfig.lspFirmNumber(), E2eConfig.lspOfficeCode());

    assignContractManager(
            E2eConfig.lspFirmNumber(),
            E2eConfig.lspOfficeCode(),
            Map.of("contractManagerGUID", "not-a-valid-uuid"))
        .statusCode(400);

    // Assert the office's contract manager state is identical to the pre-request snapshot.
    getContractManagers(E2eConfig.lspFirmNumber(), E2eConfig.lspOfficeCode())
        .statusCode(200)
        .body("data.content", hasSize(originalContractManagerIds.size()))
        .body("data.content.contractManagerId", equalTo(originalContractManagerIds));
  }

  /**
   * AC1 + AC3: assigning a different contract manager to an office that already has one replaces
   * the existing assignment (rather than appending). After the second POST, the GET response must
   * contain exactly one contract manager — the new one — proving the previous assignment was
   * removed.
   */
  @Test
  void assignContractManager_replacesExistingAssignment_onlyNewRemains() {
    // Establish a known starting state: assign the configured contract manager.
    assignContractManager(
            E2eConfig.lspFirmNumber(),
            E2eConfig.lspOfficeCode(),
            Map.of("contractManagerGUID", contractManagerGuid))
        .statusCode(201)
        .body("data.contractManagerId", equalTo(E2eConfig.contractManagerId()));

    // Replace with a different contract manager (the system default).
    assignContractManager(
            E2eConfig.lspFirmNumber(),
            E2eConfig.lspOfficeCode(),
            Map.of("contractManagerGUID", defaultContractManagerGuid))
        .statusCode(201)
        .body("data.contractManagerId", equalTo(E2eConfig.defaultContractManagerId()));

    // DSTEW-1660/DSTEW-1661 AC1: only the new assignment is present (the original CM has been
    // replaced).
    // DSTEW-1660/DSTEW-1661 AC3: exactly one contract manager is assigned to the entity after the
    // change completes.
    getContractManagers(E2eConfig.lspFirmNumber(), E2eConfig.lspOfficeCode())
        .statusCode(200)
        .body("data.content", hasSize(equalTo(1)))
        .body("data.content[0].contractManagerId", equalTo(E2eConfig.defaultContractManagerId()));
  }

  /**
   * DSTEW-1660/DSTEW-1661 AC4: when an invalid contract manager identifier is submitted, the
   * existing assignment must remain unchanged. Establishes a known assignment, sends a request with
   * an unknown GUID that the service rejects with 400, then asserts the original contract manager
   * is still the one assigned.
   */
  @Test
  void assignContractManager_invalidGuid_existingAssignmentUnchanged() {
    // Establish a known good assignment so we have something whose preservation we can verify.
    assignContractManager(
            E2eConfig.lspFirmNumber(),
            E2eConfig.lspOfficeCode(),
            Map.of("contractManagerGUID", contractManagerGuid))
        .statusCode(201);

    // Submit a syntactically valid but unknown GUID — service rejects with 400.
    assignContractManager(
            E2eConfig.lspFirmNumber(),
            E2eConfig.lspOfficeCode(),
            Map.of("contractManagerGUID", "00000000-0000-0000-0000-000000000000"))
        .statusCode(400);

    // DSTEW-1660/DSTEW-1661 AC4: the previously assigned contract manager is still the one assigned
    // — the failed request
    // did not remove or overwrite the existing link.
    getContractManagers(E2eConfig.lspFirmNumber(), E2eConfig.lspOfficeCode())
        .statusCode(200)
        .body("data.content", hasSize(equalTo(1)))
        .body("data.content[0].contractManagerId", equalTo(E2eConfig.contractManagerId()));
  }

  // TODO: Add unknownOfficeCode_returns404 test once the OpenAPI spec defines 404 for this
  // endpoint.

  /**
   * DSTEW-1904 AC1: a {@code contractManagerGUID} consisting only of whitespace must be rejected
   * with 400 Bad Request, not a 500 Internal Server Error. No {@code useDefaultContractManager} or
   * {@code useHeadOfficeContractManager} instruction is present, so a blank GUID leaves the request
   * with no valid instruction at all — the same outcome as omitting the field entirely.
   *
   * <p>Also confirms the existing contract manager assignment is not mutated by the failed request.
   */
  @Test
  void dstew1904_ac1_assignContractManager_blankGuid_returns400AndAssignmentUnchanged() {
    assertContractManagerGuidRejectedWith400("   ");
  }

  /**
   * DSTEW-1904 AC1: an empty-string {@code contractManagerGUID} must likewise be rejected with 400
   * Bad Request rather than being silently treated as "no instruction provided" resulting in a
   * different error, or causing a server error.
   *
   * <p>Also confirms the existing contract manager assignment is not mutated by the failed request.
   */
  @Test
  void dstew1904_ac1_assignContractManager_emptyGuid_returns400AndAssignmentUnchanged() {
    assertContractManagerGuidRejectedWith400("");
  }

  /**
   * DSTEW-1904: an explicit {@code contractManagerGUID: null} must be rejected with 400 Bad
   * Request, consistent with blank and empty string values, and with the field being omitted
   * entirely (covered by the "no instruction provided" test above).
   *
   * <p>Also confirms the existing contract manager assignment is not mutated by the failed request.
   */
  @Test
  void dstew1904_ac1_assignContractManager_nullGuid_returns400AndAssignmentUnchanged() {
    assertContractManagerGuidRejectedWith400(null);
  }

  /**
   * Shared assertion for DSTEW-1904: submits the given {@code contractManagerGUID} value (blank,
   * empty, or {@code null}), asserts 400 Bad Request, and confirms the existing contract manager
   * assignment is not mutated.
   */
  private static void assertContractManagerGuidRejectedWith400(
      @Nullable String contractManagerGuidValue) {
    // Uses a mutable map since {@link Map#of} rejects null values.
    Map<String, Object> body = new HashMap<>();
    body.put("contractManagerGUID", contractManagerGuidValue);

    // Snapshot the existing contract manager state before the failed attempt.
    List<String> originalContractManagerIds =
        snapshotContractManagerIds(E2eConfig.lspFirmNumber(), E2eConfig.lspOfficeCode());

    assignContractManager(E2eConfig.lspFirmNumber(), E2eConfig.lspOfficeCode(), body)
        .statusCode(400);

    // Assert the office's contract manager state is identical to the pre-request snapshot.
    getContractManagers(E2eConfig.lspFirmNumber(), E2eConfig.lspOfficeCode())
        .statusCode(200)
        .body("data.content", hasSize(originalContractManagerIds.size()))
        .body("data.content.contractManagerId", equalTo(originalContractManagerIds));
  }
}
