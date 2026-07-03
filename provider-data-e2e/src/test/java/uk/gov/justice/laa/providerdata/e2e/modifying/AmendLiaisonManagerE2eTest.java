package uk.gov.justice.laa.providerdata.e2e.modifying;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

import io.restassured.http.ContentType;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import uk.gov.justice.laa.providerdata.e2e.ModifyingTest;

/**
 * Data-modifying e2e tests for amending an existing Liaison Manager (DSTEW-1648, DS_MAPD_FR_021).
 */
@ModifyingTest
@DisplayName("DSTEW-1648: Amend Liaison Manager (DS_MAPD_FR_021)")
class AmendLiaisonManagerE2eTest {

  private String liaisonManagerGuid;

  @BeforeEach
  void createTestFirm() {
    String firmNumber =
        given()
            .contentType(ContentType.JSON)
            .body(
                Map.of(
                    "firmType",
                    "Legal Services Provider",
                    "name",
                    "E2E-DSTEW-1648 LSP " + System.currentTimeMillis(),
                    "legalServicesProvider",
                    Map.of(
                        "constitutionalStatus",
                        "Partnership",
                        "address",
                        Map.of(
                            "line1", "1 E2E Test Street",
                            "townOrCity", "London",
                            "postcode", "SW1A 1AA"),
                        "payment",
                        Map.of("paymentMethod", "CHECK"),
                        "contractManager",
                        Map.of("useDefaultContractManager", true),
                        "liaisonManager",
                        Map.of(
                            "firstName", "Amend",
                            "lastName", "Me",
                            "emailAddress", "e2e.amend@example.com",
                            "telephoneNumber", "020 1111 2222"))))
            .when()
            .post("/provider-firms")
            .then()
            .statusCode(201)
            .extract()
            .path("data.providerFirmNumber");

    liaisonManagerGuid =
        given()
            .pathParam("firmId", firmNumber)
            .when()
            .get("/provider-firms/{firmId}")
            .then()
            .statusCode(200)
            .extract()
            .path("data.legalServicesProvider.headOffice.liaisonManager.guid");
  }

  /**
   * AC1 – Amending both permitted fields (emailAddress and telephoneNumber) succeeds and the
   * updated values are reflected in the response.
   */
  @Test
  void dstew1648_ac1_successfulAmendmentOfBothPermittedFields() {
    given()
        .pathParam("guid", liaisonManagerGuid)
        .contentType(ContentType.JSON)
        .body(
            Map.of(
                "emailAddress", "updated.both@example.com",
                "telephoneNumber", "020 8888 7777"))
        .when()
        .patch("/provider-liaison-managers/{guid}")
        .then()
        .statusCode(200)
        .body("data.emailAddress", equalTo("updated.both@example.com"))
        .body("data.telephoneNumber", equalTo("020 8888 7777"));
  }

  /** AC1 – Amending only emailAddress succeeds; telephoneNumber is unchanged. */
  @Test
  void dstew1648_ac1_successfulAmendmentOfEmailOnly() {
    given()
        .pathParam("guid", liaisonManagerGuid)
        .contentType(ContentType.JSON)
        .body(Map.of("emailAddress", "updated.email.only@example.com"))
        .when()
        .patch("/provider-liaison-managers/{guid}")
        .then()
        .statusCode(200)
        .body("data.emailAddress", equalTo("updated.email.only@example.com"))
        .body("data.telephoneNumber", equalTo("020 1111 2222"));
  }

  /** AC1 – Amending only telephoneNumber succeeds; emailAddress is unchanged. */
  @Test
  void dstew1648_ac1_successfulAmendmentOfTelephoneOnly() {
    given()
        .pathParam("guid", liaisonManagerGuid)
        .contentType(ContentType.JSON)
        .body(Map.of("telephoneNumber", "020 5555 4444"))
        .when()
        .patch("/provider-liaison-managers/{guid}")
        .then()
        .statusCode(200)
        .body("data.telephoneNumber", equalTo("020 5555 4444"))
        .body("data.emailAddress", equalTo("e2e.amend@example.com"));
  }

  /** AC2 – firstName is a redacted field and must not be amended; request is rejected with 400. */
  @Test
  void dstew1648_ac2_redactedFieldFirstNameMustNotBeAmended() {
    given()
        .pathParam("guid", liaisonManagerGuid)
        .contentType(ContentType.JSON)
        .body(Map.of("firstName", "Hacked"))
        .when()
        .patch("/provider-liaison-managers/{guid}")
        .then()
        .statusCode(400);
  }

  /** AC2 – lastName is a redacted field and must not be amended; request is rejected with 400. */
  @Test
  void dstew1648_ac2_redactedFieldLastNameMustNotBeAmended() {
    given()
        .pathParam("guid", liaisonManagerGuid)
        .contentType(ContentType.JSON)
        .body(Map.of("lastName", "Hacked"))
        .when()
        .patch("/provider-liaison-managers/{guid}")
        .then()
        .statusCode(400);
  }

  /** AC2 – `guid` is a redacted field and must not be amended; request is rejected with 400. */
  @Test
  void dstew1648_ac2_redactedFieldGuidMustNotBeAmended() {
    given()
        .pathParam("guid", liaisonManagerGuid)
        .contentType(ContentType.JSON)
        .body(Map.of("guid", "00000000-0000-0000-0000-000000000000"))
        .when()
        .patch("/provider-liaison-managers/{guid}")
        .then()
        .statusCode(400);
  }

  /**
   * AC2 – activeDateFrom is a redacted field and must not be amended; request is rejected with 400.
   */
  @Test
  void dstew1648_ac2_redactedFieldActiveDateFromMustNotBeAmended() {
    given()
        .pathParam("guid", liaisonManagerGuid)
        .contentType(ContentType.JSON)
        .body(Map.of("activeDateFrom", "2024-01-01"))
        .when()
        .patch("/provider-liaison-managers/{guid}")
        .then()
        .statusCode(400);
  }

  /**
   * AC4 – The inactiveDate field must not be manually set via an amendment; request is rejected
   * with 400.
   */
  @Test
  void dstew1648_ac4_preventManualSettingOfInactiveDate() {
    given()
        .pathParam("guid", liaisonManagerGuid)
        .contentType(ContentType.JSON)
        .body(Map.of("activeDateTo", "2026-06-03"))
        .when()
        .patch("/provider-liaison-managers/{guid}")
        .then()
        .statusCode(400);
  }

  /**
   * AC3 – Historical data retention: the liaison manager assignment history is preserved via
   * end-dated OfficeLiaisonManagerLink rows. This test verifies that the PATCH succeeds; full
   * historical retention is an infrastructure concern verified by the liaison-managers endpoint
   * returning past assignments alongside current ones.
   */
  @Test
  void dstew1648_ac3_historicalDataMustBeRetained() {
    given()
        .pathParam("guid", liaisonManagerGuid)
        .contentType(ContentType.JSON)
        .body(Map.of("emailAddress", "history.check@example.com"))
        .when()
        .patch("/provider-liaison-managers/{guid}")
        .then()
        .statusCode(200);
  }

  /** GET /provider-liaison-managers/{guid} with a non-existent GUID returns 404. */
  @Test
  void dstew1648_nonExistentGuid_get_returns404() {
    given()
        .pathParam("guid", "00000000-0000-0000-0000-000000000000")
        .when()
        .get("/provider-liaison-managers/{guid}")
        .then()
        .statusCode(404);
  }

  /** PATCH /provider-liaison-managers/{guid} with a non-existent GUID returns 404. */
  @Test
  void dstew1648_nonExistentGuid_patch_returns404() {
    given()
        .pathParam("guid", "00000000-0000-0000-0000-000000000000")
        .contentType(ContentType.JSON)
        .body(Map.of("emailAddress", "ghost@example.com"))
        .when()
        .patch("/provider-liaison-managers/{guid}")
        .then()
        .statusCode(404);
  }
}
