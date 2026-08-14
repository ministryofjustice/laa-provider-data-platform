package uk.gov.justice.laa.providerdata.e2e;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Obtains an OAuth2 access token for E2E from Entra using client credentials. */
final class Oauth2ClientCredentialsTokenProvider {

  private static final Pattern ACCESS_TOKEN_PATTERN =
      Pattern.compile("\"access_token\"\\s*:\\s*\"([^\"]+)\"");
  private static final Pattern EXPIRES_IN_PATTERN =
      Pattern.compile("\"expires_in\"\\s*:\\s*(\\d+)");

  private static volatile String cachedToken;
  private static volatile long tokenExpiresAt;

  private static final String LOG_PREFIX = "[E2E OAuth2]";

  private Oauth2ClientCredentialsTokenProvider() {}

  static {
    if (E2eConfig.hasOauth2ClientCredentialsConfig()) {
      log(
          "✓ OAuth2 client credentials configured\n"
              + "  Token URL: "
              + E2eConfig.oauth2TokenUrl()
              + "\n"
              + "  Client ID: "
              + E2eConfig.oauth2ClientId()
              + "\n"
              + "  Scope: "
              + E2eConfig.oauth2Scope());
    } else {
      log("✗ OAuth2 client credentials NOT configured");
    }
  }

  static String getTokenIfConfigured() {
    if (!E2eConfig.hasOauth2ClientCredentialsConfig()) {
      log("⊘ Skipping OAuth2 - not configured");
      return null;
    }

    // Check if cached token is still valid
    if (cachedToken != null
        && !cachedToken.isBlank()
        && System.currentTimeMillis() < tokenExpiresAt) {
      long secondsRemaining = (tokenExpiresAt - System.currentTimeMillis()) / 1000;
      log("♻ Using cached OAuth2 token (expires in " + secondsRemaining + "s)");
      return cachedToken;
    }

    synchronized (Oauth2ClientCredentialsTokenProvider.class) {
      // Double-check after acquiring lock
      if (cachedToken != null
          && !cachedToken.isBlank()
          && System.currentTimeMillis() < tokenExpiresAt) {
        long secondsRemaining = (tokenExpiresAt - System.currentTimeMillis()) / 1000;
        log("♻ Using cached OAuth2 token (synchronized, expires in " + secondsRemaining + "s)");
        return cachedToken;
      }

      log("→ Fetching new OAuth2 token from Entra...");
      long startMs = System.currentTimeMillis();
      try {
        cachedToken = requestToken();
        long elapsedMs = System.currentTimeMillis() - startMs;
        long secondsRemaining = (tokenExpiresAt - System.currentTimeMillis()) / 1000;
        log("✓ OAuth2 token fetched in " + elapsedMs + "ms (expires in " + secondsRemaining + "s)");
        return cachedToken;
      } catch (Exception e) {
        long elapsedMs = System.currentTimeMillis() - startMs;
        log("✗ OAuth2 token fetch failed after " + elapsedMs + "ms: " + e.getMessage());
        throw e;
      }
    }
  }

  private static String requestToken() {
    String clientId = E2eConfig.oauth2ClientId();
    String scope = E2eConfig.oauth2Scope();
    String tokenUrl = E2eConfig.oauth2TokenUrl();

    String requestBody =
        "grant_type=client_credentials"
            + "&client_id="
            + urlEncode(clientId)
            + "&client_secret="
            + urlEncode(E2eConfig.oauth2ClientSecret())
            + "&scope="
            + urlEncode(scope);

    log("→ Preparing OAuth2 token request:");
    log("  Endpoint: " + tokenUrl);
    log("  Client ID: " + clientId);
    log("  Scope: " + scope);
    log("  Grant Type: client_credentials");

    HttpRequest request =
        HttpRequest.newBuilder(URI.create(tokenUrl))
            .header("Content-Type", "application/x-www-form-urlencoded")
            .POST(HttpRequest.BodyPublishers.ofString(requestBody))
            .build();

    try {
      log("→ Sending HTTP POST request...");
      HttpResponse<String> response =
          HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());

      log("← Received HTTP " + response.statusCode() + " response");

      if (response.statusCode() != 200) {
        String body = response.body();
        if (body != null && body.length() > 500) {
          body = body.substring(0, 500) + "...";
        }
        log("✗ Expected HTTP 200 but got " + response.statusCode());
        log("  Response body: " + body);
        throw new IllegalStateException(
            "Failed to obtain OAuth2 access token (HTTP " + response.statusCode() + ")");
      }

      String accessToken = extractAccessToken(response.body());
      if (accessToken == null || accessToken.isBlank()) {
        log("✗ Response does not contain 'access_token' field");
        throw new IllegalStateException("OAuth2 response missing access_token");
      }

      // Extract expiration time if available
      Long expiresIn = extractExpiresIn(response.body());
      if (expiresIn != null) {
        tokenExpiresAt = System.currentTimeMillis() + (expiresIn * 1000);
        log("✓ Token expires in " + expiresIn + " seconds");
      }

      // Log token claims (header.payload only, no signature)
      String[] parts = accessToken.split("\\.");
      if (parts.length >= 2) {
        try {
          String payload = decodeBase64Url(parts[1]);
          log("✓ Token payload claims: " + payload);
        } catch (Exception e) {
          log("⊘ Could not decode token payload: " + e.getMessage());
        }
      }

      return accessToken;
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      log("✗ Token request interrupted: " + e.getMessage());
      throw new IllegalStateException("OAuth2 token request interrupted", e);
    } catch (IOException e) {
      log("✗ Token request IO error: " + e.getMessage());
      throw new IllegalStateException("OAuth2 token request failed", e);
    }
  }

  private static String extractAccessToken(String responseBody) {
    Matcher matcher = ACCESS_TOKEN_PATTERN.matcher(responseBody);
    if (matcher.find()) {
      return matcher.group(1);
    }
    return null;
  }

  private static Long extractExpiresIn(String responseBody) {
    Matcher matcher = EXPIRES_IN_PATTERN.matcher(responseBody);
    if (matcher.find()) {
      try {
        return Long.parseLong(matcher.group(1));
      } catch (NumberFormatException ignored) {
        return null;
      }
    }
    return null;
  }

  private static String decodeBase64Url(String encoded) {
    // Add padding if needed
    String padded = encoded;
    switch (encoded.length() % 4) {
      case 2:
        padded = encoded + "==";
        break;
      case 3:
        padded = encoded + "=";
        break;
    }
    byte[] decoded = Base64.getUrlDecoder().decode(padded);
    return new String(decoded, StandardCharsets.UTF_8);
  }

  private static String urlEncode(String value) {
    return URLEncoder.encode(value, StandardCharsets.UTF_8);
  }

  private static void log(String message) {
    System.out.println(LOG_PREFIX + " " + message);
  }
}
