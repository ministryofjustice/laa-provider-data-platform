package uk.gov.justice.laa.providerdata.e2e.modifying;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;

import io.restassured.http.ContentType;
import io.restassured.response.Response;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import uk.gov.justice.laa.providerdata.e2e.E2eConfig;
import uk.gov.justice.laa.providerdata.e2e.ModifyingTest;

/**
 * Data-modifying E2E tests for DS_MAPD_FR_024: Create Legal Practitioner (DSTEW-1734).
 *
 * <p>Covers:
 *
 * <ul>
 *   <li>AC1 – Successful practitioner creation with valid data (Advocate and Barrister variants)
 *   <li>AC2 – Rejection when the referenced parent Chamber does not exist
 *   <li>AC3 – Rejection when the referenced parent Chamber is inactive
 *   <li>AC4 – Rejection when mandatory fields are missing
 *   <li>AC5 – Rejection when more than one parent Chamber is provided
 *   <li>AC6 – No partial record created when the request fails inside the transaction
 * </ul>
 *
 * <p>Note: {@code firmType=Advocate} is the technical identifier for what the business calls a
 * Practitioner; the {@code advocateType} field further divides into {@code Advocate} or {@code
 * Barrister}.
 *
 * <p>Test data is prefixed with {@code "E2E-DSTEW-1734 "}; reset the local database to remove it.
 */
@ModifyingTest
@DisplayName("DSTEW-1734: Create Legal Practitioner (DS_MAPD_FR_024)")
class CreatePractitionerE2eTest {

  private static String inactiveChambersFirmNumber;

  /**
   * Creates an inactive Chambers fixture used by AC3 and AC6 tests.
   *
   * <p>The fixture Chambers is created fresh and immediately deactivated so that no seeded data is
   * relied upon for the inactive-state tests.
   */
  @BeforeAll
  static void createFixtures() {
    long ts = System.currentTimeMillis();

    // Create a Chambers and deactivate it — used for AC3 / AC6 tests.
    Response inactiveCreate =
        given()
            .contentType(ContentType.JSON)
            .body(
                Map.of(
                    "firmType",
                    "Chambers",
                    "name",
                    "E2E-DSTEW-1734 Inactive Chambers " + ts,
                    "chambers",
                    Map.of(
                        "address",
                        Map.of(
                            "line1", "1 Inactive Street",
                            "townOrCity", "London",
                            "postcode", "WC1A 1ZZ"),
                        "liaisonManager",
                        Map.of(
                            "firstName", "Inactive",
                            "lastName", "ChamberLM",
                            "emailAddress", "inactive.chamber.lm." + ts + "@example.com",
                            "telephoneNumber", "020 1111 9999"))))
            .when()
            .post("/provider-firms")
            .then()
            .statusCode(201)
            .extract()
            .response();

    inactiveChambersFirmNumber = inactiveCreate.path("data.providerFirmNumber");

    String inactiveOfficeCode =
        given()
            .pathParam("firmId", inactiveChambersFirmNumber)
            .when()
            .get("/provider-firms/{firmId}/offices")
            .then()
            .statusCode(200)
            .extract()
            .path("data.content[0].accountNumber");

    given()
        .contentType(ContentType.JSON)
        .pathParam("firmId", inactiveChambersFirmNumber)
        .pathParam("officeCode", inactiveOfficeCode)
        .body(Map.of("activeDateTo", LocalDate.now().toString()))
        .when()
        .patch("/provider-firms/{firmId}/offices/{officeCode}")
        .then()
        .statusCode(200);
  }

  /**
   * AC1 – Successful practitioner creation: Advocate variant.
   *
   * <p>DS_MAPD_FR_024 AC1: Given a valid and active Chamber exists in PDP, and all mandatory
   * practitioner data is provided, when a request is submitted to create a practitioner, then PDP
   * validates and persists the practitioner record successfully.
   *
   * <p>Verifies the full response state via a subsequent GET, including {@code parentFirms}, {@code
   * advocateType}, {@code advocate} details, and the inherited {@code office}.
   */
  @Test
  void dstew1734_ac1_createAdvocate_validData_returns201AndPersists() {
    long ts = System.currentTimeMillis();
    String sraNumber = "SRA" + ts;

    String firmNumber =
        given()
            .contentType(ContentType.JSON)
            .body(
                Map.of(
                    "firmType",
                    "Advocate",
                    "name",
                    "E2E-DSTEW-1734 Advocate " + ts,
                    "practitioner",
                    Map.of(
                        "parentFirms",
                        List.of(Map.of("parentFirmNumber", E2eConfig.chambersFirmNumber())),
                        "advocateType",
                        "Advocate",
                        "advocate",
                        Map.of(
                            "advocateLevel",
                            "Junior",
                            "solicitorRegulationAuthorityRollNumber",
                            sraNumber),
                        "payment",
                        Map.of("paymentMethod", "CHECK"),
                        "liaisonManager",
                        Map.of(
                            "firstName", "Advocate",
                            "lastName", "Liaison",
                            "emailAddress", "advocate.lm." + ts + "@example.com",
                            "telephoneNumber", "020 2222 1111"))))
            .when()
            .post("/provider-firms")
            .then()
            .statusCode(201)
            .body("data.providerFirmNumber", notNullValue())
            .extract()
            .path("data.providerFirmNumber");

    given()
        .pathParam("firmId", firmNumber)
        .when()
        .get("/provider-firms/{firmId}")
        .then()
        .statusCode(200)
        .body("data.firmType", equalTo("Advocate"))
        .body("data.practitioner.parentFirms", hasSize(1))
        .body(
            "data.practitioner.parentFirms[0].parentFirmNumber",
            equalTo(E2eConfig.chambersFirmNumber()))
        .body("data.practitioner.parentFirms[0].parentFirmType", equalTo("Chambers"))
        .body("data.practitioner.advocateType", equalTo("Advocate"))
        .body("data.practitioner.advocate.advocateLevel", equalTo("Junior"))
        .body(
            "data.practitioner.advocate.solicitorRegulationAuthorityRollNumber", equalTo(sraNumber))
        .body("data.practitioner.office.accountNumber", notNullValue());
  }

  /**
   * AC1 – Successful practitioner creation: Barrister variant.
   *
   * <p>DS_MAPD_FR_024 AC1: verifies that {@code advocateType=Barrister} with the required {@code
   * barrister} block is accepted and persisted, covering the second valid practitioner type.
   */
  @Test
  void dstew1734_ac1_createBarrister_validData_returns201AndPersists() {
    long ts = System.currentTimeMillis();
    String barCouncilNumber = "BAR" + ts;

    String firmNumber =
        given()
            .contentType(ContentType.JSON)
            .body(
                Map.of(
                    "firmType",
                    "Advocate",
                    "name",
                    "E2E-DSTEW-1734 Barrister " + ts,
                    "practitioner",
                    Map.of(
                        "parentFirms",
                        List.of(Map.of("parentFirmNumber", E2eConfig.chambersFirmNumber())),
                        "advocateType",
                        "Barrister",
                        "barrister",
                        Map.of("barristerLevel", "KC", "barCouncilRollNumber", barCouncilNumber),
                        "payment",
                        Map.of("paymentMethod", "CHECK"),
                        "liaisonManager",
                        Map.of(
                            "firstName", "Barrister",
                            "lastName", "Liaison",
                            "emailAddress", "barrister.lm." + ts + "@example.com",
                            "telephoneNumber", "020 2222 2222"))))
            .when()
            .post("/provider-firms")
            .then()
            .statusCode(201)
            .body("data.providerFirmNumber", notNullValue())
            .extract()
            .path("data.providerFirmNumber");

    given()
        .pathParam("firmId", firmNumber)
        .when()
        .get("/provider-firms/{firmId}")
        .then()
        .statusCode(200)
        .body("data.firmType", equalTo("Advocate"))
        .body("data.practitioner.parentFirms", hasSize(1))
        .body(
            "data.practitioner.parentFirms[0].parentFirmNumber",
            equalTo(E2eConfig.chambersFirmNumber()))
        .body("data.practitioner.parentFirms[0].parentFirmType", equalTo("Chambers"))
        .body("data.practitioner.advocateType", equalTo("Barrister"))
        .body("data.practitioner.barrister.barristerLevel", equalTo("KC"))
        .body("data.practitioner.barrister.barCouncilRollNumber", equalTo(barCouncilNumber))
        .body("data.practitioner.office.accountNumber", notNullValue());
  }

  /**
   * AC2 – Rejection when the parent Chamber does not exist.
   *
   * <p>DS_MAPD_FR_024 AC2: Given a practitioner creation request references a parent Chamber that
   * does not exist in PDP, when the request is submitted, then PDP rejects it with 404 and no
   * practitioner record is created.
   */
  @Test
  void dstew1734_ac2_createPractitioner_nonExistentChambers_returns404AndNotCreated() {
    long ts = System.currentTimeMillis();
    String firmName = "E2E-DSTEW-1734 AC2 " + ts;

    given()
        .contentType(ContentType.JSON)
        .body(
            Map.of(
                "firmType",
                "Advocate",
                "name",
                firmName,
                "practitioner",
                Map.of(
                    "parentFirms",
                    List.of(Map.of("parentFirmNumber", E2eConfig.invalidFirmNumber())),
                    "advocateType",
                    "Advocate",
                    "advocate",
                    Map.of(
                        "advocateLevel",
                        "Junior",
                        "solicitorRegulationAuthorityRollNumber",
                        "SRA" + ts),
                    "payment",
                    Map.of("paymentMethod", "CHECK"),
                    "liaisonManager",
                    Map.of(
                        "firstName", "AC2",
                        "lastName", "Liaison",
                        "emailAddress", "ac2.lm." + ts + "@example.com",
                        "telephoneNumber", "020 2222 3333"))))
        .when()
        .post("/provider-firms")
        .then()
        .statusCode(404);

    given()
        .queryParam("name", firmName)
        .when()
        .get("/provider-firms")
        .then()
        .statusCode(200)
        .body("data.metadata.pagination.totalItems", equalTo(0));
  }

  /**
   * AC3 – Rejection when the parent Chamber is inactive.
   *
   * <p>DS_MAPD_FR_024 AC3: Given a practitioner creation request references a parent Chamber whose
   * head office has an {@code activeDateTo} set, when the request is submitted, then PDP rejects it
   * with 400 and returns an error message. Enforces BR-27.
   *
   * <p>The inactive Chambers fixture is created and deactivated in {@link #createFixtures()}.
   */
  @Test
  void dstew1734_ac3_createPractitioner_inactiveChambers_returns400AndNotCreated() {
    long ts = System.currentTimeMillis();
    String firmName = "E2E-DSTEW-1734 AC3 " + ts;

    given()
        .contentType(ContentType.JSON)
        .body(
            Map.of(
                "firmType",
                "Advocate",
                "name",
                firmName,
                "practitioner",
                Map.of(
                    "parentFirms",
                    List.of(Map.of("parentFirmNumber", inactiveChambersFirmNumber)),
                    "advocateType",
                    "Advocate",
                    "advocate",
                    Map.of(
                        "advocateLevel",
                        "Junior",
                        "solicitorRegulationAuthorityRollNumber",
                        "SRA" + ts),
                    "payment",
                    Map.of("paymentMethod", "CHECK"),
                    "liaisonManager",
                    Map.of(
                        "firstName", "AC3",
                        "lastName", "Liaison",
                        "emailAddress", "ac3.lm." + ts + "@example.com",
                        "telephoneNumber", "020 2222 4444"))))
        .when()
        .post("/provider-firms")
        .then()
        .statusCode(400);

    given()
        .queryParam("name", firmName)
        .when()
        .get("/provider-firms")
        .then()
        .statusCode(200)
        .body("data.metadata.pagination.totalItems", equalTo(0));
  }

  /**
   * AC4 – Rejection when mandatory field {@code parentFirms} is missing.
   *
   * <p>DS_MAPD_FR_024 AC4: When {@code parentFirms} is omitted from the request, PDP rejects the
   * request with 400 and no practitioner record is created.
   */
  @Test
  void dstew1734_ac4_createPractitioner_missingParentFirms_returns400AndNotCreated() {
    long ts = System.currentTimeMillis();
    String firmName = "E2E-DSTEW-1734 AC4a " + ts;

    given()
        .contentType(ContentType.JSON)
        .body(
            Map.of(
                "firmType",
                "Advocate",
                "name",
                firmName,
                "practitioner",
                Map.of(
                    "advocateType",
                    "Advocate",
                    "advocate",
                    Map.of(
                        "advocateLevel",
                        "Junior",
                        "solicitorRegulationAuthorityRollNumber",
                        "SRA" + ts),
                    "payment",
                    Map.of("paymentMethod", "CHECK"),
                    "liaisonManager",
                    Map.of(
                        "firstName", "AC4a",
                        "lastName", "Liaison",
                        "emailAddress", "ac4a.lm." + ts + "@example.com",
                        "telephoneNumber", "020 2222 5555"))))
        .when()
        .post("/provider-firms")
        .then()
        .statusCode(400);

    given()
        .queryParam("name", firmName)
        .when()
        .get("/provider-firms")
        .then()
        .statusCode(200)
        .body("data.metadata.pagination.totalItems", equalTo(0));
  }

  /**
   * AC4 – Rejection when mandatory field {@code advocateType} is missing.
   *
   * <p>DS_MAPD_FR_024 AC4: When {@code advocateType} is omitted from the request, PDP rejects the
   * request with 400 and no practitioner record is created.
   */
  @Test
  void dstew1734_ac4_createPractitioner_missingAdvocateType_returns400AndNotCreated() {
    long ts = System.currentTimeMillis();
    String firmName = "E2E-DSTEW-1734 AC4b " + ts;

    given()
        .contentType(ContentType.JSON)
        .body(
            Map.of(
                "firmType",
                "Advocate",
                "name",
                firmName,
                "practitioner",
                Map.of(
                    "parentFirms",
                    List.of(Map.of("parentFirmNumber", E2eConfig.chambersFirmNumber())),
                    "advocate",
                    Map.of(
                        "advocateLevel",
                        "Junior",
                        "solicitorRegulationAuthorityRollNumber",
                        "SRA" + ts),
                    "payment",
                    Map.of("paymentMethod", "CHECK"),
                    "liaisonManager",
                    Map.of(
                        "firstName", "AC4b",
                        "lastName", "Liaison",
                        "emailAddress", "ac4b.lm." + ts + "@example.com",
                        "telephoneNumber", "020 2222 6666"))))
        .when()
        .post("/provider-firms")
        .then()
        .statusCode(400);

    given()
        .queryParam("name", firmName)
        .when()
        .get("/provider-firms")
        .then()
        .statusCode(200)
        .body("data.metadata.pagination.totalItems", equalTo(0));
  }

  /**
   * AC4 – Rejection when mandatory field {@code payment} is missing.
   *
   * <p>DS_MAPD_FR_024 AC4: When {@code payment} is omitted from the request, PDP rejects the
   * request with 400 and no practitioner record is created.
   */
  @Test
  void dstew1734_ac4_createPractitioner_missingPayment_returns400AndNotCreated() {
    long ts = System.currentTimeMillis();
    String firmName = "E2E-DSTEW-1734 AC4c " + ts;

    given()
        .contentType(ContentType.JSON)
        .body(
            Map.of(
                "firmType",
                "Advocate",
                "name",
                firmName,
                "practitioner",
                Map.of(
                    "parentFirms",
                    List.of(Map.of("parentFirmNumber", E2eConfig.chambersFirmNumber())),
                    "advocateType",
                    "Advocate",
                    "advocate",
                    Map.of(
                        "advocateLevel",
                        "Junior",
                        "solicitorRegulationAuthorityRollNumber",
                        "SRA" + ts),
                    "liaisonManager",
                    Map.of(
                        "firstName", "AC4c",
                        "lastName", "Liaison",
                        "emailAddress", "ac4c.lm." + ts + "@example.com",
                        "telephoneNumber", "020 2222 7777"))))
        .when()
        .post("/provider-firms")
        .then()
        .statusCode(400);

    given()
        .queryParam("name", firmName)
        .when()
        .get("/provider-firms")
        .then()
        .statusCode(200)
        .body("data.metadata.pagination.totalItems", equalTo(0));
  }

  /**
   * AC4 – Rejection when mandatory field {@code liaisonManager} is missing.
   *
   * <p>DS_MAPD_FR_024 AC4: When {@code liaisonManager} is omitted from the request, PDP rejects the
   * request with 400 and no practitioner record is created.
   */
  @Test
  void dstew1734_ac4_createPractitioner_missingLiaisonManager_returns400AndNotCreated() {
    long ts = System.currentTimeMillis();
    String firmName = "E2E-DSTEW-1734 AC4d " + ts;

    given()
        .contentType(ContentType.JSON)
        .body(
            Map.of(
                "firmType",
                "Advocate",
                "name",
                firmName,
                "practitioner",
                Map.of(
                    "parentFirms",
                    List.of(Map.of("parentFirmNumber", E2eConfig.chambersFirmNumber())),
                    "advocateType",
                    "Advocate",
                    "advocate",
                    Map.of(
                        "advocateLevel",
                        "Junior",
                        "solicitorRegulationAuthorityRollNumber",
                        "SRA" + ts),
                    "payment",
                    Map.of("paymentMethod", "CHECK"))))
        .when()
        .post("/provider-firms")
        .then()
        .statusCode(400);

    given()
        .queryParam("name", firmName)
        .when()
        .get("/provider-firms")
        .then()
        .statusCode(200)
        .body("data.metadata.pagination.totalItems", equalTo(0));
  }

  /**
   * AC5 – Rejection when more than one parent Chamber is provided.
   *
   * <p>DS_MAPD_FR_024 AC5 (BR-31): A practitioner can be linked to only one Chamber at a time. When
   * {@code parentFirms} contains more than one entry, PDP rejects the request with 400 and no
   * practitioner record is created.
   *
   * <p>Note: this constraint is enforced in {@code ProviderCreationService} and may be revisited if
   * the data model is extended to allow practitioners to move between Chambers; see DSTEW-1734 AC5.
   */
  @Test
  void dstew1734_ac5_createPractitioner_multipleParentFirms_returns400AndNotCreated() {
    long ts = System.currentTimeMillis();
    String firmName = "E2E-DSTEW-1734 AC5 " + ts;

    given()
        .contentType(ContentType.JSON)
        .body(
            Map.of(
                "firmType",
                "Advocate",
                "name",
                firmName,
                "practitioner",
                Map.of(
                    "parentFirms",
                    List.of(
                        Map.of("parentFirmNumber", E2eConfig.chambersFirmNumber()),
                        Map.of("parentFirmNumber", E2eConfig.chambersDxFirmNumber())),
                    "advocateType",
                    "Advocate",
                    "advocate",
                    Map.of(
                        "advocateLevel",
                        "Junior",
                        "solicitorRegulationAuthorityRollNumber",
                        "SRA" + ts),
                    "payment",
                    Map.of("paymentMethod", "CHECK"),
                    "liaisonManager",
                    Map.of(
                        "firstName", "AC5",
                        "lastName", "Liaison",
                        "emailAddress", "ac5.lm." + ts + "@example.com",
                        "telephoneNumber", "020 2222 8888"))))
        .when()
        .post("/provider-firms")
        .then()
        .statusCode(400);

    given()
        .queryParam("name", firmName)
        .when()
        .get("/provider-firms")
        .then()
        .statusCode(200)
        .body("data.metadata.pagination.totalItems", equalTo(0));
  }

  /**
   * AC6 – No partial record created when creation fails inside the transaction.
   *
   * <p>DS_MAPD_FR_024 AC6: When a creation request fails due to an inactive parent Chamber (BR-27),
   * the failure occurs after the practitioner entity has been initially persisted in memory but
   * before the transaction commits. The {@code @Transactional} boundary on {@code
   * ProviderCreationService.createPractitionerFirm} ensures the entire operation is rolled back.
   * The subsequent GET verifies that no partial practitioner record was persisted.
   */
  @Test
  void dstew1734_ac6_createPractitioner_failureInsideTransaction_noPartialRecordCreated() {
    long ts = System.currentTimeMillis();
    String firmName = "E2E-DSTEW-1734 AC6 " + ts;

    given()
        .contentType(ContentType.JSON)
        .body(
            Map.of(
                "firmType",
                "Advocate",
                "name",
                firmName,
                "practitioner",
                Map.of(
                    "parentFirms",
                    List.of(Map.of("parentFirmNumber", inactiveChambersFirmNumber)),
                    "advocateType",
                    "Advocate",
                    "advocate",
                    Map.of(
                        "advocateLevel",
                        "Junior",
                        "solicitorRegulationAuthorityRollNumber",
                        "SRA" + ts),
                    "payment",
                    Map.of("paymentMethod", "CHECK"),
                    "liaisonManager",
                    Map.of(
                        "firstName", "AC6",
                        "lastName", "Liaison",
                        "emailAddress", "ac6.lm." + ts + "@example.com",
                        "telephoneNumber", "020 2222 9999"))))
        .when()
        .post("/provider-firms")
        .then()
        .statusCode(400);

    given()
        .queryParam("name", firmName)
        .when()
        .get("/provider-firms")
        .then()
        .statusCode(200)
        .body("data.metadata.pagination.totalItems", equalTo(0));
  }
}
