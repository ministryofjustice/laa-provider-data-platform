package uk.gov.justice.laa.providerdata.e2e;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;

import org.junit.jupiter.api.Test;

@ReadOnlyTest
class ChamberOfficePractitionersE2eTest {

  @Test
  void getPractitioners_forChambers_returns200WithExpectedAdvocate() {
    given()
        .pathParam("firmId", E2eConfig.chambersFirmNumber())
        .when()
        .get("/provider-firms/{firmId}/practitioners")
        .then()
        .statusCode(200)
        .body("data.content", hasSize(greaterThanOrEqualTo(1)))
        .body("data.content.firmNumber", hasItem(E2eConfig.advocateFirmNumber()))
        .body("data.content.name", hasItem(E2eConfig.advocateName()))
        .body("data.content.firmType", hasItem(E2eConfig.advocateFirmType()))
        .body("data.metadata.pagination.totalItems", greaterThanOrEqualTo(1));
  }

  @Test
  void getPractitioners_unknownFirm_returns404() {
    given()
        .pathParam("firmId", E2eConfig.invalidFirmNumber())
        .when()
        .get("/provider-firms/{firmId}/practitioners")
        .then()
        .statusCode(404)
        .body("error.errorCode", equalTo("P00NF"));
  }

  // TODO: Add getPractitioners_forNonChambersFirm_returns400 test once the OpenAPI spec
  // defines 400 as a valid response for this endpoint.
}
