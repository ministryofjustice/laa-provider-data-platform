package uk.gov.justice.laa.providerdata.e2e.modifying;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

import io.restassured.http.ContentType;
import java.util.Map;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import uk.gov.justice.laa.providerdata.e2e.E2eConfig;
import uk.gov.justice.laa.providerdata.e2e.ModifyingTest;

/// Data-modifying E2E tests for DSTEW-1663 (DS_MAPD_FR_021): Firm Contract Manager trickle-down to
/// Child Office.
///
/// Covers `POST /provider-firms/{firmId}/offices`, specifically the `contractManager` field's
/// `useHeadOfficeContractManager` variant (trickle-down) versus an explicit `contractManagerGUID`
/// (office-specific override).
///
/// Two distinct, real, non-default Contract Managers are looked up dynamically at test startup
/// (never hardcoded) so that trickle-down and explicit-override can be told apart unambiguously.
/// Deliberately avoids using the system default contract manager for this purpose: these ACs are
/// about trickle-down versus explicit override, not about `useDefaultContractManager` behaviour
/// (which is covered separately in `CreateProviderFirmE2eTest`/`CreateProviderFirmOfficeE2eTest`),
/// so reusing the default here would conflate the two concerns.
///
/// BR-21 note: "Given a Legal Organisation (LSP) has a Contract Manager assigned" is guaranteed by
/// the API itself - `contractManager` is a mandatory field on LSP firm creation
/// (`POST /provider-firms`), so every LSP head office always has one. The corresponding failure
/// mode - requesting `useHeadOfficeContractManager: true` when the head office has no Contract
/// Manager - is therefore unreachable through the public API and cannot be exercised at the E2E
/// level. It is covered at the unit level instead, in `OfficeServiceTest`
/// (`createLspOffice_withUseHeadOfficeContractManager_throwsWhenHeadOfficeHasNoCm`).
@ModifyingTest
class FirmContractManagerTrickleDownE2eTest {

  private static String firstContractManagerGuid;
  private static String secondContractManagerGuid;

  @BeforeAll
  static void lookUpGuids() {
    firstContractManagerGuid =
        given()
            .queryParam("contractManagerId", E2eConfig.contractManagerId())
            .when()
            .get("/provider-contract-managers")
            .then()
            .statusCode(200)
            .extract()
            .path("data.content[0].guid");

    secondContractManagerGuid =
        given()
            .queryParam("contractManagerId", E2eConfig.secondContractManagerId())
            .when()
            .get("/provider-contract-managers")
            .then()
            .statusCode(200)
            .extract()
            .path("data.content[0].guid");
  }

  /// AC1 – Default Firm Contract Manager to Child Office
  ///
  /// Given a Legal Organisation (LSP) has a Contract Manager assigned
  /// And a Child Office is created without an Office-specific Contract Manager
  /// When PDP processes the Child Office creation
  /// Then PDP assigns the Firm's Contract Manager to the Child Office.
  @Test
  void dstew1663_ac1_noOfficeSpecificCm_trickleDownFromHeadOffice() {
    long ts = System.currentTimeMillis();
    String firmNumber = createIsolatedLspFirm("DSTEW-1663-AC1", ts, firstContractManagerGuid);

    String childOfficeCode =
        given()
            .contentType(ContentType.JSON)
            .pathParam("firmId", firmNumber)
            .body(
                Map.of(
                    "address",
                    Map.of(
                        "line1", "1 Trickle Down Street",
                        "townOrCity", "London",
                        "postcode", "EC1A 1BB"),
                    "payment",
                    Map.of("paymentMethod", "CHECK"),
                    "contractManager",
                    Map.of("useHeadOfficeContractManager", true),
                    "liaisonManager",
                    Map.of("useHeadOfficeLiaisonManager", true)))
            .when()
            .post("/provider-firms/{firmId}/offices")
            .then()
            .statusCode(201)
            .extract()
            .path("data.officeCode");

    given()
        .pathParam("firmId", firmNumber)
        .pathParam("officeCode", childOfficeCode)
        .when()
        .get("/provider-firms/{firmId}/offices/{officeCode}/contract-managers")
        .then()
        .statusCode(200)
        .body("data.content", hasSize(1))
        .body("data.content[0].guid", equalTo(firstContractManagerGuid));
  }

  /// AC2 – Do not override Office-specific Contract Manager
  ///
  /// Given a Child Office is created with an Office-specific Contract Manager provided
  /// When PDP processes the Child Office creation
  /// Then PDP assigns the provided Office-specific Contract Manager
  /// And does not apply the Firm default.
  @Test
  void dstew1663_ac2_officeSpecificCmProvided_isUsedInsteadOfFirmDefault() {
    long ts = System.currentTimeMillis();
    String firmNumber = createIsolatedLspFirm("DSTEW-1663-AC2", ts, firstContractManagerGuid);

    String childOfficeCode =
        given()
            .contentType(ContentType.JSON)
            .pathParam("firmId", firmNumber)
            .body(
                Map.of(
                    "address",
                    Map.of(
                        "line1", "1 Explicit CM Street",
                        "townOrCity", "London",
                        "postcode", "EC1A 1BB"),
                    "payment",
                    Map.of("paymentMethod", "CHECK"),
                    "contractManager",
                    Map.of("contractManagerGUID", secondContractManagerGuid),
                    "liaisonManager",
                    Map.of("useHeadOfficeLiaisonManager", true)))
            .when()
            .post("/provider-firms/{firmId}/offices")
            .then()
            .statusCode(201)
            .extract()
            .path("data.officeCode");

    given()
        .pathParam("firmId", firmNumber)
        .pathParam("officeCode", childOfficeCode)
        .when()
        .get("/provider-firms/{firmId}/offices/{officeCode}/contract-managers")
        .then()
        .statusCode(200)
        .body("data.content", hasSize(1))
        .body("data.content[0].guid", equalTo(secondContractManagerGuid));
  }

  /// AC3 – Trickle-down is one-off
  ///
  /// Given a Child Office received its Contract Manager via Firm-level trickle-down
  /// When the Firm's Contract Manager changes later
  /// Then the Child Office Contract Manager remains unchanged.
  @Test
  void dstew1663_ac3_firmCmChangesLater_childOfficeCmUnchanged() {
    long ts = System.currentTimeMillis();
    String firmNumber = createIsolatedLspFirm("DSTEW-1663-AC3", ts, firstContractManagerGuid);

    String headOfficeCode =
        given()
            .pathParam("firmId", firmNumber)
            .when()
            .get("/provider-firms/{firmId}/offices")
            .then()
            .statusCode(200)
            .extract()
            .path("data.content[0].accountNumber");

    String childOfficeCode =
        given()
            .contentType(ContentType.JSON)
            .pathParam("firmId", firmNumber)
            .body(
                Map.of(
                    "address",
                    Map.of(
                        "line1", "1 One-Off Trickle Street",
                        "townOrCity", "London",
                        "postcode", "EC1A 1BB"),
                    "payment",
                    Map.of("paymentMethod", "CHECK"),
                    "contractManager",
                    Map.of("useHeadOfficeContractManager", true),
                    "liaisonManager",
                    Map.of("useHeadOfficeLiaisonManager", true)))
            .when()
            .post("/provider-firms/{firmId}/offices")
            .then()
            .statusCode(201)
            .extract()
            .path("data.officeCode");

    // Reassign the head office's Contract Manager via the standalone assignment endpoint.
    given()
        .contentType(ContentType.JSON)
        .pathParam("firmId", firmNumber)
        .pathParam("officeCode", headOfficeCode)
        .body(Map.of("contractManagerGUID", secondContractManagerGuid))
        .when()
        .post("/provider-firms/{firmId}/offices/{officeCode}/contract-managers")
        .then()
        .statusCode(201);

    // Head office now has the new Contract Manager.
    given()
        .pathParam("firmId", firmNumber)
        .pathParam("officeCode", headOfficeCode)
        .when()
        .get("/provider-firms/{firmId}/offices/{officeCode}/contract-managers")
        .then()
        .statusCode(200)
        .body("data.content[0].guid", equalTo(secondContractManagerGuid));

    // Child office's Contract Manager must remain the original one - no cascade.
    given()
        .pathParam("firmId", firmNumber)
        .pathParam("officeCode", childOfficeCode)
        .when()
        .get("/provider-firms/{firmId}/offices/{officeCode}/contract-managers")
        .then()
        .statusCode(200)
        .body("data.content", hasSize(1))
        .body("data.content[0].guid", equalTo(firstContractManagerGuid));
  }

  /// Creates an isolated LSP firm with the given head-office Contract Manager GUID, and returns
  /// its firm number.
  private static String createIsolatedLspFirm(String tag, long ts, String contractManagerGuid) {
    return given()
        .contentType(ContentType.JSON)
        .body(
            Map.of(
                "firmType",
                "Legal Services Provider",
                "name",
                "E2E-" + tag + " " + ts,
                "legalServicesProvider",
                Map.of(
                    "constitutionalStatus",
                    "Partnership",
                    "address",
                    Map.of(
                        "line1", "1 Isolated Street",
                        "townOrCity", "London",
                        "postcode", "EC1A 1BB"),
                    "payment",
                    Map.of("paymentMethod", "CHECK"),
                    "contractManager",
                    Map.of("contractManagerGUID", contractManagerGuid),
                    "liaisonManager",
                    Map.of(
                        "firstName", "Isolated",
                        "lastName", "LiaisonMgr",
                        "emailAddress",
                            "isolated.lm." + tag.toLowerCase() + "." + ts + "@example.com",
                        "telephoneNumber", "020 1234 5678"))))
        .when()
        .post("/provider-firms")
        .then()
        .statusCode(201)
        .extract()
        .path("data.providerFirmNumber");
  }
}
