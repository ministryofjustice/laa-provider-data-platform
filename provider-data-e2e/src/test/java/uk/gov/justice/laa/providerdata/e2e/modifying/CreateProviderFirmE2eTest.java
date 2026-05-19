package uk.gov.justice.laa.providerdata.e2e.modifying;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

import io.restassured.http.ContentType;
import io.restassured.response.Response;
import java.util.Map;
import org.junit.jupiter.api.Test;
import uk.gov.justice.laa.providerdata.e2e.ModifyingTest;

/**
 * Data-modifying e2e tests for {@code POST /provider-firms}.
 *
 * <p>Each test creates new data in the local database and verifies it via GET. Cleanup is handled
 * by {@code delete-test-data.sql} which removes providers with names starting with "E2E-DSTEW ".
 */
@ModifyingTest
class CreateProviderFirmE2eTest {

  // AC1 – Successful Legal Organisation creation
  @Test
  void createLspFirm_returns201ThenGetReturnsCreatedFirm() {
    String firmName = "E2E-DSTEW LSP " + System.currentTimeMillis();

    Map<String, Object> body =
        Map.of(
            "firmType",
            "Legal Services Provider",
            "name",
            firmName,
            "legalServicesProvider",
            Map.of(
                "constitutionalStatus",
                "Partnership",
                "address",
                Map.of(
                    "line1", "1 New Street",
                    "townOrCity", "London",
                    "postcode", "EC1A 1BB"),
                "payment",
                Map.of("paymentMethod", "CHECK"),
                "contractManager",
                Map.of("contractManagerGuid", "12345678-1234-1234-1234-123456789012"),
                "liaisonManager",
                Map.of(
                    "firstName", "Test",
                    "lastName", "Manager",
                    "emailAddress", "test.manager@example.com",
                    "telephoneNumber", "020 1111 2222")));

    Response response =
        given()
            .contentType(ContentType.JSON)
            .body(body)
            .when()
            .post("/provider-firms")
            .then()
            .statusCode(201)
            .body("data.providerFirmGUID", notNullValue())
            .body("data.providerFirmNumber", notNullValue())
            .extract()
            .response();

    String firmNumber = response.path("data.providerFirmNumber");

    // Verify the created firm is retrievable via GET
    given()
        .pathParam("firmId", firmNumber)
        .when()
        .get("/provider-firms/{firmId}")
        .then()
        .statusCode(200)
        .body("data.firmNumber", equalTo(firmNumber))
        .body("data.name", equalTo(firmName))
        .body("data.firmType", equalTo("Legal Services Provider"));
  }

  /**
   * AC2 – Provider type must be explicitly LSP AC3 – Business rules enforced AC4 – No partial Legal
   * Organisation records Verifies that supplying an LSP schema attribute when firmType is not
   * "Legal Services Provider" results in a 400 Bad Request.
   */
  @Test
  void createChambersFirm_withLspAttribute_returns400() {
    String firmName = "E2E-DSTEW Chambers " + System.currentTimeMillis();
    Map<String, Object> body =
        Map.of(
            "firmType",
            "Chambers",
            "name",
            firmName,
            "legalServicesProvider",
            Map.of(
                "constitutionalStatus",
                "Partnership",
                "address",
                Map.of("line1", "1 New Street", "townOrCity", "London", "postcode", "EC1A 1BB"),
                "payment",
                Map.of(
                    "paymentMethod",
                    "EFT",
                    "bankAccountDetails",
                    Map.of(
                        "accountNumber", "12345678",
                        "sortCode", "12-34-56",
                        "accountName", "Test Account")),
                "liaisonManager",
                Map.of(
                    "firstName", "Test",
                    "lastName", "Manager",
                    "emailAddress", "test@example.com",
                    "telephoneNumber", "020 1111 2222")));

    given()
        .contentType(ContentType.JSON)
        .body(body)
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

  /** Verifies that omitting constitutionalStatus on an LSP firm results in a 400 Bad Request. */
  @Test
  void createLspFirm_missingConstitutionalStatus_returns400() {
    String firmName = "E2E-DSTEW LSP " + System.currentTimeMillis();
    Map<String, Object> body =
        Map.of(
            "firmType",
            "Legal Services Provider",
            "name",
            firmName,
            "legalServicesProvider",
            Map.of(
                "address",
                Map.of("line1", "1 New Street", "townOrCity", "London", "postcode", "EC1A 1BB"),
                "payment",
                Map.of("paymentMethod", "CHECK"),
                "liaisonManager",
                Map.of(
                    "firstName", "Test",
                    "lastName", "Manager",
                    "emailAddress", "test@example.com",
                    "telephoneNumber", "020 1111 2222")));

    given()
        .contentType(ContentType.JSON)
        .body(body)
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

  /** Verifies that an unrecognised constitutionalStatus value on an LSP firm results in a 400. */
  @Test
  void createLspFirm_invalidConstitutionalStatus_returns400() {
    String firmName = "E2E-DSTEW LSP " + System.currentTimeMillis();
    Map<String, Object> body =
        Map.of(
            "firmType",
            "Legal Services Provider",
            "name",
            firmName,
            "legalServicesProvider",
            Map.of(
                "constitutionalStatus",
                "InvalidStatus",
                "address",
                Map.of("line1", "1 New Street", "townOrCity", "London", "postcode", "EC1A 1BB"),
                "payment",
                Map.of(
                    "paymentMethod",
                    "EFT",
                    "bankAccountDetails",
                    Map.of(
                        "accountNumber", "12345678",
                        "sortCode", "12-34-56",
                        "accountName", "Test Account")),
                "liaisonManager",
                Map.of(
                    "firstName", "Test",
                    "lastName", "Manager",
                    "emailAddress", "test@example.com",
                    "telephoneNumber", "020 1111 2222")));

    given()
        .contentType(ContentType.JSON)
        .body(body)
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

  @Test
  void createLspFirmWithoutValidLiaison_returns400() {
    String firmName = "E2E-DSTEW LSP " + System.currentTimeMillis();

    Map<String, Object> body =
        Map.of(
            "firmType",
            "Legal Services Provider",
            "name",
            firmName,
            "legalServicesProvider",
            Map.of(
                "constitutionalStatus",
                "Partnership",
                "address",
                Map.of(
                    "line1", "1 New Street",
                    "townOrCity", "London",
                    "postcode", "EC1A 1BB"),
                "payment",
                Map.of("paymentMethod", "CHECK"),
                "contractManager",
                Map.of("contractManagerGuid", "12345678-1234-1234-1234-123456789012")));

    Response response =
        given()
            .contentType(ContentType.JSON)
            .body(body)
            .when()
            .post("/provider-firms")
            .then()
            .statusCode(400)
            .extract()
            .response();
  }

  @Test
  void createChambersFirm_returns201ThenGetReturnsCreatedFirm() {
    String firmName = "E2E-DSTEW Chambers " + System.currentTimeMillis();

    Map<String, Object> body =
        Map.of(
            "firmType",
            "Chambers",
            "name",
            firmName,
            "chambers",
            Map.of(
                "address",
                Map.of(
                    "line1", "2 Chambers Court",
                    "townOrCity", "London",
                    "postcode", "WC2A 3EB"),
                "liaisonManager",
                Map.of(
                    "firstName", "Chambers",
                    "lastName", "Liaison",
                    "emailAddress", "chambers.liaison@example.com",
                    "telephoneNumber", "020 3333 4444")));

    Response response =
        given()
            .contentType(ContentType.JSON)
            .body(body)
            .when()
            .post("/provider-firms")
            .then()
            .statusCode(201)
            .body("data.providerFirmGUID", notNullValue())
            .body("data.providerFirmNumber", notNullValue())
            .extract()
            .response();

    String firmNumber = response.path("data.providerFirmNumber");

    // Verify the created firm is retrievable via GET
    given()
        .pathParam("firmId", firmNumber)
        .when()
        .get("/provider-firms/{firmId}")
        .then()
        .statusCode(200)
        .body("data.firmNumber", equalTo(firmNumber))
        .body("data.name", equalTo(firmName))
        .body("data.firmType", equalTo("Chambers"));
  }

  @Test
  void createProviderFirm_missingName_returns400() {
    Map<String, Object> body =
        Map.of(
            "firmType",
            "Legal Services Provider",
            "legalServicesProvider",
            Map.of(
                "address",
                Map.of("line1", "1 New Street", "townOrCity", "London", "postcode", "EC1A 1BB"),
                "payment",
                Map.of("paymentMethod", "EFT"),
                "liaisonManager",
                Map.of(
                    "firstName", "Test",
                    "lastName", "Manager",
                    "emailAddress", "test@example.com",
                    "telephoneNumber", "020 1111 2222")));

    given()
        .contentType(ContentType.JSON)
        .body(body)
        .when()
        .post("/provider-firms")
        .then()
        .statusCode(400);
  }

  @Test
  void createProviderFirm_multipleVariants_returns400() {
    Map<String, Object> address =
        Map.of("line1", "1 Street", "townOrCity", "London", "postcode", "EC1A 1BB");
    Map<String, Object> liaisonManager =
        Map.of(
            "firstName", "A",
            "lastName", "B",
            "emailAddress", "a@example.com",
            "telephoneNumber", "020 0000 0000");

    Map<String, Object> body =
        Map.of(
            "firmType",
            "Legal Services Provider",
            "name",
            "Bad Firm",
            "legalServicesProvider",
            Map.of(
                "address", address,
                "payment", Map.of("paymentMethod", "EFT"),
                "liaisonManager", liaisonManager),
            "chambers",
            Map.of("address", address, "liaisonManager", liaisonManager));

    given()
        .contentType(ContentType.JSON)
        .body(body)
        .when()
        .post("/provider-firms")
        .then()
        .statusCode(400);
  }
}
