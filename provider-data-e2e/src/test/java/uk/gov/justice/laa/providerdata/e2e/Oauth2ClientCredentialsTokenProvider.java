package uk.gov.justice.laa.providerdata.e2e;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Obtains an OAuth2 access token for E2E from Entra using client credentials. */
final class Oauth2ClientCredentialsTokenProvider {

  private static final Pattern ACCESS_TOKEN_PATTERN =
      Pattern.compile("\"access_token\"\\s*:\\s*\"([^\"]+)\"");

  private static volatile String cachedToken;

  private Oauth2ClientCredentialsTokenProvider() {}

  static String getTokenIfConfigured() {
    if (!E2eConfig.hasOauth2ClientCredentialsConfig()) {
      return null;
    }
    if (cachedToken != null && !cachedToken.isBlank()) {
      return cachedToken;
    }
    synchronized (Oauth2ClientCredentialsTokenProvider.class) {
      if (cachedToken != null && !cachedToken.isBlank()) {
        return cachedToken;
      }
      cachedToken = requestToken();
      return cachedToken;
    }
  }

  private static String requestToken() {
    String requestBody =
        "grant_type=client_credentials"
            + "&client_id="
            + urlEncode(E2eConfig.oauth2ClientId())
            + "&client_secret="
            + urlEncode(E2eConfig.oauth2ClientSecret())
            + "&scope="
            + urlEncode(E2eConfig.oauth2Scope());

    HttpRequest request =
        HttpRequest.newBuilder(URI.create(E2eConfig.oauth2TokenUrl()))
            .header("Content-Type", "application/x-www-form-urlencoded")
            .POST(HttpRequest.BodyPublishers.ofString(requestBody))
            .build();

    try {
      HttpResponse<String> response =
          HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
      if (response.statusCode() != 200) {
        throw new IllegalStateException(
            "Failed to obtain OAuth2 access token for E2E (status " + response.statusCode() + ")");
      }
      String accessToken = extractAccessToken(response.body());
      if (accessToken == null || accessToken.isBlank()) {
        throw new IllegalStateException("OAuth2 token response does not contain access_token");
      }
      return accessToken;
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("OAuth2 token request interrupted", e);
    } catch (IOException e) {
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

  private static String urlEncode(String value) {
    return URLEncoder.encode(value, StandardCharsets.UTF_8);
  }
}
