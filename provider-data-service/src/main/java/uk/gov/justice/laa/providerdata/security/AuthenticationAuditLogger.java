package uk.gov.justice.laa.providerdata.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Logs authentication mechanism (API key vs OAuth2) and failure categories. Follows security best
 * practices: no token/key values are logged.
 */
@Component
public class AuthenticationAuditLogger {

  private static final Logger log = LoggerFactory.getLogger(AuthenticationAuditLogger.class);

  /**
   * Log successful authentication with mechanism used.
   *
   * @param mechanism The authentication mechanism used: "api-key" or "oauth2"
   * @param principal The authenticated principal (user/application identifier)
   */
  public void logAuthenticationSuccess(String mechanism, String principal) {
    log.info("Authentication successful [mechanism={}] [principal={}]", mechanism, principal);
  }

  /**
   * Log authentication failure with category.
   *
   * @param mechanism The authentication mechanism attempted: "api-key" or "oauth2"
   * @param failureCategory The failure reason (e.g., "missing", "invalid", "unauthorized",
   *     "expired")
   */
  public void logAuthenticationFailure(String mechanism, String failureCategory) {
    log.warn(
        "Authentication failed [mechanism={}] [failure_category={}]", mechanism, failureCategory);
  }

  /**
   * Log when both credentials are supplied; bearer token takes precedence.
   *
   * @param bearerPresent Whether bearer token was supplied
   * @param apiKeyPresent Whether API key was supplied
   */
  public void logCredentialPrecedence(boolean bearerPresent, boolean apiKeyPresent) {
    if (bearerPresent && apiKeyPresent) {
      log.debug("Both bearer and API key supplied; bearer token takes precedence");
    }
  }
}
