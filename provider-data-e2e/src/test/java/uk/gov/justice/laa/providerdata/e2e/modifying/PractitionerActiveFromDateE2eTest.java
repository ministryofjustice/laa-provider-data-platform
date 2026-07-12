package uk.gov.justice.laa.providerdata.e2e.modifying;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import io.restassured.http.ContentType;
import io.restassured.response.Response;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import uk.gov.justice.laa.providerdata.e2e.ModifyingTest;

/**
 * E2E tests for DSTEW-1742 (DS_MAPD_FR_030): practitioner Active From Date.
 *
 * <p>For this ticket, "Active From Date" is treated as the practitioner provider's {@code
 * createdTimestamp} date.
 *
 * <p>AC5 ("cannot set date") is not directly API-observable without fault injection into internal
 * persistence. The closest observable proxy used here is failed create validation with "no
 * practitioner record created".
 */
@ModifyingTest
@DisplayName("DSTEW-1742: Practitioner Active From Date")
class PractitionerActiveFromDateE2eTest {

  private String chambersFirmNumber;

  @BeforeEach
  void createChambersFixture() {
    long ts = System.currentTimeMillis();
    Response chambersCreateResponse =
        given()
            .contentType(ContentType.JSON)
            .body(
                Map.of(
                    "firmType",
                    "Chambers",
                    "name",
                    "E2E-DSTEW-1742 Chambers " + ts,
                    "chambers",
                    Map.of(
                        "address",
                        Map.of(
                            "line1",
                            "1 Chambers Street",
                            "townOrCity",
                            "London",
                            "postcode",
                            "WC1A 1AA"),
                        "liaisonManager",
                        Map.of(
                            "firstName", "Chambers",
                            "lastName", "Manager",
                            "emailAddress", "chambers.lm." + ts + "@example.com",
                            "telephoneNumber", "020 7000 0000"))))
            .when()
            .post("/provider-firms")
            .then()
            .statusCode(201)
            .extract()
            .response();
    chambersFirmNumber = chambersCreateResponse.path("data.providerFirmNumber");
  }

  @Test
  void dstew1742_ac1_createPractitioner_setsActiveFromDateAndStoresCompleteRecord() {
    long ts = System.currentTimeMillis();
    Response createResponse = createPractitioner("E2E-DSTEW-1742 AC1 " + ts, null, "SRA-AC1-" + ts);

    String practitionerFirmNumber = createResponse.path("data.providerFirmNumber");
    Response readResponse =
        given()
            .pathParam("firmId", practitionerFirmNumber)
            .when()
            .get("/provider-firms/{firmId}")
            .then()
            .statusCode(200)
            .body("data.createdTimestamp", notNullValue())
            .body("data.practitioner.parentFirms.size()", equalTo(1))
            .body("data.practitioner.office.accountNumber", notNullValue())
            .body("data.practitioner.office.activeDateTo", nullValue())
            .extract()
            .response();

    String createdTimestamp = readResponse.path("data.createdTimestamp");
    assertEquals(LocalDate.now(), LocalDate.parse(createdTimestamp.substring(0, 10)));
  }

  @Test
  void dstew1742_ac2_createPractitioner_withExternalCreatedTimestamp_returns400AndNotCreated() {
    long ts = System.currentTimeMillis();
    String practitionerName = "E2E-DSTEW-1742 AC2 " + ts;

    Map<String, Object> payload = basePractitionerCreatePayload(practitionerName, "SRA-AC2-" + ts);
    payload.put("createdTimestamp", "2001-01-01T00:00:00Z");

    given()
        .contentType(ContentType.JSON)
        .body(payload)
        .when()
        .post("/provider-firms")
        .then()
        .statusCode(400);

    assertFirmNotCreated(practitionerName);
  }

  @Test
  void dstew1742_ac3_patchPractitioner_attemptCreatedTimestampUpdate_returns400AndUnchanged() {
    long ts = System.currentTimeMillis();
    String practitionerName = "E2E-DSTEW-1742 AC3 " + ts;
    Response createResponse = createPractitioner(practitionerName, null, "SRA-AC3-" + ts);
    String practitionerFirmNumber = createResponse.path("data.providerFirmNumber");
    String beforeCreatedTimestamp = getProviderCreatedTimestamp(practitionerFirmNumber);

    given()
        .pathParam("firmId", practitionerFirmNumber)
        .contentType(ContentType.JSON)
        .body(
            Map.of(
                "createdTimestamp",
                "2000-01-01T00:00:00Z",
                "practitioner",
                Map.of("advocateLevel", "Junior")))
        .when()
        .patch("/provider-firms/{firmId}")
        .then()
        .statusCode(400)
        .body("error.errorCode", notNullValue());

    assertEquals(beforeCreatedTimestamp, getProviderCreatedTimestamp(practitionerFirmNumber));
  }

  @Test
  void dstew1742_ac4_patchPractitioner_validAmendment_doesNotResetActiveFromDate() {
    long ts = System.currentTimeMillis();
    Response createResponse = createPractitioner("E2E-DSTEW-1742 AC4 " + ts, null, "SRA-AC4-" + ts);
    String practitionerFirmNumber = createResponse.path("data.providerFirmNumber");
    String beforeCreatedTimestamp = getProviderCreatedTimestamp(practitionerFirmNumber);

    given()
        .pathParam("firmId", practitionerFirmNumber)
        .contentType(ContentType.JSON)
        .body(Map.of("name", "E2E-DSTEW-1742 AC4 AMENDED " + ts))
        .when()
        .patch("/provider-firms/{firmId}")
        .then()
        .statusCode(200);

    assertEquals(beforeCreatedTimestamp, getProviderCreatedTimestamp(practitionerFirmNumber));
  }

  @Test
  void dstew1742_ac5_noPartialRecord_whenCreateRejectedForCreatedTimestampInjection() {
    long ts = System.currentTimeMillis();
    String practitionerName = "E2E-DSTEW-1742 AC5 " + ts;

    Map<String, Object> payload = basePractitionerCreatePayload(practitionerName, "SRA-AC5-" + ts);
    payload.put("createdTimestamp", "2099-01-01T00:00:00Z");

    given()
        .contentType(ContentType.JSON)
        .body(payload)
        .when()
        .post("/provider-firms")
        .then()
        .statusCode(400)
        .body("error.errorCode", notNullValue());

    assertFirmNotCreated(practitionerName);
  }

  private Response createPractitioner(
      String practitionerName, String createdTimestamp, String sraNumber) {
    Map<String, Object> payload = basePractitionerCreatePayload(practitionerName, sraNumber);
    if (createdTimestamp != null) {
      payload.put("createdTimestamp", createdTimestamp);
    }
    return given()
        .contentType(ContentType.JSON)
        .body(payload)
        .when()
        .post("/provider-firms")
        .then()
        .statusCode(201)
        .body("data.providerFirmGUID", notNullValue())
        .body("data.providerFirmNumber", notNullValue())
        .extract()
        .response();
  }

  private Map<String, Object> basePractitionerCreatePayload(
      String practitionerName, String sraNumber) {
    return Map.of(
        "firmType",
        "Advocate",
        "name",
        practitionerName,
        "practitioner",
        Map.of(
            "parentFirms",
            List.of(Map.of("parentFirmNumber", chambersFirmNumber)),
            "advocateType",
            "Advocate",
            "advocate",
            Map.of("advocateLevel", "Junior", "solicitorRegulationAuthorityRollNumber", sraNumber),
            "liaisonManager",
            Map.of(
                "firstName", "Practitioner",
                "lastName", "Manager",
                "emailAddress", "practitioner.lm." + System.currentTimeMillis() + "@example.com",
                "telephoneNumber", "020 7111 1111"),
            "payment",
            Map.of("paymentMethod", "CHECK")));
  }

  private String getProviderCreatedTimestamp(String practitionerFirmNumber) {
    String createdTimestamp =
        given()
            .pathParam("firmId", practitionerFirmNumber)
            .when()
            .get("/provider-firms/{firmId}")
            .then()
            .statusCode(200)
            .extract()
            .path("data.createdTimestamp");
    assertEquals(LocalDate.now(), LocalDate.parse(createdTimestamp.substring(0, 10)));
    return createdTimestamp;
  }

  private void assertFirmNotCreated(String practitionerName) {
    given()
        .queryParam("name", practitionerName)
        .when()
        .get("/provider-firms")
        .then()
        .statusCode(200)
        .body("data.metadata.pagination.totalItems", equalTo(0));
  }
}
