package uk.gov.justice.laa.providerdata.e2e.modifying;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

import io.restassured.http.ContentType;
import io.restassured.response.Response;
import java.util.Map;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import uk.gov.justice.laa.providerdata.e2e.ModifyingTest;

/**
 * Data-modifying e2e tests for changing the Liaison Manager of an existing office (DSTEW-1652,
 * DS_MAPD_FR_025).
 *
 * <p>A fresh LSP firm (name prefixed {@code "E2E-DSTEW-1652 "}) with two offices is created once in
 * {@link #createTestFirm()}. Tests share the firm but each operates independently.
 *
 * <p>AC4 – atomicity – is also covered by the {@code @Transactional} integration test in {@code
 * OfficeLiaisonManagerServiceTest}, which is a more effective test as it can observe behaviour
 * within a single transaction.
 */
@ModifyingTest
@DisplayName("DSTEW-1652: Change Liaison Manager (DS_MAPD_FR_025)")
class ChangeLiaisonManagerE2eTest {

  private static String firmNumber;
  private static String headOfficeCode;
  private static String secondOfficeCode;

  @BeforeAll
  static void createTestFirm() {
    firmNumber =
        given()
            .contentType(ContentType.JSON)
            .body(
                Map.of(
                    "firmType",
                    "Legal Services Provider",
                    "name",
                    "E2E-DSTEW-1652 LSP " + System.currentTimeMillis(),
                    "legalServicesProvider",
                    Map.of(
                        "constitutionalStatus",
                        "Partnership",
                        "address",
                        Map.of(
                            "line1", "1 E2E Test Street",
                            "townOrCity", "London",
                            "postcode", "SW1A 1AA"),
                        "payment",
                        Map.of("paymentMethod", "CHECK"),
                        "contractManager",
                        Map.of("contractManagerGUID", "12345678-1234-1234-1234-123456789012"),
                        "liaisonManager",
                        Map.of(
                            "firstName", "Initial",
                            "lastName", "Manager",
                            "emailAddress", "e2e.initial@example.com",
                            "telephoneNumber", "020 1111 2222"))))
            .when()
            .post("/provider-firms")
            .then()
            .statusCode(201)
            .extract()
            .path("data.providerFirmNumber");

    headOfficeCode =
        given()
            .pathParam("firmId", firmNumber)
            .when()
            .get("/provider-firms/{firmId}/offices")
            .then()
            .statusCode(200)
            .extract()
            .path("data.content[0].accountNumber");

    secondOfficeCode =
        given()
            .contentType(ContentType.JSON)
            .pathParam("firmId", firmNumber)
            .body(
                Map.of(
                    "address",
                    Map.of(
                        "line1", "2 E2E Test Street",
                        "townOrCity", "Manchester",
                        "postcode", "M1 1AA"),
                    "payment",
                    Map.of("paymentMethod", "CHECK"),
                    "liaisonManager",
                    Map.of(
                        "firstName", "Second",
                        "lastName", "Office Manager",
                        "emailAddress", "e2e.second@example.com",
                        "telephoneNumber", "0161 111 2222")))
            .when()
            .post("/provider-firms/{firmId}/offices")
            .then()
            .statusCode(201)
            .extract()
            .path("data.officeCode");
  }

  /**
   * AC1 – Replace the Liaison Manager with an existing Liaison Manager identified by GUID.
   *
   * <p>Creates a new LM on the head office to obtain a known GUID, then links that LM to the second
   * office. Verifies that exactly one active assignment exists on the second office after the link,
   * and that it is the expected manager.
   */
  @Test
  @DisplayName("AC1: Replace with existing LM by GUID")
  void dstew1652_ac1_replaceWithExistingByGuid_endsOldAndActivatesNew() {
    // Create a new LM on the head office to obtain a known GUID.
    String lmGuid =
        given()
            .contentType(ContentType.JSON)
            .pathParam("firmId", firmNumber)
            .pathParam("officeCode", headOfficeCode)
            .body(
                Map.of(
                    "firstName", "Replacement",
                    "lastName", "By Guid",
                    "emailAddress",
                        "e2e.replacement.guid." + System.currentTimeMillis() + "@example.com",
                    "telephoneNumber", "020 2222 3333"))
            .when()
            .post("/provider-firms/{firmId}/offices/{officeCode}/liaison-managers")
            .then()
            .statusCode(201)
            .body("data.liaisonManagerGUID", notNullValue())
            .extract()
            .path("data.liaisonManagerGUID");

    // Capture the outgoing LM on the second office before the replacement.
    String outgoingGuid =
        given()
            .pathParam("firmId", firmNumber)
            .pathParam("officeCode", secondOfficeCode)
            .when()
            .get("/provider-firms/{firmId}/offices/{officeCode}/liaison-managers")
            .then()
            .statusCode(200)
            .extract()
            .path("data.content.find { it.activeDateTo == null }.guid");

    // Link that LM to the second office by GUID.
    given()
        .contentType(ContentType.JSON)
        .pathParam("firmId", firmNumber)
        .pathParam("officeCode", secondOfficeCode)
        .body(Map.of("liaisonManagerGUID", lmGuid))
        .when()
        .post("/provider-firms/{firmId}/offices/{officeCode}/liaison-managers")
        .then()
        .statusCode(201)
        .body("data.liaisonManagerGUID", equalTo(lmGuid));

    // AC1 + AC5: exactly one active assignment on the second office, and it is the linked LM.
    // And: the specific outgoing LM has its Inactive Date set and is retained as historical
    // (if it were deleted the path expression would return null and the assertion would fail).
    given()
        .pathParam("firmId", firmNumber)
        .pathParam("officeCode", secondOfficeCode)
        .when()
        .get("/provider-firms/{firmId}/offices/{officeCode}/liaison-managers")
        .then()
        .statusCode(200)
        .body("data.content.findAll { it.activeDateTo == null }.size()", equalTo(1))
        .body("data.content.find { it.activeDateTo == null }.guid", equalTo(lmGuid))
        .body(
            "data.content.find { it.guid == '" + outgoingGuid + "' }.activeDateTo", notNullValue());
  }

  /**
   * AC2 – Replace the Liaison Manager with a newly created Liaison Manager.
   *
   * <p>The outgoing assignment must be ended and retained in history; the new assignment must be
   * the only active one after the replacement.
   */
  @Test
  @DisplayName("AC2: Replace with newly created LM")
  void dstew1652_ac2_replaceWithNewLm_endsOldAndActivatesNew() {
    String newFirstName = "New";
    String newLastName = "Liaison Manager";

    // Capture the outgoing LM on the head office before the replacement.
    String outgoingGuid =
        given()
            .pathParam("firmId", firmNumber)
            .pathParam("officeCode", headOfficeCode)
            .when()
            .get("/provider-firms/{firmId}/offices/{officeCode}/liaison-managers")
            .then()
            .statusCode(200)
            .extract()
            .path("data.content.find { it.activeDateTo == null }.guid");

    // Create the new LM and capture its GUID so the follow-up GET can verify by GUID.
    String newGuid =
        given()
            .contentType(ContentType.JSON)
            .pathParam("firmId", firmNumber)
            .pathParam("officeCode", headOfficeCode)
            .body(
                Map.of(
                    "firstName",
                    newFirstName,
                    "lastName",
                    newLastName,
                    "emailAddress",
                    "e2e.new.lm." + System.currentTimeMillis() + "@example.com",
                    "telephoneNumber",
                    "020 4444 5555"))
            .when()
            .post("/provider-firms/{firmId}/offices/{officeCode}/liaison-managers")
            .then()
            .statusCode(201)
            .body("data.liaisonManagerGUID", notNullValue())
            .extract()
            .path("data.liaisonManagerGUID");

    // AC2 + AC5: only one active assignment, and it is the newly created manager.
    // Verify by GUID (not just name) to confirm the LM just created is the active one.
    // And: the specific outgoing LM has its Inactive Date set and is retained as historical
    // (if it were deleted the path expression would return null and the assertion would fail).
    given()
        .pathParam("firmId", firmNumber)
        .pathParam("officeCode", headOfficeCode)
        .when()
        .get("/provider-firms/{firmId}/offices/{officeCode}/liaison-managers")
        .then()
        .statusCode(200)
        .body("data.content.findAll { it.activeDateTo == null }.size()", equalTo(1))
        .body("data.content.find { it.activeDateTo == null }.guid", equalTo(newGuid))
        .body("data.content.find { it.activeDateTo == null }.firstName", equalTo(newFirstName))
        .body("data.content.find { it.activeDateTo == null }.lastName", equalTo(newLastName))
        .body(
            "data.content.find { it.guid == '" + outgoingGuid + "' }.activeDateTo", notNullValue());
  }

  /**
   * AC3 – Mandatory details required when replacing with a new Liaison Manager.
   *
   * <p>Each of the four mandatory fields (first name, last name, telephone number, email address)
   * is tested in isolation. A request with a missing mandatory field must be rejected with HTTP
   * 400, no new LM record must be created, and the existing active assignment must remain
   * unchanged.
   */
  @Test
  @DisplayName("AC3a: Missing first name rejected, existing assignment unchanged")
  void dstew1652_ac3a_missingFirstName_rejectedAndExistingAssignmentUnchanged() {
    assertRejectedAndAssignmentUnchanged(
        Map.of(
            "lastName", "Test",
            "telephoneNumber", "020 1111 2222",
            "emailAddress", "e2e.ac3@example.com"));
  }

  @Test
  @DisplayName("AC3b: Missing last name rejected, existing assignment unchanged")
  void dstew1652_ac3b_missingLastName_rejectedAndExistingAssignmentUnchanged() {
    assertRejectedAndAssignmentUnchanged(
        Map.of(
            "firstName", "Test",
            "telephoneNumber", "020 1111 2222",
            "emailAddress", "e2e.ac3@example.com"));
  }

  @Test
  @DisplayName("AC3c: Missing telephone number rejected, existing assignment unchanged")
  void dstew1652_ac3c_missingTelephoneNumber_rejectedAndExistingAssignmentUnchanged() {
    assertRejectedAndAssignmentUnchanged(
        Map.of(
            "firstName", "Test",
            "lastName", "Test",
            "emailAddress", "e2e.ac3@example.com"));
  }

  @Test
  @DisplayName("AC3d: Missing email address rejected, existing assignment unchanged")
  void dstew1652_ac3d_missingEmailAddress_rejectedAndExistingAssignmentUnchanged() {
    assertRejectedAndAssignmentUnchanged(
        Map.of(
            "firstName", "Test",
            "lastName", "Test",
            "telephoneNumber", "020 1111 2222"));
  }

  /**
   * Sends {@code body} as a POST to replace the head office LM, asserts HTTP 400, and then verifies
   * that the existing active assignment is entirely unchanged (same GUID, same total record count —
   * no partial record was created).
   */
  private void assertRejectedAndAssignmentUnchanged(Map<String, ?> body) {
    Response pre =
        given()
            .pathParam("firmId", firmNumber)
            .pathParam("officeCode", headOfficeCode)
            .when()
            .get("/provider-firms/{firmId}/offices/{officeCode}/liaison-managers")
            .then()
            .statusCode(200)
            .extract()
            .response();

    String existingLmGuid = pre.path("data.content.find { it.activeDateTo == null }.guid");
    int existingCount = pre.<Integer>path("data.content.size()");

    given()
        .contentType(ContentType.JSON)
        .pathParam("firmId", firmNumber)
        .pathParam("officeCode", headOfficeCode)
        .body(body)
        .when()
        .post("/provider-firms/{firmId}/offices/{officeCode}/liaison-managers")
        .then()
        .statusCode(400);

    given()
        .pathParam("firmId", firmNumber)
        .pathParam("officeCode", headOfficeCode)
        .when()
        .get("/provider-firms/{firmId}/offices/{officeCode}/liaison-managers")
        .then()
        .statusCode(200)
        .body("data.content.findAll { it.activeDateTo == null }.size()", equalTo(1))
        .body("data.content.find { it.activeDateTo == null }.guid", equalTo(existingLmGuid))
        .body("data.content.size()", equalTo(existingCount));
  }

  /**
   * AC4 – Entity must never be left without a Liaison Manager.
   *
   * <p>Verifies at the API level that the entity has exactly one active Liaison Manager both before
   * and after the replacement. The {@code @Transactional} integration test in {@code
   * OfficeLiaisonManagerServiceTest} is a more effective test of true atomicity, as it can observe
   * behaviour within a single transaction.
   */
  @Test
  @DisplayName("AC4: Entity always has an active LM before and after replacement")
  void dstew1652_ac4_replacementCompletes_entityNeverLeftWithoutActiveLm() {
    // Verify exactly one active LM exists before the replacement.
    given()
        .pathParam("firmId", firmNumber)
        .pathParam("officeCode", secondOfficeCode)
        .when()
        .get("/provider-firms/{firmId}/offices/{officeCode}/liaison-managers")
        .then()
        .statusCode(200)
        .body("data.content.findAll { it.activeDateTo == null }.size()", equalTo(1));

    // Perform the replacement.
    given()
        .contentType(ContentType.JSON)
        .pathParam("firmId", firmNumber)
        .pathParam("officeCode", secondOfficeCode)
        .body(
            Map.of(
                "firstName", "Replacement",
                "lastName", "For AC4",
                "emailAddress", "e2e.ac4." + System.currentTimeMillis() + "@example.com",
                "telephoneNumber", "020 5555 6666"))
        .when()
        .post("/provider-firms/{firmId}/offices/{officeCode}/liaison-managers")
        .then()
        .statusCode(201);

    // Verify exactly one active LM exists after the replacement — no gap in coverage.
    given()
        .pathParam("firmId", firmNumber)
        .pathParam("officeCode", secondOfficeCode)
        .when()
        .get("/provider-firms/{firmId}/offices/{officeCode}/liaison-managers")
        .then()
        .statusCode(200)
        .body("data.content.findAll { it.activeDateTo == null }.size()", equalTo(1));
  }
}
