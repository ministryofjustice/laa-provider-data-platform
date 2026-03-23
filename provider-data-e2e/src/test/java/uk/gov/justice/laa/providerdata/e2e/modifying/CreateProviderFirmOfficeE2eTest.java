package uk.gov.justice.laa.providerdata.e2e.modifying;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

import io.restassured.http.ContentType;
import io.restassured.response.Response;
import java.util.Map;
import org.junit.jupiter.api.Test;
import uk.gov.justice.laa.providerdata.e2e.E2eConfig;
import uk.gov.justice.laa.providerdata.e2e.ModifyingTest;

/**
 * Data-modifying e2e tests for {@code POST /provider-firms/{firmId}/offices}.
 *
 * <p>Each test creates a new office linked to the E2E LSP provider and verifies it via GET. Cleanup
 * is handled by {@code delete-test-data.sql} which removes offices linked to E2E providers.
 */
@ModifyingTest
class CreateProviderFirmOfficeE2eTest {

  @Test
  void createOffice_forExistingLspFirm_returns201ThenGetReturnsCreatedOffice() {
    Map<String, Object> body =
        Map.of(
            "address",
            Map.of(
                "line1", "99 New Office Street " + System.currentTimeMillis(),
                "townOrCity", "Bristol",
                "postcode", "BS1 1AA"),
            "payment",
            Map.of("paymentMethod", "EFT"),
            "liaisonManager",
            Map.of(
                "firstName", "Office",
                "lastName", "Liaison",
                "emailAddress", "office.liaison." + System.currentTimeMillis() + "@example.com",
                "telephoneNumber", "0117 1111 2222"));

    Response response =
        given()
            .contentType(ContentType.JSON)
            .pathParam("firmId", E2eConfig.lspFirmNumber())
            .body(body)
            .when()
            .post("/provider-firms/{firmId}/offices")
            .then()
            .statusCode(201)
            .body("data.providerFirmGUID", notNullValue())
            .body("data.providerFirmNumber", equalTo(E2eConfig.lspFirmNumber()))
            .body("data.officeGUID", notNullValue())
            .body("data.officeCode", notNullValue())
            .extract()
            .response();

    String officeCode = response.path("data.officeCode");

    // Verify the created office is retrievable via GET
    given()
        .pathParam("firmId", E2eConfig.lspFirmNumber())
        .pathParam("officeCode", officeCode)
        .when()
        .get("/provider-firms/{firmId}/offices/{officeCode}")
        .then()
        .statusCode(200)
        .body("data.accountNumber", equalTo(officeCode))
        .body("data.address.townOrCity", equalTo("Bristol"))
        .body("data.payment.paymentMethod", equalTo("EFT"));
  }

  @Test
  void createOffice_unknownFirm_returns404() {
    Map<String, Object> body =
        Map.of(
            "address",
            Map.of("line1", "1 Street", "townOrCity", "London", "postcode", "EC1A 1BB"),
            "payment",
            Map.of("paymentMethod", "EFT"),
            "liaisonManager",
            Map.of(
                "firstName", "Test",
                "lastName", "Person",
                "emailAddress", "test@example.com",
                "telephoneNumber", "020 0000 0000"));

    given()
        .contentType(ContentType.JSON)
        .pathParam("firmId", E2eConfig.invalidFirmNumber())
        .body(body)
        .when()
        .post("/provider-firms/{firmId}/offices")
        .then()
        .statusCode(404);
  }
}
