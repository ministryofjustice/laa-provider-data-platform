package uk.gov.justice.laa.providerdata.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.justice.laa.providerdata.entity.ContractManagerEntity;
import uk.gov.justice.laa.providerdata.mapper.ContractManagerMapper;
import uk.gov.justice.laa.providerdata.model.ContractManagerV2;
import uk.gov.justice.laa.providerdata.service.ProviderContractManagersService;

@WebMvcTest(ProviderContractManagersController.class)
class ProviderContractManagersControllerTest {

  @Autowired private MockMvc mockMvc;
  @MockitoBean private ProviderContractManagersService providerContractManagersService;
  @MockitoBean private ContractManagerMapper contractManagerMapper;

  @Test
  void getProviderContractManagers_returns200_andContentAndMetadata() throws Exception {
    ContractManagerEntity entity = new ContractManagerEntity();
    entity.setContractManagerId("CM-001");
    entity.setFirstName("Alex");
    entity.setLastName("Smith");

    when(providerContractManagersService.getContractManagers(any(), any(), any()))
        .thenReturn(new PageImpl<>(List.of(entity)));

    when(contractManagerMapper.toContractManagerV2(any()))
        .thenReturn(
            new ContractManagerV2()
                .contractManagerId("CM-001")
                .firstName("Alex")
                .lastName("Smith"));

    mockMvc
        .perform(get("/provider-contract-managers").accept(APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.content[0].contractManagerId").value("CM-001"))
        .andExpect(jsonPath("$.data.metadata").exists())
        .andExpect(jsonPath("$.data.links").exists());
  }

  @Test
  void getProviderContractManagers_acceptsPaginationParams() throws Exception {
    when(providerContractManagersService.getContractManagers(any(), any(), any()))
        .thenReturn(new PageImpl<>(List.of()));

    mockMvc
        .perform(
            get("/provider-contract-managers")
                .queryParam("page", "0")
                .queryParam("pageSize", "2")
                .accept(APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.content").isArray());
  }
}
