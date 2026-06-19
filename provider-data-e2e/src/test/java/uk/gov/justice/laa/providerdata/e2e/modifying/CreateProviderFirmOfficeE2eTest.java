package uk.gov.justice.laa.providerdata.e2e.modifying;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
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
 * <p>Most tests create a new office linked to the E2E LSP provider and verify it via GET. The
 * DSTEW-1651 tests create isolated firms so that head-office LM state is fully controlled.
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

  /**
   * AC1 – Default parent Liaison Manager when none is provided for the child office.
   *
   * <p>DS_MAPD_FR_024 (DSTEW-1651): When a child office is created using {@code
   * useHeadOfficeLiaisonManager: true}, PDP must assign the parent LSP's active Liaison Manager to
   * that office. The resulting link must have {@code linkedFlag=true} to indicate it was inherited
   * rather than office-specific.
   */
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

  /**
   * AC2 – Office-specific Liaison Manager is used when explicitly provided; parent default is not
   * applied.
   *
   * <p>DS_MAPD_FR_024 (DSTEW-1651): When a child office is created with a new {@code
   * LiaisonManagerCreateV2} body, PDP must use that LM and must not substitute the parent's LM. The
   * resulting link must have {@code linkedFlag=false}.
   */
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

  /**
   * AC3 – Defaulting is one-off at creation; later changes to the parent LM do not propagate.
   *
   * <p>DS_MAPD_FR_024 (DSTEW-1651): After a child office inherits the head office's LM, replacing
   * the head office's LM must not affect the child office — its active assignment remains the
   * original LM.
   */
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
