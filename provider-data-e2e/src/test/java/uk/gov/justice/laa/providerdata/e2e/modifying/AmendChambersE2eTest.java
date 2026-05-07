package uk.gov.justice.laa.providerdata.e2e.modifying;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

import io.restassured.http.ContentType;
import io.restassured.response.Response;
import java.time.LocalDate;
import java.util.Map;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import uk.gov.justice.laa.providerdata.e2e.ModifyingTest;

/**
 * Data-modifying e2e tests for amending an existing Legal Organisation (Chambers) record
 * (DS_MAPD_FR_017).
 *
 * <p>A "Legal Organisation (Chambers) record" in business terms spans:
 *
 * <ul>
 *   <li>the {@code Provider} entity ({@code firmType=Chambers}) — carries the legal organisation
 *       name
 *   <li>the linked {@code Office} entity — carries address, contact, and DX details
 * </ul>
 *
 * <p>Tests covering AC1–AC7 from DSTEW-1555. A fresh Chambers firm is created in {@link
 * #createChambersFirm()} and its head office code is then found by a GET. Test data is prefixed
 * with {@code "E2E-DSTEW-1555 "}.
 *
 * <p>The QA overview for DSTEW-1555 also lists "Validate rejection of attempts to amend redacted
 * (immutable) fields". No dedicated test exists for this because the immutable fields — Legal
 * Organisation GUID, Account Number, Firm Number, Head Office Flag, and Active From Date — are not
 * present in any of the patch request schemas. So the OpenAPI spec doesn't provide any way for them
 * to be changed and there's no service code to be checked with an E2E test.
 */
@ModifyingTest
@DisplayName("DSTEW-1555: Amend Chambers record (DS_MAPD_FR_017)")
class AmendChambersE2eTest {

  private static String firmNumber;
  private static String officeCode;

  @BeforeAll
  static void createChambersFirm() {
    String firmName = "E2E-DSTEW-1555 Chambers " + System.currentTimeMillis();

    // AC1 – Successful Chambers amendment with mandatory data preserved.
    Response createResponse =
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
                            "line1", "1 Original Street",
                            "townOrCity", "London",
                            "postcode", "WC1A 1AA"),
                        "liaisonManager",
                        Map.of(
                            "firstName", "Original",
                            "lastName", "Liaison",
                            "emailAddress", "original.liaison@example.com",
                            "telephoneNumber", "020 1111 2222"))))
            .when()
            .post("/provider-firms")
            .then()
            .statusCode(201)
            .body("data.providerFirmGUID", notNullValue())
            .body("data.providerFirmNumber", notNullValue())
            .extract()
            .response();

    firmNumber = createResponse.path("data.providerFirmNumber");

    officeCode =
        given()
            .pathParam("firmId", firmNumber)
            .when()
            .get("/provider-firms/{firmId}/offices")
            .then()
            .statusCode(200)
            .extract()
            .path("data.content[0].accountNumber");
  }

  /**
   * AC1 – Successful Chambers amendment with mandatory data preserved.
   *
   * <p>Amends the legal organisation name (Provider entity) and office details (address, telephone,
   * email address), verifying both the response and the persisted state via GET.
   */
  @Test
  void dstew1555_ac1_withMandatoryDataPreserved_amendmentIsAcceptedAndPersisted() {
    // Update the legal organisation name
    given()
        .contentType(ContentType.JSON)
        .pathParam("firmId", firmNumber)
        .body(Map.of("name", "E2E-DSTEW-1555 Chambers Amended " + System.currentTimeMillis()))
        .when()
        .patch("/provider-firms/{firmId}")
        .then()
        .statusCode(200)
        .body("data.providerFirmNumber", equalTo(firmNumber));

    // Update office details (address, telephone, email)
    given()
        .contentType(ContentType.JSON)
        .pathParam("firmId", firmNumber)
        .pathParam("officeCode", officeCode)
        .body(
            Map.of(
                "address",
                Map.of(
                    "line1", "2 Amended Street",
                    "townOrCity", "Manchester",
                    "postcode", "M1 1AA"),
                "telephoneNumber",
                "0161 222 3333",
                "emailAddress",
                "amended@example.com"))
        .when()
        .patch("/provider-firms/{firmId}/offices/{officeCode}")
        .then()
        .statusCode(200)
        .body("data.providerFirmNumber", equalTo(firmNumber))
        .body("data.officeCode", equalTo(officeCode));

    // Verify the changes were persisted.
    given()
        .pathParam("firmId", firmNumber)
        .pathParam("officeCode", officeCode)
        .when()
        .get("/provider-firms/{firmId}/offices/{officeCode}")
        .then()
        .statusCode(200)
        .body("data.accountNumber", equalTo(officeCode))
        .body("data.address.line1", equalTo("2 Amended Street"))
        .body("data.address.townOrCity", equalTo("Manchester"))
        .body("data.address.postcode", equalTo("M1 1AA"))
        .body("data.telephoneNumber", equalTo("0161 222 3333"))
        .body("data.emailAddress", equalTo("amended@example.com"));
  }

  /**
   * AC2 – Provider type cannot be changed: LSP patch body rejected and record unchanged.
   *
   * <p>Sending a {@code legalServicesProvider} patch body to a Chambers firm must be rejected. The
   * provider type is determined by the entity subtype and cannot be changed via an amendment.
   */
  @Test
  void dstew1555_ac2_sendingLspPatchToChambersProvider_amendmentIsRejectedAndUnchanged() {
    String originalName = fetchProviderName();

    given()
        .contentType(ContentType.JSON)
        .pathParam("firmId", firmNumber)
        .body(Map.of("legalServicesProvider", Map.of("constitutionalStatus", "Partnership")))
        .when()
        .patch("/provider-firms/{firmId}")
        .then()
        .statusCode(400);

    assertProviderUnchanged(originalName);
  }

  /**
   * AC2 – Provider type cannot be changed: Advocate patch body rejected and record unchanged.
   *
   * <p>Sending an {@code advocate} patch body to a Chambers firm must likewise be rejected.
   */
  @Test
  void dstew1555_ac2_sendingAdvocatePatchToChambersProvider_amendmentIsRejectedAndUnchanged() {
    String originalName = fetchProviderName();

    given()
        .contentType(ContentType.JSON)
        .pathParam("firmId", firmNumber)
        .body(Map.of("advocate", Map.of("advocateType", "Barrister")))
        .when()
        .patch("/provider-firms/{firmId}")
        .then()
        .statusCode(400);

    assertProviderUnchanged(originalName);
  }

  /**
   * AC2 – Provider type cannot be changed: null chambers body rejected and record unchanged.
   *
   * <p>Attempting to nullify the {@code chambers} discriminator must be rejected and must not leave
   * the record in an inconsistent state.
   */
  @Test
  void dstew1555_ac2_sendingNullChambersPatchToChambersProvider_amendmentIsRejectedAndUnchanged() {
    String originalName = fetchProviderName();

    given()
        .contentType(ContentType.JSON)
        .pathParam("firmId", firmNumber)
        .body("{\"chambers\": null}")
        .when()
        .patch("/provider-firms/{firmId}")
        .then()
        .statusCode(400);

    assertProviderUnchanged(originalName);
  }

  /**
   * AC2 – Provider type cannot be changed: empty body rejected and record unchanged.
   *
   * <p>Sending no provider-type discriminator at all must be rejected. Kept distinct from the
   * {@code chambers: null} case because the two could diverge if the implementation changes.
   */
  @Test
  void dstew1555_ac2_sendingEmptyBodyToChambersProvider_amendmentIsRejectedAndUnchanged() {
    String originalName = fetchProviderName();

    given()
        .contentType(ContentType.JSON)
        .pathParam("firmId", firmNumber)
        .body("{}")
        .when()
        .patch("/provider-firms/{firmId}")
        .then()
        .statusCode(400);

    assertProviderUnchanged(originalName);
  }

  /**
   * AC3 – DX Number and DX Centre provided together: amendment accepted and persisted.
   *
   * <p>Both DX fields are conditional on each other. Providing both is valid (BR11).
   */
  @Test
  void dstew1555_ac3_withBothDxFields_amendmentIsAcceptedAndPersisted() {
    given()
        .contentType(ContentType.JSON)
        .pathParam("firmId", firmNumber)
        .pathParam("officeCode", officeCode)
        .body(Map.of("dxDetails", Map.of("dxNumber", "DX 12345 LONDON", "dxCentre", "London 1")))
        .when()
        .patch("/provider-firms/{firmId}/offices/{officeCode}")
        .then()
        .statusCode(200);

    given()
        .pathParam("firmId", firmNumber)
        .pathParam("officeCode", officeCode)
        .when()
        .get("/provider-firms/{firmId}/offices/{officeCode}")
        .then()
        .statusCode(200)
        .body("data.dxDetails.dxNumber", equalTo("DX 12345 LONDON"))
        .body("data.dxDetails.dxCentre", equalTo("London 1"));
  }

  /**
   * AC4 – Neither DX Number nor DX Centre provided: amendment accepted and persisted.
   *
   * <p>Omitting {@code dxDetails} entirely is valid (BR11). Verifies the amendment is persisted and
   * no DX fields are introduced.
   */
  @Test
  void dstew1555_ac4_withNoDxFields_amendmentIsAcceptedAndPersisted() {
    given()
        .contentType(ContentType.JSON)
        .pathParam("firmId", firmNumber)
        .pathParam("officeCode", officeCode)
        .body(Map.of("telephoneNumber", "020 9999 8888"))
        .when()
        .patch("/provider-firms/{firmId}/offices/{officeCode}")
        .then()
        .statusCode(200);

    given()
        .pathParam("firmId", firmNumber)
        .pathParam("officeCode", officeCode)
        .when()
        .get("/provider-firms/{firmId}/offices/{officeCode}")
        .then()
        .statusCode(200)
        .body("data.telephoneNumber", equalTo("020 9999 8888"))
        .body("data.dxDetails", nullValue());
  }

  /**
   * AC5 – DX Number provided without DX Centre: amendment rejected and record unchanged.
   *
   * <p>Both DX fields are required when the {@code dxDetails} object is present (BR11).
   */
  @Test
  void dstew1555_ac5_withDxNumberButNoDxCentre_amendmentIsRejectedAndUnchanged() {
    Response before = fetchOfficeResponse();
    String originalDxNumber = before.path("data.dxDetails.dxNumber");
    String originalDxCentre = before.path("data.dxDetails.dxCentre");

    given()
        .contentType(ContentType.JSON)
        .pathParam("firmId", firmNumber)
        .pathParam("officeCode", officeCode)
        .body(Map.of("dxDetails", Map.of("dxNumber", "DX 12345 LONDON")))
        .when()
        .patch("/provider-firms/{firmId}/offices/{officeCode}")
        .then()
        .statusCode(400);

    given()
        .pathParam("firmId", firmNumber)
        .pathParam("officeCode", officeCode)
        .when()
        .get("/provider-firms/{firmId}/offices/{officeCode}")
        .then()
        .statusCode(200)
        .body("data.dxDetails.dxNumber", equalTo(originalDxNumber))
        .body("data.dxDetails.dxCentre", equalTo(originalDxCentre));
  }

  /**
   * AC6 – DX Centre provided without DX Number: amendment rejected and record unchanged.
   *
   * <p>Both DX fields are required when the {@code dxDetails} object is present (BR11).
   */
  @Test
  void dstew1555_ac6_withDxCentreButNoDxNumber_amendmentIsRejectedAndUnchanged() {
    Response before = fetchOfficeResponse();
    String originalDxNumber = before.path("data.dxDetails.dxNumber");
    String originalDxCentre = before.path("data.dxDetails.dxCentre");

    given()
        .contentType(ContentType.JSON)
        .pathParam("firmId", firmNumber)
        .pathParam("officeCode", officeCode)
        .body(Map.of("dxDetails", Map.of("dxCentre", "London 1")))
        .when()
        .patch("/provider-firms/{firmId}/offices/{officeCode}")
        .then()
        .statusCode(400);

    given()
        .pathParam("firmId", firmNumber)
        .pathParam("officeCode", officeCode)
        .when()
        .get("/provider-firms/{firmId}/offices/{officeCode}")
        .then()
        .statusCode(200)
        .body("data.dxDetails.dxNumber", equalTo(originalDxNumber))
        .body("data.dxDetails.dxCentre", equalTo(originalDxCentre));
  }

  /**
   * AC7 – No partial amendments applied on failed validation (BR18). ({@code activeDateTo} and
   * {@code clearActiveDateTo} together), which the service rejects. The address must remain as it
   * was before the request — no partial changes applied.
   */
  @Test
  void dstew1555_ac7_failedValidation_leavesRecordUnchanged() {
    // Read current state.
    String addressBeforePatch =
        given()
            .pathParam("firmId", firmNumber)
            .pathParam("officeCode", officeCode)
            .when()
            .get("/provider-firms/{firmId}/offices/{officeCode}")
            .then()
            .statusCode(200)
            .extract()
            .path("data.address.townOrCity");

    // Combine a valid address change with a conflicting activation request — must be rejected
    given()
        .contentType(ContentType.JSON)
        .pathParam("firmId", firmNumber)
        .pathParam("officeCode", officeCode)
        .body(
            Map.of(
                "address",
                Map.of(
                    "line1", "99 Should-Not-Be-Saved Street",
                    "townOrCity", "Sheffield",
                    "postcode", "S1 1AA"),
                "activeDateTo",
                LocalDate.now().toString(),
                "clearActiveDateTo",
                true))
        .when()
        .patch("/provider-firms/{firmId}/offices/{officeCode}")
        .then()
        .statusCode(400);

    // Check no partial changes were persisted.
    given()
        .pathParam("firmId", firmNumber)
        .pathParam("officeCode", officeCode)
        .when()
        .get("/provider-firms/{firmId}/offices/{officeCode}")
        .then()
        .statusCode(200)
        .body("data.address.townOrCity", equalTo(addressBeforePatch));
  }

  private String fetchProviderName() {
    return given()
        .pathParam("firmId", firmNumber)
        .when()
        .get("/provider-firms/{firmId}")
        .then()
        .statusCode(200)
        .extract()
        .path("data.name");
  }

  private void assertProviderUnchanged(String expectedName) {
    given()
        .pathParam("firmId", firmNumber)
        .when()
        .get("/provider-firms/{firmId}")
        .then()
        .statusCode(200)
        .body("data.firmNumber", equalTo(firmNumber))
        .body("data.name", equalTo(expectedName));
  }

  private Response fetchOfficeResponse() {
    return given()
        .pathParam("firmId", firmNumber)
        .pathParam("officeCode", officeCode)
        .when()
        .get("/provider-firms/{firmId}/offices/{officeCode}")
        .then()
        .statusCode(200)
        .extract()
        .response();
  }
}
