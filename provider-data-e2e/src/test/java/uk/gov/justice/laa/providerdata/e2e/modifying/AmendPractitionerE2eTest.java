package uk.gov.justice.laa.providerdata.e2e.modifying;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import io.restassured.http.ContentType;
import io.restassured.response.Response;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import uk.gov.justice.laa.providerdata.e2e.ModifyingTest;

/**
 * Data-modifying E2E tests for DS_MAPD_FR_025: Amend Legal Practitioner (DSTEW-1735).
 *
 * <p>The ticket language is logical-model based: "Legal Practitioner" maps to {@code
 * firmType=Advocate}, including both Advocate and Barrister variants.
 */
@ModifyingTest
@DisplayName("DSTEW-1735: Amend Legal Practitioner (DS_MAPD_FR_025)")
class AmendPractitionerE2eTest {

  private ChambersFixture activeChambersA;
  private ChambersFixture activeChambersB;
  private ChambersFixture inactiveChambers;
  private PractitionerFixture practitioner;
  private BarristerPractitionerFixture barristerPractitioner;

  @BeforeEach
  void setup() {
    long ts = System.currentTimeMillis();
    activeChambersA = createChambers("A-" + ts, false);
    activeChambersB = createChambers("B-" + ts, false);
    inactiveChambers = createChambers("Inactive-" + ts, true);
    practitioner = createPractitioner(activeChambersA.firmNumber(), ts);
    barristerPractitioner = createBarristerPractitioner(activeChambersA.firmNumber(), ts);
  }

  /**
   * AC1 - Amend Practitioner with valid data.
   *
   * <p>Given an existing practitioner and valid amendable data, PATCH succeeds and persisted values
   * are visible on subsequent GET.
   */
  @Test
  void dstew1735_ac1_amendPractitioner_withValidData_returns200AndPersists() {
    String updatedRollNumber = "SRA-AMEND-" + System.currentTimeMillis();

    given()
        .contentType(ContentType.JSON)
        .pathParam("firmId", practitioner.firmNumber())
        .body(
            Map.of(
                "practitioner",
                Map.of(
                    "advocateLevel",
                    "KC",
                    "solicitorRegulationAuthorityRollNumber",
                    updatedRollNumber,
                    "parentFirms",
                    List.of(Map.of("parentFirmNumber", activeChambersB.firmNumber())))))
        .when()
        .patch("/provider-firms/{firmId}")
        .then()
        .statusCode(200);

    given()
        .pathParam("firmId", practitioner.firmNumber())
        .when()
        .get("/provider-firms/{firmId}")
        .then()
        .statusCode(200)
        .body("data.practitioner.parentFirms", hasSize(1))
        .body(
            "data.practitioner.parentFirms[0].parentFirmNumber",
            equalTo(activeChambersB.firmNumber()))
        .body("data.practitioner.advocate.advocateLevel", equalTo("KC"))
        .body(
            "data.practitioner.advocate.solicitorRegulationAuthorityRollNumber",
            equalTo(updatedRollNumber));
  }

  /**
   * AC1 - Amend Practitioner with valid data.
   *
   * <p>Given an existing Barrister practitioner and valid amendable data, PATCH succeeds and
   * persisted values are visible on subsequent GET.
   */
  @Test
  void dstew1735_ac1_amendBarrister_withValidData_returns200AndPersists() {
    String updatedRollNumber = "BAR-AMEND-" + System.currentTimeMillis();

    given()
        .contentType(ContentType.JSON)
        .pathParam("firmId", barristerPractitioner.firmNumber())
        .body(
            Map.of(
                "practitioner",
                Map.of(
                    "barristerLevel",
                    "Junior",
                    "barCouncilRollNumber",
                    updatedRollNumber,
                    "parentFirms",
                    List.of(Map.of("parentFirmNumber", activeChambersB.firmNumber())))))
        .when()
        .patch("/provider-firms/{firmId}")
        .then()
        .statusCode(200);

    given()
        .pathParam("firmId", barristerPractitioner.firmNumber())
        .when()
        .get("/provider-firms/{firmId}")
        .then()
        .statusCode(200)
        .body("data.practitioner.parentFirms", hasSize(1))
        .body(
            "data.practitioner.parentFirms[0].parentFirmNumber",
            equalTo(activeChambersB.firmNumber()))
        .body("data.practitioner.barrister.barristerLevel", equalTo("Junior"))
        .body("data.practitioner.barrister.barCouncilRollNumber", equalTo(updatedRollNumber));
  }

  /**
   * AC2 - Mandatory fields cannot be cleared.
   *
   * <p>Rejects blank and whitespace-only roll-number values and leaves practitioner unchanged.
   */
  @Test
  void dstew1735_ac2_rollNumberBlankOrWhitespace_returns400AndUnchanged() {
    PractitionerSnapshot before = snapshotPractitioner(practitioner.firmNumber());

    given()
        .contentType(ContentType.JSON)
        .pathParam("firmId", practitioner.firmNumber())
        .body(Map.of("practitioner", Map.of("solicitorRegulationAuthorityRollNumber", "")))
        .when()
        .patch("/provider-firms/{firmId}")
        .then()
        .statusCode(400);

    assertPractitionerUnchanged(practitioner.firmNumber(), before);

    given()
        .contentType(ContentType.JSON)
        .pathParam("firmId", practitioner.firmNumber())
        .body(Map.of("practitioner", Map.of("solicitorRegulationAuthorityRollNumber", "   ")))
        .when()
        .patch("/provider-firms/{firmId}")
        .then()
        .statusCode(400);

    assertPractitionerUnchanged(practitioner.firmNumber(), before);
  }

  /**
   * AC2 - Mandatory fields cannot be cleared.
   *
   * <p>Rejects blank and whitespace-only Barrister roll-number values and leaves practitioner
   * unchanged.
   */
  @Test
  void dstew1735_ac2_barristerRollNumberBlankOrWhitespace_returns400AndUnchanged() {
    BarristerSnapshot before = snapshotBarrister(barristerPractitioner.firmNumber());

    given()
        .contentType(ContentType.JSON)
        .pathParam("firmId", barristerPractitioner.firmNumber())
        .body(Map.of("practitioner", Map.of("barCouncilRollNumber", "")))
        .when()
        .patch("/provider-firms/{firmId}")
        .then()
        .statusCode(400);

    assertBarristerUnchanged(barristerPractitioner.firmNumber(), before);

    given()
        .contentType(ContentType.JSON)
        .pathParam("firmId", barristerPractitioner.firmNumber())
        .body(Map.of("practitioner", Map.of("barCouncilRollNumber", "   ")))
        .when()
        .patch("/provider-firms/{firmId}")
        .then()
        .statusCode(400);

    assertBarristerUnchanged(barristerPractitioner.firmNumber(), before);
  }

  /**
   * AC3 - Parent Chamber validation.
   *
   * <p>Non-existent parent Chamber returns 404 and no practitioner fields are changed.
   */
  @Test
  void dstew1735_ac3_parentChamberNotFound_returns404AndUnchanged() {
    PractitionerSnapshot before = snapshotPractitioner(practitioner.firmNumber());

    given()
        .contentType(ContentType.JSON)
        .pathParam("firmId", practitioner.firmNumber())
        .body(
            Map.of(
                "practitioner",
                Map.of("parentFirms", List.of(Map.of("parentFirmNumber", "UNKNOWN-CHAMBERS")))))
        .when()
        .patch("/provider-firms/{firmId}")
        .then()
        .statusCode(404)
        .body("detail", containsString("Parent provider not found"));

    assertPractitionerUnchanged(practitioner.firmNumber(), before);
  }

  /**
   * AC3 - Parent Chamber validation.
   *
   * <p>Inactive parent Chamber is rejected with 400 and no practitioner fields are changed.
   */
  @Test
  void dstew1735_ac3_parentChamberInactive_returns400AndUnchanged() {
    PractitionerSnapshot before = snapshotPractitioner(practitioner.firmNumber());

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
        .body("detail", containsString("Parent Chamber is inactive"));

    assertPractitionerUnchanged(practitioner.firmNumber(), before);
  }

  /**
   * AC4 - Single Chamber association enforcement.
   *
   * <p>Rejects amendments that would result in multiple parent Chambers.
   */
  @Test
  void dstew1735_ac4_parentFirmsMultiple_returns400AndUnchanged() {
    PractitionerSnapshot before = snapshotPractitioner(practitioner.firmNumber());

    given()
        .contentType(ContentType.JSON)
        .pathParam("firmId", practitioner.firmNumber())
        .body(
            Map.of(
                "practitioner",
                Map.of(
                    "parentFirms",
                    List.of(
                        Map.of("parentFirmNumber", activeChambersA.firmNumber()),
                        Map.of("parentFirmNumber", activeChambersB.firmNumber())))))
        .when()
        .patch("/provider-firms/{firmId}")
        .then()
        .statusCode(400)
        .body("detail", containsString("Exactly one parent Chamber"));

    assertPractitionerUnchanged(practitioner.firmNumber(), before);
  }

  /**
   * AC4 - Single Chamber association enforcement.
   *
   * <p>Rejects empty {@code parentFirms} arrays; {@code null}/absent still means "no parent
   * change".
   */
  @Test
  void dstew1735_ac4_parentFirmsEmptyArray_returns400AndUnchanged() {
    PractitionerSnapshot before = snapshotPractitioner(practitioner.firmNumber());

    given()
        .contentType(ContentType.JSON)
        .pathParam("firmId", practitioner.firmNumber())
        .body(Map.of("practitioner", Map.of("parentFirms", List.of())))
        .when()
        .patch("/provider-firms/{firmId}")
        .then()
        .statusCode(400)
        .body("detail", containsString("Exactly one parent Chamber"));

    assertPractitionerUnchanged(practitioner.firmNumber(), before);
  }

  /**
   * AC5 - Conditional field validation.
   *
   * <p>Office-level intervened details must include the required counterpart fields.
   */
  @Test
  void dstew1735_ac5_intervenedDateWithoutFlag_returns400AndOfficeUnchanged() {
    OfficeSnapshot before = snapshotOffice(practitioner.firmNumber(), practitioner.officeCode());

    given()
        .contentType(ContentType.JSON)
        .pathParam("firmId", practitioner.firmNumber())
        .pathParam("officeCode", practitioner.officeCode())
        .body(Map.of("intervened", Map.of("intervenedChangeDate", LocalDate.now().toString())))
        .when()
        .patch("/provider-firms/{firmId}/offices/{officeCode}")
        .then()
        // intervenedFlag is a required field on IntervenedOfficeDetailsPatchV2, so this is
        // rejected at request deserialization with a generic "Invalid request content" detail
        // rather than the field-specific message produced by service-level validation.
        .statusCode(400);

    assertOfficeUnchanged(practitioner.firmNumber(), practitioner.officeCode(), before);
  }

  /**
   * AC5 - Conditional field validation.
   *
   * <p>Office-level hold-payment reason must not be provided without its required hold flag.
   */
  @Test
  void dstew1735_ac5_paymentHeldReasonWithoutFlag_returns400AndOfficeUnchanged() {
    OfficeSnapshot before = snapshotOffice(practitioner.firmNumber(), practitioner.officeCode());

    given()
        .contentType(ContentType.JSON)
        .pathParam("firmId", practitioner.firmNumber())
        .pathParam("officeCode", practitioner.officeCode())
        .body(
            Map.of("payment", Map.of("paymentMethod", "CHECK", "paymentHeldReason", "Reason only")))
        .when()
        .patch("/provider-firms/{firmId}/offices/{officeCode}")
        .then()
        // paymentHeldFlag is a required field on PaymentDetailsPatchOrLinkV2, so this is
        // rejected at request deserialization with a generic "Invalid request content" detail
        // rather than the field-specific message produced by service-level validation.
        .statusCode(400);

    assertOfficeUnchanged(practitioner.firmNumber(), practitioner.officeCode(), before);
  }

  /**
   * AC5 - Conditional field validation.
   *
   * <p>Office-level intervened flag must not be provided without intervened change date.
   */
  @Test
  void dstew1735_ac5_intervenedFlagWithoutDate_returns400AndOfficeUnchanged() {
    OfficeSnapshot before = snapshotOffice(practitioner.firmNumber(), practitioner.officeCode());

    given()
        .contentType(ContentType.JSON)
        .pathParam("firmId", practitioner.firmNumber())
        .pathParam("officeCode", practitioner.officeCode())
        .body(Map.of("intervened", Map.of("intervenedFlag", true)))
        .when()
        .patch("/provider-firms/{firmId}/offices/{officeCode}")
        .then()
        .statusCode(400)
        .body("detail", containsString("intervenedChangeDate"));

    assertOfficeUnchanged(practitioner.firmNumber(), practitioner.officeCode(), before);
  }

  /**
   * AC5 - Conditional field validation.
   *
   * <p>Office-level hold-payment flag true must not be provided without hold-payment reason.
   */
  @Test
  void dstew1735_ac5_paymentHeldFlagTrueWithoutReason_returns400AndOfficeUnchanged() {
    OfficeSnapshot before = snapshotOffice(practitioner.firmNumber(), practitioner.officeCode());

    given()
        .contentType(ContentType.JSON)
        .pathParam("firmId", practitioner.firmNumber())
        .pathParam("officeCode", practitioner.officeCode())
        .body(Map.of("payment", Map.of("paymentMethod", "CHECK", "paymentHeldFlag", true)))
        .when()
        .patch("/provider-firms/{firmId}/offices/{officeCode}")
        .then()
        .statusCode(400)
        .body("detail", containsString("paymentHeldReason"));

    assertOfficeUnchanged(practitioner.firmNumber(), practitioner.officeCode(), before);
  }

  /**
   * AC5 - Conditional field validation.
   *
   * <p>Office-level EFT payment must include bank account details.
   */
  @Test
  void dstew1735_ac5_eftWithoutBankAccountDetails_returns400AndOfficeUnchanged() {
    OfficeSnapshot before = snapshotOffice(practitioner.firmNumber(), practitioner.officeCode());

    given()
        .contentType(ContentType.JSON)
        .pathParam("firmId", practitioner.firmNumber())
        .pathParam("officeCode", practitioner.officeCode())
        .body(Map.of("payment", Map.of("paymentMethod", "EFT", "paymentHeldFlag", false)))
        .when()
        .patch("/provider-firms/{firmId}/offices/{officeCode}")
        .then()
        .statusCode(400)
        .body("detail", containsString("bankAccountDetails"));

    assertOfficeUnchanged(practitioner.firmNumber(), practitioner.officeCode(), before);
  }

  /**
   * AC6 - Redacted field protection.
   *
   * <p>Redacted/non-amendable fields are rejected: provider-level practitioner name and
   * practitioner type.
   */
  @Test
  void dstew1735_ac6_redactedFieldsRejected_returns400AndUnchanged() {
    PractitionerSnapshot before = snapshotPractitioner(practitioner.firmNumber());

    given()
        .contentType(ContentType.JSON)
        .pathParam("firmId", practitioner.firmNumber())
        .body(Map.of("name", "Attempted practitioner rename"))
        .when()
        .patch("/provider-firms/{firmId}")
        .then()
        .statusCode(400);

    assertPractitionerUnchanged(practitioner.firmNumber(), before);

    given()
        .contentType(ContentType.JSON)
        .pathParam("firmId", practitioner.firmNumber())
        .body(Map.of("practitioner", Map.of("advocateType", "Barrister")))
        .when()
        .patch("/provider-firms/{firmId}")
        .then()
        .statusCode(400);

    assertPractitionerUnchanged(practitioner.firmNumber(), before);

    given()
        .contentType(ContentType.JSON)
        .pathParam("firmId", practitioner.firmNumber())
        .body(Map.of("practitioner", Map.of("accountNumber", "ACC-REDACTED-1")))
        .when()
        .patch("/provider-firms/{firmId}")
        .then()
        .statusCode(400);

    assertPractitionerUnchanged(practitioner.firmNumber(), before);
  }

  /**
   * AC7 - Complete record enforcement.
   *
   * <p>When amendment validation fails, no partial changes are persisted.
   */
  @Test
  void dstew1735_ac7_failedAmendment_rollsBackAndLeavesRecordUnchanged() {
    PractitionerSnapshot before = snapshotPractitioner(practitioner.firmNumber());

    given()
        .contentType(ContentType.JSON)
        .pathParam("firmId", practitioner.firmNumber())
        .body(
            Map.of(
                "practitioner",
                Map.of(
                    "advocateLevel",
                    "KC",
                    "solicitorRegulationAuthorityRollNumber",
                    "SRA-FAILED-" + System.currentTimeMillis(),
                    "parentFirms",
                    List.of(
                        Map.of("parentFirmNumber", activeChambersA.firmNumber()),
                        Map.of("parentFirmNumber", activeChambersB.firmNumber())))))
        .when()
        .patch("/provider-firms/{firmId}")
        .then()
        .statusCode(400);

    assertPractitionerUnchanged(practitioner.firmNumber(), before);
  }

  private ChambersFixture createChambers(String label, boolean inactive) {
    long ts = System.currentTimeMillis();
    String name = "E2E-DSTEW-1735 Chambers " + label;

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
                            "line1", "1 " + label + " Street",
                            "townOrCity", "London",
                            "postcode", "WC1A 1AA"),
                        "liaisonManager",
                        Map.of(
                            "firstName",
                            "Chambers",
                            "lastName",
                            "LM-" + label,
                            "emailAddress",
                            "dstew1735.chambers." + ts + "." + label + "@example.com",
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
          .body(Map.of("activeDateTo", LocalDate.now().toString()))
          .when()
          .patch("/provider-firms/{firmId}/offices/{officeCode}")
          .then()
          .statusCode(200);
    }

    return new ChambersFixture(firmNumber, officeCode);
  }

  private PractitionerFixture createPractitioner(String parentFirmNumber, long seedTs) {
    String name = "E2E-DSTEW-1735 Practitioner " + seedTs;
    String initialRollNumber = "SRA-" + seedTs;

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
                        Map.of(
                            "advocateLevel",
                            "Junior",
                            "solicitorRegulationAuthorityRollNumber",
                            initialRollNumber),
                        "liaisonManager",
                        Map.of(
                            "firstName", "Amend",
                            "lastName", "PractitionerLM",
                            "emailAddress", "amend.practitioner.lm." + seedTs + "@example.com",
                            "telephoneNumber", "020 2222 8888"),
                        "payment",
                        Map.of("paymentMethod", "CHECK"))))
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

    return new PractitionerFixture(firmNumber, officeCode, name, initialRollNumber);
  }

  private BarristerPractitionerFixture createBarristerPractitioner(
      String parentFirmNumber, long seedTs) {
    String name = "E2E-DSTEW-1735 Barrister " + seedTs;
    String initialRollNumber = "BAR-" + seedTs;

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
                        "Barrister",
                        "barrister",
                        Map.of("barristerLevel", "KC", "barCouncilRollNumber", initialRollNumber),
                        "liaisonManager",
                        Map.of(
                            "firstName", "AmendBarrister",
                            "lastName", "PractitionerLM",
                            "emailAddress", "amend.barrister.lm." + seedTs + "@example.com",
                            "telephoneNumber", "020 3333 7777"),
                        "payment",
                        Map.of("paymentMethod", "CHECK"))))
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

    return new BarristerPractitionerFixture(firmNumber, officeCode, name, initialRollNumber);
  }

  private PractitionerSnapshot snapshotPractitioner(String practitionerFirmNumber) {
    Response response =
        given()
            .pathParam("firmId", practitionerFirmNumber)
            .when()
            .get("/provider-firms/{firmId}")
            .then()
            .statusCode(200)
            .extract()
            .response();

    return new PractitionerSnapshot(
        response.path("data.name"),
        response.path("data.practitioner.parentFirms[0].parentFirmNumber"),
        response.path("data.practitioner.advocate.advocateLevel"),
        response.path("data.practitioner.advocate.solicitorRegulationAuthorityRollNumber"));
  }

  private OfficeSnapshot snapshotOffice(String practitionerFirmNumber, String officeCode) {
    Response response =
        given()
            .pathParam("firmId", practitionerFirmNumber)
            .pathParam("officeCode", officeCode)
            .when()
            .get("/provider-firms/{firmId}/offices/{officeCode}")
            .then()
            .statusCode(200)
            .extract()
            .response();

    return new OfficeSnapshot(
        response.path("data.payment.paymentMethod"),
        response.path("data.payment.paymentHeldFlag"),
        response.path("data.payment.paymentHeldReason"));
  }

  private BarristerSnapshot snapshotBarrister(String practitionerFirmNumber) {
    Response response =
        given()
            .pathParam("firmId", practitionerFirmNumber)
            .when()
            .get("/provider-firms/{firmId}")
            .then()
            .statusCode(200)
            .extract()
            .response();

    return new BarristerSnapshot(
        response.path("data.name"),
        response.path("data.practitioner.parentFirms[0].parentFirmNumber"),
        response.path("data.practitioner.barrister.barristerLevel"),
        response.path("data.practitioner.barrister.barCouncilRollNumber"));
  }

  private void assertPractitionerUnchanged(
      String practitionerFirmNumber, PractitionerSnapshot expected) {
    PractitionerSnapshot actual = snapshotPractitioner(practitionerFirmNumber);
    assertEquals(expected, actual);
  }

  private void assertOfficeUnchanged(
      String practitionerFirmNumber, String officeCode, OfficeSnapshot expected) {
    OfficeSnapshot actual = snapshotOffice(practitionerFirmNumber, officeCode);
    assertEquals(expected, actual);
  }

  private void assertBarristerUnchanged(String practitionerFirmNumber, BarristerSnapshot expected) {
    BarristerSnapshot actual = snapshotBarrister(practitionerFirmNumber);
    assertEquals(expected, actual);
  }

  private record ChambersFixture(String firmNumber, String officeCode) {}

  private record PractitionerFixture(
      String firmNumber, String officeCode, String initialName, String initialRollNumber) {}

  private record BarristerPractitionerFixture(
      String firmNumber, String officeCode, String initialName, String initialRollNumber) {}

  private record PractitionerSnapshot(
      String name, String parentFirmNumber, String advocateLevel, String rollNumber) {}

  private record BarristerSnapshot(
      String name, String parentFirmNumber, String barristerLevel, String rollNumber) {}

  private record OfficeSnapshot(
      String paymentMethod, Boolean paymentHeldFlag, String paymentHeldReason) {}
}
