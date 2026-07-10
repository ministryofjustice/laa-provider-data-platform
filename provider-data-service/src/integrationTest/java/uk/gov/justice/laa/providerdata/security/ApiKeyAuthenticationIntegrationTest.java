package uk.gov.justice.laa.providerdata.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.justice.laa.providerdata.PostgresqlTestcontainersConfiguration;

@SpringBootTest(
    properties = {
      "app.security.apikey.enabled=true",
      "AUTHENTICATION_HEADER=X-Authorization",
      "AUTHORIZED_CLIENTS="
          + "[{\"name\":\"Team1\",\"roles\":[\"STANDARD\"],\"token\":\"Dummy1\"},"
          + "{\"name\":\"Team2\",\"roles\":[\"LIMITED\"],\"token\":\"Dummy2\"}]",
      "AUTHORIZED_ROLES="
          + "[{\"name\":\"STANDARD\",\"uris\":[\"/**\"]},"
          + "{\"name\":\"LIMITED\",\"uris\":[\"/limited/**\"]}]",
      "UNPROTECTED_URIS=/error,/actuator/**"
    })
@ActiveProfiles("test")
@Import(PostgresqlTestcontainersConfiguration.class)
@AutoConfigureMockMvc
@ExtendWith(OutputCaptureExtension.class)
class ApiKeyAuthenticationIntegrationTest {

  @Autowired private MockMvc mockMvc;

  /// AC3 – missing key returns 401 and the failure is logged.
  @Test
  void protectedEndpointWithoutApiKeyReturnsUnauthorized(CapturedOutput output) throws Exception {
    mockMvc
        .perform(get("/trace/1"))
        .andExpect(status().isUnauthorized())
        .andExpect(content().string(containsString("No API access token provided")));
    assertThat(output).contains("no API access token provided");
  }

  /// AC2 – invalid key returns 401 and the failure is logged.
  @Test
  void protectedEndpointWithInvalidApiKeyReturnsUnauthorized(CapturedOutput output)
      throws Exception {
    mockMvc
        .perform(get("/trace/1").header("X-Authorization", "invalid-token"))
        .andExpect(status().isUnauthorized())
        .andExpect(content().string(containsString("Invalid API access token provided")));
    assertThat(output).contains("invalid API access token provided");
  }

  /// AC1 – valid key is accepted and the request is processed.
  @Test
  void protectedEndpointWithValidApiKeyReturnsSuccess() throws Exception {
    mockMvc.perform(get("/trace/1").header("X-Authorization", "Dummy1")).andExpect(status().isOk());
  }

  /// AC4 – URIs listed in UNPROTECTED_URIS bypass the filter without a key.
  @Test
  void unprotectedEndpointIsAccessibleWithoutApiKeyWhenAuthEnabled() throws Exception {
    mockMvc.perform(get("/actuator/health")).andExpect(status().isOk());
  }

  /// AC4 – valid key with a role that does not cover the requested URI returns 403.
  @Test
  void protectedEndpointWithValidKeyButUnauthorisedRoleReturnsForbidden(CapturedOutput output)
      throws Exception {
    mockMvc
        .perform(get("/trace/1").header("X-Authorization", "Dummy2"))
        .andExpect(status().isForbidden())
        .andExpect(content().string(containsString("Access Denied")));
    assertThat(output).contains("Authorisation failure");
  }
}
