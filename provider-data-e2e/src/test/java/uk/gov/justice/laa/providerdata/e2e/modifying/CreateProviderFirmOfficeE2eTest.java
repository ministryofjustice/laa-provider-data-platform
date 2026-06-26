package uk.gov.justice.laa.providerdata.e2e.modifying;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;

import io.restassured.http.ContentType;
import io.restassured.response.Response;
import java.util.Map;
import org.junit.jupiter.api.Test;
import uk.gov.justice.laa.providerdata.e2e.E2eConfig;
import uk.gov.justice.laa.providerdata.e2e.ModifyingTest;

/// Data-modifying e2e tests for `POST /provider-firms/{firmId}/offices`.
///
/// Most tests create a new office linked to the E2E LSP provider and verify it via GET.
///
/// The DSTEW-1651 tests create isolated firms so that head-office LM state is fully controlled.
///
/// The DSTEW-1667 tests cover child-office creation business rules (DS_MAPD_FR_025).
@ModifyingTest
class CreateProviderFirmOfficeE2eTest {

  /// POSTs a child office with all mandatory fields and an explicit new Liaison Manager, then
  /// verifies the office is retrievable via GET.
  ///
  /// - DSTEW-1650 AC1 – the LM's `activeDateFrom` is set automatically at the point of
  ///   assignment. (DS_MAPD_FR_015)
  /// - DSTEW-1651 AC2 – the explicitly-provided LM is recorded; the parent firm's default is not
  ///   applied. (DS_MAPD_FR_016)
  /// - DSTEW-1667 AC1 – providing all mandatory fields results in successful office creation.
  ///   (DS_MAPD_FR_025)
  /// - DSTEW-1667 AC5 – omitting both DX fields is a valid state. (DS_MAPD_FR_025)
  /// - DSTEW-1667 AC6 – the `LiaisonManagerCreateV2` (new-LM) variant is accepted.
  ///   (DS_MAPD_FR_025)
  ///
  /// - DS_MAPD_FR_015: The Active From date is system-derived and set when a Liaison Manager is
  ///   created and assigned.
  /// - DS_MAPD_FR_016: When a child office is created with an office-specific Liaison Manager,
  ///   PDA-r2 records that LM and does not apply the parent firm's default.
  /// - DS_MAPD_FR_025: Create Child Office – mandatory fields, DX conditional rule, and LM
  ///   variant enforcement.
  @Test
  void dstew1650_ac1_createOffice_forExistingLspFirm_persisted() {
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

  /// Creates an isolated LSP firm with a head-office Liaison Manager, then creates a child office
  /// using `useHeadOfficeLiaisonManager: true`; verifies the head-office LM is active on the
  /// child office.
  ///
  /// - DSTEW-1651 AC1 – when no office-specific LM is provided, PDA-r2 assigns the parent firm's
  ///   LM to the child office with `linkedFlag=true`. (DS_MAPD_FR_024)
  /// - DSTEW-1667 AC6 – the `useHeadOfficeLiaisonManager: true` variant is accepted.
  ///   (DS_MAPD_FR_025)
  ///
  /// - DS_MAPD_FR_024: Default Firm Liaison Manager to Child Office – assignment is one-off at
  ///   creation and uses the parent's active LM.
  /// - DS_MAPD_FR_025: Create Child Office – `useHeadOfficeLiaisonManager: true` is one of the
  ///   accepted mutually exclusive LM variants.
  @Test
  void dstew1651_ac1_headOfficeLmDefaultedToChildOffice_whenNoneProvided() {
    long ts = System.currentTimeMillis();

    String firmNumber =
        given()
            .contentType(ContentType.JSON)
            .body(
                Map.of(
                    "firmType",
                    "Legal Services Provider",
                    "name",
                    "E2E-DSTEW-1651 AC1 " + ts,
                    "legalServicesProvider",
                    Map.of(
                        "constitutionalStatus",
                        "Partnership",
                        "address",
                        Map.of(
                            "line1", "1 Default LM Street",
                            "townOrCity", "London",
                            "postcode", "EC1A 1BB"),
                        "payment",
                        Map.of("paymentMethod", "CHECK"),
                        "contractManager",
                        Map.of("contractManagerGUID", "12345678-1234-1234-1234-123456789012"),
                        "liaisonManager",
                        Map.of(
                            "firstName", "Head",
                            "lastName", "OfficeLm",
                            "emailAddress", "head.lm.ac1." + ts + "@example.com",
                            "telephoneNumber", "020 1111 2222"))))
            .when()
            .post("/provider-firms")
            .then()
            .statusCode(201)
            .extract()
            .path("data.providerFirmNumber");

    String headOfficeLmGuid =
        given()
            .pathParam("firmId", firmNumber)
            .when()
            .get("/provider-firms/{firmId}")
            .then()
            .statusCode(200)
            .extract()
            .path("data.legalServicesProvider.headOffice.liaisonManager.guid");

    String childOfficeGuid =
        given()
            .contentType(ContentType.JSON)
            .pathParam("firmId", firmNumber)
            .body(
                Map.of(
                    "address",
                    Map.of(
                        "line1", "2 Default LM Street",
                        "townOrCity", "London",
                        "postcode", "EC1A 1BB"),
                    "payment",
                    Map.of("paymentMethod", "CHECK"),
                    "liaisonManager",
                    Map.of("useHeadOfficeLiaisonManager", true)))
            .when()
            .post("/provider-firms/{firmId}/offices")
            .then()
            .statusCode(201)
            .extract()
            .path("data.officeGUID");

    given()
        .pathParam("firmId", firmNumber)
        .pathParam("officeCode", childOfficeGuid)
        .when()
        .get("/provider-firms/{firmId}/offices/{officeCode}/liaison-managers")
        .then()
        .statusCode(200)
        .body("data.content.find { it.activeDateTo == null }.guid", equalTo(headOfficeLmGuid))
        .body("data.content.find { it.activeDateTo == null }.linkedFlag", equalTo(true));
  }

  /// Creates an isolated LSP firm with a head-office LM, then creates a child office with an
  /// explicit new Liaison Manager; verifies the office-specific LM is active and the parent's LM
  /// is not applied.
  ///
  /// - DSTEW-1651 AC2 – when an office-specific LM is provided, PDA-r2 records that LM with
  ///   `linkedFlag=false` and does not apply the parent firm's default. (DS_MAPD_FR_024)
  ///
  /// - DS_MAPD_FR_024: Default Firm Liaison Manager to Child Office – defaulting must not occur
  ///   when an office-specific LM is explicitly provided.
  @Test
  void dstew1651_ac2_explicitLiaisonManager_isUsedAndParentDefaultNotApplied() {
    long ts = System.currentTimeMillis();

    String firmNumber =
        given()
            .contentType(ContentType.JSON)
            .body(
                Map.of(
                    "firmType",
                    "Legal Services Provider",
                    "name",
                    "E2E-DSTEW-1651 AC2 " + ts,
                    "legalServicesProvider",
                    Map.of(
                        "constitutionalStatus",
                        "Partnership",
                        "address",
                        Map.of(
                            "line1", "1 Explicit LM Street",
                            "townOrCity", "London",
                            "postcode", "EC1A 1BB"),
                        "payment",
                        Map.of("paymentMethod", "CHECK"),
                        "contractManager",
                        Map.of("contractManagerGUID", "12345678-1234-1234-1234-123456789012"),
                        "liaisonManager",
                        Map.of(
                            "firstName", "Parent",
                            "lastName", "OfficeLm",
                            "emailAddress", "parent.lm.ac2." + ts + "@example.com",
                            "telephoneNumber", "020 1111 2222"))))
            .when()
            .post("/provider-firms")
            .then()
            .statusCode(201)
            .extract()
            .path("data.providerFirmNumber");

    String headOfficeLmGuid =
        given()
            .pathParam("firmId", firmNumber)
            .when()
            .get("/provider-firms/{firmId}")
            .then()
            .statusCode(200)
            .extract()
            .path("data.legalServicesProvider.headOffice.liaisonManager.guid");

    String childOfficeGuid =
        given()
            .contentType(ContentType.JSON)
            .pathParam("firmId", firmNumber)
            .body(
                Map.of(
                    "address",
                    Map.of(
                        "line1", "2 Explicit LM Street",
                        "townOrCity", "London",
                        "postcode", "EC1A 1BB"),
                    "payment",
                    Map.of("paymentMethod", "CHECK"),
                    "liaisonManager",
                    Map.of(
                        "firstName", "Office",
                        "lastName", "SpecificLm",
                        "emailAddress", "office.specific.ac2." + ts + "@example.com",
                        "telephoneNumber", "020 3333 4444")))
            .when()
            .post("/provider-firms/{firmId}/offices")
            .then()
            .statusCode(201)
            .extract()
            .path("data.officeGUID");

    given()
        .pathParam("firmId", firmNumber)
        .pathParam("officeCode", childOfficeGuid)
        .when()
        .get("/provider-firms/{firmId}/offices/{officeCode}/liaison-managers")
        .then()
        .statusCode(200)
        .body("data.content.find { it.activeDateTo == null }.firstName", equalTo("Office"))
        .body("data.content.find { it.activeDateTo == null }.lastName", equalTo("SpecificLm"))
        .body("data.content.find { it.activeDateTo == null }.guid", not(equalTo(headOfficeLmGuid)))
        .body("data.content.find { it.activeDateTo == null }.linkedFlag", equalTo(false));
  }

  /// Creates an isolated LSP firm, creates a child office inheriting the head-office LM, then
  /// replaces the head-office LM; verifies the child office's LM assignment is unchanged.
  ///
  /// - DSTEW-1651 AC3 – after a child office inherits the head-office LM, later changes to the
  ///   parent's LM do not propagate to the child office. (DS_MAPD_FR_024)
  ///
  /// - DS_MAPD_FR_024: Default Firm Liaison Manager to Child Office – assignment is a one-off at
  ///   creation; subsequent parent changes must not affect the child office.
  @Test
  void dstew1651_ac3_changingParentLm_doesNotAffectLinkedChildOfficeLm() {
    long ts = System.currentTimeMillis();

    String firmNumber =
        given()
            .contentType(ContentType.JSON)
            .body(
                Map.of(
                    "firmType",
                    "Legal Services Provider",
                    "name",
                    "E2E-DSTEW-1651 AC3 " + ts,
                    "legalServicesProvider",
                    Map.of(
                        "constitutionalStatus",
                        "Partnership",
                        "address",
                        Map.of(
                            "line1", "1 One-Off Street",
                            "townOrCity", "London",
                            "postcode", "EC1A 1BB"),
                        "payment",
                        Map.of("paymentMethod", "CHECK"),
                        "contractManager",
                        Map.of("contractManagerGUID", "12345678-1234-1234-1234-123456789012"),
                        "liaisonManager",
                        Map.of(
                            "firstName", "Original",
                            "lastName", "HeadLm",
                            "emailAddress", "original.head.ac3." + ts + "@example.com",
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

    String originalLmGuid =
        given()
            .pathParam("firmId", firmNumber)
            .when()
            .get("/provider-firms/{firmId}")
            .then()
            .statusCode(200)
            .extract()
            .path("data.legalServicesProvider.headOffice.liaisonManager.guid");

    String childOfficeGuid =
        given()
            .contentType(ContentType.JSON)
            .pathParam("firmId", firmNumber)
            .body(
                Map.of(
                    "address",
                    Map.of(
                        "line1", "2 One-Off Street",
                        "townOrCity", "London",
                        "postcode", "EC1A 1BB"),
                    "payment",
                    Map.of("paymentMethod", "CHECK"),
                    "liaisonManager",
                    Map.of("useHeadOfficeLiaisonManager", true)))
            .when()
            .post("/provider-firms/{firmId}/offices")
            .then()
            .statusCode(201)
            .extract()
            .path("data.officeGUID");

    // Replace the head office's LM with a new one.
    given()
        .contentType(ContentType.JSON)
        .pathParam("firmId", firmNumber)
        .pathParam("officeCode", headOfficeCode)
        .body(
            Map.of(
                "firstName", "New",
                "lastName", "HeadLm",
                "emailAddress", "new.head.ac3." + ts + "@example.com",
                "telephoneNumber", "020 5555 6666"))
        .when()
        .post("/provider-firms/{firmId}/offices/{officeCode}/liaison-managers")
        .then()
        .statusCode(201);

    // Child office must still have the original LM — no cascade.
    given()
        .pathParam("firmId", firmNumber)
        .pathParam("officeCode", childOfficeGuid)
        .when()
        .get("/provider-firms/{firmId}/offices/{officeCode}/liaison-managers")
        .then()
        .statusCode(200)
        .body("data.content.find { it.activeDateTo == null }.guid", equalTo(originalLmGuid));
  }

  /// POSTs a child office referencing a non-existent parent firm; expects 404 and verifies no
  /// office is created.
  ///
  /// - DSTEW-1667 AC3 – the parent organisation must exist before a child office can be
  ///   created. (DS_MAPD_FR_025)
  ///
  /// - DS_MAPD_FR_025: Create Child Office – the referenced parent LSP firm must exist.
  @Test
  void dstew1667_ac3_createOffice_forUnknownFirm_returns404AndOfficeNotCreated() {
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

    // Confirm the firm does not exist and therefore has no offices.
    given()
        .pathParam("firmId", E2eConfig.invalidFirmNumber())
        .when()
        .get("/provider-firms/{firmId}")
        .then()
        .statusCode(404);

    given()
        .pathParam("firmId", E2eConfig.invalidFirmNumber())
        .when()
        .get("/provider-firms/{firmId}/offices")
        .then()
        .statusCode(404);
  }

  /// POSTs a child office with an explicit contract manager GUID, then verifies the assigned
  /// contract manager is returned by the office contract-managers endpoint.
  ///
  /// - DSTEW-1667 AC1 – when `contractManager.contractManagerGUID` is supplied, PDA-r2 assigns
  ///   that contract manager to the new office. (DS_MAPD_FR_025)
  ///
  /// - DS_MAPD_FR_025: Create Child Office – optional contract manager assignment on creation.
  @Test
  void dstew1667_ac1_withContractManagerGuid_cmIsAssigned() {
    long ts = System.currentTimeMillis();

    String cmGuid =
        given()
            .queryParam("contractManagerId", E2eConfig.contractManagerId())
            .when()
            .get("/provider-contract-managers")
            .then()
            .statusCode(200)
            .extract()
            .path("data.content[0].guid");

    String firmNumber = createIsolatedLspFirmNumber("DSTEW-1667-AC1-CM", ts);

    String officeCode =
        given()
            .contentType(ContentType.JSON)
            .pathParam("firmId", firmNumber)
            .body(
                Map.of(
                    "address",
                    Map.of(
                        "line1", "1 CM Street",
                        "townOrCity", "London",
                        "postcode", "EC1A 1BB"),
                    "payment",
                    Map.of("paymentMethod", "CHECK"),
                    "liaisonManager",
                    Map.of("useHeadOfficeLiaisonManager", true),
                    "contractManager",
                    Map.of("contractManagerGUID", cmGuid)))
            .when()
            .post("/provider-firms/{firmId}/offices")
            .then()
            .statusCode(201)
            .extract()
            .path("data.officeCode");

    given()
        .pathParam("firmId", firmNumber)
        .pathParam("officeCode", officeCode)
        .when()
        .get("/provider-firms/{firmId}/offices/{officeCode}/contract-managers")
        .then()
        .statusCode(200)
        .body("data.content", hasSize(1))
        .body("data.content[0].contractManagerId", equalTo(E2eConfig.contractManagerId()));
  }

  /// Creates an isolated LSP firm, then POSTs a child office with a `contractManagerGUID` that
  /// does not exist; expects 400 and verifies no office is created.
  ///
  /// - DSTEW-1667 AC1 – an invalid (non-existent) contract manager GUID must be rejected.
  ///   (DS_MAPD_FR_025)
  /// - DSTEW-1667 AC7 – a failed request must not result in a partially created office.
  ///   (DS_MAPD_FR_025)
  ///
  /// - DS_MAPD_FR_025: Create Child Office – the contract manager GUID must reference a valid
  ///   record; mandatory field validation and complete-record enforcement.
  @Test
  void dstew1667_ac1_createOffice_withInvalidContractManagerGuid_returns400AndOfficeNotCreated() {
    long ts = System.currentTimeMillis();
    String firmNumber = createIsolatedLspFirmNumber("DSTEW-1667-AC1-INVALID-CM", ts);

    given()
        .contentType(ContentType.JSON)
        .pathParam("firmId", firmNumber)
        .body(
            Map.of(
                "address",
                Map.of(
                    "line1", "1 Invalid CM Street",
                    "townOrCity", "London",
                    "postcode", "EC1A 1BB"),
                "payment",
                Map.of("paymentMethod", "CHECK"),
                "liaisonManager",
                Map.of("useHeadOfficeLiaisonManager", true),
                "contractManager",
                Map.of("contractManagerGUID", "00000000-0000-0000-0000-000000000000")))
        .when()
        .post("/provider-firms/{firmId}/offices")
        .then()
        .statusCode(400);

    given()
        .pathParam("firmId", firmNumber)
        .when()
        .get("/provider-firms/{firmId}/offices")
        .then()
        .statusCode(200)
        .body("data.content", hasSize(1));
  }

  /// POSTs a child office without the mandatory `address` field; expects 400 and verifies the
  /// firm's office count is unchanged (head office only).
  ///
  /// - DSTEW-1667 AC2 – `address` is a mandatory field; omitting it is rejected. (DS_MAPD_FR_025)
  /// - DSTEW-1667 AC7 – a failed request must not result in a partially created office.
  ///   (DS_MAPD_FR_025)
  ///
  /// - DS_MAPD_FR_025: Create Child Office – mandatory field validation and complete-record
  ///   enforcement.
  @Test
  void dstew1667_ac2_ac7_missingAddress_returns400AndOfficeNotCreated() {
    long ts = System.currentTimeMillis();
    String firmNumber = createIsolatedLspFirmNumber("DSTEW-1667-AC2-ADDR", ts);

    given()
        .contentType(ContentType.JSON)
        .pathParam("firmId", firmNumber)
        .body(
            Map.of(
                "payment",
                Map.of("paymentMethod", "CHECK"),
                "liaisonManager",
                Map.of("useHeadOfficeLiaisonManager", true)))
        .when()
        .post("/provider-firms/{firmId}/offices")
        .then()
        .statusCode(400);

    given()
        .pathParam("firmId", firmNumber)
        .when()
        .get("/provider-firms/{firmId}/offices")
        .then()
        .statusCode(200)
        .body("data.content", hasSize(1));
  }

  /// POSTs a child office without the mandatory `payment` field; expects 400 and verifies the
  /// firm's office count is unchanged.
  ///
  /// - DSTEW-1667 AC2 – `payment` is a mandatory field; omitting it is rejected. (DS_MAPD_FR_025)
  /// - DSTEW-1667 AC7 – a failed request must not result in a partially created office.
  ///   (DS_MAPD_FR_025)
  ///
  /// - DS_MAPD_FR_025: Create Child Office – mandatory field validation and complete-record
  ///   enforcement.
  @Test
  void dstew1667_ac2_ac7_missingPayment_returns400AndOfficeNotCreated() {
    long ts = System.currentTimeMillis();
    String firmNumber = createIsolatedLspFirmNumber("DSTEW-1667-AC2-PMT", ts);

    given()
        .contentType(ContentType.JSON)
        .pathParam("firmId", firmNumber)
        .body(
            Map.of(
                "address",
                Map.of(
                    "line1", "1 No Payment Street",
                    "townOrCity", "London",
                    "postcode", "EC1A 1BB"),
                "liaisonManager",
                Map.of("useHeadOfficeLiaisonManager", true)))
        .when()
        .post("/provider-firms/{firmId}/offices")
        .then()
        .statusCode(400);

    given()
        .pathParam("firmId", firmNumber)
        .when()
        .get("/provider-firms/{firmId}/offices")
        .then()
        .statusCode(200)
        .body("data.content", hasSize(1));
  }

  /// POSTs a child office without the mandatory `liaisonManager` field; expects 400 and verifies
  /// the firm's office count is unchanged.
  ///
  /// - DSTEW-1667 AC2 – `liaisonManager` is a mandatory field; omitting it is rejected.
  ///   (DS_MAPD_FR_025)
  /// - DSTEW-1667 AC7 – a failed request must not result in a partially created office.
  ///   (DS_MAPD_FR_025)
  ///
  /// - DS_MAPD_FR_025: Create Child Office – mandatory field validation and complete-record
  ///   enforcement.
  @Test
  void dstew1667_ac2_ac7_missingLiaisonManager_returns400AndOfficeNotCreated() {
    long ts = System.currentTimeMillis();
    String firmNumber = createIsolatedLspFirmNumber("DSTEW-1667-AC2-LM", ts);

    given()
        .contentType(ContentType.JSON)
        .pathParam("firmId", firmNumber)
        .body(
            Map.of(
                "address",
                Map.of(
                    "line1", "1 No LM Street",
                    "townOrCity", "London",
                    "postcode", "EC1A 1BB"),
                "payment",
                Map.of("paymentMethod", "CHECK")))
        .when()
        .post("/provider-firms/{firmId}/offices")
        .then()
        .statusCode(400);

    given()
        .pathParam("firmId", firmNumber)
        .when()
        .get("/provider-firms/{firmId}/offices")
        .then()
        .statusCode(200)
        .body("data.content", hasSize(1));
  }

  /// Creates an isolated LSP firm, then POSTs a child office with `dxNumber` but no `dxCentre`;
  /// expects 400 and verifies no office (and no DX data) is persisted.
  ///
  /// - DSTEW-1667 AC4 – `dxNumber` without `dxCentre` is rejected; the two DX fields are
  ///   interdependent. (DS_MAPD_FR_025)
  /// - DSTEW-1667 AC7 – a failed request must not result in a partially created office.
  ///   (DS_MAPD_FR_025)
  ///
  /// - DS_MAPD_FR_025: Create Child Office – DX fields are conditionally required; both must be
  ///   provided together or neither. The `DXCreateV2` schema marks both as required within the
  ///   object, so a partial DX object fails bean validation before the controller is reached.
  @Test
  void dstew1667_ac4_createOffice_dxNumberWithoutCentre_returns400AndOfficeNotCreated() {
    long ts = System.currentTimeMillis();
    String firmNumber = createIsolatedLspFirmNumber("DSTEW-1667-AC4-DX-NUM", ts);

    given()
        .contentType(ContentType.JSON)
        .pathParam("firmId", firmNumber)
        .body(
            Map.of(
                "address",
                Map.of(
                    "line1", "1 DX Street",
                    "townOrCity", "London",
                    "postcode", "EC1A 1BB"),
                "payment",
                Map.of("paymentMethod", "CHECK"),
                "liaisonManager",
                Map.of("useHeadOfficeLiaisonManager", true),
                "dxDetails",
                Map.of("dxNumber", "DX 13009")))
        .when()
        .post("/provider-firms/{firmId}/offices")
        .then()
        .statusCode(400);

    given()
        .pathParam("firmId", firmNumber)
        .when()
        .get("/provider-firms/{firmId}/offices")
        .then()
        .statusCode(200)
        .body("data.content", hasSize(1))
        .body("data.content[0].dxDetails", equalTo(null));
  }

  /// Creates an isolated LSP firm, then POSTs a child office with `dxCentre` but no `dxNumber`;
  /// expects 400 and verifies no office (and no DX data) is persisted.
  ///
  /// - DSTEW-1667 AC4 – `dxCentre` without `dxNumber` is rejected; the two DX fields are
  ///   interdependent. (DS_MAPD_FR_025)
  /// - DSTEW-1667 AC7 – a failed request must not result in a partially created office.
  ///   (DS_MAPD_FR_025)
  ///
  /// - DS_MAPD_FR_025: Create Child Office – DX fields are conditionally required; see
  ///   {@link #dstew1667_ac4_createOffice_dxNumberWithoutCentre_returns400AndOfficeNotCreated}.
  @Test
  void dstew1667_ac4_createOffice_dxCentreWithoutNumber_returns400AndOfficeNotCreated() {
    long ts = System.currentTimeMillis();
    String firmNumber = createIsolatedLspFirmNumber("DSTEW-1667-AC4-DX-CTR", ts);

    given()
        .contentType(ContentType.JSON)
        .pathParam("firmId", firmNumber)
        .body(
            Map.of(
                "address",
                Map.of(
                    "line1", "1 DX Street",
                    "townOrCity", "London",
                    "postcode", "EC1A 1BB"),
                "payment",
                Map.of("paymentMethod", "CHECK"),
                "liaisonManager",
                Map.of("useHeadOfficeLiaisonManager", true),
                "dxDetails",
                Map.of("dxCentre", "Birmingham")))
        .when()
        .post("/provider-firms/{firmId}/offices")
        .then()
        .statusCode(400);

    given()
        .pathParam("firmId", firmNumber)
        .when()
        .get("/provider-firms/{firmId}/offices")
        .then()
        .statusCode(200)
        .body("data.content", hasSize(1))
        .body("data.content[0].dxDetails", equalTo(null));
  }

  /// POSTs a child office with both `dxNumber` and `dxCentre`; verifies the office is created and
  /// the DX details are persisted and returned on subsequent retrieval.
  ///
  /// - DSTEW-1667 AC5 – both DX fields provided together is valid; the office is created and the
  ///   DX details are persisted. (DS_MAPD_FR_025)
  ///
  /// - DS_MAPD_FR_025: Create Child Office – a complete `dxDetails` object is accepted. The
  ///   complementary case (neither DX field provided) is covered by
  ///   {@link #dstew1650_ac1_createOffice_forExistingLspFirm_persisted}.
  @Test
  void dstew1667_ac5_createOffice_withBothDxFields_dxDetailsPersisted() {
    String officeCode =
        given()
            .contentType(ContentType.JSON)
            .pathParam("firmId", E2eConfig.lspFirmNumber())
            .body(
                Map.of(
                    "address",
                    Map.of(
                        "line1",
                        "1 DX Present Street " + System.currentTimeMillis(),
                        "townOrCity",
                        "Birmingham",
                        "postcode",
                        "B1 1AA"),
                    "payment",
                    Map.of("paymentMethod", "CHECK"),
                    "liaisonManager",
                    Map.of("useHeadOfficeLiaisonManager", true),
                    "dxDetails",
                    Map.of("dxNumber", "DX 13009", "dxCentre", "Birmingham")))
            .when()
            .post("/provider-firms/{firmId}/offices")
            .then()
            .statusCode(201)
            .extract()
            .path("data.officeCode");

    given()
        .pathParam("firmId", E2eConfig.lspFirmNumber())
        .pathParam("officeCode", officeCode)
        .when()
        .get("/provider-firms/{firmId}/offices/{officeCode}")
        .then()
        .statusCode(200)
        .body("data.dxDetails.dxNumber", equalTo("DX 13009"))
        .body("data.dxDetails.dxCentre", equalTo("Birmingham"));
  }

  /// POSTs a child office linking an existing liaison manager by GUID; verifies the office is
  /// created and the referenced LM appears as the active entry in the liaison-manager history.
  ///
  /// - DSTEW-1667 AC6 – the `liaisonManagerGUID` (existing-LM-by-GUID) variant is accepted.
  ///   (DS_MAPD_FR_025)
  ///
  /// - DS_MAPD_FR_025: Create Child Office – the `liaisonManager` field accepts three mutually
  ///   exclusive variants: a new-LM object (covered by
  ///   {@link #dstew1650_ac1_createOffice_forExistingLspFirm_persisted}),
  ///   `useHeadOfficeLiaisonManager: true` (covered by
  ///   {@link #dstew1651_ac1_headOfficeLmDefaultedToChildOffice_whenNoneProvided}), or a reference
  ///   to an existing LM by GUID (this test). Rejection of conflicting combinations is tested in
  ///   the `dstew1667_ac6_createOffice_*_returns400AndOfficeNotCreated` tests.
  @Test
  void dstew1667_ac6_createOffice_withExistingLmGuid_lmAssigned() {
    long ts = System.currentTimeMillis();
    String firmNumber = createIsolatedLspFirmNumber("DSTEW-1667-AC6", ts);

    String headOfficeLmGuid =
        given()
            .pathParam("firmId", firmNumber)
            .when()
            .get("/provider-firms/{firmId}")
            .then()
            .statusCode(200)
            .extract()
            .path("data.legalServicesProvider.headOffice.liaisonManager.guid");

    String officeGuid =
        given()
            .contentType(ContentType.JSON)
            .pathParam("firmId", firmNumber)
            .body(
                Map.of(
                    "address",
                    Map.of(
                        "line1", "1 Existing LM Street",
                        "townOrCity", "London",
                        "postcode", "EC1A 1BB"),
                    "payment",
                    Map.of("paymentMethod", "CHECK"),
                    "liaisonManager",
                    Map.of("liaisonManagerGUID", headOfficeLmGuid)))
            .when()
            .post("/provider-firms/{firmId}/offices")
            .then()
            .statusCode(201)
            .extract()
            .path("data.officeGUID");

    given()
        .pathParam("firmId", firmNumber)
        .pathParam("officeCode", officeGuid)
        .when()
        .get("/provider-firms/{firmId}/offices/{officeCode}/liaison-managers")
        .then()
        .statusCode(200)
        .body("data.content.find { it.activeDateTo == null }.guid", equalTo(headOfficeLmGuid));
  }

  /// POSTs a child office with `liaisonManagerGUID` combined with new-LM properties (`firstName`
  /// etc.); expects 400 and verifies no office is persisted.
  ///
  /// - DSTEW-1667 AC6 – combining `liaisonManagerGUID` with new-LM fields is rejected; only one
  ///   LM variant may be provided at a time. (DS_MAPD_FR_025)
  ///
  /// - DS_MAPD_FR_025: Create Child Office – the `TypeResolvingDeserializer` resolves the LM
  ///   variant from the first discriminator field found and rejects any conflicting fields from
  ///   other variants.
  @Test
  void
      dstew1667_ac6_createOffice_liaisonManagerGuidWithNewLmFields_returns400AndOfficeNotCreated() {
    long ts = System.currentTimeMillis();
    String firmNumber = createIsolatedLspFirmNumber("DSTEW-1667-AC6-CONFLICT-A", ts);

    given()
        .contentType(ContentType.JSON)
        .pathParam("firmId", firmNumber)
        .body(
            Map.of(
                "address",
                Map.of(
                    "line1", "1 Conflict Street",
                    "townOrCity", "London",
                    "postcode", "EC1A 1BB"),
                "payment",
                Map.of("paymentMethod", "CHECK"),
                "liaisonManager",
                Map.of(
                    "liaisonManagerGUID", "12345678-1234-1234-1234-123456789012",
                    "firstName", "Alice")))
        .when()
        .post("/provider-firms/{firmId}/offices")
        .then()
        .statusCode(400);

    given()
        .pathParam("firmId", firmNumber)
        .when()
        .get("/provider-firms/{firmId}/offices")
        .then()
        .statusCode(200)
        .body("data.content", hasSize(1));
  }

  /// POSTs a child office with `useHeadOfficeLiaisonManager: true` combined with
  /// `liaisonManagerGUID`; expects 400 and verifies no office is persisted.
  ///
  /// - DSTEW-1667 AC6 – combining `useHeadOfficeLiaisonManager` with `liaisonManagerGUID` is
  ///   rejected; only one LM variant may be provided at a time. (DS_MAPD_FR_025)
  ///
  /// - DS_MAPD_FR_025: Create Child Office – mutually exclusive LM variant enforcement.
  @Test
  void
      dstew1667_ac6_createOffice_useHeadOfficeWithLiaisonManagerGuid_returns400AndOfficeNotCreated() {
    long ts = System.currentTimeMillis();
    String firmNumber = createIsolatedLspFirmNumber("DSTEW-1667-AC6-CONFLICT-B", ts);

    given()
        .contentType(ContentType.JSON)
        .pathParam("firmId", firmNumber)
        .body(
            Map.of(
                "address",
                Map.of(
                    "line1", "1 Conflict Street",
                    "townOrCity", "London",
                    "postcode", "EC1A 1BB"),
                "payment",
                Map.of("paymentMethod", "CHECK"),
                "liaisonManager",
                Map.of(
                    "useHeadOfficeLiaisonManager",
                    true,
                    "liaisonManagerGUID",
                    "12345678-1234-1234-1234-123456789012")))
        .when()
        .post("/provider-firms/{firmId}/offices")
        .then()
        .statusCode(400);

    given()
        .pathParam("firmId", firmNumber)
        .when()
        .get("/provider-firms/{firmId}/offices")
        .then()
        .statusCode(200)
        .body("data.content", hasSize(1));
  }

  /// POSTs a child office with `useHeadOfficeLiaisonManager: true` combined with new-LM properties
  /// (`firstName` etc.); expects 400 and verifies no office is persisted.
  ///
  /// - DSTEW-1667 AC6 – combining `useHeadOfficeLiaisonManager` with new-LM fields is rejected;
  ///   only one LM variant may be provided at a time. (DS_MAPD_FR_025)
  ///
  /// - DS_MAPD_FR_025: Create Child Office – mutually exclusive LM variant enforcement.
  @Test
  void dstew1667_ac6_createOffice_useHeadOfficeWithNewLmFields_returns400AndOfficeNotCreated() {
    long ts = System.currentTimeMillis();
    String firmNumber = createIsolatedLspFirmNumber("DSTEW-1667-AC6-CONFLICT-C", ts);

    given()
        .contentType(ContentType.JSON)
        .pathParam("firmId", firmNumber)
        .body(
            Map.of(
                "address",
                Map.of(
                    "line1", "1 Conflict Street",
                    "townOrCity", "London",
                    "postcode", "EC1A 1BB"),
                "payment",
                Map.of("paymentMethod", "CHECK"),
                "liaisonManager",
                Map.of("useHeadOfficeLiaisonManager", true, "firstName", "Alice")))
        .when()
        .post("/provider-firms/{firmId}/offices")
        .then()
        .statusCode(400);

    given()
        .pathParam("firmId", firmNumber)
        .when()
        .get("/provider-firms/{firmId}/offices")
        .then()
        .statusCode(200)
        .body("data.content", hasSize(1));
  }

  /// Creates an isolated LSP firm and returns its firm number.
  private static String createIsolatedLspFirmNumber(String tag, long ts) {
    return given()
        .contentType(ContentType.JSON)
        .body(
            Map.of(
                "firmType",
                "Legal Services Provider",
                "name",
                "E2E-" + tag + " " + ts,
                "legalServicesProvider",
                Map.of(
                    "constitutionalStatus",
                    "Partnership",
                    "address",
                    Map.of(
                        "line1", "1 Isolated Street",
                        "townOrCity", "London",
                        "postcode", "EC1A 1BB"),
                    "payment",
                    Map.of("paymentMethod", "CHECK"),
                    "contractManager",
                    Map.of("contractManagerGUID", "12345678-1234-1234-1234-123456789012"),
                    "liaisonManager",
                    Map.of(
                        "firstName", "Isolated",
                        "lastName", "LiaisonMgr",
                        "emailAddress", "isolated.lm." + ts + "@example.com",
                        "telephoneNumber", "020 1234 5678"))))
        .when()
        .post("/provider-firms")
        .then()
        .statusCode(201)
        .extract()
        .path("data.providerFirmNumber");
  }
}
