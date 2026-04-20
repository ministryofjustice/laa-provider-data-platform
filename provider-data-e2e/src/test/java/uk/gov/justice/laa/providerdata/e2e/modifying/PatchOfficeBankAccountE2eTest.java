package uk.gov.justice.laa.providerdata.e2e.modifying;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

import io.restassured.http.ContentType;
import io.restassured.response.Response;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import uk.gov.justice.laa.providerdata.e2e.E2eConfig;
import uk.gov.justice.laa.providerdata.e2e.ModifyingTest;

/**
 * Data-modifying e2e tests for bank account reassignment via {@code PATCH
 * /provider-firms/{firmId}/offices/{officeCode}}.
 *
 * <p>Each test patches the E2E LSP office's payment details. Because {@link
 * uk.gov.justice.laa.providerdata.service.BankDetailsService} end-dates the previous primary link
 * before creating the new one, repeated runs accumulate historical records rather than causing
 * constraint violations.
 */
@ModifyingTest
class PatchOfficeBankAccountE2eTest {

  private static String existingBankAccountGuid;

  @BeforeAll
  static void lookUpBankAccountGuid() {
    existingBankAccountGuid =
        given()
            .pathParam("firmId", E2eConfig.lspFirmNumber())
            .when()
            .get("/provider-firms/{firmId}/bank-details")
            .then()
            .statusCode(200)
            .extract()
            .path("data.content[0].guid");
  }

  @Test
  void patchOffice_linkExistingBankAccount_returns200WithGuids() {
    Map<String, Object> body =
        Map.of(
            "payment",
            Map.of(
                "paymentMethod",
                "EFT",
                "paymentHeldFlag",
                false,
                "bankAccountDetails",
                Map.of("type", "link", "bankAccountGUID", existingBankAccountGuid)));

    given()
        .contentType(ContentType.JSON)
        .pathParam("firmId", E2eConfig.lspFirmNumber())
        .pathParam("officeCode", E2eConfig.lspOfficeCode())
        .body(body)
        .when()
        .patch("/provider-firms/{firmId}/offices/{officeCode}")
        .then()
        .statusCode(200)
        .body("data.providerFirmGUID", notNullValue())
        .body("data.providerFirmNumber", equalTo(E2eConfig.lspFirmNumber()))
        .body("data.officeGUID", notNullValue())
        .body("data.officeCode", equalTo(E2eConfig.lspOfficeCode()));
  }

  @Test
  void patchOffice_createAndLinkNewBankAccount_returns200WithGuids() {
    String uniqueAccountNumber = "9" + (System.currentTimeMillis() % 10_000_000L);

    Map<String, Object> body =
        Map.of(
            "payment",
            Map.of(
                "paymentMethod",
                "EFT",
                "paymentHeldFlag",
                false,
                "bankAccountDetails",
                Map.of(
                    "accountName",
                    "E2E Test Account " + System.currentTimeMillis(),
                    "sortCode",
                    "601111",
                    "accountNumber",
                    uniqueAccountNumber)));

    Response response =
        given()
            .contentType(ContentType.JSON)
            .pathParam("firmId", E2eConfig.lspFirmNumber())
            .pathParam("officeCode", E2eConfig.lspOfficeCode())
            .body(body)
            .when()
            .patch("/provider-firms/{firmId}/offices/{officeCode}")
            .then()
            .statusCode(200)
            .body("data.providerFirmGUID", notNullValue())
            .body("data.providerFirmNumber", equalTo(E2eConfig.lspFirmNumber()))
            .body("data.officeGUID", notNullValue())
            .body("data.officeCode", equalTo(E2eConfig.lspOfficeCode()))
            .extract()
            .response();

    String officeGuid = response.path("data.officeGUID");

    // Verify the new account appears as primary on the office
    given()
        .pathParam("firmId", E2eConfig.lspFirmNumber())
        .pathParam("officeCode", officeGuid)
        .when()
        .get("/provider-firms/{firmId}/offices/{officeCode}/bank-details")
        .then()
        .statusCode(200)
        .body(
            "data.content.findAll { it.primaryFlag == true }.accountNumber[0]",
            equalTo(uniqueAccountNumber));
  }

  @Test
  void patchOffice_linkUnknownBankAccountGuid_returns404() {
    Map<String, Object> body =
        Map.of(
            "payment",
            Map.of(
                "paymentMethod",
                "EFT",
                "paymentHeldFlag",
                false,
                "bankAccountDetails",
                Map.of("type", "link", "bankAccountGUID", UUID.randomUUID().toString())));

    given()
        .contentType(ContentType.JSON)
        .pathParam("firmId", E2eConfig.lspFirmNumber())
        .pathParam("officeCode", E2eConfig.lspOfficeCode())
        .body(body)
        .when()
        .patch("/provider-firms/{firmId}/offices/{officeCode}")
        .then()
        .statusCode(404);
  }

  @Test
  void patchOffice_unknownOfficeCode_returns404() {
    Map<String, Object> body =
        Map.of(
            "payment",
            Map.of(
                "paymentMethod",
                "EFT",
                "paymentHeldFlag",
                false,
                "bankAccountDetails",
                Map.of("type", "link", "bankAccountGUID", existingBankAccountGuid)));

    given()
        .contentType(ContentType.JSON)
        .pathParam("firmId", E2eConfig.lspFirmNumber())
        .pathParam("officeCode", E2eConfig.invalidOfficeCode())
        .body(body)
        .when()
        .patch("/provider-firms/{firmId}/offices/{officeCode}")
        .then()
        .statusCode(404);
  }
}
