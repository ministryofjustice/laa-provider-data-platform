package uk.gov.justice.laa.providerdata.e2e.modifying;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

import io.restassured.http.ContentType;
import io.restassured.response.Response;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import uk.gov.justice.laa.providerdata.e2e.E2eConfig;
import uk.gov.justice.laa.providerdata.e2e.ModifyingTest;

/**
 * Data-modifying e2e tests for {@code POST /provider-firms/{firmId}/offices}.
 *
 * <p>Each test creates a new office linked to the E2E LSP provider, verifies it via GET, and
 * records identifiers for cleanup in {@link #afterAll()}.
 */
@ModifyingTest
class CreateProviderFirmOfficeE2eTest {

  private static final List<String> createdOfficeCodes = new ArrayList<>();

  @AfterAll
  static void afterAll() throws SQLException {
    if (createdOfficeCodes.isEmpty()) {
      return;
    }
    try (var conn =
            DriverManager.getConnection(
                E2eConfig.dbUrl(), E2eConfig.dbUsername(), E2eConfig.dbPassword());
        var stmt = conn.createStatement()) {
      for (String officeCode : createdOfficeCodes) {
        stmt.execute(
            "DELETE FROM office_liaison_manager_link WHERE office_guid IN "
                + "(SELECT office_guid FROM provider_office_link WHERE account_number = '"
                + officeCode
                + "')");
        stmt.execute(
            "DELETE FROM office_bank_account_link WHERE provider_office_link_guid IN "
                + "(SELECT guid FROM provider_office_link WHERE account_number = '"
                + officeCode
                + "')");
        stmt.execute(
            "DELETE FROM provider_office_link WHERE account_number = '" + officeCode + "'");
      }
    }
  }

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
    createdOfficeCodes.add(officeCode);

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
