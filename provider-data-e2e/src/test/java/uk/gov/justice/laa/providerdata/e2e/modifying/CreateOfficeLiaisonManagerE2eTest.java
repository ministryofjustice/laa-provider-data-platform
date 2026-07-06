package uk.gov.justice.laa.providerdata.e2e.modifying;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;

import io.restassured.http.ContentType;
import java.util.Map;
import org.junit.jupiter.api.Test;
import uk.gov.justice.laa.providerdata.e2e.E2eConfig;
import uk.gov.justice.laa.providerdata.e2e.ModifyingTest;

/**
 * Data-modifying e2e tests for {@code POST
 * /provider-firms/{firmId}/offices/{officeCode}/liaison-managers}.
 *
 * <p>Tests create new liaison manager records linked to the E2E office and verify via GET.
 */
@ModifyingTest
class CreateOfficeLiaisonManagerE2eTest {

  /**
   * DSTEW-1649 AC1 – Inactive Date is shown when it exists.
   *
   * <p>DS_MAPD_FR_014 (DSTEW-1649): When a previous Liaison Manager has been superseded by a new
   * assignment, its {@code activeDateTo} must be populated in the list response. The currently
   * active entry must have {@code activeDateTo} absent.
   */
  @Test
  void dstew1649_ac1_supersededLiaisonManager_hasActiveDateToPopulated() {
    long ts = System.currentTimeMillis();

    String firmNumber =
        given()
            .contentType(ContentType.JSON)
            .body(
                Map.of(
                    "firmType",
                    "Legal Services Provider",
                    "name",
                    "E2E-DSTEW-1649 " + ts,
                    "legalServicesProvider",
                    Map.of(
                        "constitutionalStatus",
                        "Partnership",
                        "address",
                        Map.of(
                            "line1", "1 History Street",
                            "townOrCity", "London",
                            "postcode", "EC1A 1BB"),
                        "payment",
                        Map.of("paymentMethod", "CHECK"),
                        "contractManager",
                        Map.of("useDefaultContractManager", true),
                        "liaisonManager",
                        Map.of(
                            "firstName", "First",
                            "lastName", "Manager",
                            "emailAddress", "first.lm." + ts + "@example.com",
                            "telephoneNumber", "020 1111 2222"))))
            .when()
            .post("/provider-firms")
            .then()
            .statusCode(201)
            .extract()
            .path("data.providerFirmNumber");

    String headOfficeCode =
        given()
            .pathParam("firmId", firmNumber)
            .when()
            .get("/provider-firms/{firmId}/offices")
            .then()
            .statusCode(200)
            .extract()
            .path("data.content[0].accountNumber");

    // Assigning a second LM end-dates the first.
    given()
        .contentType(ContentType.JSON)
        .pathParam("firmId", firmNumber)
        .pathParam("officeCode", headOfficeCode)
        .body(
            Map.of(
                "firstName", "Second",
                "lastName", "Manager",
                "emailAddress", "second.lm." + ts + "@example.com",
                "telephoneNumber", "020 3333 4444"))
        .when()
        .post("/provider-firms/{firmId}/offices/{officeCode}/liaison-managers")
        .then()
        .statusCode(201);

    given()
        .pathParam("firmId", firmNumber)
        .pathParam("officeCode", headOfficeCode)
        .when()
        .get("/provider-firms/{firmId}/offices/{officeCode}/liaison-managers")
        .then()
        .statusCode(200)
        .body("data.content", hasSize(2))
        // Tautological but explicit: DSTEW-1649 AC1 requires that the superseded entry carries
        // an Inactive Date. The find predicate alone is not a sufficient assertion — the field
        // must be present in the response object itself.
        .body("data.content.find { it.activeDateTo != null }.activeDateTo", notNullValue())
        .body("data.content.find { it.activeDateTo == null }.firstName", equalTo("Second"));
  }

  @Test
  void createLiaisonManager_forExistingOffice_returns201ThenGetReturnsCreatedManager() {
    String firstName = "New";
    String lastName = "Liaison";

    Map<String, Object> body =
        Map.of(
            "firstName",
            firstName,
            "lastName",
            lastName,
            "emailAddress",
            "new.liaison." + System.currentTimeMillis() + "@example.com",
            "telephoneNumber",
            "020 9999 8888");

    given()
        .contentType(ContentType.JSON)
        .pathParam("firmId", E2eConfig.lspFirmNumber())
        .pathParam("officeCode", E2eConfig.lspOfficeCode())
        .body(body)
        .when()
        .post("/provider-firms/{firmId}/offices/{officeCode}/liaison-managers")
        .then()
        .statusCode(201)
        .body("data.providerFirmGUID", notNullValue())
        .body("data.providerFirmNumber", equalTo(E2eConfig.lspFirmNumber()))
        .body("data.officeGUID", notNullValue())
        .body("data.officeCode", equalTo(E2eConfig.lspOfficeCode()))
        .body("data.liaisonManagerGUID", notNullValue());

    // Verify the created liaison manager appears in the GET response
    given()
        .pathParam("firmId", E2eConfig.lspFirmNumber())
        .pathParam("officeCode", E2eConfig.lspOfficeCode())
        .when()
        .get("/provider-firms/{firmId}/offices/{officeCode}/liaison-managers")
        .then()
        .statusCode(200)
        .body("data.content", hasSize(greaterThanOrEqualTo(1)))
        .body("data.content.firstName", hasItem(firstName))
        .body("data.content.lastName", hasItem(lastName));
  }

  @Test
  void createLiaisonManager_unknownFirm_returns404() {
    Map<String, Object> body =
        Map.of(
            "firstName", "Test",
            "lastName", "Person",
            "emailAddress", "test@example.com",
            "telephoneNumber", "020 0000 0000");

    given()
        .contentType(ContentType.JSON)
        .pathParam("firmId", E2eConfig.invalidFirmNumber())
        .pathParam("officeCode", E2eConfig.lspOfficeCode())
        .body(body)
        .when()
        .post("/provider-firms/{firmId}/offices/{officeCode}/liaison-managers")
        .then()
        .statusCode(404);
  }

  @Test
  void createLiaisonManager_unknownOffice_returns404() {
    Map<String, Object> body =
        Map.of(
            "firstName", "Test",
            "lastName", "Person",
            "emailAddress", "test@example.com",
            "telephoneNumber", "020 0000 0000");

    given()
        .contentType(ContentType.JSON)
        .pathParam("firmId", E2eConfig.lspFirmNumber())
        .pathParam("officeCode", E2eConfig.invalidOfficeCode())
        .body(body)
        .when()
        .post("/provider-firms/{firmId}/offices/{officeCode}/liaison-managers")
        .then()
        .statusCode(404);
  }

  @Test
  void createLiaisonManager_missingRequiredFields_returns400() {
    Map<String, Object> body = Map.of("firstName", "Incomplete");

    given()
        .contentType(ContentType.JSON)
        .pathParam("firmId", E2eConfig.lspFirmNumber())
        .pathParam("officeCode", E2eConfig.lspOfficeCode())
        .body(body)
        .when()
        .post("/provider-firms/{firmId}/offices/{officeCode}/liaison-managers")
        .then()
        .statusCode(400);
  }
}
