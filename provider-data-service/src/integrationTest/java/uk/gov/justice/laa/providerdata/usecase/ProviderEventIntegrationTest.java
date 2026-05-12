package uk.gov.justice.laa.providerdata.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
import uk.gov.justice.laa.providerdata.usecase.repository.ProviderEventRepository;

@Transactional
class ProviderEventIntegrationTest extends PostgresqlSpringBootTest {

  @Autowired private WebApplicationContext context;
  @Autowired private ProviderEventRepository providerEventRepository;

  private MockMvc mockMvc;

  private static final String CREATE_FIRM_BODY =
      """
      {
        "firmType": "Legal Services Provider",
        "name": "Events Integration Test LSP",
        "legalServicesProvider": {
          "address": {
            "line1": "1 Event Street",
            "townOrCity": "London",
            "postcode": "SW1A 1AA"
          }
        }
      }
      """;

  @BeforeEach
  void setUp() {
    mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
  }

  @Test
  void createProviderFirm_writesOneProviderEvent() throws Exception {
    mockMvc
        .perform(
            post("/provider-firms")
                .contentType(MediaType.APPLICATION_JSON)
                .content(CREATE_FIRM_BODY))
        .andExpect(status().isCreated());

    var events = providerEventRepository.findAll();
    assertThat(events).hasSize(1);
    assertThat(events.getFirst().getEventType()).isEqualTo("ProviderFirmChangedSnapshotEvent");
  }

  @Test
  void getProviderEvents_returnsPublishedEvent() throws Exception {
    mockMvc
        .perform(
            post("/provider-firms")
                .contentType(MediaType.APPLICATION_JSON)
                .content(CREATE_FIRM_BODY))
        .andExpect(status().isCreated());

    mockMvc
        .perform(get("/provider-events"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.content.length()").value(1))
        .andExpect(
            jsonPath("$.data.content[0].eventType").value("ProviderFirmChangedSnapshotEvent"));
  }

  @Test
  void getEventByGUID_returnsFullEvent() throws Exception {
    mockMvc
        .perform(
            post("/provider-firms")
                .contentType(MediaType.APPLICATION_JSON)
                .content(CREATE_FIRM_BODY))
        .andExpect(status().isCreated());

    ProviderEventEntity entity = providerEventRepository.findAll().getFirst();
    String guid = entity.getGuid().toString();

    mockMvc
        .perform(get("/provider-events/{guid}", guid))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.eventHeader.guid").value(guid))
        .andExpect(jsonPath("$.data.eventPayload").isNotEmpty());
  }
}
