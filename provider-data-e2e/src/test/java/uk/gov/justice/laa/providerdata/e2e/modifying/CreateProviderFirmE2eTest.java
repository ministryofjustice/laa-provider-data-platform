package uk.gov.justice.laa.providerdata.e2e.modifying;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

import io.restassured.http.ContentType;
import io.restassured.response.Response;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import uk.gov.justice.laa.providerdata.e2e.E2eConfig;
import uk.gov.justice.laa.providerdata.e2e.ModifyingTest;

/**
 * Data-modifying e2e tests for {@code POST /provider-firms}.
 *
 * <p>Each test creates new data in the local database and verifies it via GET.
 */
@ModifyingTest
class CreateProviderFirmE2eTest {

  /**
   * AC1 – Active From date is set automatically when a Liaison Manager is created and assigned.
   *
   * <p>DS_MAPD_FR_023 (DSTEW-1650): The {@code activeDateFrom} field must be populated in the
   * response after a firm is created with a Liaison Manager. The date is system-derived (today) and
   * is not supplied by the caller.
   */
  @Test
  void dstew1650_ac1_activeDateFromSetAutomaticallyOnCreation() {
    long ts = System.currentTimeMillis();

    String firmNumber =
        given()
            .contentType(ContentType.JSON)
            .body(
                Map.of(
                    "firmType",
                    "Legal Services Provider",
                    "name",
                    "E2E-DSTEW-1650 LSP " + ts,
                    "legalServicesProvider",
                    Map.of(
                        "constitutionalStatus",
                        "Partnership",
                        "address",
                        Map.of(
                            "line1", "1 Active Date Street",
                            "townOrCity", "London",
                            "postcode", "EC1A 1BB"),
                        "payment",
                        Map.of("paymentMethod", "CHECK"),
                        "contractManager",
                        Map.of("useDefaultContractManager", true),
                        "liaisonManager",
                        Map.of(
                            "firstName", "Active",
                            "lastName", "DateTest",
                            "emailAddress", "active.date." + ts + "@example.com",
                            "telephoneNumber", "020 1111 2222"))))
            .when()
            .post("/provider-firms")
            .then()
            .statusCode(201)
            .extract()
            .path("data.providerFirmNumber");

    given()
        .pathParam("firmId", firmNumber)
        .when()
        .get("/provider-firms/{firmId}")
        .then()
        .statusCode(200)
        .body(
            "data.legalServicesProvider.headOffice.liaisonManager.activeDateFrom", notNullValue());
  }

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
                Map.of("useDefaultContractManager", true),
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

  /** AC1 (DSTEW-1640) - Successful bank account creation records audit fields. */
  @Test
  void createLspFirmWithEftBankAccount_getBankDetailsReturnsAuditFields() {
    long timestamp = System.currentTimeMillis();
    String firmName = "E2E-DSTEW LSP EFT " + timestamp;
    String accountNumber = "8" + (timestamp % 10_000_000L);

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
                Map.of(
                    "paymentMethod",
                    "EFT",
                    "bankAccountDetails",
                    Map.of(
                        "accountName", "Audit Test Account",
                        "sortCode", "601111",
                        "accountNumber", accountNumber)),
                "contractManager",
                Map.of("useDefaultContractManager", true),
                "liaisonManager",
                Map.of(
                    "firstName", "Test",
                    "lastName", "Manager",
                    "emailAddress", "test.manager@example.com",
                    "telephoneNumber", "020 1111 2222")));

    String firmNumber =
        given()
            .contentType(ContentType.JSON)
            .body(body)
            .when()
            .post("/provider-firms")
            .then()
            .statusCode(201)
            .extract()
            .path("data.providerFirmNumber");

    given()
        .pathParam("firmId", firmNumber)
        .queryParam("bankAccountNumber", accountNumber)
        .when()
        .get("/provider-firms/{firmId}/bank-details")
        .then()
        .statusCode(200)
        .body("data.metadata.pagination.totalItems", equalTo(1))
        .body("data.content[0].accountNumber", equalTo(accountNumber))
        .body("data.content[0].createdBy", notNullValue())
        .body("data.content[0].createdTimestamp", notNullValue())
        .body("data.content[0].lastUpdatedBy", notNullValue())
        .body("data.content[0].lastUpdatedTimestamp", notNullValue());
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

  /// POSTs an LSP firm with the `contractManager` field omitted entirely (not just null);
  /// expects 400 and verifies no firm is persisted. `contractManager` is a mandatory field for
  /// LSPs (BR-21).
  @Test
  void createLspFirm_contractManagerOmittedEntirely_returns400AndFirmNotCreated() {
    long ts = System.currentTimeMillis();
    String firmName = "E2E-OMITTED-CM " + ts;

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
                        "line1", "1 Omitted CM Street",
                        "townOrCity", "London",
                        "postcode", "EC1A 1BB"),
                    "payment",
                    Map.of("paymentMethod", "CHECK"),
                    "liaisonManager",
                    Map.of(
                        "firstName", "Omitted",
                        "lastName", "CmTest",
                        "emailAddress", "omitted.cm." + ts + "@example.com",
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
                Map.of("useDefaultContractManager", true)));

    given()
        .contentType(ContentType.JSON)
        .body(body)
        .when()
        .post("/provider-firms")
        .then()
        .statusCode(400);
  }

  /**
   * AC1 - Verifies that a chambers firm can be created successfully. AC4 – Neither DX Number nor DX
   * Centre provided
   */
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

  /**
   * AC2 - Verifies that attempting to create a chambers firm without providing a required variant/
   * firm type field results in a 400 Bad Request response. (BR02)
   */
  @Test
  void createChambersFirm_missingType_returns400() {
    Map<String, Object> body =
        Map.of(
            "firmType",
            "",
            "name",
            "E2E-DSTEW Chambers " + System.currentTimeMillis(),
            "chambers",
            Map.of(
                "line1",
                "2 Chambers Court",
                "townOrCity",
                "London",
                "postcode",
                "WC2A 3EB",
                "headOffice",
                true,
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
        .queryParam("name", body.get("name"))
        .when()
        .get("/provider-firms")
        .then()
        .statusCode(200)
        .body("data.metadata.pagination.totalItems", equalTo(0));
  }

  /**
   * AC2 - Verifies that attempting to create a chambers firm with an invalid firm type results in a
   * 400 response.
   */
  @Test
  void createChambersFirm_wrongFirmType_returns400() {
    Map<String, Object> body =
        Map.of(
            "firmType",
            "Legal Services Provider",
            "name",
            "E2E-DSTEW Chambers " + System.currentTimeMillis(),
            "chambers",
            Map.of(
                "address",
                Map.of(
                    "line1", "2 Chambers Court",
                    "townOrCity", "London",
                    "postcode", "WC2A 3EB")),
            "liaisonManager",
            Map.of(
                "firstName", "Chambers",
                "lastName", "Liaison",
                "emailAddress", "chambers.liaison@example.com",
                "telephoneNumber", "020 3333 4444"));

    given()
        .contentType(ContentType.JSON)
        .body(body)
        .when()
        .post("/provider-firms")
        .then()
        .statusCode(400);

    given()
        .queryParam("name", body.get("name"))
        .when()
        .get("/provider-firms")
        .then()
        .statusCode(200)
        .body("data.metadata.pagination.totalItems", equalTo(0));
  }

  /** AC3 – DX Number and DX Centre provided together. */
  @Test
  void createChambersFirmWithDxDetails_returns201ThenGetReturnsCreatedFirm() {
    String firmName = "E2E-DSTEW Chambers " + System.currentTimeMillis();
    String dxNumber = "DX 13009";
    String dxCenter = "Birmingham";

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
                "dxDetails",
                Map.of(
                    "dxNumber", dxNumber,
                    "dxCentre", dxCenter),
                "liaisonManager",
                Map.of(
                    "firstName", "Chambers",
                    "lastName", "Liaison",
                    "emailAddress", "chambers.liaison@example.com",
                    "telephoneNumber", "020 3333 4444")));

    String firmNumber =
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
            .path("data.providerFirmNumber");

    String officeAccountNumber =
        given()
            .pathParam("firmId", firmNumber)
            .when()
            .get("/provider-firms/{firmId}")
            .then()
            .statusCode(200)
            .body("data.firmNumber", equalTo(firmNumber))
            .body("data.firmType", equalTo("Chambers"))
            .body("data.chambers.office.accountNumber", notNullValue())
            .extract()
            .path("data.chambers.office.accountNumber");

    // Verify DX details were persisted correctly on the head office
    given()
        .pathParam("firmId", firmNumber)
        .pathParam("officeCode", officeAccountNumber)
        .when()
        .get("/provider-firms/{firmId}/offices/{officeCode}")
        .then()
        .statusCode(200)
        .body("data.dxDetails.dxNumber", equalTo(dxNumber))
        .body("data.dxDetails.dxCentre", equalTo(dxCenter));
  }

  /**
   * AC5 Verifies that attempting to create a chambers provider with DX details, Number but missing
   * Centre results in a 400 Bad Request response.
   */
  @Test
  void createChambersDxDetailsWithNumberButNoCentre_returns400() {
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
                "dxDetails",
                Map.of("dxNumber", "DX 13009"),
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

  /**
   * AC6- Verifies that attempting to create a chambers provider with DX details, dxCentre, but
   * missing dxNumber results in a 400 Bad Request response.
   */
  @Test
  void createChambersDxDetailsWithCentreButNoNumber_returns400() {
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
                "dxDetails",
                Map.of("dxCentre", "Birmingham"),
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

  /**
   * AC7 - Verifies that attempting to create a chambers firm without providing an address results
   * in a 409. AC8 – No partial Chambers records.
   */
  @Test
  void createChambersFirm_missingAddress_returns409() {
    String firmName = "E2E-DSTEW Chambers " + System.currentTimeMillis();
    Map<String, Object> body =
        Map.of(
            "firmType",
            "Chambers",
            "name",
            firmName,
            "chambers",
            Map.of(
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
        .statusCode(409);

    // AC8 – Confirm no partial record was persisted
    given()
        .queryParam("name", firmName)
        .when()
        .get("/provider-firms")
        .then()
        .statusCode(200)
        .body("data.metadata.pagination.totalItems", equalTo(0));
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

    String firmName = "Bad Firm";
    Map<String, Object> body =
        Map.of(
            "firmType",
            "Legal Services Provider",
            "name",
            firmName,
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

    given()
        .queryParam("name", firmName)
        .when()
        .get("/provider-firms")
        .then()
        .statusCode(200)
        .body("data.metadata.pagination.totalItems", equalTo(0));
  }

  /// POSTs an LSP firm with `contractManager.useDefaultContractManager: true`; verifies the head
  /// office is assigned the system default Contract Manager.
  @Test
  void createLspFirm_useDefaultContractManager_assignsDefaultCm() {
    long ts = System.currentTimeMillis();

    Response response =
        given()
            .contentType(ContentType.JSON)
            .body(
                Map.of(
                    "firmType",
                    "Legal Services Provider",
                    "name",
                    "E2E-DEFAULT-CM " + ts,
                    "legalServicesProvider",
                    Map.of(
                        "constitutionalStatus",
                        "Partnership",
                        "address",
                        Map.of(
                            "line1", "1 Default CM Street",
                            "townOrCity", "London",
                            "postcode", "EC1A 1BB"),
                        "payment",
                        Map.of("paymentMethod", "CHECK"),
                        "contractManager",
                        Map.of("useDefaultContractManager", true),
                        "liaisonManager",
                        Map.of(
                            "firstName", "Default",
                            "lastName", "CmTest",
                            "emailAddress", "default.cm." + ts + "@example.com",
                            "telephoneNumber", "020 1111 2222"))))
            .when()
            .post("/provider-firms")
            .then()
            .statusCode(201)
            .extract()
            .response();

    String firmNumber = response.path("data.providerFirmNumber");
    String headOfficeCode =
        given()
            .pathParam("firmId", firmNumber)
            .when()
            .get("/provider-firms/{firmId}/offices")
            .then()
            .statusCode(200)
            .extract()
            .path("data.content[0].accountNumber");

    given()
        .pathParam("firmId", firmNumber)
        .pathParam("officeCode", headOfficeCode)
        .when()
        .get("/provider-firms/{firmId}/offices/{officeCode}/contract-managers")
        .then()
        .statusCode(200)
        .body("data.content[0].contractManagerId", equalTo(E2eConfig.defaultContractManagerId()));
  }

  /// POSTs an LSP firm with `contractManager.contractManagerGUID: null` (i.e. the object is
  /// present but neither option is actually supplied); expects 400 and verifies no firm is
  /// persisted.
  @Test
  void createLspFirm_contractManagerGUIDExplicitlyNull_returns400AndFirmNotCreated() {
    long ts = System.currentTimeMillis();
    String firmName = "E2E-NULL-CM " + ts;

    // Map.of(...) rejects null values, so HashMap is used to serialise an explicit
    // contractManagerGUID: null.
    Map<String, Object> contractManager = new HashMap<>();
    contractManager.put("contractManagerGUID", null);

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
                        "line1", "1 Null CM Street",
                        "townOrCity", "London",
                        "postcode", "EC1A 1BB"),
                    "payment",
                    Map.of("paymentMethod", "CHECK"),
                    "contractManager",
                    contractManager,
                    "liaisonManager",
                    Map.of(
                        "firstName", "Null",
                        "lastName", "CmTest",
                        "emailAddress", "null.cm." + ts + "@example.com",
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

  /// POSTs a Chambers firm with `contractManager.useDefaultContractManager: true`; verifies the
  /// office is assigned the system default Contract Manager. Unlike LSP, `contractManager` is
  /// optional for Chambers (BR-21 only mandates a Contract Manager for LSPs), but the same two
  /// options are supported when it is supplied.
  @Test
  void createChambersFirm_useDefaultContractManager_assignsDefaultCm() {
    long ts = System.currentTimeMillis();

    String firmNumber =
        given()
            .contentType(ContentType.JSON)
            .body(
                Map.of(
                    "firmType",
                    "Chambers",
                    "name",
                    "E2E-CHAMBERS-DEFAULT-CM " + ts,
                    "chambers",
                    Map.of(
                        "address",
                        Map.of(
                            "line1", "1 Chambers Default CM Street",
                            "townOrCity", "London",
                            "postcode", "EC1A 1BB"),
                        "contractManager",
                        Map.of("useDefaultContractManager", true),
                        "liaisonManager",
                        Map.of(
                            "firstName", "Chambers",
                            "lastName", "DefaultCmTest",
                            "emailAddress", "chambers.default.cm." + ts + "@example.com",
                            "telephoneNumber", "020 1111 2222"))))
            .when()
            .post("/provider-firms")
            .then()
            .statusCode(201)
            .extract()
            .path("data.providerFirmNumber");

    String officeCode =
        given()
            .pathParam("firmId", firmNumber)
            .when()
            .get("/provider-firms/{firmId}/offices")
            .then()
            .statusCode(200)
            .extract()
            .path("data.content[0].accountNumber");

    given()
        .pathParam("firmId", firmNumber)
        .pathParam("officeCode", officeCode)
        .when()
        .get("/provider-firms/{firmId}/offices/{officeCode}/contract-managers")
        .then()
        .statusCode(200)
        .body("data.content[0].contractManagerId", equalTo(E2eConfig.defaultContractManagerId()));
  }

  /// POSTs a Chambers firm without a `contractManager` field at all; verifies the firm is still
  /// created successfully (the field remains optional for Chambers) with no Contract Manager
  /// assigned.
  @Test
  void createChambersFirm_noContractManager_stillSucceedsWithNoCmAssigned() {
    long ts = System.currentTimeMillis();

    String firmNumber =
        given()
            .contentType(ContentType.JSON)
            .body(
                Map.of(
                    "firmType",
                    "Chambers",
                    "name",
                    "E2E-CHAMBERS-NO-CM " + ts,
                    "chambers",
                    Map.of(
                        "address",
                        Map.of(
                            "line1", "1 Chambers No CM Street",
                            "townOrCity", "London",
                            "postcode", "EC1A 1BB"),
                        "liaisonManager",
                        Map.of(
                            "firstName", "Chambers",
                            "lastName", "NoCmTest",
                            "emailAddress", "chambers.no.cm." + ts + "@example.com",
                            "telephoneNumber", "020 1111 2222"))))
            .when()
            .post("/provider-firms")
            .then()
            .statusCode(201)
            .extract()
            .path("data.providerFirmNumber");

    String officeCode =
        given()
            .pathParam("firmId", firmNumber)
            .when()
            .get("/provider-firms/{firmId}/offices")
            .then()
            .statusCode(200)
            .extract()
            .path("data.content[0].accountNumber");

    given()
        .pathParam("firmId", firmNumber)
        .pathParam("officeCode", officeCode)
        .when()
        .get("/provider-firms/{firmId}/offices/{officeCode}/contract-managers")
        .then()
        .statusCode(200)
        .body("data.content", empty());
  }

  /// POSTs a Chambers firm with `contractManager.contractManagerGUID: null`; expects 400 and
  /// verifies no firm is persisted, mirroring the LSP behaviour.
  @Test
  void createChambersFirm_contractManagerGUIDExplicitlyNull_returns400AndFirmNotCreated() {
    long ts = System.currentTimeMillis();
    String firmName = "E2E-CHAMBERS-NULL-CM " + ts;

    // Map.of(...) rejects null values, so HashMap is used to serialise an explicit
    // contractManagerGUID: null.
    Map<String, Object> contractManager = new HashMap<>();
    contractManager.put("contractManagerGUID", null);

    given()
        .contentType(ContentType.JSON)
        .body(
            Map.of(
                "firmType",
                "Chambers",
                "name",
                firmName,
                "chambers",
                Map.of(
                    "address",
                    Map.of(
                        "line1", "1 Chambers Null CM Street",
                        "townOrCity", "London",
                        "postcode", "EC1A 1BB"),
                    "contractManager",
                    contractManager,
                    "liaisonManager",
                    Map.of(
                        "firstName", "Chambers",
                        "lastName", "NullCmTest",
                        "emailAddress", "chambers.null.cm." + ts + "@example.com",
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
}
