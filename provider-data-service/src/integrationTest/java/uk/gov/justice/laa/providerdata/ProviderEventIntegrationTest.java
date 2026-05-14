package uk.gov.justice.laa.providerdata;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.modulith.test.EnableScenarios;
import org.springframework.modulith.test.Scenario;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import uk.gov.justice.laa.providerdata.event.ProviderFirmChangedSnapshotEvent;
import uk.gov.justice.laa.providerdata.repository.ProviderEventRepository;

@EnableScenarios
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

  @AfterEach
  void cleanUp() {
    providerEventRepository.deleteAll();
  }

  @Test
  void createProviderFirm_writesOneProviderEvent(Scenario scenario) {
    scenario
        .stimulate(
            () -> {
              try {
                mockMvc
                    .perform(
                        post("/provider-firms")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(CREATE_FIRM_BODY))
                    .andExpect(status().isCreated());
              } catch (Exception e) {
                throw new RuntimeException(e);
              }
            })
        .andWaitForEventOfType(ProviderFirmChangedSnapshotEvent.class)
        .toArriveAndVerify(
            event ->
                assertThat(providerEventRepository.findAll())
                    .anyMatch(e -> e.getEventType().equals("ProviderFirmChangedSnapshotEvent")));
  }

  @Test
  void getProviderEvents_returnsPublishedEvent(Scenario scenario) throws Exception {
    scenario
        .stimulate(
            () -> {
              try {
                mockMvc
                    .perform(
                        post("/provider-firms")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(CREATE_FIRM_BODY))
                    .andExpect(status().isCreated());
              } catch (Exception e) {
                throw new RuntimeException(e);
              }
            })
        .andWaitForEventOfType(ProviderFirmChangedSnapshotEvent.class)
        .toArriveAndVerify(
            event -> {
              try {
                mockMvc
                    .perform(get("/provider-events"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content.length()").value(1))
                    .andExpect(
                        jsonPath("$.data.content[0].eventType")
                            .value("ProviderFirmChangedSnapshotEvent"));
              } catch (Exception e) {
                throw new RuntimeException(e);
              }
            });
  }

  @Test
  void getEventByGUID_returnsFullEvent(Scenario scenario) {
    scenario
        .stimulate(
            () -> {
              try {
                mockMvc
                    .perform(
                        post("/provider-firms")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(CREATE_FIRM_BODY))
                    .andExpect(status().isCreated());
              } catch (Exception e) {
                throw new RuntimeException(e);
              }
            })
        .andWaitForEventOfType(ProviderFirmChangedSnapshotEvent.class)
        .toArriveAndVerify(
            event -> {
              String guid = event.eventGuid().toString();
              try {
                mockMvc
                    .perform(get("/provider-events/{guid}", guid))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.eventHeader.guid").value(guid))
                    .andExpect(jsonPath("$.data.eventPayload").isNotEmpty());
              } catch (Exception e) {
                throw new RuntimeException(e);
              }
            });
  }
}
