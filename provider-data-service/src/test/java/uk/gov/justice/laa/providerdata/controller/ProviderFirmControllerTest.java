package uk.gov.justice.laa.providerdata.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.support.DefaultFormattingConversionService;
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
import uk.gov.justice.laa.providerdata.util.ProviderFirmTypeConverter;

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

    DefaultFormattingConversionService conversionService = new DefaultFormattingConversionService();
    conversionService.addConverter(new ProviderFirmTypeConverter()); // ✅ KEY FIX

    mockMvc =
        MockMvcBuilders.standaloneSetup(
                new ProviderFirmController(
                    providerFirmCreationService,
                    providerFirmService,
                    officeMapper,
                    providerFirmMapper))
            .setControllerAdvice(new GlobalExceptionHandler())
            .setConversionService(conversionService) // ✅ REGISTER HERE
            .build();
  }

  @Test
  void createProviderFirm_lsp_returns201WithGuidAndFirmNumber() throws Exception {
    UUID guid = UUID.randomUUID();
    when(providerFirmCreationService.createLspFirm(any(), any(), any(), any(), any(), any()))
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
    when(providerFirmCreationService.createPractitionerFirm(any(), any(), any()))
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
    when(providerFirmService.getLspHeadOffice(entity)).thenReturn(Optional.empty());
    when(providerFirmService.getChambersHeadOffice(entity)).thenReturn(Optional.empty());
    when(providerFirmService.getAdvocateOfficeLink(entity)).thenReturn(Optional.empty());
    when(providerFirmService.getParentLinks(entity)).thenReturn(List.of());
    when(providerFirmMapper.toProviderV2(entity, null, null, null, List.of()))
        .thenReturn(providerV2);

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

  // ------------------- NEW TESTS FOR GET PROVIDER FIRMS -------------------

  @Test
  void getProviderFirms_withTypeFilter_returns200WithFilteredList() throws Exception {
    UUID guid1 = UUID.randomUUID();
    UUID guid2 = UUID.randomUUID();

    // Mock provider entities
    ProviderEntity entity1 =
        ProviderEntity.builder().firmNumber("FRM001").name("Test Advocate 1").build();
    entity1.setGuid(guid1);
    ProviderEntity entity2 =
        ProviderEntity.builder().firmNumber("FRM002").name("Test Advocate 2").build();
    entity2.setGuid(guid2);

    // Mock paged result
    Page<ProviderEntity> page = new PageImpl<>(List.of(entity1, entity2), PageRequest.of(0, 20), 2);
    when(providerFirmService.searchProviders(
            any(), any(), any(), any(), any(), any(PageRequest.class)))
        .thenReturn(page);

    // Mock head office / parent links
    when(providerFirmService.getLspHeadOffice(entity1)).thenReturn(Optional.empty());
    when(providerFirmService.getChambersHeadOffice(entity1)).thenReturn(Optional.empty());
    when(providerFirmService.getAdvocateOfficeLink(entity1)).thenReturn(Optional.empty());
    when(providerFirmService.getParentLinks(entity1)).thenReturn(List.of());

    when(providerFirmService.getLspHeadOffice(entity2)).thenReturn(Optional.empty());
    when(providerFirmService.getChambersHeadOffice(entity2)).thenReturn(Optional.empty());
    when(providerFirmService.getAdvocateOfficeLink(entity2)).thenReturn(Optional.empty());
    when(providerFirmService.getParentLinks(entity2)).thenReturn(List.of());

    // Mock mapping to DTOs
    when(providerFirmMapper.toProviderV2(entity1, null, null, null, List.of()))
        .thenReturn(
            new ProviderV2()
                .guid(guid1.toString())
                .firmNumber("FRM001")
                .firmType(ProviderFirmTypeV2.ADVOCATE)
                .name("Test Advocate 1"));

    when(providerFirmMapper.toProviderV2(entity2, null, null, null, List.of()))
        .thenReturn(
            new ProviderV2()
                .guid(guid2.toString())
                .firmNumber("FRM002")
                .firmType(ProviderFirmTypeV2.ADVOCATE)
                .name("Test Advocate 2"));

    // Perform request
    mockMvc
        .perform(
            get("/provider-firms")
                .param("type", "Advocate")
                .param("page", "0")
                .param("pageSize", "20")
                .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        // Check content
        .andExpect(jsonPath("$.data.content.length()").value(2))
        .andExpect(jsonPath("$.data.content[0].guid").value(guid1.toString()))
        .andExpect(jsonPath("$.data.content[0].firmType").value("Advocate"))
        .andExpect(jsonPath("$.data.content[1].guid").value(guid2.toString()))
        .andExpect(jsonPath("$.data.content[1].firmType").value("Advocate"))
        // Check pagination metadata
        .andExpect(jsonPath("$.data.metadata.pagination.currentPage").value(0))
        .andExpect(jsonPath("$.data.metadata.pagination.pageSize").value(20))
        .andExpect(jsonPath("$.data.metadata.pagination.totalItems").value(2))
        .andExpect(jsonPath("$.data.metadata.pagination.totalPages").value(1))
        // Check that search criteria includes 'type'
        .andExpect(
            jsonPath("$.data.metadata.searchCriteria.criteria[?(@.filter=='type')].values[0]")
                .value("Advocate"))
        // Check links are present
        .andExpect(jsonPath("$.data.links").exists());
  }

  @Test
  void getProviderFirms_withoutFilter_returns200WithAll() throws Exception {
    UUID guid = UUID.randomUUID();

    ProviderEntity entity =
        ProviderEntity.builder().firmNumber("FRM001").name("Test Advocate").build();
    entity.setGuid(guid);

    Page<ProviderEntity> page = new PageImpl<>(List.of(entity), PageRequest.of(0, 20), 1);

    when(providerFirmService.searchProviders(
            any(), any(), any(), any(), any(), any(PageRequest.class)))
        .thenReturn(page);

    when(providerFirmService.getLspHeadOffice(entity)).thenReturn(Optional.empty());
    when(providerFirmService.getChambersHeadOffice(entity)).thenReturn(Optional.empty());
    when(providerFirmService.getAdvocateOfficeLink(entity)).thenReturn(Optional.empty());
    when(providerFirmService.getParentLinks(entity)).thenReturn(List.of());
    when(providerFirmMapper.toProviderV2(entity, null, null, null, List.of()))
        .thenReturn(
            new ProviderV2()
                .guid(guid.toString())
                .firmNumber("FRM001")
                .firmType(ProviderFirmTypeV2.ADVOCATE)
                .name("Test Advocate"));

    mockMvc
        .perform(
            get("/provider-firms")
                .param("page", "0")
                .param("pageSize", "20")
                .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.content.length()").value(1))
        .andExpect(jsonPath("$.data.content[0].guid").value(guid.toString()))
        .andExpect(jsonPath("$.data.content[0].firmType").value("Advocate"));
  }
}
