package uk.gov.justice.laa.providerdata.service;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.justice.laa.providerdata.api.model.OfficeLiaisonManagerCreateRequest;
import uk.gov.justice.laa.providerdata.entity.LiaisonManagerEntity;
import uk.gov.justice.laa.providerdata.entity.OfficeLiaisonManagerLinkEntity;
import uk.gov.justice.laa.providerdata.exception.ItemNotFoundException;
import uk.gov.justice.laa.providerdata.repository.LiaisonManagerRepository;
import uk.gov.justice.laa.providerdata.repository.OfficeLiaisonManagerLinkRepository;
import uk.gov.justice.laa.providerdata.repository.ProviderOfficeLinkRepository;
import uk.gov.justice.laa.providerdata.repository.ProviderRepository;

/** java doc. */
@Service
public class OfficeLiaisonManagerService {

  private final ProviderRepository providerRepository;
  private final ProviderOfficeLinkRepository providerOfficeLinkRepository;
  private final LiaisonManagerRepository liaisonManagerRepository;
  private final OfficeLiaisonManagerLinkRepository officeLiaisonManagerLinkRepository;

  /** java doc. */
  public OfficeLiaisonManagerService(
      ProviderRepository providerRepository,
      ProviderOfficeLinkRepository providerOfficeLinkRepository,
      LiaisonManagerRepository liaisonManagerRepository,
      OfficeLiaisonManagerLinkRepository officeLiaisonManagerLinkRepository) {
    this.providerRepository = providerRepository;
    this.providerOfficeLinkRepository = providerOfficeLinkRepository;
    this.liaisonManagerRepository = liaisonManagerRepository;
    this.officeLiaisonManagerLinkRepository = officeLiaisonManagerLinkRepository;
  }

  /** java doc. */
  @Transactional
  public List<LiaisonManagerEntity> postOfficeLiaisonManager(
      String providerFirmGuidOrNumber,
      String officeGuidOrCode,
      OfficeLiaisonManagerCreateRequest request) {

    var provider =
        parseUuid(providerFirmGuidOrNumber)
            .flatMap(providerRepository::findById)
            .orElseGet(
                () ->
                    providerRepository
                        .findByFirmNumber(providerFirmGuidOrNumber)
                        .orElseThrow(() -> new ItemNotFoundException("Provider not found")));

    var providerOfficeLink =
        parseUuid(officeGuidOrCode)
            .flatMap(
                officeGuid ->
                    providerOfficeLinkRepository.findByProvider_GuidAndOffice_Guid(
                        provider.getGuid(), officeGuid))
            .orElseGet(
                () ->
                    providerOfficeLinkRepository
                        .findByProvider_GuidAndAccountNumber(provider.getGuid(), officeGuidOrCode)
                        .orElseThrow(
                            () -> new ItemNotFoundException("Office not found for provider")));

    LocalDate today = LocalDate.now();
    OffsetDateTime now = OffsetDateTime.now();

    // End-date existing office liaison manager links (MVP behaviour).
    var existingLinks =
        officeLiaisonManagerLinkRepository.findByOffice_Guid(
            providerOfficeLink.getOffice().getGuid());

    for (OfficeLiaisonManagerLinkEntity link : existingLinks) {
      if (link.getActiveDateTo() == null || link.getActiveDateTo().isAfter(today)) {
        link.setActiveDateTo(today);
      }
    }

    LiaisonManagerEntity liaisonManager = resolveOrCreateLiaisonManager(request, now);

    OfficeLiaisonManagerLinkEntity newLink = new OfficeLiaisonManagerLinkEntity();
    newLink.setOffice(providerOfficeLink.getOffice());
    newLink.setLiaisonManager(liaisonManager);
    newLink.setActiveDateFrom(today);
    newLink.setActiveDateTo(null);
    newLink.setLinkedFlag(true);

    officeLiaisonManagerLinkRepository.save(newLink);

    // Return current active liaison managers for the office (content only).
    return officeLiaisonManagerLinkRepository
        .findByOffice_Guid(providerOfficeLink.getOffice().getGuid())
        .stream()
        .filter(l -> l.getActiveDateTo() == null || l.getActiveDateTo().isAfter(today))
        .map(OfficeLiaisonManagerLinkEntity::getLiaisonManager)
        .toList();
  }

  private LiaisonManagerEntity resolveOrCreateLiaisonManager(
      OfficeLiaisonManagerCreateRequest request, OffsetDateTime now) {

    if (request.create() != null) {
      LiaisonManagerEntity entity = new LiaisonManagerEntity();
      entity.setFirstName(request.create().firstName());
      entity.setLastName(request.create().lastName());
      entity.setEmailAddress(request.create().emailAddress());
      entity.setTelephoneNumber(request.create().telephoneNumber());
      return liaisonManagerRepository.save(entity);
    }

    if (request.linkHeadOffice() != null) {
      return liaisonManagerRepository
          .findById(request.linkHeadOffice().liaisonManagerGuid())
          .orElseThrow(() -> new ItemNotFoundException("Liaison manager not found"));
    }

    if (request.linkChambers() != null) {
      return liaisonManagerRepository
          .findById(request.linkChambers().liaisonManagerGuid())
          .orElseThrow(() -> new ItemNotFoundException("Liaison manager not found"));
    }

    throw new IllegalArgumentException(
        "Exactly one of create, linkHeadOffice, linkChambers must be provided");
  }

  private static Optional<UUID> parseUuid(String value) {
    try {
      return Optional.of(UUID.fromString(value));
    } catch (Exception ignored) {
      return Optional.empty();
    }
  }
}
