package uk.gov.justice.laa.providerdata.e2e.modifying;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

import io.restassured.http.ContentType;
import io.restassured.response.Response;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import uk.gov.justice.laa.providerdata.e2e.ModifyingTest;

@ModifyingTest
@DisplayName("DSTEW-1740: Create and amend practitioner liaison manager")
class PractitionerLiaisonManagerE2eTest {

  private String chambersFirmNumber;
  private String chambersFirmGuid;
  private String chambersOfficeGuid;
  private String chambersLmGuid;

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
                    "E2E-DSTEW-1740 Chambers " + ts,
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

    chambersFirmGuid = chambersCreateResponse.path("data.providerFirmGUID");
    chambersFirmNumber = chambersCreateResponse.path("data.providerFirmNumber");
    chambersOfficeGuid =
        given()
            .pathParam("firmId", chambersFirmNumber)
            .when()
            .get("/provider-firms/{firmId}")
            .then()
            .statusCode(200)
            .extract()
            .path("data.chambers.office.officeGUID");
    chambersLmGuid =
        given()
            .pathParam("firmId", chambersFirmGuid)
            .pathParam("officeId", chambersOfficeGuid)
            .when()
            .get("/provider-firms/{firmId}/offices/{officeId}/liaison-managers")
            .then()
            .statusCode(200)
            .extract()
            .path("data.content[0].guid");
  }

  @Test
  void
      dstew1740_ac1_createPractitioner_useChambersLiaisonManager_returns201AndLinksChambersManager() {
    long ts = System.currentTimeMillis();
    Response practitioner =
        createPractitioner("E2E-DSTEW-1740 AC1 " + ts, Map.of("useChambersLiaisonManager", true));
    String practitionerOfficeGuid = officeGuid(practitioner.path("data.providerFirmNumber"));

    given()
        .pathParam("firmId", practitioner.path("data.providerFirmGUID"))
        .pathParam("officeId", practitionerOfficeGuid)
        .when()
        .get("/provider-firms/{firmId}/offices/{officeId}/liaison-managers")
        .then()
        .statusCode(200)
        .body("data.content.findAll { it.activeDateTo == null }.size()", equalTo(1))
        .body("data.content[0].guid", equalTo(chambersLmGuid))
        .body("data.content[0].linkedFlag", equalTo(true))
        .body("data.content[0].activeDateTo", nullValue());
  }

  @Test
  void dstew1740_ac2_createPractitioner_createNewLiaisonManager_returns201AndStoresManager() {
    long ts = System.currentTimeMillis();
    Response practitioner =
        createPractitioner(
            "E2E-DSTEW-1740 AC2 " + ts,
            Map.of(
                "firstName", "Create",
                "lastName", "Manager",
                "emailAddress", "ac2." + ts + "@example.com",
                "telephoneNumber", "020 7100 0000"));
    String practitionerOfficeGuid = officeGuid(practitioner.path("data.providerFirmNumber"));

    given()
        .pathParam("firmId", practitioner.path("data.providerFirmGUID"))
        .pathParam("officeId", practitionerOfficeGuid)
        .when()
        .get("/provider-firms/{firmId}/offices/{officeId}/liaison-managers")
        .then()
        .statusCode(200)
        .body("data.content.findAll { it.activeDateTo == null }.size()", equalTo(1))
        .body("data.content[0].firstName", equalTo("Create"))
        .body("data.content[0].lastName", equalTo("Manager"))
        .body("data.content[0].linkedFlag", equalTo(false))
        .body("data.content[0].activeDateTo", nullValue());
  }

  @Test
  void dstew1740_ac3_amendPractitioner_assignChambersManager_replacesExistingManager() {
    long ts = System.currentTimeMillis();
    Response practitioner =
        createPractitioner(
            "E2E-DSTEW-1740 AC3 " + ts,
            Map.of(
                "firstName", "Initial",
                "lastName", "Manager",
                "emailAddress", "ac3.initial." + ts + "@example.com",
                "telephoneNumber", "020 7200 0000"));
    String practitionerNumber = practitioner.path("data.providerFirmNumber");
    String practitionerGuid = practitioner.path("data.providerFirmGUID");
    String practitionerOfficeGuid = officeGuid(practitionerNumber);

    String oldLmGuid = activeLmGuid(practitionerGuid, practitionerOfficeGuid);

    given()
        .pathParam("firmId", practitionerNumber)
        .contentType(ContentType.JSON)
        .body(
            Map.of(
                "practitioner",
                Map.of("liaisonManager", Map.of("useChambersLiaisonManager", true))))
        .when()
        .patch("/provider-firms/{firmId}")
        .then()
        .statusCode(200);

    given()
        .pathParam("firmId", practitionerGuid)
        .pathParam("officeId", practitionerOfficeGuid)
        .when()
        .get("/provider-firms/{firmId}/offices/{officeId}/liaison-managers")
        .then()
        .statusCode(200)
        .body("data.content.findAll { it.activeDateTo == null }.size()", equalTo(1))
        .body("data.content.find { it.activeDateTo == null }.guid", equalTo(chambersLmGuid))
        .body("data.content.find { it.guid == '" + oldLmGuid + "' }.activeDateTo", notNullValue());
  }

  @Test
  void dstew1740_ac4_amendPractitioner_withoutLiaisonManager_keepsExistingManager() {
    long ts = System.currentTimeMillis();
    Response practitioner =
        createPractitioner(
            "E2E-DSTEW-1740 AC4 " + ts,
            Map.of(
                "firstName", "Keep",
                "lastName", "Manager",
                "emailAddress", "ac4.keep." + ts + "@example.com",
                "telephoneNumber", "020 7300 0000"));
    String practitionerNumber = practitioner.path("data.providerFirmNumber");
    String practitionerGuid = practitioner.path("data.providerFirmGUID");
    String practitionerOfficeGuid = officeGuid(practitionerNumber);
    String beforeGuid = activeLmGuid(practitionerGuid, practitionerOfficeGuid);

    given()
        .pathParam("firmId", practitionerNumber)
        .contentType(ContentType.JSON)
        .body(Map.of("practitioner", Map.of("advocateLevel", "Junior")))
        .when()
        .patch("/provider-firms/{firmId}")
        .then()
        .statusCode(200);

    given()
        .pathParam("firmId", practitionerGuid)
        .pathParam("officeId", practitionerOfficeGuid)
        .when()
        .get("/provider-firms/{firmId}/offices/{officeId}/liaison-managers")
        .then()
        .statusCode(200)
        .body("data.content.findAll { it.activeDateTo == null }.size()", equalTo(1))
        .body("data.content.find { it.activeDateTo == null }.guid", equalTo(beforeGuid));
  }

  @Test
  void dstew1740_ac4_amendPractitioner_nullLiaisonManager_keepsExistingManager() {
    long ts = System.currentTimeMillis();
    Response practitioner =
        createPractitioner(
            "E2E-DSTEW-1740 AC4 NULL " + ts,
            Map.of(
                "firstName", "KeepNull",
                "lastName", "Manager",
                "emailAddress", "ac4.keep.null." + ts + "@example.com",
                "telephoneNumber", "020 7310 0000"));
    String practitionerNumber = practitioner.path("data.providerFirmNumber");
    String practitionerGuid = practitioner.path("data.providerFirmGUID");
    String practitionerOfficeGuid = officeGuid(practitionerNumber);
    String beforeGuid = activeLmGuid(practitionerGuid, practitionerOfficeGuid);

    Map<String, Object> practitionerPatch = new HashMap<>();
    practitionerPatch.put("advocateLevel", "Junior");
    practitionerPatch.put("liaisonManager", null);

    given()
        .pathParam("firmId", practitionerNumber)
        .contentType(ContentType.JSON)
        .body(Map.of("practitioner", practitionerPatch))
        .when()
        .patch("/provider-firms/{firmId}")
        .then()
        .statusCode(200);

    given()
        .pathParam("firmId", practitionerGuid)
        .pathParam("officeId", practitionerOfficeGuid)
        .when()
        .get("/provider-firms/{firmId}/offices/{officeId}/liaison-managers")
        .then()
        .statusCode(200)
        .body("data.content.findAll { it.activeDateTo == null }.size()", equalTo(1))
        .body("data.content.find { it.activeDateTo == null }.guid", equalTo(beforeGuid));
  }

  @Test
  void dstew1740_ac5_amendPractitioner_useChambersLiaisonManagerFalse_returns400AndUnchanged() {
    long ts = System.currentTimeMillis();
    Response practitioner =
        createPractitioner(
            "E2E-DSTEW-1740 AC5 " + ts,
            Map.of(
                "firstName", "Invalid",
                "lastName", "Manager",
                "emailAddress", "ac5.invalid." + ts + "@example.com",
                "telephoneNumber", "020 7400 0000"));
    String practitionerNumber = practitioner.path("data.providerFirmNumber");
    String practitionerGuid = practitioner.path("data.providerFirmGUID");
    String practitionerOfficeGuid = officeGuid(practitionerNumber);
    String beforeGuid = activeLmGuid(practitionerGuid, practitionerOfficeGuid);

    given()
        .pathParam("firmId", practitionerNumber)
        .contentType(ContentType.JSON)
        .body(
            Map.of(
                "practitioner",
                Map.of("liaisonManager", Map.of("useChambersLiaisonManager", false))))
        .when()
        .patch("/provider-firms/{firmId}")
        .then()
        .statusCode(400);

    given()
        .pathParam("firmId", practitionerGuid)
        .pathParam("officeId", practitionerOfficeGuid)
        .when()
        .get("/provider-firms/{firmId}/offices/{officeId}/liaison-managers")
        .then()
        .statusCode(200)
        .body("data.content.findAll { it.activeDateTo == null }.size()", equalTo(1))
        .body("data.content.find { it.activeDateTo == null }.guid", equalTo(beforeGuid));
  }

  @Test
  void dstew1740_ac5_amendPractitioner_emptyLiaisonManagerPayload_returns400AndUnchanged() {
    long ts = System.currentTimeMillis();
    Response practitioner =
        createPractitioner(
            "E2E-DSTEW-1740 AC5 EMPTY " + ts,
            Map.of(
                "firstName", "InvalidEmpty",
                "lastName", "Manager",
                "emailAddress", "ac5.invalid.empty." + ts + "@example.com",
                "telephoneNumber", "020 7410 0000"));
    String practitionerNumber = practitioner.path("data.providerFirmNumber");
    String practitionerGuid = practitioner.path("data.providerFirmGUID");
    String practitionerOfficeGuid = officeGuid(practitionerNumber);
    String beforeGuid = activeLmGuid(practitionerGuid, practitionerOfficeGuid);

    given()
        .pathParam("firmId", practitionerNumber)
        .contentType(ContentType.JSON)
        .body(Map.of("practitioner", Map.of("liaisonManager", Map.of())))
        .when()
        .patch("/provider-firms/{firmId}")
        .then()
        .statusCode(400);

    given()
        .pathParam("firmId", practitionerGuid)
        .pathParam("officeId", practitionerOfficeGuid)
        .when()
        .get("/provider-firms/{firmId}/offices/{officeId}/liaison-managers")
        .then()
        .statusCode(200)
        .body("data.content.findAll { it.activeDateTo == null }.size()", equalTo(1))
        .body("data.content.find { it.activeDateTo == null }.guid", equalTo(beforeGuid));
  }

  @Test
  void dstew1740_ac6_amendPractitioner_unknownLiaisonManagerGuid_returns404AndUnchanged() {
    long ts = System.currentTimeMillis();
    Response practitioner =
        createPractitioner(
            "E2E-DSTEW-1740 AC6 " + ts,
            Map.of(
                "firstName", "Guid",
                "lastName", "Manager",
                "emailAddress", "ac6.guid." + ts + "@example.com",
                "telephoneNumber", "020 7500 0000"));
    String practitionerNumber = practitioner.path("data.providerFirmNumber");
    String practitionerGuid = practitioner.path("data.providerFirmGUID");
    String practitionerOfficeGuid = officeGuid(practitionerNumber);
    String beforeGuid = activeLmGuid(practitionerGuid, practitionerOfficeGuid);

    given()
        .pathParam("firmId", practitionerNumber)
        .contentType(ContentType.JSON)
        .body(
            Map.of(
                "practitioner",
                Map.of(
                    "liaisonManager",
                    Map.of("liaisonManagerGUID", "00000000-0000-0000-0000-000000000000"))))
        .when()
        .patch("/provider-firms/{firmId}")
        .then()
        .statusCode(404);

    given()
        .pathParam("firmId", practitionerGuid)
        .pathParam("officeId", practitionerOfficeGuid)
        .when()
        .get("/provider-firms/{firmId}/offices/{officeId}/liaison-managers")
        .then()
        .statusCode(200)
        .body("data.content.findAll { it.activeDateTo == null }.size()", equalTo(1))
        .body("data.content.find { it.activeDateTo == null }.guid", equalTo(beforeGuid));
  }

  @Test
  void dstew1740_ac7_createPractitioner_missingLiaisonManager_returns400AndNotCreated() {
    long ts = System.currentTimeMillis();
    String firmName = "E2E-DSTEW-1740 AC7 " + ts;
    Map<String, Object> practitioner = basePractitionerRequest(ts);
    practitioner.remove("liaisonManager");

    given()
        .contentType(ContentType.JSON)
        .body(Map.of("firmType", "Advocate", "name", firmName, "practitioner", practitioner))
        .when()
        .post("/provider-firms")
        .then()
        .statusCode(400);

    assertFirmNotCreated(firmName);
  }

  @Test
  void dstew1740_ac8_createPractitioner_partialLiaisonManagerDetails_returns400AndNotCreated() {
    long ts = System.currentTimeMillis();
    String firmName = "E2E-DSTEW-1740 AC8 " + ts;
    Map<String, Object> practitioner = basePractitionerRequest(ts);
    practitioner.put(
        "liaisonManager",
        Map.of(
            "firstName", "Partial",
            "lastName", "Manager",
            "emailAddress", "ac8.partial." + ts + "@example.com"));

    given()
        .contentType(ContentType.JSON)
        .body(Map.of("firmType", "Advocate", "name", firmName, "practitioner", practitioner))
        .when()
        .post("/provider-firms")
        .then()
        .statusCode(400);

    assertFirmNotCreated(firmName);
  }

  private Response createPractitioner(String firmName, Map<String, Object> liaisonManager) {
    Map<String, Object> practitioner = basePractitionerRequest(System.currentTimeMillis());
    practitioner.put("liaisonManager", liaisonManager);
    return given()
        .contentType(ContentType.JSON)
        .body(Map.of("firmType", "Advocate", "name", firmName, "practitioner", practitioner))
        .when()
        .post("/provider-firms")
        .then()
        .statusCode(201)
        .body("data.providerFirmGUID", notNullValue())
        .body("data.providerFirmNumber", notNullValue())
        .extract()
        .response();
  }

  private Map<String, Object> basePractitionerRequest(long ts) {
    Map<String, Object> practitioner = new HashMap<>();
    practitioner.put("parentFirms", List.of(Map.of("parentFirmNumber", chambersFirmNumber)));
    practitioner.put("advocateType", "Advocate");
    practitioner.put(
        "advocate",
        Map.of("advocateLevel", "Junior", "solicitorRegulationAuthorityRollNumber", "SRA" + ts));
    practitioner.put("payment", Map.of("paymentMethod", "CHECK"));
    return practitioner;
  }

  private String officeGuid(String practitionerFirmNumber) {
    return given()
        .pathParam("firmId", practitionerFirmNumber)
        .when()
        .get("/provider-firms/{firmId}/offices")
        .then()
        .statusCode(200)
        .extract()
        .path("data.content[0].guid");
  }

  private String activeLmGuid(String practitionerGuid, String officeGuid) {
    return given()
        .pathParam("firmId", practitionerGuid)
        .pathParam("officeId", officeGuid)
        .when()
        .get("/provider-firms/{firmId}/offices/{officeId}/liaison-managers")
        .then()
        .statusCode(200)
        .extract()
        .path("data.content.find { it.activeDateTo == null }.guid");
  }

  private void assertFirmNotCreated(String firmName) {
    given()
        .queryParam("name", firmName)
        .when()
        .get("/provider-firms")
        .then()
        .statusCode(200)
        .body("data.metadata.pagination.totalItems", equalTo(0));
  }
}
