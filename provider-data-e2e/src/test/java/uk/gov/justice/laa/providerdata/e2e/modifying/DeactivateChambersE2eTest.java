package uk.gov.justice.laa.providerdata.e2e.modifying;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsStringIgnoringCase;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

import io.restassured.http.ContentType;
import io.restassured.response.Response;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import uk.gov.justice.laa.providerdata.e2e.ModifyingTest;

/**
 * Data-modifying e2e tests for DS_MAPD_FR_019: prevent a Chambers from being made inactive while
 * active practitioners are linked.
 *
 * <p>Two fresh Chambers firms are created in {@link #createFixtures()}:
 *
 * <ul>
 *   <li><b>Empty Chambers</b> — no practitioners linked; used for AC1.
 *   <li><b>Chambers with practitioner</b> — one active practitioner linked; used for AC2–AC4.
 * </ul>
 *
 * <p>Test data is prefixed with {@code "E2E-DSTEW-1557 "}; reset the local database to remove it.
 *
 * <p>Note: {@code firmType=Advocate} is the technical value for what the business calls a
 * Practitioner. It covers both Advocates and Barristers, subdivided by {@code advocateType}.
 */
@ModifyingTest
@DisplayName("DSTEW-1557: Prevent Chambers deactivation with active practitioners (DS_MAPD_FR_019)")
class DeactivateChambersE2eTest {

  private static String emptyChambersNumber;
  private static String emptyChambersOfficeCode;

  private static String populatedChambersNumber;
  private static String populatedChambersOfficeCode;

  @BeforeAll
  static void createFixtures() {
    long ts = System.currentTimeMillis();

    // Chambers with no practitioners — for AC1
    Response emptyCreate =
        given()
            .contentType(ContentType.JSON)
            .body(
                Map.of(
                    "firmType",
                    "Chambers",
                    "name",
                    "E2E-DSTEW-1557 Chambers Empty " + ts,
                    "chambers",
                    Map.of(
                        "address",
                        Map.of(
                            "line1", "1 Empty Street",
                            "townOrCity", "London",
                            "postcode", "WC1A 1AA"),
                        "liaisonManager",
                        Map.of(
                            "firstName", "Empty",
                            "lastName", "Liaison",
                            "emailAddress", "empty.liaison@example.com",
                            "telephoneNumber", "020 1111 2222"))))
            .when()
            .post("/provider-firms")
            .then()
            .statusCode(201)
            .body("data.providerFirmNumber", notNullValue())
            .extract()
            .response();

    emptyChambersNumber = emptyCreate.path("data.providerFirmNumber");
    emptyChambersOfficeCode =
        given()
            .pathParam("firmId", emptyChambersNumber)
            .when()
            .get("/provider-firms/{firmId}/offices")
            .then()
            .statusCode(200)
            .extract()
            .path("data.content[0].accountNumber");

    // Chambers with one active practitioner — for AC2–AC4
    Response populatedCreate =
        given()
            .contentType(ContentType.JSON)
            .body(
                Map.of(
                    "firmType",
                    "Chambers",
                    "name",
                    "E2E-DSTEW-1557 Chambers Populated " + ts,
                    "chambers",
                    Map.of(
                        "address",
                        Map.of(
                            "line1", "1 Populated Street",
                            "townOrCity", "London",
                            "postcode", "WC1A 1BB"),
                        "liaisonManager",
                        Map.of(
                            "firstName", "Populated",
                            "lastName", "Liaison",
                            "emailAddress", "populated.liaison@example.com",
                            "telephoneNumber", "020 1111 3333"))))
            .when()
            .post("/provider-firms")
            .then()
            .statusCode(201)
            .body("data.providerFirmNumber", notNullValue())
            .extract()
            .response();

    populatedChambersNumber = populatedCreate.path("data.providerFirmNumber");
    populatedChambersOfficeCode =
        given()
            .pathParam("firmId", populatedChambersNumber)
            .when()
            .get("/provider-firms/{firmId}/offices")
            .then()
            .statusCode(200)
            .extract()
            .path("data.content[0].accountNumber");

    // Create one active practitioner (firmType=Advocate) linked to the populated Chambers.
    given()
        .contentType(ContentType.JSON)
        .body(
            Map.of(
                "firmType",
                "Advocate",
                "name",
                "E2E-DSTEW-1557 Practitioner " + ts,
                "practitioner",
                Map.of(
                    "parentFirms",
                    List.of(Map.of("parentFirmNumber", populatedChambersNumber)),
                    "advocateType",
                    "Advocate",
                    "advocate",
                    Map.of(
                        "advocateLevel",
                        "Junior",
                        "solicitorRegulationAuthorityRollNumber",
                        "SRA" + ts),
                    "payment",
                    Map.of("paymentMethod", "CHECK"),
                    "liaisonManager",
                    Map.of(
                        "firstName", "Practitioner",
                        "lastName", "Liaison",
                        "emailAddress", "practitioner.liaison@example.com",
                        "telephoneNumber", "020 1111 4444"))))
        .when()
        .post("/provider-firms")
        .then()
        .statusCode(201);
  }

  /**
   * AC1 – Apply inactive date when no associated active practitioners exist.
   *
   * <p>A Chambers with no linked practitioners can be made inactive by setting {@code
   * activeDateTo}. The change is verified via a subsequent GET.
   */
  @Test
  void dstew1557_ac1_noActivePractitioners_deactivationSucceeds() {
    String activeDateTo = LocalDate.now().toString();

    given()
        .contentType(ContentType.JSON)
        .pathParam("firmId", emptyChambersNumber)
        .pathParam("officeCode", emptyChambersOfficeCode)
        .body(Map.of("activeDateTo", activeDateTo))
        .when()
        .patch("/provider-firms/{firmId}/offices/{officeCode}")
        .then()
        .statusCode(200);

    // Verify the inactive date was persisted and pre-existing fields are unchanged.
    given()
        .pathParam("firmId", emptyChambersNumber)
        .pathParam("officeCode", emptyChambersOfficeCode)
        .when()
        .get("/provider-firms/{firmId}/offices/{officeCode}")
        .then()
        .statusCode(200)
        .body("data.accountNumber", equalTo(emptyChambersOfficeCode))
        .body("data.activeDateTo", equalTo(activeDateTo))
        .body("data.address.line1", equalTo("1 Empty Street"))
        .body("data.address.townOrCity", equalTo("London"))
        .body("data.address.postcode", equalTo("WC1A 1AA"));
  }

  /**
   * AC2 – Prevent inactive date when associated active practitioners exist.
   *
   * <p>A Chambers with at least one active practitioner must not be made inactive. Verifies the
   * record remains unchanged after the rejection.
   */
  @Test
  void dstew1557_ac2_withActivePractitioners_deactivationIsRejectedAndUnchanged() {
    given()
        .contentType(ContentType.JSON)
        .pathParam("firmId", populatedChambersNumber)
        .pathParam("officeCode", populatedChambersOfficeCode)
        .body(Map.of("activeDateTo", LocalDate.now().toString()))
        .when()
        .patch("/provider-firms/{firmId}/offices/{officeCode}")
        .then()
        .statusCode(400);

    assertPopulatedOfficeUnchanged();
  }

  /**
   * AC3 – No partial updates when deactivation is rejected.
   *
   * <p>After a rejected deactivation attempt the office must remain fully unchanged.
   */
  @Test
  void dstew1557_ac3_rejectedDeactivation_leavesRecordUnchanged() {
    given()
        .contentType(ContentType.JSON)
        .pathParam("firmId", populatedChambersNumber)
        .pathParam("officeCode", populatedChambersOfficeCode)
        .body(Map.of("activeDateTo", LocalDate.now().toString()))
        .when()
        .patch("/provider-firms/{firmId}/offices/{officeCode}")
        .then()
        .statusCode(400);

    assertPopulatedOfficeUnchanged();
  }

  /**
   * AC4 – Clear feedback when deactivation is blocked.
   *
   * <p>The 400 response must contain a message indicating why deactivation was blocked: the
   * Chambers has active practitioners that must be deactivated or reassigned first. Verifies the
   * record remains unchanged after the rejection.
   */
  @Test
  void dstew1557_ac4_rejectedDeactivation_responseBodyMentionsPractitioners() {
    given()
        .contentType(ContentType.JSON)
        .pathParam("firmId", populatedChambersNumber)
        .pathParam("officeCode", populatedChambersOfficeCode)
        .body(Map.of("activeDateTo", LocalDate.now().toString()))
        .when()
        .patch("/provider-firms/{firmId}/offices/{officeCode}")
        .then()
        .statusCode(400)
        .body(
            "detail",
            allOf(
                containsStringIgnoringCase("deactivate"),
                containsStringIgnoringCase("active practitioners"),
                containsStringIgnoringCase("Chambers")));

    assertPopulatedOfficeUnchanged();
  }

  private void assertPopulatedOfficeUnchanged() {
    given()
        .pathParam("firmId", populatedChambersNumber)
        .pathParam("officeCode", populatedChambersOfficeCode)
        .when()
        .get("/provider-firms/{firmId}/offices/{officeCode}")
        .then()
        .statusCode(200)
        .body("data.accountNumber", equalTo(populatedChambersOfficeCode))
        .body("data.activeDateTo", nullValue())
        .body("data.address.line1", equalTo("1 Populated Street"))
        .body("data.address.townOrCity", equalTo("London"))
        .body("data.address.postcode", equalTo("WC1A 1BB"));
  }
}
