package uk.gov.justice.laa.providerdata.e2e.modifying;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

import io.restassured.http.ContentType;
import io.restassured.response.Response;
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
                        "office",
                        Map.of(
                            "address",
                            Map.of(
                                "line1", "1 Chambers Street",
                                "townOrCity", "London",
                                "postcode", "WC1A 1AA"),
                            "payment",
                            Map.of("paymentMethod", "CHECK"),
                            "liaisonManager",
                            Map.of(
                                "firstName", "Chambers",
                                "lastName", "Manager",
                                "emailAddress", "chambers.lm@example.com",
                                "telephoneNumber", "020 7946 0958")))))
            .when()
            .post("/provider-firms")
            .then()
            .statusCode(201)
            .body("data.providerFirmGUID", notNullValue())
            .body("data.providerFirmNumber", notNullValue())
            .body("data.officeGUID", notNullValue())
            .extract()
            .response();

    chambersFirmGuid = chambersCreateResponse.jsonPath().getString("data.providerFirmGUID");
    chambersFirmNumber = chambersCreateResponse.jsonPath().getString("data.providerFirmNumber");
    chambersOfficeGuid = chambersCreateResponse.jsonPath().getString("data.officeGUID");

    // Retrieve chambers to get the LM GUID
    Response chambersGetResponse =
        given()
            .pathParam("firmId", chambersFirmNumber)
            .when()
            .get("/provider-firms/{firmId}")
            .then()
            .statusCode(200)
            .extract()
            .response();

    chambersLmGuid =
        chambersGetResponse.jsonPath().getString("data.chambers.office.liaisonManager.guid");

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
                        "advocateLevel",
                        "QC",
                        "sraNumber",
                        "A123456",
                        "office",
                        Map.of(
                            "address",
                            Map.of(
                                "line1", "1 Advocate Street",
                                "townOrCity", "London",
                                "postcode", "SW1A 1AA"),
                            "payment",
                            Map.of("paymentMethod", "CHECK"),
                            "liaisonManager",
                            Map.of(
                                "firstName", "Advocate",
                                "lastName", "LM-Original",
                                "emailAddress", "advocate.original@example.com",
                                "telephoneNumber", "020 1111 1111")),
                        "parentFirm",
                        chambersFirmNumber)))
            .when()
            .post("/provider-firms")
            .then()
            .statusCode(201)
            .body("data.providerFirmGUID", notNullValue())
            .body("data.providerFirmNumber", notNullValue())
            .body("data.officeGUID", notNullValue())
            .extract()
            .response();

    advocateFirmGuid = advocateCreateResponse.jsonPath().getString("data.providerFirmGUID");
    advocateFirmNumber = advocateCreateResponse.jsonPath().getString("data.providerFirmNumber");
    advocateOfficeGuid = advocateCreateResponse.jsonPath().getString("data.officeGUID");
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

    // Verify: GET advocate shows it now has chambers' LM linked
    given()
        .pathParam("firmId", advocateFirmNumber)
        .when()
        .get("/provider-firms/{firmId}")
        .then()
        .statusCode(200)
        .body("data.practitioner.office.liaisonManager.guid", equalTo(chambersLmGuid))
        .body("data.practitioner.office.liaisonManager.firstName", equalTo("Chambers"))
        .body("data.practitioner.office.liaisonManager.lastName", equalTo("Manager"));
  }

  // -- Option 2: Keep existing liaison manager (implicit via null)

  @Test
  void patchPractitioner_option2_nullLiaisonManager_keepsExisting() {
    // First, retrieve original LM GUID before patch
    Response originalResponse =
        given()
            .pathParam("firmId", advocateFirmNumber)
            .when()
            .get("/provider-firms/{firmId}")
            .then()
            .statusCode(200)
            .extract()
            .response();

    String originalLmGuid =
        originalResponse.jsonPath().getString("data.practitioner.office.liaisonManager.guid");
    String originalLmFirstName =
        originalResponse.jsonPath().getString("data.practitioner.office.liaisonManager.firstName");

    // Execute: PATCH without specifying liaisonManager (Option 2 - keep existing)
    given()
        .pathParam("firmId", advocateFirmNumber)
        .contentType(ContentType.JSON)
        .body(Map.of("practitioner", Map.of("advocateLevel", "QC")))
        .when()
        .patch("/provider-firms/{firmId}")
        .then()
        .statusCode(200)
        .body("data.providerFirmGUID", equalTo(advocateFirmGuid));

    // Verify: GET shows LM is unchanged
    given()
        .pathParam("firmId", advocateFirmNumber)
        .when()
        .get("/provider-firms/{firmId}")
        .then()
        .statusCode(200)
        .body("data.practitioner.office.liaisonManager.guid", equalTo(originalLmGuid))
        .body("data.practitioner.office.liaisonManager.firstName", equalTo(originalLmFirstName));
  }

  // -- Option 3: Create new liaison manager

  @Test
  void patchPractitioner_option3_createNew_createsNewLmSuccessfully() {
    // Note: This test requires a fresh advocate setup (via BeforeAll creates multiple advocates
    // or this test uses the same one after Option 1 was applied).
    // For simplicity, we create a fresh advocate inline.

    String newAdvocateFirmName =
        "E2E-DSTEW-1647-UC5 Advocate-Option3 " + System.currentTimeMillis();
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
                        "advocateLevel",
                        "QC",
                        "sraNumber",
                        "A123789",
                        "office",
                        Map.of(
                            "address",
                            Map.of(
                                "line1", "1 Advocate Street",
                                "townOrCity", "London",
                                "postcode", "SW1A 1AA"),
                            "payment",
                            Map.of("paymentMethod", "CHECK"),
                            "liaisonManager",
                            Map.of(
                                "firstName", "Advocate",
                                "lastName", "LM-Original-Option3",
                                "emailAddress", "advocate.option3@example.com",
                                "telephoneNumber", "020 2222 2222")),
                        "parentFirm",
                        chambersFirmNumber)))
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

    // Verify: GET shows new LM with correct details
    given()
        .pathParam("firmId", newAdvocateFirmNumber)
        .when()
        .get("/provider-firms/{firmId}")
        .then()
        .statusCode(200)
        .body("data.practitioner.office.liaisonManager.firstName", equalTo("NewCreated"))
        .body("data.practitioner.office.liaisonManager.lastName", equalTo("Manager"))
        .body(
            "data.practitioner.office.liaisonManager.emailAddress",
            equalTo("new.created@example.com"))
        .body("data.practitioner.office.liaisonManager.telephoneNumber", equalTo("020 3333 3333"))
        .body("data.practitioner.office.liaisonManager.activeDateFrom", notNullValue());
  }

  // -- Option 3: Reject creation if active already exists (AC4 enforcement)

  @Test
  void patchPractitioner_option3_createNew_rejectsWhenActiveLmExists() {
    // Execute: Try to create new LM when one already exists - should be rejected
    given()
        .pathParam("firmId", advocateFirmNumber)
        .contentType(ContentType.JSON)
        .body(
            Map.of(
                "practitioner",
                Map.of(
                    "liaisonManager",
                    Map.of(
                        "firstName", "Attempted",
                        "lastName", "Creation",
                        "emailAddress", "attempted@example.com",
                        "telephoneNumber", "020 4444 4444"))))
        .when()
        .patch("/provider-firms/{firmId}")
        .then()
        .statusCode(400)
        .body("message", notNullValue());

    // Verify: GET still shows original LM (unchanged by failed patch)
    Response verifyResponse =
        given()
            .pathParam("firmId", advocateFirmNumber)
            .when()
            .get("/provider-firms/{firmId}")
            .then()
            .statusCode(200)
            .extract()
            .response();

    String lmFirstName =
        verifyResponse.jsonPath().getString("data.practitioner.office.liaisonManager.firstName");
    // Verify it's still the original "Advocate" LM (unchanged by failed patch)
    // Each test has fresh setup, so we know exactly which LM should be there
    assert (lmFirstName.equals("Advocate"));
  }

  // -- AC6: Verify cannot specify activeDateTo during creation

  @Test
  void patchPractitioner_option3_rejectsActiveDateToDuringCreation() {
    // Create a fresh advocate without existing LM
    String advocateFirmName = "E2E-AC6-Test " + System.currentTimeMillis();
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
                        "advocateLevel",
                        "QC",
                        "sraNumber",
                        "A999999",
                        "office",
                        Map.of(
                            "address",
                            Map.of(
                                "line1", "1 Test Street",
                                "townOrCity", "London",
                                "postcode", "SW1A 1AA"),
                            "payment",
                            Map.of("paymentMethod", "CHECK"),
                            "liaisonManager",
                            Map.of(
                                "firstName", "Test",
                                "lastName", "Manager",
                                "emailAddress", "test@example.com",
                                "telephoneNumber", "020 0000 0000")),
                        "parentFirm",
                        chambersFirmNumber)))
            .when()
            .post("/provider-firms")
            .then()
            .statusCode(201)
            .extract()
            .response();

    String testAdvocateFirmNumber =
        advocateCreateResponse.jsonPath().getString("data.providerFirmNumber");

    // Execute: Try to create new LM with activeDateTo specified - should be rejected
    given()
        .pathParam("firmId", testAdvocateFirmNumber)
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
                        "telephoneNumber", "020 9999 9999",
                        "activeDateTo", "2026-12-31"))))
        .when()
        .patch("/provider-firms/{firmId}")
        .then()
        .statusCode(400);
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
