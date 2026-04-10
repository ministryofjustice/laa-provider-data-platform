package uk.gov.justice.laa.providerdata.controller;

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
import uk.gov.justice.laa.providerdata.model.OfficeContractManagerV2;
import uk.gov.justice.laa.providerdata.service.ContractManagerService;
import uk.gov.justice.laa.providerdata.service.OfficeContractManagerAssignmentService;

/**
 * Unit tests for {@link ProviderFirmOfficeContractManagersController}.
 *
 * <p>This suite exercises the POST endpoint that assigns a contract manager to an office. It uses
 * {@link MockMvc} in standalone mode along with a mocked {@link
 * OfficeContractManagerAssignmentService} and the {@link GlobalExceptionHandler} to validate status
 * codes and response payloads for both success and error conditions.
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
  @MockitoBean private OfficeContractManagerAssignmentService assignmentService;
  @MockitoBean private ContractManagerService contractManagerService;

  /**
   * Verifies a successful POST request.
   *
   * <ul>
   *   <li>returns HTTP 201 (Created)
   *   <li>returns the assigned {@code officeGUID} and {@code contractManagerId} in the response
   *       body
   * </ul>
   *
   * @throws Exception if the request fails to execute
   */
  @Test
  void postContractManagers_returns201_andBodyContainsOfficeGuidAndContractManagerId()
      throws Exception {
    UUID providerOfficeLinkGuid = UUID.randomUUID();
    UUID providerGuid = UUID.randomUUID();
    UUID contractManagerGuid = UUID.randomUUID();

    when(assignmentService.assign(
            eq(providerGuid.toString()),
            eq(providerOfficeLinkGuid.toString()),
            eq(contractManagerGuid)))
        .thenReturn(
            new OfficeContractManagerAssignmentService.AssignmentResult(
                providerOfficeLinkGuid, "CM-001"));

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

  /**
   * Ensures office codes are accepted as path values and are delegated unchanged to the service.
   *
   * @throws Exception if the request fails to execute
   */
  @Test
  void postContractManagers_acceptsOfficeCodePathValue() throws Exception {
    UUID providerOfficeLinkGuid = UUID.randomUUID();
    UUID contractManagerGuid = UUID.randomUUID();

    when(assignmentService.assign(eq("FRM001"), eq("ACC001"), eq(contractManagerGuid)))
        .thenReturn(
            new OfficeContractManagerAssignmentService.AssignmentResult(
                providerOfficeLinkGuid, "CM-001"));

    mockMvc
        .perform(
            post(
                    "/provider-firms/{providerFirmId}/offices/{officeId}/contract-managers",
                    "FRM001",
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

  /**
   * Ensures HTTP 400 is returned when the request body is missing the required {@code
   * contractManagerGUID} field.
   *
   * @throws Exception if the request fails to execute
   */
  @Test
  void postContractManagers_returns400_whenBodyMissingContractManagerGuid() throws Exception {

    mockMvc
        .perform(
            post(
                    "/provider-firms/{providerFirmId}/offices/{officeId}/contract-managers",
                    "FRM001",
                    "ACC001")
                .contentType(APPLICATION_JSON)
                .content("{}"))
        .andExpect(status().isBadRequest());
  }

  /**
   * Ensures HTTP 400 is returned when {@code contractManagerGUID} contains only whitespace.
   *
   * @throws Exception if the request fails to execute
   */
  @Test
  void postContractManagers_returns400_whenContractManagerGuidIsBlank() throws Exception {
    mockMvc
        .perform(
            post(
                    "/provider-firms/{providerFirmId}/offices/{officeId}/contract-managers",
                    "FRM001",
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

  /**
   * Ensures HTTP 400 is returned when the {@code contractManagerGUID} in the request body is not a
   * valid UUID.
   *
   * @throws Exception if the request fails to execute
   */
  @Test
  void postContractManagers_returns400_whenContractManagerGuidIsNotAuuid() throws Exception {
    mockMvc
        .perform(
            post(
                    "/provider-firms/{providerFirmId}/offices/{officeId}/contract-managers",
                    "FRM001",
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

  /**
   * Verifies that an unexpected runtime exception from the service layer results in a 500 Internal
   * Server Error via the global exception handler.
   *
   * @throws Exception if the request fails to execute
   */
  @Test
  void postContractManagers_returns500_whenServiceThrowsUnexpectedException() throws Exception {
    UUID officeGuid = UUID.randomUUID();
    UUID contractManagerGuid = UUID.randomUUID();

    when(assignmentService.assign(eq("FRM001"), eq("ACC001"), eq(contractManagerGuid)))
        .thenThrow(new RuntimeException("boom"));

    mockMvc
        .perform(
            post(
                    "/provider-firms/{providerFirmId}/offices/{officeId}/contract-managers",
                    "FRM001",
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
    when(contractManagerService.getContractManagers("FRM001", "ACC001", PageRequest.of(0, 100)))
        .thenReturn(Page.empty(PageRequest.of(0, 100)));

    mockMvc
        .perform(
            get(
                    "/provider-firms/{providerFirmId}/offices/{officeId}/contract-managers",
                    "FRM001",
                    "ACC001")
                .contentType("application/json"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.content").isEmpty());
  }

  @Test
  void getContractManagers_acceptsPaginationParams() throws Exception {
    when(contractManagerService.getContractManagers("FRM001", "ACC001", PageRequest.of(2, 5)))
        .thenReturn(Page.empty(PageRequest.of(2, 5)));

    mockMvc
        .perform(
            get(
                    "/provider-firms/{providerFirmId}/offices/{officeId}/contract-managers",
                    "FRM001",
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
                    "FRM001",
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
                    "FRM001",
                    "ACC001")
                .param("pageSize", "0")
                .contentType("application/json"))
        .andExpect(status().isBadRequest());
  }
}
