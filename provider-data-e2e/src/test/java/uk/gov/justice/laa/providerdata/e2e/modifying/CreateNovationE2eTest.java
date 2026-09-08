package uk.gov.justice.laa.providerdata.e2e.modifying;

import static io.restassured.RestAssured.given;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;

import io.restassured.http.ContentType;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import uk.gov.justice.laa.providerdata.e2e.ModifyingTest;

/**
 * Data-modifying e2e tests for {@code POST /novations} and {@code GET /novations/{novationGUID}}
 * (DSTEW-1975).
 *
 * <p>Each test creates one or two new Legal Services Provider firms, then submits a novation
 * linking them, verifying the response and (where relevant) that a subsequent GET reflects the
 * persisted state.
 */
@ModifyingTest
class CreateNovationE2eTest {

  // AC1 - Create Novation Record; also covers AC2's "relationship recorded" clause.
  @Test
  void createNovation_returns201WithGeneratedIdentifiers() {
    long ts = System.currentTimeMillis();
    String previousProviderFirmGuid = createLspFirmExpect201("E2E-DSTEW-1975 Predecessor " + ts);
    String newProviderFirmGuid = createLspFirmExpect201("E2E-DSTEW-1975 Successor " + ts);

    given()
        .contentType(ContentType.JSON)
        .body(
            Map.of(
                "novationType",
                "Merger",
                "novationEffectiveDate",
                "2026-01-01",
                "notes",
                "E2E test novation",
                "relationships",
                List.of(
                    Map.of(
                        "previousProviderFirmGUID", previousProviderFirmGuid,
                        "newProviderFirmGUID", newProviderFirmGuid,
                        "notes", "E2E test relationship"))))
        .when()
        .post("/novations")
        .then()
        .statusCode(201)
        .body("data.novationGUID", notNullValue())
        .body("data.novationRelationshipGUIDs", hasSize(1))
        .body("data.novationRelationshipGUIDs[0]", notNullValue());
  }

  // AC2 - the relationship recorded on creation can be retrieved for operational, reporting and
  // audit purposes.
  @Test
  void getNovation_returnsPersistedNovationAndRelationship_afterCreation() {
    long ts = System.currentTimeMillis();
    String previousProviderFirmGuid =
        createLspFirmExpect201("E2E-DSTEW-1975 Retrieve Predecessor " + ts);
    String newProviderFirmGuid = createLspFirmExpect201("E2E-DSTEW-1975 Retrieve Successor " + ts);

    String novationGuid =
        given()
            .contentType(ContentType.JSON)
            .body(
                Map.of(
                    "novationType",
                    "Merger",
                    "novationEffectiveDate",
                    "2026-01-01",
                    "notes",
                    "E2E retrieval test novation",
                    "relationships",
                    List.of(
                        Map.of(
                            "previousProviderFirmGUID", previousProviderFirmGuid,
                            "newProviderFirmGUID", newProviderFirmGuid,
                            "notes", "E2E retrieval test relationship"))))
            .when()
            .post("/novations")
            .then()
            .statusCode(201)
            .extract()
            .path("data.novationGUID");

    given()
        .pathParam("novationGUID", novationGuid)
        .when()
        .get("/novations/{novationGUID}")
        .then()
        .statusCode(200)
        .body("data.guid", equalTo(novationGuid))
        .body("data.novationType", equalTo("Merger"))
        .body("data.notes", equalTo("E2E retrieval test novation"))
        .body("data.relationships", hasSize(1))
        .body("data.relationships[0].previousProviderFirmGUID", equalTo(previousProviderFirmGuid))
        .body("data.relationships[0].newProviderFirmGUID", equalTo(newProviderFirmGuid))
        .body("data.relationships[0].notes", equalTo("E2E retrieval test relationship"));
  }

  @Test
  void getNovation_unknownNovationGuid_returns404() {
    given()
        .pathParam("novationGUID", "00000000-0000-0000-0000-000000000000")
        .when()
        .get("/novations/{novationGUID}")
        .then()
        .statusCode(404);
  }

  @Test
  void createNovation_withPreviousOfficeGuid_returns201() {
    long ts = System.currentTimeMillis();
    String previousProviderFirmGuid =
        createLspFirmExpect201("E2E-DSTEW-1975 Office Predecessor " + ts);
    String newProviderFirmGuid = createLspFirmExpect201("E2E-DSTEW-1975 Office Successor " + ts);
    String previousOfficeGuid = getHeadOfficeGuid(previousProviderFirmGuid);

    given()
        .contentType(ContentType.JSON)
        .body(
            Map.of(
                "novationType",
                "Merger",
                "novationEffectiveDate",
                "2026-01-01",
                "relationships",
                List.of(
                    Map.of(
                        "previousProviderFirmGUID", previousProviderFirmGuid,
                        "newProviderFirmGUID", newProviderFirmGuid,
                        "previousOfficeGUID", previousOfficeGuid))))
        .when()
        .post("/novations")
        .then()
        .statusCode(201)
        .body("data.novationGUID", notNullValue())
        .body("data.novationRelationshipGUIDs", hasSize(1));
  }

  // AC3 - missing mandatory information is rejected.
  @Test
  void createNovation_missingNovationType_returns400() {
    long ts = System.currentTimeMillis();
    String previousProviderFirmGuid =
        createLspFirmExpect201("E2E-DSTEW-1975 MissingType Predecessor " + ts);
    String newProviderFirmGuid =
        createLspFirmExpect201("E2E-DSTEW-1975 MissingType Successor " + ts);

    given()
        .contentType(ContentType.JSON)
        .body(
            Map.of(
                "novationEffectiveDate",
                "2026-01-01",
                "relationships",
                List.of(
                    Map.of(
                        "previousProviderFirmGUID", previousProviderFirmGuid,
                        "newProviderFirmGUID", newProviderFirmGuid))))
        .when()
        .post("/novations")
        .then()
        .statusCode(400);
  }

  // AC3 - missing mandatory information (no relationships supplied at all) is rejected.
  @Test
  void createNovation_noRelationships_returns400() {
    given()
        .contentType(ContentType.JSON)
        .body(
            Map.of(
                "novationType", "Merger",
                "novationEffectiveDate", "2026-01-01",
                "relationships", List.of()))
        .when()
        .post("/novations")
        .then()
        .statusCode(400);
  }

  // AC4 - an unresolvable predecessor entity is rejected.
  @Test
  void createNovation_unknownPreviousProvider_returns404() {
    long ts = System.currentTimeMillis();
    String newProviderFirmGuid = createLspFirmExpect201("E2E-DSTEW-1975 Unknown Successor " + ts);

    given()
        .contentType(ContentType.JSON)
        .body(
            Map.of(
                "novationType",
                "Merger",
                "novationEffectiveDate",
                "2026-01-01",
                "relationships",
                List.of(
                    Map.of(
                        "previousProviderFirmGUID",
                        "00000000-0000-0000-0000-000000000000",
                        "newProviderFirmGUID",
                        newProviderFirmGuid))))
        .when()
        .post("/novations")
        .then()
        .statusCode(404);
  }

  // AC4 - an unresolvable successor entity is rejected.
  @Test
  void createNovation_unknownNewProvider_returns404() {
    long ts = System.currentTimeMillis();
    String previousProviderFirmGuid =
        createLspFirmExpect201("E2E-DSTEW-1975 Unknown Predecessor " + ts);

    given()
        .contentType(ContentType.JSON)
        .body(
            Map.of(
                "novationType",
                "Merger",
                "novationEffectiveDate",
                "2026-01-01",
                "relationships",
                List.of(
                    Map.of(
                        "previousProviderFirmGUID",
                        previousProviderFirmGuid,
                        "newProviderFirmGUID",
                        "00000000-0000-0000-0000-000000000000"))))
        .when()
        .post("/novations")
        .then()
        .statusCode(404);
  }

  // AC4 - an unresolvable predecessor child office is rejected.
  @Test
  void createNovation_unknownPreviousOffice_returns404() {
    long ts = System.currentTimeMillis();
    String previousProviderFirmGuid =
        createLspFirmExpect201("E2E-DSTEW-1975 UnknownOffice Predecessor " + ts);
    String newProviderFirmGuid =
        createLspFirmExpect201("E2E-DSTEW-1975 UnknownOffice Successor " + ts);

    given()
        .contentType(ContentType.JSON)
        .body(
            Map.of(
                "novationType",
                "Merger",
                "novationEffectiveDate",
                "2026-01-01",
                "relationships",
                List.of(
                    Map.of(
                        "previousProviderFirmGUID", previousProviderFirmGuid,
                        "newProviderFirmGUID", newProviderFirmGuid,
                        "previousOfficeGUID", "00000000-0000-0000-0000-000000000000"))))
        .when()
        .post("/novations")
        .then()
        .statusCode(404);
  }

  // AC4 - an unresolvable new (successor) child office is rejected.
  @Test
  void createNovation_unknownNewOffice_returns404() {
    long ts = System.currentTimeMillis();
    String previousProviderFirmGuid =
        createLspFirmExpect201("E2E-DSTEW-1975 UnknownNewOffice Predecessor " + ts);
    String newProviderFirmGuid =
        createLspFirmExpect201("E2E-DSTEW-1975 UnknownNewOffice Successor " + ts);

    given()
        .contentType(ContentType.JSON)
        .body(
            Map.of(
                "novationType",
                "Merger",
                "novationEffectiveDate",
                "2026-01-01",
                "relationships",
                List.of(
                    Map.of(
                        "previousProviderFirmGUID", previousProviderFirmGuid,
                        "newProviderFirmGUID", newProviderFirmGuid,
                        "newOfficeGUID", "00000000-0000-0000-0000-000000000000"))))
        .when()
        .post("/novations")
        .then()
        .statusCode(404);
  }

  // AC5 - a Novation Status value outside the permitted set is rejected.
  @Test
  void createNovation_invalidStatusValue_returns400() {
    long ts = System.currentTimeMillis();
    String previousProviderFirmGuid =
        createLspFirmExpect201("E2E-DSTEW-1975 InvalidStatus Predecessor " + ts);
    String newProviderFirmGuid =
        createLspFirmExpect201("E2E-DSTEW-1975 InvalidStatus Successor " + ts);

    given()
        .contentType(ContentType.JSON)
        .body(
            Map.of(
                "novationType",
                "Merger",
                "novationEffectiveDate",
                "2026-01-01",
                "novationStatus",
                "NotARealStatus",
                "relationships",
                List.of(
                    Map.of(
                        "previousProviderFirmGUID", previousProviderFirmGuid,
                        "newProviderFirmGUID", newProviderFirmGuid))))
        .when()
        .post("/novations")
        .then()
        .statusCode(400);
  }

  // AC6 - a Novation must not be created directly with a status of Rescinded.
  @Test
  void createNovation_rescindedStatus_returns400() {
    long ts = System.currentTimeMillis();
    String previousProviderFirmGuid =
        createLspFirmExpect201("E2E-DSTEW-1975 Rescinded Predecessor " + ts);
    String newProviderFirmGuid = createLspFirmExpect201("E2E-DSTEW-1975 Rescinded Successor " + ts);

    given()
        .contentType(ContentType.JSON)
        .body(
            Map.of(
                "novationType",
                "Merger",
                "novationEffectiveDate",
                "2026-01-01",
                "novationStatus",
                "Rescinded",
                "relationships",
                List.of(
                    Map.of(
                        "previousProviderFirmGUID", previousProviderFirmGuid,
                        "newProviderFirmGUID", newProviderFirmGuid))))
        .when()
        .post("/novations")
        .then()
        .statusCode(400);
  }

  // AC7 - Decision Date is mandatory for every status other than Proposed (BR-39).
  @ParameterizedTest
  @ValueSource(strings = {"Approved", "Approved with Conditions", "Rejected", "Withdrawn"})
  void createNovation_statusRequiringDecisionInfoWithoutDecisionDate_returns400(String status) {
    long ts = System.currentTimeMillis();
    String previousProviderFirmGuid =
        createLspFirmExpect201("E2E-DSTEW-1975 NoDecisionDate Predecessor " + ts);
    String newProviderFirmGuid =
        createLspFirmExpect201("E2E-DSTEW-1975 NoDecisionDate Successor " + ts);

    Map<String, Object> body = new HashMap<>();
    body.put("novationType", "Merger");
    body.put("novationEffectiveDate", "2026-01-01");
    body.put("novationStatus", status);
    body.put(
        "relationships",
        List.of(
            Map.of(
                "previousProviderFirmGUID", previousProviderFirmGuid,
                "newProviderFirmGUID", newProviderFirmGuid)));
    // Deliberately omit decisionDate and decisionReason.

    given()
        .contentType(ContentType.JSON)
        .body(body)
        .when()
        .post("/novations")
        .then()
        .statusCode(400);
  }

  // AC8 - Decision Reason is mandatory for every status other than Proposed/Approved (BR-39).
  @ParameterizedTest
  @ValueSource(strings = {"Approved with Conditions", "Rejected", "Withdrawn"})
  void createNovation_statusRequiringDecisionReasonWithoutDecisionReason_returns400(String status) {
    long ts = System.currentTimeMillis();
    String previousProviderFirmGuid =
        createLspFirmExpect201("E2E-DSTEW-1975 NoDecisionReason Predecessor " + ts);
    String newProviderFirmGuid =
        createLspFirmExpect201("E2E-DSTEW-1975 NoDecisionReason Successor " + ts);

    Map<String, Object> body = new HashMap<>();
    body.put("novationType", "Merger");
    body.put("novationEffectiveDate", "2026-01-01");
    body.put("novationStatus", status);
    body.put("decisionDate", "2026-02-01");
    // Deliberately omit decisionReason.
    body.put(
        "relationships",
        List.of(
            Map.of(
                "previousProviderFirmGUID", previousProviderFirmGuid,
                "newProviderFirmGUID", newProviderFirmGuid)));

    given()
        .contentType(ContentType.JSON)
        .body(body)
        .when()
        .post("/novations")
        .then()
        .statusCode(400);
  }

  // AC9 - existing provider records remain unchanged after a Novation is created.
  @Test
  void createNovation_doesNotModifyExistingProviderFirmRecords() {
    long ts = System.currentTimeMillis();
    String previousProviderFirmGuid =
        createLspFirmExpect201("E2E-DSTEW-1975 Unchanged Predecessor " + ts);
    String newProviderFirmGuid = createLspFirmExpect201("E2E-DSTEW-1975 Unchanged Successor " + ts);

    Map<String, Object> beforePrevious = getProviderFirm(previousProviderFirmGuid);
    Map<String, Object> beforeNew = getProviderFirm(newProviderFirmGuid);

    given()
        .contentType(ContentType.JSON)
        .body(
            Map.of(
                "novationType",
                "Merger",
                "novationEffectiveDate",
                "2026-01-01",
                "relationships",
                List.of(
                    Map.of(
                        "previousProviderFirmGUID", previousProviderFirmGuid,
                        "newProviderFirmGUID", newProviderFirmGuid))))
        .when()
        .post("/novations")
        .then()
        .statusCode(201);

    Map<String, Object> afterPrevious = getProviderFirm(previousProviderFirmGuid);
    Map<String, Object> afterNew = getProviderFirm(newProviderFirmGuid);

    assertThat(afterPrevious, equalTo(beforePrevious));
    assertThat(afterNew, equalTo(beforeNew));
  }

  /// Creates a Legal Services Provider firm and returns its GUID, asserting a 201 response.
  private String createLspFirmExpect201(String firmName) {
    return given()
        .contentType(ContentType.JSON)
        .body(
            Map.of(
                "firmType",
                "Legal Services Provider",
                "name",
                firmName,
                "legalServicesProvider",
                Map.of(
                    "constitutionalStatus",
                    "Partnership",
                    "address",
                    Map.of(
                        "line1", "1 Novation Street",
                        "townOrCity", "London",
                        "postcode", "SW1A 1AA"),
                    "payment",
                    Map.of("paymentMethod", "CHECK"),
                    "contractManager",
                    Map.of("useDefaultContractManager", true),
                    "liaisonManager",
                    Map.of(
                        "firstName", "Novation",
                        "lastName", "Manager",
                        "emailAddress", "novation.manager." + System.nanoTime() + "@example.com",
                        "telephoneNumber", "020 1111 2222"))))
        .when()
        .post("/provider-firms")
        .then()
        .statusCode(201)
        .extract()
        .path("data.providerFirmGUID");
  }

  /// Fetches the GUID of a firm's (head) office, via {@code GET /provider-firms/{firmId}/offices},
  /// assuming a single office exists.
  private String getHeadOfficeGuid(String providerFirmGuid) {
    return given()
        .pathParam("firmId", providerFirmGuid)
        .when()
        .get("/provider-firms/{firmId}/offices")
        .then()
        .statusCode(200)
        .extract()
        .path("data.content[0].guid");
  }

  /// Fetches the full {@code data} object for a provider firm, via {@code GET
  /// /provider-firms/{firmId}}, for before/after comparison.
  private Map<String, Object> getProviderFirm(String providerFirmGuid) {
    return given()
        .pathParam("firmId", providerFirmGuid)
        .when()
        .get("/provider-firms/{firmId}")
        .then()
        .statusCode(200)
        .extract()
        .path("data");
  }
}
