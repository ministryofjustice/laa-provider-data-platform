package uk.gov.justice.laa.providerdata.e2e.modifying;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

import io.restassured.http.ContentType;
import java.util.Map;
import org.junit.jupiter.api.Test;
import uk.gov.justice.laa.providerdata.e2e.ModifyingTest;

/// Data-modifying end-to-end tests for PDS provider amendments.
@ModifyingTest
class AmendPublicDefenderServiceE2eTest {

  @Test
  void dstew2025_ac1_validAmendment_updatesPdsProvider() {
    String firmNumber = createPds("E2E-DSTEW-2025 Valid " + System.currentTimeMillis());
    String originalGuid = getPdsField(firmNumber, "data.providerFirmGUID");
    String originalAccountNumber =
        getPdsField(firmNumber, "data.publicDefenderService.headOffice.accountNumber");

    given()
        .contentType(ContentType.JSON)
        .body(
            Map.of(
                "name",
                "E2E-DSTEW-2025 Amended",
                "publicDefenderService",
                Map.of(
                    "companiesHouseNumber",
                    "12345678",
                    "headOffice",
                    Map.of(
                        "telephoneNumber",
                        "01234567890",
                        "address",
                        Map.of(
                            "line1",
                            "2 Amended Street",
                            "townOrCity",
                            "Birmingham",
                            "postcode",
                            "B2 2BB")))))
        .pathParam("firmId", firmNumber)
        .when()
        .patch("/provider-firms/{firmId}")
        .then()
        .statusCode(200)
        .body("data.providerFirmNumber", equalTo(firmNumber));

    given()
        .pathParam("firmId", firmNumber)
        .when()
        .get("/provider-firms/{firmId}")
        .then()
        .statusCode(200)
        .body("data.name", equalTo("E2E-DSTEW-2025 Amended"))
        .body("data.providerFirmGUID", equalTo(originalGuid))
        .body("data.publicDefenderService.headOffice.accountNumber", equalTo(originalAccountNumber))
        .body("data.publicDefenderService.companiesHouseNumber", equalTo("12345678"))
        .body("data.publicDefenderService.headOffice.address.line1", equalTo("2 Amended Street"))
        .body("data.publicDefenderService.headOffice.address.townOrCity", equalTo("Birmingham"))
        .body("data.publicDefenderService.headOffice.address.postcode", equalTo("B2 2BB"))
        .body("data.publicDefenderService.headOffice.telephoneNumber", equalTo("01234567890"));
  }

  @Test
  void dstew2025_ac2_dxNumberWithoutCentre_returns400() {
    assertInvalidPatch(
        Map.of(
            "publicDefenderService",
            Map.of("headOffice", Map.of("dxDetails", Map.of("dxNumber", "12345")))),
        "dxNumber and dxCentre");
  }

  @Test
  void dstew2025_ac2_dxCentreWithoutNumber_returns400() {
    assertInvalidPatch(
        Map.of(
            "publicDefenderService",
            Map.of("headOffice", Map.of("dxDetails", Map.of("dxCentre", "BIRMINGHAM")))),
        "dxNumber and dxCentre");
  }

  @Test
  void dstew2025_ac3_blankMandatoryAddress_returns400WithValidationDetail() {
    assertInvalidPatch(
        Map.of(
            "publicDefenderService", Map.of("headOffice", Map.of("address", Map.of("line1", "")))),
        "Mandatory address fields");
  }

  @Test
  void dstew2025_ac3_blankMandatoryName_returns400WithValidationDetail() {
    assertInvalidPatch(
        Map.of(
            "name",
            " ",
            "publicDefenderService",
            Map.of("constitutionalStatus", "Government Funded Organisation")),
        "name must not be blank");
  }

  @Test
  void dstew2025_ac3_invalidConstitutionalStatus_returns400WithValidationDetail() {
    String firmNumber = createPds("E2E-DSTEW-2025 Invalid Status " + System.currentTimeMillis());
    String originalName = getPdsField(firmNumber, "data.name");
    String originalStatus =
        getPdsField(firmNumber, "data.publicDefenderService.constitutionalStatus");

    given()
        .contentType(ContentType.JSON)
        .body(
            Map.of(
                "publicDefenderService",
                Map.of("constitutionalStatus", "Invalid Constitutional Status")))
        .pathParam("firmId", firmNumber)
        .when()
        .patch("/provider-firms/{firmId}")
        .then()
        .statusCode(400)
        .body("error.errorCode", equalTo("P00XX"))
        .body("detail", notNullValue());

    given()
        .pathParam("firmId", firmNumber)
        .when()
        .get("/provider-firms/{firmId}")
        .then()
        .statusCode(200)
        .body("data.name", equalTo(originalName))
        .body("data.publicDefenderService.constitutionalStatus", equalTo(originalStatus));
  }

  @Test
  void dstew2025_ac3_blankMandatoryTown_returns400WithValidationDetail() {
    assertInvalidPatch(
        Map.of(
            "publicDefenderService",
            Map.of("headOffice", Map.of("address", Map.of("townOrCity", " ")))),
        "Mandatory address fields");
  }

  @Test
  void dstew2025_ac3_blankMandatoryPostcode_returns400WithValidationDetail() {
    assertInvalidPatch(
        Map.of(
            "publicDefenderService",
            Map.of("headOffice", Map.of("address", Map.of("postcode", " ")))),
        "Mandatory address fields");
  }

  @Test
  void dstew2025_ac4_providerIdentifiersAreRejectedAndUnchanged() {
    String firmNumber = createPds("E2E-DSTEW-2025 Provider Redacted " + System.currentTimeMillis());
    String originalName = getPdsField(firmNumber, "data.name");

    given()
        .contentType(ContentType.JSON)
        .body(
            Map.of(
                "providerFirmGUID",
                "00000000-0000-0000-0000-000000000000",
                "providerFirmNumber",
                "REDACTED",
                "name",
                "Should not persist"))
        .pathParam("firmId", firmNumber)
        .when()
        .patch("/provider-firms/{firmId}")
        .then()
        .statusCode(400)
        .body("detail", containsString("must not be provided"));

    given()
        .pathParam("firmId", firmNumber)
        .when()
        .get("/provider-firms/{firmId}")
        .then()
        .statusCode(200)
        .body("data.name", equalTo(originalName))
        .body("data.firmNumber", equalTo(firmNumber));
  }

  @Test
  void dstew2025_ac4_providerAccountNumberIsRejectedAndUnchanged() {
    assertProviderIdentifierRejected("accountNumber", "REDACTED");
  }

  @Test
  void dstew2025_ac4_providerHeadOfficeFlagIsRejectedAndUnchanged() {
    assertProviderIdentifierRejected("headOfficeFlag", false);
  }

  private static void assertProviderIdentifierRejected(String field, Object value) {
    String firmNumber = createPds("E2E-DSTEW-2025 Provider Redacted " + System.currentTimeMillis());
    String originalName = getPdsField(firmNumber, "data.name");

    given()
        .contentType(ContentType.JSON)
        .body(
            Map.of(
                "publicDefenderService",
                Map.of(
                    field,
                    value,
                    "headOffice",
                    Map.of("address", Map.of("line1", "Should not persist")))))
        .pathParam("firmId", firmNumber)
        .when()
        .patch("/provider-firms/{firmId}")
        .then()
        .statusCode(400)
        .body("detail", containsString("must not be provided"));

    given()
        .pathParam("firmId", firmNumber)
        .when()
        .get("/provider-firms/{firmId}")
        .then()
        .statusCode(200)
        .body("data.name", equalTo(originalName));
  }

  @Test
  void dstew2025_ac4_officeGuidIsRejectedAndUnchanged() {
    assertOfficeIdentifierRejected("officeGUID", "00000000-0000-0000-0000-000000000000");
  }

  @Test
  void dstew2025_ac4_officeAccountNumberIsRejectedAndUnchanged() {
    assertOfficeIdentifierRejected("accountNumber", "REDACTED");
  }

  @Test
  void dstew2025_ac4_headOfficeFlagIsRejectedAndUnchanged() {
    assertOfficeIdentifierRejected("headOfficeFlag", false);
  }

  private static void assertOfficeIdentifierRejected(String field, Object value) {
    String firmNumber = createPds("E2E-DSTEW-2025 Office Redacted " + System.currentTimeMillis());
    String originalLine1 =
        getPdsField(firmNumber, "data.publicDefenderService.headOffice.address.line1");

    given()
        .contentType(ContentType.JSON)
        .body(
            Map.of(
                "publicDefenderService",
                Map.of(
                    "headOffice",
                    Map.of(field, value, "address", Map.of("line1", "Should not persist")))))
        .pathParam("firmId", firmNumber)
        .when()
        .patch("/provider-firms/{firmId}")
        .then()
        .statusCode(400)
        .body("detail", containsString("must not be provided"));

    given()
        .pathParam("firmId", firmNumber)
        .when()
        .get("/provider-firms/{firmId}")
        .then()
        .statusCode(200)
        .body("data.publicDefenderService.headOffice.address.line1", equalTo(originalLine1));
  }

  @Test
  void dstew2025_ac5_invalidAmendmentDoesNotPersistAnyChanges() {
    String firmNumber = createPds("E2E-DSTEW-2025 Atomicity " + System.currentTimeMillis());
    String originalName = getPdsField(firmNumber, "data.name");
    String originalLine1 =
        getPdsField(firmNumber, "data.publicDefenderService.headOffice.address.line1");

    given()
        .contentType(ContentType.JSON)
        .body(
            Map.of(
                "name",
                "Should not persist",
                "publicDefenderService",
                Map.of(
                    "headOffice",
                    Map.of(
                        "address",
                        Map.of("line1", "Should not persist"),
                        "dxDetails",
                        Map.of("dxNumber", "12345")))))
        .pathParam("firmId", firmNumber)
        .when()
        .patch("/provider-firms/{firmId}")
        .then()
        .statusCode(400)
        .body("detail", containsString("dxNumber and dxCentre"));

    given()
        .pathParam("firmId", firmNumber)
        .when()
        .get("/provider-firms/{firmId}")
        .then()
        .statusCode(200)
        .body("data.name", equalTo(originalName))
        .body("data.publicDefenderService.headOffice.address.line1", equalTo(originalLine1));
  }

  private static void assertInvalidPatch(Map<String, Object> body, String detail) {
    String firmNumber = createPds("E2E-DSTEW-2025 Invalid " + System.currentTimeMillis());
    String originalName = getPdsField(firmNumber, "data.name");
    String originalLine1 =
        getPdsField(firmNumber, "data.publicDefenderService.headOffice.address.line1");

    given()
        .contentType(ContentType.JSON)
        .body(body)
        .pathParam("firmId", firmNumber)
        .when()
        .patch("/provider-firms/{firmId}")
        .then()
        .statusCode(400)
        .body("error.errorCode", equalTo("P00XX"))
        .body("detail", containsString(detail));

    given()
        .pathParam("firmId", firmNumber)
        .when()
        .get("/provider-firms/{firmId}")
        .then()
        .statusCode(200)
        .body("data.name", equalTo(originalName))
        .body("data.publicDefenderService.headOffice.address.line1", equalTo(originalLine1));
  }

  private static String getPdsField(String firmNumber, String path) {
    return given()
        .pathParam("firmId", firmNumber)
        .when()
        .get("/provider-firms/{firmId}")
        .then()
        .statusCode(200)
        .extract()
        .path(path);
  }

  private static String createPds(String name) {
    return given()
        .contentType(ContentType.JSON)
        .body(
            Map.of(
                "name",
                name,
                "constitutionalStatus",
                "Government Funded Organisation",
                "headOffice",
                Map.of(
                    "address",
                    Map.of(
                        "line1",
                        "1 E2E Street",
                        "townOrCity",
                        "Birmingham",
                        "postcode",
                        "B1 1AA"))))
        .when()
        .post("/provider-firms/public-defender-services")
        .then()
        .statusCode(201)
        .extract()
        .path("data.providerFirmNumber");
  }
}
