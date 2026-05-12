package uk.gov.justice.laa.providerdata.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.justice.laa.providerdata.exception.ItemNotFoundException;
import uk.gov.justice.laa.providerdata.mapper.OfficeMapper;
import uk.gov.justice.laa.providerdata.service.ProviderCreationResult;
import uk.gov.justice.laa.providerdata.service.ProviderFirmCommandService;

/**
 * {@link WebMvcTest} slice tests for {@link ProviderFirmCommandController}.
 *
 * <p>Covers POST and PATCH endpoints, validation logic, and error paths.
 */
@WebMvcTest(ProviderFirmCommandController.class)
class ProviderFirmCommandControllerTest {

  @Autowired private MockMvc mockMvc;
  @MockitoBean private ProviderFirmCommandService commandService;
  @MockitoBean private OfficeMapper officeMapper;

  // ---- POST /v2/provider-firms ----

  @Test
  void createProviderFirm_lsp_returns201WithIdentifiers() throws Exception {
    UUID guid = UUID.randomUUID();
    when(commandService.createLspFirm(any(), any(), any(), any(), any(), any()))
        .thenReturn(new ProviderCreationResult(guid, "LSP-0001", UUID.randomUUID(), "ACC-001"));

    mockMvc
        .perform(
            post("/v2/provider-firms")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "firmType": "Legal Services Provider",
                      "name": "Test LSP",
                      "legalServicesProvider": {}
                    }
                    """))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.data.providerFirmGUID").value(guid.toString()))
        .andExpect(jsonPath("$.data.providerFirmNumber").value("LSP-0001"));
  }

  @Test
  void createProviderFirm_chambers_returns201WithIdentifiers() throws Exception {
    UUID guid = UUID.randomUUID();
    when(commandService.createChambersFirm(any(), any(), any(), any(), any()))
        .thenReturn(new ProviderCreationResult(guid, "CH-0001", UUID.randomUUID(), "ACC-002"));

    mockMvc
        .perform(
            post("/v2/provider-firms")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "firmType": "Chambers",
                      "name": "Test Chambers",
                      "chambers": {}
                    }
                    """))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.data.providerFirmGUID").value(guid.toString()))
        .andExpect(jsonPath("$.data.providerFirmNumber").value("CH-0001"));
  }

  @Test
  void createProviderFirm_practitioner_returns201() throws Exception {
    UUID guid = UUID.randomUUID();
    when(commandService.createPractitionerFirm(any(), any(), any()))
        .thenReturn(ProviderCreationResult.withoutOffice(guid, "ADV-0001"));

    mockMvc
        .perform(
            post("/v2/provider-firms")
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
        .andExpect(jsonPath("$.data.providerFirmGUID").value(guid.toString()))
        .andExpect(jsonPath("$.data.providerFirmNumber").value("ADV-0001"));
  }

  @Test
  void createProviderFirm_missingName_returns400() throws Exception {
    mockMvc
        .perform(
            post("/v2/provider-firms")
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
  void createProviderFirm_blankName_returns400() throws Exception {
    mockMvc
        .perform(
            post("/v2/provider-firms")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "name": "   ",
                      "legalServicesProvider": {}
                    }
                    """))
        .andExpect(status().isBadRequest());
  }

  @Test
  void createProviderFirm_noVariant_returns400() throws Exception {
    mockMvc
        .perform(
            post("/v2/provider-firms")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "name": "Test"
                    }
                    """))
        .andExpect(status().isBadRequest());
  }

  @Test
  void createProviderFirm_multipleVariants_returns400() throws Exception {
    mockMvc
        .perform(
            post("/v2/provider-firms")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "name": "Test",
                      "legalServicesProvider": {},
                      "chambers": {}
                    }
                    """))
        .andExpect(status().isBadRequest());
  }

  @Test
  void createProviderFirm_inconsistentFirmType_returns400() throws Exception {
    mockMvc
        .perform(
            post("/v2/provider-firms")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "firmType": "Chambers",
                      "name": "Test",
                      "legalServicesProvider": {}
                    }
                    """))
        .andExpect(status().isBadRequest());
  }

  // ---- PATCH /v2/provider-firms/{id} ----

  @Test
  void patchProviderFirm_nameOnly_returns200() throws Exception {
    UUID guid = UUID.randomUUID();
    when(commandService.patchProviderFirm(anyString(), any()))
        .thenReturn(ProviderCreationResult.withoutOffice(guid, "LSP-0001"));

    mockMvc
        .perform(
            patch("/v2/provider-firms/{id}", guid)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    { "name": "Updated Name" }
                    """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.providerFirmGUID").value(guid.toString()))
        .andExpect(jsonPath("$.data.providerFirmNumber").value("LSP-0001"));
  }

  @Test
  void patchProviderFirm_lspDetails_returns200() throws Exception {
    UUID guid = UUID.randomUUID();
    when(commandService.patchProviderFirm(anyString(), any()))
        .thenReturn(ProviderCreationResult.withoutOffice(guid, "LSP-0001"));

    mockMvc
        .perform(
            patch("/v2/provider-firms/{id}", guid)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "legalServicesProvider": {
                        "companiesHouseNumber": "12345678"
                      }
                    }
                    """))
        .andExpect(status().isOk());
  }

  @Test
  void patchProviderFirm_emptyBody_returns400() throws Exception {
    mockMvc
        .perform(
            patch("/v2/provider-firms/{id}", UUID.randomUUID())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void patchProviderFirm_blankName_returns400() throws Exception {
    mockMvc
        .perform(
            patch("/v2/provider-firms/{id}", UUID.randomUUID())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    { "name": "  " }
                    """))
        .andExpect(status().isBadRequest());
  }

  @Test
  void patchProviderFirm_mixedSubtypes_returns400() throws Exception {
    mockMvc
        .perform(
            patch("/v2/provider-firms/{id}", UUID.randomUUID())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "legalServicesProvider": {},
                      "practitioner": {}
                    }
                    """))
        .andExpect(status().isBadRequest());
  }

  @Test
  void patchProviderFirm_headOfficeReassignment_returns400() throws Exception {
    mockMvc
        .perform(
            patch("/v2/provider-firms/{id}", UUID.randomUUID())
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
  void patchProviderFirm_notFound_returns404() throws Exception {
    when(commandService.patchProviderFirm(anyString(), any()))
        .thenThrow(new ItemNotFoundException("not found"));

    mockMvc
        .perform(
            patch("/v2/provider-firms/{id}", UUID.randomUUID())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    { "name": "New Name" }
                    """))
        .andExpect(status().isNotFound());
  }
}
