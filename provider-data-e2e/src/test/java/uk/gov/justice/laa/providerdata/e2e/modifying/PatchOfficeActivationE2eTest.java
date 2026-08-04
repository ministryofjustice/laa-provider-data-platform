package uk.gov.justice.laa.providerdata.e2e.modifying;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

import io.restassured.http.ContentType;
import io.restassured.response.ValidatableResponse;
import java.time.LocalDate;
import java.util.Map;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import uk.gov.justice.laa.providerdata.e2e.E2eConfig;
import uk.gov.justice.laa.providerdata.e2e.ModifyingTest;

/**
 * Data-modifying e2e tests for office activation/deactivation via {@code PATCH
 * /provider-firms/{firmId}/offices/{officeCode}}, including the DSTEW-1674 {@code
 * paymentHeldFlag}/{@code falseBalanceFlag} same-request transition rules (DS_MAPD_FR_045 status
 * flag rules for LSP entities).
 *
 * <p><strong>Note:</strong> unlike the rest of this codebase's e2e tests, which are independent of
 * execution order, this class deliberately uses {@link TestMethodOrder} with {@link
 * OrderAnnotation} because its tests share a single office fixture and chain state transitions
 * (active &rarr; inactive &rarr; active) across the suite; each test's assertions depend on the
 * office being left in the state the previous test produced. This is specific to this class and
 * {@link PatchPractitionerOfficeActivationE2eTest}, not a general convention. A fresh non-head
 * office is created in {@code @BeforeAll} so that deactivation does not cascade to other offices or
 * affect other test classes.
 *
 * <p>DSTEW-1674 AC3 ("Status Flag Rules Validated at LSP Entity Creation or Amendment") is only
 * covered here for amendment: {@code debtRecoveryFlag}/{@code falseBalanceFlag} are absent from
 * {@code LSPOfficeCreateV2}/{@code LSPHeadOfficeCreateV2} in the OpenAPI spec, so there is no
 * creation-time request path that can set these flags to validate.
 */
@ModifyingTest
@TestMethodOrder(OrderAnnotation.class)
@DisplayName("DSTEW-1674: Status flag rules for LSP office activation")
class PatchOfficeActivationE2eTest {

  private static String officeCode;

  @BeforeAll
  static void createNonHeadOffice() {
    String accountNumber = "8" + (System.currentTimeMillis() % 10_000_000L);
    Map<String, Object> body =
        Map.of(
            "address",
            Map.of(
                "line1", "1 Test Street",
                "townOrCity", "Leeds",
                "postcode", "LS1 1AA"),
            "telephoneNumber",
            "0113 000 0001",
            "payment",
            Map.of(
                "paymentMethod",
                "EFT",
                "bankAccountDetails",
                Map.of(
                    "accountName", "Non Head Office Account",
                    "sortCode", "601111",
                    "accountNumber", accountNumber)),
            "liaisonManager",
            Map.of("useHeadOfficeLiaisonManager", true),
            "contractManager",
            Map.of("useHeadOfficeContractManager", true));

    officeCode =
        given()
            .contentType(ContentType.JSON)
            .pathParam("firmId", E2eConfig.lspFirmNumber())
            .body(body)
            .when()
            .post("/provider-firms/{firmId}/offices")
            .then()
            .statusCode(201)
            .extract()
            .path("data.officeCode");
  }

  @Test
  @Order(1)
  void patchOffice_setDebtRecoveryFlagTrue_onActiveOffice_returns200() {
    patchOffice(Map.of("debtRecoveryFlag", true))
        .statusCode(200)
        .body("data.providerFirmGUID", notNullValue())
        .body("data.providerFirmNumber", equalTo(E2eConfig.lspFirmNumber()))
        .body("data.officeGUID", notNullValue())
        .body("data.officeCode", equalTo(officeCode));

    getOffice()
        .statusCode(200)
        .body("data.debtRecoveryFlag", equalTo(true))
        // Unrelated fields from the @BeforeAll fixture must survive this narrow flag patch.
        .body("data.address.line1", equalTo("1 Test Street"))
        .body("data.telephoneNumber", equalTo("0113 000 0001"));
  }

  @Test
  @Order(2)
  void patchOffice_setFalseBalanceFlagTrue_onActiveOffice_returns400() {
    patchOffice(Map.of("falseBalanceFlag", true))
        .statusCode(400)
        // AC2 requires "an appropriate error message is sent", not just a bare 400.
        .body("detail", containsString("falseBalanceFlag"));

    // No partial update: debtRecoveryFlag set in the previous test is untouched, and the
    // rejected falseBalanceFlag change was not applied.
    getOffice()
        .statusCode(200)
        .body("data.debtRecoveryFlag", equalTo(true))
        .body("data.falseBalanceFlag", not(equalTo(true)));
  }

  /// Deactivation is rejected when `paymentHeldFlag` is not explicitly held true in the
  /// same request (unless it is already true).
  ///
  /// - DSTEW-1674 AC2 – invalid status flag change rejected. (DS_MAPD_FR_045)
  ///
  /// - DS_MAPD_FR_045: Implement Status Flag Rules for LSP Entities.
  @Test
  @Order(3)
  void dstew1674_ac2_deactivateOffice_withoutPaymentHeldFlag_returns400() {
    patchOffice(Map.of("activeDateTo", LocalDate.now().toString()))
        .statusCode(400)
        // AC2 requires "an appropriate error message is sent", not just a bare 400.
        .body("detail", containsString("payment.paymentHeldFlag"));

    getOffice().statusCode(200).body("data.activeDateTo", nullValue());
  }

  /// Deactivation succeeds when `payment.paymentHeldFlag` is explicitly set to `true` in
  /// the same request.
  ///
  /// - DSTEW-1674 AC1 – successful status flag change accepted. (DS_MAPD_FR_045)
  ///
  /// - DS_MAPD_FR_045: Implement Status Flag Rules for LSP Entities.
  @Test
  @Order(4)
  void dstew1674_ac1_deactivateOffice_withPaymentHeldFlagTrue_returns200() {
    String deactivationDate = LocalDate.now().toString();

    patchOffice(
            Map.of(
                "activeDateTo",
                deactivationDate,
                "payment",
                Map.of(
                    "paymentMethod",
                    "CHECK",
                    "paymentHeldFlag",
                    true,
                    "paymentHeldReason",
                    "Deactivation hold")))
        .statusCode(200)
        .body("data.officeCode", equalTo(officeCode));

    // Full effect verified: activeDateTo set, payment.paymentHeldFlag held, and debtRecoveryFlag
    // auto-reset to false per the OpenAPI spec ("activeDateTo set on an LSP office ... the
    // debtRecoveryFlag should be set to false").
    getOffice()
        .statusCode(200)
        .body("data.activeDateTo", equalTo(deactivationDate))
        .body("data.debtRecoveryFlag", equalTo(false))
        .body("data.payment.paymentHeldFlag", equalTo(true));
  }

  @Test
  @Order(5)
  void patchOffice_setDebtRecoveryFlagTrue_onInactiveOffice_returns400() {
    patchOffice(Map.of("debtRecoveryFlag", true))
        .statusCode(400)
        // AC2 requires "an appropriate error message is sent", not just a bare 400.
        .body("detail", containsString("debtRecoveryFlag"));

    // No partial update: office remains inactive and debtRecoveryFlag remains false.
    getOffice()
        .statusCode(200)
        .body("data.activeDateTo", notNullValue())
        .body("data.debtRecoveryFlag", not(equalTo(true)));
  }

  @Test
  @Order(6)
  void patchOffice_setFalseBalanceFlagTrue_onInactiveOffice_returns200() {
    patchOffice(Map.of("falseBalanceFlag", true))
        .statusCode(200)
        .body("data.officeCode", equalTo(officeCode));

    getOffice().statusCode(200).body("data.falseBalanceFlag", equalTo(true));
  }

  /// Reactivation is rejected when `falseBalanceFlag`/`payment.paymentHeldFlag` are
  /// already `true` and are not explicitly cleared in the same request.
  ///
  /// - DSTEW-1674 AC2 – invalid status flag change rejected. (DS_MAPD_FR_045)
  ///
  /// - DS_MAPD_FR_045: Implement Status Flag Rules for LSP Entities.
  @Test
  @Order(7)
  void dstew1674_ac2_reactivateOffice_withoutClearingFlags_returns400() {
    patchOffice(Map.of("clearActiveDateTo", true))
        .statusCode(400)
        // AC2 requires "an appropriate error message is sent", not just a bare 400.
        .body("detail", containsString("payment.paymentHeldFlag"));

    getOffice()
        .statusCode(200)
        .body("data.activeDateTo", notNullValue())
        .body("data.falseBalanceFlag", equalTo(true))
        .body("data.payment.paymentHeldFlag", equalTo(true));
  }

  /// Reactivation succeeds when `falseBalanceFlag` and `payment.paymentHeldFlag` are
  /// explicitly cleared in the same request.
  ///
  /// - DSTEW-1674 AC1 – successful status flag change accepted. (DS_MAPD_FR_045)
  ///
  /// - DS_MAPD_FR_045: Implement Status Flag Rules for LSP Entities.
  @Test
  @Order(8)
  void dstew1674_ac1_reactivateOffice_withFlagsExplicitlyCleared_returns200() {
    patchOffice(
            Map.of(
                "clearActiveDateTo",
                true,
                "falseBalanceFlag",
                false,
                "payment",
                Map.of("paymentMethod", "CHECK", "paymentHeldFlag", false)))
        .statusCode(200)
        .body("data.officeCode", equalTo(officeCode));

    getOffice()
        .statusCode(200)
        .body("data.activeDateTo", nullValue())
        .body("data.falseBalanceFlag", not(equalTo(true)))
        .body("data.payment.paymentHeldFlag", not(equalTo(true)));
  }

  @Test
  @Order(9)
  void patchOffice_activeDateToAndClearActiveDateToTogether_returns400() {
    patchOffice(
            Map.of(
                "activeDateTo",
                LocalDate.now().toString(),
                "clearActiveDateTo",
                true,
                "telephoneNumber",
                "0113 000 0001"))
        .statusCode(400)
        .body("detail", containsString("activeDateTo"))
        .body("detail", containsString("clearActiveDateTo"));

    // No partial update: office remains active (from the previous test's reactivation).
    getOffice().statusCode(200).body("data.activeDateTo", nullValue());
  }

  /// Debt Recovery status can be removed on its own, independent of the office's
  /// activation state (logic table: Debt Recovery removal has no status dependency).
  ///
  /// - DSTEW-1674 AC1 – successful status flag change accepted. (DS_MAPD_FR_045)
  ///
  /// - DS_MAPD_FR_045: Implement Status Flag Rules for LSP Entities.
  @Test
  @Order(10)
  void dstew1674_ac1_removeDebtRecoveryFlag_whileActive_returns200() {
    patchOffice(Map.of("debtRecoveryFlag", true)).statusCode(200);

    patchOffice(Map.of("debtRecoveryFlag", false)).statusCode(200);

    getOffice().statusCode(200).body("data.debtRecoveryFlag", not(equalTo(true)));
  }

  /// Payment On Hold can be set and cleared independently of any activation change (logic
  /// table: Payment On Hold add/remove has no status dependency). See the TODO on {@link
  /// uk.gov.justice.laa.providerdata.service.OfficeService#validateActivationFlagTransitionRules}
  /// regarding whether same-request coupling with activation is actually required by the BA; this
  /// test documents that the API currently permits `paymentHeldFlag` changes on their own too.
  ///
  /// - DSTEW-1674 AC1 – successful status flag change accepted. (DS_MAPD_FR_045)
  ///
  /// - DS_MAPD_FR_045: Implement Status Flag Rules for LSP Entities.
  @Test
  @Order(11)
  void dstew1674_ac1_setAndClearPaymentHeldFlag_independentOfActivation_returns200() {
    patchOffice(
            Map.of(
                "payment",
                Map.of(
                    "paymentMethod",
                    "CHECK",
                    "paymentHeldFlag",
                    true,
                    "paymentHeldReason",
                    "Standalone hold")))
        .statusCode(200);

    getOffice()
        .statusCode(200)
        .body("data.activeDateTo", nullValue())
        .body("data.payment.paymentHeldFlag", equalTo(true));

    patchOffice(Map.of("payment", Map.of("paymentMethod", "CHECK", "paymentHeldFlag", false)))
        .statusCode(200);

    getOffice().statusCode(200).body("data.payment.paymentHeldFlag", not(equalTo(true)));
  }

  /// False Balance status can be removed while the office remains inactive, without
  /// reactivating it in the same request (logic table: False Balance removal only depends on the
  /// office being inactive, not on a reactivation transition).
  ///
  /// - DSTEW-1674 AC1 – successful status flag change accepted. (DS_MAPD_FR_045)
  ///
  /// - DS_MAPD_FR_045: Implement Status Flag Rules for LSP Entities.
  @Test
  @Order(12)
  void dstew1674_ac1_removeFalseBalanceFlag_whileInactive_withoutReactivating_returns200() {
    String deactivationDate = LocalDate.now().toString();

    patchOffice(
            Map.of(
                "activeDateTo",
                deactivationDate,
                "payment",
                Map.of(
                    "paymentMethod",
                    "CHECK",
                    "paymentHeldFlag",
                    true,
                    "paymentHeldReason",
                    "Deactivation hold")))
        .statusCode(200);

    patchOffice(Map.of("falseBalanceFlag", true)).statusCode(200);

    patchOffice(Map.of("falseBalanceFlag", false)).statusCode(200);

    getOffice()
        .statusCode(200)
        .body("data.activeDateTo", notNullValue())
        .body("data.falseBalanceFlag", not(equalTo(true)));
  }

  /// No partial update when a request mixes one invalid flag change with another flag
  /// change that would be valid in isolation: `falseBalanceFlag: true` is valid on an inactive
  /// office on its own, but the whole request must still be rejected because `debtRecoveryFlag`
  /// cannot be set to `true` while inactive, and neither field must be applied.
  ///
  /// - DSTEW-1674 AC2 – invalid status flag change rejected. (DS_MAPD_FR_045)
  ///
  /// - DS_MAPD_FR_045: Implement Status Flag Rules for LSP Entities.
  @Test
  @Order(13)
  void dstew1674_ac2_conflictingFlagsInSameRequest_rejectsWholeRequest_returns400() {
    // Arrange: ensure the office is inactive (it already is, from Order(12)) with both flags
    // false, so this test only observes the effect of this request's own field combination.
    getOffice()
        .statusCode(200)
        .body("data.activeDateTo", notNullValue())
        .body("data.debtRecoveryFlag", not(equalTo(true)))
        .body("data.falseBalanceFlag", not(equalTo(true)));

    patchOffice(Map.of("debtRecoveryFlag", true, "falseBalanceFlag", true))
        .statusCode(400)
        .body("detail", containsString("debtRecoveryFlag"));

    // No partial update: falseBalanceFlag (which would have been valid alone) must not have been
    // applied either, since the whole request was rejected.
    getOffice()
        .statusCode(200)
        .body("data.debtRecoveryFlag", not(equalTo(true)))
        .body("data.falseBalanceFlag", not(equalTo(true)));
  }

  /// `debtRecoveryFlag` follows the same same-request transition-coupling rule as
  /// `falseBalanceFlag`/`paymentHeldFlag`: reactivating and setting `debtRecoveryFlag: true` in
  /// the same request is accepted, because the office is validated against its post-transition
  /// (active) state.
  ///
  /// - DSTEW-1674 AC1 – successful status flag change accepted. (DS_MAPD_FR_045)
  ///
  /// - DS_MAPD_FR_045: Implement Status Flag Rules for LSP Entities.
  @Test
  @Order(14)
  void dstew1674_ac1_reactivateOffice_withDebtRecoveryFlagTrue_returns200() {
    // Arrange: office is inactive from Order(13), with debtRecoveryFlag/falseBalanceFlag false
    // and payment.paymentHeldFlag true (set during Order(12)'s deactivation, never cleared).
    getOffice().statusCode(200).body("data.activeDateTo", notNullValue());

    patchOffice(
            Map.of(
                "clearActiveDateTo",
                true,
                "debtRecoveryFlag",
                true,
                "falseBalanceFlag",
                false,
                "payment",
                Map.of("paymentMethod", "CHECK", "paymentHeldFlag", false)))
        .statusCode(200);

    getOffice()
        .statusCode(200)
        .body("data.activeDateTo", nullValue())
        .body("data.debtRecoveryFlag", equalTo(true));
  }

  /// The reverse coupling is rejected: deactivating and setting `debtRecoveryFlag: true`
  /// (even redundantly, when it is already `true`) in the same request must fail, because the
  /// office is validated against its post-transition (inactive) state.
  ///
  /// - DSTEW-1674 AC2 – invalid status flag change rejected. (DS_MAPD_FR_045)
  ///
  /// - DS_MAPD_FR_045: Implement Status Flag Rules for LSP Entities.
  @Test
  @Order(15)
  void dstew1674_ac2_deactivateOffice_withDebtRecoveryFlagTrue_returns400() {
    // Arrange: office is active from Order(14), with debtRecoveryFlag already true.
    patchOffice(
            Map.of(
                "activeDateTo",
                LocalDate.now().toString(),
                "debtRecoveryFlag",
                true,
                "payment",
                Map.of(
                    "paymentMethod",
                    "CHECK",
                    "paymentHeldFlag",
                    true,
                    "paymentHeldReason",
                    "Deactivation hold")))
        .statusCode(400)
        .body("detail", containsString("debtRecoveryFlag"));

    // No partial update: office remains active and debtRecoveryFlag remains unchanged.
    getOffice()
        .statusCode(200)
        .body("data.activeDateTo", nullValue())
        .body("data.debtRecoveryFlag", equalTo(true));
  }

  /// `falseBalanceFlag` may be set to `true` in the *same* request that deactivates the
  /// office, not only as a separate follow-up call: the office is validated against its
  /// post-transition (inactive) state, exactly as already proven for `debtRecoveryFlag` +
  /// reactivation above.
  ///
  /// - DSTEW-1674 AC1 – successful status flag change accepted. (DS_MAPD_FR_045)
  ///
  /// - DS_MAPD_FR_045: Implement Status Flag Rules for LSP Entities.
  @Test
  @Order(16)
  void dstew1674_ac1_deactivateOffice_withFalseBalanceFlagTrue_returns200() {
    // Arrange: office is active from Order(15), with falseBalanceFlag false.
    String deactivationDate = LocalDate.now().toString();

    patchOffice(
            Map.of(
                "activeDateTo",
                deactivationDate,
                "falseBalanceFlag",
                true,
                "payment",
                Map.of(
                    "paymentMethod",
                    "CHECK",
                    "paymentHeldFlag",
                    true,
                    "paymentHeldReason",
                    "Deactivation hold")))
        .statusCode(200);

    getOffice()
        .statusCode(200)
        .body("data.activeDateTo", equalTo(deactivationDate))
        .body("data.falseBalanceFlag", equalTo(true));
  }

  @Test
  @Order(17)
  void patchOffice_unknownOfficeCode_returns404() {
    given()
        .contentType(ContentType.JSON)
        .pathParam("firmId", E2eConfig.lspFirmNumber())
        .pathParam("officeCode", E2eConfig.invalidOfficeCode())
        .body(Map.of("debtRecoveryFlag", true))
        .when()
        .patch("/provider-firms/{firmId}/offices/{officeCode}")
        .then()
        .statusCode(404);
  }

  /// Sends {@code PATCH /provider-firms/{firmId}/offices/{officeCode}} for the fixture office
  /// created in {@code @BeforeAll}, with the given request body.
  private static ValidatableResponse patchOffice(Map<String, Object> body) {
    return given()
        .contentType(ContentType.JSON)
        .pathParam("firmId", E2eConfig.lspFirmNumber())
        .pathParam("officeCode", officeCode)
        .body(body)
        .when()
        .patch("/provider-firms/{firmId}/offices/{officeCode}")
        .then();
  }

  /// Fetches the fixture office created in {@code @BeforeAll} via {@code GET
  /// /provider-firms/{firmId}/offices/{officeCode}}, to verify the effect of a preceding {@link
  /// #patchOffice}.
  private static ValidatableResponse getOffice() {
    return given()
        .pathParam("firmId", E2eConfig.lspFirmNumber())
        .pathParam("officeCode", officeCode)
        .when()
        .get("/provider-firms/{firmId}/offices/{officeCode}")
        .then();
  }
}
