package uk.gov.justice.laa.providerdata.controller;

import java.util.List;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.justice.laa.providerdata.api.NovationRelationshipsApi;
import uk.gov.justice.laa.providerdata.api.NovationsApi;
import uk.gov.justice.laa.providerdata.entity.NovationEntity;
import uk.gov.justice.laa.providerdata.entity.NovationLinkEntity;
import uk.gov.justice.laa.providerdata.exception.ItemNotFoundException;
import uk.gov.justice.laa.providerdata.mapper.NovationMapper;
import uk.gov.justice.laa.providerdata.model.CreateNovation201Response;
import uk.gov.justice.laa.providerdata.model.CreateNovationRelationship201Response;
import uk.gov.justice.laa.providerdata.model.GetNovation200Response;
import uk.gov.justice.laa.providerdata.model.GetNovations200Response;
import uk.gov.justice.laa.providerdata.model.NovationCreateResponseV2;
import uk.gov.justice.laa.providerdata.model.NovationCreateV2;
import uk.gov.justice.laa.providerdata.model.NovationPatchV2;
import uk.gov.justice.laa.providerdata.model.NovationRelationshipCreateV2;
import uk.gov.justice.laa.providerdata.model.NovationRelationshipPatchV2;
import uk.gov.justice.laa.providerdata.model.NovationStatusV2;
import uk.gov.justice.laa.providerdata.model.UpdateNovationRelationship200Response;
import uk.gov.justice.laa.providerdata.repository.NovationLinkRepository;
import uk.gov.justice.laa.providerdata.repository.NovationRepository;
import uk.gov.justice.laa.providerdata.service.NovationAmendmentService;
import uk.gov.justice.laa.providerdata.service.NovationCreationResult;
import uk.gov.justice.laa.providerdata.service.NovationCreationService;

/**
 * REST controller implementing the Novations API.
 *
 * <p>{@code createNovation} (DSTEW-1975), {@code getNovation} (DSTEW-1980), {@code updateNovation}
 * and {@code updateNovationRelationship} (DSTEW-1977) are implemented. {@code getNovations}
 * (list/search) and {@code createNovationRelationship} are published as contract-only stubs for
 * future stories and return 501 Not Implemented.
 */
@RestController
public class NovationsController implements NovationsApi, NovationRelationshipsApi {

  private final NovationCreationService novationCreationService;
  private final NovationAmendmentService novationAmendmentService;
  private final NovationRepository novationRepository;
  private final NovationLinkRepository novationLinkRepository;
  private final NovationMapper novationMapper;

  /**
   * Creates the controller with its collaborators.
   *
   * @param novationCreationService creates Novation records and their relationships
   * @param novationAmendmentService amends Novation records and relationships
   * @param novationRepository looks up persisted Novation entities for retrieval
   * @param novationLinkRepository looks up persisted Novation relationships for retrieval
   * @param novationMapper maps between entities and API models
   */
  public NovationsController(
      NovationCreationService novationCreationService,
      NovationAmendmentService novationAmendmentService,
      NovationRepository novationRepository,
      NovationLinkRepository novationLinkRepository,
      NovationMapper novationMapper) {
    this.novationCreationService = novationCreationService;
    this.novationAmendmentService = novationAmendmentService;
    this.novationRepository = novationRepository;
    this.novationLinkRepository = novationLinkRepository;
    this.novationMapper = novationMapper;
  }

  /**
   * Creates a Novation record together with its initial predecessor/successor provider
   * relationships.
   *
   * @param novationCreateV2 the Novation creation request
   * @param traceparent W3C Trace Context header, unused
   * @return 201 with the generated Novation GUID and Novation Relationship GUIDs
   */
  @Override
  public ResponseEntity<CreateNovation201Response> createNovation(
      NovationCreateV2 novationCreateV2, @Nullable String traceparent) {

    NovationCreationResult result = novationCreationService.createNovation(novationCreateV2);

    NovationCreateResponseV2 data =
        new NovationCreateResponseV2()
            .novationGUID(result.novationGUID())
            .novationRelationshipGUIDs(result.novationRelationshipGUIDs());

    return ResponseEntity.status(HttpStatus.CREATED).body(new CreateNovation201Response(data));
  }

  @Override
  public ResponseEntity<GetNovation200Response> getNovation(
      UUID novationGUID, @Nullable String traceparent) {
    NovationEntity novation =
        novationRepository
            .findById(novationGUID)
            .orElseThrow(() -> new ItemNotFoundException("Unknown novationGUID: " + novationGUID));
    List<NovationLinkEntity> links =
        novationLinkRepository.findByNovationOrderByCreatedTimestampAsc(novation);

    return ResponseEntity.ok(
        new GetNovation200Response(novationMapper.toNovationV2(novation, links)));
  }

  @Override
  public ResponseEntity<GetNovations200Response> getNovations(
      @Nullable String traceparent,
      @Nullable List<String> novationGUID,
      @Nullable List<NovationStatusV2> novationStatus,
      @Nullable List<String> providerFirmGUID,
      @Nullable Integer page,
      @Nullable Integer pageSize) {
    return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
  }

  @Override
  public ResponseEntity<GetNovation200Response> updateNovation(
      UUID novationGUID, NovationPatchV2 novationPatchV2, @Nullable String traceparent) {
    NovationEntity novation =
        novationAmendmentService.updateNovation(novationGUID, novationPatchV2);
    List<NovationLinkEntity> links =
        novationLinkRepository.findByNovationOrderByCreatedTimestampAsc(novation);
    return ResponseEntity.ok(
        new GetNovation200Response(novationMapper.toNovationV2(novation, links)));
  }

  @Override
  public ResponseEntity<CreateNovationRelationship201Response> createNovationRelationship(
      UUID novationGUID,
      NovationRelationshipCreateV2 novationRelationshipCreateV2,
      @Nullable String traceparent) {
    return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
  }

  @Override
  public ResponseEntity<UpdateNovationRelationship200Response> updateNovationRelationship(
      UUID novationGUID,
      UUID novationRelationshipGUID,
      NovationRelationshipPatchV2 novationRelationshipPatchV2,
      @Nullable String traceparent) {
    NovationLinkEntity link =
        novationAmendmentService.updateNovationRelationship(
            novationGUID, novationRelationshipGUID, novationRelationshipPatchV2);
    return ResponseEntity.ok(
        new UpdateNovationRelationship200Response(novationMapper.toNovationRelationshipV2(link)));
  }
}
