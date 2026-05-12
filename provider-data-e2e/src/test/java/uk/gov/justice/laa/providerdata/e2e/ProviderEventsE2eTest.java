package uk.gov.justice.laa.providerdata.e2e;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.notNullValue;

import org.junit.jupiter.api.Test;

/**
 * Read-only e2e tests for the event history endpoints.
 *
 * <p>Error-case tests do not require pre-existing event data and are safe as read-only. Happy-path
 * tests that exercise write-then-read behaviour are in {@link ProviderEventsModifyingE2eTest}.
 */
@ReadOnlyTest
class ProviderEventsE2eTest {

  @Test
  void eventList_withNoFilters_isPaginatedWithMetadata() {
    given()
        .when()
        .get("/provider-events")
        .then()
        .statusCode(200)
        .body("data.content", notNullValue())
        .body("data.metadata.pagination", notNullValue())
        .body("data.metadata.pagination.pageSize", greaterThanOrEqualTo(1));
  }

  @Test
  void eventList_filteredByEventType_isAccepted() {
    given()
        .queryParam("eventType", "ProviderFirmChangedSnapshotEvent")
        .when()
        .get("/provider-events")
        .then()
        .statusCode(200)
        .body("data.content", notNullValue());
  }

  @Test
  void eventByUnknownGuid_isNotFound() {
    given()
        .pathParam("eventGUID", "00000000-0000-0000-0000-000000000000")
        .when()
        .get("/provider-events/{eventGUID}")
        .then()
        .statusCode(404);
  }

  @Test
  void eventByInvalidGuid_isRejected() {
    given()
        .pathParam("eventGUID", "not-a-valid-uuid")
        .when()
        .get("/provider-events/{eventGUID}")
        .then()
        .statusCode(400);
  }
}
