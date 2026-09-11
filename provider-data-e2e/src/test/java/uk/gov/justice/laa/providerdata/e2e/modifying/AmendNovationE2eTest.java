package uk.gov.justice.laa.providerdata.e2e.modifying;

import static io.restassured.RestAssured.given;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.emptyOrNullString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;

import io.restassured.http.ContentType;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import uk.gov.justice.laa.providerdata.e2e.ModifyingTest;

/// Data-modifying e2e tests for `PATCH /novations/{novationGUID}` and `PATCH
/// /novations/{novationGUID}/relationships/{novationRelationshipGUID}`.
///
/// Each test creates its fixture data through public API endpoints before applying a Novation
/// amendment.
@ModifyingTest
class AmendNovationE2eTest {

  /// Amends scalar Novation fields and verifies the existing relationship remains associated.
  ///
  /// - DSTEW-1977 AC1 - valid Novation amendments are persisted successfully. (DS_MAPD_FR_052)
  /// - DSTEW-1977 AC3 - existing lineage relationships remain available after amendment.
  ///   (DS_MAPD_FR_052)
  ///
  /// - DS_MAPD_FR_052: Amend Novation record.
  @Test
  void dstew1977_ac1_validNovationAmendment_returns200AndRetainsRelationship() {
    CreatedNovation created = createNovation("E2E-DSTEW-1977 Amend");

    given()
        .contentType(ContentType.JSON)
        .pathParam("novationGUID", created.novationGuid())
        .body(
            Map.of(
                "novationType", "Legal entity change",
                "novationEffectiveDate", "2026-03-01",
                "novationStatus", "Approved",
                "decisionDate", "2026-02-01",
                "driverForNovation", "Correction",
                "notes", "Updated Novation"))
        .when()
        .patch("/novations/{novationGUID}")
        .then()
        .statusCode(200)
        .body("data.guid", equalTo(created.novationGuid()))
        .body("data.novationType", equalTo("Legal entity change"))
        .body("data.novationEffectiveDate", equalTo("2026-03-01"))
        .body("data.novationStatus", equalTo("Approved"))
        .body("data.decisionDate", equalTo("2026-02-01"))
        .body("data.driverForNovation", equalTo("Correction"))
        .body("data.notes", equalTo("Updated Novation"))
        .body("data.relationships", hasSize(1))
        .body("data.relationships[0].guid", equalTo(created.relationshipGuid()));
  }

  /// Attempts to amend a mandatory string field to a blank value and verifies the record remains
  /// unchanged.
  ///
  /// - DSTEW-1977 AC2 - invalid mandatory amendment information is rejected. (DS_MAPD_FR_052)
  ///
  /// - DS_MAPD_FR_052: Amend Novation record.
  @Test
  void dstew1977_ac2_blankNovationType_returns400AndRecordUnchanged() {
    CreatedNovation created = createNovation("E2E-DSTEW-1977 Mandatory");
    Map<String, Object> before = getNovationData(created.novationGuid());

    given()
        .contentType(ContentType.JSON)
        .pathParam("novationGUID", created.novationGuid())
        .body(Map.of("novationType", " "))
        .when()
        .patch("/novations/{novationGUID}")
        .then()
        .statusCode(400)
        .body("detail", containsString("novationType"));

    assertThat(getNovationData(created.novationGuid()), equalTo(before));
  }

  /// Attempts to amend a relationship without a mandatory predecessor Provider Firm GUID and
  /// verifies the relationship remains unchanged.
  ///
  /// - DSTEW-1977 AC2 - invalid mandatory relationship amendment information is rejected.
  ///   (DS_MAPD_FR_052)
  ///
  /// - DS_MAPD_FR_052: Amend Novation record.
  @Test
  void dstew1977_ac2_relationshipMissingPreviousProvider_returns400AndRelationshipUnchanged() {
    CreatedNovation created = createNovation("E2E-DSTEW-1977 Relationship Mandatory");
    Map<String, Object> before = getNovationData(created.novationGuid());

    given()
        .contentType(ContentType.JSON)
        .pathParam("novationGUID", created.novationGuid())
        .pathParam("novationRelationshipGUID", created.relationshipGuid())
        .body(Map.of("newProviderFirmGUID", created.newProviderFirmGuid()))
        .when()
        .patch("/novations/{novationGUID}/relationships/{novationRelationshipGUID}")
        .then()
        .statusCode(400)
        .body("detail", not(emptyOrNullString()));

    assertThat(getNovationData(created.novationGuid()), equalTo(before));
  }

  /// Sets optional text fields to empty strings and verifies the amendment is accepted.
  ///
  /// - DSTEW-1977 AC1 - optional text fields may be amended to empty values where permitted.
  ///   (DS_MAPD_FR_052)
  ///
  /// - DS_MAPD_FR_052: Amend Novation record.
  @Test
  void dstew1977_ac1_optionalTextFieldsCanBeSetToEmptyStrings() {
    CreatedNovation created = createApprovedNovation("E2E-DSTEW-1977 Empty Text");

    given()
        .contentType(ContentType.JSON)
        .pathParam("novationGUID", created.novationGuid())
        .body(
            Map.of(
                "decisionReason", "",
                "driverForNovation", "",
                "notes", ""))
        .when()
        .patch("/novations/{novationGUID}")
        .then()
        .statusCode(200)
        .body("data.decisionReason", equalTo(""))
        .body("data.driverForNovation", equalTo(""))
        .body("data.notes", equalTo(""));
  }

  /// Applies each permitted status transition from Proposed.
  ///
  /// - DSTEW-1977 AC4 - permitted status changes are accepted. (DS_MAPD_FR_052)
  ///
  /// - DS_MAPD_FR_052: Amend Novation record.
  @ParameterizedTest
  @ValueSource(strings = {"Approved", "Approved with Conditions", "Rejected", "Withdrawn"})
  void dstew1977_ac4_proposedToPermittedStatus_returns200(String status) {
    CreatedNovation created = createNovation("E2E-DSTEW-1977 Status");

    given()
        .contentType(ContentType.JSON)
        .pathParam("novationGUID", created.novationGuid())
        .body(statusPatch(status, true, !"Approved".equals(status)))
        .when()
        .patch("/novations/{novationGUID}")
        .then()
        .statusCode(200)
        .body("data.novationStatus", equalTo(status))
        .body("data.decisionDate", equalTo("2026-02-01"));
  }

  /// Attempts an invalid direct rescission from Proposed.
  ///
  /// - DSTEW-1977 AC4 - invalid status changes are rejected. (DS_MAPD_FR_052)
  /// - DSTEW-1977 AC8 - invalid rescission is rejected. (DS_MAPD_FR_052)
  ///
  /// - DS_MAPD_FR_052: Amend Novation record.
  @Test
  void dstew1977_ac8_proposedToRescinded_returns400() {
    CreatedNovation created = createNovation("E2E-DSTEW-1977 Invalid Rescission");

    given()
        .contentType(ContentType.JSON)
        .pathParam("novationGUID", created.novationGuid())
        .body(
            Map.of(
                "novationStatus", "Rescinded",
                "rescindedDate", "2026-04-01",
                "rescindedReason", "Cancelled"))
        .when()
        .patch("/novations/{novationGUID}")
        .then()
        .statusCode(400)
        .body("detail", containsString("not permitted"));
  }

  /// Attempts an invalid status change from terminal statuses.
  ///
  /// - DSTEW-1977 AC4 - invalid status changes are rejected. (DS_MAPD_FR_052)
  ///
  /// - DS_MAPD_FR_052: Amend Novation record.
  @ParameterizedTest
  @ValueSource(strings = {"Rejected", "Withdrawn"})
  void dstew1977_ac4_terminalStatusToApproved_returns400(String status) {
    CreatedNovation created = createNovationWithStatus("E2E-DSTEW-1977 Terminal", status);

    given()
        .contentType(ContentType.JSON)
        .pathParam("novationGUID", created.novationGuid())
        .body(statusPatch("Approved", true, false))
        .when()
        .patch("/novations/{novationGUID}")
        .then()
        .statusCode(400)
        .body("detail", containsString("not permitted"));
  }

  /// Attempts invalid non-rescission status changes from statuses that can only move to Rescinded.
  ///
  /// - DSTEW-1977 AC4 - invalid status changes are rejected. (DS_MAPD_FR_052)
  ///
  /// - DS_MAPD_FR_052: Amend Novation record.
  @ParameterizedTest
  @CsvSource({"Approved,Rejected", "Approved with Conditions,Withdrawn"})
  void dstew1977_ac4_approvedOrApprovedWithConditionsToNonRescindedStatus_returns400(
      String currentStatus, String requestedStatus) {
    CreatedNovation created =
        createNovationWithStatus("E2E-DSTEW-1977 Invalid Transition", currentStatus);
    Map<String, Object> before = getNovationData(created.novationGuid());

    given()
        .contentType(ContentType.JSON)
        .pathParam("novationGUID", created.novationGuid())
        .body(statusPatch(requestedStatus, true, !"Approved".equals(requestedStatus)))
        .when()
        .patch("/novations/{novationGUID}")
        .then()
        .statusCode(400)
        .body("detail", containsString("not permitted"));

    assertThat(getNovationData(created.novationGuid()), equalTo(before));
  }

  /// Attempts an invalid status change from Rescinded.
  ///
  /// - DSTEW-1977 AC4 - invalid status changes are rejected. (DS_MAPD_FR_052)
  /// - DSTEW-1977 AC8 - invalid rescission sources are rejected. (DS_MAPD_FR_052)
  ///
  /// - DS_MAPD_FR_052: Amend Novation record.
  @Test
  void dstew1977_ac4_rescindedToApproved_returns400() {
    CreatedNovation created = createApprovedNovation("E2E-DSTEW-1977 Rescinded Terminal");
    rescindNovation(created);

    given()
        .contentType(ContentType.JSON)
        .pathParam("novationGUID", created.novationGuid())
        .body(statusPatch("Approved", true, false))
        .when()
        .patch("/novations/{novationGUID}")
        .then()
        .statusCode(400)
        .body("detail", containsString("not permitted"));
  }

  /// Attempts to rescind non-approved terminal statuses.
  ///
  /// - DSTEW-1977 AC8 - invalid rescission from non-approved statuses is rejected.
  ///   (DS_MAPD_FR_052)
  ///
  /// - DS_MAPD_FR_052: Amend Novation record.
  @ParameterizedTest
  @ValueSource(strings = {"Rejected", "Withdrawn"})
  void dstew1977_ac8_rejectedOrWithdrawnToRescinded_returns400(String status) {
    CreatedNovation created = createNovationWithStatus("E2E-DSTEW-1977 Invalid Rescind", status);
    Map<String, Object> before = getNovationData(created.novationGuid());

    given()
        .contentType(ContentType.JSON)
        .pathParam("novationGUID", created.novationGuid())
        .body(
            Map.of(
                "novationStatus", "Rescinded",
                "rescindedDate", "2026-04-01",
                "rescindedReason", "Cancelled"))
        .when()
        .patch("/novations/{novationGUID}")
        .then()
        .statusCode(400)
        .body("detail", containsString("not permitted"));

    assertThat(getNovationData(created.novationGuid()), equalTo(before));
  }

  /// Omits Decision Date when moving to statuses that require it.
  ///
  /// - DSTEW-1977 AC5 - missing Decision Date is rejected. (DS_MAPD_FR_052)
  ///
  /// - DS_MAPD_FR_052: Amend Novation record.
  @ParameterizedTest
  @ValueSource(strings = {"Approved", "Approved with Conditions", "Rejected", "Withdrawn"})
  void dstew1977_ac5_statusRequiringDecisionDateWithoutDecisionDate_returns400(String status) {
    CreatedNovation created = createNovation("E2E-DSTEW-1977 Decision Date");

    given()
        .contentType(ContentType.JSON)
        .pathParam("novationGUID", created.novationGuid())
        .body(statusPatch(status, false, !"Approved".equals(status)))
        .when()
        .patch("/novations/{novationGUID}")
        .then()
        .statusCode(400)
        .body("detail", containsString("decisionDate"));
  }

  /// Omits Decision Reason when moving to statuses that require it.
  ///
  /// - DSTEW-1977 AC6 - missing Decision Reason is rejected. (DS_MAPD_FR_052)
  ///
  /// - DS_MAPD_FR_052: Amend Novation record.
  @ParameterizedTest
  @ValueSource(strings = {"Approved with Conditions", "Rejected", "Withdrawn"})
  void dstew1977_ac6_statusRequiringDecisionReasonWithoutDecisionReason_returns400(String status) {
    CreatedNovation created = createNovation("E2E-DSTEW-1977 Decision Reason");

    given()
        .contentType(ContentType.JSON)
        .pathParam("novationGUID", created.novationGuid())
        .body(statusPatch(status, true, false))
        .when()
        .patch("/novations/{novationGUID}")
        .then()
        .statusCode(400)
        .body("detail", containsString("decisionReason"));
  }

  /// Attempts to rescind an Approved Novation without rescission-specific mandatory fields.
  ///
  /// - DSTEW-1977 AC5 - Rescinded requires a rescission date. (DS_MAPD_FR_052)
  /// - DSTEW-1977 AC6 - Rescinded requires a rescission reason. (DS_MAPD_FR_052)
  ///
  /// - DS_MAPD_FR_052: Amend Novation record.
  @ParameterizedTest
  @CsvSource({"false,true,rescindedDate", "true,false,rescindedReason"})
  void dstew1977_ac5Ac6_rescindedWithoutMandatoryRescissionField_returns400(
      boolean includeRescindedDate, boolean includeRescindedReason, String expectedDetail) {
    CreatedNovation created = createApprovedNovation("E2E-DSTEW-1977 Rescind Mandatory");
    Map<String, Object> before = getNovationData(created.novationGuid());

    given()
        .contentType(ContentType.JSON)
        .pathParam("novationGUID", created.novationGuid())
        .body(rescissionPatch(includeRescindedDate, includeRescindedReason))
        .when()
        .patch("/novations/{novationGUID}")
        .then()
        .statusCode(400)
        .body("detail", containsString(expectedDetail));

    assertThat(getNovationData(created.novationGuid()), equalTo(before));
  }

  /// Rescinds an Approved Novation and verifies the existing Novation GUID is retained.
  ///
  /// - DSTEW-1977 AC7 - an Approved Novation can be updated to Rescinded without creating a new
  ///   Novation record. (DS_MAPD_FR_052)
  ///
  /// - DS_MAPD_FR_052: Amend Novation record.
  @Test
  void dstew1977_ac7_approvedToRescinded_returns200WithSameNovationGuid() {
    CreatedNovation created = createApprovedNovation("E2E-DSTEW-1977 Rescind");

    given()
        .contentType(ContentType.JSON)
        .pathParam("novationGUID", created.novationGuid())
        .body(
            Map.of(
                "novationStatus", "Rescinded",
                "rescindedDate", "2026-04-01",
                "rescindedReason", "Cancelled"))
        .when()
        .patch("/novations/{novationGUID}")
        .then()
        .statusCode(200)
        .body("data.guid", equalTo(created.novationGuid()))
        .body("data.novationStatus", equalTo("Rescinded"))
        .body("data.decisionDate", equalTo("2026-02-01"))
        .body("data.rescindedDate", equalTo("2026-04-01"))
        .body("data.rescindedReason", equalTo("Cancelled"));
  }

  /// Rescinds an Approved with Conditions Novation and verifies the existing Novation GUID and
  /// original decision reason are retained.
  ///
  /// - DSTEW-1977 AC7 - an Approved with Conditions Novation can be updated to Rescinded without
  ///   creating a new Novation record. (DS_MAPD_FR_052)
  ///
  /// - DS_MAPD_FR_052: Amend Novation record.
  @Test
  void dstew1977_ac7_approvedWithConditionsToRescinded_returns200WithSameNovationGuid() {
    CreatedNovation created =
        createNovationWithStatus("E2E-DSTEW-1977 Rescind Conditions", "Approved with Conditions");

    given()
        .contentType(ContentType.JSON)
        .pathParam("novationGUID", created.novationGuid())
        .body(
            Map.of(
                "novationStatus", "Rescinded",
                "rescindedDate", "2026-04-01",
                "rescindedReason", "Cancelled"))
        .when()
        .patch("/novations/{novationGUID}")
        .then()
        .statusCode(200)
        .body("data.guid", equalTo(created.novationGuid()))
        .body("data.novationStatus", equalTo("Rescinded"))
        .body("data.decisionReason", equalTo("Initial decision reason"))
        .body("data.rescindedDate", equalTo("2026-04-01"))
        .body("data.rescindedReason", equalTo("Cancelled"));
  }

  /// Verifies a successful amendment remains traceable using the resource audit/version fields.
  ///
  /// - DSTEW-1977 AC9 - amendment traceability is retained through audit/version metadata.
  ///   (DS_MAPD_FR_052)
  ///
  /// - DS_MAPD_FR_052: Amend Novation record.
  @Test
  void dstew1977_ac9_successfulAmendment_incrementsVersionAndRetainsCreatedAuditFields() {
    CreatedNovation created = createNovation("E2E-DSTEW-1977 Audit");
    Map<String, Object> before = getNovationData(created.novationGuid());

    given()
        .contentType(ContentType.JSON)
        .pathParam("novationGUID", created.novationGuid())
        .body(Map.of("notes", "Audit trace update"))
        .when()
        .patch("/novations/{novationGUID}")
        .then()
        .statusCode(200)
        .body("data.notes", equalTo("Audit trace update"));

    Map<String, Object> after = getNovationData(created.novationGuid());
    assertThat(after.get("guid"), equalTo(before.get("guid")));
    assertThat(after.get("createdBy"), equalTo(before.get("createdBy")));
    assertThat(after.get("createdTimestamp"), equalTo(before.get("createdTimestamp")));
    assertThat(
        ((Number) after.get("version")).longValue(),
        greaterThan(((Number) before.get("version")).longValue()));
  }

  /// Attempts to amend a server-controlled Novation field.
  ///
  /// - DSTEW-1977 AC10 - redacted Novation fields are rejected. (DS_MAPD_FR_052)
  ///
  /// - DS_MAPD_FR_052: Amend Novation record.
  @Test
  void dstew1977_ac10_redactedNovationGuid_returns400() {
    CreatedNovation created = createNovation("E2E-DSTEW-1977 Redacted");

    given()
        .contentType(ContentType.JSON)
        .pathParam("novationGUID", created.novationGuid())
        .body(Map.of("guid", "00000000-0000-0000-0000-000000000000"))
        .when()
        .patch("/novations/{novationGUID}")
        .then()
        .statusCode(400)
        .body("detail", not(emptyOrNullString()));
  }

  /// Amends a supported predecessor/successor relationship and verifies the same relationship GUID
  /// is returned.
  ///
  /// - DSTEW-1977 AC1 - supported relationship amendments are persisted successfully.
  ///   (DS_MAPD_FR_052)
  /// - DSTEW-1977 AC3 - the relationship remains available for audit/reporting after amendment.
  ///   (DS_MAPD_FR_052)
  ///
  /// - DS_MAPD_FR_052: Amend Novation record.
  @Test
  void dstew1977_ac1_validRelationshipAmendment_returns200WithSameRelationshipGuid() {
    CreatedNovation created = createNovation("E2E-DSTEW-1977 Relationship");
    long ts = System.nanoTime();
    String previousProviderFirmGuid =
        createLspFirmExpect201("E2E-DSTEW-1977 Replacement Previous " + ts);
    String newProviderFirmGuid = createLspFirmExpect201("E2E-DSTEW-1977 Replacement New " + ts);
    String previousOfficeGuid = getHeadOfficeGuid(previousProviderFirmGuid);
    String newOfficeGuid = getHeadOfficeGuid(newProviderFirmGuid);

    given()
        .contentType(ContentType.JSON)
        .pathParam("novationGUID", created.novationGuid())
        .pathParam("novationRelationshipGUID", created.relationshipGuid())
        .body(
            Map.of(
                "previousProviderFirmGUID", previousProviderFirmGuid,
                "newProviderFirmGUID", newProviderFirmGuid,
                "previousOfficeGUID", previousOfficeGuid,
                "newOfficeGUID", newOfficeGuid,
                "notes", "Updated relationship"))
        .when()
        .patch("/novations/{novationGUID}/relationships/{novationRelationshipGUID}")
        .then()
        .statusCode(200)
        .body("data.guid", equalTo(created.relationshipGuid()))
        .body("data.novationGUID", equalTo(created.novationGuid()))
        .body("data.previousProviderFirmGUID", equalTo(previousProviderFirmGuid))
        .body("data.newProviderFirmGUID", equalTo(newProviderFirmGuid))
        .body("data.previousOfficeGUID", equalTo(previousOfficeGuid))
        .body("data.newOfficeGUID", equalTo(newOfficeGuid))
        .body("data.notes", equalTo("Updated relationship"));

    given()
        .pathParam("novationGUID", created.novationGuid())
        .when()
        .get("/novations/{novationGUID}")
        .then()
        .statusCode(200)
        .body("data.relationships", hasSize(1))
        .body("data.relationships[0].guid", equalTo(created.relationshipGuid()))
        .body("data.relationships[0].previousProviderFirmGUID", equalTo(previousProviderFirmGuid))
        .body("data.relationships[0].newProviderFirmGUID", equalTo(newProviderFirmGuid));
  }

  /// Attempts to amend a server-controlled relationship field.
  ///
  /// - DSTEW-1977 AC10 - redacted relationship fields are rejected. (DS_MAPD_FR_052)
  ///
  /// - DS_MAPD_FR_052: Amend Novation record.
  @Test
  void dstew1977_ac10_redactedRelationshipGuid_returns400() {
    CreatedNovation created = createNovation("E2E-DSTEW-1977 Relationship Redacted");

    given()
        .contentType(ContentType.JSON)
        .pathParam("novationGUID", created.novationGuid())
        .pathParam("novationRelationshipGUID", created.relationshipGuid())
        .body(
            Map.of(
                "guid", "00000000-0000-0000-0000-000000000000",
                "previousProviderFirmGUID", created.previousProviderFirmGuid(),
                "newProviderFirmGUID", created.newProviderFirmGuid()))
        .when()
        .patch("/novations/{novationGUID}/relationships/{novationRelationshipGUID}")
        .then()
        .statusCode(400)
        .body("detail", not(emptyOrNullString()));
  }

  private CreatedNovation createNovation(String namePrefix) {
    long ts = System.nanoTime();
    String previousProviderFirmGuid = createLspFirmExpect201(namePrefix + " Previous " + ts);
    String newProviderFirmGuid = createLspFirmExpect201(namePrefix + " New " + ts);
    return createNovation(previousProviderFirmGuid, newProviderFirmGuid, Map.of());
  }

  private CreatedNovation createNovation(
      String previousProviderFirmGuid,
      String newProviderFirmGuid,
      Map<String, Object> extraNovationFields) {
    Map<String, Object> body = new HashMap<>();
    body.put("novationType", "Merger");
    body.put("novationEffectiveDate", "2026-01-01");
    body.putAll(extraNovationFields);
    body.put(
        "relationships",
        List.of(
            Map.of(
                "previousProviderFirmGUID", previousProviderFirmGuid,
                "newProviderFirmGUID", newProviderFirmGuid,
                "notes", "Initial relationship")));

    Map<String, Object> data =
        given()
            .contentType(ContentType.JSON)
            .body(body)
            .when()
            .post("/novations")
            .then()
            .statusCode(201)
            .extract()
            .path("data");

    @SuppressWarnings("unchecked")
    List<String> relationshipGuids = (List<String>) data.get("novationRelationshipGUIDs");
    return new CreatedNovation(
        (String) data.get("novationGUID"),
        relationshipGuids.getFirst(),
        previousProviderFirmGuid,
        newProviderFirmGuid);
  }

  private CreatedNovation createNovationWithStatus(String namePrefix, String status) {
    long ts = System.nanoTime();
    String previousProviderFirmGuid = createLspFirmExpect201(namePrefix + " Previous " + ts);
    String newProviderFirmGuid = createLspFirmExpect201(namePrefix + " New " + ts);
    return createNovation(
        previousProviderFirmGuid, newProviderFirmGuid, statusPatch(status, true, true));
  }

  private CreatedNovation createApprovedNovation(String namePrefix) {
    long ts = System.nanoTime();
    String previousProviderFirmGuid = createLspFirmExpect201(namePrefix + " Previous " + ts);
    String newProviderFirmGuid = createLspFirmExpect201(namePrefix + " New " + ts);
    return createNovation(
        previousProviderFirmGuid,
        newProviderFirmGuid,
        Map.of(
            "novationStatus", "Approved",
            "decisionDate", "2026-02-01"));
  }

  private void rescindNovation(CreatedNovation created) {
    given()
        .contentType(ContentType.JSON)
        .pathParam("novationGUID", created.novationGuid())
        .body(
            Map.of(
                "novationStatus", "Rescinded",
                "rescindedDate", "2026-04-01",
                "rescindedReason", "Cancelled"))
        .when()
        .patch("/novations/{novationGUID}")
        .then()
        .statusCode(200);
  }

  private Map<String, Object> statusPatch(
      String status, boolean includeDecisionDate, boolean includeDecisionReason) {
    Map<String, Object> body = new HashMap<>();
    body.put("novationStatus", status);
    if (includeDecisionDate) {
      body.put("decisionDate", "2026-02-01");
    }
    if (includeDecisionReason) {
      body.put("decisionReason", "Initial decision reason");
    }
    return body;
  }

  private Map<String, Object> rescissionPatch(
      boolean includeRescindedDate, boolean includeRescindedReason) {
    Map<String, Object> body = new HashMap<>();
    body.put("novationStatus", "Rescinded");
    if (includeRescindedDate) {
      body.put("rescindedDate", "2026-04-01");
    }
    if (includeRescindedReason) {
      body.put("rescindedReason", "Cancelled");
    }
    return body;
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

  /// Fetches the GUID of a firm's head office.
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

  /// Fetches the full `data` object for a Novation.
  private Map<String, Object> getNovationData(String novationGuid) {
    return given()
        .pathParam("novationGUID", novationGuid)
        .when()
        .get("/novations/{novationGUID}")
        .then()
        .statusCode(200)
        .extract()
        .path("data");
  }

  private record CreatedNovation(
      String novationGuid,
      String relationshipGuid,
      String previousProviderFirmGuid,
      String newProviderFirmGuid) {}
}
