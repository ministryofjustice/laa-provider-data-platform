package uk.gov.justice.laa.providerdata.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import uk.gov.justice.laa.providerdata.entity.ProviderEntity;
import uk.gov.justice.laa.providerdata.entity.ProviderParentLinkEntity;
import uk.gov.justice.laa.providerdata.exception.GlobalExceptionHandler;
import uk.gov.justice.laa.providerdata.mapper.ProviderMapper;
import uk.gov.justice.laa.providerdata.model.OfficePractitionerV2;
import uk.gov.justice.laa.providerdata.service.ProviderService;

class ChamberOfficePractitionersControllerTest {

  private MockMvc mockMvc;
  private ProviderService providerService;
  private ProviderMapper providerMapper;

  @BeforeEach
  void setUp() {
    providerService = mock(ProviderService.class);
    providerMapper = mock(ProviderMapper.class);

    mockMvc =
        MockMvcBuilders.standaloneSetup(
                new ChamberOfficePractitionersController(providerService, providerMapper))
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();
  }

  @Test
  void getProviderFirmOfficePractitioners_returns200WithPractitioners() throws Exception {
    String chambersId = UUID.randomUUID().toString();
    ProviderEntity practitioner = ProviderEntity.builder().name("Practitioner").build();
    ProviderParentLinkEntity link =
        ProviderParentLinkEntity.builder().provider(practitioner).build();

    when(providerService.getPractitionersByChambers(anyString(), any()))
        .thenReturn(new PageImpl<>(List.of(link), PageRequest.of(0, 20), 1));
    when(providerService.getAdvocateOfficeLink(practitioner)).thenReturn(Optional.empty());
    when(providerService.getParentLinks(practitioner)).thenReturn(List.of());
    when(providerMapper.toOfficePractitionerV2(any(), any(), any()))
        .thenReturn(new OfficePractitionerV2().name("Practitioner"));

    mockMvc
        .perform(
            get("/provider-firms/{id}/practitioners", chambersId)
                .param("page", "0")
                .param("pageSize", "20"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.content[0].name").value("Practitioner"))
        .andExpect(jsonPath("$.data.metadata.pagination.currentPage").value(0))
        .andExpect(jsonPath("$.data.metadata.pagination.pageSize").value(20))
        .andExpect(jsonPath("$.data.metadata.pagination.totalItems").value(1))
        .andExpect(jsonPath("$.data.metadata.pagination.totalPages").value(1))
        .andExpect(jsonPath("$.data.links.self").exists())
        .andExpect(jsonPath("$.data.links.first").exists())
        .andExpect(jsonPath("$.data.links.last").exists());
  }

  @Test
  void getProviderFirmOfficePractitioners_returnsBadRequest_whenPageIsNegative() throws Exception {
    mockMvc
        .perform(get("/provider-firms/{id}/practitioners", UUID.randomUUID()).param("page", "-1"))
        .andExpect(status().isBadRequest());
  }
}
