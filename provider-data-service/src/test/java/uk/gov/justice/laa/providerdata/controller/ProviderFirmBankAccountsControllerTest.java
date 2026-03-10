package uk.gov.justice.laa.providerdata.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import uk.gov.justice.laa.providerdata.entity.FirmType;
import uk.gov.justice.laa.providerdata.entity.ProviderEntity;
import uk.gov.justice.laa.providerdata.exception.GlobalExceptionHandler;
import uk.gov.justice.laa.providerdata.exception.ItemNotFoundException;
import uk.gov.justice.laa.providerdata.mapper.BankAccountMapper;
import uk.gov.justice.laa.providerdata.service.BankDetailsService;
import uk.gov.justice.laa.providerdata.service.OfficeService;
import uk.gov.justice.laa.providerdata.service.ProviderService;

/**
 * Web layer tests for {@link ProviderFirmBankAccountsController}.
 *
 * <p>Uses {@code MockMvcBuilders.standaloneSetup} because {@code @WebMvcTest} was removed in Spring
 * Boot 4.0.
 */
class ProviderFirmBankAccountsControllerTest {

  private MockMvc mockMvc;
  private ProviderService providerService;
  private OfficeService officeService;
  private BankDetailsService bankDetailsService;
  private BankAccountMapper bankAccountMapper;

  @BeforeEach
  void setUp() {
    providerService = mock(ProviderService.class);
    officeService = mock(OfficeService.class);
    bankDetailsService = mock(BankDetailsService.class);
    bankAccountMapper = mock(BankAccountMapper.class);
    mockMvc =
        MockMvcBuilders.standaloneSetup(
                new ProviderFirmBankAccountsController(
                    providerService, officeService, bankDetailsService, bankAccountMapper))
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();
  }

  // --- GET /provider-firms/{id}/bank-details ---

  @Test
  void getProviderFirmBankAccounts_returnsOkWithEmptyPage() throws Exception {
    UUID guid = UUID.randomUUID();
    when(providerService.getProvider(guid.toString())).thenReturn(ProviderEntity.builder().build());
    when(bankDetailsService.getProviderBankAccounts(any(), isNull(), any()))
        .thenReturn(Page.empty());

    mockMvc
        .perform(get("/provider-firms/{id}/bank-details", guid))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.content").isArray())
        .andExpect(jsonPath("$.data.metadata.pagination.totalItems").value(0));
  }

  @Test
  void getProviderFirmBankAccounts_returnsNotFound_whenProviderMissing() throws Exception {
    UUID guid = UUID.randomUUID();
    when(providerService.getProvider(guid.toString()))
        .thenThrow(new ItemNotFoundException("Provider not found: " + guid));

    mockMvc
        .perform(get("/provider-firms/{id}/bank-details", guid))
        .andExpect(status().isNotFound());
  }

  @Test
  void getProviderFirmBankAccounts_returnsBadRequest_whenPageIsNegative() throws Exception {
    mockMvc
        .perform(get("/provider-firms/{id}/bank-details", UUID.randomUUID()).param("page", "-1"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void getProviderFirmBankAccounts_returnsBadRequest_whenPageSizeIsZero() throws Exception {
    UUID guid = UUID.randomUUID();
    when(providerService.getProvider(guid.toString())).thenReturn(ProviderEntity.builder().build());

    mockMvc
        .perform(get("/provider-firms/{id}/bank-details", guid).param("pageSize", "0"))
        .andExpect(status().isBadRequest());
  }

  // --- GET /provider-firms/{id}/offices/{officeId}/bank-details ---

  @Test
  void getProviderFirmOfficeBankAccounts_returnsOkWithEmptyPage() throws Exception {
    UUID guid = UUID.randomUUID();
    UUID officeGuid = UUID.randomUUID();
    when(officeService.getLspOffice(eq(guid.toString()), eq(officeGuid.toString())))
        .thenReturn(null);
    when(bankDetailsService.getOfficeBankAccounts(isNull(), isNull(), any()))
        .thenReturn(Page.empty());

    mockMvc
        .perform(get("/provider-firms/{id}/offices/{officeId}/bank-details", guid, officeGuid))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.content").isArray())
        .andExpect(jsonPath("$.data.metadata.pagination.totalItems").value(0));
  }

  @Test
  void getProviderFirmOfficeBankAccounts_returnsNotFound_whenProviderOrOfficeMissing()
      throws Exception {
    UUID guid = UUID.randomUUID();
    UUID officeGuid = UUID.randomUUID();
    when(officeService.getLspOffice(guid.toString(), officeGuid.toString()))
        .thenThrow(new ItemNotFoundException("Office not found: " + officeGuid));

    mockMvc
        .perform(get("/provider-firms/{id}/offices/{officeId}/bank-details", guid, officeGuid))
        .andExpect(status().isNotFound());
  }

  @Test
  void getProviderFirmOfficeBankAccounts_returnsBadRequest_whenPageIsNegative() throws Exception {
    mockMvc
        .perform(
            get(
                    "/provider-firms/{id}/offices/{officeId}/bank-details",
                    UUID.randomUUID(),
                    UUID.randomUUID())
                .param("page", "-1"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void getProviderFirmOfficeBankAccounts_returnsBadRequest_whenPageSizeIsZero() throws Exception {
    UUID guid = UUID.randomUUID();
    UUID officeGuid = UUID.randomUUID();
    when(officeService.getLspOffice(eq(guid.toString()), eq(officeGuid.toString())))
        .thenReturn(null);

    mockMvc
        .perform(
            get("/provider-firms/{id}/offices/{officeId}/bank-details", guid, officeGuid)
                .param("pageSize", "0"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void getProviderFirmBankAccounts_returnsBadRequest_whenProviderIsChambers() throws Exception {
    UUID guid = UUID.randomUUID();
    when(providerService.getProvider(guid.toString()))
        .thenReturn(ProviderEntity.builder().firmType(FirmType.CHAMBERS).build());

    mockMvc
        .perform(get("/provider-firms/{id}/bank-details", guid))
        .andExpect(status().isBadRequest());
  }
}
