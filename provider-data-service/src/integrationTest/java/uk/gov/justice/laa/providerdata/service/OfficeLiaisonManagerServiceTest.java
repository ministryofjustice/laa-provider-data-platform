package uk.gov.justice.laa.providerdata.service;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import uk.gov.justice.laa.providerdata.PostgresqlSpringBootTest;

@Transactional
class OfficeLiaisonManagerServiceTest extends PostgresqlSpringBootTest {

  @Autowired private WebApplicationContext context;
  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
  }

  @Test
  void post_createsLiaisonManager_and_linksToOffice_byOfficeCode() throws Exception {
    String createFirmResponse =
        mockMvc
            .perform(
                post("/provider-firms")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(lspFirmJson("LM Service Test LSP", "11111111")))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();

    String providerFirmGuid = JsonPath.read(createFirmResponse, "$.data.providerFirmGUID");
    String providerFirmNumber = JsonPath.read(createFirmResponse, "$.data.providerFirmNumber");
    String officeCode = headOfficeCode(providerFirmGuid);

    mockMvc
        .perform(
            post(
                    "/provider-firms/{id}/offices/{officeCode}/liaison-managers",
                    providerFirmGuid,
                    officeCode)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "firstName": "Alice",
                      "lastName": "Jones",
                      "emailAddress": "alice@example.com",
                      "telephoneNumber": "0123456789"
                    }
                    """))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.data.providerFirmGUID").isNotEmpty())
        .andExpect(jsonPath("$.data.providerFirmNumber").value(providerFirmNumber))
        .andExpect(jsonPath("$.data.officeCode").value(officeCode))
        .andExpect(jsonPath("$.data.liaisonManagerGUID").isNotEmpty());
  }

  @Test
  void post_linksExistingLiaisonManager_byGuid() throws Exception {
    // Create the first LSP firm and POST a new liaison manager to its head office to get a GUID.
    String firm1Response =
        mockMvc
            .perform(
                post("/provider-firms")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(lspFirmJson("LM Source LSP", "11111111")))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
    String firm1Guid = JsonPath.read(firm1Response, "$.data.providerFirmGUID");
    String firm1OfficeCode = headOfficeCode(firm1Guid);

    String createLmResponse =
        mockMvc
            .perform(
                post(
                        "/provider-firms/{id}/offices/{officeCode}/liaison-managers",
                        firm1Guid,
                        firm1OfficeCode)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {
                          "firstName": "Bob",
                          "lastName": "Smith",
                          "emailAddress": "bob@example.com",
                          "telephoneNumber": "0987654321"
                        }
                        """))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
    String lmGuid = JsonPath.read(createLmResponse, "$.data.liaisonManagerGUID");

    // Create a second LSP firm and link the existing liaison manager to its head office.
    String firm2Response =
        mockMvc
            .perform(
                post("/provider-firms")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(lspFirmJson("LM Target LSP", "22222222")))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
    String firm2Guid = JsonPath.read(firm2Response, "$.data.providerFirmGUID");
    String firm2Number = JsonPath.read(firm2Response, "$.data.providerFirmNumber");
    String firm2OfficeCode = headOfficeCode(firm2Guid);

    mockMvc
        .perform(
            post(
                    "/provider-firms/{id}/offices/{officeCode}/liaison-managers",
                    firm2Guid,
                    firm2OfficeCode)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "liaisonManagerGUID": "%s"
                    }
                    """
                        .formatted(lmGuid)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.data.liaisonManagerGUID").value(lmGuid))
        .andExpect(jsonPath("$.data.providerFirmNumber").value(firm2Number));
  }

  private String headOfficeCode(String providerFirmGuid) throws Exception {
    String officesResponse =
        mockMvc
            .perform(get("/provider-firms/{id}/offices", providerFirmGuid))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    return JsonPath.read(officesResponse, "$.data.content[0].accountNumber");
  }

  private String lspFirmJson(String name, String bankAccountNumber) {
    return """
        {
          "firmType": "Legal Services Provider",
          "name": "%s",
          "legalServicesProvider": {
            "constitutionalStatus": "Partnership",
            "address": {
              "line1": "1 Test Street",
              "townOrCity": "London",
              "postcode": "SW1A 1AA"
            },
            "payment": {
              "paymentMethod": "EFT",
              "bankAccountDetails": {
                "accountName": "Test Account",
                "sortCode": "12-34-56",
                "accountNumber": "%s"
              }
            },
            "liaisonManager": {
              "firstName": "Initial",
              "lastName": "Manager",
              "emailAddress": "initial@example.com",
              "telephoneNumber": "020 1111 2222"
            },
            "contractManager": {
              "contractManagerGUID": "12345678-1234-1234-1234-123456789012"
            }
          }
        }
        """
        .formatted(name, bankAccountNumber);
  }
}
