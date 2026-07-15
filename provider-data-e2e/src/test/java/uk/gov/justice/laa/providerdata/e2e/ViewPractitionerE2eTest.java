package uk.gov.justice.laa.providerdata.e2e;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.blankOrNullString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Read-only e2e tests for DSTEW-1738: View Legal Practitioner (DS_MAPD_FR_026).
 *
 * <p>Fixture notes:
 *
 * <ul>
 *   <li>The standard seeded Advocate (firmNumber 100003) is deliberately minimally populated and is
 *       not suitable for demonstrating every field this ticket covers.
 *   <li>A dedicated, fully-populated Advocate ("Advocate2", firmNumber 100006, office ACC007) is
 *       seeded by {@code LocalDataSeeder} with advocate level, roll number, VAT registration,
 *       website, full payment details (including a held-payment reason and an intervened date),
 *       false-balance/debt-recovery flags, a liaison manager, and a bank account.
 *   <li>Both records are pre-existing; no setup or teardown is required.
 * </ul>
 *
 * <p>Practitioner-level data is returned from {@code GET /provider-firms/{firmId}}; office-level
 * contact/financial data (VAT, payment, intervened, debt/false-balance flags) is returned from the
 * separate {@code GET /provider-firms/{firmId}/offices/{officeCode}} endpoint, with liaison-manager
 * and bank-account details available via their own sub-resource endpoints.
 *
 * <p>Interpretation note: the ticket's "Data Returned" list includes "Active From Date". Neither
 * the practitioner nor the office response exposes such a field (only {@code activeDateTo}); the
 * only "active from" concept in the data model is on the liaison-manager and bank-account link
 * sub-resources, so this is verified there. This interpretation should be confirmed with the
 * requirement author if the intent was different.
 *
 * <p>Test data is managed by {@code LocalDataSeeder}; reset the local database to re-seed.
 */
@ReadOnlyTest
@DisplayName("DSTEW-1738: View Legal Practitioner (DS_MAPD_FR_026)")
class ViewPractitionerE2eTest {

  /**
   * AC1/AC2 - View an existing Advocate practitioner and confirm every "Data Returned" field is
   * present and reflects the current persisted state: practitioner account number, name,
   * practitioner type, practitioner level, SRA roll number, parent Chamber reference, payment
   * method, liaison manager GUID, bank account GUID, VAT registration number, website, inactive
   * date (not applicable here so asserted absent), false-balance flag, debt-recovery flag,
   * intervened flag/date, and hold-all-payment flag/reason.
   */
  @Test
  void dstew1738_ac1_ac2_existingAdvocate_allAvailableDataIsDisplayed() {
    given()
        .pathParam("firmId", E2eConfig.advocate2FirmNumber())
        .when()
        .get("/provider-firms/{firmId}")
        .then()
        .statusCode(200)
        .body("data.guid", not(blankOrNullString()))
        .body("data.firmNumber", equalTo(E2eConfig.advocate2FirmNumber()))
        .body("data.name", not(blankOrNullString()))
        .body("data.firmType", equalTo("Advocate"))
        .body("data.practitioner.advocateType", not(blankOrNullString()))
        .body("data.practitioner.office.accountNumber", equalTo(E2eConfig.advocate2OfficeCode()))
        .body("data.practitioner.advocate.advocateLevel", equalTo(E2eConfig.advocate2Level()))
        .body(
            "data.practitioner.advocate.solicitorRegulationAuthorityRollNumber",
            equalTo(E2eConfig.advocate2RollNumber()))
        .body("data.practitioner.parentFirms", not(empty()))
        .body(
            "data.practitioner.parentFirms[0].parentFirmNumber",
            equalTo(E2eConfig.advocate2ParentFirmNumber()));

    given()
        .pathParam("firmId", E2eConfig.advocate2FirmNumber())
        .pathParam("officeCode", E2eConfig.advocate2OfficeCode())
        .when()
        .get("/provider-firms/{firmId}/offices/{officeCode}")
        .then()
        .statusCode(200)
        .body("data.firmType", equalTo("Advocate"))
        .body("data.accountNumber", equalTo(E2eConfig.advocate2OfficeCode()))
        .body("data.activeDateTo", nullValue())
        .body("data.address.line1", not(blankOrNullString()))
        .body("data.address.townOrCity", not(blankOrNullString()))
        .body("data.address.postcode", not(blankOrNullString()))
        .body("data.telephoneNumber", not(blankOrNullString()))
        .body("data.emailAddress", not(blankOrNullString()))
        .body("data.website", not(blankOrNullString()))
        .body("data.dxDetails.dxNumber", not(blankOrNullString()))
        .body("data.dxDetails.dxCentre", not(blankOrNullString()))
        .body("data.vatRegistration.vatNumber", not(blankOrNullString()))
        .body("data.payment.paymentMethod", not(blankOrNullString()))
        .body("data.payment.paymentHeldFlag", equalTo(true))
        .body("data.payment.paymentHeldReason", not(blankOrNullString()))
        .body("data.intervened.intervenedFlag", equalTo(true))
        .body("data.intervened.intervenedChangeDate", not(blankOrNullString()))
        .body("data.debtRecoveryFlag", equalTo(true))
        .body("data.falseBalanceFlag", equalTo(true));

    given()
        .pathParam("firmId", E2eConfig.advocate2FirmNumber())
        .pathParam("officeCode", E2eConfig.advocate2OfficeCode())
        .when()
        .get("/provider-firms/{firmId}/offices/{officeCode}/liaison-managers")
        .then()
        .statusCode(200)
        .body("data.content", not(empty()))
        .body("data.content[0].guid", not(blankOrNullString()))
        .body("data.content[0].firstName", not(blankOrNullString()))
        .body("data.content[0].lastName", equalTo(E2eConfig.advocate2LiaisonManagerLastName()))
        .body("data.content[0].emailAddress", not(blankOrNullString()))
        .body("data.content[0].telephoneNumber", not(blankOrNullString()))
        .body("data.content[0].activeDateFrom", not(blankOrNullString()))
        .body("data.content[0].linkedFlag", equalTo(true));

    given()
        .pathParam("firmId", E2eConfig.advocate2FirmNumber())
        .pathParam("officeCode", E2eConfig.advocate2OfficeCode())
        .when()
        .get("/provider-firms/{firmId}/offices/{officeCode}/bank-details")
        .then()
        .statusCode(200)
        .body("data.content", not(empty()))
        .body("data.content[0].guid", not(blankOrNullString()))
        .body("data.content[0].accountName", not(blankOrNullString()))
        .body("data.content[0].sortCode", not(blankOrNullString()))
        .body("data.content[0].accountNumber", not(blankOrNullString()))
        .body("data.content[0].activeDateFrom", not(blankOrNullString()))
        .body("data.content[0].primaryFlag", equalTo(true));
  }

  /** AC3 - Non-existent practitioner returns 404. */
  @Test
  void dstew1738_ac3_nonExistentPractitioner_recordNotFoundResponseReturned() {
    given()
        .pathParam("firmId", E2eConfig.invalidFirmNumber())
        .when()
        .get("/provider-firms/{firmId}")
        .then()
        .statusCode(404)
        .body("error.errorCode", equalTo("P00NF"));
  }

  /**
   * AC4 - Read-only behaviour. This test class only issues GET requests (never
   * create/amend/delete), consistent with the {@link ReadOnlyTest} tag. As a black-box proxy for
   * "no data is created, amended, or deleted", this confirms that retrieving the same practitioner
   * twice in succession returns an identical {@code lastUpdatedTimestamp} and {@code version}, i.e.
   * the act of viewing does not itself mutate the record.
   */
  @Test
  void dstew1738_ac4_viewingPractitionerTwice_recordIsUnchanged() {
    var first =
        given()
            .pathParam("firmId", E2eConfig.advocate2FirmNumber())
            .when()
            .get("/provider-firms/{firmId}")
            .then()
            .statusCode(200)
            .extract();

    given()
        .pathParam("firmId", E2eConfig.advocate2FirmNumber())
        .when()
        .get("/provider-firms/{firmId}")
        .then()
        .statusCode(200)
        .body("data.version", equalTo(first.path("data.version")))
        .body("data.lastUpdatedTimestamp", equalTo(first.path("data.lastUpdatedTimestamp")));
  }
}
