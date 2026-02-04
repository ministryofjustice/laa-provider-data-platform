package uk.gov.justice.laa.providerdata.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.justice.laa.providerdata.ProviderDataServiceApplication;

/** Integration test for {@link ItemController}. */
@ActiveProfiles("test")
@SpringBootTest(classes = ProviderDataServiceApplication.class)
@AutoConfigureMockMvc
@Transactional
public class ItemControllerIntegrationTest {

  @Autowired private MockMvc mockMvc;

  @Test
  void shouldGetAllItems() throws Exception {
    mockMvc.perform(get("/api/v1/items")).andExpect(status().isOk());
  }
}
