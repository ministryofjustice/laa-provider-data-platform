package uk.gov.justice.laa.providerdata.service;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.justice.laa.providerdata.entity.LiaisonManagerEntity;
import uk.gov.justice.laa.providerdata.entity.OfficeLiaisonManagerLinkEntity;
import uk.gov.justice.laa.providerdata.entity.ProviderEntity;
import uk.gov.justice.laa.providerdata.entity.ProviderOfficeLinkEntity;
import uk.gov.justice.laa.providerdata.exception.ItemNotFoundException;
import uk.gov.justice.laa.providerdata.model.LiaisonManagerCreateV2;
import uk.gov.justice.laa.providerdata.model.LiaisonManagerLinkChambersV2;
import uk.gov.justice.laa.providerdata.model.LiaisonManagerLinkHeadOfficeV2;
import uk.gov.justice.laa.providerdata.model.OfficeLiaisonManagerCreateOrLinkV2;
import uk.gov.justice.laa.providerdata.repository.LiaisonManagerRepository;
import uk.gov.justice.laa.providerdata.repository.OfficeLiaisonManagerLinkRepository;
import uk.gov.justice.laa.providerdata.repository.ProviderOfficeLinkRepository;
import uk.gov.justice.laa.providerdata.repository.ProviderRepository;

/** Service responsible for Office Liaison Manager operations. */
@Service
public class OfficeLiaisonManagerService {

  private static final String FIRM_TYPE_LSP = "Legal Services Provider";
  private static final String FIRM_TYPE_CHAMBERS = "Chambers";

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
  public record OfficeLiaisonManagerOperationResult(
      UUID providerFirmGuid,
      String providerFirmNumber,
      UUID officeGuid,
      String officeCode,
      UUID liaisonManagerGuid) {}

  /** Returns liaison managers linked to a provider office (including historical) as a page. */
  @Transactional(readOnly = true)
  public Page<OfficeLiaisonManagerLinkEntity> getOfficeLiaisonManagers(
      String providerFirmGuidOrNumber, String officeGuidOrCode, Pageable pageable) {

    ProviderOfficeLinkEntity providerOfficeLink =
        resolveProviderOfficeLink(providerFirmGuidOrNumber, officeGuidOrCode);

    return officeLiaisonManagerLinkRepository.findByOfficeLink_GuidOrderByActiveDateFromDesc(
        providerOfficeLink.getGuid(), pageable);
  }

  /** POST create/link liaison manager (end-dates existing links for target office). */
  @Transactional
  public OfficeLiaisonManagerOperationResult postOfficeLiaisonManager(
      String providerFirmGuidOrNumber,
      String officeGuidOrCode,
      OfficeLiaisonManagerCreateOrLinkV2 request) {

    ProviderEntity provider = resolveProvider(providerFirmGuidOrNumber);
    ProviderOfficeLinkEntity providerOfficeLink =
        resolveProviderOfficeLink(provider, officeGuidOrCode);

    // IMPORTANT:
    // Resolve the liaison manager BEFORE end-dating existing links.
    // This prevents linkHeadOffice/linkChambers from failing when the target office is the same
    // as the source office (e.g. calling linkHeadOffice on the head office itself).
    LiaisonManagerEntity liaisonManager = resolveOrCreateLiaisonManager(provider, request);

    // End-date existing office liaison manager links (current behaviour).
    var existingLinks =
        officeLiaisonManagerLinkRepository.findByOfficeLink_Guid(providerOfficeLink.getGuid());

    LocalDate today = LocalDate.now();
    for (OfficeLiaisonManagerLinkEntity link : existingLinks) {
      if (link.getActiveDateTo() == null || link.getActiveDateTo().isAfter(today)) {
        link.setActiveDateTo(today);
      }
    }

    OfficeLiaisonManagerLinkEntity newLink = new OfficeLiaisonManagerLinkEntity();
    newLink.setOfficeLink(providerOfficeLink);
    newLink.setLiaisonManager(liaisonManager);
    newLink.setActiveDateFrom(today);
    newLink.setActiveDateTo(null);
    newLink.setLinkedFlag(true);

    officeLiaisonManagerLinkRepository.save(newLink);

    return new OfficeLiaisonManagerOperationResult(
        provider.getGuid(),
        provider.getFirmNumber(),
        providerOfficeLink.getGuid(),
        providerOfficeLink.getAccountNumber(),
        liaisonManager.getGuid());
  }

  private LiaisonManagerEntity resolveOrCreateLiaisonManager(
      ProviderEntity provider, OfficeLiaisonManagerCreateOrLinkV2 request) {

    if (request instanceof LiaisonManagerCreateV2 create) {
      LiaisonManagerEntity entity = new LiaisonManagerEntity();
      entity.setFirstName(create.getFirstName());
      entity.setLastName(create.getLastName());
      entity.setEmailAddress(create.getEmailAddress());
      entity.setTelephoneNumber(create.getTelephoneNumber());
      return liaisonManagerRepository.save(entity);
    }

    if (request instanceof LiaisonManagerLinkHeadOfficeV2 linkHeadOffice) {
      if (!Boolean.TRUE.equals(linkHeadOffice.getUseHeadOfficeLiaisonManager())) {
        throw new IllegalArgumentException("useHeadOfficeLiaisonManager must be true");
      }
      if (!FIRM_TYPE_LSP.equals(provider.getFirmType())) {
        throw new IllegalArgumentException(
            "linkHeadOffice is only applicable for firmType=" + FIRM_TYPE_LSP);
      }
      ProviderOfficeLinkEntity headOfficeLink = resolveHeadOfficeOffice(provider, "Head office");
      return resolveActiveLiaisonManagerForOffice(headOfficeLink);
    }

    if (request instanceof LiaisonManagerLinkChambersV2 linkChambers) {
      if (!Boolean.TRUE.equals(linkChambers.getUseChambersLiaisonManager())) {
        throw new IllegalArgumentException("useChambersLiaisonManager must be true");
      }
      if (!FIRM_TYPE_CHAMBERS.equals(provider.getFirmType())) {
        throw new IllegalArgumentException(
            "linkChambers currently supports firmType=" + FIRM_TYPE_CHAMBERS);
      }
      ProviderOfficeLinkEntity chambersOfficeLink =
          resolveHeadOfficeOffice(provider, "Chambers head office");
      return resolveActiveLiaisonManagerForOffice(chambersOfficeLink);
    }

    throw new IllegalArgumentException("Unsupported liaison manager request type: " + request);
  }

  private ProviderOfficeLinkEntity resolveProviderOfficeLink(
      String providerFirmGuidOrNumber, String officeGuidOrCode) {
    return resolveProviderOfficeLink(resolveProvider(providerFirmGuidOrNumber), officeGuidOrCode);
  }

  private ProviderOfficeLinkEntity resolveProviderOfficeLink(
      ProviderEntity provider, String officeGuidOrCode) {
    return parseUuid(officeGuidOrCode)
        .flatMap(uuid -> providerOfficeLinkRepository.findByProviderAndGuid(provider, uuid))
        .orElseGet(
            () ->
                providerOfficeLinkRepository
                    .findByProvider_GuidAndAccountNumber(provider.getGuid(), officeGuidOrCode)
                    .orElseThrow(() -> new ItemNotFoundException("Office not found for provider")));
  }

  private ProviderEntity resolveProvider(String providerFirmGuidOrNumber) {
    return parseUuid(providerFirmGuidOrNumber)
        .flatMap(providerRepository::findById)
        .orElseGet(
            () ->
                providerRepository
                    .findByFirmNumber(providerFirmGuidOrNumber)
                    .orElseThrow(() -> new ItemNotFoundException("Provider not found")));
  }

  private ProviderOfficeLinkEntity resolveHeadOfficeOffice(
      ProviderEntity provider, String description) {
    return providerOfficeLinkRepository
        .findByProviderAndHeadOfficeFlagTrue(provider)
        .orElseThrow(
            () ->
                new ItemNotFoundException(
                    description + " not found for provider: " + provider.getGuid()));
  }

  private LiaisonManagerEntity resolveActiveLiaisonManagerForOffice(
      ProviderOfficeLinkEntity officeLink) {
    var activeLinks =
        officeLiaisonManagerLinkRepository.findByOfficeLinkAndActiveDateToIsNull(officeLink);
    if (activeLinks.isEmpty()) {
      throw new ItemNotFoundException(
          "No active liaison manager found for office: " + officeLink.getGuid());
    }
    return activeLinks.getFirst().getLiaisonManager();
  }

  private static Optional<UUID> parseUuid(String value) {
    try {
      return Optional.of(UUID.fromString(value));
    } catch (Exception ignored) {
      return Optional.empty();
    }
  }
}
