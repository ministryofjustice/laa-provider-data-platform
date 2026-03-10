package uk.gov.justice.laa.providerdata.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

/**
 * Integration tests for the bank-details GET endpoints.
 *
 * <p>Uses a full Spring context with an H2 in-memory database and {@code
 * MockMvcBuilders.webAppContextSetup} so that all custom Jackson deserializers and filters are
 * active. Each test method runs inside the test-managed transaction, which is rolled back after the
 * method completes so setup data does not persist between tests.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ProviderFirmBankAccountsIntegrationTest {

  @Autowired private WebApplicationContext context;

  private MockMvc mockMvc;
  private String providerGuid;
  private String secondOfficeGuid;

  @BeforeEach
  void setUp() throws Exception {
    mockMvc = MockMvcBuilders.webAppContextSetup(context).build();

    // Create an LSP firm with EFT payment → head office + bank account.
    String createFirmResponse =
        mockMvc
            .perform(
                post("/provider-firms")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {
                          "firmType": "Legal Services Provider",
                          "name": "Integration Test LSP",
                          "legalServicesProvider": {
                            "address": {
                              "line1": "1 Test Street",
                              "townOrCity": "London",
                              "postcode": "SW1A 1AA"
                            },
                            "payment": {
                              "paymentMethod": "EFT",
                              "bankAccountDetails": {
                                "accountName": "Firm Account",
                                "sortCode": "12-34-56",
                                "accountNumber": "11111111"
                              }
                            }
                          }
                        }
                        """))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();

    providerGuid = JsonPath.read(createFirmResponse, "$.data.providerFirmGUID");

    // Create a second office with EFT payment and a new liaison manager.
    String createOfficeResponse =
        mockMvc
            .perform(
                post("/provider-firms/{id}/offices", providerGuid)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {
                          "address": {
                            "line1": "2 Test Street",
                            "townOrCity": "Manchester",
                            "postcode": "M1 1AA"
                          },
                          "payment": {
                            "paymentMethod": "EFT",
                            "bankAccountDetails": {
                              "accountName": "Office Account",
                              "sortCode": "65-43-21",
                              "accountNumber": "22222222"
                            }
                          },
                          "liaisonManager": {
                            "firstName": "Jane",
                            "lastName": "Smith",
                            "emailAddress": "jane.smith@example.com",
                            "telephoneNumber": "07700900000"
                          }
                        }
                        """))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();

    secondOfficeGuid = JsonPath.read(createOfficeResponse, "$.data.officeGUID");
  }

  @Test
  void getProviderFirmBankAccounts_returns200WithTwoAccounts() throws Exception {
    // Both accounts (head office + second office) are linked to the provider.
    mockMvc
        .perform(get("/provider-firms/{id}/bank-details", providerGuid))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.content").isArray())
        .andExpect(jsonPath("$.data.content.length()").value(2))
        .andExpect(jsonPath("$.data.metadata.pagination.totalItems").value(2));
  }

  @Test
  void getProviderFirmBankAccounts_filterByAccountNumber_returnsMatchingAccount() throws Exception {
    mockMvc
        .perform(
            get("/provider-firms/{id}/bank-details", providerGuid)
                .param("bankAccountNumber", "111"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.content.length()").value(1))
        .andExpect(jsonPath("$.data.content[0].accountNumber").value("11111111"));
  }

  @Test
  void getProviderFirmBankAccounts_filterByAccountNumber_returnsEmptyWhenNoMatch()
      throws Exception {
    mockMvc
        .perform(
            get("/provider-firms/{id}/bank-details", providerGuid)
                .param("bankAccountNumber", "XXXXXX"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.content.length()").value(0))
        .andExpect(jsonPath("$.data.metadata.pagination.totalItems").value(0));
  }

  @Test
  void getProviderFirmBankAccounts_returnsNotFound_whenProviderMissing() throws Exception {
    mockMvc
        .perform(get("/provider-firms/{id}/bank-details", "UNKNOWN-999"))
        .andExpect(status().isNotFound());
  }

  @Test
  void getProviderFirmOfficeBankAccounts_returns200ForSecondOffice() throws Exception {
    mockMvc
        .perform(
            get(
                "/provider-firms/{id}/offices/{officeId}/bank-details",
                providerGuid,
                secondOfficeGuid))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.content").isArray())
        .andExpect(jsonPath("$.data.content.length()").value(1))
        .andExpect(jsonPath("$.data.content[0].accountNumber").value("22222222"))
        .andExpect(jsonPath("$.data.metadata.pagination.totalItems").value(1));
  }

  @Test
  void getProviderFirmBankAccounts_returnsOk_withEmptyList_whenProviderIsChambers()
      throws Exception {
    // Create a Chambers firm — bank account is linked to the Chambers itself.
    String createChambersResponse =
        mockMvc
            .perform(
                post("/provider-firms")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {
                          "firmType": "Chambers",
                          "name": "Integration Test Chambers",
                          "chambers": {
                            "address": {
                              "line1": "3 Inn Lane",
                              "townOrCity": "London",
                              "postcode": "EC4Y 7AA"
                            },
                            "payment": {
                              "paymentMethod": "EFT",
                              "bankAccountDetails": {
                                "accountName": "Chambers Account",
                                "sortCode": "11-22-33",
                                "accountNumber": "33333333"
                              }
                            },
                            "liaisonManager": {
                              "firstName": "Bob",
                              "lastName": "Clarke",
                              "emailAddress": "b.clarke@example.com",
                              "telephoneNumber": "07700900001"
                            }
                          }
                        }
                        """))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();

    String chambersGuid = JsonPath.read(createChambersResponse, "$.data.providerFirmGUID");

    // No Advocates are linked to this Chambers, so the response is empty — but 200, not 400.
    mockMvc
        .perform(get("/provider-firms/{id}/bank-details", chambersGuid))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.content").isArray())
        .andExpect(jsonPath("$.data.content.length()").value(0))
        .andExpect(jsonPath("$.data.metadata.pagination.totalItems").value(0));
  }
}
