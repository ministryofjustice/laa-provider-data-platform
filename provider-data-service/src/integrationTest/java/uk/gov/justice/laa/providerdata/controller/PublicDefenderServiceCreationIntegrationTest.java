package uk.gov.justice.laa.providerdata.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import uk.gov.justice.laa.providerdata.PostgresqlSpringBootTest;
import uk.gov.justice.laa.providerdata.entity.PdsProviderEntity;
import uk.gov.justice.laa.providerdata.entity.PdsProviderOfficeLinkEntity;
import uk.gov.justice.laa.providerdata.repository.PdsProviderOfficeLinkRepository;
import uk.gov.justice.laa.providerdata.repository.ProviderRepository;

/// Verifies complete PDS creation through the Spring MVC, service, JPA and Flyway layers.
@Transactional
class PublicDefenderServiceCreationIntegrationTest extends PostgresqlSpringBootTest {

  @Autowired private WebApplicationContext context;
  @Autowired private ProviderRepository providerRepository;
  @Autowired private PdsProviderOfficeLinkRepository pdsOfficeLinkRepository;

  @Test
  void dstew2024_ac1_validPdsRequest_createsProviderAndHeadOffice() throws Exception {
    MockMvc mockMvc = MockMvcBuilders.webAppContextSetup(context).build();

    String response =
        mockMvc
            .perform(
                post("/provider-firms/public-defender-services")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(validRequest("Integration PDS")))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();

    String providerGuid = JsonPath.read(response, "$.data.providerFirmGUID");
    String firmNumber = JsonPath.read(response, "$.data.providerFirmNumber");
    PdsProviderEntity provider =
        (PdsProviderEntity) providerRepository.findByFirmNumber(firmNumber).orElseThrow();
    PdsProviderOfficeLinkEntity officeLink =
        pdsOfficeLinkRepository
            .findById(
                java.util.UUID.fromString(
                    pdsOfficeLinkRepository.findAll().stream()
                        .filter(
                            link -> link.getProvider().getGuid().toString().equals(providerGuid))
                        .findFirst()
                        .orElseThrow()
                        .getGuid()
                        .toString()))
            .orElseThrow();

    assertThat(provider.getGuid().toString()).isEqualTo(providerGuid);
    assertThat(provider.getFirmNumber()).isEqualTo(firmNumber);
    assertThat(provider.getName()).isEqualTo("Integration PDS");
    assertThat(provider.getConstitutionalStatus()).isEqualTo("Government Funded Organisation");
    assertThat(officeLink.getHeadOfficeFlag()).isTrue();
    assertThat(officeLink.getOffice().getAddressLine1()).isEqualTo("1 Integration Street");
  }

  @Test
  void dstew2025_ac1_validAmendment_updatesPdsProviderAndHeadOffice() throws Exception {
    MockMvc mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
    String response =
        mockMvc
            .perform(
                post("/provider-firms/public-defender-services")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(validRequest("Integration PDS Amendment")))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
    String firmNumber = JsonPath.read(response, "$.data.providerFirmNumber");

    mockMvc
        .perform(
            patch("/provider-firms/{id}", firmNumber)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "name": "Integration PDS Amended",
                      "publicDefenderService": {
                        "companiesHouseNumber": "12345678",
                        "headOffice": {
                          "address": {
                            "line1": "2 Integration Street",
                            "townOrCity": "Birmingham",
                            "postcode": "B2 2BB"
                          }
                        }
                      }
                    }
                    """))
        .andExpect(status().isOk());

    PdsProviderEntity provider =
        (PdsProviderEntity) providerRepository.findByFirmNumber(firmNumber).orElseThrow();
    PdsProviderOfficeLinkEntity officeLink =
        pdsOfficeLinkRepository.findByProviderAndHeadOfficeFlagTrue(provider).orElseThrow();
    assertThat(provider.getName()).isEqualTo("Integration PDS Amended");
    assertThat(provider.getCompaniesHouseNumber()).isEqualTo("12345678");
    assertThat(officeLink.getOffice().getAddressLine1()).isEqualTo("2 Integration Street");
  }

  @Test
  void dstew2024_ac2_missingMandatoryField_returns400AndCreatesNothing() throws Exception {
    MockMvc mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
    String firmName = "Integration PDS Missing Status";

    mockMvc
        .perform(
            post("/provider-firms/public-defender-services")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "name": "%s",
                      "headOffice": {
                        "address": {
                          "line1": "1 Integration Street",
                          "townOrCity": "Birmingham",
                          "postcode": "B1 1AA"
                        }
                      }
                    }
                    """
                        .formatted(firmName)))
        .andExpect(status().isBadRequest());

    assertThat(providerRepository.findAll().stream().noneMatch(p -> firmName.equals(p.getName())))
        .isTrue();
  }

  @Test
  void dstew2024_ac3_unpairedDxFields_returns400() throws Exception {
    MockMvc mockMvc = MockMvcBuilders.webAppContextSetup(context).build();

    mockMvc
        .perform(
            post("/provider-firms/public-defender-services")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    validRequestWithDx("Integration PDS Invalid DX", "\"dxNumber\": \"12345\"")))
        .andExpect(status().isBadRequest());
  }

  @Test
  void dstew2024_ac4_invalidRequest_createsNoProviderOrOffice() throws Exception {
    MockMvc mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
    String firmName = "Integration PDS Atomicity";

    mockMvc
        .perform(
            post("/provider-firms/public-defender-services")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    validRequestWithDx(firmName, "\"dxCentre\": \"INVALID\", \"dxNumber\": null")))
        .andExpect(status().isBadRequest());

    assertThat(providerRepository.findAll().stream().noneMatch(p -> firmName.equals(p.getName())))
        .isTrue();
    assertThat(
            pdsOfficeLinkRepository.findAll().stream()
                .noneMatch(link -> link.getProvider().getName().equals(firmName)))
        .isTrue();
  }

  private static String validRequest(String name) {
    return """
        {
          "name": "%s",
          "constitutionalStatus": "Government Funded Organisation",
          "headOffice": {
            "address": {
              "line1": "1 Integration Street",
              "townOrCity": "Birmingham",
              "postcode": "B1 1AA"
            }
          }
        }
        """
        .formatted(name);
  }

  private static String validRequestWithDx(String name, String dxFields) {
    return """
        {
          "name": "%s",
          "constitutionalStatus": "Government Funded Organisation",
          "headOffice": {
            "address": {
              "line1": "1 Integration Street",
              "townOrCity": "Birmingham",
              "postcode": "B1 1AA"
            },
            "dxDetails": {%s}
          }
        }
        """
        .formatted(name, dxFields);
  }
}
