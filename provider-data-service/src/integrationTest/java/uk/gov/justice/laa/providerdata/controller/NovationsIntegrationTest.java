package uk.gov.justice.laa.providerdata.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import uk.gov.justice.laa.providerdata.PostgresqlSpringBootTest;
import uk.gov.justice.laa.providerdata.entity.NovationEntity;
import uk.gov.justice.laa.providerdata.entity.NovationLinkEntity;
import uk.gov.justice.laa.providerdata.repository.NovationLinkRepository;
import uk.gov.justice.laa.providerdata.repository.NovationRepository;

/**
 * Integration tests for {@code POST /novations} (DSTEW-1975).
 *
 * <p>Uses a full Spring context with a PostgreSQL Testcontainers database so that the {@code
 * V5__create_novation_tables.sql} migration, the {@code NovationEntity}/{@code NovationLinkEntity}
 * mappings and their database-level CHECK constraints are all exercised, not just mocked repository
 * behaviour. Each test method runs inside the test-managed transaction, which is rolled back after
 * the method completes so setup data does not persist between tests.
 */
@Transactional
class NovationsIntegrationTest extends PostgresqlSpringBootTest {

  @Autowired private WebApplicationContext context;
  @Autowired private NovationRepository novationRepository;
  @Autowired private NovationLinkRepository novationLinkRepository;

  private MockMvc mockMvc;
  private String previousProviderFirmGuid;
  private String newProviderFirmGuid;
  private String previousOfficeGuid;
  private String newOfficeGuid;

  @BeforeEach
  void setUp() throws Exception {
    mockMvc = MockMvcBuilders.webAppContextSetup(context).build();

    previousProviderFirmGuid = createLspFirm("Integration Test Predecessor LSP");
    newProviderFirmGuid = createLspFirm("Integration Test Successor LSP");

    previousOfficeGuid = getHeadOfficeGuid(previousProviderFirmGuid);
    newOfficeGuid = getHeadOfficeGuid(newProviderFirmGuid);
  }

  private String createLspFirm(String firmName) throws Exception {
    var result =
        mockMvc
            .perform(
                post("/provider-firms")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
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
                                  "paymentMethod": "CHECK"
                                },
                                "liaisonManager": {
                                  "firstName": "Test",
                                  "lastName": "Manager",
                                  "emailAddress": "test.manager@example.com",
                                  "telephoneNumber": "020 1111 2222"
                                },
                                "contractManager": {
                                  "useDefaultContractManager": true
                                }
                              }
                            }
                            """
                            .formatted(firmName)))
            .andExpect(status().isCreated())
            .andReturn();
    return JsonPath.read(result.getResponse().getContentAsString(), "$.data.providerFirmGUID");
  }

  private String getHeadOfficeGuid(String providerFirmGuid) throws Exception {
    String getOfficesResponse =
        mockMvc
            .perform(get("/provider-firms/{id}/offices", providerFirmGuid))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    return JsonPath.read(getOfficesResponse, "$.data.content[0].guid");
  }

  @Test
  void createNovation_persistsNovationAndRelationship_returnsGeneratedIdentifiers()
      throws Exception {
    String requestBody =
        """
        {
          "novationType": "Merger",
          "novationEffectiveDate": "2026-01-01",
          "notes": "Integration test novation",
          "relationships": [
            {
              "previousProviderFirmGUID": "%s",
              "newProviderFirmGUID": "%s",
              "notes": "Integration test relationship"
            }
          ]
        }
        """
            .formatted(previousProviderFirmGuid, newProviderFirmGuid);

    var result =
        mockMvc
            .perform(
                post("/novations").contentType(MediaType.APPLICATION_JSON).content(requestBody))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.data.novationGUID").isNotEmpty())
            .andExpect(jsonPath("$.data.novationRelationshipGUIDs.length()").value(1))
            .andReturn();

    String novationGuid =
        JsonPath.read(result.getResponse().getContentAsString(), "$.data.novationGUID");
    String relationshipGuid =
        JsonPath.read(
            result.getResponse().getContentAsString(), "$.data.novationRelationshipGUIDs[0]");

    NovationEntity savedNovation =
        novationRepository.findById(UUID.fromString(novationGuid)).orElseThrow();
    assertThat(savedNovation.getNovationType()).isEqualTo("Merger");
    assertThat(savedNovation.getNotes()).isEqualTo("Integration test novation");
    assertThat(savedNovation.getNovationStatus()).isNull();
    assertThat(savedNovation.getDecisionBy()).isNull();

    NovationLinkEntity savedLink =
        novationLinkRepository.findById(UUID.fromString(relationshipGuid)).orElseThrow();
    assertThat(savedLink.getNovation().getGuid()).isEqualTo(savedNovation.getGuid());
    assertThat(savedLink.getPreviousProvider().getGuid())
        .isEqualTo(UUID.fromString(previousProviderFirmGuid));
    assertThat(savedLink.getNewProvider().getGuid())
        .isEqualTo(UUID.fromString(newProviderFirmGuid));
    assertThat(savedLink.getNotes()).isEqualTo("Integration test relationship");
  }

  @Test
  void getNovation_returnsPersistedNovationAndRelationship_afterCreation() throws Exception {
    String createRequestBody =
        """
        {
          "novationType": "Merger",
          "novationEffectiveDate": "2026-01-01",
          "novationStatus": "Approved with Conditions",
          "decisionDate": "2026-02-01",
          "decisionReason": "Board approval required before transfer",
          "driverForNovation": "Organisational restructure",
          "notes": "Integration test novation",
          "relationships": [
            {
              "previousProviderFirmGUID": "%s",
              "newProviderFirmGUID": "%s",
              "previousOfficeGUID": "%s",
              "newOfficeGUID": "%s",
              "notes": "Integration test relationship"
            }
          ]
        }
        """
            .formatted(
                previousProviderFirmGuid, newProviderFirmGuid, previousOfficeGuid, newOfficeGuid);

    var createResult =
        mockMvc
            .perform(
                post("/novations")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(createRequestBody))
            .andExpect(status().isCreated())
            .andReturn();
    String novationGuid =
        JsonPath.read(createResult.getResponse().getContentAsString(), "$.data.novationGUID");

    // AC2: the relationship recorded on creation can be retrieved for operational, reporting and
    // audit purposes.
    mockMvc
        .perform(get("/novations/{novationGUID}", novationGuid))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.guid").value(novationGuid))
        .andExpect(jsonPath("$.data.novationType").value("Merger"))
        .andExpect(jsonPath("$.data.novationEffectiveDate").value("2026-01-01"))
        .andExpect(jsonPath("$.data.novationStatus").value("Approved with Conditions"))
        .andExpect(jsonPath("$.data.decisionDate").value("2026-02-01"))
        .andExpect(
            jsonPath("$.data.decisionReason").value("Board approval required before transfer"))
        .andExpect(jsonPath("$.data.driverForNovation").value("Organisational restructure"))
        .andExpect(jsonPath("$.data.notes").value("Integration test novation"))
        .andExpect(jsonPath("$.data.relationships.length()").value(1))
        .andExpect(
            jsonPath("$.data.relationships[0].previousProviderFirmGUID")
                .value(previousProviderFirmGuid))
        .andExpect(
            jsonPath("$.data.relationships[0].newProviderFirmGUID").value(newProviderFirmGuid))
        .andExpect(jsonPath("$.data.relationships[0].previousOfficeGUID").value(previousOfficeGuid))
        .andExpect(jsonPath("$.data.relationships[0].newOfficeGUID").value(newOfficeGuid))
        .andExpect(
            jsonPath("$.data.relationships[0].notes").value("Integration test relationship"));
  }

  @Test
  void getNovation_doesNotCreateAmendOrDeleteNovationData() throws Exception {
    String createRequestBody =
        """
        {
          "novationType": "Merger",
          "novationEffectiveDate": "2026-01-01",
          "relationships": [
            {
              "previousProviderFirmGUID": "%s",
              "newProviderFirmGUID": "%s"
            }
          ]
        }
        """
            .formatted(previousProviderFirmGuid, newProviderFirmGuid);

    var createResult =
        mockMvc
            .perform(
                post("/novations")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(createRequestBody))
            .andExpect(status().isCreated())
            .andReturn();
    String novationGuid =
        JsonPath.read(createResult.getResponse().getContentAsString(), "$.data.novationGUID");
    long novationCountBefore = novationRepository.count();
    long relationshipCountBefore = novationLinkRepository.count();

    mockMvc.perform(get("/novations/{novationGUID}", novationGuid)).andExpect(status().isOk());

    assertThat(novationRepository.count()).isEqualTo(novationCountBefore);
    assertThat(novationLinkRepository.count()).isEqualTo(relationshipCountBefore);
  }

  @Test
  void getNovation_unknownNovationGuid_returns404() throws Exception {
    mockMvc
        .perform(get("/novations/{novationGUID}", UUID.randomUUID()))
        .andExpect(status().isNotFound());
  }

  @Test
  void createNovation_withPreviousOfficeGuid_persistsOfficeLink() throws Exception {
    String requestBody =
        """
        {
          "novationType": "Merger",
          "novationEffectiveDate": "2026-01-01",
          "relationships": [
            {
              "previousProviderFirmGUID": "%s",
              "newProviderFirmGUID": "%s",
              "previousOfficeGUID": "%s"
            }
          ]
        }
        """
            .formatted(previousProviderFirmGuid, newProviderFirmGuid, previousOfficeGuid);

    var result =
        mockMvc
            .perform(
                post("/novations").contentType(MediaType.APPLICATION_JSON).content(requestBody))
            .andExpect(status().isCreated())
            .andReturn();

    String relationshipGuid =
        JsonPath.read(
            result.getResponse().getContentAsString(), "$.data.novationRelationshipGUIDs[0]");
    NovationLinkEntity savedLink =
        novationLinkRepository.findById(UUID.fromString(relationshipGuid)).orElseThrow();
    assertThat(savedLink.getPreviousOffice().getGuid())
        .isEqualTo(UUID.fromString(previousOfficeGuid));
  }

  @Test
  void createNovation_approvedStatusWithDecisionDate_persistsDecisionByFromAuditor()
      throws Exception {
    String requestBody =
        """
        {
          "novationType": "Merger",
          "novationEffectiveDate": "2026-01-01",
          "novationStatus": "Approved",
          "decisionDate": "2026-02-01",
          "relationships": [
            {
              "previousProviderFirmGUID": "%s",
              "newProviderFirmGUID": "%s"
            }
          ]
        }
        """
            .formatted(previousProviderFirmGuid, newProviderFirmGuid);

    var result =
        mockMvc
            .perform(
                post("/novations").contentType(MediaType.APPLICATION_JSON).content(requestBody))
            .andExpect(status().isCreated())
            .andReturn();

    String novationGuid =
        JsonPath.read(result.getResponse().getContentAsString(), "$.data.novationGUID");
    NovationEntity savedNovation =
        novationRepository.findById(UUID.fromString(novationGuid)).orElseThrow();
    // Satisfies CK_NOVATION_DECISION_INFORMATION: Approved requires DECISION_DATE and DECISION_BY.
    assertThat(savedNovation.getNovationStatus()).isEqualTo("Approved");
    assertThat(savedNovation.getDecisionBy()).isEqualTo("SYSTEM");
  }

  @Test
  void createNovation_unknownPreviousProvider_returns404() throws Exception {
    String requestBody =
        """
        {
          "novationType": "Merger",
          "novationEffectiveDate": "2026-01-01",
          "relationships": [
            {
              "previousProviderFirmGUID": "%s",
              "newProviderFirmGUID": "%s"
            }
          ]
        }
        """
            .formatted(UUID.randomUUID(), newProviderFirmGuid);

    mockMvc
        .perform(post("/novations").contentType(MediaType.APPLICATION_JSON).content(requestBody))
        .andExpect(status().isNotFound());
  }

  @Test
  void createNovation_approvedWithoutDecisionDate_returns400() throws Exception {
    String requestBody =
        """
        {
          "novationType": "Merger",
          "novationEffectiveDate": "2026-01-01",
          "novationStatus": "Approved",
          "relationships": [
            {
              "previousProviderFirmGUID": "%s",
              "newProviderFirmGUID": "%s"
            }
          ]
        }
        """
            .formatted(previousProviderFirmGuid, newProviderFirmGuid);

    mockMvc
        .perform(post("/novations").contentType(MediaType.APPLICATION_JSON).content(requestBody))
        .andExpect(status().isBadRequest());
  }
}
