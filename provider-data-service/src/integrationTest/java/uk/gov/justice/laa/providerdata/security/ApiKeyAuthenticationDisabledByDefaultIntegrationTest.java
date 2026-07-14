package uk.gov.justice.laa.providerdata.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.justice.laa.providerdata.PostgresqlSpringBootTest;

@AutoConfigureMockMvc
class ApiKeyAuthenticationDisabledByDefaultIntegrationTest extends PostgresqlSpringBootTest {

  @Autowired private MockMvc mockMvc;

  @Test
  void requestWithoutApiKeyIsAllowedWhenApiKeyAuthDisabled() throws Exception {
    mockMvc.perform(get("/trace/1")).andExpect(status().isOk());
  }
}
