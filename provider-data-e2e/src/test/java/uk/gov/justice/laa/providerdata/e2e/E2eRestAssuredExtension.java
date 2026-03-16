package uk.gov.justice.laa.providerdata.e2e;

import com.atlassian.oai.validator.restassured.OpenApiValidationFilter;
import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.config.HttpClientConfig;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

/**
 * JUnit 5 extension that configures RestAssured before each e2e test class runs.
 *
 * <p>Applied automatically via {@link ReadOnlyTest} and {@link DestructiveTest} — test classes do
 * not need to reference this extension directly.
 *
 * <p>Sets up:
 *
 * <ul>
 *   <li>Base URI and optional auth token from {@link E2eConfig}
 *   <li>OpenAPI response validation against {@code laa-data-pda.yml}
 *   <li>Connection and socket timeouts
 *   <li>Request/response logging on validation failure
 * </ul>
 */
class E2eRestAssuredExtension implements BeforeAllCallback {

  private static final int CONNECTION_TIMEOUT_MS = 20_000;
  private static final int SOCKET_TIMEOUT_MS = 100_000;

  private static final OpenApiValidationFilter OPENAPI_FILTER =
      new OpenApiValidationFilter("classpath:laa-data-pda.yml");

  @Override
  public void beforeAll(ExtensionContext context) {
    String baseUri = E2eConfig.baseUri();
    String authToken = E2eConfig.authToken();

    if (baseUri == null || baseUri.isBlank()) {
      throw new IllegalStateException(
          "Missing required e2e configuration: set system property 'e2e.baseUri' or env var 'E2E_BASEURI'");
    }

    RequestSpecBuilder builder =
        new RequestSpecBuilder()
            .setBaseUri(baseUri)
            .setContentType(ContentType.JSON)
            .addFilter(OPENAPI_FILTER);

    if (authToken != null && !authToken.isBlank()) {
      builder.addHeader("X-Authorization", authToken);
    }

    RestAssured.requestSpecification = builder.build();

    RestAssured.config =
        RestAssured.config()
            .httpClient(
                HttpClientConfig.httpClientConfig()
                    .setParam("http.connection.timeout", CONNECTION_TIMEOUT_MS)
                    .setParam("http.socket.timeout", SOCKET_TIMEOUT_MS));

    RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
  }
}
