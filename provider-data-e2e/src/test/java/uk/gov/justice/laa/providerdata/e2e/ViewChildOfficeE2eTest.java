package uk.gov.justice.laa.providerdata.e2e;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.blankOrNullString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

import org.junit.jupiter.api.Test;

/// Read-only E2E tests for DSTEW-1669 (DS_MAPD_FR_044): View Legal Organisation Child Office.
///
/// Fixture notes:
///
/// - The test uses the seeded LSP2 firm (firmNumber 100005) and its child office (ACC006,
///   `headOfficeFlag=false`). This is a separate firm from the LSP used by other tests (100001),
///   so neither set of tests can affect the other.
/// - LSP2 is seeded by `LocalDataSeeder` on application startup when running the `local` or
///   `preview` Spring profile. All data is clearly fictional.
/// - The child office (ACC006) is pre-populated with all optional fields — DX, VAT, telephone,
///   email, liaison manager, contract manager, bank account — so every field listed in the
///   ticket's "Data Returned" section can be verified.
/// - Note: the ticket lists "Parent Legal Organisation GUID" as a field to return. This is not
///   part of the `GET /provider-firms/{firmId}/offices/{officeCode}` response body; the parent
///   firm is identified by the `{firmId}` path parameter in the request URI, not echoed back in
///   the response. This is by design and is not a gap.
/// - Note: the ticket lists "Active From Date". There is no dedicated `activeDateFrom` field on
///   an office; an office becomes active when it is created. The closest equivalent is
///   `createdTimestamp` from the base entity, which is verified in AC1.
@ReadOnlyTest
class ViewChildOfficeE2eTest {

  /// Retrieves the child office and verifies that all populated fields are returned correctly.
  ///
  /// Covers all data items listed in DS_MAPD_FR_044 that are part of the main office GET
  /// response: GUID, account number, firm type, address, contact details, DX details, VAT
  /// registration, payment details (including `paymentHeldFlag`), LSP flags (intervened,
  /// debt recovery, false balance), and audit timestamps.
  ///
  /// - DSTEW-1669 AC1 – PDP returns the child office record successfully. (DS_MAPD_FR_044)
  @Test
  void dstew1669_ac1_existingChildOffice_allFieldsReturned() {
    given()
        .pathParam("firmId", E2eConfig.lsp2FirmNumber())
        .pathParam("officeCode", E2eConfig.lsp2ChildOfficeCode())
        .when()
        .get("/provider-firms/{firmId}/offices/{officeCode}")
        .then()
        .statusCode(200)
        // Base entity fields
        .body("data.guid", not(blankOrNullString()))
        .body("data.createdTimestamp", not(blankOrNullString()))
        .body("data.lastUpdatedTimestamp", not(blankOrNullString()))
        // Identity
        .body("data.firmType", equalTo("Legal Services Provider"))
        .body("data.accountNumber", equalTo(E2eConfig.lsp2ChildOfficeCode()))
        // Address
        .body("data.address.line1", equalTo("2 Test Child Office Street"))
        .body("data.address.line2", equalTo("Floor 3"))
        .body("data.address.townOrCity", equalTo("Leeds"))
        .body("data.address.county", equalTo("West Yorkshire"))
        .body("data.address.postcode", equalTo("LS1 1AB"))
        // Contact
        .body("data.telephoneNumber", equalTo("0113 000 0001"))
        .body("data.emailAddress", equalTo("child-office@test.example.com"))
        // DX details
        .body("data.dxDetails.dxNumber", equalTo("DX 00001"))
        .body("data.dxDetails.dxCentre", equalTo("Leeds DX Centre"))
        // VAT registration
        .body("data.vatRegistration.vatNumber", equalTo("GB000000000"))
        // Payment details — paymentHeldFlag is the "Hold All Payment Flag" (BR ref)
        .body("data.payment.paymentMethod", equalTo("EFT"))
        .body("data.payment.paymentHeldFlag", equalTo(false))
        .body("data.payment.paymentHeldReason", nullValue())
        // LSP flags
        .body("data.debtRecoveryFlag", equalTo(false))
        .body("data.falseBalanceFlag", equalTo(false))
        .body("data.intervened.intervenedFlag", equalTo(false))
        .body("data.intervened.intervenedChangeDate", nullValue())
        // Inactive date absent for an active office
        .body("data.activeDateTo", nullValue());
  }

  /// Retrieves the liaison manager assigned to the child office and verifies that the expected
  /// fields match the seeded values.
  ///
  /// - DSTEW-1669 AC1 – liaison manager details are returned via the sub-resource.
  ///   (DS_MAPD_FR_044)
  @Test
  void dstew1669_ac1_existingChildOffice_liaisonManagerReturned() {
    given()
        .pathParam("firmId", E2eConfig.lsp2FirmNumber())
        .pathParam("officeCode", E2eConfig.lsp2ChildOfficeCode())
        .when()
        .get("/provider-firms/{firmId}/offices/{officeCode}/liaison-managers")
        .then()
        .statusCode(200)
        .body("data.content", hasSize(greaterThanOrEqualTo(1)))
        .body("data.content[0].guid", not(blankOrNullString()))
        .body("data.content[0].firstName", equalTo("Test"))
        .body("data.content[0].lastName", equalTo("ChildLm"))
        .body("data.content[0].emailAddress", equalTo("child-lm@test.example.com"))
        .body("data.content[0].telephoneNumber", equalTo("0113 000 0002"))
        .body("data.content[0].activeDateFrom", not(blankOrNullString()))
        .body("data.content[0].linkedFlag", equalTo(true));
  }

  /// Retrieves the contract manager assigned to the child office and verifies that the expected
  /// fields match the seeded values.
  ///
  /// - DSTEW-1669 AC1 – contract manager details are returned via the sub-resource.
  ///   (DS_MAPD_FR_044)
  @Test
  void dstew1669_ac1_existingChildOffice_contractManagerReturned() {
    given()
        .pathParam("firmId", E2eConfig.lsp2FirmNumber())
        .pathParam("officeCode", E2eConfig.lsp2ChildOfficeCode())
        .when()
        .get("/provider-firms/{firmId}/offices/{officeCode}/contract-managers")
        .then()
        .statusCode(200)
        .body("data.content", hasSize(greaterThanOrEqualTo(1)))
        .body("data.content[0].guid", notNullValue())
        .body("data.content[0].contractManagerId", equalTo("CM003"))
        .body("data.content[0].lastName", equalTo("ChildCm"));
  }

  /// Retrieves the bank account assigned to the child office and verifies that the expected
  /// fields match the seeded values.
  ///
  /// - DSTEW-1669 AC1 – bank account details are returned via the sub-resource. (DS_MAPD_FR_044)
  @Test
  void dstew1669_ac1_existingChildOffice_bankAccountReturned() {
    given()
        .pathParam("firmId", E2eConfig.lsp2FirmNumber())
        .pathParam("officeCode", E2eConfig.lsp2ChildOfficeCode())
        .when()
        .get("/provider-firms/{firmId}/offices/{officeCode}/bank-details")
        .then()
        .statusCode(200)
        .body("data.content", hasSize(greaterThanOrEqualTo(1)))
        .body("data.content[0].guid", notNullValue())
        .body("data.content[0].accountName", equalTo("Test Child Office Account"))
        .body("data.content[0].sortCode", equalTo("300000"))
        .body("data.content[0].accountNumber", equalTo("00000001"))
        .body("data.content[0].primaryFlag", equalTo(true));
  }

  /// Requests an office that does not exist and verifies that a 404 error is returned.
  ///
  /// - DSTEW-1669 AC2 – PDP returns an error when the child office cannot be found.
  ///   (DS_MAPD_FR_044)
  @Test
  void dstew1669_ac2_nonExistentOffice_returns404() {
    given()
        .pathParam("firmId", E2eConfig.lsp2FirmNumber())
        .pathParam("officeCode", E2eConfig.invalidOfficeCode())
        .when()
        .get("/provider-firms/{firmId}/offices/{officeCode}")
        .then()
        .statusCode(404)
        .body("error.errorCode", equalTo("P00NF"));
  }

  /// Requests a real office code under the wrong provider and verifies that a 404 is returned.
  /// Confirms that office lookups are scoped to the specified provider, not global.
  ///
  /// - DSTEW-1669 AC2 – PDP returns an error when the child office cannot be found.
  ///   (DS_MAPD_FR_044)
  @Test
  void dstew1669_ac2_officeCodeUnderWrongFirm_returns404() {
    given()
        .pathParam("firmId", E2eConfig.lspFirmNumber())
        .pathParam("officeCode", E2eConfig.lsp2ChildOfficeCode())
        .when()
        .get("/provider-firms/{firmId}/offices/{officeCode}")
        .then()
        .statusCode(404)
        .body("error.errorCode", equalTo("P00NF"));
  }
}
