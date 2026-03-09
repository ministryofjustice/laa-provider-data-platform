package uk.gov.justice.laa.providerdata.service;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.justice.laa.providerdata.entity.LiaisonManagerEntity;
import uk.gov.justice.laa.providerdata.entity.OfficeLiaisonManagerLinkEntity;
import uk.gov.justice.laa.providerdata.exception.ItemNotFoundException;
import uk.gov.justice.laa.providerdata.model.LiaisonManagerCreateV2;
import uk.gov.justice.laa.providerdata.model.LiaisonManagerLinkChambersV2;
import uk.gov.justice.laa.providerdata.model.LiaisonManagerLinkHeadOfficeV2;
import uk.gov.justice.laa.providerdata.model.OfficeLiaisonManagerCreateOrLinkV2;
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
  public record OfficeLiaisonManagerOperationResult(
      UUID providerFirmGuid,
      String providerFirmNumber,
      UUID officeGuid,
      String officeCode,
      UUID liaisonManagerGuid) {}

  /** java doc. */
  @Transactional
  public OfficeLiaisonManagerOperationResult postOfficeLiaisonManager(
      String providerFirmGuidOrNumber,
      String officeGuidOrCode,
      OfficeLiaisonManagerCreateOrLinkV2 request) {

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

    return new OfficeLiaisonManagerOperationResult(
        provider.getGuid(),
        provider.getFirmNumber(),
        providerOfficeLink.getOffice().getGuid(),
        providerOfficeLink.getAccountNumber(),
        liaisonManager.getGuid());
  }

  private LiaisonManagerEntity resolveOrCreateLiaisonManager(
      OfficeLiaisonManagerCreateOrLinkV2 request, OffsetDateTime now) {

    if (request instanceof LiaisonManagerCreateV2 create) {
      LiaisonManagerEntity entity = new LiaisonManagerEntity();
      entity.setFirstName(create.getFirstName());
      entity.setLastName(create.getLastName());
      entity.setEmailAddress(create.getEmailAddress());
      entity.setTelephoneNumber(create.getTelephoneNumber());
      return liaisonManagerRepository.save(entity);
    }

    if (request instanceof LiaisonManagerLinkHeadOfficeV2 linkHeadOffice) {
      UUID guid = extractLiaisonManagerGuid(linkHeadOffice);
      return liaisonManagerRepository
          .findById(guid)
          .orElseThrow(() -> new ItemNotFoundException("Liaison manager not found"));
    }

    if (request instanceof LiaisonManagerLinkChambersV2 linkChambers) {
      UUID guid = extractLiaisonManagerGuid(linkChambers);
      return liaisonManagerRepository
          .findById(guid)
          .orElseThrow(() -> new ItemNotFoundException("Liaison manager not found"));
    }

    throw new IllegalArgumentException("Unsupported liaison manager request type: " + request);
  }

  private static UUID extractLiaisonManagerGuid(Object dto) {
    for (String methodName :
        new String[] {
          "getLiaisonManagerGUID",
          "getLiaisonManagerGuid",
          "liaisonManagerGUID",
          "liaisonManagerGuid"
        }) {
      UUID viaMethod = tryInvokeUuidGetter(dto, methodName);
      if (viaMethod != null) {
        return viaMethod;
      }
      UUID viaField = tryReadUuidField(dto, methodName);
      if (viaField != null) {
        return viaField;
      }
    }
    throw new IllegalArgumentException(
        "Could not extract liaison manager GUID from " + dto.getClass().getName());
  }

  private static UUID tryInvokeUuidGetter(Object dto, String methodName) {
    try {
      Method m = dto.getClass().getMethod(methodName);
      Object v = m.invoke(dto);
      return (UUID) v;
    } catch (NoSuchMethodException ignored) {
      return null;
    } catch (Exception e) {
      throw new IllegalArgumentException(
          "Failed calling " + methodName + " on " + dto.getClass().getName(), e);
    }
  }

  private static UUID tryReadUuidField(Object dto, String fieldName) {
    try {
      Field f = dto.getClass().getDeclaredField(fieldName);
      f.setAccessible(true);
      Object v = f.get(dto);
      return (UUID) v;
    } catch (NoSuchFieldException ignored) {
      return null;
    } catch (Exception e) {
      throw new IllegalArgumentException(
          "Failed reading field " + fieldName + " on " + dto.getClass().getName(), e);
    }
  }

  private static Optional<UUID> parseUuid(String value) {
    try {
      return Optional.of(UUID.fromString(value));
    } catch (Exception ignored) {
      return Optional.empty();
    }
  }
}
