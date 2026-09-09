package uk.gov.justice.laa.providerdata.service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.AuditorAware;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.justice.laa.providerdata.entity.NovationEntity;
import uk.gov.justice.laa.providerdata.entity.NovationLinkEntity;
import uk.gov.justice.laa.providerdata.entity.NovationStatus;
import uk.gov.justice.laa.providerdata.entity.ProviderEntity;
import uk.gov.justice.laa.providerdata.entity.ProviderOfficeLinkEntity;
import uk.gov.justice.laa.providerdata.exception.ItemNotFoundException;
import uk.gov.justice.laa.providerdata.mapper.NovationMapper;
import uk.gov.justice.laa.providerdata.model.NovationCreateStatusV2;
import uk.gov.justice.laa.providerdata.model.NovationCreateV2;
import uk.gov.justice.laa.providerdata.model.NovationRelationshipCreateV2;
import uk.gov.justice.laa.providerdata.repository.NovationLinkRepository;
import uk.gov.justice.laa.providerdata.repository.NovationRepository;
import uk.gov.justice.laa.providerdata.repository.ProviderOfficeLinkRepository;
import uk.gov.justice.laa.providerdata.repository.ProviderRepository;

/**
 * Orchestrates atomic Novation creation: persists the Novation record together with all of its
 * predecessor/successor provider relationships (DSTEW-1975).
 *
 * <p>Contract and Schedule relationships are explicitly out of scope for this story; their GUID
 * fields are persisted as supplied but are not validated here.
 */
@Service
public class NovationCreationService {

  private final NovationRepository novationRepository;
  private final NovationLinkRepository novationLinkRepository;
  private final ProviderRepository providerRepository;
  private final ProviderOfficeLinkRepository providerOfficeLinkRepository;
  private final NovationMapper novationMapper;
  private final AuditorAware<String> auditorAware;

  /**
   * Inject dependencies.
   *
   * @param novationRepository persists Novation records
   * @param novationLinkRepository persists Novation relationship records
   * @param providerRepository resolves predecessor/successor provider firms
   * @param providerOfficeLinkRepository resolves predecessor/successor child offices
   * @param novationMapper maps request DTOs to entity templates
   * @param auditorAware supplies the identifier of the authenticated user for {@code decisionBy}
   */
  public NovationCreationService(
      NovationRepository novationRepository,
      NovationLinkRepository novationLinkRepository,
      ProviderRepository providerRepository,
      ProviderOfficeLinkRepository providerOfficeLinkRepository,
      NovationMapper novationMapper,
      AuditorAware<String> auditorAware) {
    this.novationRepository = novationRepository;
    this.novationLinkRepository = novationLinkRepository;
    this.providerRepository = providerRepository;
    this.providerOfficeLinkRepository = providerOfficeLinkRepository;
    this.novationMapper = novationMapper;
    this.auditorAware = auditorAware;
  }

  /**
   * Creates a Novation record together with its predecessor/successor relationships.
   *
   * @param request the validated Novation creation request
   * @return the identifiers of the created Novation and its relationships, in request order
   * @throws IllegalArgumentException if the Decision Date/Reason requirements for the selected
   *     {@code novationStatus} are not met (BR-39)
   * @throws ItemNotFoundException if a referenced provider firm or child office cannot be found
   */
  @Transactional
  public NovationCreationResult createNovation(NovationCreateV2 request) {
    validateDecisionInformation(request);

    NovationEntity novationTemplate = novationMapper.toNovationEntity(request);
    if (novationTemplate.getNovationStatus() != null
        && !NovationStatus.PROPOSED.equals(novationTemplate.getNovationStatus())) {
      novationTemplate.setDecisionBy(currentAuditor());
    }
    NovationEntity savedNovation = novationRepository.save(novationTemplate);

    List<UUID> relationshipGuids = new ArrayList<>();
    for (NovationRelationshipCreateV2 relationshipRequest : request.getRelationships()) {
      NovationLinkEntity savedLink = createRelationship(savedNovation, relationshipRequest);
      relationshipGuids.add(savedLink.getGuid());
    }

    return new NovationCreationResult(savedNovation.getGuid(), relationshipGuids);
  }

  private NovationLinkEntity createRelationship(
      NovationEntity novation, NovationRelationshipCreateV2 relationshipRequest) {
    ProviderEntity previousProvider =
        resolveProvider(
            relationshipRequest.getPreviousProviderFirmGUID(), "previousProviderFirmGUID");
    ProviderEntity newProvider =
        resolveProvider(relationshipRequest.getNewProviderFirmGUID(), "newProviderFirmGUID");

    NovationLinkEntity linkTemplate = novationMapper.toNovationLinkEntity(relationshipRequest);
    linkTemplate.setNovation(novation);
    linkTemplate.setPreviousProvider(previousProvider);
    linkTemplate.setNewProvider(newProvider);
    linkTemplate.setPreviousOffice(
        resolveOffice(
            relationshipRequest.getPreviousOfficeGUID(), previousProvider, "previousOfficeGUID"));
    linkTemplate.setNewOffice(
        resolveOffice(relationshipRequest.getNewOfficeGUID(), newProvider, "newOfficeGUID"));

    return novationLinkRepository.save(linkTemplate);
  }

  /**
   * Validates the Decision Date/Reason requirements for the selected Novation status (BR-39, AC7,
   * AC8). {@code Proposed} and an omitted status both require neither field; {@code Approved}
   * requires only a Decision Date; all other permitted statuses require both.
   */
  private void validateDecisionInformation(NovationCreateV2 request) {
    NovationCreateStatusV2 status = request.getNovationStatus();
    if (status == null || status == NovationCreateStatusV2.PROPOSED) {
      return;
    }
    if (request.getDecisionDate() == null) {
      throw new IllegalArgumentException(
          "decisionDate is required when novationStatus is '" + status.getValue() + "'");
    }
    boolean reasonRequired = status != NovationCreateStatusV2.APPROVED;
    if (reasonRequired && request.getDecisionReason() == null) {
      throw new IllegalArgumentException(
          "decisionReason is required when novationStatus is '" + status.getValue() + "'");
    }
  }

  private ProviderEntity resolveProvider(UUID guid, String fieldName) {
    return providerRepository
        .findById(guid)
        .orElseThrow(() -> new ItemNotFoundException("Unknown " + fieldName + ": " + guid));
  }

  /**
   * Resolves an optional Child Office GUID, ensuring it belongs to the given predecessor/successor
   * provider firm.
   */
  private @Nullable ProviderOfficeLinkEntity resolveOffice(
      @Nullable UUID guid, ProviderEntity provider, String fieldName) {
    if (guid == null) {
      return null;
    }
    return providerOfficeLinkRepository
        .findByProviderAndGuid(provider, guid)
        .orElseThrow(
            () ->
                new ItemNotFoundException(
                    "Unknown "
                        + fieldName
                        + ": "
                        + guid
                        + " for provider firm "
                        + provider.getGuid()));
  }

  private String currentAuditor() {
    return auditorAware.getCurrentAuditor().orElse("SYSTEM");
  }
}
