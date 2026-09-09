package uk.gov.justice.laa.providerdata.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.justice.laa.providerdata.config.JacksonConfig;
import uk.gov.justice.laa.providerdata.entity.NovationEntity;
import uk.gov.justice.laa.providerdata.entity.NovationLinkEntity;
import uk.gov.justice.laa.providerdata.entity.ProviderEntity;
import uk.gov.justice.laa.providerdata.exception.ItemNotFoundException;
import uk.gov.justice.laa.providerdata.mapper.NovationMapperImpl;
import uk.gov.justice.laa.providerdata.repository.NovationLinkRepository;
import uk.gov.justice.laa.providerdata.repository.NovationRepository;
import uk.gov.justice.laa.providerdata.service.NovationAmendmentService;
import uk.gov.justice.laa.providerdata.service.NovationCreationResult;
import uk.gov.justice.laa.providerdata.service.NovationCreationService;

@WebMvcTest(NovationsController.class)
@Import({JacksonConfig.class, NovationMapperImpl.class})
class NovationsControllerTest {

  @Autowired private MockMvc mockMvc;
  @MockitoBean private NovationCreationService novationCreationService;
  @MockitoBean private NovationAmendmentService novationAmendmentService;
  @MockitoBean private NovationRepository novationRepository;
  @MockitoBean private NovationLinkRepository novationLinkRepository;

  private static final String VALID_REQUEST_BODY =
      """
      {
        "novationType": "Merger",
        "novationEffectiveDate": "2026-01-01",
        "relationships": [
          {
            "previousProviderFirmGUID": "11111111-1111-1111-1111-111111111111",
            "newProviderFirmGUID": "22222222-2222-2222-2222-222222222222"
          }
        ]
      }
      """;

  @Test
  void createNovation_returns201WithGeneratedIdentifiers() throws Exception {
    UUID novationGuid = UUID.randomUUID();
    UUID relationshipGuid = UUID.randomUUID();
    when(novationCreationService.createNovation(any()))
        .thenReturn(new NovationCreationResult(novationGuid, List.of(relationshipGuid)));

    mockMvc
        .perform(
            post("/novations").contentType(MediaType.APPLICATION_JSON).content(VALID_REQUEST_BODY))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.data.novationGUID").value(novationGuid.toString()))
        .andExpect(
            jsonPath("$.data.novationRelationshipGUIDs[0]").value(relationshipGuid.toString()));
  }

  @Test
  void createNovation_missingMandatoryField_returns400() throws Exception {
    mockMvc
        .perform(
            post("/novations")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "novationEffectiveDate": "2026-01-01",
                      "relationships": [
                        {
                          "previousProviderFirmGUID": "11111111-1111-1111-1111-111111111111",
                          "newProviderFirmGUID": "22222222-2222-2222-2222-222222222222"
                        }
                      ]
                    }
                    """))
        .andExpect(status().isBadRequest());
  }

  @Test
  void createNovation_noRelationships_returns400() throws Exception {
    mockMvc
        .perform(
            post("/novations")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "novationType": "Merger",
                      "novationEffectiveDate": "2026-01-01",
                      "relationships": []
                    }
                    """))
        .andExpect(status().isBadRequest());
  }

  @Test
  void createNovation_rescindedStatus_returns400() throws Exception {
    mockMvc
        .perform(
            post("/novations")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "novationType": "Merger",
                      "novationEffectiveDate": "2026-01-01",
                      "novationStatus": "Rescinded",
                      "relationships": [
                        {
                          "previousProviderFirmGUID": "11111111-1111-1111-1111-111111111111",
                          "newProviderFirmGUID": "22222222-2222-2222-2222-222222222222"
                        }
                      ]
                    }
                    """))
        .andExpect(status().isBadRequest());
  }

  @Test
  void createNovation_unknownReferencedProvider_returns404() throws Exception {
    when(novationCreationService.createNovation(any()))
        .thenThrow(new ItemNotFoundException("Unknown previousProviderFirmGUID: ..."));

    mockMvc
        .perform(
            post("/novations").contentType(MediaType.APPLICATION_JSON).content(VALID_REQUEST_BODY))
        .andExpect(status().isNotFound());
  }

  @Test
  void getNovations_returns501NotImplemented() throws Exception {
    mockMvc.perform(get("/novations")).andExpect(status().isNotImplemented());
  }

  @Test
  void getNovation_returnsPersistedNovationWithRelationships() throws Exception {
    UUID novationGuid = UUID.randomUUID();
    UUID previousProviderGuid = UUID.randomUUID();
    UUID newProviderGuid = UUID.randomUUID();
    UUID relationshipGuid = UUID.randomUUID();

    NovationEntity novation =
        NovationEntity.builder()
            .guid(novationGuid)
            .novationType("Merger")
            .novationEffectiveDate(LocalDate.of(2026, 1, 1))
            .build();
    NovationLinkEntity link =
        NovationLinkEntity.builder()
            .guid(relationshipGuid)
            .novation(novation)
            .previousProvider(ProviderEntity.builder().guid(previousProviderGuid).build())
            .newProvider(ProviderEntity.builder().guid(newProviderGuid).build())
            .build();

    when(novationRepository.findById(novationGuid)).thenReturn(Optional.of(novation));
    when(novationLinkRepository.findByNovationOrderByCreatedTimestampAsc(novation))
        .thenReturn(List.of(link));

    mockMvc
        .perform(get("/novations/{novationGUID}", novationGuid))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.guid").value(novationGuid.toString()))
        .andExpect(jsonPath("$.data.novationType").value("Merger"))
        .andExpect(jsonPath("$.data.relationships[0].guid").value(relationshipGuid.toString()))
        .andExpect(
            jsonPath("$.data.relationships[0].previousProviderFirmGUID")
                .value(previousProviderGuid.toString()))
        .andExpect(
            jsonPath("$.data.relationships[0].newProviderFirmGUID")
                .value(newProviderGuid.toString()));
  }

  @Test
  void getNovation_unknownNovationGuid_returns404() throws Exception {
    UUID novationGuid = UUID.randomUUID();
    when(novationRepository.findById(novationGuid)).thenReturn(Optional.empty());

    mockMvc
        .perform(get("/novations/{novationGUID}", novationGuid))
        .andExpect(status().isNotFound());
  }

  @Test
  void updateNovation_returnsUpdatedNovation() throws Exception {
    UUID novationGuid = UUID.randomUUID();
    NovationEntity novation =
        NovationEntity.builder()
            .guid(novationGuid)
            .novationType("Legal entity change")
            .novationEffectiveDate(LocalDate.of(2026, 3, 1))
            .novationStatus("Approved")
            .decisionDate(LocalDate.of(2026, 2, 1))
            .build();
    when(novationAmendmentService.updateNovation(any(), any())).thenReturn(novation);
    when(novationLinkRepository.findByNovationOrderByCreatedTimestampAsc(novation))
        .thenReturn(List.of());

    mockMvc
        .perform(
            patch("/novations/{novationGUID}", novationGuid)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "novationType": "Legal entity change",
                      "novationEffectiveDate": "2026-03-01",
                      "novationStatus": "Approved",
                      "decisionDate": "2026-02-01"
                    }
                    """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.guid").value(novationGuid.toString()))
        .andExpect(jsonPath("$.data.novationType").value("Legal entity change"))
        .andExpect(jsonPath("$.data.novationStatus").value("Approved"))
        .andExpect(jsonPath("$.data.decisionDate").value("2026-02-01"));
  }

  @Test
  void updateNovation_invalidTransition_returns400() throws Exception {
    when(novationAmendmentService.updateNovation(any(), any()))
        .thenThrow(new IllegalArgumentException("novationStatus transition is not permitted"));

    mockMvc
        .perform(
            patch("/novations/{novationGUID}", UUID.randomUUID())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "novationStatus": "Rescinded",
                      "rescindedDate": "2026-04-01",
                      "rescindedReason": "Cancelled"
                    }
                    """))
        .andExpect(status().isBadRequest());
  }

  @Test
  void createNovationRelationship_returns501NotImplemented() throws Exception {
    mockMvc
        .perform(
            post("/novations/{novationGUID}/relationships", UUID.randomUUID())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "previousProviderFirmGUID": "11111111-1111-1111-1111-111111111111",
                      "newProviderFirmGUID": "22222222-2222-2222-2222-222222222222"
                    }
                    """))
        .andExpect(status().isNotImplemented());
  }

  @Test
  void updateNovationRelationship_returnsUpdatedRelationship() throws Exception {
    UUID novationGuid = UUID.randomUUID();
    UUID relationshipGuid = UUID.randomUUID();
    UUID previousProviderGuid = UUID.randomUUID();
    UUID newProviderGuid = UUID.randomUUID();
    NovationEntity novation = NovationEntity.builder().guid(novationGuid).build();
    NovationLinkEntity link =
        NovationLinkEntity.builder()
            .guid(relationshipGuid)
            .novation(novation)
            .previousProvider(ProviderEntity.builder().guid(previousProviderGuid).build())
            .newProvider(ProviderEntity.builder().guid(newProviderGuid).build())
            .notes("Updated relationship")
            .build();
    when(novationAmendmentService.updateNovationRelationship(any(), any(), any())).thenReturn(link);

    mockMvc
        .perform(
            patch(
                    "/novations/{novationGUID}/relationships/{novationRelationshipGUID}",
                    novationGuid,
                    relationshipGuid)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "previousProviderFirmGUID": "%s",
                      "newProviderFirmGUID": "%s",
                      "notes": "Updated relationship"
                    }
                    """
                        .formatted(previousProviderGuid, newProviderGuid)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.guid").value(relationshipGuid.toString()))
        .andExpect(jsonPath("$.data.novationGUID").value(novationGuid.toString()))
        .andExpect(
            jsonPath("$.data.previousProviderFirmGUID").value(previousProviderGuid.toString()))
        .andExpect(jsonPath("$.data.newProviderFirmGUID").value(newProviderGuid.toString()))
        .andExpect(jsonPath("$.data.notes").value("Updated relationship"));
  }
}
