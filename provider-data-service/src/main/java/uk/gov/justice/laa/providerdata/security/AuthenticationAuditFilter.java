package uk.gov.justice.laa.providerdata.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.web.filter.OncePerRequestFilter;

/** Logs authentication outcomes without exposing token or API-key values. */
public class AuthenticationAuditFilter extends OncePerRequestFilter {

  private final AuthenticationAuditLogger authenticationAuditLogger;

  public AuthenticationAuditFilter(AuthenticationAuditLogger authenticationAuditLogger) {
    this.authenticationAuditLogger = authenticationAuditLogger;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    boolean bearerPresent = hasBearerToken(request);
    boolean apiKeyPresent = hasApiKey(request);
    String mechanism = resolveMechanism(bearerPresent, apiKeyPresent);

    authenticationAuditLogger.logCredentialPrecedence(bearerPresent, apiKeyPresent);

    filterChain.doFilter(request, response);

    if (!"none".equals(mechanism) && response.getStatus() < HttpServletResponse.SC_BAD_REQUEST) {
      authenticationAuditLogger.logAuthenticationSuccess(mechanism, "redacted");
      return;
    }

    if (response.getStatus() == HttpServletResponse.SC_UNAUTHORIZED
        || response.getStatus() == HttpServletResponse.SC_FORBIDDEN) {
      authenticationAuditLogger.logAuthenticationFailure(
          mechanism, resolveFailureCategory(mechanism));
    }
  }

  private static boolean hasBearerToken(HttpServletRequest request) {
    String authorizationHeader = request.getHeader("Authorization");
    return authorizationHeader != null
        && authorizationHeader.regionMatches(true, 0, "Bearer ", 0, "Bearer ".length());
  }

  private static boolean hasApiKey(HttpServletRequest request) {
    String apiKeyHeader = request.getHeader("X-Authorization");
    return apiKeyHeader != null && !apiKeyHeader.isBlank();
  }

  private static String resolveMechanism(boolean bearerPresent, boolean apiKeyPresent) {
    if (bearerPresent) {
      return "oauth2";
    }
    if (apiKeyPresent) {
      return "api-key";
    }
    return "none";
  }

  private static String resolveFailureCategory(String mechanism) {
    if ("none".equals(mechanism)) {
      return "missing_credentials";
    }
    return "invalid_or_unauthorised";
  }
}
