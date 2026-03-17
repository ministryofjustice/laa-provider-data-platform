package uk.gov.justice.laa.providerdata.e2e;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;

import org.junit.jupiter.api.Test;

@ReadOnlyTest
class ProviderFirmOfficesE2eTest {

  @Test
  void getProviderFirmOffices_returnsPaginatedListContainingSeededOffice() {
    given()
        .pathParam("firmId", E2eConfig.lspFirmNumber())
        .when()
        .get("/provider-firms/{firmId}/offices")
        .then()
        .statusCode(200)
        .body("data.content", hasSize(greaterThanOrEqualTo(1)))
        .body("data.content[0].accountNumber", equalTo(E2eConfig.lspOfficeCode()))
        .body("data.content[0].firmType", equalTo(E2eConfig.lspFirmType()))
        .body("data.metadata.pagination.totalItems", greaterThanOrEqualTo(1))
        .body("data.metadata.searchCriteria.criteria", empty());
  }

  @Test
  void getProviderFirmOffices_unknownFirm_returns404() {
    given()
        .pathParam("firmId", E2eConfig.invalidFirmNumber())
        .when()
        .get("/provider-firms/{firmId}/offices")
        .then()
        .statusCode(404)
        .body("error.errorCode", equalTo("P00NF"));
  }

  @Test
  void getProviderFirmOfficeByCode_returns200WithAddressAndPaymentDetails() {
    given()
        .pathParam("firmId", E2eConfig.lspFirmNumber())
        .pathParam("officeCode", E2eConfig.lspOfficeCode())
        .when()
        .get("/provider-firms/{firmId}/offices/{officeCode}")
        .then()
        .statusCode(200)
        .body("data.accountNumber", equalTo(E2eConfig.lspOfficeCode()))
        .body("data.firmType", equalTo(E2eConfig.lspFirmType()))
        .body("data.address.line1", equalTo(E2eConfig.lspOfficeAddressLine1()))
        .body("data.address.line2", equalTo(E2eConfig.lspOfficeAddressLine2()))
        .body("data.address.townOrCity", equalTo(E2eConfig.lspOfficeAddressTownOrCity()))
        .body("data.address.postcode", equalTo(E2eConfig.lspOfficeAddressPostcode()))
        .body("data.telephoneNumber", equalTo(E2eConfig.lspOfficeTelephoneNumber()))
        .body("data.emailAddress", equalTo(E2eConfig.lspOfficeEmailAddress()))
        .body("data.dxDetails.dxNumber", equalTo(E2eConfig.lspOfficeDxNumber()))
        .body("data.dxDetails.dxCentre", equalTo(E2eConfig.lspOfficeDxCentre()))
        .body("data.vatRegistration.vatNumber", equalTo(E2eConfig.lspOfficeVatNumber()))
        .body("data.payment.paymentMethod", equalTo(E2eConfig.lspOfficePaymentMethod()))
        // Active office has no end date
        .body("data.activeDateTo", nullValue());
  }

  @Test
  void getProviderFirmOfficeByCode_unknownOfficeCode_returns404() {
    given()
        .pathParam("firmId", E2eConfig.lspFirmNumber())
        .pathParam("officeCode", E2eConfig.invalidOfficeCode())
        .when()
        .get("/provider-firms/{firmId}/offices/{officeCode}")
        .then()
        .statusCode(404)
        .body("error.errorCode", equalTo("P00NF"));
  }

  @Test
  void getProviderFirmOfficeByCode_unknownFirm_returns404() {
    given()
        .pathParam("firmId", E2eConfig.invalidFirmNumber())
        .pathParam("officeCode", E2eConfig.lspOfficeCode())
        .when()
        .get("/provider-firms/{firmId}/offices/{officeCode}")
        .then()
        .statusCode(404)
        .body("error.errorCode", equalTo("P00NF"));
  }
}
