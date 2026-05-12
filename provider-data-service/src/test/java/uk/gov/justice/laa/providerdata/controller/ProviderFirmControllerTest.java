package uk.gov.justice.laa.providerdata.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.justice.laa.providerdata.exception.ItemNotFoundException;
import uk.gov.justice.laa.providerdata.model.CreateProviderFirm201Response;
import uk.gov.justice.laa.providerdata.model.CreateProviderFirm201ResponseData;
import uk.gov.justice.laa.providerdata.model.GetProviderFirmByGUIDorFirmNumber200Response;
import uk.gov.justice.laa.providerdata.model.GetProviderFirms200Response;
import uk.gov.justice.laa.providerdata.model.GetProviderFirms200ResponseData;
import uk.gov.justice.laa.providerdata.model.LinksV2;
import uk.gov.justice.laa.providerdata.model.ProviderFirmTypeV2;
import uk.gov.justice.laa.providerdata.model.ProviderV2;
import uk.gov.justice.laa.providerdata.util.PageMetadata;

@WebMvcTest(ProviderFirmController.class)
class ProviderFirmControllerTest {

  @Autowired private MockMvc mockMvc;
  @MockitoBean private ProviderFirmCommandController commandController;
  @MockitoBean private ProviderFirmQueryController queryController;

  @Test
  void createProviderFirm_lsp_returns201WithGuidAndFirmNumber() throws Exception {
    UUID guid = UUID.randomUUID();
    when(commandController.createProviderFirm(any()))
        .thenReturn(
            ResponseEntity.status(HttpStatus.CREATED)
                .body(
                    new CreateProviderFirm201Response()
                        .data(
                            new CreateProviderFirm201ResponseData()
                                .providerFirmGUID(guid)
                                .providerFirmNumber("LSP-ABCD1234"))));

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
    when(commandController.createProviderFirm(any()))
        .thenReturn(
            ResponseEntity.status(HttpStatus.CREATED)
                .body(
                    new CreateProviderFirm201Response()
                        .data(
                            new CreateProviderFirm201ResponseData()
                                .providerFirmGUID(guid)
                                .providerFirmNumber("CH-ABCD1234"))));

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
    when(commandController.createProviderFirm(any()))
        .thenReturn(
            ResponseEntity.status(HttpStatus.CREATED)
                .body(
                    new CreateProviderFirm201Response()
                        .data(
                            new CreateProviderFirm201ResponseData()
                                .providerFirmGUID(guid)
                                .providerFirmNumber("ADV-ABCD1234"))));

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
    when(commandController.createProviderFirm(any()))
        .thenReturn(ResponseEntity.badRequest().build());

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
    when(commandController.createProviderFirm(any()))
        .thenReturn(ResponseEntity.badRequest().build());

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
    when(commandController.createProviderFirm(any()))
        .thenReturn(ResponseEntity.badRequest().build());

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
    ProviderV2 providerV2 =
        new ProviderV2()
            .guid(guid)
            .firmNumber("100001")
            .firmType(ProviderFirmTypeV2.LEGAL_SERVICES_PROVIDER)
            .name("My LSP");
    when(queryController.getProviderFirm(guid.toString()))
        .thenReturn(
            ResponseEntity.ok(new GetProviderFirmByGUIDorFirmNumber200Response().data(providerV2)));

    mockMvc
        .perform(get("/provider-firms/{guid}", guid))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.guid").value(guid.toString()))
        .andExpect(jsonPath("$.data.firmNumber").value("100001"))
        .andExpect(jsonPath("$.data.name").value("My LSP"));
  }

  @Test
  void getProviderFirm_notFound_returns404() throws Exception {
    when(queryController.getProviderFirm(anyString()))
        .thenThrow(new ItemNotFoundException("Provider not found: UNKNOWN"));

    mockMvc.perform(get("/provider-firms/{id}", "UNKNOWN")).andExpect(status().isNotFound());
  }

  @Test
  void patchProviderFirm_lspNameAndBasicDetails_returns200WithIdentifiers() throws Exception {
    UUID guid = UUID.randomUUID();
    when(commandController.patchProviderFirm(anyString(), any()))
        .thenReturn(
            ResponseEntity.ok(
                new CreateProviderFirm201Response()
                    .data(
                        new CreateProviderFirm201ResponseData()
                            .providerFirmGUID(guid)
                            .providerFirmNumber("100001"))));

    mockMvc
        .perform(
            patch("/provider-firms/{id}", guid.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "name": "Updated Firm Name",
                      "legalServicesProvider": {
                        "constitutionalStatus": "Partnership",
                        "indemnityReceivedDate": "2024-01-02",
                        "companiesHouseNumber": "12345678"
                      }
                    }
                    """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.providerFirmGUID").value(guid.toString()))
        .andExpect(jsonPath("$.data.providerFirmNumber").value("100001"));
  }

  @Test
  void patchProviderFirm_practitionerDetails_returns200WithIdentifiers() throws Exception {
    UUID guid = UUID.randomUUID();
    when(commandController.patchProviderFirm(anyString(), any()))
        .thenReturn(
            ResponseEntity.ok(
                new CreateProviderFirm201Response()
                    .data(
                        new CreateProviderFirm201ResponseData()
                            .providerFirmGUID(guid)
                            .providerFirmNumber("100003"))));

    mockMvc
        .perform(
            patch("/provider-firms/{id}", guid.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "practitioner": {
                        "advocateLevel": "KC"
                      }
                    }
                    """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.providerFirmGUID").value(guid.toString()))
        .andExpect(jsonPath("$.data.providerFirmNumber").value("100003"));
  }

  @Test
  void patchProviderFirm_mixedSubtypeBranchesRejected_returns400() throws Exception {
    when(commandController.patchProviderFirm(anyString(), any()))
        .thenReturn(ResponseEntity.badRequest().build());

    mockMvc
        .perform(
            patch("/provider-firms/{id}", "100001")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "legalServicesProvider": {
                        "companiesHouseNumber": "12345678"
                      },
                      "practitioner": {
                        "advocateLevel": "KC"
                      }
                    }
                    """))
        .andExpect(status().isBadRequest());
  }

  @Test
  void patchProviderFirm_headOfficeReassignmentRejected_returns400() throws Exception {
    when(commandController.patchProviderFirm(anyString(), any()))
        .thenReturn(ResponseEntity.badRequest().build());

    mockMvc
        .perform(
            patch("/provider-firms/{id}", "100001")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "legalServicesProvider": {
                        "headOffice": {
                          "officeGUID": "11111111-1111-1111-1111-111111111111"
                        }
                      }
                    }
                    """))
        .andExpect(status().isBadRequest());
  }

  @Test
  void patchProviderFirm_emptyPatchRejected_returns400() throws Exception {
    when(commandController.patchProviderFirm(anyString(), any()))
        .thenReturn(ResponseEntity.badRequest().build());

    mockMvc
        .perform(
            patch("/provider-firms/{id}", "100001")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{ }"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void getProviderFirms_withTypeFilter_returns200WithFilteredList() throws Exception {
    UUID guid1 = UUID.randomUUID();
    UUID guid2 = UUID.randomUUID();

    Page<ProviderV2> page =
        new PageImpl<>(
            List.of(
                new ProviderV2()
                    .guid(guid1)
                    .firmNumber("100001")
                    .firmType(ProviderFirmTypeV2.ADVOCATE)
                    .name("Test Advocate 1"),
                new ProviderV2()
                    .guid(guid2)
                    .firmNumber("100002")
                    .firmType(ProviderFirmTypeV2.ADVOCATE)
                    .name("Test Advocate 2")),
            PageRequest.of(0, 20),
            2);

    when(queryController.getProviderFirms(
            any(), any(), any(), any(), any(), any(), any(), any(), any()))
        .thenReturn(
            ResponseEntity.ok(
                new GetProviderFirms200Response()
                    .data(
                        new GetProviderFirms200ResponseData()
                            .content(page.getContent())
                            .metadata(
                                PageMetadata.builder(page)
                                    .search("type", List.of(ProviderFirmTypeV2.ADVOCATE.getValue()))
                                    .build())
                            .links(new LinksV2()))));

    mockMvc
        .perform(
            get("/provider-firms")
                .param("type", ProviderFirmTypeV2.ADVOCATE.getValue())
                .param("page", "0")
                .param("pageSize", "20")
                .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.content.length()").value(2))
        .andExpect(jsonPath("$.data.content[0].guid").value(guid1.toString()))
        .andExpect(
            jsonPath("$.data.content[0].firmType").value(ProviderFirmTypeV2.ADVOCATE.getValue()))
        .andExpect(jsonPath("$.data.content[1].guid").value(guid2.toString()))
        .andExpect(
            jsonPath("$.data.content[1].firmType").value(ProviderFirmTypeV2.ADVOCATE.getValue()))
        .andExpect(jsonPath("$.data.metadata.pagination.currentPage").value(0))
        .andExpect(jsonPath("$.data.metadata.pagination.pageSize").value(20))
        .andExpect(jsonPath("$.data.metadata.pagination.totalItems").value(2))
        .andExpect(jsonPath("$.data.metadata.pagination.totalPages").value(1))
        .andExpect(
            jsonPath("$.data.metadata.searchCriteria.criteria[?(@.filter=='type')].values[0]")
                .value(ProviderFirmTypeV2.ADVOCATE.getValue()))
        .andExpect(jsonPath("$.data.links").exists());
  }

  @Test
  void getProviderFirms_withoutFilter_returns200WithAll() throws Exception {
    UUID guid = UUID.randomUUID();

    Page<ProviderV2> page =
        new PageImpl<>(
            List.of(
                new ProviderV2()
                    .guid(guid)
                    .firmNumber("100001")
                    .firmType(ProviderFirmTypeV2.ADVOCATE)
                    .name("Test Advocate")),
            PageRequest.of(0, 20),
            1);

    when(queryController.getProviderFirms(
            any(), any(), any(), any(), any(), any(), any(), any(), any()))
        .thenReturn(
            ResponseEntity.ok(
                new GetProviderFirms200Response()
                    .data(
                        new GetProviderFirms200ResponseData()
                            .content(page.getContent())
                            .metadata(PageMetadata.builder(page).build())
                            .links(new LinksV2()))));

    mockMvc
        .perform(
            get("/provider-firms")
                .param("page", "0")
                .param("pageSize", "20")
                .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.content.length()").value(1))
        .andExpect(jsonPath("$.data.content[0].guid").value(guid.toString()))
        .andExpect(
            jsonPath("$.data.content[0].firmType").value(ProviderFirmTypeV2.ADVOCATE.getValue()));
  }
}
