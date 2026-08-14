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

  private static OpenApiValidationFilter createValidationFilter(boolean authEnabled) {
    try (InputStream is =
        E2eRestAssuredExtension.class.getClassLoader().getResourceAsStream("laa-data-pda.yml")) {
      if (is == null) {
        throw new IllegalStateException("Cannot find laa-data-pda.yml on classpath");
      }
      String spec = new String(is.readAllBytes(), StandardCharsets.UTF_8);
      if (!authEnabled) {
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
    String authToken = resolveAuthToken();
    String authHeader = resolveAuthHeader(authToken);
    String authHeaderValue = resolveAuthHeaderValue(authHeader, authToken);
    boolean authEnabled = authToken != null && !authToken.isBlank();

    log("═════════════════════════════════════════════════");
    log("E2E Test Configuration");
    log("═════════════════════════════════════════════════");
    log("Base URI: " + baseUri);
    log("Auth Enabled: " + (authEnabled ? "✓ YES" : "✗ NO"));

    if (authEnabled) {
      log("Auth Header: " + authHeader);
      boolean isBearer = authHeaderValue.startsWith("Bearer ");
      log("Token Type: " + (isBearer ? "Bearer JWT" : "Custom"));
      log("Token Length: " + authHeaderValue.length() + " characters");

      // Log first few characters of token for debugging (safe obfuscation)
      if (authHeaderValue.length() > 20) {
        String prefix = authHeaderValue.substring(0, 20);
        log("Token Prefix: " + prefix + "...");
      }
    }
    log("═════════════════════════════════════════════════");

    if (baseUri == null || baseUri.isBlank()) {
      throw new IllegalStateException(
          "Missing required e2e configuration: "
              + "set system property 'e2e.baseUri' or env var 'E2E_BASEURI'");
    }

    RequestSpecBuilder builder =
        new RequestSpecBuilder()
            .setBaseUri(baseUri)
            .setContentType(ContentType.JSON)
            .addFilter(createValidationFilter(authEnabled));

    if (authEnabled) {
      builder.addHeader(authHeader, authHeaderValue);
      log("✓ Added " + authHeader + " header to all requests");
    }

    RestAssured.requestSpecification = builder.build();

    RestAssured.config =
        RestAssured.config()
            .httpClient(
                HttpClientConfig.httpClientConfig()
                    .setParam("http.connection.timeout", CONNECTION_TIMEOUT_MS)
                    .setParam("http.socket.timeout", SOCKET_TIMEOUT_MS));

    RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
    log("✓ RestAssured configured with validation filter and logging");
  }

  private static String removeSecurityRequirements(String spec) {
    return spec.replaceAll(
        "(?ms)^\\s*security:\\n(?:^\\s*-\\s*(?:ApiKeyAuth|AzureAD|bearerAuth):\\s*\\[\\]\\n?)+",
        "");
  }

  private static String resolveAuthToken() {
    String configuredToken = E2eConfig.authToken();
    if (configuredToken != null && !configuredToken.isBlank()) {
      log("→ Using explicit auth token from e2e.authToken property");
      return configuredToken;
    }

    log("→ No explicit e2e.authToken found, checking OAuth2 configuration...");
    if (!E2eConfig.hasOauth2ClientCredentialsConfig()) {
      log("✗ OAuth2 not configured (missing E2E_OAUTH2_* environment variables)");
      return null;
    }

    log("✓ OAuth2 configuration found, acquiring token...");
    String token = Oauth2ClientCredentialsTokenProvider.getTokenIfConfigured();
    if (token != null && !token.isBlank()) {
      log("✓ Successfully acquired OAuth2 token");
      return token;
    }

    log("✗ Failed to acquire OAuth2 token");
    return null;
  }

  private static String resolveAuthHeader(String authToken) {
    if (authToken == null || authToken.isBlank()) {
      String header = E2eConfig.authHeader();
      log("→ No auth token, using default auth header: " + header);
      return header;
    }
    if (E2eConfig.hasExplicitAuthHeader()) {
      String header = E2eConfig.authHeader();
      log("→ Using explicit auth header: " + header);
      return header;
    }
    if (E2eConfig.hasOauth2ClientCredentialsConfig()) {
      log("→ OAuth2 configured, using standard Authorization header");
      return "Authorization";
    }
    String header = E2eConfig.authHeader();
    log("→ Using default auth header: " + header);
    return header;
  }

  private static String resolveAuthHeaderValue(String authHeader, String authToken) {
    if (authToken == null || authToken.isBlank()) {
      return authToken;
    }
    if ("Authorization".equalsIgnoreCase(authHeader)
        && !authToken.regionMatches(true, 0, "Bearer ", 0, "Bearer ".length())) {
      log("✓ Prepending 'Bearer ' to OAuth2 token");
      return "Bearer " + authToken;
    }
    return authToken;
  }

  private static void log(String message) {
    System.out.println("[E2E Config] " + message);
  }
}
