package uk.gov.justice.laa.providerdata.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
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
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.justice.laa.providerdata.PostgresqlTestcontainersConfiguration;
import uk.gov.laa.springboot.oauth2.testsupport.StubJwtToken;

@SpringBootTest(
    properties = {
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
  Oauth2AuthenticationIntegrationTest.JwtTestConfig.class
})
@AutoConfigureMockMvc
class Oauth2AuthenticationIntegrationTest {

  @Autowired private MockMvc mockMvc;

  @Test
  void protectedEndpointWithoutBearerTokenReturnsUnauthorized() throws Exception {
    mockMvc.perform(get("/trace/1")).andExpect(status().isUnauthorized());
  }

  @Test
  void protectedEndpointWithInvalidBearerTokenReturnsUnauthorized() throws Exception {
    mockMvc
        .perform(get("/trace/1").header("Authorization", "Bearer invalid-token"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void protectedEndpointWithKnownBearerTokenReturnsSuccess() throws Exception {
    mockMvc
        .perform(get("/trace/1").header("Authorization", "Bearer expired-token"))
        .andExpect(status().isOk());
  }

  @Test
  void protectedEndpointWithValidBearerTokenReturnsSuccess() throws Exception {
    mockMvc
        .perform(get("/trace/1").header("Authorization", "Bearer valid-token"))
        .andExpect(status().isOk());
  }

  @Test
  void protectedEndpointWithMissingRequiredRoleReturnsForbidden() throws Exception {
    mockMvc
        .perform(get("/trace/1").header("Authorization", "Bearer missing-role-token"))
        .andExpect(status().isForbidden());
  }

  @Test
  void protectedEndpointWithWrongAudienceReturnsUnauthorized() throws Exception {
    mockMvc
        .perform(get("/trace/1").header("Authorization", "Bearer wrong-audience-token"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void unprotectedEndpointIsAccessibleWithoutBearerTokenWhenAuthEnabled() throws Exception {
    mockMvc.perform(get("/actuator/health")).andExpect(status().isOk());
  }

  @TestConfiguration
  static class JwtTestConfig {

    @Bean
    JwtDecoder jwtDecoder() {
      Map<String, StubJwtToken> tokens = new HashMap<>();
      tokens.put(
          "valid-token",
          new StubJwtToken(
              "valid-token",
              "integration-test-client",
              new String[] {"PDA_ACCESS"},
              null,
              Map.of()));
      tokens.put(
          "expired-token",
          new StubJwtToken(
              "expired-token",
              "integration-test-client",
              new String[] {"PDA_ACCESS"},
              null,
              Map.of("exp", Instant.now().minusSeconds(60).getEpochSecond())));
      tokens.put(
          "missing-role-token",
          new StubJwtToken(
              "missing-role-token", "integration-test-client", new String[0], null, Map.of()));

      return token -> {
        if ("wrong-audience-token".equals(token)) {
          throw new OAuth2AuthenticationException(
              new OAuth2Error("invalid_token", "Invalid audience", null));
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
