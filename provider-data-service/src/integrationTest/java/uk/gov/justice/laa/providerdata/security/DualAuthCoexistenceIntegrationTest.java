package uk.gov.justice.laa.providerdata.security;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.justice.laa.providerdata.PostgresqlTestcontainersConfiguration;
import uk.gov.laa.springboot.oauth2.testsupport.StubJwtToken;

@SpringBootTest(
    properties = {
      "app.security.apikey.enabled=true",
      "app.security.oauth2.enabled=true",
      "laa.springboot.starter.auth.authentication-header=X-Authorization",
      "laa.springboot.starter.auth.authorized-clients="
          + "[{\"name\":\"Team1\",\"roles\":[\"STANDARD\"],\"token\":\"Dummy1\"}]",
      "laa.springboot.starter.auth.authorized-roles="
          + "[{\"name\":\"STANDARD\",\"uris\":[\"/**\"]}]",
      "laa.springboot.starter.auth.unprotected-uris=/error,/actuator/**",
      "laa.springboot.starter.oauth2.resourceserver.jwt.tenants[0].issuer-uri="
          + "https://login.microsoftonline.com/test-tenant/v2.0",
      "laa.springboot.starter.oauth2.resourceserver.jwt.tenants[0].audiences[0]=api://pda-r2",
      "laa.springboot.starter.oauth2.authorized-roles="
          + "[{\"name\":\"PDA_ACCESS\",\"uris\":[\"/**\"]}]",
      "laa.springboot.starter.oauth2.unprotected-uris=/error,/actuator/**"
    })
@ActiveProfiles("test")
@Import({
  PostgresqlTestcontainersConfiguration.class,
  DualAuthCoexistenceIntegrationTest.JwtTestConfig.class
})
@AutoConfigureMockMvc
class DualAuthCoexistenceIntegrationTest {

  @Autowired private MockMvc mockMvc;
  @MockitoBean private AuthenticationAuditLogger authenticationAuditLogger;

  @BeforeEach
  void clearAuditLoggerInteractions() {
    clearInvocations(authenticationAuditLogger);
  }

  /// AC6 – API key authentication works when OAuth2 is also enabled.
  @Test
  void protectedEndpointWithValidApiKeyWorksWhenOauth2Enabled() throws Exception {
    mockMvc.perform(get("/trace/1").header("X-Authorization", "Dummy1")).andExpect(status().isOk());
    verify(authenticationAuditLogger).logAuthenticationSuccess(eq("api-key"), anyString());
  }

  /// AC6 – OAuth2 bearer token works when API key is also enabled.
  @Test
  void protectedEndpointWithValidBearerTokenWorksWhenApiKeyEnabled() throws Exception {
    mockMvc
        .perform(get("/trace/1").header("Authorization", "Bearer valid-oauth2-token"))
        .andExpect(status().isOk());
    verify(authenticationAuditLogger).logAuthenticationSuccess(eq("oauth2"), anyString());
  }

  /// AC6 – Bearer token takes precedence when both API key and bearer token are supplied.
  @Test
  void bearerTokenTakesPrecedenceWhenBothCredentialsSupplied() throws Exception {
    mockMvc
        .perform(
            get("/trace/1")
                .header("Authorization", "Bearer valid-oauth2-token")
                .header("X-Authorization", "Dummy1"))
        .andExpect(status().isOk());
    verify(authenticationAuditLogger).logCredentialPrecedence(true, true);
    verify(authenticationAuditLogger).logAuthenticationSuccess(eq("oauth2"), anyString());
  }

  /// AC3 – Invalid API key is rejected when OAuth2 is enabled.
  @Test
  void invalidApiKeyIsRejectedWhenOauth2Enabled() throws Exception {
    mockMvc
        .perform(get("/trace/1").header("X-Authorization", "invalid-api-key"))
        .andExpect(status().isUnauthorized());
    verify(authenticationAuditLogger)
        .logAuthenticationFailure("api-key", "invalid_or_unauthorised");
  }

  /// AC3 – Invalid bearer token is rejected when API key is enabled.
  @Test
  void invalidBearerTokenIsRejectedWhenApiKeyEnabled() throws Exception {
    mockMvc
        .perform(get("/trace/1").header("Authorization", "Bearer invalid-oauth2-token"))
        .andExpect(status().isUnauthorized());
    verify(authenticationAuditLogger).logAuthenticationFailure("oauth2", "invalid_or_unauthorised");
  }

  /// AC4 – No credentials returns 401 even when both mechanisms are enabled.
  @Test
  void noCredentialsReturnsUnauthorizedWhenBothEnabled() throws Exception {
    mockMvc.perform(get("/trace/1")).andExpect(status().isUnauthorized());
    verify(authenticationAuditLogger).logAuthenticationFailure("none", "missing_credentials");
  }

  /// AC1 – API key consumer unaffected by OAuth2 being enabled.
  @Test
  void existingApiKeyConsumerContinuesToWork() throws Exception {
    // Existing consumer supplies only API key; should not be affected by OAuth2 enablement
    mockMvc.perform(get("/trace/1").header("X-Authorization", "Dummy1")).andExpect(status().isOk());
  }

  /// AC2 – OAuth2 consumer can authenticate without API key.
  @Test
  void newOauth2ConsumerCanAuthenticateWithoutApiKey() throws Exception {
    // New consumer supplies only bearer token; should work without providing API key
    mockMvc
        .perform(get("/trace/1").header("Authorization", "Bearer valid-oauth2-token"))
        .andExpect(status().isOk());
  }

  @TestConfiguration
  static class JwtTestConfig {

    @Bean
    JwtDecoder jwtDecoder() {
      Map<String, StubJwtToken> tokens = new HashMap<>();
      tokens.put(
          "valid-oauth2-token",
          new StubJwtToken(
              "valid-oauth2-token", "test-client-id", new String[] {"PDA_ACCESS"}, null, Map.of()));

      return token -> {
        if ("invalid-oauth2-token".equals(token)) {
          throw new OAuth2AuthenticationException(
              new OAuth2Error("invalid_token", "Invalid token", null));
        }
        StubJwtToken stubJwtToken = tokens.get(token);
        if (stubJwtToken == null) {
          throw new OAuth2AuthenticationException(
              new OAuth2Error("invalid_token", "No matching stub JWT token.", null));
        }
        return stubJwtToken.toJwt();
      };
    }
  }
}
