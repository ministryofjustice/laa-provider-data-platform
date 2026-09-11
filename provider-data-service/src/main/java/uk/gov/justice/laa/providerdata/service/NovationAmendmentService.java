package uk.gov.justice.laa.providerdata.service;

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
import uk.gov.justice.laa.providerdata.model.NovationPatchV2;
import uk.gov.justice.laa.providerdata.model.NovationRelationshipPatchV2;
import uk.gov.justice.laa.providerdata.model.NovationStatusV2;
import uk.gov.justice.laa.providerdata.repository.NovationLinkRepository;
import uk.gov.justice.laa.providerdata.repository.NovationRepository;
import uk.gov.justice.laa.providerdata.repository.ProviderOfficeLinkRepository;
import uk.gov.justice.laa.providerdata.repository.ProviderRepository;

/** Applies permitted amendments to existing Novation and Novation Relationship records. */
@Service
public class NovationAmendmentService {

  private final NovationRepository novationRepository;
  private final NovationLinkRepository novationLinkRepository;
  private final ProviderRepository providerRepository;
  private final ProviderOfficeLinkRepository providerOfficeLinkRepository;
  private final AuditorAware<String> auditorAware;

  /**
   * Creates the amendment service.
   *
   * @param novationRepository looks up and persists Novation records
   * @param novationLinkRepository looks up and persists Novation Relationship records
   * @param providerRepository resolves predecessor/successor provider firms
   * @param providerOfficeLinkRepository resolves predecessor/successor child offices
   * @param auditorAware supplies the authenticated user for decision/rescission metadata
   */
  public NovationAmendmentService(
      NovationRepository novationRepository,
      NovationLinkRepository novationLinkRepository,
      ProviderRepository providerRepository,
      ProviderOfficeLinkRepository providerOfficeLinkRepository,
      AuditorAware<String> auditorAware) {
    this.novationRepository = novationRepository;
    this.novationLinkRepository = novationLinkRepository;
    this.providerRepository = providerRepository;
    this.providerOfficeLinkRepository = providerOfficeLinkRepository;
    this.auditorAware = auditorAware;
  }

  /**
   * Updates permitted scalar fields on an existing Novation and validates status transition rules.
   *
   * @param novationGuid the Novation GUID
   * @param patch the amendment request
   * @return the persisted Novation entity
   */
  @Transactional
  public NovationEntity updateNovation(UUID novationGuid, NovationPatchV2 patch) {
    NovationEntity novation = getNovation(novationGuid);

    applyScalarFields(novation, patch);

    String currentStatus = statusOrProposed(novation.getNovationStatus());
    String requestedStatus = statusValue(patch.getNovationStatus());
    String finalStatus = requestedStatus == null ? currentStatus : requestedStatus;

    if (requestedStatus != null && !requestedStatus.equals(currentStatus)) {
      validateStatusTransition(currentStatus, requestedStatus);
      if (NovationStatus.RESCINDED.equals(requestedStatus)) {
        validateRescissionPatch(patch);
      } else if (patch.getRescindedDate() != null || patch.getRescindedReason() != null) {
        throw new IllegalArgumentException(
            "rescindedDate and rescindedReason may only be provided when status is Rescinded");
      }
      novation.setNovationStatus(requestedStatus);
    } else if (patch.getRescindedDate() != null || patch.getRescindedReason() != null) {
      throw new IllegalArgumentException(
          "rescindedDate and rescindedReason may only be provided when status is Rescinded");
    }

    validateDecisionInformation(novation, finalStatus);
    if (requestedStatus != null && !requestedStatus.equals(currentStatus)) {
      if (isDecisionStatus(requestedStatus) && NovationStatus.PROPOSED.equals(currentStatus)) {
        novation.setDecisionBy(currentAuditor());
      }
      if (NovationStatus.RESCINDED.equals(requestedStatus)) {
        novation.setRescindedDate(patch.getRescindedDate());
        novation.setRescindedReason(patch.getRescindedReason());
        novation.setRescindedBy(currentAuditor());
      }
    }
    return novationRepository.save(novation);
  }

  /**
   * Updates an existing relationship belonging to the given Novation.
   *
   * @param novationGuid the parent Novation GUID
   * @param relationshipGuid the Novation Relationship GUID
   * @param patch the relationship amendment request
   * @return the persisted relationship entity
   */
  @Transactional
  public NovationLinkEntity updateNovationRelationship(
      UUID novationGuid, UUID relationshipGuid, NovationRelationshipPatchV2 patch) {
    NovationEntity novation = getNovation(novationGuid);
    NovationLinkEntity link =
        novationLinkRepository
            .findById(relationshipGuid)
            .filter(existing -> existing.getNovation().getGuid().equals(novation.getGuid()))
            .orElseThrow(
                () ->
                    new ItemNotFoundException(
                        "Unknown novationRelationshipGUID: "
                            + relationshipGuid
                            + " for novationGUID "
                            + novationGuid));

    ProviderEntity previousProvider =
        resolveProvider(patch.getPreviousProviderFirmGUID(), "previousProviderFirmGUID");
    ProviderEntity newProvider =
        resolveProvider(patch.getNewProviderFirmGUID(), "newProviderFirmGUID");
    link.setPreviousProvider(previousProvider);
    link.setNewProvider(newProvider);
    link.setPreviousOffice(
        resolveOffice(patch.getPreviousOfficeGUID(), previousProvider, "previousOfficeGUID"));
    link.setNewOffice(resolveOffice(patch.getNewOfficeGUID(), newProvider, "newOfficeGUID"));
    link.setPreviousContractGuid(patch.getPreviousContractGUID());
    link.setNewContractGuid(patch.getNewContractGUID());
    link.setPreviousScheduleGuid(patch.getPreviousScheduleGUID());
    link.setNewScheduleGuid(patch.getNewScheduleGUID());
    link.setNotes(patch.getNotes());

    return novationLinkRepository.save(link);
  }

  private NovationEntity getNovation(UUID novationGuid) {
    return novationRepository
        .findById(novationGuid)
        .orElseThrow(() -> new ItemNotFoundException("Unknown novationGUID: " + novationGuid));
  }

  private void applyScalarFields(NovationEntity novation, NovationPatchV2 patch) {
    if (patch.getNovationType() != null) {
      if (patch.getNovationType().isBlank()) {
        throw new IllegalArgumentException("novationType must not be blank");
      }
      novation.setNovationType(patch.getNovationType());
    }
    if (patch.getNovationEffectiveDate() != null) {
      novation.setNovationEffectiveDate(patch.getNovationEffectiveDate());
    }
    if (patch.getDecisionDate() != null) {
      novation.setDecisionDate(patch.getDecisionDate());
    }
    if (patch.getDecisionReason() != null) {
      novation.setDecisionReason(patch.getDecisionReason());
    }
    if (patch.getDriverForNovation() != null) {
      novation.setDriverForNovation(patch.getDriverForNovation());
    }
    if (patch.getNotes() != null) {
      novation.setNotes(patch.getNotes());
    }
  }

  private void validateStatusTransition(String currentStatus, String requestedStatus) {
    boolean valid =
        switch (currentStatus) {
          case NovationStatus.PROPOSED ->
              NovationStatus.APPROVED.equals(requestedStatus)
                  || NovationStatus.APPROVED_WITH_CONDITIONS.equals(requestedStatus)
                  || NovationStatus.REJECTED.equals(requestedStatus)
                  || NovationStatus.WITHDRAWN.equals(requestedStatus);
          case NovationStatus.APPROVED, NovationStatus.APPROVED_WITH_CONDITIONS ->
              NovationStatus.RESCINDED.equals(requestedStatus);
          default -> false;
        };
    if (!valid) {
      throw new IllegalArgumentException(
          "novationStatus transition from '"
              + currentStatus
              + "' to '"
              + requestedStatus
              + "' is not permitted");
    }
  }

  private void validateRescissionPatch(NovationPatchV2 patch) {
    if (patch.getRescindedDate() == null) {
      throw new IllegalArgumentException(
          "rescindedDate is required when novationStatus is Rescinded");
    }
    if (patch.getRescindedReason() == null || patch.getRescindedReason().isBlank()) {
      throw new IllegalArgumentException(
          "rescindedReason is required when novationStatus is Rescinded");
    }
  }

  private void validateDecisionInformation(NovationEntity novation, String status) {
    if (!isDecisionStatus(status)) {
      return;
    }
    if (!NovationStatus.RESCINDED.equals(status) && novation.getDecisionDate() == null) {
      throw new IllegalArgumentException(
          "decisionDate is required when novationStatus is '" + status + "'");
    }
    boolean decisionReasonRequired =
        NovationStatus.APPROVED_WITH_CONDITIONS.equals(status)
            || NovationStatus.REJECTED.equals(status)
            || NovationStatus.WITHDRAWN.equals(status);
    if (decisionReasonRequired
        && (novation.getDecisionReason() == null || novation.getDecisionReason().isBlank())) {
      throw new IllegalArgumentException(
          "decisionReason is required when novationStatus is '" + status + "'");
    }
  }

  private static boolean isDecisionStatus(String status) {
    return !NovationStatus.PROPOSED.equals(status);
  }

  private static String statusOrProposed(@Nullable String status) {
    return status == null ? NovationStatus.PROPOSED : status;
  }

  private static @Nullable String statusValue(@Nullable NovationStatusV2 status) {
    return status == null ? null : status.getValue();
  }

  private ProviderEntity resolveProvider(UUID guid, String fieldName) {
    return providerRepository
        .findById(guid)
        .orElseThrow(() -> new ItemNotFoundException("Unknown " + fieldName + ": " + guid));
  }

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
