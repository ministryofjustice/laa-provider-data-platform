package uk.gov.justice.laa.providerdata.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
// Explicit matchers instead of wildcard:
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import uk.gov.justice.laa.providerdata.api.model.OfficeLiaisonManagerCreateRequest;
import uk.gov.justice.laa.providerdata.entity.LiaisonManagerEntity;
import uk.gov.justice.laa.providerdata.exception.GlobalExceptionHandler;
import uk.gov.justice.laa.providerdata.service.OfficeLiaisonManagerService;

/**
 * Web layer tests for {@link ProviderFirmOfficesLiaisonManagersController}.
 *
 * <p>Uses {@code MockMvcBuilders.standaloneSetup} to avoid loading the full Spring context,
 * matching the pattern used in {@link ProviderFirmOfficesControllerTest}.
 */
class ProviderFirmOfficesLiaisonManagersControllerTest {

  private MockMvc mockMvc;
  private OfficeLiaisonManagerService service;

  @BeforeEach
  void setUp() {
    service = mock(OfficeLiaisonManagerService.class);
    mockMvc =
        MockMvcBuilders.standaloneSetup(new ProviderFirmOfficesLiaisonManagersController(service))
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();
  }

  @Test
  void postOfficeLiaisonManagers_returnsBadRequest_whenBodyIsEmpty() throws Exception {
    mockMvc
        .perform(
            post(
                    "/provider-firms/{providerFirmGUIDorFirmNumber}"
                        + "/offices/{officeGUIDorCode}/liaison-managers",
                    "FRM001",
                    "0Q731M")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void postOfficeLiaisonManagers_returnsBadRequest_whenExactlyOneNotProvided() throws Exception {
    // Violates @AssertTrue in OfficeLiaisonManagerPostRequest (all three are null)
    String invalidJson =
        """
                {
                  "create": null,
                  "linkHeadOffice": null,
                  "linkChambers": null
                }
                """;

    mockMvc
        .perform(
            post(
                    "/provider-firms/{providerFirmGUIDorFirmNumber}"
                        + "/offices/{officeGUIDorCode}/liaison-managers",
                    "FRM001",
                    "0Q731M")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidJson))
        .andExpect(status().isBadRequest());
  }

  @Test
  void postOfficeLiaisonManagers_returnsOk_withResponseBody() throws Exception {
    // Arrange: mock service to return one LiaisonManagerEntity
    var lm = new LiaisonManagerEntity();
    lm.setGuid(UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee"));
    lm.setFirstName("Alice");
    lm.setLastName("Jones");
    lm.setEmailAddress("alice@example.com");
    lm.setTelephoneNumber("0123456789");

    given(
            service.postOfficeLiaisonManager(
                eq("FRM100"), eq("0Q731M"), any(OfficeLiaisonManagerCreateRequest.class)))
        .willReturn(List.of(lm));

    // Valid "create" request (camelCase keys)
    String validJson =
        """
                {
                  "create": {
                    "firstName": "Alice",
                    "lastName": "Jones",
                    "emailAddress": "alice@example.com",
                    "telephoneNumber": "0123456789"
                  }
                }
                """;

    mockMvc
        .perform(
            post(
                    "/provider-firms/{providerFirmGUIDorFirmNumber}"
                        + "/offices/{officeGUIDorCode}/liaison-managers",
                    "FRM100",
                    "0Q731M")
                .contentType(MediaType.APPLICATION_JSON)
                .content(validJson))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        // The controller response has a top-level 'data' array
        .andExpect(jsonPath("$.data[0].guid").value("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee"))
        .andExpect(jsonPath("$.data[0].firstName").value("Alice"))
        .andExpect(jsonPath("$.data[0].lastName").value("Jones"))
        .andExpect(jsonPath("$.data[0].emailAddress").value("alice@example.com"))
        .andExpect(jsonPath("$.data[0].telephoneNumber").value("0123456789"));

    // Verify delegation to the service with route variables preserved
    verify(service)
        .postOfficeLiaisonManager(
            eq("FRM100"), eq("0Q731M"), any(OfficeLiaisonManagerCreateRequest.class));
  }
}
