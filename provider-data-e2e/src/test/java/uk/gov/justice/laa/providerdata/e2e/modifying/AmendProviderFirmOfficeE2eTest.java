package uk.gov.justice.laa.providerdata.e2e.modifying;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;

import io.restassured.http.ContentType;
import io.restassured.response.ValidatableResponse;
import java.util.Map;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import uk.gov.justice.laa.providerdata.e2e.E2eConfig;
import uk.gov.justice.laa.providerdata.e2e.ModifyingTest;

/// Data-modifying E2E tests for `PATCH /provider-firms/{firmId}/offices/{officeCode}`
/// covering DSTEW-1668 (DS_MAPD_FR_043): Amend Legal Organisation Child Office.
///
/// A single isolated LSP firm with one child office is created once in `setUp()` and shared
/// across all tests. Tests that expect rejection assert the office is unchanged afterwards;
/// tests that expect success verify the change via a subsequent GET.
@ModifyingTest
class AmendProviderFirmOfficeE2eTest {

  private static String firmNumber;
  private static String childOfficeCode;

  @BeforeAll
  static void setUp() {
    firmNumber = createLspFirm("Amend", "1 Amend Test Street");
    childOfficeCode =
        createChildOffice(
            firmNumber,
            "1 Child Office Street",
            "Manchester",
            "M1 1AA",
            Map.of("useHeadOfficeContractManager", true));

    // Seed DX details so AC3 and AC4 tests can verify that existing DX is preserved on rejection
    // or when dxDetails is omitted from a patch.
    patchOffice(
            firmNumber,
            childOfficeCode,
            Map.of("dxDetails", Map.of("dxNumber", "DX 12345", "dxCentre", "Manchester")))
        .statusCode(200);
  }

  /// Creates an isolated LSP head office firm with a default Contract Manager and a Liaison
  /// Manager whose first name is {@code namePrefix}, for use as a fixture in tests that need
  /// their own independent firm/office pair.
  private static String createLspFirm(String namePrefix, String addressLine1) {
    long ts = System.currentTimeMillis();
    return given()
        .contentType(ContentType.JSON)
        .body(
            Map.of(
                "firmType",
                "Legal Services Provider",
                "name",
                "E2E-DSTEW-1668-" + namePrefix + " " + ts,
                "legalServicesProvider",
                Map.of(
                    "constitutionalStatus",
                    "Partnership",
                    "address",
                    Map.of(
                        "line1", addressLine1,
                        "townOrCity", "London",
                        "postcode", "EC1A 1BB"),
                    "payment",
                    Map.of("paymentMethod", "CHECK"),
                    "contractManager",
                    Map.of("useDefaultContractManager", true),
                    "liaisonManager",
                    Map.of(
                        "firstName",
                        namePrefix,
                        "lastName",
                        "TestLm",
                        "emailAddress",
                        namePrefix.toLowerCase() + ".lm." + ts + "@example.com",
                        "telephoneNumber",
                        "020 1111 2222"))))
        .when()
        .post("/provider-firms")
        .then()
        .statusCode(201)
        .extract()
        .path("data.providerFirmNumber");
  }

  /// Creates a child office under {@code firmNumber}, using the head office's Liaison Manager and
  /// the given Contract Manager instruction.
  private static String createChildOffice(
      String parentFirmNumber,
      String addressLine1,
      String city,
      String postcode,
      Map<String, Object> contractManager) {
    return given()
        .contentType(ContentType.JSON)
        .pathParam("firmId", parentFirmNumber)
        .body(
            Map.of(
                "address",
                Map.of(
                    "line1", addressLine1,
                    "townOrCity", city,
                    "postcode", postcode),
                "payment",
                Map.of("paymentMethod", "CHECK"),
                "contractManager",
                contractManager,
                "liaisonManager",
                Map.of("useHeadOfficeLiaisonManager", true)))
        .when()
        .post("/provider-firms/{firmId}/offices")
        .then()
        .statusCode(201)
        .extract()
        .path("data.officeCode");
  }

  /// PATCHes the given office with the given (partial) body.
  private static ValidatableResponse patchOffice(
      String firmId, String officeCode, Map<String, Object> body) {
    return given()
        .contentType(ContentType.JSON)
        .pathParam("firmId", firmId)
        .pathParam("officeCode", officeCode)
        .body(body)
        .when()
        .patch("/provider-firms/{firmId}/offices/{officeCode}")
        .then();
  }

  /// GETs the given office.
  private static ValidatableResponse getOffice(String firmId, String officeCode) {
    return given()
        .pathParam("firmId", firmId)
        .pathParam("officeCode", officeCode)
        .when()
        .get("/provider-firms/{firmId}/offices/{officeCode}")
        .then();
  }

  /// PATCHes a child office with valid optional fields and verifies the changes are persisted.
  ///
  /// - DSTEW-1668 AC1 – a valid amendment is accepted and the updated record is returned.
  ///   (DS_MAPD_FR_043)
  ///
  /// - DS_MAPD_FR_043: Amend Child Office – permitted fields may be updated; the record remains
  ///   valid after amendment.
  @Test
  void dstew1668_ac1_amendOffice_withValidData_changesPersisted() {
    patchOffice(
            firmNumber,
            childOfficeCode,
            Map.of(
                "address",
                Map.of(
                    "line1", "99 Amended Street",
                    "townOrCity", "Leeds",
                    "postcode", "LS1 1AA"),
                "telephoneNumber",
                "0113 999 8888"))
        .statusCode(200)
        .body("data.providerFirmGUID", notNullValue())
        .body("data.providerFirmNumber", equalTo(firmNumber))
        .body("data.officeGUID", notNullValue())
        .body("data.officeCode", equalTo(childOfficeCode));

    getOffice(firmNumber, childOfficeCode)
        .statusCode(200)
        .body("data.address.line1", equalTo("99 Amended Street"))
        .body("data.address.townOrCity", equalTo("Leeds"))
        .body("data.address.postcode", equalTo("LS1 1AA"))
        .body("data.telephoneNumber", equalTo("0113 999 8888"));
  }

  /// PATCHes a child office with an empty string for a mandatory address field; verifies the
  /// request is rejected with 400 and the address is unchanged.
  ///
  /// `OfficeAddressPatchV2` enforces `minLength: 1` on all fields — an empty string for any
  /// field is rejected at schema validation level.
  ///
  /// - DSTEW-1668 AC2 – an amendment that would cause a mandatory field to become blank or be
  ///   removed must be rejected; the record remains unchanged. (DS_MAPD_FR_043)
  @Test
  void dstew1668_ac2_amendOffice_blankMandatoryField_returns400AndRecordUnchanged() {
    String line1Before =
        getOffice(firmNumber, childOfficeCode).statusCode(200).extract().path("data.address.line1");

    patchOffice(firmNumber, childOfficeCode, Map.of("address", Map.of("line1", "")))
        .statusCode(400);

    getOffice(firmNumber, childOfficeCode)
        .statusCode(200)
        .body("data.address.line1", equalTo(line1Before));
  }

  /// PATCHes a child office with only `line1` in the address object; verifies the request is
  /// accepted and that unprovided address fields (`townOrCity`, `postcode`) are left unchanged.
  ///
  /// Absent fields are treated as "leave unchanged" under merge-patch semantics.
  ///
  /// - DSTEW-1668 AC2 – a valid partial update that leaves all mandatory fields populated is
  ///   accepted. (DS_MAPD_FR_043)
  @Test
  void dstew1668_ac2_amendOffice_partialAddressUpdate_omittedFieldsUnchanged() {
    String townOrCityBefore =
        getOffice(firmNumber, childOfficeCode)
            .statusCode(200)
            .extract()
            .path("data.address.townOrCity");

    String postcodeBefore =
        getOffice(firmNumber, childOfficeCode)
            .statusCode(200)
            .extract()
            .path("data.address.postcode");

    patchOffice(firmNumber, childOfficeCode, Map.of("address", Map.of("line1", "Updated Street")))
        .statusCode(200);

    getOffice(firmNumber, childOfficeCode)
        .statusCode(200)
        .body("data.address.line1", equalTo("Updated Street"))
        .body("data.address.townOrCity", equalTo(townOrCityBefore))
        .body("data.address.postcode", equalTo(postcodeBefore));
  }

  /// PATCHes a child office with only `dxNumber` (no `dxCentre`); verifies the request is
  /// rejected and the DX details are unchanged.
  ///
  /// - DSTEW-1668 AC3 – if only one DX field is provided, the request is rejected.
  ///   (DS_MAPD_FR_043)
  ///
  /// - DS_MAPD_FR_043: Amend Child Office – DX Number and DX Centre are conditionally required;
  ///   both must be provided or neither (BR-11).
  @Test
  void dstew1668_ac3_amendOffice_dxNumberWithoutCentre_returns400AndDxUnchanged() {
    Object dxBefore =
        getOffice(firmNumber, childOfficeCode).statusCode(200).extract().path("data.dxDetails");

    patchOffice(firmNumber, childOfficeCode, Map.of("dxDetails", Map.of("dxNumber", "DX 99001")))
        .statusCode(400);

    getOffice(firmNumber, childOfficeCode)
        .statusCode(200)
        .body("data.dxDetails", equalTo(dxBefore));
  }

  /// PATCHes a child office with only `dxCentre` (no `dxNumber`); verifies the request is
  /// rejected and the DX details are unchanged.
  ///
  /// - DSTEW-1668 AC3 – if only one DX field is provided, the request is rejected.
  ///   (DS_MAPD_FR_043)
  @Test
  void dstew1668_ac3_amendOffice_dxCentreWithoutNumber_returns400AndDxUnchanged() {
    Object dxBefore =
        getOffice(firmNumber, childOfficeCode).statusCode(200).extract().path("data.dxDetails");

    patchOffice(firmNumber, childOfficeCode, Map.of("dxDetails", Map.of("dxCentre", "Leeds")))
        .statusCode(400);

    getOffice(firmNumber, childOfficeCode)
        .statusCode(200)
        .body("data.dxDetails", equalTo(dxBefore));
  }

  /// PATCHes a child office without providing `dxDetails`; verifies the request is accepted and
  /// the existing DX state is left unchanged.
  ///
  /// - DSTEW-1668 AC4 – if neither DX field is provided, the request is accepted and DX is
  ///   unchanged. (DS_MAPD_FR_043)
  ///
  /// - DS_MAPD_FR_043: Amend Child Office – omitting `dxDetails` entirely leaves DX state
  ///   untouched (BR-11).
  @Test
  void dstew1668_ac4_amendOffice_noDxFields_dxUnchanged() {
    Object dxBefore =
        getOffice(firmNumber, childOfficeCode).statusCode(200).extract().path("data.dxDetails");

    patchOffice(firmNumber, childOfficeCode, Map.of("telephoneNumber", "0113 111 2233"))
        .statusCode(200);

    getOffice(firmNumber, childOfficeCode)
        .statusCode(200)
        .body("data.telephoneNumber", equalTo("0113 111 2233"))
        .body("data.dxDetails", equalTo(dxBefore));
  }

  /// PATCHes a child office with both `dxNumber` and `dxCentre`; verifies the DX details are
  /// persisted.
  ///
  /// - DSTEW-1668 AC3 – both DX fields provided together is valid. (DS_MAPD_FR_043)
  ///
  /// - DS_MAPD_FR_043: Amend Child Office – a complete `dxDetails` object is accepted (BR-11).
  @Test
  void dstew1668_ac3_amendOffice_withBothDxFields_dxDetailsPersisted() {
    String firmNum = createLspFirm("DX", "1 DX Street");
    String officeCode =
        createChildOffice(
            firmNum,
            "1 Child DX Street",
            "Birmingham",
            "B1 1AA",
            Map.of("useHeadOfficeContractManager", true));

    patchOffice(
            firmNum,
            officeCode,
            Map.of("dxDetails", Map.of("dxNumber", "DX 13009", "dxCentre", "Birmingham")))
        .statusCode(200);

    getOffice(firmNum, officeCode)
        .statusCode(200)
        .body("data.dxDetails.dxNumber", equalTo("DX 13009"))
        .body("data.dxDetails.dxCentre", equalTo("Birmingham"));
  }

  /// PATCHes a child office including `providerFirmGUID` (a redacted field that identifies the
  /// parent organisation); verifies the request is rejected and the record is unchanged.
  ///
  /// - DSTEW-1668 AC6 – redacted fields in the request body are rejected. (DS_MAPD_FR_043)
  ///
  /// - DS_MAPD_FR_043: Amend Child Office – the parent LSP GUID is read-only and must not be
  ///   present in an amendment request. Enforced via `@RejectProperties` on `OfficePatchV2`
  ///   subtypes and in the type resolver before subtype resolution.
  @Test
  void dstew1668_ac6_amendOffice_withParentGuid_returns400AndRecordUnchanged() {
    String phoneBefore =
        getOffice(firmNumber, childOfficeCode)
            .statusCode(200)
            .extract()
            .path("data.telephoneNumber");

    patchOffice(
            firmNumber,
            childOfficeCode,
            Map.of(
                "providerFirmGUID", "00000000-0000-0000-0000-000000000000",
                "telephoneNumber", "0113 000 0001"))
        .statusCode(400);

    getOffice(firmNumber, childOfficeCode)
        .statusCode(200)
        .body("data.telephoneNumber", equalTo(phoneBefore));
  }

  /// Verifies that a Contract Manager remains assigned after a valid amendment to other fields.
  ///
  /// - DSTEW-1668 AC5 – an amendment must not result in no Contract Manager assigned.
  ///   (DS_MAPD_FR_043)
  ///
  /// - DS_MAPD_FR_043: Amend Child Office – the CM field is not present in `LSPOfficePatchV2`
  ///   so a PATCH cannot remove the CM; this test confirms that structural guarantee holds.
  @Test
  void dstew1668_ac5_amendOffice_cmRemainsAssigned_afterValidAmendment() {
    String cmGuid =
        given()
            .queryParam("contractManagerId", E2eConfig.contractManagerId())
            .when()
            .get("/provider-contract-managers")
            .then()
            .statusCode(200)
            .extract()
            .path("data.content[0].guid");

    String firmNum = createLspFirm("CM", "1 CM Street");
    String officeCode =
        createChildOffice(
            firmNum,
            "1 CM Office Street",
            "Bristol",
            "BS1 1AA",
            Map.of("contractManagerGUID", cmGuid));

    // Verify CM is assigned before amendment.
    given()
        .pathParam("firmId", firmNum)
        .pathParam("officeCode", officeCode)
        .when()
        .get("/provider-firms/{firmId}/offices/{officeCode}/contract-managers")
        .then()
        .statusCode(200)
        .body("data.content", hasSize(1));

    // Amend an unrelated field.
    patchOffice(firmNum, officeCode, Map.of("telephoneNumber", "0117 999 0000")).statusCode(200);

    // CM must still be assigned after the amendment.
    given()
        .pathParam("firmId", firmNum)
        .pathParam("officeCode", officeCode)
        .when()
        .get("/provider-firms/{firmId}/offices/{officeCode}/contract-managers")
        .then()
        .statusCode(200)
        .body("data.content", hasSize(1))
        .body("data.content[0].contractManagerId", equalTo(E2eConfig.contractManagerId()));
  }

  /// PATCHes a child office including `officeGUID` (a redacted field); verifies the request is
  /// rejected and the record is unchanged.
  ///
  /// - DSTEW-1668 AC6 – redacted fields in the request body are rejected. (DS_MAPD_FR_043)
  ///
  /// - DS_MAPD_FR_043: Amend Child Office – `officeGUID` and `accountNumber` are read-only and
  ///   must not be present in an amendment request. Enforced via `@RejectProperties` on
  ///   `LSPOfficePatchV2`.
  @Test
  void dstew1668_ac6_amendOffice_withOfficeGuid_returns400AndRecordUnchanged() {
    String phoneBefore =
        getOffice(firmNumber, childOfficeCode)
            .statusCode(200)
            .extract()
            .path("data.telephoneNumber");

    patchOffice(
            firmNumber,
            childOfficeCode,
            Map.of(
                "officeGUID", "00000000-0000-0000-0000-000000000000",
                "telephoneNumber", "0113 000 0002"))
        .statusCode(400);

    getOffice(firmNumber, childOfficeCode)
        .statusCode(200)
        .body("data.telephoneNumber", equalTo(phoneBefore));
  }

  /// PATCHes a child office including `accountNumber` (a redacted field); verifies the request
  /// is rejected and the record is unchanged.
  ///
  /// - DSTEW-1668 AC6 – redacted fields in the request body are rejected. (DS_MAPD_FR_043)
  ///
  /// - DS_MAPD_FR_043: Amend Child Office – see
  ///   {@link #dstew1668_ac6_amendOffice_withOfficeGuid_returns400AndRecordUnchanged}.
  @Test
  void dstew1668_ac6_amendOffice_withAccountNumber_returns400AndRecordUnchanged() {
    String phoneBefore =
        getOffice(firmNumber, childOfficeCode)
            .statusCode(200)
            .extract()
            .path("data.telephoneNumber");

    patchOffice(
            firmNumber,
            childOfficeCode,
            Map.of(
                "accountNumber", "FAKE001",
                "telephoneNumber", "0113 000 0003"))
        .statusCode(400);

    getOffice(firmNumber, childOfficeCode)
        .statusCode(200)
        .body("data.telephoneNumber", equalTo(phoneBefore));
  }
}
