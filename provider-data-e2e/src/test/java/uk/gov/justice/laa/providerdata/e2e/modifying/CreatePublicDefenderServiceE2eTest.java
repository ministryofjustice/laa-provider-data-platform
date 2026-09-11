package uk.gov.justice.laa.providerdata.e2e.modifying;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.restassured.http.ContentType;
import io.restassured.response.Response;
import java.util.Map;
import java.util.Objects;
import org.junit.jupiter.api.Test;
import uk.gov.justice.laa.providerdata.e2e.ModifyingTest;

/// Data-modifying end-to-end tests for `POST /provider-firms/public-defender-services`.
@ModifyingTest
class CreatePublicDefenderServiceE2eTest {

  @Test
  void dstew2024_ac1_validPdsRequest_returns201WithGeneratedIdentifiers() {
    long timestamp = System.currentTimeMillis();
    String name = "E2E-DSTEW-2024 PDS " + timestamp;

    String firmNumber =
        given()
            .contentType(ContentType.JSON)
            .body(validRequest(name))
            .when()
            .post("/provider-firms/public-defender-services")
            .then()
            .statusCode(201)
            .body("data.providerFirmGUID", notNullValue())
            .body("data.providerFirmNumber", notNullValue())
            .extract()
            .path("data.providerFirmNumber");

    String providerResponse =
        given()
            .pathParam("firmId", firmNumber)
            .when()
            .get("/provider-firms/{firmId}")
            .then()
            .statusCode(200)
            .body("data.firmNumber", equalTo(firmNumber))
            .body("data.name", equalTo(name))
            .body("data.firmType", equalTo("Public Defender Service"))
            .body(
                "data.publicDefenderService.constitutionalStatus",
                equalTo("Government Funded Organisation"))
            .body("data.publicDefenderService.headOffice.accountNumber", notNullValue())
            .body("data.publicDefenderService.headOffice.address.line1", equalTo("1 E2E Street"))
            .body("data.publicDefenderService.headOffice.liaisonManager", equalTo(null))
            .body("data.publicDefenderService.headOffice.contractManager", equalTo(null))
            .body("data.publicDefenderService.headOffice.bankAccount", equalTo(null))
            .body("data.publicDefenderService.headOffice.contract", equalTo(null))
            .body("data.publicDefenderService.headOffice.schedule", equalTo(null))
            .body("data.legalServicesProvider", equalTo(null))
            .body("data.chambers", equalTo(null))
            .body("data.practitioner", equalTo(null))
            .extract()
            .asString();

    assertFalse(providerResponse.contains("\"liaisonManager\""));
    assertFalse(providerResponse.contains("\"contractManager\""));
    assertFalse(providerResponse.contains("\"bankAccount\""));
    assertFalse(providerResponse.contains("\"contract\""));
    assertFalse(providerResponse.contains("\"schedule\""));

    given()
        .pathParam("firmId", firmNumber)
        .when()
        .get("/provider-firms/{firmId}/offices")
        .then()
        .statusCode(200)
        .body("data.content", hasSize(1))
        .body("data.content[0].accountNumber", notNullValue())
        .body("data.content[0].firmType", equalTo("Public Defender Service"));
  }

  @Test
  void dstew2026_ac2_unknownPdsProvider_returns404() {
    given()
        .pathParam("firmId", "PDS-NOT-FOUND-" + System.currentTimeMillis())
        .when()
        .get("/provider-firms/{firmId}")
        .then()
        .statusCode(404)
        .body("error.errorCode", equalTo("P00NF"));
  }

  @Test
  void dstew2026_ac1_configuredPdsFields_areReturnedFromProviderAndOfficeEndpoints() {
    String name = "E2E-DSTEW-2026 Configured PDS " + System.currentTimeMillis();

    var createResponse =
        given()
            .contentType(ContentType.JSON)
            .body(configuredRequest(name))
            .when()
            .post("/provider-firms/public-defender-services")
            .then()
            .statusCode(201)
            .body("data.providerFirmGUID", notNullValue())
            .body("data.providerFirmNumber", notNullValue())
            .extract();

    String firmGuid = createResponse.path("data.providerFirmGUID");
    String firmNumber = createResponse.path("data.providerFirmNumber");

    String providerResponse =
        given()
            .pathParam("firmId", firmGuid)
            .when()
            .get("/provider-firms/{firmId}")
            .then()
            .statusCode(200)
            .body("data.guid", equalTo(firmGuid))
            .body("data.firmNumber", equalTo(firmNumber))
            .body("data.name", equalTo(name))
            .body("data.firmType", equalTo("Public Defender Service"))
            .body(
                "data.publicDefenderService.constitutionalStatus",
                equalTo("Government Funded Organisation"))
            .body("data.publicDefenderService.companiesHouseNumber", equalTo("12345678"))
            .body("data.publicDefenderService.indemnityReceivedDate", equalTo("2025-01-15"))
            .body("data.publicDefenderService.headOffice.officeGUID", notNullValue())
            .body("data.publicDefenderService.headOffice.accountNumber", notNullValue())
            .body("data.publicDefenderService.headOffice.address.line1", equalTo("1 PDS Street"))
            .body("data.publicDefenderService.headOffice.address.line2", equalTo("Floor 2"))
            .body("data.publicDefenderService.headOffice.address.line3", equalTo("Building A"))
            .body("data.publicDefenderService.headOffice.address.line4", equalTo("Civic Quarter"))
            .body("data.publicDefenderService.headOffice.address.townOrCity", equalTo("Birmingham"))
            .body("data.publicDefenderService.headOffice.address.county", equalTo("West Midlands"))
            .body("data.publicDefenderService.headOffice.address.postcode", equalTo("B1 1AA"))
            .body("data.publicDefenderService.headOffice.telephoneNumber", equalTo("0121 555 0101"))
            .body("data.publicDefenderService.headOffice.emailAddress", equalTo("pds@example.com"))
            .body("data.publicDefenderService.headOffice.website", equalTo("https://pds.example"))
            .body("data.publicDefenderService.headOffice.dxDetails.dxNumber", equalTo("DX 12345"))
            .body("data.publicDefenderService.headOffice.dxDetails.dxCentre", equalTo("Birmingham"))
            .body(
                "data.publicDefenderService.headOffice.vatRegistration.vatNumber",
                equalTo("GB123456789"))
            .body("data.publicDefenderService.headOffice.liaisonManager", equalTo(null))
            .body("data.publicDefenderService.headOffice.contractManager", equalTo(null))
            .body("data.publicDefenderService.headOffice.bankAccount", equalTo(null))
            .body("data.publicDefenderService.headOffice.contract", equalTo(null))
            .body("data.publicDefenderService.headOffice.schedule", equalTo(null))
            .extract()
            .asString();

    assertFalse(providerResponse.contains("\"liaisonManager\""));
    assertFalse(providerResponse.contains("\"contractManager\""));
    assertFalse(providerResponse.contains("\"bankAccount\""));
    assertFalse(providerResponse.contains("\"contract\""));
    assertFalse(providerResponse.contains("\"schedule\""));

    String officeGuid =
        given()
            .pathParam("firmId", firmNumber)
            .when()
            .get("/provider-firms/{firmId}/offices")
            .then()
            .statusCode(200)
            .body("data.content", hasSize(1))
            .body("data.content[0].guid", notNullValue())
            .body("data.content[0].firmType", equalTo("Public Defender Service"))
            .body("data.content[0].headOfficeFlag", equalTo(true))
            .extract()
            .path("data.content[0].guid");

    String officeResponse =
        given()
            .pathParam("firmId", firmNumber)
            .pathParam("officeId", officeGuid)
            .when()
            .get("/provider-firms/{firmId}/offices/{officeId}")
            .then()
            .statusCode(200)
            .body("data.guid", equalTo(officeGuid))
            .body("data.firmType", equalTo("Public Defender Service"))
            .body("data.accountNumber", notNullValue())
            .body("data.headOfficeFlag", equalTo(true))
            .body("data.address.line1", equalTo("1 PDS Street"))
            .body("data.dxDetails.dxNumber", equalTo("DX 12345"))
            .body("data.vatRegistration.vatNumber", equalTo("GB123456789"))
            .extract()
            .asString();

    assertFalse(officeResponse.contains("\"liaisonManager\""));
    assertFalse(officeResponse.contains("\"contractManager\""));
    assertFalse(officeResponse.contains("\"bankAccount\""));
    assertFalse(officeResponse.contains("\"contract\""));
    assertFalse(officeResponse.contains("\"schedule\""));

    // PATCH is setup only: DSTEW-2026 exercises the subsequent read, not amendment behaviour.
    given()
        .pathParam("firmId", firmNumber)
        .when()
        .get("/provider-firms/{firmId}")
        .then()
        .statusCode(200)
        .body("data.guid", equalTo(firmGuid))
        .body("data.firmNumber", equalTo(firmNumber))
        .body("data.name", equalTo(name));

    given()
        .contentType(ContentType.JSON)
        .pathParam("firmId", firmNumber)
        .body(
            Map.of(
                "name",
                name,
                "publicDefenderService",
                Map.of(
                    "firmIntervenedFlag",
                    true,
                    "firmIntervenedDate",
                    "2025-02-01",
                    "holdAllPaymentsFlag",
                    true,
                    "holdAllPaymentsReason",
                    "Annual review",
                    "referredToDebtRecoveryFlag",
                    true,
                    "headOffice",
                    Map.of(
                        "activeDateTo",
                        "2025-03-01",
                        "falseBalanceFlag",
                        true,
                        "payment",
                        Map.of("paymentHeldFlag", true, "paymentHeldReason", "Annual review")))))
        .when()
        .patch("/provider-firms/{firmId}")
        .then()
        .statusCode(200);

    given()
        .pathParam("firmId", firmNumber)
        .when()
        .get("/provider-firms/{firmId}")
        .then()
        .statusCode(200)
        .body("data.publicDefenderService.firmIntervenedFlag", equalTo(true))
        .body("data.publicDefenderService.firmIntervenedDate", equalTo("2025-02-01"))
        .body("data.publicDefenderService.holdAllPaymentsFlag", equalTo(true))
        .body("data.publicDefenderService.holdAllPaymentsReason", equalTo("Annual review"))
        .body("data.publicDefenderService.referredToDebtRecoveryFlag", equalTo(true))
        .body("data.publicDefenderService.headOffice.headOfficeFlag", equalTo(true))
        .body("data.publicDefenderService.headOffice.activeDateTo", equalTo("2025-03-01"))
        .body("data.publicDefenderService.headOffice.falseBalanceFlag", equalTo(true))
        .body("data.publicDefenderService.headOffice.debtRecoveryFlag", equalTo(true))
        .body("data.publicDefenderService.headOffice.intervened.intervenedFlag", equalTo(true))
        .body(
            "data.publicDefenderService.headOffice.intervened.intervenedChangeDate",
            equalTo("2025-02-01"))
        .body("data.publicDefenderService.headOffice.payment.paymentHeldFlag", equalTo(true))
        .body(
            "data.publicDefenderService.headOffice.payment.paymentHeldReason",
            equalTo("Annual review"));
  }

  @Test
  void dstew2026_ac1_retrievalDoesNotMutatePdsRecord() {
    String name = "E2E-DSTEW-2026 Read-only PDS " + System.currentTimeMillis();

    String firmNumber =
        given()
            .contentType(ContentType.JSON)
            .body(configuredRequest(name))
            .when()
            .post("/provider-firms/public-defender-services")
            .then()
            .statusCode(201)
            .extract()
            .path("data.providerFirmNumber");

    Response firstResponse =
        given()
            .pathParam("firmId", firmNumber)
            .when()
            .get("/provider-firms/{firmId}")
            .then()
            .statusCode(200)
            .extract()
            .response();

    given()
        .pathParam("firmId", firmNumber)
        .when()
        .get("/provider-firms/{firmId}/offices")
        .then()
        .statusCode(200)
        .body("data.content", hasSize(1));

    Response secondResponse =
        given()
            .pathParam("firmId", firmNumber)
            .when()
            .get("/provider-firms/{firmId}")
            .then()
            .statusCode(200)
            .extract()
            .response();

    assertTrue(Objects.equals(firstResponse.path("data.guid"), secondResponse.path("data.guid")));
    assertTrue(
        Objects.equals(
            firstResponse.path("data.firmNumber"), secondResponse.path("data.firmNumber")));
    assertTrue(
        Objects.equals(firstResponse.path("data.version"), secondResponse.path("data.version")));
    assertTrue(
        Objects.equals(
            firstResponse.path("data.publicDefenderService.headOffice.officeGUID"),
            secondResponse.path("data.publicDefenderService.headOffice.officeGUID")));
    assertTrue(
        Objects.equals(
            firstResponse.path("data.publicDefenderService.headOffice.accountNumber"),
            secondResponse.path("data.publicDefenderService.headOffice.accountNumber")));
    assertTrue(Objects.equals(firstResponse.path("data.name"), secondResponse.path("data.name")));
  }

  @Test
  void dstew2026_ac1_retrievalReflectsSubsequentProviderAmendment() {
    String originalName = "E2E-DSTEW-2026 Original PDS " + System.currentTimeMillis();
    String amendedName = originalName + " Amended";

    String firmNumber =
        given()
            .contentType(ContentType.JSON)
            .body(validRequest(originalName))
            .when()
            .post("/provider-firms/public-defender-services")
            .then()
            .statusCode(201)
            .extract()
            .path("data.providerFirmNumber");

    given()
        .contentType(ContentType.JSON)
        .pathParam("firmId", firmNumber)
        .body(Map.of("name", amendedName))
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
        .body("data.firmNumber", equalTo(firmNumber))
        .body("data.name", equalTo(amendedName))
        .body("data.firmType", equalTo("Public Defender Service"));
  }

  @Test
  void dstew2024_ac2_missingMandatoryField_returns400() {
    given()
        .contentType(ContentType.JSON)
        .body(
            Map.of(
                "name",
                "E2E-DSTEW-2024 Missing Status " + System.currentTimeMillis(),
                "headOffice",
                Map.of(
                    "address",
                    Map.of(
                        "line1", "1 E2E Street",
                        "townOrCity", "Birmingham",
                        "postcode", "B1 1AA"))))
        .when()
        .post("/provider-firms/public-defender-services")
        .then()
        .statusCode(400)
        .body("error.errorCode", notNullValue());
  }

  @Test
  void dstew2024_ac2_invalidAddressField_returns400() {
    given()
        .contentType(ContentType.JSON)
        .body(
            Map.of(
                "name",
                "E2E-DSTEW-2024 Invalid Address " + System.currentTimeMillis(),
                "constitutionalStatus",
                "Government Funded Organisation",
                "headOffice",
                Map.of("address", Map.of("line1", "1 E2E Street", "townOrCity", "Birmingham"))))
        .when()
        .post("/provider-firms/public-defender-services")
        .then()
        .statusCode(400)
        .body("error.errorCode", notNullValue());
  }

  @Test
  void dstew2024_ac3_unpairedDxFields_returns400() {
    given()
        .contentType(ContentType.JSON)
        .body(
            Map.of(
                "name",
                "E2E-DSTEW-2024 Invalid DX " + System.currentTimeMillis(),
                "constitutionalStatus",
                "Government Funded Organisation",
                "headOffice",
                Map.of(
                    "address",
                    Map.of(
                        "line1", "1 E2E Street",
                        "townOrCity", "Birmingham",
                        "postcode", "B1 1AA"),
                    "dxDetails",
                    Map.of("dxNumber", "12345"))))
        .when()
        .post("/provider-firms/public-defender-services")
        .then()
        .statusCode(400)
        .body("error.errorCode", notNullValue());
  }

  @Test
  void dstew2024_ac3_dxCentreWithoutDxNumber_returns400() {
    given()
        .contentType(ContentType.JSON)
        .body(
            Map.of(
                "name",
                "E2E-DSTEW-2024 Invalid DX Centre " + System.currentTimeMillis(),
                "constitutionalStatus",
                "Government Funded Organisation",
                "headOffice",
                Map.of(
                    "address",
                    Map.of(
                        "line1", "1 E2E Street",
                        "townOrCity", "Birmingham",
                        "postcode", "B1 1AA"),
                    "dxDetails",
                    Map.of("dxCentre", "BIRMINGHAM"))))
        .when()
        .post("/provider-firms/public-defender-services")
        .then()
        .statusCode(400)
        .body("error.errorCode", notNullValue());
  }

  @Test
  void dstew2024_ac4_invalidRequest_returns400WithoutPartialResponse() {
    String name = "E2E-DSTEW-2024 Invalid " + System.currentTimeMillis();
    given()
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
                        "line1", "1 E2E Street",
                        "townOrCity", "Birmingham",
                        "postcode", "B1 1AA"),
                    "dxDetails",
                    Map.of("dxCentre", "INVALID"))))
        .when()
        .post("/provider-firms/public-defender-services")
        .then()
        .statusCode(400)
        .body("data", equalTo(null))
        .body("error.errorCode", notNullValue());

    given()
        .queryParam("name", name)
        .when()
        .get("/provider-firms")
        .then()
        .statusCode(200)
        .body("data.content", empty());
  }

  private static Map<String, Object> validRequest(String name) {
    return Map.of(
        "name",
        name,
        "constitutionalStatus",
        "Government Funded Organisation",
        "headOffice",
        Map.of(
            "address",
            Map.of("line1", "1 E2E Street", "townOrCity", "Birmingham", "postcode", "B1 1AA")));
  }

  private static Map<String, Object> configuredRequest(String name) {
    return Map.of(
        "name",
        name,
        "constitutionalStatus",
        "Government Funded Organisation",
        "indemnityReceivedDate",
        "2025-01-15",
        "companiesHouseNumber",
        "12345678",
        "headOffice",
        Map.of(
            "vatRegistration",
            Map.of("vatNumber", "GB123456789"),
            "address",
            Map.of(
                "line1", "1 PDS Street",
                "line2", "Floor 2",
                "line3", "Building A",
                "line4", "Civic Quarter",
                "townOrCity", "Birmingham",
                "county", "West Midlands",
                "postcode", "B1 1AA"),
            "telephoneNumber",
            "0121 555 0101",
            "emailAddress",
            "pds@example.com",
            "website",
            "https://pds.example",
            "dxDetails",
            Map.of("dxNumber", "DX 12345", "dxCentre", "Birmingham")));
  }
}
