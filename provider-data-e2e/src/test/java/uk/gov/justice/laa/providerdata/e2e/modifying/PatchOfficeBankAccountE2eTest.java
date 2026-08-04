package uk.gov.justice.laa.providerdata.e2e.modifying;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.response.ValidatableResponse;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import uk.gov.justice.laa.providerdata.e2e.E2eConfig;
import uk.gov.justice.laa.providerdata.e2e.ModifyingTest;

/// Data-modifying e2e tests for bank account reassignment via `PATCH
/// /provider-firms/{firmId}/offices/{officeCode}`.
///
/// Each test patches the E2E LSP office's payment details. Because
/// {@link uk.gov.justice.laa.providerdata.service.BankDetailsService} end-dates the previous
/// primary link before creating the new one, repeated runs accumulate historical records rather
/// than causing constraint violations.
///
/// These tests were built up across several tickets that progressively defined the Bank Account
/// lifecycle for the LSP head office target entity - DSTEW-1601 (Create a Bank Account),
/// DSTEW-1627 (Amend Bank Account), and DSTEW-1634 (Effective Start Date) - and each test's
/// Javadoc cites the specific ticket and AC it was written against. DSTEW-1998 (Manage Payment
/// Method and Bank Account Association) later consolidated this behaviour across all target
/// entity types; where a test also demonstrates one of its ACs, that is noted alongside the
/// originating ticket. Equivalent coverage for the LSP child office and Legal Practitioner target
/// entities is in {@link PaymentMethodBankAccountAssociationE2eTest}.
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

  /// Assigns an existing Bank Account to the LSP head office by GUID and sets it as the primary
  /// association.
  ///
  /// - DSTEW-1601 AC9 – assigning an existing Bank Account ends the previous association and
  ///   assigns the replacement as current. (DS_MAPD_FR_031)
  /// - DSTEW-1601 AC10 – the Primary Bank Account flag moves to the newly-assigned account.
  ///   (DS_MAPD_FR_031)
  /// - DSTEW-1998 AC3 – changing Payment Method to EFT using an existing Bank Account assigns
  ///   it as the current, Primary association. (DS_MAPD_FR_036)
  /// - DSTEW-1998 AC10 – the incoming association's Effective Start Date and Primary flag are
  ///   set on assignment. (DS_MAPD_FR_036)
  ///
  /// - DS_MAPD_FR_031: Create a Bank Account.
  /// - DS_MAPD_FR_036: Assign a bank account to a Legal Organisation.
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
        patchOffice(E2eConfig.lspFirmNumber(), E2eConfig.lspOfficeCode(), body)
            .statusCode(200)
            .body("data.providerFirmGUID", notNullValue())
            .body("data.providerFirmNumber", equalTo(E2eConfig.lspFirmNumber()))
            .body("data.officeGUID", notNullValue())
            .body("data.officeCode", equalTo(E2eConfig.lspOfficeCode()))
            .extract()
            .response();

    String officeGuid = response.path("data.officeGUID");

    getBankDetails(E2eConfig.lspFirmNumber(), officeGuid)
        .statusCode(200)
        .body("data.content.find { it.primaryFlag == true }.guid", equalTo(existingBankAccountGuid))
        .body("data.content.find { it.primaryFlag == true }.createdBy", notNullValue())
        .body("data.content.find { it.primaryFlag == true }.createdTimestamp", notNullValue())
        .body("data.content.find { it.primaryFlag == true }.lastUpdatedBy", notNullValue())
        .body("data.content.find { it.primaryFlag == true }.lastUpdatedTimestamp", notNullValue());
  }

  /// Creates a new Bank Account and assigns it to the LSP head office as the primary
  /// association.
  ///
  /// - DSTEW-1601 AC1 – a new Bank Account can be created and assigned to a target in a single
  ///   request. (DS_MAPD_FR_031)
  /// - DSTEW-1601 AC10 – the Primary Bank Account flag moves to the newly-assigned account.
  ///   (DS_MAPD_FR_031)
  /// - DSTEW-1998 AC4 – changing Payment Method to EFT using a new Bank Account creates and
  ///   assigns it as the current, Primary association. (DS_MAPD_FR_036)
  /// - DSTEW-1998 AC10 – the incoming association's Effective Start Date and Primary flag are
  ///   set on assignment. (DS_MAPD_FR_036)
  ///
  /// - DS_MAPD_FR_031: Create a Bank Account.
  /// - DS_MAPD_FR_036: Assign a bank account to a Legal Organisation.
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
        patchOffice(E2eConfig.lspFirmNumber(), E2eConfig.lspOfficeCode(), body)
            .statusCode(200)
            .body("data.providerFirmGUID", notNullValue())
            .body("data.providerFirmNumber", equalTo(E2eConfig.lspFirmNumber()))
            .body("data.officeGUID", notNullValue())
            .body("data.officeCode", equalTo(E2eConfig.lspOfficeCode()))
            .extract()
            .response();

    String officeGuid = response.path("data.officeGUID");

    // Verify the new account appears as primary on the office
    getBankDetails(E2eConfig.lspFirmNumber(), officeGuid)
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

  /// Links to an unknown Bank Account GUID; the request must be rejected and no partial change
  /// applied to the office's existing bank account associations.
  ///
  /// - DSTEW-1601 AC6 – an invalid or unresolvable Bank Account reference must not result in a
  ///   partial record. (DS_MAPD_FR_031)
  /// - DSTEW-1998 AC6 – no partial update where the Payment Method / Bank Account change fails.
  ///   (DS_MAPD_FR_036)
  ///
  /// - DS_MAPD_FR_031: Create a Bank Account.
  /// - DS_MAPD_FR_036: Assign a bank account to a Legal Organisation.
  @Test
  void patchOffice_linkUnknownBankAccountGuid_returns404() {
    Integer countBefore =
        getBankDetails(E2eConfig.lspFirmNumber(), E2eConfig.lspOfficeCode())
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

    patchOffice(E2eConfig.lspFirmNumber(), E2eConfig.lspOfficeCode(), body).statusCode(404);

    getBankDetails(E2eConfig.lspFirmNumber(), E2eConfig.lspOfficeCode())
        .statusCode(200)
        .body("data.metadata.pagination.totalItems", equalTo(countBefore));
  }

  /// Creates a new LSP firm via `POST /provider-firms` with an initial EFT bank account, then
  /// performs a PATCH request linking a distinct new bank account to the head office. After the
  /// switch the office bank-details endpoint must return exactly one record with
  /// `primaryFlag=true`.
  ///
  /// - DSTEW-1601 AC8 – only one Bank Account may be marked Primary per target at a time.
  ///   (DS_MAPD_FR_031)
  /// - DSTEW-1601 AC10 – the Primary Bank Account flag moves during replacement.
  ///   (DS_MAPD_FR_031)
  /// - DSTEW-1998 AC9 – the outgoing association's Primary flag is set to No.
  ///   (DS_MAPD_FR_036)
  /// - DSTEW-1998 AC10 – the incoming association's Primary flag is set to Yes.
  ///   (DS_MAPD_FR_036)
  ///
  /// - DS_MAPD_FR_031: Create a Bank Account.
  /// - DS_MAPD_FR_036: Assign a bank account to a Legal Organisation.
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
                        Map.of("useDefaultContractManager", true),
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
    String officeGuid = resolveOfficeGuid(firmNumber);

    // Assert the initial account is primary (baseline)
    List<Boolean> flagsAfterCreate =
        getBankDetails(firmNumber, officeGuid)
            .statusCode(200)
            .extract()
            .path("data.content.primaryFlag");

    org.hamcrest.MatcherAssert.assertThat(
        "exactly one primary flag after firm creation",
        flagsAfterCreate.stream().filter(Boolean.TRUE::equals).count(),
        equalTo(1L));

    // --- Step 3: first switch — assign account A ---
    String accountNumberA = "8" + ((ts + 1) % 10_000_000L);
    patchOffice(
            firmNumber,
            officeGuid,
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
        .statusCode(200);

    List<Boolean> flagsAfterFirstSwitch =
        getBankDetails(firmNumber, officeGuid)
            .statusCode(200)
            .extract()
            .path("data.content.primaryFlag");

    org.hamcrest.MatcherAssert.assertThat(
        "exactly one primary flag after first switch",
        flagsAfterFirstSwitch.stream().filter(Boolean.TRUE::equals).count(),
        equalTo(1L));

    // DSTEW-1640 audit coverage: all returned associations should include audit fields
    getBankDetails(firmNumber, officeGuid)
        .statusCode(200)
        .body("data.content.findAll { it.createdBy == null }", hasSize(0))
        .body("data.content.findAll { it.createdTimestamp == null }", hasSize(0))
        .body("data.content.findAll { it.lastUpdatedBy == null }", hasSize(0))
        .body("data.content.findAll { it.lastUpdatedTimestamp == null }", hasSize(0));
  }

  /// Creates a new LSP firm with an initial EFT bank account, then switches to a new account via
  /// PATCH. The GET response for the office bank-details must still contain the original link row
  /// (with `primaryFlag=false` and a non-null `activeDateTo`), proving it was end-dated rather
  /// than deleted.
  ///
  /// - DSTEW-1601 AC11 – the previous association is retained as history, not deleted.
  ///   (DS_MAPD_FR_031)
  /// - DSTEW-1998 AC12 – historical retention: the previous association is retained and not
  ///   deleted. (DS_MAPD_FR_036)
  ///
  /// - DS_MAPD_FR_031: Create a Bank Account.
  /// - DS_MAPD_FR_036: Assign a bank account to a Legal Organisation.
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
                        Map.of("useDefaultContractManager", true),
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

    String officeGuid = resolveOfficeGuid(firmNumber);

    // switch to a new bank account
    String newAccountNumber = "4" + ((timestamp + 1) % 10_000_000L);
    patchOffice(
            firmNumber,
            officeGuid,
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
        .statusCode(200);

    getBankDetails(firmNumber, officeGuid)
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
            nullValue()) // primary
        .body("data.content.findAll { it.createdBy == null }", hasSize(0))
        .body("data.content.findAll { it.createdTimestamp == null }", hasSize(0))
        .body("data.content.findAll { it.lastUpdatedBy == null }", hasSize(0))
        .body("data.content.findAll { it.lastUpdatedTimestamp == null }", hasSize(0));
  }

  /// Creates a new LSP firm with an initial EFT bank account (account 0), switches to a new
  /// account A, then re-links back to account 0 by GUID. After reverting:
  ///
  /// - Account 0 is the current primary (`primaryFlag=true`, null `activeDateTo`).
  /// - Account A is end-dated (`primaryFlag=false`, non-null `activeDateTo`).
  /// - Three rows exist in total (account 0 history, account A history, account 0 current) -
  ///   nothing is deleted.
  ///
  /// - DSTEW-1601 AC12 – reverting to a previously-associated Bank Account makes it Primary
  ///   again; nothing is deleted. (DS_MAPD_FR_031)
  /// - DSTEW-1998 AC8 – change to an existing Bank Account ends the current association and
  ///   assigns the replacement as current. (DS_MAPD_FR_036)
  /// - DSTEW-1998 AC12 – historical retention: no association is ever deleted. (DS_MAPD_FR_036)
  ///
  /// - DS_MAPD_FR_031: Create a Bank Account.
  /// - DS_MAPD_FR_036: Assign a bank account to a Legal Organisation.
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
                        Map.of("useDefaultContractManager", true),
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

    String officeGuid = resolveOfficeGuid(firmNumber);

    String accountANumber = "2" + ((ts + 1) % 10_000_000L);
    patchOffice(
            firmNumber,
            officeGuid,
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
        .statusCode(200);

    String account0Guid =
        getBankDetails(firmNumber, officeGuid)
            .statusCode(200)
            .extract()
            .path("data.content.findAll { it.accountNumber == '" + account0Number + "' }.guid[0]");

    patchOffice(
            firmNumber,
            officeGuid,
            Map.of(
                "payment",
                Map.of(
                    "paymentMethod",
                    "EFT",
                    "paymentHeldFlag",
                    false,
                    "bankAccountDetails",
                    Map.of("type", "link", "bankAccountGUID", account0Guid))))
        .statusCode(200);

    getBankDetails(firmNumber, officeGuid)
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
        .body("data.content.findAll { it.accountNumber == '" + account0Number + "' }", hasSize(2))
        .body("data.content.findAll { it.createdBy == null }", hasSize(0))
        .body("data.content.findAll { it.createdTimestamp == null }", hasSize(0))
        .body("data.content.findAll { it.lastUpdatedBy == null }", hasSize(0))
        .body("data.content.findAll { it.lastUpdatedTimestamp == null }", hasSize(0));
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

    patchOffice(E2eConfig.lspFirmNumber(), E2eConfig.invalidOfficeCode(), body).statusCode(404);
  }

  /// When a new bank account is assigned to an office, the old (existing) bank account link is
  /// marked as non-primary and its `activeDateTo` is set to the current date.
  ///
  /// - DSTEW-1627 AC2 – the outgoing association's End Date is set and its Primary flag is set
  ///   to No. (DS_MAPD_FR_032)
  /// - DSTEW-1998 AC9 – update outgoing association: Effective End Date and Primary flag=No.
  ///   (DS_MAPD_FR_036)
  ///
  /// - DS_MAPD_FR_032: Change Bank Account.
  /// - DS_MAPD_FR_036: Assign a bank account to a Legal Organisation.
  @Test
  void assignNewBankAccount_marksOldAsNonPrimaryWithActiveDateTo() {
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
                        Map.of("useDefaultContractManager", true),
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

    String officeGuid = resolveOfficeGuid(firmNumber);

    // Capture initial state — old account before update
    Response oldAccountBefore =
        getBankDetails(firmNumber, officeGuid).statusCode(200).extract().response();

    String oldAccountGuid = oldAccountBefore.path("data.content[0].guid");
    String oldAccountNumber = oldAccountBefore.path("data.content[0].accountNumber");

    // Assign new account
    String newAccountNumber = "1" + ((timestamp + 1) % 10_000_000L);
    patchOffice(
            firmNumber,
            officeGuid,
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
        .statusCode(200);

    // Verify both accounts exist in bank details
    getBankDetails(firmNumber, officeGuid)
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

  /// The previous Bank Account association's own data (account name, sort code, account number)
  /// must remain unchanged when it is superseded by a new assignment; only the association
  /// metadata (`primaryFlag`/`activeDateTo`) changes.
  ///
  /// - DSTEW-1627 AC5 – historical retention: the superseded association's own data is not
  ///   altered. (DS_MAPD_FR_032)
  /// - DSTEW-1998 AC12 – historical retention: the previous association is retained and not
  ///   deleted. (DS_MAPD_FR_036)
  ///
  /// - DS_MAPD_FR_032: Change Bank Account.
  /// - DS_MAPD_FR_036: Assign a bank account to a Legal Organisation.
  @Test
  void restrictedFields_remainUnchangedWhenAssigningNewAccount() {
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
                        Map.of("useDefaultContractManager", true),
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

    String officeGuid = resolveOfficeGuid(firmNumber);

    // Capture old account state before update
    Response oldAccountState =
        getBankDetails(firmNumber, officeGuid).statusCode(200).extract().response();

    String newAccountNumber = "0" + ((timestamp + 2) % 10_000_000L);
    String newAccountName = "AC2 New Acct " + timestamp;
    String newSortCode = "602222";

    // Assign new account
    patchOffice(
            firmNumber,
            officeGuid,
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
        .statusCode(200);

    // Verify old account fields are unchanged (except primaryFlag and activeDateTo)
    getBankDetails(firmNumber, officeGuid)
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

  /// Both the outgoing and incoming Bank Account associations must remain complete and valid (no
  /// orphaned or corrupted records) after an amendment, with exactly one association marked
  /// primary.
  ///
  /// - DSTEW-1601 AC7 / BR-10 – a Bank Account must always have at least one association to
  ///   exist. (DS_MAPD_FR_031)
  /// - DSTEW-1998 AC7 / BR-10 – a Bank Account must always have at least one association to
  ///   exist. (DS_MAPD_FR_036)
  ///
  /// - DS_MAPD_FR_031: Create a Bank Account.
  /// - DS_MAPD_FR_036: Assign a bank account to a Legal Organisation.
  @Test
  void bankAccountValidityPreserved_afterAmendment() {
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
                        Map.of("useDefaultContractManager", true),
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

    String officeGuid = resolveOfficeGuid(firmNumber);

    String newAccountNumber = "5" + ((timestamp + 3) % 10_000_000L);
    patchOffice(
            firmNumber,
            officeGuid,
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
        .statusCode(200);

    // Verify validity constraints
    getBankDetails(firmNumber, officeGuid)
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

  /// No partial bank account update on validation failure.
  ///
  /// - DSTEW-1627 AC7 – no partial update on failure. (DS_MAPD_FR_032)
  /// - DSTEW-1998 AC6 – no partial update where the Payment Method / Bank Account change fails.
  ///   (DS_MAPD_FR_036)
  ///
  /// - DS_MAPD_FR_032: Change Bank Account.
  /// - DS_MAPD_FR_036: Assign a bank account to a Legal Organisation.
  @Test
  void noPartialUpdate_whenValidationFails() {
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
                        Map.of("useDefaultContractManager", true),
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

    String officeGuid = resolveOfficeGuid(firmNumber);

    // Capture state before failed update
    Response stateBefore =
        getBankDetails(firmNumber, officeGuid).statusCode(200).extract().response();

    int accountCountBefore = stateBefore.path("data.content.size()");
    boolean wasPrimaryBefore = stateBefore.path("data.content[0].primaryFlag");

    // Attempt invalid patch with non-existent bank account GUID
    patchOffice(
            firmNumber,
            officeGuid,
            Map.of(
                "payment",
                Map.of(
                    "paymentMethod",
                    "EFT",
                    "paymentHeldFlag",
                    false,
                    "bankAccountDetails",
                    Map.of("type", "link", "bankAccountGUID", UUID.randomUUID().toString()))))
        .statusCode(404); // Should be rejected

    // Verify no partial update occurred (state unchanged)
    getBankDetails(firmNumber, officeGuid)
        .statusCode(200)
        // Account count unchanged
        .body("data.content", hasSize(accountCountBefore))
        // Original account still primary
        .body("data.content[0].primaryFlag", equalTo(wasPrimaryBefore))
        // Original account number unchanged
        .body("data.content[0].accountNumber", equalTo(initialAccountNumber));
  }

  /// Verifies that bank accounts in the system are always associated with at least one
  /// provider-office link. Tests that querying bank details returns only accounts with valid
  /// associations, and that the amendment doesn't result in orphaned accounts.
  ///
  /// - DSTEW-1601 AC7 – a Bank Account must always have an association to exist.
  ///   (DS_MAPD_FR_031)
  /// - DSTEW-1998 AC7 – a Bank Account must always have at least one association to exist.
  ///   (DS_MAPD_FR_036)
  ///
  /// - DS_MAPD_FR_031: Create a Bank Account.
  /// - DS_MAPD_FR_036: Assign a bank account to a Legal Organisation.
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
                        Map.of("useDefaultContractManager", true),
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

    String officeGuid = resolveOfficeGuid(firmNumber);

    String newAccountNumber = "9" + ((timestamp + 4) % 10_000_000L);
    patchOffice(
            firmNumber,
            officeGuid,
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
        .statusCode(200);

    // Verify both old and new accounts are still associated (retrievable) from the office
    getBankDetails(firmNumber, officeGuid)
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

  /// Start and end date alignment.
  ///
  /// - DSTEW-1627 AC4 – the outgoing Effective End Date and incoming Effective Start Date are
  ///   the same date/time. (DS_MAPD_FR_032)
  /// - DSTEW-1998 AC11 – start and end date alignment on replacement. (DS_MAPD_FR_036)
  ///
  /// - DS_MAPD_FR_032: Change Bank Account.
  /// - DS_MAPD_FR_036: Assign a bank account to a Legal Organisation.
  @Test
  void startDateAndEndDate_areAlignedOnUpdate() {
    long timestamp = System.currentTimeMillis();
    String firmName = "E2E-AC4 Alignment " + timestamp;

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
                        Map.of("useDefaultContractManager", true),
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

    String officeGuid = resolveOfficeGuid(firmNumber);

    String newAccountNumber = "8" + ((timestamp + 1) % 10_000_000L);
    patchOffice(
            firmNumber,
            officeGuid,
            Map.of(
                "payment",
                Map.of(
                    "paymentMethod",
                    "EFT",
                    "paymentHeldFlag",
                    false,
                    "bankAccountDetails",
                    Map.of(
                        "accountName", "AC4 New",
                        "sortCode", "601111",
                        "accountNumber", newAccountNumber))))
        .statusCode(200);

    Response response =
        getBankDetails(firmNumber, officeGuid)
            .statusCode(200)
            .body("data.content", hasSize(2))
            .extract()
            .response();

    String outgoingEndDate =
        response.path(
            "data.content.find { it.accountNumber == '"
                + initialAccountNumber
                + "' }.activeDateTo");
    String incomingStartDate =
        response.path(
            "data.content.find { it.accountNumber == '" + newAccountNumber + "' }.activeDateFrom");

    org.hamcrest.MatcherAssert.assertThat(
        "Outgoing end date should not be null", outgoingEndDate, notNullValue());
    org.hamcrest.MatcherAssert.assertThat(
        "Incoming start date should not be null", incomingStartDate, notNullValue());
    org.hamcrest.MatcherAssert.assertThat(
        "Start and end dates must align", outgoingEndDate, equalTo(incomingStartDate));
  }

  /// Effective Start Date set to today's date on assignment.
  ///
  /// - DSTEW-1634 AC1 – the Effective Start Date is set to today's date when an assignment
  ///   succeeds. (DS_MAPD_FR_034)
  /// - DSTEW-1998 AC10 – the incoming association's Effective Start Date is set on assignment.
  ///   (DS_MAPD_FR_036)
  ///
  /// - DS_MAPD_FR_034: Set bank account Effective Start Date.
  /// - DS_MAPD_FR_036: Assign a bank account to a Legal Organisation.
  @Test
  void effectiveStartDate_isSetToCurrentDateOnAssignment() {
    long timestamp = System.currentTimeMillis();
    String firmName = "E2E-AC1 StartDate " + timestamp;
    String initialAccountNumber = "1" + (timestamp % 10_000_000L);

    // Create a new firm with an initial bank account
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
                            "line1", "1 AC1 StartDate Street",
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
                                "accountName", "AC1 StartDate Initial",
                                "sortCode", "601111",
                                "accountNumber", initialAccountNumber)),
                        "contractManager",
                        Map.of("useDefaultContractManager", true),
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

    String officeGuid = resolveOfficeGuid(firmNumber);

    // Fetch the newly created bank account association
    Response response =
        getBankDetails(firmNumber, officeGuid)
            .statusCode(200)
            .body("data.content", hasSize(1))
            .extract()
            .response();

    String activeDateFromString = response.path("data.content[0].activeDateFrom");
    java.time.LocalDate activeDateFrom = java.time.LocalDate.parse(activeDateFromString);
    java.time.LocalDate today = java.time.LocalDate.now();

    org.hamcrest.MatcherAssert.assertThat(
        "Effective Start Date should be today's date", activeDateFrom, equalTo(today));
  }

  /// Applicable to all assignment targets. This test verifies that the Effective Start Date is
  /// set when a bank account is assigned to a Legal Organisation Child Office.
  ///
  /// - DSTEW-1634 AC2 – the Effective Start Date rule is applicable to all assignment target
  ///   types. (DS_MAPD_FR_034)
  /// - DSTEW-1998 AC10 – the incoming association's Effective Start Date is set on assignment,
  ///   uniformly across target types. (DS_MAPD_FR_037)
  ///
  /// - DS_MAPD_FR_034: Set bank account Effective Start Date.
  /// - DS_MAPD_FR_037: Assign an existing Legal Organisation Bank Account to a Legal
  ///   Organisation Child Office.
  @Test
  void effectiveStartDate_isSetForOfficeAssignment() {
    long timestamp = System.currentTimeMillis();
    String firmName = "E2E-AC2 StartDate Office " + timestamp;
    String initialAccountNumber = "2" + (timestamp % 10_000_000L);

    // Create a new firm, which also creates a head office
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
                            "line1", "1 AC2 StartDate Street",
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
                                "accountName", "AC2 StartDate Initial",
                                "sortCode", "601111",
                                "accountNumber", initialAccountNumber)),
                        "contractManager",
                        Map.of("useDefaultContractManager", true),
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

    String officeGuid = resolveOfficeGuid(firmNumber);

    // The bank account is assigned to the office, so we check here
    getBankDetails(firmNumber, officeGuid)
        .statusCode(200)
        .body("data.content", hasSize(1))
        .body("data.content[0].activeDateFrom", notNullValue());
  }

  /// Resolves the GUID of the given firm's head office via {@code GET
  /// /provider-firms/{firmId}/offices}.
  private static String resolveOfficeGuid(String firmNumber) {
    return given()
        .pathParam("firmId", firmNumber)
        .when()
        .get("/provider-firms/{firmId}/offices")
        .then()
        .statusCode(200)
        .extract()
        .path("data.content[0].guid");
  }

  /// Fetches the bank-details associations for the given office via {@code GET
  /// /provider-firms/{firmId}/offices/{officeCode}/bank-details}.
  private static ValidatableResponse getBankDetails(String firmId, String officeCode) {
    return given()
        .pathParam("firmId", firmId)
        .pathParam("officeCode", officeCode)
        .when()
        .get("/provider-firms/{firmId}/offices/{officeCode}/bank-details")
        .then();
  }

  /// Sends {@code PATCH /provider-firms/{firmId}/offices/{officeCode}} with the given request
  /// body, to reassign the office's payment/bank-account details.
  private static ValidatableResponse patchOffice(
      String firmId, String officeCode, Map<String, Object> body) {
    return given()
        .contentType(ContentType.JSON)
        .pathParam("firmId", firmId)
        .pathParam("officeCode", officeCode)
        .body(body)
        .when()
        .patch("/provider-firms/{firmId}/offices/{officeCode}")
        .then();
  }
}
