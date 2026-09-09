package uk.gov.justice.laa.providerdata.e2e.modifying;

import static io.restassured.RestAssured.given;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;

import io.restassured.http.ContentType;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import uk.gov.justice.laa.providerdata.e2e.ModifyingTest;

/// Data-modifying e2e tests for viewing a Novation via `GET /novations/{novationGUID}`.
///
/// Each test creates its fixture data through public API endpoints, then exercises the read-only
/// Novation retrieval endpoint.
///
/// This class uses `@ModifyingTest`, not `@ReadOnlyTest`, because the e2e suite has no stable
/// pre-seeded Novation fixture. The operation under test remains read-only; only the fixture setup
/// modifies data.
@ModifyingTest
class ViewNovationE2eTest {

  /// Retrieves an existing Novation and verifies the core Novation fields and supported provider
  /// relationship fields, including predecessor/successor Child Office links.
  ///
  /// - DSTEW-1980 AC1 - existing Novation details are returned successfully. (DS_MAPD_FR_053)
  /// - DSTEW-1980 AC2 - predecessor and successor provider relationships remain retrievable.
  ///   (DS_MAPD_FR_053)
  ///
  /// - DS_MAPD_FR_053: View Novation record.
  @Test
  void dstew1980_ac1_existingNovation_returnsRecordAndSupportedRelationships() {
    long ts = System.currentTimeMillis();
    String previousProviderFirmGuid =
        createLspFirmExpect201("E2E-DSTEW-1980 View Predecessor " + ts);
    String newProviderFirmGuid = createLspFirmExpect201("E2E-DSTEW-1980 View Successor " + ts);
    String previousOfficeGuid = getHeadOfficeGuid(previousProviderFirmGuid);
    String newOfficeGuid = getHeadOfficeGuid(newProviderFirmGuid);

    String novationGuid =
        createApprovedWithConditionsNovation(
            previousProviderFirmGuid, newProviderFirmGuid, previousOfficeGuid, newOfficeGuid);

    given()
        .pathParam("novationGUID", novationGuid)
        .when()
        .get("/novations/{novationGUID}")
        .then()
        .statusCode(200)
        .body("data.guid", equalTo(novationGuid))
        .body("data.novationType", equalTo("Merger"))
        .body("data.novationEffectiveDate", equalTo("2026-01-01"))
        .body("data.novationStatus", equalTo("Approved with Conditions"))
        .body("data.decisionDate", equalTo("2026-02-01"))
        .body("data.decisionReason", equalTo("Board approval required before transfer"))
        .body("data.driverForNovation", equalTo("Organisational restructure"))
        .body("data.notes", equalTo("DSTEW-1980 view test novation"))
        .body("data.relationships", hasSize(1))
        .body("data.relationships[0].guid", notNullValue())
        .body("data.relationships[0].novationGUID", equalTo(novationGuid))
        .body("data.relationships[0].previousProviderFirmGUID", equalTo(previousProviderFirmGuid))
        .body("data.relationships[0].newProviderFirmGUID", equalTo(newProviderFirmGuid))
        .body("data.relationships[0].previousOfficeGUID", equalTo(previousOfficeGuid))
        .body("data.relationships[0].newOfficeGUID", equalTo(newOfficeGuid))
        .body("data.relationships[0].notes", equalTo("DSTEW-1980 view test relationship"));
  }

  /// Requests a Novation GUID that does not exist and verifies the API reports it as not found.
  ///
  /// - DSTEW-1980 AC3 - a missing Novation record returns an error. (DS_MAPD_FR_053)
  ///
  /// - DS_MAPD_FR_053: View Novation record.
  @Test
  void dstew1980_ac3_unknownNovation_returns404() {
    given()
        .pathParam("novationGUID", "00000000-0000-0000-0000-000000000000")
        .when()
        .get("/novations/{novationGUID}")
        .then()
        .statusCode(404);
  }

  /// Retrieves the same Novation twice and verifies the second response is identical to the first.
  ///
  /// - DSTEW-1980 AC4 - viewing a Novation does not create, amend or delete Novation data or
  ///   relationship data. (DS_MAPD_FR_053)
  ///
  /// - DS_MAPD_FR_053: View Novation record.
  @Test
  void dstew1980_ac4_viewNovation_doesNotChangeReturnedData() {
    long ts = System.currentTimeMillis();
    String previousProviderFirmGuid =
        createLspFirmExpect201("E2E-DSTEW-1980 ReadOnly Predecessor " + ts);
    String newProviderFirmGuid = createLspFirmExpect201("E2E-DSTEW-1980 ReadOnly Successor " + ts);
    String previousOfficeGuid = getHeadOfficeGuid(previousProviderFirmGuid);
    String newOfficeGuid = getHeadOfficeGuid(newProviderFirmGuid);
    String novationGuid =
        createApprovedWithConditionsNovation(
            previousProviderFirmGuid, newProviderFirmGuid, previousOfficeGuid, newOfficeGuid);

    Map<String, Object> before = getNovationData(novationGuid);
    Map<String, Object> after = getNovationData(novationGuid);

    assertThat(after, equalTo(before));
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

  /// Creates an Approved with Conditions Novation and returns its GUID.
  private String createApprovedWithConditionsNovation(
      String previousProviderFirmGuid,
      String newProviderFirmGuid,
      String previousOfficeGuid,
      String newOfficeGuid) {
    return given()
        .contentType(ContentType.JSON)
        .body(
            Map.of(
                "novationType",
                "Merger",
                "novationEffectiveDate",
                "2026-01-01",
                "novationStatus",
                "Approved with Conditions",
                "decisionDate",
                "2026-02-01",
                "decisionReason",
                "Board approval required before transfer",
                "driverForNovation",
                "Organisational restructure",
                "notes",
                "DSTEW-1980 view test novation",
                "relationships",
                List.of(
                    Map.of(
                        "previousProviderFirmGUID", previousProviderFirmGuid,
                        "newProviderFirmGUID", newProviderFirmGuid,
                        "previousOfficeGUID", previousOfficeGuid,
                        "newOfficeGUID", newOfficeGuid,
                        "notes", "DSTEW-1980 view test relationship"))))
        .when()
        .post("/novations")
        .then()
        .statusCode(201)
        .extract()
        .path("data.novationGUID");
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
}
