package uk.gov.justice.laa.providerdata.e2e.modifying;

import static io.restassured.RestAssured.given;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

import io.restassured.http.ContentType;
import io.restassured.response.Response;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import uk.gov.justice.laa.providerdata.e2e.ModifyingTest;

/// Data-modifying E2E tests for DSTEW-1998: Manage Payment Method and Bank Account Association
/// (DS_MAPD_FR_031/036/037/038).
///
/// {@link PatchOfficeBankAccountE2eTest} already exercises the LSP head office extensively (under
/// DSTEW-1601/DSTEW-1627/DSTEW-1634 naming) for AC6-AC12, since those tickets cover
/// bank-account assignment/replacement mechanics once a target is already on `paymentMethod=EFT`.
/// It does not, however, exercise the `paymentMethod` transition ACs (AC1-AC5) for the head
/// office - none of its tests start from `paymentMethod=CHECK` or omit `bankAccountDetails` on an
/// EFT request. This file fills that gap for the head office, and also covers every AC for the
/// LSP **child office** and **Legal Practitioner** (an `Advocate`-firmType office link) target
/// entity types, since {@link uk.gov.justice.laa.providerdata.service.OfficeService} and
/// {@link uk.gov.justice.laa.providerdata.service.BankDetailsService} apply identical logic
/// regardless of which `ProviderOfficeLinkEntity` subtype is patched.
@ModifyingTest
class PaymentMethodBankAccountAssociationE2eTest {

  /// Submitting `paymentMethod=EFT` without `bankAccountDetails`.
  ///
  /// - DSTEW-1998 AC1 – EFT requires a valid active Bank Account association; rejected if none
  ///   is provided. (DS_MAPD_FR_036)
  /// - DSTEW-1998 AC5 – the required Payment Method and Bank Account changes must be provided
  ///   within the same request; PDP rejects the request if incomplete. (DS_MAPD_FR_036)
  ///
  /// - DS_MAPD_FR_036: Assign a bank account to a Legal Organisation.
  @Test
  void dstew1998_ac1and5_headOffice_eftWithoutBankAccountDetails_returns400AndUnchanged() {
    HeadOfficeFixture office = createHeadOfficeFirm("AC1", "CHECK", null);

    given()
        .contentType(ContentType.JSON)
        .pathParam("firmId", office.firmNumber())
        .pathParam("officeCode", office.officeGuid())
        .body(Map.of("payment", Map.of("paymentMethod", "EFT", "paymentHeldFlag", false)))
        .when()
        .patch("/provider-firms/{firmId}/offices/{officeCode}")
        .then()
        .statusCode(400);

    given()
        .pathParam("firmId", office.firmNumber())
        .pathParam("officeCode", office.officeGuid())
        .when()
        .get("/provider-firms/{firmId}/offices/{officeCode}/bank-details")
        .then()
        .statusCode(200)
        .body("data.content", hasSize(0));
  }

  /// Submitting `paymentMethod=EFT` without `bankAccountDetails`.
  ///
  /// - DSTEW-1998 AC1 – EFT requires a valid active Bank Account association; rejected if none
  ///   is provided. (DS_MAPD_FR_037)
  /// - DSTEW-1998 AC5 – the required Payment Method and Bank Account changes must be provided
  ///   within the same request; PDP rejects the request if incomplete. (DS_MAPD_FR_037)
  ///
  /// - DS_MAPD_FR_037: Assign an existing Legal Organisation Bank Account to a Legal
  ///   Organisation Child Office.
  @Test
  void dstew1998_ac1and5_childOffice_eftWithoutBankAccountDetails_returns400AndUnchanged() {
    ChildOfficeFixture office = createChildOffice("AC1", "CHECK", null);

    given()
        .contentType(ContentType.JSON)
        .pathParam("firmId", office.firmNumber())
        .pathParam("officeCode", office.officeGuid())
        .body(Map.of("payment", Map.of("paymentMethod", "EFT", "paymentHeldFlag", false)))
        .when()
        .patch("/provider-firms/{firmId}/offices/{officeCode}")
        .then()
        .statusCode(400);

    given()
        .pathParam("firmId", office.firmNumber())
        .pathParam("officeCode", office.officeGuid())
        .when()
        .get("/provider-firms/{firmId}/offices/{officeCode}/bank-details")
        .then()
        .statusCode(200)
        .body("data.content", hasSize(0));
  }

  /// Submitting `paymentMethod=EFT` without `bankAccountDetails`.
  ///
  /// - DSTEW-1998 AC1 – EFT requires a valid active Bank Account association; rejected if none
  ///   is provided. (DS_MAPD_FR_038)
  /// - DSTEW-1998 AC5 – the required Payment Method and Bank Account changes must be provided
  ///   within the same request; PDP rejects the request if incomplete. (DS_MAPD_FR_038)
  ///
  /// - DS_MAPD_FR_038: Assign a bank account to a Legal Practitioner.
  @Test
  void dstew1998_ac1and5_practitioner_eftWithoutBankAccountDetails_returns400AndUnchanged() {
    PractitionerFixture practitioner = createChambersAndPractitioner("AC1");

    given()
        .contentType(ContentType.JSON)
        .pathParam("firmId", practitioner.firmNumber())
        .pathParam("officeCode", practitioner.officeCode())
        .body(Map.of("payment", Map.of("paymentMethod", "EFT", "paymentHeldFlag", false)))
        .when()
        .patch("/provider-firms/{firmId}/offices/{officeCode}")
        .then()
        .statusCode(400);

    given()
        .pathParam("firmId", practitioner.firmNumber())
        .pathParam("officeCode", practitioner.officeCode())
        .when()
        .get("/provider-firms/{firmId}/offices/{officeCode}/bank-details")
        .then()
        .statusCode(200)
        .body("data.content", hasSize(0));
  }

  /// Creating a Legal Services Provider (LSP head office) with `paymentMethod=EFT` and no
  /// `bankAccountDetails`.
  ///
  /// - DSTEW-1998 AC1 – EFT requires a valid active Bank Account association at creation time,
  ///   not just on amendment; rejected if none is provided. (DS_MAPD_FR_036)
  ///
  /// - DS_MAPD_FR_036: Assign a bank account to a Legal Organisation.
  @Test
  void dstew1998_ac1_headOffice_creation_eftWithoutBankAccountDetails_returns400AndNotPersisted() {
    long ts = System.currentTimeMillis();
    String firmName = "E2E-DSTEW-1998 AC1 Creation Head Office " + ts;

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
                        "line1", "1 AC1 Creation Street",
                        "townOrCity", "London",
                        "postcode", "EC1A 1BB"),
                    "payment",
                    Map.of("paymentMethod", "EFT"),
                    "contractManager",
                    Map.of("useDefaultContractManager", true),
                    "liaisonManager",
                    Map.of(
                        "firstName", "AC1",
                        "lastName", "Creation LM",
                        "emailAddress", "ac1.creation.lm." + ts + "@example.com",
                        "telephoneNumber", "020 1111 2222"))))
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

  /// Creating a Legal Organisation Child Office with `paymentMethod=EFT` and no
  /// `bankAccountDetails`.
  ///
  /// - DSTEW-1998 AC1 – EFT requires a valid active Bank Account association at creation time,
  ///   not just on amendment; rejected if none is provided. (DS_MAPD_FR_037)
  ///
  /// - DS_MAPD_FR_037: Assign an existing Legal Organisation Bank Account to a Legal
  ///   Organisation Child Office.
  @Test
  void dstew1998_ac1_childOffice_creation_eftWithoutBankAccountDetails_returns400AndNotPersisted() {
    HeadOfficeFixture headOffice = createHeadOfficeFirm("AC1Creation", "CHECK", null);

    given()
        .contentType(ContentType.JSON)
        .pathParam("firmId", headOffice.firmNumber())
        .body(
            Map.of(
                "address",
                Map.of(
                    "line1", "1 AC1 Creation Child Office Street",
                    "townOrCity", "Bristol",
                    "postcode", "BS1 1AA"),
                "payment",
                Map.of("paymentMethod", "EFT"),
                "contractManager",
                Map.of("useHeadOfficeContractManager", true),
                "liaisonManager",
                Map.of(
                    "firstName", "AC1",
                    "lastName", "Creation Child LM",
                    "emailAddress",
                        "ac1.creation.child.lm." + System.currentTimeMillis() + "@example.com",
                    "telephoneNumber", "0117 1111 2222")))
        .when()
        .post("/provider-firms/{firmId}/offices")
        .then()
        .statusCode(400);

    given()
        .pathParam("firmId", headOffice.firmNumber())
        .when()
        .get("/provider-firms/{firmId}/offices")
        .then()
        .statusCode(200)
        .body("data.content", hasSize(1));
  }

  /// Creating a Legal Practitioner (Advocate) with `paymentMethod=EFT` and no
  /// `bankAccountDetails`.
  ///
  /// - DSTEW-1998 AC1 – EFT requires a valid active Bank Account association at creation time,
  ///   not just on amendment; rejected if none is provided. (DS_MAPD_FR_038)
  ///
  /// - DS_MAPD_FR_038: Assign a bank account to a Legal Practitioner.
  @Test
  void
      dstew1998_ac1_practitioner_creation_eftWithoutBankAccountDetails_returns400AndNotPersisted() {
    long ts = System.currentTimeMillis();
    String chambersFirmNumber =
        given()
            .contentType(ContentType.JSON)
            .body(
                Map.of(
                    "firmType",
                    "Chambers",
                    "name",
                    "E2E-DSTEW-1998 Chambers AC1Creation " + ts,
                    "chambers",
                    Map.of(
                        "address",
                        Map.of(
                            "line1", "1 AC1 Creation Chambers Street",
                            "townOrCity", "London",
                            "postcode", "WC1A 1AA"),
                        "liaisonManager",
                        Map.of(
                            "firstName",
                            "AC1Creation",
                            "lastName",
                            "Chambers LM",
                            "emailAddress",
                            "ac1.creation.chambers.lm." + ts + "@example.com",
                            "telephoneNumber",
                            "020 7000 1234"))))
            .when()
            .post("/provider-firms")
            .then()
            .statusCode(201)
            .extract()
            .path("data.providerFirmNumber");

    String practitionerName = "E2E-DSTEW-1998 Practitioner AC1Creation " + ts;

    given()
        .contentType(ContentType.JSON)
        .body(
            Map.of(
                "firmType",
                "Advocate",
                "name",
                practitionerName,
                "practitioner",
                Map.of(
                    "parentFirms",
                    List.of(Map.of("parentFirmNumber", chambersFirmNumber)),
                    "advocateType",
                    "Advocate",
                    "advocate",
                    Map.of(
                        "advocateLevel",
                        "Junior",
                        "solicitorRegulationAuthorityRollNumber",
                        "SRA-AC1Creation-" + ts),
                    "liaisonManager",
                    Map.of(
                        "firstName",
                        "AC1Creation",
                        "lastName",
                        "Practitioner LM",
                        "emailAddress",
                        "ac1.creation.practitioner.lm." + ts + "@example.com",
                        "telephoneNumber",
                        "020 2222 8888"),
                    "payment",
                    Map.of("paymentMethod", "EFT"))))
        .when()
        .post("/provider-firms")
        .then()
        .statusCode(400);

    given()
        .queryParam("name", practitionerName)
        .when()
        .get("/provider-firms")
        .then()
        .statusCode(200)
        .body("data.metadata.pagination.totalItems", equalTo(0));
  }

  /// Changing `paymentMethod` from `EFT` to `CHECK`.
  ///
  /// - DSTEW-1998 AC2 – ends the active Bank Account association and clears its primary flag.
  ///   (DS_MAPD_FR_036)
  ///
  /// - DS_MAPD_FR_036: Assign a bank account to a Legal Organisation.
  @Test
  void dstew1998_ac2_headOffice_eftToCheque_endsAssociationAndClearsPrimary() {
    long ts = System.currentTimeMillis();
    String accountNumber = "1" + ((ts + 5) % 10_000_000L);
    HeadOfficeFixture office = createHeadOfficeFirm("AC2", "EFT", accountNumber);

    patchOfficePayment(office.firmNumber(), office.officeGuid(), Map.of("paymentMethod", "CHECK"));

    given()
        .pathParam("firmId", office.firmNumber())
        .pathParam("officeCode", office.officeGuid())
        .when()
        .get("/provider-firms/{firmId}/offices/{officeCode}/bank-details")
        .then()
        .statusCode(200)
        .body("data.content", hasSize(1))
        .body("data.content[0].primaryFlag", equalTo(false))
        .body("data.content[0].activeDateTo", notNullValue());
  }

  /// Changing `paymentMethod` from `EFT` to `CHECK`.
  ///
  /// - DSTEW-1998 AC2 – ends the active Bank Account association and clears its primary flag.
  ///   (DS_MAPD_FR_037)
  ///
  /// - DS_MAPD_FR_037: Assign an existing Legal Organisation Bank Account to a Legal
  ///   Organisation Child Office.
  @Test
  void dstew1998_ac2_childOffice_eftToCheque_endsAssociationAndClearsPrimary() {
    long ts = System.currentTimeMillis();
    String accountNumber = "1" + (ts % 10_000_000L);
    ChildOfficeFixture office = createChildOffice("AC2", "EFT", accountNumber);

    patchOfficePayment(office.firmNumber(), office.officeGuid(), Map.of("paymentMethod", "CHECK"));

    given()
        .pathParam("firmId", office.firmNumber())
        .pathParam("officeCode", office.officeGuid())
        .when()
        .get("/provider-firms/{firmId}/offices/{officeCode}/bank-details")
        .then()
        .statusCode(200)
        .body("data.content", hasSize(1))
        .body("data.content[0].primaryFlag", equalTo(false))
        .body("data.content[0].activeDateTo", notNullValue());
  }

  /// Changing `paymentMethod` from `EFT` to `CHECK`.
  ///
  /// - DSTEW-1998 AC2 – ends the active Bank Account association and clears its primary flag.
  ///   (DS_MAPD_FR_038)
  ///
  /// - DS_MAPD_FR_038: Assign a bank account to a Legal Practitioner.
  @Test
  void dstew1998_ac2_practitioner_eftToCheque_endsAssociationAndClearsPrimary() {
    long ts = System.currentTimeMillis();
    String accountNumber = "2" + (ts % 10_000_000L);
    PractitionerFixture practitioner = createChambersAndPractitioner("AC2");

    patchOfficePayment(
        practitioner.firmNumber(),
        practitioner.officeCode(),
        Map.of(
            "paymentMethod",
            "EFT",
            "bankAccountDetails",
            Map.of(
                "accountName",
                "AC2 Practitioner",
                "sortCode",
                "601111",
                "accountNumber",
                accountNumber)));

    patchOfficePayment(
        practitioner.firmNumber(), practitioner.officeCode(), Map.of("paymentMethod", "CHECK"));

    given()
        .pathParam("firmId", practitioner.firmNumber())
        .pathParam("officeCode", practitioner.officeCode())
        .when()
        .get("/provider-firms/{firmId}/offices/{officeCode}/bank-details")
        .then()
        .statusCode(200)
        .body("data.content", hasSize(1))
        .body("data.content[0].primaryFlag", equalTo(false))
        .body("data.content[0].activeDateTo", notNullValue());
  }

  /// Changing `paymentMethod` from `CHECK` to `EFT` using an existing Bank Account. "Existing
  /// Bank Account" is interpreted as one already known to PDP for this target (i.e. a
  /// previously-used account still in its history), matching how
  /// {@link PatchOfficeBankAccountE2eTest} resolves `existingBankAccountGuid` against the same
  /// firm's own bank-details rather than an arbitrary other provider's account.
  ///
  /// - DSTEW-1998 AC3 – assigns the existing Bank Account as the new primary association.
  ///   (DS_MAPD_FR_036)
  ///
  /// - DS_MAPD_FR_036: Assign a bank account to a Legal Organisation.
  @Test
  void dstew1998_ac3_headOffice_chequeToEft_existingBankAccount_assignsAsPrimary() {
    long ts = System.currentTimeMillis();
    String accountNumber = "9" + ((ts + 5) % 10_000_000L);
    HeadOfficeFixture office = createHeadOfficeFirm("AC3", "EFT", accountNumber);
    String bankAccountGuid = lookUpPrimaryBankAccountGuid(office.firmNumber(), office.officeGuid());

    patchOfficePayment(office.firmNumber(), office.officeGuid(), Map.of("paymentMethod", "CHECK"));
    patchOfficePayment(
        office.firmNumber(),
        office.officeGuid(),
        Map.of(
            "paymentMethod",
            "EFT",
            "bankAccountDetails",
            Map.of("type", "link", "bankAccountGUID", bankAccountGuid)));

    given()
        .pathParam("firmId", office.firmNumber())
        .pathParam("officeCode", office.officeGuid())
        .when()
        .get("/provider-firms/{firmId}/offices/{officeCode}/bank-details")
        .then()
        .statusCode(200)
        .body("data.content.find { it.primaryFlag == true }.guid", equalTo(bankAccountGuid))
        .body("data.content.find { it.primaryFlag == true }.activeDateTo", nullValue());
  }

  /// Changing `paymentMethod` from `CHECK` to `EFT` using an existing Bank Account (see
  /// interpretation note above).
  ///
  /// - DSTEW-1998 AC3 – assigns the existing Bank Account as the new primary association.
  ///   (DS_MAPD_FR_037)
  ///
  /// - DS_MAPD_FR_037: Assign an existing Legal Organisation Bank Account to a Legal
  ///   Organisation Child Office.
  @Test
  void dstew1998_ac3_childOffice_chequeToEft_existingBankAccount_assignsAsPrimary() {
    long ts = System.currentTimeMillis();
    String accountNumber = "9" + (ts % 10_000_000L);
    ChildOfficeFixture office = createChildOffice("AC3", "EFT", accountNumber);
    String bankAccountGuid = lookUpPrimaryBankAccountGuid(office.firmNumber(), office.officeGuid());

    patchOfficePayment(office.firmNumber(), office.officeGuid(), Map.of("paymentMethod", "CHECK"));
    patchOfficePayment(
        office.firmNumber(),
        office.officeGuid(),
        Map.of(
            "paymentMethod",
            "EFT",
            "bankAccountDetails",
            Map.of("type", "link", "bankAccountGUID", bankAccountGuid)));

    given()
        .pathParam("firmId", office.firmNumber())
        .pathParam("officeCode", office.officeGuid())
        .when()
        .get("/provider-firms/{firmId}/offices/{officeCode}/bank-details")
        .then()
        .statusCode(200)
        .body("data.content.find { it.primaryFlag == true }.guid", equalTo(bankAccountGuid))
        .body("data.content.find { it.primaryFlag == true }.activeDateTo", nullValue());
  }

  /// Changing `paymentMethod` from `CHECK` to `EFT` using an existing Bank Account (see
  /// interpretation note above).
  ///
  /// - DSTEW-1998 AC3 – assigns the existing Bank Account as the new primary association.
  ///   (DS_MAPD_FR_038)
  ///
  /// - DS_MAPD_FR_038: Assign a bank account to a Legal Practitioner.
  @Test
  void dstew1998_ac3_practitioner_chequeToEft_existingBankAccount_assignsAsPrimary() {
    long ts = System.currentTimeMillis();
    String accountNumber = "1" + ((ts + 3) % 10_000_000L);
    PractitionerFixture practitioner = createChambersAndPractitioner("AC3");
    patchOfficePayment(
        practitioner.firmNumber(),
        practitioner.officeCode(),
        Map.of(
            "paymentMethod",
            "EFT",
            "bankAccountDetails",
            Map.of(
                "accountName",
                "AC3 Practitioner",
                "sortCode",
                "601111",
                "accountNumber",
                accountNumber)));
    String bankAccountGuid =
        lookUpPrimaryBankAccountGuid(practitioner.firmNumber(), practitioner.officeCode());

    patchOfficePayment(
        practitioner.firmNumber(), practitioner.officeCode(), Map.of("paymentMethod", "CHECK"));
    patchOfficePayment(
        practitioner.firmNumber(),
        practitioner.officeCode(),
        Map.of(
            "paymentMethod",
            "EFT",
            "bankAccountDetails",
            Map.of("type", "link", "bankAccountGUID", bankAccountGuid)));

    given()
        .pathParam("firmId", practitioner.firmNumber())
        .pathParam("officeCode", practitioner.officeCode())
        .when()
        .get("/provider-firms/{firmId}/offices/{officeCode}/bank-details")
        .then()
        .statusCode(200)
        .body("data.content.find { it.primaryFlag == true }.guid", equalTo(bankAccountGuid))
        .body("data.content.find { it.primaryFlag == true }.activeDateTo", nullValue());
  }

  /// Changing `paymentMethod` from `CHECK` to `EFT` using a new (previously unknown) Bank
  /// Account.
  ///
  /// - DSTEW-1998 AC4 – creates the Bank Account and assigns it as the new primary association.
  ///   (DS_MAPD_FR_036)
  ///
  /// - DS_MAPD_FR_036: Assign a bank account to a Legal Organisation.
  @Test
  void dstew1998_ac4_headOffice_chequeToEft_newBankAccount_createsAndAssignsAsPrimary() {
    long ts = System.currentTimeMillis();
    String accountNumber = "3" + ((ts + 5) % 10_000_000L);
    HeadOfficeFixture office = createHeadOfficeFirm("AC4", "CHECK", null);

    patchOfficePayment(
        office.firmNumber(),
        office.officeGuid(),
        Map.of(
            "paymentMethod",
            "EFT",
            "bankAccountDetails",
            Map.of(
                "accountName",
                "AC4 Head New",
                "sortCode",
                "601111",
                "accountNumber",
                accountNumber)));

    given()
        .pathParam("firmId", office.firmNumber())
        .pathParam("officeCode", office.officeGuid())
        .when()
        .get("/provider-firms/{firmId}/offices/{officeCode}/bank-details")
        .then()
        .statusCode(200)
        .body("data.content", hasSize(1))
        .body("data.content[0].accountNumber", equalTo(accountNumber))
        .body("data.content[0].primaryFlag", equalTo(true))
        .body("data.content[0].activeDateTo", nullValue());
  }

  /// Changing `paymentMethod` from `CHECK` to `EFT` using a new (previously unknown) Bank
  /// Account.
  ///
  /// - DSTEW-1998 AC4 – creates the Bank Account and assigns it as the new primary association.
  ///   (DS_MAPD_FR_037)
  ///
  /// - DS_MAPD_FR_037: Assign an existing Legal Organisation Bank Account to a Legal
  ///   Organisation Child Office.
  @Test
  void dstew1998_ac4_childOffice_chequeToEft_newBankAccount_createsAndAssignsAsPrimary() {
    long ts = System.currentTimeMillis();
    String accountNumber = "3" + (ts % 10_000_000L);
    ChildOfficeFixture office = createChildOffice("AC4", "CHECK", null);

    patchOfficePayment(
        office.firmNumber(),
        office.officeGuid(),
        Map.of(
            "paymentMethod",
            "EFT",
            "bankAccountDetails",
            Map.of(
                "accountName", "AC4 New", "sortCode", "601111", "accountNumber", accountNumber)));

    given()
        .pathParam("firmId", office.firmNumber())
        .pathParam("officeCode", office.officeGuid())
        .when()
        .get("/provider-firms/{firmId}/offices/{officeCode}/bank-details")
        .then()
        .statusCode(200)
        .body("data.content", hasSize(1))
        .body("data.content[0].accountNumber", equalTo(accountNumber))
        .body("data.content[0].primaryFlag", equalTo(true))
        .body("data.content[0].activeDateTo", nullValue());
  }

  /// Changing `paymentMethod` from `CHECK` to `EFT` using a new (previously unknown) Bank
  /// Account.
  ///
  /// - DSTEW-1998 AC4 – creates the Bank Account and assigns it as the new primary association.
  ///   (DS_MAPD_FR_038)
  ///
  /// - DS_MAPD_FR_038: Assign a bank account to a Legal Practitioner.
  @Test
  void dstew1998_ac4_practitioner_chequeToEft_newBankAccount_createsAndAssignsAsPrimary() {
    long ts = System.currentTimeMillis();
    String accountNumber = "4" + (ts % 10_000_000L);
    PractitionerFixture practitioner = createChambersAndPractitioner("AC4");

    patchOfficePayment(
        practitioner.firmNumber(),
        practitioner.officeCode(),
        Map.of(
            "paymentMethod",
            "EFT",
            "bankAccountDetails",
            Map.of(
                "accountName",
                "AC4 Practitioner New",
                "sortCode",
                "601111",
                "accountNumber",
                accountNumber)));

    given()
        .pathParam("firmId", practitioner.firmNumber())
        .pathParam("officeCode", practitioner.officeCode())
        .when()
        .get("/provider-firms/{firmId}/offices/{officeCode}/bank-details")
        .then()
        .statusCode(200)
        .body("data.content", hasSize(1))
        .body("data.content[0].accountNumber", equalTo(accountNumber))
        .body("data.content[0].primaryFlag", equalTo(true))
        .body("data.content[0].activeDateTo", nullValue());
  }

  /// Patching with an unknown `bankAccountGUID` link.
  ///
  /// - DSTEW-1998 AC6 – rejected and leaves the existing Bank Account association unchanged (no
  ///   partial update). (DS_MAPD_FR_037)
  ///
  /// - DS_MAPD_FR_037: Assign an existing Legal Organisation Bank Account to a Legal
  ///   Organisation Child Office.
  @Test
  void dstew1998_ac6_childOffice_unknownBankAccountGuid_returns404AndNoPartialUpdate() {
    long ts = System.currentTimeMillis();
    String accountNumber = "5" + (ts % 10_000_000L);
    ChildOfficeFixture office = createChildOffice("AC6", "EFT", accountNumber);

    given()
        .contentType(ContentType.JSON)
        .pathParam("firmId", office.firmNumber())
        .pathParam("officeCode", office.officeGuid())
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
        .statusCode(404);

    given()
        .pathParam("firmId", office.firmNumber())
        .pathParam("officeCode", office.officeGuid())
        .when()
        .get("/provider-firms/{firmId}/offices/{officeCode}/bank-details")
        .then()
        .statusCode(200)
        .body("data.content", hasSize(1))
        .body("data.content[0].accountNumber", equalTo(accountNumber))
        .body("data.content[0].primaryFlag", equalTo(true));
  }

  /// Patching with an unknown `bankAccountGUID` link.
  ///
  /// - DSTEW-1998 AC6 – rejected and leaves the existing Bank Account association unchanged (no
  ///   partial update). (DS_MAPD_FR_038)
  ///
  /// - DS_MAPD_FR_038: Assign a bank account to a Legal Practitioner.
  @Test
  void dstew1998_ac6_practitioner_unknownBankAccountGuid_returns404AndNoPartialUpdate() {
    long ts = System.currentTimeMillis();
    String accountNumber = "6" + (ts % 10_000_000L);
    PractitionerFixture practitioner = createChambersAndPractitioner("AC6");

    patchOfficePayment(
        practitioner.firmNumber(),
        practitioner.officeCode(),
        Map.of(
            "paymentMethod",
            "EFT",
            "bankAccountDetails",
            Map.of(
                "accountName",
                "AC6 Practitioner",
                "sortCode",
                "601111",
                "accountNumber",
                accountNumber)));

    given()
        .contentType(ContentType.JSON)
        .pathParam("firmId", practitioner.firmNumber())
        .pathParam("officeCode", practitioner.officeCode())
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
        .statusCode(404);

    given()
        .pathParam("firmId", practitioner.firmNumber())
        .pathParam("officeCode", practitioner.officeCode())
        .when()
        .get("/provider-firms/{firmId}/offices/{officeCode}/bank-details")
        .then()
        .statusCode(200)
        .body("data.content", hasSize(1))
        .body("data.content[0].accountNumber", equalTo(accountNumber))
        .body("data.content[0].primaryFlag", equalTo(true));
  }

  /// Switching the primary Bank Account to a different one.
  ///
  /// - DSTEW-1998 AC7 – both the outgoing and incoming Bank Accounts remain associated with the
  ///   target; neither is deleted. (DS_MAPD_FR_037)
  ///
  /// - DS_MAPD_FR_037: Assign an existing Legal Organisation Bank Account to a Legal
  ///   Organisation Child Office.
  ///
  /// AC7's stricter claim that a Bank Account with zero associations "must not exist in PDP" is
  /// an internal invariant, not directly observable via the API (there is no way to query a
  /// Bank Account independently of its associations), so it is not asserted here.
  @Test
  void dstew1998_ac7_childOffice_bothAccountsRemainAssociatedAfterSwitch() {
    long ts = System.currentTimeMillis();
    String initialAccountNumber = "7" + (ts % 10_000_000L);
    String newAccountNumber = "8" + ((ts + 1) % 10_000_000L);
    ChildOfficeFixture office = createChildOffice("AC7", "EFT", initialAccountNumber);

    patchOfficePayment(
        office.firmNumber(),
        office.officeGuid(),
        Map.of(
            "paymentMethod",
            "EFT",
            "bankAccountDetails",
            Map.of(
                "accountName",
                "AC7 New",
                "sortCode",
                "601111",
                "accountNumber",
                newAccountNumber)));

    given()
        .pathParam("firmId", office.firmNumber())
        .pathParam("officeCode", office.officeGuid())
        .when()
        .get("/provider-firms/{firmId}/offices/{officeCode}/bank-details")
        .then()
        .statusCode(200)
        .body("data.content", hasSize(2))
        .body(
            "data.content.findAll { it.accountNumber == '" + initialAccountNumber + "' }",
            hasSize(1))
        .body(
            "data.content.findAll { it.accountNumber == '" + newAccountNumber + "' }", hasSize(1));
  }

  /// Switching the primary Bank Account to a different one.
  ///
  /// - DSTEW-1998 AC7 – both the outgoing and incoming Bank Accounts remain associated with the
  ///   target; neither is deleted. (DS_MAPD_FR_038)
  ///
  /// - DS_MAPD_FR_038: Assign a bank account to a Legal Practitioner.
  ///
  /// AC7's stricter claim that a Bank Account with zero associations "must not exist in PDP" is
  /// an internal invariant, not directly observable via the API (there is no way to query a
  /// Bank Account independently of its associations), so it is not asserted here.
  @Test
  void dstew1998_ac7_practitioner_bothAccountsRemainAssociatedAfterSwitch() {
    long ts = System.currentTimeMillis();
    String initialAccountNumber = "9" + (ts % 10_000_000L);
    String newAccountNumber = "1" + ((ts + 11) % 10_000_000L);
    PractitionerFixture practitioner = createChambersAndPractitioner("AC7");

    patchOfficePayment(
        practitioner.firmNumber(),
        practitioner.officeCode(),
        Map.of(
            "paymentMethod",
            "EFT",
            "bankAccountDetails",
            Map.of(
                "accountName",
                "AC7 Initial",
                "sortCode",
                "601111",
                "accountNumber",
                initialAccountNumber)));
    patchOfficePayment(
        practitioner.firmNumber(),
        practitioner.officeCode(),
        Map.of(
            "paymentMethod",
            "EFT",
            "bankAccountDetails",
            Map.of(
                "accountName",
                "AC7 New",
                "sortCode",
                "601111",
                "accountNumber",
                newAccountNumber)));

    given()
        .pathParam("firmId", practitioner.firmNumber())
        .pathParam("officeCode", practitioner.officeCode())
        .when()
        .get("/provider-firms/{firmId}/offices/{officeCode}/bank-details")
        .then()
        .statusCode(200)
        .body("data.content", hasSize(2))
        .body(
            "data.content.findAll { it.accountNumber == '" + initialAccountNumber + "' }",
            hasSize(1))
        .body(
            "data.content.findAll { it.accountNumber == '" + newAccountNumber + "' }", hasSize(1));
  }

  /// Switching the primary Bank Account to a different one.
  ///
  /// - DSTEW-1998 AC8-AC11 – the outgoing association ends and the incoming one starts on the
  ///   same date, with the primary flag flipped on each side. (DS_MAPD_FR_037)
  ///
  /// - DS_MAPD_FR_037: Assign an existing Legal Organisation Bank Account to a Legal
  ///   Organisation Child Office.
  @Test
  void dstew1998_ac8to11_childOffice_switchBankAccount_datesAlignedAndPrimaryFlagsCorrect() {
    long ts = System.currentTimeMillis();
    String initialAccountNumber = "2" + (ts % 10_000_000L);
    String newAccountNumber = "3" + ((ts + 1) % 10_000_000L);
    ChildOfficeFixture office = createChildOffice("AC8to11", "EFT", initialAccountNumber);

    patchOfficePayment(
        office.firmNumber(),
        office.officeGuid(),
        Map.of(
            "paymentMethod",
            "EFT",
            "bankAccountDetails",
            Map.of(
                "accountName",
                "AC8to11 New",
                "sortCode",
                "601111",
                "accountNumber",
                newAccountNumber)));

    Response response =
        given()
            .pathParam("firmId", office.firmNumber())
            .pathParam("officeCode", office.officeGuid())
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

    assertThat("outgoing end date should not be null", outgoingEndDate, notNullValue());
    assertThat("incoming start date should not be null", incomingStartDate, notNullValue());
    assertThat("start and end dates must align", outgoingEndDate, equalTo(incomingStartDate));
  }

  /// Switching the primary Bank Account to a different one.
  ///
  /// - DSTEW-1998 AC8-AC11 – the outgoing association ends and the incoming one starts on the
  ///   same date, with the primary flag flipped on each side. (DS_MAPD_FR_038)
  ///
  /// - DS_MAPD_FR_038: Assign a bank account to a Legal Practitioner.
  @Test
  void dstew1998_ac8to11_practitioner_switchBankAccount_datesAlignedAndPrimaryFlagsCorrect() {
    long ts = System.currentTimeMillis();
    String initialAccountNumber = "4" + (ts % 10_000_000L);
    String newAccountNumber = "5" + ((ts + 1) % 10_000_000L);
    PractitionerFixture practitioner = createChambersAndPractitioner("AC8to11");

    patchOfficePayment(
        practitioner.firmNumber(),
        practitioner.officeCode(),
        Map.of(
            "paymentMethod",
            "EFT",
            "bankAccountDetails",
            Map.of(
                "accountName",
                "AC8to11 Initial",
                "sortCode",
                "601111",
                "accountNumber",
                initialAccountNumber)));
    patchOfficePayment(
        practitioner.firmNumber(),
        practitioner.officeCode(),
        Map.of(
            "paymentMethod",
            "EFT",
            "bankAccountDetails",
            Map.of(
                "accountName",
                "AC8to11 New",
                "sortCode",
                "601111",
                "accountNumber",
                newAccountNumber)));

    Response response =
        given()
            .pathParam("firmId", practitioner.firmNumber())
            .pathParam("officeCode", practitioner.officeCode())
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

    assertThat("outgoing end date should not be null", outgoingEndDate, notNullValue());
    assertThat("incoming start date should not be null", incomingStartDate, notNullValue());
    assertThat("start and end dates must align", outgoingEndDate, equalTo(incomingStartDate));
  }

  /// Reverting to a previously-used Bank Account.
  ///
  /// - DSTEW-1998 AC12 – makes it primary again and retains all prior link rows (no deletion of
  ///   history). (DS_MAPD_FR_037)
  ///
  /// - DS_MAPD_FR_037: Assign an existing Legal Organisation Bank Account to a Legal
  ///   Organisation Child Office.
  @Test
  void dstew1998_ac12_childOffice_revertToPreviousBankAccount_becomesPrimaryAgainAndHistoryKept() {
    long ts = System.currentTimeMillis();
    String account0Number = "6" + (ts % 10_000_000L);
    String replacementAccountNumber = "7" + ((ts + 1) % 10_000_000L);
    ChildOfficeFixture office = createChildOffice("AC12", "EFT", account0Number);

    String account0Guid =
        given()
            .pathParam("firmId", office.firmNumber())
            .pathParam("officeCode", office.officeGuid())
            .when()
            .get("/provider-firms/{firmId}/offices/{officeCode}/bank-details")
            .then()
            .statusCode(200)
            .extract()
            .path("data.content[0].guid");

    patchOfficePayment(
        office.firmNumber(),
        office.officeGuid(),
        Map.of(
            "paymentMethod",
            "EFT",
            "bankAccountDetails",
            Map.of(
                "accountName",
                "AC12 A",
                "sortCode",
                "601111",
                "accountNumber",
                replacementAccountNumber)));

    patchOfficePayment(
        office.firmNumber(),
        office.officeGuid(),
        Map.of(
            "paymentMethod",
            "EFT",
            "bankAccountDetails",
            Map.of("type", "link", "bankAccountGUID", account0Guid)));

    given()
        .pathParam("firmId", office.firmNumber())
        .pathParam("officeCode", office.officeGuid())
        .when()
        .get("/provider-firms/{firmId}/offices/{officeCode}/bank-details")
        .then()
        .statusCode(200)
        // Three rows: account 0's original span, account A (now historical), account 0's new span.
        .body("data.content", hasSize(3))
        .body("data.content.findAll { it.primaryFlag == true }", hasSize(1))
        .body(
            "data.content.findAll { it.primaryFlag == true }.accountNumber[0]",
            equalTo(account0Number))
        .body(
            "data.content.findAll { it.accountNumber == '"
                + replacementAccountNumber
                + "' }[0].primaryFlag",
            equalTo(false))
        .body(
            "data.content.findAll { it.accountNumber == '"
                + replacementAccountNumber
                + "' }[0].activeDateTo",
            notNullValue());
  }

  /// Reverting to a previously-used Bank Account.
  ///
  /// - DSTEW-1998 AC12 – makes it primary again and retains all prior link rows (no deletion of
  ///   history). (DS_MAPD_FR_038)
  ///
  /// - DS_MAPD_FR_038: Assign a bank account to a Legal Practitioner.
  @Test
  void dstew1998_ac12_practitioner_revertToPreviousBankAccount_becomesPrimaryAgainAndHistoryKept() {
    long ts = System.currentTimeMillis();
    String account0Number = "8" + (ts % 10_000_000L);
    String replacementAccountNumber = "9" + ((ts + 1) % 10_000_000L);
    PractitionerFixture practitioner = createChambersAndPractitioner("AC12");

    patchOfficePayment(
        practitioner.firmNumber(),
        practitioner.officeCode(),
        Map.of(
            "paymentMethod",
            "EFT",
            "bankAccountDetails",
            Map.of(
                "accountName",
                "AC12 Practitioner 0",
                "sortCode",
                "601111",
                "accountNumber",
                account0Number)));

    String account0Guid =
        given()
            .pathParam("firmId", practitioner.firmNumber())
            .pathParam("officeCode", practitioner.officeCode())
            .when()
            .get("/provider-firms/{firmId}/offices/{officeCode}/bank-details")
            .then()
            .statusCode(200)
            .extract()
            .path("data.content[0].guid");

    patchOfficePayment(
        practitioner.firmNumber(),
        practitioner.officeCode(),
        Map.of(
            "paymentMethod",
            "EFT",
            "bankAccountDetails",
            Map.of(
                "accountName",
                "AC12 Practitioner A",
                "sortCode",
                "601111",
                "accountNumber",
                replacementAccountNumber)));

    patchOfficePayment(
        practitioner.firmNumber(),
        practitioner.officeCode(),
        Map.of(
            "paymentMethod",
            "EFT",
            "bankAccountDetails",
            Map.of("type", "link", "bankAccountGUID", account0Guid)));

    given()
        .pathParam("firmId", practitioner.firmNumber())
        .pathParam("officeCode", practitioner.officeCode())
        .when()
        .get("/provider-firms/{firmId}/offices/{officeCode}/bank-details")
        .then()
        .statusCode(200)
        .body("data.content", hasSize(3))
        .body("data.content.findAll { it.primaryFlag == true }", hasSize(1))
        .body(
            "data.content.findAll { it.primaryFlag == true }.accountNumber[0]",
            equalTo(account0Number))
        .body(
            "data.content.findAll { it.accountNumber == '"
                + replacementAccountNumber
                + "' }[0].primaryFlag",
            equalTo(false))
        .body(
            "data.content.findAll { it.accountNumber == '"
                + replacementAccountNumber
                + "' }[0].activeDateTo",
            notNullValue());
  }

  // ---------------------------------------------------------------------------------------
  // Fixtures and helpers
  // ---------------------------------------------------------------------------------------

  /// Creates an isolated LSP firm with the given head-office payment setup, then resolves the
  /// head office GUID. Unlike {@link #createChildOffice}, the head office itself is the target
  /// under test here, so its `payment` block (rather than the child office's) is parameterised.
  ///
  /// @param label unique label used in names/emails to keep test data distinguishable
  /// @param paymentMethod `"EFT"` or `"CHECK"`
  /// @param initialAccountNumber account number for the initial EFT bank account, or `null` when
  ///     `paymentMethod` is `"CHECK"`
  private HeadOfficeFixture createHeadOfficeFirm(
      String label, String paymentMethod, String initialAccountNumber) {
    long ts = System.currentTimeMillis();

    Map<String, Object> payment =
        "EFT".equals(paymentMethod)
            ? Map.of(
                "paymentMethod",
                "EFT",
                "paymentHeldFlag",
                false,
                "bankAccountDetails",
                Map.of(
                    "accountName",
                    label + " Head Office",
                    "sortCode",
                    "601111",
                    "accountNumber",
                    initialAccountNumber))
            : Map.of("paymentMethod", "CHECK", "paymentHeldFlag", false);

    String firmNumber =
        given()
            .contentType(ContentType.JSON)
            .body(
                Map.of(
                    "firmType",
                    "Legal Services Provider",
                    "name",
                    "E2E-DSTEW-1998 " + label + " " + ts,
                    "legalServicesProvider",
                    Map.of(
                        "constitutionalStatus",
                        "Partnership",
                        "address",
                        Map.of(
                            "line1", "1 " + label + " Head Office Street",
                            "townOrCity", "London",
                            "postcode", "EC1A 1BB"),
                        "payment",
                        payment,
                        "contractManager",
                        Map.of("useDefaultContractManager", true),
                        "liaisonManager",
                        Map.of(
                            "firstName",
                            "Head",
                            "lastName",
                            "Office LM " + label,
                            "emailAddress",
                            "head.office.lm." + label + "." + ts + "@example.com",
                            "telephoneNumber",
                            "020 1111 2222"))))
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

    return new HeadOfficeFixture(firmNumber, officeGuid);
  }

  /// Creates an isolated LSP firm (head office payment method is irrelevant to these tests) and
  /// then POSTs a child office with the given payment setup.
  ///
  /// @param label unique label used in names/emails to keep test data distinguishable
  /// @param paymentMethod `"EFT"` or `"CHECK"`
  /// @param initialAccountNumber account number for the initial EFT bank account, or `null` when
  ///     `paymentMethod` is `"CHECK"`
  private ChildOfficeFixture createChildOffice(
      String label, String paymentMethod, String initialAccountNumber) {
    long ts = System.currentTimeMillis();

    String firmNumber =
        given()
            .contentType(ContentType.JSON)
            .body(
                Map.of(
                    "firmType",
                    "Legal Services Provider",
                    "name",
                    "E2E-DSTEW-1998 " + label + " " + ts,
                    "legalServicesProvider",
                    Map.of(
                        "constitutionalStatus",
                        "Partnership",
                        "address",
                        Map.of(
                            "line1", "1 " + label + " Head Office Street",
                            "townOrCity", "London",
                            "postcode", "EC1A 1BB"),
                        "payment",
                        Map.of("paymentMethod", "CHECK"),
                        "contractManager",
                        Map.of("useDefaultContractManager", true),
                        "liaisonManager",
                        Map.of(
                            "firstName",
                            "Head",
                            "lastName",
                            "Office LM " + label,
                            "emailAddress",
                            "head.office.lm." + label + "." + ts + "@example.com",
                            "telephoneNumber",
                            "020 1111 2222"))))
            .when()
            .post("/provider-firms")
            .then()
            .statusCode(201)
            .extract()
            .path("data.providerFirmNumber");

    Map<String, Object> payment =
        "EFT".equals(paymentMethod)
            ? Map.of(
                "paymentMethod",
                "EFT",
                "bankAccountDetails",
                Map.of(
                    "accountName",
                    label + " Child Office",
                    "sortCode",
                    "601111",
                    "accountNumber",
                    initialAccountNumber))
            : Map.of("paymentMethod", "CHECK");

    String officeGuid =
        given()
            .contentType(ContentType.JSON)
            .pathParam("firmId", firmNumber)
            .body(
                Map.of(
                    "address",
                    Map.of(
                        "line1", "1 " + label + " Child Office Street",
                        "townOrCity", "Bristol",
                        "postcode", "BS1 1AA"),
                    "payment",
                    payment,
                    "contractManager",
                    Map.of("useHeadOfficeContractManager", true),
                    "liaisonManager",
                    Map.of(
                        "firstName",
                        "Child",
                        "lastName",
                        "Office LM " + label,
                        "emailAddress",
                        "child.office.lm." + label + "." + ts + "@example.com",
                        "telephoneNumber",
                        "0117 1111 2222")))
            .when()
            .post("/provider-firms/{firmId}/offices")
            .then()
            .statusCode(201)
            .extract()
            .path("data.officeGUID");

    return new ChildOfficeFixture(firmNumber, officeGuid);
  }

  /// Creates an isolated active Chambers and a Practitioner (Advocate) linked to it, with an
  /// initial `paymentMethod=CHECK` (no bank account required).
  ///
  /// @param label unique label used in names/emails to keep test data distinguishable
  private PractitionerFixture createChambersAndPractitioner(String label) {
    long ts = System.currentTimeMillis();

    String chambersFirmNumber =
        given()
            .contentType(ContentType.JSON)
            .body(
                Map.of(
                    "firmType",
                    "Chambers",
                    "name",
                    "E2E-DSTEW-1998 Chambers " + label + " " + ts,
                    "chambers",
                    Map.of(
                        "address",
                        Map.of(
                            "line1", "1 " + label + " Chambers Street",
                            "townOrCity", "London",
                            "postcode", "WC1A 1AA"),
                        "liaisonManager",
                        Map.of(
                            "firstName",
                            "Chambers",
                            "lastName",
                            "LM " + label,
                            "emailAddress",
                            "chambers.lm." + label + "." + ts + "@example.com",
                            "telephoneNumber",
                            "020 7000 1234"))))
            .when()
            .post("/provider-firms")
            .then()
            .statusCode(201)
            .extract()
            .path("data.providerFirmNumber");

    String practitionerFirmNumber =
        given()
            .contentType(ContentType.JSON)
            .body(
                Map.of(
                    "firmType",
                    "Advocate",
                    "name",
                    "E2E-DSTEW-1998 Practitioner " + label + " " + ts,
                    "practitioner",
                    Map.of(
                        "parentFirms",
                        List.of(Map.of("parentFirmNumber", chambersFirmNumber)),
                        "advocateType",
                        "Advocate",
                        "advocate",
                        Map.of(
                            "advocateLevel",
                            "Junior",
                            "solicitorRegulationAuthorityRollNumber",
                            "SRA-" + label + "-" + ts),
                        "liaisonManager",
                        Map.of(
                            "firstName",
                            "Practitioner",
                            "lastName",
                            "LM " + label,
                            "emailAddress",
                            "practitioner.lm." + label + "." + ts + "@example.com",
                            "telephoneNumber",
                            "020 2222 8888"),
                        "payment",
                        Map.of("paymentMethod", "CHECK"))))
            .when()
            .post("/provider-firms")
            .then()
            .statusCode(201)
            .extract()
            .path("data.providerFirmNumber");

    String officeCode =
        given()
            .pathParam("firmId", practitionerFirmNumber)
            .when()
            .get("/provider-firms/{firmId}/offices")
            .then()
            .statusCode(200)
            .extract()
            .path("data.content[0].accountNumber");

    return new PractitionerFixture(practitionerFirmNumber, officeCode);
  }

  /// Patches an office's `payment` fields. `paymentHeldFlag` is a required field of
  /// `PaymentDetailsPatchOrLinkV2`, so it defaults to `false` here unless the caller already
  /// supplied it, keeping call sites focused on the paymentMethod/bankAccountDetails fields under
  /// test.
  private void patchOfficePayment(
      String firmNumber, String officeCode, Map<String, Object> payment) {
    Map<String, Object> paymentWithDefaults = new java.util.HashMap<>(payment);
    paymentWithDefaults.putIfAbsent("paymentHeldFlag", false);
    given()
        .contentType(ContentType.JSON)
        .pathParam("firmId", firmNumber)
        .pathParam("officeCode", officeCode)
        .body(Map.of("payment", paymentWithDefaults))
        .when()
        .patch("/provider-firms/{firmId}/offices/{officeCode}")
        .then()
        .statusCode(200);
  }

  /// Looks up the GUID of the office's current primary bank account link.
  private String lookUpPrimaryBankAccountGuid(String firmNumber, String officeCode) {
    return given()
        .pathParam("firmId", firmNumber)
        .pathParam("officeCode", officeCode)
        .when()
        .get("/provider-firms/{firmId}/offices/{officeCode}/bank-details")
        .then()
        .statusCode(200)
        .extract()
        .path("data.content.find { it.primaryFlag == true }.guid");
  }

  private record HeadOfficeFixture(String firmNumber, String officeGuid) {}

  private record ChildOfficeFixture(String firmNumber, String officeGuid) {}

  private record PractitionerFixture(String firmNumber, String officeCode) {}
}
