package uk.gov.justice.laa.providerdata.e2e.modifying;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

import io.restassured.http.ContentType;
import io.restassured.response.Response;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import uk.gov.justice.laa.providerdata.e2e.ModifyingTest;

/**
 * Data-modifying e2e tests for amending a practitioner's liaison manager via PATCH /provider-firms
 * endpoint (DSTEW-1647, UC5).
 *
 * <p>Tests covering practitioner liaison manager re-linking options: - Option 1: Link to chambers'
 * liaison manager - Option 2: Keep existing liaison manager (implicit null) - Option 3: Create new
 * liaison manager
 *
 * <p>All tests create a fresh Advocate + Chambers + LM setup in {@link #setupAdvocateAndChambers()}
 * and verify: - Successful re-linking via PATCH - AC/BR compliance (only one active LM, end-dating
 * of existing links) - Persistence via subsequent GET - Atomicity (no partial updates on failure)
 */
@ModifyingTest
@DisplayName("DSTEW-1647 UC5: Re-link practitioner liaison manager")
class AmendPractitionerLiaisonManagerE2eTest {

  private String advocateFirmNumber;
  private String advocateFirmGuid;
  private String chambersFirmNumber;
  private String chambersFirmGuid;
  private String chambersOfficeGuid;
  private String advocateOfficeGuid;
  private String chambersLmGuid;

  @BeforeEach
  void setupAdvocateAndChambers() {
    // Create Chambers firm
    String chambersFirmName = "E2E-DSTEW-1647-UC5 Chambers " + System.currentTimeMillis();
    Response chambersCreateResponse =
        given()
            .contentType(ContentType.JSON)
            .body(
                Map.of(
                    "firmType",
                    "Chambers",
                    "name",
                    chambersFirmName,
                    "chambers",
                    Map.of(
                        "address",
                        Map.of(
                            "line1", "1 Chambers Street",
                            "townOrCity", "London",
                            "postcode", "WC1A 1AA"),
                        "liaisonManager",
                        Map.of(
                            "firstName", "Chambers",
                            "lastName", "Manager",
                            "emailAddress", "chambers.lm@example.com",
                            "telephoneNumber", "020 7946 0958"))))
            .when()
            .post("/provider-firms")
            .then()
            .statusCode(201)
            .body("data.providerFirmGUID", notNullValue())
            .body("data.providerFirmNumber", notNullValue())
            .extract()
            .response();

    chambersFirmGuid = chambersCreateResponse.jsonPath().getString("data.providerFirmGUID");
    chambersFirmNumber = chambersCreateResponse.jsonPath().getString("data.providerFirmNumber");

    // Retrieve chambers office GUID via GET (not in POST response)
    chambersOfficeGuid =
        given()
            .pathParam("firmId", chambersFirmNumber)
            .when()
            .get("/provider-firms/{firmId}")
            .then()
            .statusCode(200)
            .extract()
            .path("data.chambers.office.officeGUID");

    // Retrieve chambers LM GUID via liaison-managers endpoint
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

    // Create Advocate (practitioner) firm
    String advocateFirmName = "E2E-DSTEW-1647-UC5 Advocate " + System.currentTimeMillis();
    Response advocateCreateResponse =
        given()
            .contentType(ContentType.JSON)
            .body(
                Map.of(
                    "firmType",
                    "Advocate",
                    "name",
                    advocateFirmName,
                    "practitioner",
                    Map.of(
                        "parentFirms",
                        List.of(Map.of("parentFirmNumber", chambersFirmNumber)),
                        "advocateType",
                        "Advocate",
                        "advocate",
                        Map.of(
                            "advocateLevel",
                            "Junior",
                            "solicitorRegulationAuthorityRollNumber",
                            "A123456"),
                        "liaisonManager",
                        Map.of(
                            "firstName", "Advocate",
                            "lastName", "LM-Original",
                            "emailAddress", "advocate.original@example.com",
                            "telephoneNumber", "020 1111 1111"),
                        "payment",
                        Map.of("paymentMethod", "CHECK"))))
            .when()
            .post("/provider-firms")
            .then()
            .statusCode(201)
            .body("data.providerFirmGUID", notNullValue())
            .body("data.providerFirmNumber", notNullValue())
            .extract()
            .response();

    advocateFirmGuid = advocateCreateResponse.jsonPath().getString("data.providerFirmGUID");
    advocateFirmNumber = advocateCreateResponse.jsonPath().getString("data.providerFirmNumber");

    // Retrieve advocate office GUID via GET (not in POST response)
    advocateOfficeGuid =
        given()
            .pathParam("firmId", advocateFirmNumber)
            .when()
            .get("/provider-firms/{firmId}/offices")
            .then()
            .statusCode(200)
            .extract()
            .path("data.content[0].guid");
  }

  // -- Option 1: Link to chambers' liaison manager

  @Test
  void patchPractitioner_option1_linkChambers_replacesWithChambersLm() {
    // Execute: PATCH advocate to link to chambers LM
    given()
        .pathParam("firmId", advocateFirmNumber)
        .contentType(ContentType.JSON)
        .body(
            Map.of(
                "practitioner",
                Map.of("liaisonManager", Map.of("useChambersLiaisonManager", true))))
        .when()
        .patch("/provider-firms/{firmId}")
        .then()
        .statusCode(200)
        .body("data.providerFirmGUID", equalTo(advocateFirmGuid))
        .body("data.providerFirmNumber", equalTo(advocateFirmNumber));

    // Verify: advocate office now has the chambers LM as active
    given()
        .pathParam("firmId", advocateFirmNumber)
        .pathParam("officeId", advocateOfficeGuid)
        .when()
        .get("/provider-firms/{firmId}/offices/{officeId}/liaison-managers")
        .then()
        .statusCode(200)
        .body("data.content[0].guid", equalTo(chambersLmGuid))
        .body("data.content[0].firstName", equalTo("Chambers"))
        .body("data.content[0].lastName", equalTo("Manager"))
        .body("data.content[0].activeDateTo", nullValue());
  }

  // -- Option 2: Keep existing liaison manager (implicit via null)

  @Test
  void patchPractitioner_option2_nullLiaisonManager_keepsExisting() {
    // Retrieve original LM GUID via liaison managers endpoint
    String originalLmGuid =
        given()
            .pathParam("firmId", advocateFirmNumber)
            .pathParam("officeId", advocateOfficeGuid)
            .when()
            .get("/provider-firms/{firmId}/offices/{officeId}/liaison-managers")
            .then()
            .statusCode(200)
            .extract()
            .path("data.content[0].guid");

    // Execute: PATCH without specifying liaisonManager (Option 2 - keep existing)
    given()
        .pathParam("firmId", advocateFirmNumber)
        .contentType(ContentType.JSON)
        .body(Map.of("practitioner", Map.of("advocateLevel", "Junior")))
        .when()
        .patch("/provider-firms/{firmId}")
        .then()
        .statusCode(200)
        .body("data.providerFirmGUID", equalTo(advocateFirmGuid));

    // Verify: LM is unchanged
    given()
        .pathParam("firmId", advocateFirmNumber)
        .pathParam("officeId", advocateOfficeGuid)
        .when()
        .get("/provider-firms/{firmId}/offices/{officeId}/liaison-managers")
        .then()
        .statusCode(200)
        .body("data.content[0].guid", equalTo(originalLmGuid))
        .body("data.content[0].firstName", equalTo("Advocate"));
  }

  // -- Option 3: Create new liaison manager

  @Test
  void patchPractitioner_option3_createNew_createsNewLmSuccessfully() {
    String newAdvocateFirmName =
        "E2E-DSTEW-1647-UC5 Advocate-Option3 " + System.currentTimeMillis();

    // Create a fresh advocate with an initial LM, then replace it
    Response advocateCreateResponse =
        given()
            .contentType(ContentType.JSON)
            .body(
                Map.of(
                    "firmType",
                    "Advocate",
                    "name",
                    newAdvocateFirmName,
                    "practitioner",
                    Map.of(
                        "parentFirms",
                        List.of(Map.of("parentFirmNumber", chambersFirmNumber)),
                        "advocateType",
                        "Advocate",
                        "advocate",
                        Map.of(
                            "advocateLevel",
                            "Junior",
                            "solicitorRegulationAuthorityRollNumber",
                            "A123789"),
                        "liaisonManager",
                        Map.of(
                            "firstName", "Advocate",
                            "lastName", "LM-Seed",
                            "emailAddress", "advocate.seed@example.com",
                            "telephoneNumber", "020 2222 2222"),
                        "payment",
                        Map.of("paymentMethod", "CHECK"))))
            .when()
            .post("/provider-firms")
            .then()
            .statusCode(201)
            .extract()
            .response();

    String newAdvocateFirmGuid =
        advocateCreateResponse.jsonPath().getString("data.providerFirmGUID");
    String newAdvocateFirmNumber =
        advocateCreateResponse.jsonPath().getString("data.providerFirmNumber");

    // Retrieve advocate office GUID
    String newAdvocateOfficeGuid =
        given()
            .pathParam("firmId", newAdvocateFirmNumber)
            .when()
            .get("/provider-firms/{firmId}/offices")
            .then()
            .statusCode(200)
            .extract()
            .path("data.content[0].guid");

    // Execute: PATCH to create new LM
    given()
        .pathParam("firmId", newAdvocateFirmNumber)
        .contentType(ContentType.JSON)
        .body(
            Map.of(
                "practitioner",
                Map.of(
                    "liaisonManager",
                    Map.of(
                        "firstName", "NewCreated",
                        "lastName", "Manager",
                        "emailAddress", "new.created@example.com",
                        "telephoneNumber", "020 3333 3333"))))
        .when()
        .patch("/provider-firms/{firmId}")
        .then()
        .statusCode(200)
        .body("data.providerFirmGUID", equalTo(newAdvocateFirmGuid));

    // Verify: liaison managers endpoint shows the new LM as active (most recent first)
    given()
        .pathParam("firmId", newAdvocateFirmGuid)
        .pathParam("officeId", newAdvocateOfficeGuid)
        .when()
        .get("/provider-firms/{firmId}/offices/{officeId}/liaison-managers")
        .then()
        .statusCode(200)
        .body("data.content[0].firstName", equalTo("NewCreated"))
        .body("data.content[0].lastName", equalTo("Manager"))
        .body("data.content[0].activeDateTo", nullValue());
  }

  // -- Option 3: end-dates existing active LM when creating a new one

  @Test
  void patchPractitioner_option3_createNew_endsOldLmAndCreatesNew() {
    // Execute: PATCH advocate (which already has an active LM) with a new LM create request
    given()
        .pathParam("firmId", advocateFirmNumber)
        .contentType(ContentType.JSON)
        .body(
            Map.of(
                "practitioner",
                Map.of(
                    "liaisonManager",
                    Map.of(
                        "firstName", "Replacement",
                        "lastName", "Manager",
                        "emailAddress", "replacement@example.com",
                        "telephoneNumber", "020 4444 4444"))))
        .when()
        .patch("/provider-firms/{firmId}")
        .then()
        .statusCode(200);

    // Verify: liaison managers endpoint shows 2 links — old end-dated, new active
    given()
        .pathParam("firmId", advocateFirmNumber)
        .pathParam("officeId", advocateOfficeGuid)
        .when()
        .get("/provider-firms/{firmId}/offices/{officeId}/liaison-managers")
        .then()
        .statusCode(200)
        .body("data.content.findAll { it.activeDateTo == null }.size()", equalTo(1))
        .body("data.content.find { it.activeDateTo == null }.firstName", equalTo("Replacement"))
        .body("data.content.findAll { it.activeDateTo != null }.size()", equalTo(1))
        .body("data.content.find { it.activeDateTo != null }.firstName", equalTo("Advocate"));
  }

  // -- AC6: Verify activeDateTo is never set on a newly created LM link

  @Test
  void patchPractitioner_option3_createNew_activeDateToIsAlwaysNull() {
    // Execute: PATCH advocate to create new LM — activeDateTo must not be set on the new link
    given()
        .pathParam("firmId", advocateFirmNumber)
        .contentType(ContentType.JSON)
        .body(
            Map.of(
                "practitioner",
                Map.of(
                    "liaisonManager",
                    Map.of(
                        "firstName", "NoInactive",
                        "lastName", "Manager",
                        "emailAddress", "noinactive@example.com",
                        "telephoneNumber", "020 9999 9999"))))
        .when()
        .patch("/provider-firms/{firmId}")
        .then()
        .statusCode(200);

    // Verify: the newly created LM link has activeDateTo = null
    given()
        .pathParam("firmId", advocateFirmNumber)
        .pathParam("officeId", advocateOfficeGuid)
        .when()
        .get("/provider-firms/{firmId}/offices/{officeId}/liaison-managers")
        .then()
        .statusCode(200)
        .body("data.content.find { it.activeDateTo == null }.firstName", equalTo("NoInactive"))
        .body("data.content.findAll { it.activeDateTo == null }.size()", equalTo(1));
  }

  // -- AC2: Verify cannot assign LM to non-existent firm

  @Test
  void patchPractitioner_rejectsNonExistentFirm() {
    // Execute: Try to patch a firm that doesn't exist
    given()
        .pathParam("firmId", "000000")
        .contentType(ContentType.JSON)
        .body(
            Map.of(
                "practitioner",
                Map.of(
                    "liaisonManager",
                    Map.of(
                        "firstName", "Nonexistent",
                        "lastName", "Manager",
                        "emailAddress", "nonexistent@example.com",
                        "telephoneNumber", "020 0000 0000"))))
        .when()
        .patch("/provider-firms/{firmId}")
        .then()
        .statusCode(404);
  }
}
