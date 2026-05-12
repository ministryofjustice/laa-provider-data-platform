package uk.gov.justice.laa.providerdata.usecase.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.justice.laa.providerdata.contractmanager.OfficeContractManagerQueryService;
import uk.gov.justice.laa.providerdata.model.OfficeContractManagerV2;
import uk.gov.justice.laa.providerdata.usecase.ContractManagerAssignmentResult;
import uk.gov.justice.laa.providerdata.usecase.OfficeFirmUseCase;

/**
 * Unit tests for {@link ProviderFirmOfficeContractManagersController}.
 *
 * <p>Covered scenarios:
 *
 * <ul>
 *   <li>201 Created on successful assignment with expected response fields
 *   <li>400 Bad Request when path or body GUIDs are missing/invalid
 *   <li>500 Internal Server Error when the service throws an unexpected exception
 * </ul>
 */
@WebMvcTest(ProviderFirmOfficeContractManagersController.class)
class ProviderFirmOfficeContractManagersControllerTest {

  @Autowired private MockMvc mockMvc;
  @MockitoBean private OfficeFirmUseCase officeFirmUseCase;
  @MockitoBean private OfficeContractManagerQueryService contractManagerService;

  /**
   * Verifies a successful POST request returns 201 with the office GUID and contract manager ID.
   */
  @Test
  void postContractManagers_returns201_andBodyContainsOfficeGuidAndContractManagerId()
      throws Exception {
    UUID providerOfficeLinkGuid = UUID.randomUUID();
    UUID providerGuid = UUID.randomUUID();
    UUID contractManagerGuid = UUID.randomUUID();

    when(officeFirmUseCase.assignContractManager(
            eq(providerGuid.toString()),
            eq(providerOfficeLinkGuid.toString()),
            eq(contractManagerGuid),
            any()))
        .thenReturn(new ContractManagerAssignmentResult(providerOfficeLinkGuid, "CM-001"));

    mockMvc
        .perform(
            post(
                    "/provider-firms/{providerFirmId}/offices/{officeId}/contract-managers",
                    providerGuid,
                    providerOfficeLinkGuid)
                .contentType(APPLICATION_JSON)
                .content(
                    """
                    {
                      "contractManagerGUID": "%s"
                    }
                    """
                        .formatted(contractManagerGuid)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.data.officeGUID").value(providerOfficeLinkGuid.toString()))
        .andExpect(jsonPath("$.data.contractManagerId").value("CM-001"));
  }

  /** Ensures office codes are accepted as path values and delegated unchanged to the use case. */
  @Test
  void postContractManagers_acceptsOfficeCodePathValue() throws Exception {
    UUID providerOfficeLinkGuid = UUID.randomUUID();
    UUID contractManagerGuid = UUID.randomUUID();

    when(officeFirmUseCase.assignContractManager(
            eq("100001"), eq("ACC001"), eq(contractManagerGuid), any()))
        .thenReturn(new ContractManagerAssignmentResult(providerOfficeLinkGuid, "CM-001"));

    mockMvc
        .perform(
            post(
                    "/provider-firms/{providerFirmId}/offices/{officeId}/contract-managers",
                    "100001",
                    "ACC001")
                .contentType(APPLICATION_JSON)
                .content(
                    """
                    {
                      "contractManagerGUID": "%s"
                    }
                    """
                        .formatted(contractManagerGuid)))
        .andExpect(status().isCreated());
  }

  /** Returns 400 when the request body is missing the required {@code contractManagerGUID}. */
  @Test
  void postContractManagers_returns400_whenBodyMissingContractManagerGuid() throws Exception {
    mockMvc
        .perform(
            post(
                    "/provider-firms/{providerFirmId}/offices/{officeId}/contract-managers",
                    "100001",
                    "ACC001")
                .contentType(APPLICATION_JSON)
                .content("{}"))
        .andExpect(status().isBadRequest());
  }

  /** Returns 400 when {@code contractManagerGUID} contains only whitespace. */
  @Test
  void postContractManagers_returns400_whenContractManagerGuidIsBlank() throws Exception {
    mockMvc
        .perform(
            post(
                    "/provider-firms/{providerFirmId}/offices/{officeId}/contract-managers",
                    "100001",
                    "ACC001")
                .contentType(APPLICATION_JSON)
                .content(
                    """
                    {
                      "contractManagerGUID": "   "
                    }
                    """))
        .andExpect(status().isBadRequest());
  }

  /** Returns 400 when {@code contractManagerGUID} is not a valid UUID. */
  @Test
  void postContractManagers_returns400_whenContractManagerGuidIsNotAuuid() throws Exception {
    mockMvc
        .perform(
            post(
                    "/provider-firms/{providerFirmId}/offices/{officeId}/contract-managers",
                    "100001",
                    "ACC001")
                .contentType(APPLICATION_JSON)
                .content(
                    """
                    {
                      "contractManagerGUID": "NOT-A-UUID"
                    }
                    """))
        .andExpect(status().isBadRequest());
  }

  /** Returns 500 when the use case throws an unexpected exception. */
  @Test
  void postContractManagers_returns500_whenServiceThrowsUnexpectedException() throws Exception {
    UUID contractManagerGuid = UUID.randomUUID();

    when(officeFirmUseCase.assignContractManager(
            eq("100001"), eq("ACC001"), eq(contractManagerGuid), any()))
        .thenThrow(new RuntimeException("boom"));

    mockMvc
        .perform(
            post(
                    "/provider-firms/{providerFirmId}/offices/{officeId}/contract-managers",
                    "100001",
                    "ACC001")
                .contentType(APPLICATION_JSON)
                .content(
                    """
                    {
                      "contractManagerGUID": "%s"
                    }
                    """
                        .formatted(contractManagerGuid)))
        .andExpect(status().isInternalServerError());
  }

  @Test
  void getContractManagers_returns200_andResponseContainsManagers() throws Exception {
    UUID providerGuid = UUID.randomUUID();
    UUID providerOfficeLinkGuid = UUID.randomUUID();
    Page<OfficeContractManagerV2> mockManagers =
        new PageImpl<>(
            List.of(new OfficeContractManagerV2().contractManagerId("CM-001")),
            PageRequest.of(0, 100),
            1);

    when(contractManagerService.getContractManagers(
            providerGuid.toString(), providerOfficeLinkGuid.toString(), PageRequest.of(0, 100)))
        .thenReturn(mockManagers);

    mockMvc
        .perform(
            get(
                    "/provider-firms/{providerFirmId}/offices/{officeId}/contract-managers",
                    providerGuid,
                    providerOfficeLinkGuid)
                .contentType("application/json"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.content[0].contractManagerId").value("CM-001"))
        .andExpect(jsonPath("$.data.metadata.pagination.currentPage").value(0))
        .andExpect(jsonPath("$.data.metadata.pagination.pageSize").value(100))
        .andExpect(jsonPath("$.data.metadata.pagination.totalItems").value(1));
  }

  @Test
  void getContractManagers_returnsEmptyList_whenNoManagersAssigned() throws Exception {
    when(contractManagerService.getContractManagers("100001", "ACC001", PageRequest.of(0, 100)))
        .thenReturn(Page.empty(PageRequest.of(0, 100)));

    mockMvc
        .perform(
            get(
                    "/provider-firms/{providerFirmId}/offices/{officeId}/contract-managers",
                    "100001",
                    "ACC001")
                .contentType("application/json"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.content").isEmpty());
  }

  @Test
  void getContractManagers_acceptsPaginationParams() throws Exception {
    when(contractManagerService.getContractManagers("100001", "ACC001", PageRequest.of(2, 5)))
        .thenReturn(Page.empty(PageRequest.of(2, 5)));

    mockMvc
        .perform(
            get(
                    "/provider-firms/{providerFirmId}/offices/{officeId}/contract-managers",
                    "100001",
                    "ACC001")
                .param("page", "2")
                .param("pageSize", "5")
                .contentType("application/json"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.metadata.pagination.currentPage").value(2))
        .andExpect(jsonPath("$.data.metadata.pagination.pageSize").value(5));
  }

  @Test
  void getContractManagers_returns400_whenPageIsNegative() throws Exception {
    mockMvc
        .perform(
            get(
                    "/provider-firms/{providerFirmId}/offices/{officeId}/contract-managers",
                    "100001",
                    "ACC001")
                .param("page", "-1")
                .contentType("application/json"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void getContractManagers_returns400_whenPageSizeIsZero() throws Exception {
    mockMvc
        .perform(
            get(
                    "/provider-firms/{providerFirmId}/offices/{officeId}/contract-managers",
                    "100001",
                    "ACC001")
                .param("pageSize", "0")
                .contentType("application/json"))
        .andExpect(status().isBadRequest());
  }
}
