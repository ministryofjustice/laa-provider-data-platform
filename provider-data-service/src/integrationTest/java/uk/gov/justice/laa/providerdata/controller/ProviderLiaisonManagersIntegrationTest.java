package uk.gov.justice.laa.providerdata.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import uk.gov.justice.laa.providerdata.PostgresqlSpringBootTest;
import uk.gov.justice.laa.providerdata.entity.LiaisonManagerEntity;
import uk.gov.justice.laa.providerdata.repository.LiaisonManagerRepository;

@Transactional
class ProviderLiaisonManagersIntegrationTest extends PostgresqlSpringBootTest {

  @Autowired private WebApplicationContext webApplicationContext;
  @Autowired private LiaisonManagerRepository liaisonManagerRepository;

  private MockMvc mockMvc;
  private LiaisonManagerEntity liaisonManagerEntity;

  @BeforeEach
  void setUp() {
    mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();

    liaisonManagerEntity =
        LiaisonManagerEntity.builder()
            .firstName("Jane")
            .lastName("Doe")
            .emailAddress("jane.doe@example.com")
            .telephoneNumber("01234567890")
            .build();
    liaisonManagerEntity = liaisonManagerRepository.save(liaisonManagerEntity);
  }

  @Test
  void getLiaisonManager_returnsOk() throws Exception {
    mockMvc
        .perform(get("/provider-liaison-managers/{guid}", liaisonManagerEntity.getGuid()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.firstName").value("Jane"))
        .andExpect(jsonPath("$.data.lastName").value("Doe"))
        .andExpect(jsonPath("$.data.emailAddress").value("jane.doe@example.com"))
        .andExpect(jsonPath("$.data.telephoneNumber").value("01234567890"));
  }

  @Test
  void updateLiaisonManager_updatesPermittedFields() throws Exception {
    String payload =
        """
        {
          "emailAddress": "jane.smith@example.com",
          "telephoneNumber": "09876543210"
        }
        """;

    mockMvc
        .perform(
            patch("/provider-liaison-managers/{guid}", liaisonManagerEntity.getGuid())
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.firstName").value("Jane"))
        .andExpect(jsonPath("$.data.emailAddress").value("jane.smith@example.com"))
        .andExpect(jsonPath("$.data.telephoneNumber").value("09876543210"));

    var updated = liaisonManagerRepository.findById(liaisonManagerEntity.getGuid()).orElseThrow();
    assertThat(updated.getEmailAddress()).isEqualTo("jane.smith@example.com");
    assertThat(updated.getTelephoneNumber()).isEqualTo("09876543210");
  }

  @Test
  void updateLiaisonManager_rejectsRedactedFields() throws Exception {
    String payload =
        """
        {
          "firstName": "Janet"
        }
        """;

    mockMvc
        .perform(
            patch("/provider-liaison-managers/{guid}", liaisonManagerEntity.getGuid())
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
        .andExpect(status().isBadRequest());
  }
}
