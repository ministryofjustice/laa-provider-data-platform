package uk.gov.justice.laa.providerdata.controller;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import uk.gov.justice.laa.providerdata.exception.GlobalExceptionHandler;
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
class ProviderFirmOfficeContractManagersControllerTest {

  private MockMvc mockMvc;
  private OfficeContractManagerAssignmentService assignmentService;

  /**
   * Initializes the test fixture before each test.
   *
   * <ul>
   *   <li>Creates a Mockito mock for {@link OfficeContractManagerAssignmentService}
   *   <li>Builds a standalone {@link MockMvc} instance for the controller
   *   <li>Registers {@link GlobalExceptionHandler} for consistent error translation
   * </ul>
   */
  @BeforeEach
  void setUp() {
    assignmentService = org.mockito.Mockito.mock(OfficeContractManagerAssignmentService.class);
    mockMvc =
        MockMvcBuilders.standaloneSetup(
                new ProviderFirmOfficeContractManagersController(assignmentService))
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();
  }

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
    UUID officeGuid = UUID.randomUUID();
    UUID contractManagerGuid = UUID.randomUUID();

    when(assignmentService.assign(eq(officeGuid), eq(contractManagerGuid)))
        .thenReturn(
            new OfficeContractManagerAssignmentService.AssignmentResult(officeGuid, "CM-001"));

    mockMvc
        .perform(
            post(
                    "/provider-firms/{providerFirmId}/offices/{officeId}/contract-managers",
                    "FRM001",
                    officeGuid)
                .contentType(APPLICATION_JSON)
                .content(
                    """
                                        {
                                          "contractManagerGUID": "%s"
                                        }
                                        """
                        .formatted(contractManagerGuid)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.data.officeGUID").value(officeGuid.toString()))
        .andExpect(jsonPath("$.data.contractManagerId").value("CM-001"));
  }

  /**
   * Ensures HTTP 400 is returned when the office identifier in the path is not a valid UUID.
   *
   * @throws Exception if the request fails to execute
   */
  @Test
  void postContractManagers_returns400_whenOfficeGuidIsNotAuuid() throws Exception {
    mockMvc
        .perform(
            post(
                    "/provider-firms/{providerFirmId}/offices/{officeId}/contract-managers",
                    "FRM001",
                    "NOT-A-UUID")
                .contentType(APPLICATION_JSON)
                .content(
                    """
                                        {
                                          "contractManagerGUID": "00000000"
                                        }
                                        """))
        .andExpect(status().isBadRequest());
  }

  /**
   * Ensures HTTP 400 is returned when the request body is missing the required {@code
   * contractManagerGUID} field.
   *
   * @throws Exception if the request fails to execute
   */
  @Test
  void postContractManagers_returns400_whenBodyMissingContractManagerGuid() throws Exception {
    UUID officeGuid = UUID.randomUUID();

    mockMvc
        .perform(
            post(
                    "/provider-firms/{providerFirmId}/offices/{officeId}/contract-managers",
                    "FRM001",
                    officeGuid)
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
    UUID officeGuid = UUID.randomUUID();

    mockMvc
        .perform(
            post(
                    "/provider-firms/{providerFirmId}/offices/{officeId}/contract-managers",
                    "FRM001",
                    officeGuid)
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
    UUID officeGuid = UUID.randomUUID();

    mockMvc
        .perform(
            post(
                    "/provider-firms/{providerFirmId}/offices/{officeId}/contract-managers",
                    "FRM001",
                    officeGuid)
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

    when(assignmentService.assign(eq(officeGuid), eq(contractManagerGuid)))
        .thenThrow(new RuntimeException("boom"));

    mockMvc
        .perform(
            post(
                    "/provider-firms/{providerFirmId}/offices/{officeId}/contract-managers",
                    "FRM001",
                    officeGuid)
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
}
