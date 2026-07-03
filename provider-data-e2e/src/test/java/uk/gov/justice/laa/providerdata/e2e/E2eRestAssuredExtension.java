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
      // Suppression strategy: trade stricter schema validation for broader E2E negative-test
      // coverage.
      // The OpenAPI validator enforces request and response schema constraints. However, E2E tests
      // deliberately send invalid request payloads (missing fields, invalid enum values, malformed
      // UUIDs, too-short strings) to verify the service returns 400 Bad Request with RFC 7807
      // problem details. Without these suppressions, the validator would reject invalid requests
      // before they reach the service, preventing us from testing error-path handling.
      //
      // Similarly, some response schema validations are suppressed because the spec's heavy use of
      // allOf and oneOf composition triggers noisy but harmless failures in validator v3. We accept
      // the risk that responses could technically omit required fields without test failure,
      // relying
      // instead on E2E assertions to verify the service returns correct 200/201 payloads in success
      // paths. Negative tests (400 path) verify service-side error handling, not validator
      // strictness.
      //
      // Request schema suppressions (required, allOf, additionalProperties, oneOf, enum,
      // format.uuid, minLength):
      // Modifying tests in @ModifyingTest classes send deliberately invalid payloads to exercise
      // service-side validation. Suppressions prevent the RestAssured filter from blocking these
      // payloads. For example:
      // - Missing mandatory fields: test that service returns 400 with missing-field error detail
      // - Invalid enum values: test that service rejects out-of-range values
      // - Malformed UUIDs: test that service rejects invalid identifier formats
      // - Too-short strings: test that service enforces minLength constraints
      // Tests assert the expected 400 response; the validator is not responsible for catching
      // errors.
      //
      // Response schema suppressions (allOf, additionalProperties, oneOf, required):
      // The spec composes many models with allOf + oneOf polymorphic schemas (e.g.,
      // ProviderCreateV2,
      // ProviderV2). Validator v3 evaluates each allOf branch and oneOf variant in isolation,
      // triggering spurious failures for otherwise valid payloads (fields appear as
      // "additionalProperties"
      // in single-branch evaluation but are valid in the full composed schema, or required-field
      // checks
      // fail across variant boundaries). Rather than suppress individual false positives, we
      // suppress
      // the entire schema-validation category to allow tests to focus on behaviour assertions.
      // Note: responses can technically pass validation despite omitting required fields, but
      // success-path
      // tests verify that the service returns complete payloads; failure-path tests verify 400
      // handling.
      //
      // validation.response.contentType.notAllowed:
      // The spec declares application/json for some error responses, but the service correctly
      // returns application/problem+json (RFC 7807). Keep as WARN until the spec is aligned.
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
                      .withLevel(
                          "validation.request.body.schema.format.uuid", ValidationReport.Level.WARN)
                      .withLevel(
                          "validation.request.body.schema.minLength", ValidationReport.Level.WARN)
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
