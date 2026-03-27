package uk.gov.justice.laa.providerdata.e2e;

import com.atlassian.oai.validator.OpenApiInteractionValidator;
import com.atlassian.oai.validator.report.LevelResolver;
import com.atlassian.oai.validator.report.ValidationReport;
import com.atlassian.oai.validator.restassured.OpenApiValidationFilter;
import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.config.HttpClientConfig;
import io.restassured.http.ContentType;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

/**
 * JUnit 5 extension that configures RestAssured before each e2e test class runs.
 *
 * <p>Applied automatically via {@link ReadOnlyTest} and {@link ModifyingTest} — test classes do not
 * need to reference this extension directly.
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

  private static final OpenApiValidationFilter OPENAPI_FILTER = createValidationFilter();

  private static OpenApiValidationFilter createValidationFilter() {
    try (InputStream is =
        E2eRestAssuredExtension.class.getClassLoader().getResourceAsStream("laa-data-pda.yml")) {
      if (is == null) {
        throw new IllegalStateException("Cannot find laa-data-pda.yml on classpath");
      }
      String spec = new String(is.readAllBytes(), StandardCharsets.UTF_8);
      // The spec declares application/json for error responses, but the service correctly returns
      // application/problem+json (RFC 7807). Downgrade to WARN until the spec is updated.
      //
      // The spec uses allOf:[{$ref:BaseEntityV2},{type:object,properties:{...}}] throughout.
      // The Atlassian validator evaluates each allOf branch in isolation and incorrectly rejects
      // properties defined in the other branch as "additional properties not allowed", even though
      // additionalProperties defaults to true. This affects both request and response validation.
      // Downgrade to WARN until the library is fixed or the spec is restructured.
      //
      // Similarly, oneOf validation on requests is downgraded so that intentionally invalid
      // request bodies (used in error-case tests) reach the service rather than being blocked.
      OpenApiInteractionValidator validator =
          OpenApiInteractionValidator.createForInlineApiSpecification(spec)
              .withLevelResolver(
                  LevelResolver.create()
                      .withLevel(
                          "validation.response.contentType.notAllowed", ValidationReport.Level.WARN)
                      .withLevel(
                          "validation.response.body.schema.allOf", ValidationReport.Level.WARN)
                      .withLevel(
                          "validation.response.body.schema.additionalProperties",
                          ValidationReport.Level.WARN)
                      .withLevel(
                          "validation.request.body.schema.allOf", ValidationReport.Level.WARN)
                      .withLevel(
                          "validation.request.body.schema.additionalProperties",
                          ValidationReport.Level.WARN)
                      .withLevel(
                          "validation.request.body.schema.oneOf", ValidationReport.Level.WARN)
                      .build())
              .build();
      return new OpenApiValidationFilter(validator);
    } catch (IOException e) {
      throw new IllegalStateException("Failed to load OpenAPI spec from classpath", e);
    }
  }

  @Override
  public void beforeAll(ExtensionContext context) {
    String baseUri = E2eConfig.baseUri();
    String authToken = E2eConfig.authToken();

    if (baseUri == null || baseUri.isBlank()) {
      throw new IllegalStateException(
          "Missing required e2e configuration: "
              + "set system property 'e2e.baseUri' or env var 'E2E_BASEURI'");
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
