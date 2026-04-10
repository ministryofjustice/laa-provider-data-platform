package uk.gov.justice.laa.providerdata.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.justice.laa.providerdata.entity.FirmType;
import uk.gov.justice.laa.providerdata.entity.ProviderEntity;
import uk.gov.justice.laa.providerdata.exception.ItemNotFoundException;
import uk.gov.justice.laa.providerdata.mapper.BankAccountMapper;
import uk.gov.justice.laa.providerdata.service.BankDetailsService;
import uk.gov.justice.laa.providerdata.service.OfficeService;
import uk.gov.justice.laa.providerdata.service.ProviderService;

/** Web layer tests for {@link ProviderFirmBankAccountsController}. */
@WebMvcTest(ProviderFirmBankAccountsController.class)
class ProviderFirmBankAccountsControllerTest {

  @Autowired private MockMvc mockMvc;
  @MockitoBean private ProviderService providerService;
  @MockitoBean private OfficeService officeService;
  @MockitoBean private BankDetailsService bankDetailsService;
  @MockitoBean private BankAccountMapper bankAccountMapper;

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
        .andExpect(jsonPath("$.data.metadata.pagination.totalItems").value(0))
        .andExpect(jsonPath("$.data.metadata.searchCriteria.criteria").isEmpty());
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

  @Test
  void getProviderFirmBankAccounts_withBankAccountNumberFilter_echoesFilterInSearchCriteria()
      throws Exception {
    UUID guid = UUID.randomUUID();
    when(providerService.getProvider(guid.toString())).thenReturn(ProviderEntity.builder().build());
    when(bankDetailsService.getProviderBankAccounts(any(), eq("12345"), any()))
        .thenReturn(Page.empty());

    mockMvc
        .perform(get("/provider-firms/{id}/bank-details", guid).param("bankAccountNumber", "12345"))
        .andExpect(status().isOk())
        .andExpect(
            jsonPath("$.data.metadata.searchCriteria.criteria[0].filter")
                .value("bankAccountNumber"))
        .andExpect(jsonPath("$.data.metadata.searchCriteria.criteria[0].values[0]").value("12345"));
  }

  // --- GET /provider-firms/{id}/offices/{officeId}/bank-details ---

  @Test
  void getProviderFirmOfficeBankAccounts_returnsOkWithEmptyPage() throws Exception {
    UUID guid = UUID.randomUUID();
    UUID officeGuid = UUID.randomUUID();
    ProviderEntity provider = ProviderEntity.builder().build();
    when(providerService.getProvider(guid.toString())).thenReturn(provider);
    when(officeService.getProviderOfficeLink(eq(provider), eq(officeGuid.toString())))
        .thenReturn(null);
    when(bankDetailsService.getOfficeBankAccounts(isNull(), isNull(), any()))
        .thenReturn(Page.empty());

    mockMvc
        .perform(get("/provider-firms/{id}/offices/{officeId}/bank-details", guid, officeGuid))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.content").isArray())
        .andExpect(jsonPath("$.data.metadata.pagination.totalItems").value(0))
        .andExpect(jsonPath("$.data.metadata.searchCriteria.criteria").isEmpty());
  }

  @Test
  void getProviderFirmOfficeBankAccounts_returnsNotFound_whenProviderOrOfficeMissing()
      throws Exception {
    UUID guid = UUID.randomUUID();
    UUID officeGuid = UUID.randomUUID();
    ProviderEntity provider = ProviderEntity.builder().build();
    when(providerService.getProvider(guid.toString())).thenReturn(provider);
    when(officeService.getProviderOfficeLink(provider, officeGuid.toString()))
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
    ProviderEntity provider = ProviderEntity.builder().build();
    when(providerService.getProvider(guid.toString())).thenReturn(provider);
    when(officeService.getProviderOfficeLink(eq(provider), eq(officeGuid.toString())))
        .thenReturn(null);

    mockMvc
        .perform(
            get("/provider-firms/{id}/offices/{officeId}/bank-details", guid, officeGuid)
                .param("pageSize", "0"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void getProviderFirmOfficeBankAccounts_returnsOk_forAnyFirmType() throws Exception {
    UUID guid = UUID.randomUUID();
    UUID officeGuid = UUID.randomUUID();
    ProviderEntity provider = ProviderEntity.builder().firmType(FirmType.CHAMBERS).build();
    when(providerService.getProvider(guid.toString())).thenReturn(provider);
    when(officeService.getProviderOfficeLink(eq(provider), eq(officeGuid.toString())))
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
  void getProviderFirmBankAccounts_returnsOk_whenProviderIsChambers() throws Exception {
    UUID guid = UUID.randomUUID();
    when(providerService.getProvider(guid.toString()))
        .thenReturn(ProviderEntity.builder().firmType(FirmType.CHAMBERS).build());
    when(bankDetailsService.getProviderBankAccounts(any(), isNull(), any()))
        .thenReturn(Page.empty());

    mockMvc
        .perform(get("/provider-firms/{id}/bank-details", guid))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.content").isArray());
  }
}
