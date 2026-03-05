package uk.gov.justice.laa.providerdata.controller;

import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import uk.gov.justice.laa.providerdata.exception.GlobalExceptionHandler;
import uk.gov.justice.laa.providerdata.service.OfficeService;

/**
 * Web layer tests for {@link ProviderFirmOfficesController}.
 *
 * <p>Uses {@code MockMvcBuilders.standaloneSetup} because {@code @WebMvcTest} was removed in Spring
 * Boot 4.0.
 *
 * <p>Note: a full 201 happy-path test is not included here because {@code
 * LSPOfficeLiaisonManagerCreateOrLinkV2} is an untagged interface with no {@code @JsonSubTypes}
 * annotation, so Jackson cannot deserialise a value for the required {@code liaisonManager} field.
 * The happy path is covered by {@link uk.gov.justice.laa.providerdata.service.OfficeServiceTest}.
 */
class ProviderFirmOfficesControllerTest {

  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    OfficeService officeService = mock(OfficeService.class);
    mockMvc =
        MockMvcBuilders.standaloneSetup(new ProviderFirmOfficesController(officeService))
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();
  }

  @Test
  void createProviderFirmOffice_returnsBadRequest_whenBodyIsEmpty() throws Exception {
    mockMvc
        .perform(
            post("/provider-firms/{id}/offices", "FRM001")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void createProviderFirmOffice_returnsBadRequest_whenAddressIsMissing() throws Exception {
    mockMvc
        .perform(
            post("/provider-firms/{id}/offices", "FRM001")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "payment": {"paymentMethod": "EFT"}
                    }
                    """))
        .andExpect(status().isBadRequest());
  }

  @Test
  void createProviderFirmOffice_returnsBadRequest_whenPaymentIsMissing() throws Exception {
    mockMvc
        .perform(
            post("/provider-firms/{id}/offices", "FRM001")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "address": {
                        "line1": "1 Test Street",
                        "townOrCity": "London",
                        "postcode": "SW1A 1AA"
                      }
                    }
                    """))
        .andExpect(status().isBadRequest());
  }

  @Test
  void getProviderFirmOffices_returnsNotImplemented() throws Exception {
    mockMvc
        .perform(get("/provider-firms/{id}/offices", "FRM001"))
        .andExpect(status().isNotImplemented());
  }
}
