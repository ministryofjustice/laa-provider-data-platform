package uk.gov.justice.laa.providerdata.e2e.modifying;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

import io.restassured.http.ContentType;
import java.time.LocalDate;
import java.util.Map;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import uk.gov.justice.laa.providerdata.e2e.E2eConfig;
import uk.gov.justice.laa.providerdata.e2e.ModifyingTest;

/**
 * Data-modifying e2e tests for office activation/deactivation via {@code PATCH
 * /provider-firms/{firmId}/offices/{officeCode}}.
 *
 * <p>Tests run in order because the office state changes from active to inactive mid-suite. A fresh
 * non-head office is created in {@code @BeforeAll} so that deactivation does not cascade to other
 * offices or affect other test classes.
 */
@ModifyingTest
@TestMethodOrder(OrderAnnotation.class)
class PatchOfficeActivationE2eTest {

  private static String officeCode;

  @BeforeAll
  static void createNonHeadOffice() {
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
            Map.of("paymentMethod", "EFT"),
            "liaisonManager",
            Map.of("useHeadOfficeLiaisonManager", true));

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
    given()
        .contentType(ContentType.JSON)
        .pathParam("firmId", E2eConfig.lspFirmNumber())
        .pathParam("officeCode", officeCode)
        .body(Map.of("debtRecoveryFlag", true))
        .when()
        .patch("/provider-firms/{firmId}/offices/{officeCode}")
        .then()
        .statusCode(200)
        .body("data.providerFirmGUID", notNullValue())
        .body("data.providerFirmNumber", equalTo(E2eConfig.lspFirmNumber()))
        .body("data.officeGUID", notNullValue())
        .body("data.officeCode", equalTo(officeCode));
  }

  @Test
  @Order(2)
  void patchOffice_setFalseBalanceFlagTrue_onActiveOffice_returns400() {
    given()
        .contentType(ContentType.JSON)
        .pathParam("firmId", E2eConfig.lspFirmNumber())
        .pathParam("officeCode", officeCode)
        .body(Map.of("falseBalanceFlag", true))
        .when()
        .patch("/provider-firms/{firmId}/offices/{officeCode}")
        .then()
        .statusCode(400);
  }

  @Test
  @Order(3)
  void patchOffice_deactivateOffice_returns200() {
    String deactivationDate = LocalDate.now().toString();

    given()
        .contentType(ContentType.JSON)
        .pathParam("firmId", E2eConfig.lspFirmNumber())
        .pathParam("officeCode", officeCode)
        .body(Map.of("activeDateTo", deactivationDate))
        .when()
        .patch("/provider-firms/{firmId}/offices/{officeCode}")
        .then()
        .statusCode(200)
        .body("data.officeCode", equalTo(officeCode));
  }

  @Test
  @Order(4)
  void patchOffice_setDebtRecoveryFlagTrue_onInactiveOffice_returns400() {
    given()
        .contentType(ContentType.JSON)
        .pathParam("firmId", E2eConfig.lspFirmNumber())
        .pathParam("officeCode", officeCode)
        .body(Map.of("debtRecoveryFlag", true))
        .when()
        .patch("/provider-firms/{firmId}/offices/{officeCode}")
        .then()
        .statusCode(400);
  }

  @Test
  @Order(5)
  void patchOffice_setFalseBalanceFlagTrue_onInactiveOffice_returns200() {
    given()
        .contentType(ContentType.JSON)
        .pathParam("firmId", E2eConfig.lspFirmNumber())
        .pathParam("officeCode", officeCode)
        .body(Map.of("falseBalanceFlag", true))
        .when()
        .patch("/provider-firms/{firmId}/offices/{officeCode}")
        .then()
        .statusCode(200)
        .body("data.officeCode", equalTo(officeCode));
  }

  @Test
  @Order(6)
  void patchOffice_reactivateOffice_returns200() {
    given()
        .contentType(ContentType.JSON)
        .pathParam("firmId", E2eConfig.lspFirmNumber())
        .pathParam("officeCode", officeCode)
        .body(Map.of("clearActiveDateTo", true))
        .when()
        .patch("/provider-firms/{firmId}/offices/{officeCode}")
        .then()
        .statusCode(200)
        .body("data.officeCode", equalTo(officeCode));
  }

  @Test
  @Order(7)
  void patchOffice_activeDateToAndClearActiveDateToTogether_returns400() {
    given()
        .contentType(ContentType.JSON)
        .pathParam("firmId", E2eConfig.lspFirmNumber())
        .pathParam("officeCode", officeCode)
        .body(
            Map.of(
                "activeDateTo",
                LocalDate.now().toString(),
                "clearActiveDateTo",
                true,
                "telephoneNumber",
                "0113 000 0001"))
        .when()
        .patch("/provider-firms/{firmId}/offices/{officeCode}")
        .then()
        .statusCode(400);
  }

  @Test
  @Order(8)
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
}
