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

  /**
   * AC1 – When a new bank account is assigned to an office, the old (existing) bank account link is
   * marked as non-primary and its activeDateTo is set to the current date.
   */
  @Test
  void ac1_assignNewBankAccount_marksOldAsNonPrimaryWithActiveDateTo() {
    long timestamp = System.currentTimeMillis();
    String firmName = "E2E-AC1 Old-NonPrimary " + timestamp;

    // Create firm with initial account
    String initialAccountNumber = "7" + (timestamp % 10_000_000L);
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
                            "line1", "1 AC1 Test Street",
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
                                "accountName", "AC1 Initial Account",
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

    // Capture initial state — old account before update
    Response oldAccountBefore =
        given()
            .pathParam("firmId", firmNumber)
            .pathParam("officeCode", officeGuid)
            .when()
            .get("/provider-firms/{firmId}/offices/{officeCode}/bank-details")
            .then()
            .statusCode(200)
            .extract()
            .response();

    String oldAccountGuid = oldAccountBefore.path("data.content[0].guid");
    String oldAccountNumber = oldAccountBefore.path("data.content[0].accountNumber");

    // Assign new account
    String newAccountNumber = "1" + ((timestamp + 1) % 10_000_000L);
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
                        "accountName", "AC1 New Account",
                        "sortCode", "601111",
                        "accountNumber", newAccountNumber))))
        .when()
        .patch("/provider-firms/{firmId}/offices/{officeCode}")
        .then()
        .statusCode(200);

    // Verify both accounts exist in bank details
    given()
        .pathParam("firmId", firmNumber)
        .pathParam("officeCode", officeGuid)
        .when()
        .get("/provider-firms/{firmId}/offices/{officeCode}/bank-details")
        .then()
        .statusCode(200)
        .body("data.content", hasSize(2))
        // Old account: non-primary with activeDateTo set
        .body(
            "data.content.findAll { it.guid == '" + oldAccountGuid + "' }.primaryFlag[0]",
            equalTo(false))
        .body(
            "data.content.findAll { it.guid == '" + oldAccountGuid + "' }.activeDateTo[0]",
            notNullValue())
        .body(
            "data.content.findAll { it.guid == '" + oldAccountGuid + "' }.accountNumber[0]",
            equalTo(oldAccountNumber))
        // New account: primary with no activeDateTo
        .body(
            "data.content.findAll { it.accountNumber == '"
                + newAccountNumber
                + "' }.primaryFlag[0]",
            equalTo(true))
        .body(
            "data.content.findAll { it.accountNumber == '"
                + newAccountNumber
                + "' }.activeDateTo[0]",
            nullValue());
  }

  /** AC2 – Restricted fields must not be amended when a new bank account is assigned. */
  @Test
  void ac2_restrictedFields_remainUnchangedWhenAssigningNewAccount() {
    long timestamp = System.currentTimeMillis();
    String firmName = "E2E-AC2 Restricted-Fields " + timestamp;

    String initialAccountNumber = "2" + (timestamp % 10_000_000L);
    String initialAccountName = "AC2 Initial Acct " + timestamp;
    String initialSortCode = "601111";

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
                            "line1", "1 AC2 Test Street",
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
                                "accountName", initialAccountName,
                                "sortCode", initialSortCode,
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

    // Capture old account state before update
    Response oldAccountState =
        given()
            .pathParam("firmId", firmNumber)
            .pathParam("officeCode", officeGuid)
            .when()
            .get("/provider-firms/{firmId}/offices/{officeCode}/bank-details")
            .then()
            .statusCode(200)
            .extract()
            .response();

    String newAccountNumber = "0" + ((timestamp + 2) % 10_000_000L);
    String newAccountName = "AC2 New Acct " + timestamp;
    String newSortCode = "602222";

    // Assign new account
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
                        "accountName", newAccountName,
                        "sortCode", newSortCode,
                        "accountNumber", newAccountNumber))))
        .when()
        .patch("/provider-firms/{firmId}/offices/{officeCode}")
        .then()
        .statusCode(200);

    // Verify old account fields are unchanged (except primaryFlag and activeDateTo)
    given()
        .pathParam("firmId", firmNumber)
        .pathParam("officeCode", officeGuid)
        .when()
        .get("/provider-firms/{firmId}/offices/{officeCode}/bank-details")
        .then()
        .statusCode(200)
        // Old account core fields unchanged
        .body(
            "data.content.findAll { it.accountNumber == '"
                + initialAccountNumber
                + "' }.accountName[0]",
            equalTo(initialAccountName))
        .body(
            "data.content.findAll { it.accountNumber == '"
                + initialAccountNumber
                + "' }.sortCode[0]",
            equalTo(initialSortCode))
        .body(
            "data.content.findAll { it.accountNumber == '"
                + initialAccountNumber
                + "' }.accountNumber[0]",
            equalTo(initialAccountNumber))
        // New account fields match request
        .body(
            "data.content.findAll { it.accountNumber == '"
                + newAccountNumber
                + "' }.accountName[0]",
            equalTo(newAccountName))
        .body(
            "data.content.findAll { it.accountNumber == '" + newAccountNumber + "' }.sortCode[0]",
            equalTo(newSortCode))
        .body(
            "data.content.findAll { it.accountNumber == '"
                + newAccountNumber
                + "' }.accountNumber[0]",
            equalTo(newAccountNumber));
  }

  /** AC3 – Bank Account validity must be preserved. */
  @Test
  void ac3_bankAccountValidityPreserved_afterAmendment() {
    long timestamp = System.currentTimeMillis();
    String firmName = "E2E-AC3 Validity " + timestamp;

    String initialAccountNumber = "4" + (timestamp % 10_000_000L);
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
                            "line1", "1 AC3 Test Street",
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
                                "accountName", "AC3 Initial",
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

    String newAccountNumber = "5" + ((timestamp + 3) % 10_000_000L);
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
                        "accountName", "AC3 New",
                        "sortCode", "601111",
                        "accountNumber", newAccountNumber))))
        .when()
        .patch("/provider-firms/{firmId}/offices/{officeCode}")
        .then()
        .statusCode(200);

    // Verify validity constraints
    given()
        .pathParam("firmId", firmNumber)
        .pathParam("officeCode", officeGuid)
        .when()
        .get("/provider-firms/{firmId}/offices/{officeCode}/bank-details")
        .then()
        .statusCode(200)
        // Both accounts still exist (no orphan state)
        .body("data.content", hasSize(2))
        // Exactly one primary
        .body("data.content.findAll { it.primaryFlag == true }", hasSize(1))
        // New account complete and valid
        .body(
            "data.content.findAll { it.accountNumber == '"
                + newAccountNumber
                + "' }.accountName[0]",
            notNullValue())
        .body(
            "data.content.findAll { it.accountNumber == '" + newAccountNumber + "' }.sortCode[0]",
            notNullValue())
        .body(
            "data.content.findAll { it.accountNumber == '"
                + newAccountNumber
                + "' }.accountNumber[0]",
            notNullValue())
        // Old account still complete (not deleted or corrupted)
        .body(
            "data.content.findAll { it.accountNumber == '"
                + initialAccountNumber
                + "' }.accountName[0]",
            notNullValue())
        .body(
            "data.content.findAll { it.accountNumber == '"
                + initialAccountNumber
                + "' }.sortCode[0]",
            notNullValue());
  }

  /** AC4 – No partial bank account update on validation failure. */
  @Test
  void ac4_noPartialUpdate_whenValidationFails() {
    long timestamp = System.currentTimeMillis();
    String firmName = "E2E-AC4 NoPartial " + timestamp;

    String initialAccountNumber = "6" + (timestamp % 10_000_000L);
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
                            "line1", "1 AC4 Test Street",
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
                                "accountName", "AC4 Initial",
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

    // Capture state before failed update
    Response stateBefore =
        given()
            .pathParam("firmId", firmNumber)
            .pathParam("officeCode", officeGuid)
            .when()
            .get("/provider-firms/{firmId}/offices/{officeCode}/bank-details")
            .then()
            .statusCode(200)
            .extract()
            .response();

    int accountCountBefore = stateBefore.path("data.content.size()");
    boolean wasPrimaryBefore = stateBefore.path("data.content[0].primaryFlag");

    // Attempt invalid patch with non-existent bank account GUID
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
                    Map.of("type", "link", "bankAccountGUID", UUID.randomUUID().toString()))))
        .when()
        .patch("/provider-firms/{firmId}/offices/{officeCode}")
        .then()
        .statusCode(404); // Should be rejected

    // Verify no partial update occurred (state unchanged)
    given()
        .pathParam("firmId", firmNumber)
        .pathParam("officeCode", officeGuid)
        .when()
        .get("/provider-firms/{firmId}/offices/{officeCode}/bank-details")
        .then()
        .statusCode(200)
        // Account count unchanged
        .body("data.content", hasSize(accountCountBefore))
        // Original account still primary
        .body("data.content[0].primaryFlag", equalTo(wasPrimaryBefore))
        // Original account number unchanged
        .body("data.content[0].accountNumber", equalTo(initialAccountNumber));
  }

  /**
   * AC5 – Bank Account must always have an association to exist.
   *
   * <p>Verifies that bank accounts in the system are always associated with at least one
   * provider-office link. Tests that querying bank details returns only accounts with valid
   * associations, and that the amendment doesn't result in orphaned accounts.
   */
  @Test
  void ac5_bankAccountAlwaysHasAssociation() {
    long timestamp = System.currentTimeMillis();
    String firmName = "E2E-AC5 Association " + timestamp;

    String initialAccountNumber = "8" + (timestamp % 10_000_000L);
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
                            "line1", "1 AC5 Test Street",
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
                                "accountName", "AC5 Initial",
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

    String newAccountNumber = "9" + ((timestamp + 4) % 10_000_000L);
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
                        "accountName", "AC5 New",
                        "sortCode", "601111",
                        "accountNumber", newAccountNumber))))
        .when()
        .patch("/provider-firms/{firmId}/offices/{officeCode}")
        .then()
        .statusCode(200);

    // Verify both old and new accounts are still associated (retrievable) from the office
    given()
        .pathParam("firmId", firmNumber)
        .pathParam("officeCode", officeGuid)
        .when()
        .get("/provider-firms/{firmId}/offices/{officeCode}/bank-details")
        .then()
        .statusCode(200)
        .body("data.content", hasSize(2))
        // Both accounts have valid office links (retrievable via office endpoint)
        .body(
            "data.content.findAll { it.accountNumber == '" + initialAccountNumber + "' }",
            hasSize(1))
        .body("data.content.findAll { it.accountNumber == '" + newAccountNumber + "' }", hasSize(1))
        // Both have activeDateFrom set (proof of association)
        .body(
            "data.content.findAll { it.accountNumber == '"
                + initialAccountNumber
                + "' }.activeDateFrom[0]",
            notNullValue())
        .body(
            "data.content.findAll { it.accountNumber == '"
                + newAccountNumber
                + "' }.activeDateFrom[0]",
            notNullValue());

    // Verify accounts are still retrievable at firm level (association to provider still exists)
    given()
        .pathParam("firmId", firmNumber)
        .when()
        .get("/provider-firms/{firmId}/bank-details")
        .then()
        .statusCode(200)
        // Both accounts still linked to provider
        .body(
            "data.content.findAll { it.accountNumber == '" + initialAccountNumber + "' }",
            hasSize(1))
        .body(
            "data.content.findAll { it.accountNumber == '" + newAccountNumber + "' }", hasSize(1));
  }
}
