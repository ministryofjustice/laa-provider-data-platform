package uk.gov.justice.laa.providerdata.controller;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import uk.gov.justice.laa.providerdata.entity.LspProviderOfficeLinkEntity;
import uk.gov.justice.laa.providerdata.exception.GlobalExceptionHandler;
import uk.gov.justice.laa.providerdata.exception.ItemNotFoundException;
import uk.gov.justice.laa.providerdata.mapper.OfficeMapper;
import uk.gov.justice.laa.providerdata.model.OfficeV2;
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
  private OfficeService officeService;
  private OfficeMapper officeMapper;

  @BeforeEach
  void setUp() {
    officeService = mock(OfficeService.class);
    officeMapper = mock(OfficeMapper.class);
    mockMvc =
        MockMvcBuilders.standaloneSetup(
                new ProviderFirmOfficesController(officeService, officeMapper))
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
  void getProviderFirmOffices_returnsOk() throws Exception {
    when(officeService.getOffices("FRM001", PageRequest.of(0, 100))).thenReturn(Page.empty());

    mockMvc.perform(get("/provider-firms/{id}/offices", "FRM001")).andExpect(status().isOk());
  }

  @Test
  void getProviderFirmOffices_returnsNotFound_whenProviderMissing() throws Exception {
    when(officeService.getOffices("UNKNOWN", PageRequest.of(0, 100)))
        .thenThrow(new ItemNotFoundException("Provider not found: UNKNOWN"));

    mockMvc
        .perform(get("/provider-firms/{id}/offices", "UNKNOWN"))
        .andExpect(status().isNotFound());
  }

  @Test
  void getProviderFirmOfficeByGUID_returnsOk() throws Exception {
    LspProviderOfficeLinkEntity link = new LspProviderOfficeLinkEntity();
    when(officeService.getLspOffice("FRM001", "ABC123")).thenReturn(link);
    when(officeMapper.toLspOfficeV2(link)).thenReturn(new OfficeV2());

    mockMvc
        .perform(get("/provider-firms/{id}/offices/{officeId}", "FRM001", "ABC123"))
        .andExpect(status().isOk());
  }

  @Test
  void getProviderFirmOfficeByGUID_returnsNotFound_whenOfficeMissing() throws Exception {
    when(officeService.getLspOffice("FRM001", "NOTEXIST"))
        .thenThrow(new ItemNotFoundException("Office not found: NOTEXIST"));

    mockMvc
        .perform(get("/provider-firms/{id}/offices/{officeId}", "FRM001", "NOTEXIST"))
        .andExpect(status().isNotFound());
  }

  @Test
  void getProviderFirmOffices_returnsBadRequest_whenPageNegative() throws Exception {
    mockMvc
        .perform(get("/provider-firms/{id}/offices?page=-1", "FRM001"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void getProviderFirmOffices_returnsBadRequest_whenPageSizeZero() throws Exception {
    mockMvc
        .perform(get("/provider-firms/{id}/offices?pageSize=0", "FRM001"))
        .andExpect(status().isBadRequest());
  }
}
