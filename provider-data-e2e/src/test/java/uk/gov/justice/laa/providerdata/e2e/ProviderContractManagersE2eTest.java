package uk.gov.justice.laa.providerdata.e2e;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;

import org.junit.jupiter.api.Test;

@ReadOnlyTest
class ProviderContractManagersE2eTest {

  @Test
  void getContractManagers_noFilters_returnsPaginatedList() {
    given()
        .when()
        .get("/provider-contract-managers")
        .then()
        .statusCode(200)
        .body("data.content", hasSize(greaterThanOrEqualTo(1)))
        .body("data.metadata.pagination.totalItems", greaterThanOrEqualTo(1));
  }

  @Test
  void getContractManagers_filterByContractManagerId_returnsResults() {
    given()
        .queryParam("contractManagerId", E2eConfig.contractManagerId())
        .when()
        .get("/provider-contract-managers")
        .then()
        .statusCode(200)
        .body("data.content", hasSize(greaterThanOrEqualTo(1)));
  }

  @Test
  void getContractManagers_filterByName_returnsResults() {
    given()
        .queryParam("name", E2eConfig.contractManagerLastName())
        .when()
        .get("/provider-contract-managers")
        .then()
        .statusCode(200)
        .body("data.content", hasSize(greaterThanOrEqualTo(1)));
  }

  @Test
  void getContractManagers_filterByNonMatchingId_returnsEmpty() {
    given()
        .queryParam("contractManagerId", "UNKNOWN-CM-999")
        .when()
        .get("/provider-contract-managers")
        .then()
        .statusCode(200)
        .body("data.content", empty())
        .body("data.metadata.pagination.totalItems", equalTo(0));
  }
}
