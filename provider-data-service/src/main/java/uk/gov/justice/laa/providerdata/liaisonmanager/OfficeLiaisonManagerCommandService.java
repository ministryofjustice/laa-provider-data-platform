package uk.gov.justice.laa.providerdata.liaisonmanager;

import java.time.LocalDate;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.justice.laa.providerdata.liaisonmanager.repository.LiaisonManagerRepository;
import uk.gov.justice.laa.providerdata.liaisonmanager.repository.OfficeLiaisonManagerLinkRepository;
import uk.gov.justice.laa.providerdata.model.LiaisonManagerCreateV2;
import uk.gov.justice.laa.providerdata.model.LiaisonManagerLinkChambersV2;
import uk.gov.justice.laa.providerdata.model.LiaisonManagerLinkHeadOfficeV2;
import uk.gov.justice.laa.providerdata.model.OfficeLiaisonManagerCreateOrLinkV2;
import uk.gov.justice.laa.providerdata.office.OfficeQueryService;
import uk.gov.justice.laa.providerdata.office.ProviderOfficeLinkEntity;
import uk.gov.justice.laa.providerdata.provider.ProviderEntity;
import uk.gov.justice.laa.providerdata.provider.ProviderQueryService;
import uk.gov.justice.laa.providerdata.shared.FirmType;
import uk.gov.justice.laa.providerdata.shared.ItemNotFoundException;

/** Service responsible for office liaison manager write operations. */
@Service
@RequiredArgsConstructor
public class OfficeLiaisonManagerCommandService {

  private final ProviderQueryService providerQueryService;
  private final OfficeQueryService officeQueryService;
  private final LiaisonManagerRepository liaisonManagerRepository;
  private final OfficeLiaisonManagerLinkRepository officeLiaisonManagerLinkRepository;

  /**
   * Result record returned after a successful liaison manager create or link operation.
   *
   * @param providerFirmGuid GUID of the resolved provider firm.
   * @param providerFirmNumber firm number of the resolved provider.
   * @param officeGuid GUID of the resolved office link.
   * @param officeCode account number of the resolved office.
   * @param liaisonManagerGuid GUID of the liaison manager now linked to the office.
   */
  public record OfficeLiaisonManagerOperationResult(
      UUID providerFirmGuid,
      String providerFirmNumber,
      UUID officeGuid,
      String officeCode,
      UUID liaisonManagerGuid) {}

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

  /**
   * Saves a new liaison manager and links it to an office. The {@code lmLinkTemplate} must have
   * {@code officeLink} set; {@code liaisonManager}, {@code activeDateFrom} (defaults to today), and
   * {@code linkedFlag} (defaults to {@code false}) are applied by this method.
   *
   * @param lmTemplate unpersisted liaison manager entity to create.
   * @param lmLinkTemplate link template with {@code officeLink} already set.
   */
  @Transactional
  public void createAndLink(
      LiaisonManagerEntity lmTemplate, OfficeLiaisonManagerLinkEntity lmLinkTemplate) {
    var saved = liaisonManagerRepository.save(lmTemplate);
    lmLinkTemplate.setLiaisonManager(saved);
    if (lmLinkTemplate.getActiveDateFrom() == null) {
      lmLinkTemplate.setActiveDateFrom(LocalDate.now());
    }
    if (lmLinkTemplate.getLinkedFlag() == null) {
      lmLinkTemplate.setLinkedFlag(Boolean.FALSE);
    }
    officeLiaisonManagerLinkRepository.save(lmLinkTemplate);
  }

  /**
   * Copies the active liaison manager from the provider's head office to the given target office.
   *
   * @param provider the provider whose head office's active LM is to be copied.
   * @param targetOfficeLink the office to receive the liaison manager link.
   * @throws ItemNotFoundException if no head office or no active liaison manager is found.
   */
  @Transactional
  public void copyFromHeadOfficeToOffice(
      ProviderEntity provider, ProviderOfficeLinkEntity targetOfficeLink) {
    ProviderOfficeLinkEntity headOfficeLink =
        officeQueryService
            .getProviderHeadOfficeLink(provider)
            .orElseThrow(
                () ->
                    new ItemNotFoundException(
                        "Head office not found for provider: " + provider.getGuid()));
    var activeLinks =
        officeLiaisonManagerLinkRepository.findByOfficeLinkAndActiveDateToIsNull(headOfficeLink);
    if (activeLinks.isEmpty()) {
      throw new ItemNotFoundException(
          "No active liaison manager found on head office for provider: " + provider.getGuid());
    }
    var headOfficeLm = activeLinks.getFirst().getLiaisonManager();
    var link = new OfficeLiaisonManagerLinkEntity();
    link.setLiaisonManager(headOfficeLm);
    link.setOfficeLink(targetOfficeLink);
    link.setActiveDateFrom(LocalDate.now());
    link.setLinkedFlag(Boolean.TRUE);
    officeLiaisonManagerLinkRepository.save(link);
  }

  private LiaisonManagerEntity resolveOrCreateLiaisonManager(
      ProviderEntity provider, OfficeLiaisonManagerCreateOrLinkV2 request) {

    return switch (request) {
      case LiaisonManagerCreateV2 create -> {
        LiaisonManagerEntity entity = new LiaisonManagerEntity();
        entity.setFirstName(create.getFirstName());
        entity.setLastName(create.getLastName());
        entity.setEmailAddress(create.getEmailAddress());
        entity.setTelephoneNumber(create.getTelephoneNumber());
        yield liaisonManagerRepository.save(entity);
      }
      case LiaisonManagerLinkHeadOfficeV2 linkHeadOffice -> {
        if (!Boolean.TRUE.equals(linkHeadOffice.getUseHeadOfficeLiaisonManager())) {
          throw new IllegalArgumentException("useHeadOfficeLiaisonManager must be true");
        }
        if (!FirmType.LEGAL_SERVICES_PROVIDER.equals(provider.getFirmType())) {
          throw new IllegalArgumentException(
              "linkHeadOffice is only applicable for firmType=" + FirmType.LEGAL_SERVICES_PROVIDER);
        }
        ProviderOfficeLinkEntity headOfficeLink = resolveHeadOfficeOffice(provider, "Head office");
        yield resolveActiveLiaisonManagerForOffice(headOfficeLink);
      }
      case LiaisonManagerLinkChambersV2 linkChambers -> {
        if (!Boolean.TRUE.equals(linkChambers.getUseChambersLiaisonManager())) {
          throw new IllegalArgumentException("useChambersLiaisonManager must be true");
        }
        if (!FirmType.CHAMBERS.equals(provider.getFirmType())) {
          throw new IllegalArgumentException(
              "linkChambers currently supports firmType=" + FirmType.CHAMBERS);
        }
        ProviderOfficeLinkEntity chambersOfficeLink =
            resolveHeadOfficeOffice(provider, "Chambers head office");
        yield resolveActiveLiaisonManagerForOffice(chambersOfficeLink);
      }
      default ->
          throw new IllegalArgumentException(
              "Unsupported liaison manager request type: " + request);
    };
  }

  private ProviderOfficeLinkEntity resolveProviderOfficeLink(
      ProviderEntity provider, String officeGuidOrCode) {
    return officeQueryService
        .findProviderOfficeLink(provider, officeGuidOrCode)
        .orElseThrow(() -> new ItemNotFoundException("Office not found for provider"));
  }

  private ProviderEntity resolveProvider(String providerFirmGuidOrNumber) {
    return providerQueryService.getProvider(providerFirmGuidOrNumber);
  }

  private ProviderOfficeLinkEntity resolveHeadOfficeOffice(
      ProviderEntity provider, String description) {
    return officeQueryService
        .getProviderHeadOfficeLink(provider)
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
}
