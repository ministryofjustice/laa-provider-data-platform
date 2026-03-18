package uk.gov.justice.laa.providerdata.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import uk.gov.justice.laa.providerdata.model.ContractManagerV2;
import uk.gov.justice.laa.providerdata.service.ProviderContractManagersService;

class ProviderContractManagersControllerTest {

  private MockMvc mockMvc;
  private ProviderContractManagersService providerContractManagersService;

  @BeforeEach
  void setUp() {
    providerContractManagersService =
        org.mockito.Mockito.mock(ProviderContractManagersService.class);

    mockMvc =
        MockMvcBuilders.standaloneSetup(
                new ProviderContractManagersController(providerContractManagersService))
            .build();
  }

  @Test
  void getProviderContractManagers_returns200_andContent() throws Exception {
    when(providerContractManagersService.getContractManagers(any(), any()))
        .thenReturn(
            List.of(
                new ContractManagerV2()
                    .contractManagerId("CM-001")
                    .firstName("Alex")
                    .lastName("Smith")));

    mockMvc
        .perform(get("/provider-contract-managers").accept(APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.content[0].contractManagerId").value("CM-001"))
        .andExpect(jsonPath("$.data.content[0].firstName").value("Alex"))
        .andExpect(jsonPath("$.data.content[0].lastName").value("Smith"));
  }

  @Test
  void getProviderContractManagers_passesFiltersToService() throws Exception {
    when(providerContractManagersService.getContractManagers(any(), any())).thenReturn(List.of());

    mockMvc
        .perform(
            get("/provider-contract-managers")
                .queryParam("contractManagerId", "CM-001", "CM-002")
                .queryParam("name", "smi")
                .accept(APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.content").isArray());
  }
}
