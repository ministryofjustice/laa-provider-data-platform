package uk.gov.justice.laa.providerdata.e2e.modifying;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;

import io.restassured.http.ContentType;
import java.util.Map;
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
        .body("data.legalServicesProvider", equalTo(null))
        .body("data.chambers", equalTo(null))
        .body("data.practitioner", equalTo(null));

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
}
