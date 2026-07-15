package uk.gov.justice.laa.providerdata.e2e.modifying;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

import io.restassured.http.ContentType;
import io.restassured.response.Response;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import uk.gov.justice.laa.providerdata.e2e.ModifyingTest;

/**
 * Data-modifying E2E tests for DS_MAPD_FR_029: Assign Chambers contact details to practitioner
 * (DSTEW-1741).
 *
 * <p>The ticket language is logical-model based: a "Legal Practitioner" maps to {@code
 * firmType=Advocate}, and "contact details" (address, email address, telephone number, DX number,
 * DX centre) are carried by the {@code Office} entity. A practitioner's head office link ({@code
 * AdvocateProviderOfficeLinkEntity}) points at the exact same {@code Office} row as its parent
 * Chamber's head office, so inheritance and dynamic synchronisation (AC1/AC2) happen automatically
 * with no copy step. Reassignment (AC3) requires the practitioner's office link to be repointed at
 * the new parent's office, which is what this ticket adds.
 *
 * <p>{@code officeGUID} in this API identifies the provider-office *link* record, not the
 * underlying office record (see the API description), so it differs between a Chambers office link
 * and a practitioner's office link even when they share the same underlying office. Contact-detail
 * inheritance is therefore verified here by comparing the actual field values returned by {@code
 * GET /provider-firms/{firmId}/offices/{officeCode}} for the practitioner against the parent
 * Chamber's office, not by comparing GUIDs.
 */
@ModifyingTest
@DisplayName("DSTEW-1741: Assign Chambers contact details to practitioner (DS_MAPD_FR_029)")
class PractitionerContactDetailsE2eTest {

  private ChambersFixture chambersA;
  private ChambersFixture chambersB;
  private ChambersFixture inactiveChambers;

  @BeforeEach
  void setup() {
    long ts = System.currentTimeMillis();
    chambersA = createChambers("A-" + ts, false);
    chambersB = createChambers("B-" + ts, false);
    inactiveChambers = createChambers("Inactive-" + ts, true);
  }

  /**
   * AC1 - Inherit contact details on creation.
   *
   * <p>A practitioner created under a Chambers parent inherits that Chambers' office contact
   * details (address, telephone, email, DX) because its office link points at the same underlying
   * office record. Also verifies the practitioner's own data (parent link, advocate details) is
   * correctly recorded and untouched by the inheritance mechanism.
   */
  @Test
  void dstew1741_ac1_createPractitioner_inheritsParentChambersContactDetails() {
    PractitionerFixture practitioner = createPractitioner(chambersA.firmNumber());

    given()
        .pathParam("firmId", practitioner.firmNumber())
        .when()
        .get("/provider-firms/{firmId}")
        .then()
        .statusCode(200)
        .body("data.firmType", equalTo("Advocate"))
        .body("data.practitioner.parentFirms", org.hamcrest.Matchers.hasSize(1))
        .body("data.practitioner.parentFirms[0].parentFirmNumber", equalTo(chambersA.firmNumber()))
        .body("data.practitioner.advocateType", equalTo("Advocate"))
        .body("data.practitioner.advocate.advocateLevel", equalTo("Junior"));

    Response practitionerOffice = fetchOffice(practitioner.firmNumber(), practitioner.officeCode());

    practitionerOffice
        .then()
        .statusCode(200)
        .body("data.address.line1", equalTo(chambersA.addressLine1()))
        .body("data.address.townOrCity", equalTo(chambersA.townOrCity()))
        .body("data.address.postcode", equalTo(chambersA.postcode()))
        .body("data.telephoneNumber", equalTo(chambersA.telephoneNumber()))
        .body("data.emailAddress", equalTo(chambersA.emailAddress()))
        .body("data.dxDetails.dxNumber", equalTo(chambersA.dxNumber()))
        .body("data.dxDetails.dxCentre", equalTo(chambersA.dxCentre()));
  }

  /**
   * AC2 - Reflect Chamber contact updates.
   *
   * <p>Amending the parent Chambers' office contact details is immediately reflected for an
   * existing practitioner, since both records share the same underlying office.
   */
  @Test
  void dstew1741_ac2_amendChambersOffice_practitionerReflectsUpdatedContactDetails() {
    PractitionerFixture practitioner = createPractitioner(chambersA.firmNumber());

    given()
        .contentType(ContentType.JSON)
        .pathParam("firmId", chambersA.firmNumber())
        .pathParam("officeCode", chambersA.officeCode())
        .body(
            Map.of(
                "address",
                Map.of(
                    "line1", "2 Updated Street",
                    "townOrCity", "Manchester",
                    "postcode", "M1 1AA"),
                "telephoneNumber",
                "0161 999 8888",
                "emailAddress",
                "updated.chambers@example.com",
                "dxDetails",
                Map.of("dxNumber", "DX 99999 MANCHESTER", "dxCentre", "Manchester 1")))
        .when()
        .patch("/provider-firms/{firmId}/offices/{officeCode}")
        .then()
        .statusCode(200);

    fetchOffice(practitioner.firmNumber(), practitioner.officeCode())
        .then()
        .statusCode(200)
        .body("data.address.line1", equalTo("2 Updated Street"))
        .body("data.address.townOrCity", equalTo("Manchester"))
        .body("data.address.postcode", equalTo("M1 1AA"))
        .body("data.telephoneNumber", equalTo("0161 999 8888"))
        .body("data.emailAddress", equalTo("updated.chambers@example.com"))
        .body("data.dxDetails.dxNumber", equalTo("DX 99999 MANCHESTER"))
        .body("data.dxDetails.dxCentre", equalTo("Manchester 1"));
  }

  /**
   * AC3 - Update contact details on Chamber reassignment.
   *
   * <p>Reassigning a practitioner's parent Chamber via PATCH repoints its office link to the new
   * parent's office, so contact details switch to match the new Chamber (BR-28).
   */
  @Test
  void dstew1741_ac3_reassignPractitioner_contactDetailsUpdateToNewChambers() {
    PractitionerFixture practitioner = createPractitioner(chambersA.firmNumber());

    given()
        .contentType(ContentType.JSON)
        .pathParam("firmId", practitioner.firmNumber())
        .body(
            Map.of(
                "practitioner",
                Map.of("parentFirms", List.of(Map.of("parentFirmNumber", chambersB.firmNumber())))))
        .when()
        .patch("/provider-firms/{firmId}")
        .then()
        .statusCode(200);

    fetchOffice(practitioner.firmNumber(), practitioner.officeCode())
        .then()
        .statusCode(200)
        .body("data.address.line1", equalTo(chambersB.addressLine1()))
        .body("data.address.townOrCity", equalTo(chambersB.townOrCity()))
        .body("data.address.postcode", equalTo(chambersB.postcode()))
        .body("data.telephoneNumber", equalTo(chambersB.telephoneNumber()))
        .body("data.emailAddress", equalTo(chambersB.emailAddress()))
        .body("data.dxDetails.dxNumber", equalTo(chambersB.dxNumber()))
        .body("data.dxDetails.dxCentre", equalTo(chambersB.dxCentre()));
  }

  /**
   * AC4 - Prevent independent contact data (creation).
   *
   * <p>A create request that attempts to supply contact-detail fields directly on the practitioner
   * is rejected — these fields don't exist in the practitioner schema, and are explicitly rejected
   * as unknown properties (see {@code RejectProperties} on {@code
   * ProviderCreatePractitionerV2.practitioner}).
   */
  @Test
  void dstew1741_ac4_createPractitioner_withIndependentContactData_returns400AndNotCreated() {
    long ts = System.currentTimeMillis();
    String firmName = "E2E-DSTEW-1741 AC4 Create " + ts;

    Map<String, Object> practitionerBody =
        new java.util.HashMap<>(
            Map.of(
                "parentFirms",
                List.of(Map.of("parentFirmNumber", chambersA.firmNumber())),
                "advocateType",
                "Advocate",
                "advocate",
                Map.of("advocateLevel", "Junior"),
                "payment",
                Map.of("paymentMethod", "CHECK"),
                "liaisonManager",
                Map.of("useChambersLiaisonManager", true)));
    practitionerBody.put(
        "address",
        Map.of("line1", "Independent Street", "townOrCity", "London", "postcode", "E1 1AA"));

    given()
        .contentType(ContentType.JSON)
        .body(Map.of("firmType", "Advocate", "name", firmName, "practitioner", practitionerBody))
        .when()
        .post("/provider-firms")
        .then()
        .statusCode(400)
        .body("error.errorCode", notNullValue());

    given()
        .queryParam("name", firmName)
        .when()
        .get("/provider-firms")
        .then()
        .statusCode(200)
        .body("data.metadata.pagination.totalItems", equalTo(0));
  }

  /**
   * AC4 - Prevent independent contact data (amendment).
   *
   * <p>A PATCH that attempts to supply contact-detail fields directly on the practitioner is
   * rejected, and the practitioner's inherited contact details remain unchanged.
   */
  @Test
  void dstew1741_ac4_amendPractitioner_withIndependentContactData_returns400AndUnchanged() {
    PractitionerFixture practitioner = createPractitioner(chambersA.firmNumber());

    given()
        .contentType(ContentType.JSON)
        .pathParam("firmId", practitioner.firmNumber())
        .body(
            Map.of(
                "practitioner",
                Map.of(
                    "emailAddress", "independent.practitioner@example.com",
                    "telephoneNumber", "020 5555 5555")))
        .when()
        .patch("/provider-firms/{firmId}")
        .then()
        .statusCode(400)
        .body("error.errorCode", notNullValue());

    fetchOffice(practitioner.firmNumber(), practitioner.officeCode())
        .then()
        .statusCode(200)
        .body("data.telephoneNumber", equalTo(chambersA.telephoneNumber()))
        .body("data.emailAddress", equalTo(chambersA.emailAddress()));
  }

  /**
   * AC5 - Enforce Chamber dependency for contact details (unknown parent).
   *
   * <p>Reassigning a practitioner to a non-existent Chamber is rejected, and the practitioner's
   * existing contact details remain unchanged.
   */
  @Test
  void dstew1741_ac5_reassignPractitioner_unknownParent_returns404AndUnchanged() {
    PractitionerFixture practitioner = createPractitioner(chambersA.firmNumber());

    given()
        .contentType(ContentType.JSON)
        .pathParam("firmId", practitioner.firmNumber())
        .body(
            Map.of(
                "practitioner",
                Map.of("parentFirms", List.of(Map.of("parentFirmNumber", "99999999")))))
        .when()
        .patch("/provider-firms/{firmId}")
        .then()
        .statusCode(404);

    fetchOffice(practitioner.firmNumber(), practitioner.officeCode())
        .then()
        .statusCode(200)
        .body("data.address.line1", equalTo(chambersA.addressLine1()))
        .body("data.telephoneNumber", equalTo(chambersA.telephoneNumber()))
        .body("data.emailAddress", equalTo(chambersA.emailAddress()))
        .body("data.dxDetails.dxNumber", equalTo(chambersA.dxNumber()));
  }

  /**
   * AC5 - Enforce Chamber dependency for contact details (non-Chambers parent).
   *
   * <p>Reassigning a practitioner to a firm that isn't a Chambers (e.g. a Legal Services Provider)
   * is rejected, and the practitioner's existing contact details remain unchanged.
   */
  @Test
  void dstew1741_ac5_reassignPractitioner_nonChambersParent_returns400AndUnchanged() {
    PractitionerFixture practitioner = createPractitioner(chambersA.firmNumber());
    String lspFirmNumber = createLsp("E2E-DSTEW-1741 AC5 LSP " + System.currentTimeMillis());

    given()
        .contentType(ContentType.JSON)
        .pathParam("firmId", practitioner.firmNumber())
        .body(
            Map.of(
                "practitioner",
                Map.of("parentFirms", List.of(Map.of("parentFirmNumber", lspFirmNumber)))))
        .when()
        .patch("/provider-firms/{firmId}")
        .then()
        .statusCode(400)
        .body("error.errorCode", notNullValue());

    fetchOffice(practitioner.firmNumber(), practitioner.officeCode())
        .then()
        .statusCode(200)
        .body("data.address.line1", equalTo(chambersA.addressLine1()))
        .body("data.telephoneNumber", equalTo(chambersA.telephoneNumber()))
        .body("data.emailAddress", equalTo(chambersA.emailAddress()))
        .body("data.dxDetails.dxNumber", equalTo(chambersA.dxNumber()));
  }

  /**
   * AC5 - Enforce Chamber dependency for contact details (inactive parent).
   *
   * <p>Reassigning a practitioner to an inactive Chambers record is rejected (BR-27), and the
   * practitioner's existing contact details remain unchanged.
   */
  @Test
  void dstew1741_ac5_reassignPractitioner_inactiveChambers_returns400AndUnchanged() {
    PractitionerFixture practitioner = createPractitioner(chambersA.firmNumber());

    given()
        .contentType(ContentType.JSON)
        .pathParam("firmId", practitioner.firmNumber())
        .body(
            Map.of(
                "practitioner",
                Map.of(
                    "parentFirms",
                    List.of(Map.of("parentFirmNumber", inactiveChambers.firmNumber())))))
        .when()
        .patch("/provider-firms/{firmId}")
        .then()
        .statusCode(400)
        .body("error.errorCode", notNullValue());

    fetchOffice(practitioner.firmNumber(), practitioner.officeCode())
        .then()
        .statusCode(200)
        .body("data.address.line1", equalTo(chambersA.addressLine1()))
        .body("data.telephoneNumber", equalTo(chambersA.telephoneNumber()))
        .body("data.emailAddress", equalTo(chambersA.emailAddress()))
        .body("data.dxDetails.dxNumber", equalTo(chambersA.dxNumber()));
  }

  /**
   * AC6 - No partial record if inheritance fails.
   *
   * <p>When a reassignment is rejected (invalid parent), the practitioner's parentFirms link and
   * office contact details both remain exactly as they were before the attempt — no partial state
   * is applied (BR-18).
   */
  @Test
  void dstew1741_ac6_failedReassignment_leavesParentAndContactDetailsUnchanged() {
    PractitionerFixture practitioner = createPractitioner(chambersA.firmNumber());

    given()
        .contentType(ContentType.JSON)
        .pathParam("firmId", practitioner.firmNumber())
        .body(
            Map.of(
                "practitioner",
                Map.of(
                    "parentFirms",
                    List.of(Map.of("parentFirmNumber", inactiveChambers.firmNumber())))))
        .when()
        .patch("/provider-firms/{firmId}")
        .then()
        .statusCode(400);

    given()
        .pathParam("firmId", practitioner.firmNumber())
        .when()
        .get("/provider-firms/{firmId}")
        .then()
        .statusCode(200)
        .body("data.practitioner.parentFirms", org.hamcrest.Matchers.hasSize(1))
        .body("data.practitioner.parentFirms[0].parentFirmNumber", equalTo(chambersA.firmNumber()));

    fetchOffice(practitioner.firmNumber(), practitioner.officeCode())
        .then()
        .statusCode(200)
        .body("data.address.line1", equalTo(chambersA.addressLine1()))
        .body("data.address.townOrCity", equalTo(chambersA.townOrCity()))
        .body("data.address.postcode", equalTo(chambersA.postcode()))
        .body("data.telephoneNumber", equalTo(chambersA.telephoneNumber()))
        .body("data.emailAddress", equalTo(chambersA.emailAddress()))
        .body("data.dxDetails.dxNumber", equalTo(chambersA.dxNumber()))
        .body("data.dxDetails.dxCentre", equalTo(chambersA.dxCentre()));
  }

  private ChambersFixture createChambers(String label, boolean inactive) {
    long ts = System.currentTimeMillis();
    String name = "E2E-DSTEW-1741 Chambers " + label;
    String addressLine1 = "1 " + label + " Street";
    String townOrCity = "London";
    String postcode = "WC1A 1AA";
    String telephoneNumber = "020 7" + (100000 + (ts % 900000)) + "";
    String emailAddress = "dstew1741.chambers." + ts + "." + label + "@example.com";
    String dxNumber = "DX " + (10000 + (ts % 90000)) + " " + label;
    String dxCentre = label + " Centre";

    String firmNumber =
        given()
            .contentType(ContentType.JSON)
            .body(
                Map.of(
                    "firmType",
                    "Chambers",
                    "name",
                    name,
                    "chambers",
                    Map.of(
                        "address",
                        Map.of(
                            "line1", addressLine1,
                            "townOrCity", townOrCity,
                            "postcode", postcode),
                        "telephoneNumber",
                        telephoneNumber,
                        "emailAddress",
                        emailAddress,
                        "dxDetails",
                        Map.of("dxNumber", dxNumber, "dxCentre", dxCentre),
                        "liaisonManager",
                        Map.of(
                            "firstName",
                            "Chambers",
                            "lastName",
                            "LM-" + label,
                            "emailAddress",
                            "dstew1741.lm." + ts + "." + label + "@example.com",
                            "telephoneNumber",
                            "020 7000 1234"))))
            .when()
            .post("/provider-firms")
            .then()
            .statusCode(201)
            .body("data.providerFirmNumber", notNullValue())
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

    if (inactive) {
      given()
          .contentType(ContentType.JSON)
          .pathParam("firmId", firmNumber)
          .pathParam("officeCode", officeCode)
          .body(Map.of("activeDateTo", java.time.LocalDate.now().toString()))
          .when()
          .patch("/provider-firms/{firmId}/offices/{officeCode}")
          .then()
          .statusCode(200);
    }

    return new ChambersFixture(
        firmNumber,
        officeCode,
        addressLine1,
        townOrCity,
        postcode,
        telephoneNumber,
        emailAddress,
        dxNumber,
        dxCentre);
  }

  private PractitionerFixture createPractitioner(String parentFirmNumber) {
    long ts = System.currentTimeMillis();
    String name = "E2E-DSTEW-1741 Practitioner " + ts;

    String firmNumber =
        given()
            .contentType(ContentType.JSON)
            .body(
                Map.of(
                    "firmType",
                    "Advocate",
                    "name",
                    name,
                    "practitioner",
                    Map.of(
                        "parentFirms",
                        List.of(Map.of("parentFirmNumber", parentFirmNumber)),
                        "advocateType",
                        "Advocate",
                        "advocate",
                        Map.of("advocateLevel", "Junior"),
                        "payment",
                        Map.of("paymentMethod", "CHECK"),
                        "liaisonManager",
                        Map.of("useChambersLiaisonManager", true))))
            .when()
            .post("/provider-firms")
            .then()
            .statusCode(201)
            .body("data.providerFirmNumber", notNullValue())
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

    return new PractitionerFixture(firmNumber, officeCode);
  }

  private String createLsp(String firmName) {
    long ts = System.currentTimeMillis();
    return given()
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
                        "line1", "1 LSP Street",
                        "townOrCity", "London",
                        "postcode", "EC1A 1BB"),
                    "payment",
                    Map.of("paymentMethod", "CHECK"),
                    "contractManager",
                    Map.of("useDefaultContractManager", true),
                    "liaisonManager",
                    Map.of(
                        "firstName", "LSP",
                        "lastName", "Manager",
                        "emailAddress", "dstew1741.lsp." + ts + "@example.com",
                        "telephoneNumber", "020 7000 5678"))))
        .when()
        .post("/provider-firms")
        .then()
        .statusCode(201)
        .body("data.providerFirmNumber", notNullValue())
        .extract()
        .path("data.providerFirmNumber");
  }

  private Response fetchOffice(String firmNumber, String officeCode) {
    return given()
        .pathParam("firmId", firmNumber)
        .pathParam("officeCode", officeCode)
        .when()
        .get("/provider-firms/{firmId}/offices/{officeCode}")
        .then()
        .extract()
        .response();
  }

  private record ChambersFixture(
      String firmNumber,
      String officeCode,
      String addressLine1,
      String townOrCity,
      String postcode,
      String telephoneNumber,
      String emailAddress,
      String dxNumber,
      String dxCentre) {}

  private record PractitionerFixture(String firmNumber, String officeCode) {}
}
