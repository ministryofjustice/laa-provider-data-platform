package uk.gov.justice.laa.providerdata.e2e;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.Test;

@ReadOnlyTest
class ProviderFirmOauth2E2eTest {

  @Test
  void getProviderFirm_withOauth2Configuration_returns200() {
    assumeOauth2E2eEnabled();

    given()
        .pathParam("firmId", E2eConfig.lspFirmNumber())
        .when()
        .get("/provider-firms/{firmId}")
        .then()
        .statusCode(200)
        .body("data.firmNumber", equalTo(E2eConfig.lspFirmNumber()));
  }

  @Test
  void getProviderFirm_withValidOauth2AndInvalidApiKey_returns200() {
    assumeOauth2E2eEnabled();

    given()
        .header("X-Authorization", "invalid-api-key")
        .pathParam("firmId", E2eConfig.lspFirmNumber())
        .when()
        .get("/provider-firms/{firmId}")
        .then()
        .statusCode(200)
        .body("data.firmNumber", equalTo(E2eConfig.lspFirmNumber()));
  }

  @Test
  void getProviderFirm_withInvalidOauth2BearerToken_returns401()
      throws IOException, InterruptedException {
    assumeOauth2E2eEnabled();

    HttpRequest request =
        HttpRequest.newBuilder(
                URI.create(E2eConfig.baseUri() + "/provider-firms/" + E2eConfig.lspFirmNumber()))
            .header("Authorization", "Bearer invalid-oauth2-token")
            .GET()
            .build();

    HttpResponse<String> response =
        HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());

    org.junit.jupiter.api.Assertions.assertEquals(401, response.statusCode());
  }

  private static void assumeOauth2E2eEnabled() {
    assumeTrue(
        E2eConfig.hasOauth2ClientCredentialsConfig()
            || "Authorization".equalsIgnoreCase(E2eConfig.authHeader()),
        "OAuth2 E2E is not configured. "
            + "Set E2E_OAUTH2_TOKEN_URL, E2E_OAUTH2_CLIENT_ID, E2E_OAUTH2_CLIENT_SECRET, "
            + "E2E_OAUTH2_SCOPE.");
    assumeTrue(
        E2eConfig.isAppOauth2Enabled(),
        "OAuth2 E2E requires the target app to run with APP_SECURITY_OAUTH2_ENABLED=true.");
  }
}
