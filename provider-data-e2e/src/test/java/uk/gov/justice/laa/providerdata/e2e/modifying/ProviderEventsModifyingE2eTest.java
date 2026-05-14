package uk.gov.justice.laa.providerdata.e2e.modifying;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;

import io.restassured.http.ContentType;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import uk.gov.justice.laa.providerdata.e2e.ModifyingTest;

/**
 * Modifying e2e tests for event history endpoints.
 *
 * <p>A provider firm is created in {@link #createFirm()} with a unique correlation ID so that event
 * assertions are scoped to this test run and isolated from any pre-existing data. Test data is
 * prefixed with {@code "E2E-DSTEW Events "}.
 */
@ModifyingTest
class ProviderEventsModifyingE2eTest {

  private static String correlationId;
  private static String eventGuid;

  @BeforeAll
  static void createFirm() {
    correlationId = "e2e-events-" + UUID.randomUUID();

    given()
        .contentType(ContentType.JSON)
        .header("x-correlation-id", correlationId)
        .body(lspFirmBody("E2E-DSTEW Events " + System.currentTimeMillis()))
        .when()
        .post("/provider-firms")
        .then()
        .statusCode(201);

    eventGuid =
        given()
            .queryParam("correlationId", correlationId)
            .when()
            .get("/provider-events")
            .then()
            .statusCode(200)
            .extract()
            .path("data.content[0].guid");
  }

  /**
   * Firm creation produces exactly one snapshot event, visible in the event history for that
   * correlation ID.
   *
   * <p>Exactly one event is expected because the correlation ID is a unique UUID generated at the
   * start of this test run; no prior test data can match it.
   */
  @Test
  void newFirmCreation_isRecordedInEventHistory() {
    given()
        .queryParam("correlationId", correlationId)
        .when()
        .get("/provider-events")
        .then()
        .statusCode(200)
        .body("data.content", hasSize(1))
        .body("data.content[0].eventType", equalTo("ProviderFirmChangedSnapshotEvent"))
        .body("data.content[0].correlationId", equalTo(correlationId));
  }

  /** The event recorded for a new firm is retrievable by its GUID with full payload. */
  @Test
  void eventCreatedForNewFirm_isRetrievableByGuid() {
    given()
        .pathParam("eventGUID", eventGuid)
        .when()
        .get("/provider-events/{eventGUID}")
        .then()
        .statusCode(200)
        .body("data.eventHeader.guid", equalTo(eventGuid))
        .body("data.eventHeader.eventType", equalTo("ProviderFirmChangedSnapshotEvent"))
        .body("data.eventHeader.correlationId", equalTo(correlationId))
        .body("data.eventPayload", notNullValue());
  }

  private static Map<String, Object> lspFirmBody(String firmName) {
    return Map.of(
        "firmType",
        "Legal Services Provider",
        "name",
        firmName,
        "legalServicesProvider",
        Map.of(
            "address",
            Map.of(
                "line1", "1 Events Street",
                "townOrCity", "London",
                "postcode", "EC1A 1BB"),
            "payment",
            Map.of("paymentMethod", "EFT"),
            "liaisonManager",
            Map.of(
                "firstName", "Events",
                "lastName", "Test",
                "emailAddress", "events.test@example.com",
                "telephoneNumber", "020 5555 6666")));
  }
}
