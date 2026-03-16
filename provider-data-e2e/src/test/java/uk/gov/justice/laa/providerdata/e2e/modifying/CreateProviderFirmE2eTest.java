package uk.gov.justice.laa.providerdata.e2e.modifying;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.notNullValue;

import io.restassured.http.ContentType;
import java.util.Map;
import org.junit.jupiter.api.Test;
import uk.gov.justice.laa.providerdata.e2e.ModifyingTest;

/**
 * Data-modifying e2e tests for {@code POST /provider-firms}.
 *
 * <p>Each test creates new data in the local database. Tests use unique names (via timestamp
 * suffix) to avoid conflicts between runs.
 */
@ModifyingTest
class CreateProviderFirmE2eTest {

  @Test
  void createLspFirm_returns201WithGuidAndFirmNumber() {
    Map<String, Object> body =
        Map.of(
            "firmType",
            "Legal Services Provider",
            "name",
            "New LSP " + System.currentTimeMillis(),
            "legalServicesProvider",
            Map.of(
                "address",
                Map.of(
                    "line1", "1 New Street",
                    "townOrCity", "London",
                    "postcode", "EC1A 1BB"),
                "payment",
                Map.of("paymentMethod", "EFT"),
                "liaisonManager",
                Map.of(
                    "firstName", "Test",
                    "lastName", "Manager",
                    "emailAddress", "test.manager@example.com",
                    "telephoneNumber", "020 1111 2222")));

    given()
        .contentType(ContentType.JSON)
        .body(body)
        .when()
        .post("/provider-firms")
        .then()
        .statusCode(201)
        .body("data.providerFirmGUID", notNullValue())
        .body("data.providerFirmNumber", notNullValue());
  }

  @Test
  void createChambersFirm_returns201WithGuidAndFirmNumber() {
    Map<String, Object> body =
        Map.of(
            "firmType",
            "Chambers",
            "name",
            "New Chambers " + System.currentTimeMillis(),
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

    given()
        .contentType(ContentType.JSON)
        .body(body)
        .when()
        .post("/provider-firms")
        .then()
        .statusCode(201)
        .body("data.providerFirmGUID", notNullValue())
        .body("data.providerFirmNumber", notNullValue());
  }

  @Test
  void createProviderFirm_missingName_returns400() {
    // Missing required 'name' field; Bean Validation rejects the request → 400
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
    // Providing both legalServicesProvider and chambers is rejected by Bean Validation → 400
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
