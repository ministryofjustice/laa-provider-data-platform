package uk.gov.justice.laa.providerdata.e2e.modifying;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

import io.restassured.http.ContentType;
import io.restassured.response.Response;
import java.util.List;
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

    given()
        .pathParam("firmId", E2eConfig.lspFirmNumber())
        .pathParam("officeCode", officeGuid)
        .when()
        .get("/provider-firms/{firmId}/offices/{officeCode}/bank-details")
        .then()
        .statusCode(200)
        .body("data.content.find { it.primaryFlag == true }.guid", equalTo(existingBankAccountGuid))
        .body("data.content.find { it.primaryFlag == true }.createdBy", notNullValue())
        .body("data.content.find { it.primaryFlag == true }.createdTimestamp", notNullValue());
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
            equalTo(uniqueAccountNumber))
        .body(
            "data.content.find { it.accountNumber == '" + uniqueAccountNumber + "' }.createdBy",
            notNullValue())
        .body(
            "data.content.find { it.accountNumber == '"
                + uniqueAccountNumber
                + "' }.createdTimestamp",
            notNullValue())
        .body(
            "data.content.find { it.accountNumber == '" + uniqueAccountNumber + "' }.lastUpdatedBy",
            notNullValue())
        .body(
            "data.content.find { it.accountNumber == '"
                + uniqueAccountNumber
                + "' }.lastUpdatedTimestamp",
            notNullValue());
  }

  @Test
  void patchOffice_linkUnknownBankAccountGuid_returns404() {
    Integer countBefore =
        given()
            .pathParam("firmId", E2eConfig.lspFirmNumber())
            .pathParam("officeCode", E2eConfig.lspOfficeCode())
            .when()
            .get("/provider-firms/{firmId}/offices/{officeCode}/bank-details")
            .then()
            .statusCode(200)
            .extract()
            .path("data.metadata.pagination.totalItems");

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

    given()
        .pathParam("firmId", E2eConfig.lspFirmNumber())
        .pathParam("officeCode", E2eConfig.lspOfficeCode())
        .when()
        .get("/provider-firms/{firmId}/offices/{officeCode}/bank-details")
        .then()
        .statusCode(200)
        .body("data.metadata.pagination.totalItems", equalTo(countBefore));
  }

  /**
   * Verifies that only one bank account is marked as primary for an office at any given time.
   *
   * <p>Creates a new LSP firm via {@code POST /provider-firms} with an initial EFT bank account,
   * then performs a PATCH request linking a distinct new bank account to the head office. After the
   * switch the office b ank-details endpoint must return exactly one record with {@code
   * primaryFlag=true}.
   */
  @Test
  void postFirmWithBankAccount_thenSwitch_onlyOneRecordIsPrimaryAtATime() {
    long ts = System.currentTimeMillis();
    String firmName = "E2E-DSTEW Primary-Flag " + ts;

    // --- Step 1: create a new LSP firm with an initial EFT bank account ---
    String initialAccountNumber = "6" + (ts % 10_000_000L);
    String firmNumber =
        given()
            .contentType(ContentType.JSON)
            .body(
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
                            "line1", "1 Test Street",
                            "townOrCity", "London",
                            "postcode", "EC1A 1BB"),
                        "payment",
                        Map.of(
                            "paymentMethod",
                            "EFT",
                            "paymentHeldFlag",
                            false,
                            "bankAccountDetails",
                            Map.of(
                                "accountName",
                                "Initial Account",
                                "sortCode",
                                "601111",
                                "accountNumber",
                                initialAccountNumber)),
                        "contractManager",
                        Map.of("contractManagerGuid", "12345678-1234-1234-1234-123456789012"),
                        "liaisonManager",
                        Map.of(
                            "firstName", "Test",
                            "lastName", "Manager",
                            "emailAddress", "test.manager@example.com",
                            "telephoneNumber", "020 1111 2222"))))
            .when()
            .post("/provider-firms")
            .then()
            .statusCode(201)
            .extract()
            .path("data.providerFirmNumber");

    // --- Step 2: resolve the head office GUID ---
    String officeGuid =
        given()
            .pathParam("firmId", firmNumber)
            .when()
            .get("/provider-firms/{firmId}/offices")
            .then()
            .statusCode(200)
            .extract()
            .path("data.content[0].guid");

    // Assert the initial account is primary (baseline)
    List<Boolean> flagsAfterCreate =
        given()
            .pathParam("firmId", firmNumber)
            .pathParam("officeCode", officeGuid)
            .when()
            .get("/provider-firms/{firmId}/offices/{officeCode}/bank-details")
            .then()
            .statusCode(200)
            .extract()
            .path("data.content.primaryFlag");

    org.hamcrest.MatcherAssert.assertThat(
        "exactly one primary flag after firm creation",
        flagsAfterCreate.stream().filter(Boolean.TRUE::equals).count(),
        equalTo(1L));

    // --- Step 3: first switch — assign account A ---
    String accountNumberA = "8" + ((ts + 1) % 10_000_000L);
    given()
        .contentType(ContentType.JSON)
        .pathParam("firmId", firmNumber)
        .pathParam("officeCode", officeGuid)
        .body(
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
                        "Primary Test Account A",
                        "sortCode",
                        "601111",
                        "accountNumber",
                        accountNumberA))))
        .when()
        .patch("/provider-firms/{firmId}/offices/{officeCode}")
        .then()
        .statusCode(200);

    List<Boolean> flagsAfterFirstSwitch =
        given()
            .pathParam("firmId", firmNumber)
            .pathParam("officeCode", officeGuid)
            .when()
            .get("/provider-firms/{firmId}/offices/{officeCode}/bank-details")
            .then()
            .statusCode(200)
            .extract()
            .path("data.content.primaryFlag");

    org.hamcrest.MatcherAssert.assertThat(
        "exactly one primary flag after first switch",
        flagsAfterFirstSwitch.stream().filter(Boolean.TRUE::equals).count(),
        equalTo(1L));
  }

  /**
   * AC11 – Previous association retained as history (not deleted).
   *
   * <p>Creates a new LSP firm with an initial EFT bank account, then switches to a new account via
   * PATCH. The GET response for the office bank-details must still contain the original link row
   * (with {@code primaryFlag=false} and a non-null {@code activeDateTo}), proving it was end-dated
   * rather than deleted.
   */
  @Test
  void postFirmWithBankAccount_thenSwitch_previousAssociationRetainedAsHistory() {
    long timestamp = System.currentTimeMillis();

    // create an LSP firm with an initial EFT bank account
    String initialAccountNumber = "5" + (timestamp % 10_000_000L);
    String firmNumber =
        given()
            .contentType(ContentType.JSON)
            .body(
                Map.of(
                    "firmType",
                    "Legal Services Provider",
                    "name",
                    "E2E-DSTEW AC11 " + timestamp,
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
                        Map.of(
                            "paymentMethod",
                            "EFT",
                            "paymentHeldFlag",
                            false,
                            "bankAccountDetails",
                            Map.of(
                                "accountName", "Initial Account",
                                "sortCode", "601111",
                                "accountNumber", initialAccountNumber)),
                        "contractManager",
                        Map.of("contractManagerGuid", "12345678-1234-1234-1234-123456789012"),
                        "liaisonManager",
                        Map.of(
                            "firstName", "Test",
                            "lastName", "Manager",
                            "emailAddress", "test.manager@example.com",
                            "telephoneNumber", "020 1111 2222"))))
            .when()
            .post("/provider-firms")
            .then()
            .statusCode(201)
            .extract()
            .path("data.providerFirmNumber");

    String officeGuid =
        given()
            .pathParam("firmId", firmNumber)
            .when()
            .get("/provider-firms/{firmId}/offices")
            .then()
            .statusCode(200)
            .extract()
            .path("data.content[0].guid");

    // switch to a new bank account
    String newAccountNumber = "4" + ((timestamp + 1) % 10_000_000L);
    given()
        .contentType(ContentType.JSON)
        .pathParam("firmId", firmNumber)
        .pathParam("officeCode", officeGuid)
        .body(
            Map.of(
                "payment",
                Map.of(
                    "paymentMethod",
                    "EFT",
                    "paymentHeldFlag",
                    false,
                    "bankAccountDetails",
                    Map.of(
                        "accountName", "Replacement Account",
                        "sortCode", "601111",
                        "accountNumber", newAccountNumber))))
        .when()
        .patch("/provider-firms/{firmId}/offices/{officeCode}")
        .then()
        .statusCode(200);

    given()
        .pathParam("firmId", firmNumber)
        .pathParam("officeCode", officeGuid)
        .when()
        .get("/provider-firms/{firmId}/offices/{officeCode}/bank-details")
        .then()
        .statusCode(200)
        .body("data.content", hasSize(2))
        .body("data.content.findAll { it.primaryFlag == true }", hasSize(1))
        .body(
            "data.content.findAll { it.primaryFlag == true }.accountNumber[0]",
            equalTo(newAccountNumber))
        .body("data.content.findAll { it.primaryFlag == false }", hasSize(1))
        .body(
            "data.content.findAll { it.primaryFlag == false }.accountNumber[0]",
            equalTo(initialAccountNumber))
        .body(
            "data.content.findAll { it.primaryFlag == false }.activeDateTo[0]",
            notNullValue()) // historical
        .body(
            "data.content.findAll { it.primaryFlag == true }.activeDateTo[0]",
            nullValue()); // primary
  }

  /**
   * AC12 – Revert to a previously associated Bank Account (make it Primary again).
   *
   * <p>Creates a new LSP firm with an initial EFT bank account (account 0), switches to a new
   * account A, then re-links back to account 0 by GUID. After reverting:
   *
   * <ul>
   *   <li>Account 0 is the current primary ({@code primaryFlag=true}, null {@code activeDateTo}).
   *   <li>Account A is end-dated ({@code primaryFlag=false}, non-null {@code activeDateTo}).
   *   <li>Three rows exist in total (account 0 history, account A history, account 0 current) —
   *       nothing is deleted.
   * </ul>
   */
  @Test
  void postFirmWithBankAccount_thenSwitch_thenRevertToPrevious_previousBecomesCurrentPrimary() {
    long ts = System.currentTimeMillis();

    String account0Number = "3" + (ts % 10_000_000L);
    String firmNumber =
        given()
            .contentType(ContentType.JSON)
            .body(
                Map.of(
                    "firmType",
                    "Legal Services Provider",
                    "name",
                    "E2E-DSTEW AC12 " + ts,
                    "legalServicesProvider",
                    Map.of(
                        "constitutionalStatus",
                        "Partnership",
                        "address",
                        Map.of(
                            "line1", "1 Revert Street",
                            "townOrCity", "London",
                            "postcode", "EC1A 1BB"),
                        "payment",
                        Map.of(
                            "paymentMethod",
                            "EFT",
                            "paymentHeldFlag",
                            false,
                            "bankAccountDetails",
                            Map.of(
                                "accountName", "Account Zero",
                                "sortCode", "601111",
                                "accountNumber", account0Number)),
                        "contractManager",
                        Map.of("contractManagerGuid", "12345678-1234-1234-1234-123456789012"),
                        "liaisonManager",
                        Map.of(
                            "firstName", "Test",
                            "lastName", "Manager",
                            "emailAddress", "test.manager@example.com",
                            "telephoneNumber", "020 1111 2222"))))
            .when()
            .post("/provider-firms")
            .then()
            .statusCode(201)
            .extract()
            .path("data.providerFirmNumber");

    String officeGuid =
        given()
            .pathParam("firmId", firmNumber)
            .when()
            .get("/provider-firms/{firmId}/offices")
            .then()
            .statusCode(200)
            .extract()
            .path("data.content[0].guid");

    String accountANumber = "2" + ((ts + 1) % 10_000_000L);
    given()
        .contentType(ContentType.JSON)
        .pathParam("firmId", firmNumber)
        .pathParam("officeCode", officeGuid)
        .body(
            Map.of(
                "payment",
                Map.of(
                    "paymentMethod",
                    "EFT",
                    "paymentHeldFlag",
                    false,
                    "bankAccountDetails",
                    Map.of(
                        "accountName", "Account A",
                        "sortCode", "601111",
                        "accountNumber", accountANumber))))
        .when()
        .patch("/provider-firms/{firmId}/offices/{officeCode}")
        .then()
        .statusCode(200);

    String account0Guid =
        given()
            .pathParam("firmId", firmNumber)
            .pathParam("officeCode", officeGuid)
            .when()
            .get("/provider-firms/{firmId}/offices/{officeCode}/bank-details")
            .then()
            .statusCode(200)
            .extract()
            .path("data.content.findAll { it.accountNumber == '" + account0Number + "' }.guid[0]");

    given()
        .contentType(ContentType.JSON)
        .pathParam("firmId", firmNumber)
        .pathParam("officeCode", officeGuid)
        .body(
            Map.of(
                "payment",
                Map.of(
                    "paymentMethod",
                    "EFT",
                    "paymentHeldFlag",
                    false,
                    "bankAccountDetails",
                    Map.of("type", "link", "bankAccountGUID", account0Guid))))
        .when()
        .patch("/provider-firms/{firmId}/offices/{officeCode}")
        .then()
        .statusCode(200);

    given()
        .pathParam("firmId", firmNumber)
        .pathParam("officeCode", officeGuid)
        .when()
        .get("/provider-firms/{firmId}/offices/{officeCode}/bank-details")
        .then()
        .statusCode(200)
        .body("data.content", hasSize(3))
        .body("data.content.findAll { it.primaryFlag == true }", hasSize(1))
        .body(
            "data.content.findAll { it.primaryFlag == true }.accountNumber[0]",
            equalTo(account0Number))
        .body("data.content.findAll { it.primaryFlag == true }.activeDateTo[0]", nullValue())
        .body(
            "data.content.findAll { it.accountNumber == '" + accountANumber + "' }.primaryFlag[0]",
            equalTo(false))
        .body(
            "data.content.findAll { it.accountNumber == '" + accountANumber + "' }.activeDateTo[0]",
            notNullValue())
        .body("data.content.findAll { it.accountNumber == '" + account0Number + "' }", hasSize(2));
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
