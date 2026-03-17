package uk.gov.justice.laa.providerdata.e2e;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;

import org.junit.jupiter.api.Test;

@ReadOnlyTest
class ProviderFirmOfficesGlobalE2eTest {

  @Test
  void getOffices_noFilters_returnsPaginatedListWithEmptySearchCriteria() {
    given()
        .when()
        .get("/provider-firms-offices")
        .then()
        .statusCode(200)
        .body("data.content", hasSize(greaterThanOrEqualTo(1)))
        .body("data.metadata.pagination.totalItems", greaterThanOrEqualTo(1))
        .body("data.metadata.searchCriteria.criteria", empty());
  }

  @Test
  void getOffices_filterByOfficeCode_returnsMatchingOfficeAndEchoesCriteria() {
    given()
        .queryParam("officeCode", E2eConfig.lspOfficeCode())
        .when()
        .get("/provider-firms-offices")
        .then()
        .statusCode(200)
        .body("data.content", hasSize(greaterThanOrEqualTo(1)))
        .body("data.content[0].accountNumber", equalTo(E2eConfig.lspOfficeCode()))
        .body("data.metadata.searchCriteria.criteria[0].filter", equalTo("officeCode"))
        .body(
            "data.metadata.searchCriteria.criteria[0].values[0]",
            equalTo(E2eConfig.lspOfficeCode()));
  }

  @Test
  void
      getOffices_filterByOfficeCodeWithAllProviderOffices_returnsAllOfficesForProviderAndEchoesCriteria() {
    given()
        .queryParam("officeCode", E2eConfig.lspOfficeCode())
        .queryParam("allProviderOffices", "true")
        .when()
        .get("/provider-firms-offices")
        .then()
        .statusCode(200)
        .body("data.content", hasSize(greaterThanOrEqualTo(1)))
        .body("data.metadata.searchCriteria.criteria[0].filter", equalTo("officeCode"))
        .body("data.metadata.searchCriteria.criteria[1].filter", equalTo("allProviderOffices"))
        .body("data.metadata.searchCriteria.criteria[1].values[0]", equalTo("true"));
  }
}
