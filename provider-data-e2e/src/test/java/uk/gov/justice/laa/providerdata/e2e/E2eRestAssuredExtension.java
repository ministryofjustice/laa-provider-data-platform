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
      if (E2eConfig.authToken() == null || E2eConfig.authToken().isBlank()) {
        spec = removeSecurityRequirements(spec);
      }
      // Schema validation suppressions: E2E tests deliberately send invalid payloads (missing
      // required fields, invalid enums, malformed UUIDs, etc.) to verify the API returns 400 Bad
      // Request. Without these suppressions, the validator would block payloads before reaching
      // the service. Tests assert response correctness; the validator is not responsible for
      // checking.
      //
      // validation.response.contentType.notAllowed:
      // The spec declares application/json for some error responses, but the service correctly
      // returns application/problem+json (RFC 7807). Keep as WARN until the spec is aligned.
      //
      // validation.response.body.schema.allOf:
      // The spec composes many models with allOf across base and subtype schemas. Validator v3
      // emits noisy composition failures for otherwise valid payloads.
      //
      // validation.response.body.schema.additionalProperties:
      // allOf branch-level evaluation can flag legitimate fields as additionalProperties even when
      // they are valid in the fully composed schema.
      //
      // validation.response.body.schema.oneOf:
      // oneOf checks become noisy when used with allOf polymorphic composition, so this stays WARN
      // to avoid false negatives in valid responses.
      //
      // validation.response.body.schema.required:
      // Required attributes in response schema are not validated reliably because of how the
      // Atlassian filter parses oneOf and allOf properties. Your test must check for them instead.
      //
      // validation.request.body.schema.allOf:
      // Request composition can fail noisily in polymorphic branches used by negative-path
      // fixtures.
      //
      // validation.request.body.schema.additionalProperties:
      // allOf branch-level evaluation can flag legitimate fields as additional in deliberately
      // invalid request payloads.
      //
      // validation.request.body.schema.oneOf:
      // Modifying tests intentionally submit invalid variants that must reach the API to assert
      // 400 responses.
      //
      // validation.request.body.schema.required:
      // Missing mandatory fields are sent deliberately in tests that verify service-side
      // validation.
      //
      // validation.request.body.schema.enum, validation.request.body.schema.format.uuid, and
      // validation.request.body.schema.minLength:
      // Negative tests exercise enum, identifier-format, and string-length constraints and must
      // reach the API to assert expected 400 problem responses.
      //
      // validation.request.body.schema.type:
      // Negative tests send an explicit JSON null for a required string field (e.g.
      // contractManagerGUID: null) to verify the service rejects it. Without this suppression the
      // request never reaches the API.
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
                          "validation.response.body.schema.oneOf", ValidationReport.Level.WARN)
                      .withLevel(
                          "validation.response.body.schema.required", ValidationReport.Level.WARN)
                      .withLevel(
                          "validation.request.body.schema.allOf", ValidationReport.Level.WARN)
                      .withLevel(
                          "validation.request.body.schema.additionalProperties",
                          ValidationReport.Level.WARN)
                      .withLevel(
                          "validation.request.body.schema.oneOf", ValidationReport.Level.WARN)
                      .withLevel(
                          "validation.request.body.schema.required", ValidationReport.Level.WARN)
                      .withLevel("validation.request.body.schema.enum", ValidationReport.Level.WARN)
                      .withLevel("validation.request.body.schema.type", ValidationReport.Level.WARN)
                      .withLevel(
                          "validation.request.body.schema.format.uuid", ValidationReport.Level.WARN)
                      .withLevel(
                          "validation.request.body.schema.minLength", ValidationReport.Level.WARN)
                      .withLevel("validation.request.body.schema.type", ValidationReport.Level.WARN)
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
    String authHeader = E2eConfig.authHeader();
    boolean authEnabled = authToken != null && !authToken.isBlank();

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

    if (authEnabled) {
      builder.addHeader(authHeader, authToken);
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

  private static String removeSecurityRequirements(String spec) {
    return spec.replaceAll(
        "(?ms)^\\s*security:\\n(?:^\\s*-\\s*(?:ApiKeyAuth|AzureAD|bearerAuth):\\s*\\[\\]\\n?)+",
        "");
  }
}
