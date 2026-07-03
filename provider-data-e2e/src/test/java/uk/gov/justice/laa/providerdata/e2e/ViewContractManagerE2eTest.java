package uk.gov.justice.laa.providerdata.e2e;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

import org.junit.jupiter.api.Test;

/// Read-only E2E tests for DSTEW-1662 (DS_MAPD_FR_028): View Contract Manager details.
///
/// Two retrieval modes exist:
///
/// 1. Global list — `GET /provider-contract-managers` (AC1): returns all available CMs for
///    selection purposes; no entity context required.
/// 2. Entity-scoped — `GET /provider-firms/{firmId}/offices/{officeCode}/contract-managers`
///    (AC2–AC5): returns the CM assigned to a specific office. "Legal Organisation (LSP)" in the
///    ticket refers to the LSP head office, using the same office-scoped endpoint.
///
/// Fixture notes:
///
/// - LSP1 head office ({@link E2eConfig#lspOfficeCode()}) has a CM assigned
///   ({@link E2eConfig#contractManagerId()}).
/// - LSP2 child office ({@link E2eConfig#lsp2ChildOfficeCode()}) has a different CM assigned
///   ({@link E2eConfig#lsp2ContractManagerId()}).
/// - LSP2 head office ({@link E2eConfig#lsp2HeadOfficeCode()}) has no CM assigned — used for AC3.
/// - "Person ID" in the ticket's Data Retrieved section maps to `contractManagerId` in the API.
/// - The `email` field is optional and is not populated for any seeded Contract Manager, so no
///   test asserts a specific email value (that would be a fixture-dependent assumption that may
///   not hold against other databases, e.g. production). Every request in this suite already runs
///   through {@code OpenApiValidationFilter} (see {@link E2eRestAssuredExtension}), which enforces
///   that `email`, when present, conforms to the `ContractManagerEmailAddressV2` schema (format:
///   email). Tests additionally assert that the field is either absent/null or contains an "@",
///   which holds against both the current fixture and any future/production data.
@ReadOnlyTest
class ViewContractManagerE2eTest {

  /// AC1 – View list of Contract Managers for selection
  ///
  /// Given Contract Managers exist in PDP
  /// When MAPD requests the list of Contract Managers
  /// Then PDP returns the list of available Contract Managers
  ///
  /// Filtered to a single known CM (by contractManagerId) to allow specific field assertions; the
  /// unfiltered list behaviour is covered by {@code ProviderContractManagersE2eTest}.
  @Test
  void dstew1662_ac1_listContractManagers_returnsExpectedFields() {
    given()
        .queryParam("contractManagerId", E2eConfig.contractManagerId())
        .when()
        .get("/provider-contract-managers")
        .then()
        .statusCode(200)
        .body("data.content", hasSize(1))
        .body("data.content[0].guid", notNullValue())
        .body("data.content[0].contractManagerId", equalTo(E2eConfig.contractManagerId()))
        .body("data.content[0].firstName", equalTo(E2eConfig.contractManagerFirstName()))
        .body("data.content[0].lastName", equalTo(E2eConfig.contractManagerLastName()))
        .body("data.content[0].email", anyOf(nullValue(), containsString("@")))
        .body("data.metadata.pagination.totalItems", equalTo(1));
  }

  /// AC2 – View Contract Manager for an entity
  ///
  /// Given a Legal Organisation (LSP) or Child Office exists in PDP
  /// And a Contract Manager is assigned to that entity
  /// When MAPD requests the Contract Manager details for the entity
  /// Then PDP returns the Contract Manager details
  ///
  /// Covers the "Legal Organisation (LSP)" case, using the LSP head office.
  @Test
  void dstew1662_ac2_viewCmForLspHeadOffice_returnsAssignedCm() {
    given()
        .pathParam("firmId", E2eConfig.lspFirmNumber())
        .pathParam("officeCode", E2eConfig.lspOfficeCode())
        .when()
        .get("/provider-firms/{firmId}/offices/{officeCode}/contract-managers")
        .then()
        .statusCode(200)
        .body("data.content", hasSize(1))
        .body("data.content[0].guid", notNullValue())
        .body("data.content[0].contractManagerId", equalTo(E2eConfig.contractManagerId()))
        .body("data.content[0].firstName", equalTo(E2eConfig.contractManagerFirstName()))
        .body("data.content[0].lastName", equalTo(E2eConfig.contractManagerLastName()))
        .body("data.content[0].email", anyOf(nullValue(), containsString("@")));
  }

  /// AC2 – View Contract Manager for an entity
  ///
  /// Given a Legal Organisation (LSP) or Child Office exists in PDP
  /// And a Contract Manager is assigned to that entity
  /// When MAPD requests the Contract Manager details for the entity
  /// Then PDP returns the Contract Manager details
  ///
  /// Covers the "Child Office" case.
  @Test
  void dstew1662_ac2_viewCmForChildOffice_returnsAssignedCm() {
    given()
        .pathParam("firmId", E2eConfig.lsp2FirmNumber())
        .pathParam("officeCode", E2eConfig.lsp2ChildOfficeCode())
        .when()
        .get("/provider-firms/{firmId}/offices/{officeCode}/contract-managers")
        .then()
        .statusCode(200)
        .body("data.content", hasSize(1))
        .body("data.content[0].guid", notNullValue())
        .body("data.content[0].contractManagerId", equalTo(E2eConfig.lsp2ContractManagerId()))
        .body("data.content[0].firstName", equalTo(E2eConfig.lsp2ContractManagerFirstName()))
        .body("data.content[0].lastName", equalTo(E2eConfig.lsp2ContractManagerLastName()))
        .body("data.content[0].email", anyOf(nullValue(), containsString("@")));
  }

  /// AC3 – No Contract Manager assigned to entity
  ///
  /// Given a Legal Organisation (LSP) or Child Office exists in PDP
  /// And no Contract Manager is assigned to that entity
  /// When MAPD requests the Contract Manager details for the entity
  /// Then PDP returns a response indicating no Contract Manager is assigned
  ///
  /// Uses the LSP2 head office, which has no contract manager linked in the seed data. An empty
  /// list (200, zero content items) is the confirmed correct representation of "no CM assigned".
  @Test
  void dstew1662_ac3_noCmAssigned_returnsEmptyList() {
    given()
        .pathParam("firmId", E2eConfig.lsp2FirmNumber())
        .pathParam("officeCode", E2eConfig.lsp2HeadOfficeCode())
        .when()
        .get("/provider-firms/{firmId}/offices/{officeCode}/contract-managers")
        .then()
        .statusCode(200)
        .body("data.content", empty())
        .body("data.metadata.pagination.totalItems", equalTo(0));
  }

  /// AC4 – Contract Manager details cannot be retrieved
  ///
  /// Given a request for Contract Manager data is submitted
  /// When PDP is unable to retrieve the Contract Manager details
  /// Then PDP returns an error message
  ///
  /// Covers the case where the parent firm does not exist at all.
  @Test
  void dstew1662_ac4_nonExistentFirm_returns404() {
    given()
        .pathParam("firmId", E2eConfig.invalidFirmNumber())
        .pathParam("officeCode", E2eConfig.lspOfficeCode())
        .when()
        .get("/provider-firms/{firmId}/offices/{officeCode}/contract-managers")
        .then()
        .statusCode(404)
        .body("error.errorCode", equalTo("P00NF"));
  }

  /// AC5 – Prevent standalone retrieval in entity context
  ///
  /// Given a request is made to retrieve a Contract Manager in the context of an entity
  /// When the entity identifier is not provided or invalid
  /// Then the request is rejected
  /// And an error message is returned
  ///
  /// Covers "not provided": confirmed manually (and via a failing test run) that an empty office
  /// code path segment cannot reach the server as a request at all — the OpenAPI request
  /// validation, which every test in this suite goes through (see
  /// {@link E2eRestAssuredExtension}), rejects it client-side with
  /// "Parameter 'officeGUIDorCode' is required but is missing" before the HTTP call is made. There
  /// is therefore no HTTP response to assert against for this exact sub-clause; the "invalid"
  /// sub-clause (non-existent/misscoped office code) is covered by the two tests below, which do
  /// exercise the server's own rejection path.
  @Test
  void dstew1662_ac5_nonExistentOfficeCode_returns404() {
    given()
        .pathParam("firmId", E2eConfig.lspFirmNumber())
        .pathParam("officeCode", E2eConfig.invalidOfficeCode())
        .when()
        .get("/provider-firms/{firmId}/offices/{officeCode}/contract-managers")
        .then()
        .statusCode(404)
        .body("error.errorCode", equalTo("P00NF"));
  }

  /// AC5 – Prevent standalone retrieval in entity context
  ///
  /// Given a request is made to retrieve a Contract Manager in the context of an entity
  /// When the entity identifier is not provided or invalid
  /// Then the request is rejected
  /// And an error message is returned
  ///
  /// Covers "invalid": a real office code that exists, but under a different firm than the one
  /// requested — confirms that lookups are scoped to the specified parent provider, not just the
  /// office code in isolation.
  @Test
  void dstew1662_ac5_officeCodeUnderWrongFirm_returns404() {
    given()
        .pathParam("firmId", E2eConfig.lsp2FirmNumber())
        .pathParam("officeCode", E2eConfig.lspOfficeCode())
        .when()
        .get("/provider-firms/{firmId}/offices/{officeCode}/contract-managers")
        .then()
        .statusCode(404)
        .body("error.errorCode", equalTo("P00NF"));
  }
}
