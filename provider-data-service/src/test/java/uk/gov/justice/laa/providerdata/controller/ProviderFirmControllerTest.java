package uk.gov.justice.laa.providerdata.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import uk.gov.justice.laa.providerdata.entity.ProviderEntity;
import uk.gov.justice.laa.providerdata.exception.GlobalExceptionHandler;
import uk.gov.justice.laa.providerdata.exception.ItemNotFoundException;
import uk.gov.justice.laa.providerdata.mapper.OfficeMapper;
import uk.gov.justice.laa.providerdata.mapper.ProviderMapper;
import uk.gov.justice.laa.providerdata.model.ProviderFirmTypeV2;
import uk.gov.justice.laa.providerdata.model.ProviderV2;
import uk.gov.justice.laa.providerdata.service.ProviderCreationResult;
import uk.gov.justice.laa.providerdata.service.ProviderCreationService;
import uk.gov.justice.laa.providerdata.service.ProviderService;

class ProviderFirmControllerTest {

  private MockMvc mockMvc;
  private ProviderCreationService providerFirmCreationService;
  private ProviderService providerFirmService;
  private OfficeMapper officeMapper;
  private ProviderMapper providerFirmMapper;

  @BeforeEach
  void setUp() {
    providerFirmCreationService = mock(ProviderCreationService.class);
    providerFirmService = mock(ProviderService.class);
    officeMapper = mock(OfficeMapper.class);
    providerFirmMapper = mock(ProviderMapper.class);
    mockMvc =
        MockMvcBuilders.standaloneSetup(
                new ProviderFirmController(
                    providerFirmCreationService,
                    providerFirmService,
                    officeMapper,
                    providerFirmMapper))
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();
  }

  @Test
  void createProviderFirm_lsp_returns201WithGuidAndFirmNumber() throws Exception {
    UUID guid = UUID.randomUUID();
    when(providerFirmCreationService.createLspFirm(any(), any(), any(), any(), any()))
        .thenReturn(new ProviderCreationResult(guid, "LSP-ABCD1234", UUID.randomUUID(), "ACC001"));

    mockMvc
        .perform(
            post("/provider-firms")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "firmType": "Legal Services Provider",
                      "name": "My LSP",
                      "legalServicesProvider": {}
                    }
                    """))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.data.providerFirmGUID").value(guid.toString()))
        .andExpect(jsonPath("$.data.providerFirmNumber").value("LSP-ABCD1234"));
  }

  @Test
  void createProviderFirm_chambers_returns201WithGuidAndFirmNumber() throws Exception {
    UUID guid = UUID.randomUUID();
    when(providerFirmCreationService.createChambersFirm(any(), any(), any(), any(), any()))
        .thenReturn(new ProviderCreationResult(guid, "CH-ABCD1234", UUID.randomUUID(), "ACC002"));

    mockMvc
        .perform(
            post("/provider-firms")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "firmType": "Chambers",
                      "name": "Northgate Chambers",
                      "chambers": {}
                    }
                    """))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.data.providerFirmGUID").value(guid.toString()))
        .andExpect(jsonPath("$.data.providerFirmNumber").value("CH-ABCD1234"));
  }

  @Test
  void createProviderFirm_practitioner_returns201() throws Exception {
    UUID guid = UUID.randomUUID();
    when(providerFirmCreationService.createPractitionerFirm(any()))
        .thenReturn(ProviderCreationResult.withoutOffice(guid, "ADV-ABCD1234"));

    mockMvc
        .perform(
            post("/provider-firms")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "firmType": "Advocate",
                      "name": "A. Barrister",
                      "practitioner": {}
                    }
                    """))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.data.providerFirmGUID").value(guid.toString()));
  }

  @Test
  void createProviderFirm_missingVariant_returns400() throws Exception {
    mockMvc
        .perform(
            post("/provider-firms")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "firmType": "Legal Services Provider",
                      "name": "My LSP"
                    }
                    """))
        .andExpect(status().isBadRequest());
  }

  @Test
  void createProviderFirm_missingName_returns400() throws Exception {
    mockMvc
        .perform(
            post("/provider-firms")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "firmType": "Legal Services Provider",
                      "legalServicesProvider": {}
                    }
                    """))
        .andExpect(status().isBadRequest());
  }

  @Test
  void createProviderFirm_inconsistentFirmType_returns400() throws Exception {
    mockMvc
        .perform(
            post("/provider-firms")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "firmType": "Chambers",
                      "name": "My LSP",
                      "legalServicesProvider": {}
                    }
                    """))
        .andExpect(status().isBadRequest());
  }

  @Test
  void getProviderFirm_byGuid_returns200WithProviderDetails() throws Exception {
    UUID guid = UUID.randomUUID();
    ProviderEntity entity =
        ProviderEntity.builder().firmNumber("LSP-ABC123").name("My LSP").build();
    entity.setGuid(guid);
    ProviderV2 providerV2 =
        new ProviderV2()
            .guid(guid.toString())
            .firmNumber("LSP-ABC123")
            .firmType(ProviderFirmTypeV2.LEGAL_SERVICES_PROVIDER)
            .name("My LSP");
    when(providerFirmService.getProvider(guid.toString())).thenReturn(entity);
    when(providerFirmMapper.toProviderV2(entity)).thenReturn(providerV2);

    mockMvc
        .perform(get("/provider-firms/{guid}", guid))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.guid").value(guid.toString()))
        .andExpect(jsonPath("$.data.firmNumber").value("LSP-ABC123"))
        .andExpect(jsonPath("$.data.name").value("My LSP"));
  }

  @Test
  void getProviderFirm_notFound_returns404() throws Exception {
    when(providerFirmService.getProvider(anyString()))
        .thenThrow(new ItemNotFoundException("Provider not found: UNKNOWN"));

    mockMvc.perform(get("/provider-firms/{id}", "UNKNOWN")).andExpect(status().isNotFound());
  }
}
