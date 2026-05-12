package uk.gov.justice.laa.providerdata.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.justice.laa.providerdata.exception.ItemNotFoundException;
import uk.gov.justice.laa.providerdata.model.ProviderFirmTypeV2;
import uk.gov.justice.laa.providerdata.model.ProviderV2;
import uk.gov.justice.laa.providerdata.service.ProviderFirmQueryService;

/**
 * {@link WebMvcTest} slice tests for {@link ProviderFirmQueryController}.
 *
 * <p>Covers GET list and GET single endpoints, including pagination, filtering, and error paths.
 */
@WebMvcTest(ProviderFirmQueryController.class)
class ProviderFirmQueryControllerTest {

  @Autowired private MockMvc mockMvc;
  @MockitoBean private ProviderFirmQueryService queryService;

  // ---- GET /v2/provider-firms ----

  @Test
  void getProviderFirms_returnsPagedList() throws Exception {
    UUID guid1 = UUID.randomUUID();
    UUID guid2 = UUID.randomUUID();

    Page<ProviderV2> page =
        new PageImpl<>(
            List.of(
                new ProviderV2()
                    .guid(guid1)
                    .firmNumber("LSP-0001")
                    .firmType(ProviderFirmTypeV2.LEGAL_SERVICES_PROVIDER)
                    .name("Firm A"),
                new ProviderV2()
                    .guid(guid2)
                    .firmNumber("LSP-0002")
                    .firmType(ProviderFirmTypeV2.LEGAL_SERVICES_PROVIDER)
                    .name("Firm B")),
            PageRequest.of(0, 20),
            2);

    when(queryService.searchProviderFirms(any(), any(), any(), any(), any())).thenReturn(page);

    mockMvc
        .perform(
            get("/v2/provider-firms")
                .param("page", "0")
                .param("pageSize", "20")
                .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.content.length()").value(2))
        .andExpect(jsonPath("$.data.content[0].guid").value(guid1.toString()))
        .andExpect(jsonPath("$.data.content[0].firmNumber").value("LSP-0001"))
        .andExpect(jsonPath("$.data.content[1].guid").value(guid2.toString()))
        .andExpect(jsonPath("$.data.metadata.pagination.totalItems").value(2))
        .andExpect(jsonPath("$.data.metadata.pagination.currentPage").value(0))
        .andExpect(jsonPath("$.data.metadata.pagination.pageSize").value(20))
        .andExpect(jsonPath("$.data.metadata.pagination.totalPages").value(1));
  }

  @Test
  void getProviderFirms_withTypeFilter_passesTypeToService() throws Exception {
    UUID guid = UUID.randomUUID();
    Page<ProviderV2> page =
        new PageImpl<>(
            List.of(
                new ProviderV2()
                    .guid(guid)
                    .firmNumber("CH-0001")
                    .firmType(ProviderFirmTypeV2.CHAMBERS)
                    .name("Chambers X")),
            PageRequest.of(0, 20),
            1);

    when(queryService.searchProviderFirms(any(), any(), any(), any(), any())).thenReturn(page);

    mockMvc
        .perform(
            get("/v2/provider-firms")
                .param("type", "Chambers")
                .param("page", "0")
                .param("pageSize", "20"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.content[0].firmType").value("Chambers"))
        .andExpect(
            jsonPath("$.data.metadata.searchCriteria.criteria[?(@.filter=='type')].values[0]")
                .value("Chambers"));
  }

  @Test
  void getProviderFirms_emptyResult_returns200WithEmptyContent() throws Exception {
    Page<ProviderV2> empty = new PageImpl<>(List.of(), PageRequest.of(0, 20), 0);
    when(queryService.searchProviderFirms(any(), any(), any(), any(), any())).thenReturn(empty);

    mockMvc
        .perform(get("/v2/provider-firms").param("page", "0").param("pageSize", "20"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.content.length()").value(0))
        .andExpect(jsonPath("$.data.metadata.pagination.totalItems").value(0));
  }

  // ---- GET /v2/provider-firms/{id} ----

  @Test
  void getProviderFirm_byGuid_returns200() throws Exception {
    UUID guid = UUID.randomUUID();
    ProviderV2 model =
        new ProviderV2()
            .guid(guid)
            .firmNumber("LSP-0001")
            .firmType(ProviderFirmTypeV2.LEGAL_SERVICES_PROVIDER)
            .name("My LSP");
    when(queryService.getProviderFirm(guid.toString())).thenReturn(model);

    mockMvc
        .perform(get("/v2/provider-firms/{id}", guid))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.guid").value(guid.toString()))
        .andExpect(jsonPath("$.data.firmNumber").value("LSP-0001"))
        .andExpect(jsonPath("$.data.name").value("My LSP"))
        .andExpect(jsonPath("$.data.firmType").value("Legal Services Provider"));
  }

  @Test
  void getProviderFirm_byFirmNumber_returns200() throws Exception {
    UUID guid = UUID.randomUUID();
    ProviderV2 model =
        new ProviderV2()
            .guid(guid)
            .firmNumber("CH-0001")
            .firmType(ProviderFirmTypeV2.CHAMBERS)
            .name("Test Chambers");
    when(queryService.getProviderFirm("CH-0001")).thenReturn(model);

    mockMvc
        .perform(get("/v2/provider-firms/{id}", "CH-0001"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.firmNumber").value("CH-0001"))
        .andExpect(jsonPath("$.data.firmType").value("Chambers"));
  }

  @Test
  void getProviderFirm_notFound_returns404() throws Exception {
    when(queryService.getProviderFirm(anyString()))
        .thenThrow(new ItemNotFoundException("Provider not found: UNKNOWN"));

    mockMvc.perform(get("/v2/provider-firms/{id}", "UNKNOWN")).andExpect(status().isNotFound());
  }
}
