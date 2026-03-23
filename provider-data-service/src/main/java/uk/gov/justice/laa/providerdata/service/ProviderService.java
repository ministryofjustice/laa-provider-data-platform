package uk.gov.justice.laa.providerdata.service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.justice.laa.providerdata.entity.AdvocateProviderOfficeLinkEntity;
import uk.gov.justice.laa.providerdata.entity.ChamberProviderOfficeLinkEntity;
import uk.gov.justice.laa.providerdata.entity.FirmType;
import uk.gov.justice.laa.providerdata.entity.LspProviderOfficeLinkEntity;
import uk.gov.justice.laa.providerdata.entity.ProviderEntity;
import uk.gov.justice.laa.providerdata.entity.ProviderParentLinkEntity;
import uk.gov.justice.laa.providerdata.exception.ItemNotFoundException;
import uk.gov.justice.laa.providerdata.repository.AdvocateProviderOfficeLinkRepository;
import uk.gov.justice.laa.providerdata.repository.ChamberProviderOfficeLinkRepository;
import uk.gov.justice.laa.providerdata.repository.LspProviderOfficeLinkRepository;
import uk.gov.justice.laa.providerdata.repository.ProviderParentLinkRepository;
import uk.gov.justice.laa.providerdata.repository.ProviderRepository;

/** Service responsible for provider firm read operations. */
@Service
@Transactional(readOnly = true)
public class ProviderService {

  private final ProviderRepository providerRepository;
  private final LspProviderOfficeLinkRepository lspProviderOfficeLinkRepository;
  private final ChamberProviderOfficeLinkRepository chamberProviderOfficeLinkRepository;
  private final AdvocateProviderOfficeLinkRepository advocateProviderOfficeLinkRepository;
  private final ProviderParentLinkRepository providerParentLinkRepository;

  /**
   * Inject dependencies.
   *
   * @param providerRepository to find provider firms
   * @param lspProviderOfficeLinkRepository to find LSP head office links
   * @param chamberProviderOfficeLinkRepository to find Chambers head office links
   * @param advocateProviderOfficeLinkRepository to find Advocate office links
   * @param providerParentLinkRepository to find Advocate parent links
   */
  public ProviderService(
      ProviderRepository providerRepository,
      LspProviderOfficeLinkRepository lspProviderOfficeLinkRepository,
      ChamberProviderOfficeLinkRepository chamberProviderOfficeLinkRepository,
      AdvocateProviderOfficeLinkRepository advocateProviderOfficeLinkRepository,
      ProviderParentLinkRepository providerParentLinkRepository) {
    this.providerRepository = providerRepository;
    this.lspProviderOfficeLinkRepository = lspProviderOfficeLinkRepository;
    this.chamberProviderOfficeLinkRepository = chamberProviderOfficeLinkRepository;
    this.advocateProviderOfficeLinkRepository = advocateProviderOfficeLinkRepository;
    this.providerParentLinkRepository = providerParentLinkRepository;
  }

  /**
   * Returns a single provider firm by GUID or firm number.
   *
   * @param providerFirmGUIDorFirmNumber UUID string (primary key) or firm number (unique key)
   * @return the matching {@link ProviderEntity}
   * @throws ItemNotFoundException if no provider matches the given identifier
   */
  public ProviderEntity getProvider(String providerFirmGUIDorFirmNumber) {
    try {
      UUID guid = UUID.fromString(providerFirmGUIDorFirmNumber);
      return providerRepository
          .findById(guid)
          .orElseThrow(
              () ->
                  new ItemNotFoundException("Provider not found: " + providerFirmGUIDorFirmNumber));
    } catch (IllegalArgumentException e) {
      return providerRepository
          .findByFirmNumber(providerFirmGUIDorFirmNumber)
          .orElseThrow(
              () ->
                  new ItemNotFoundException("Provider not found: " + providerFirmGUIDorFirmNumber));
    }
  }

  /** Returns the LSP head office link for the given provider, if one exists. */
  public Optional<LspProviderOfficeLinkEntity> getLspHeadOffice(ProviderEntity provider) {
    return lspProviderOfficeLinkRepository.findByProviderAndHeadOfficeFlagTrue(provider);
  }

  /** Returns the Chambers head office link for the given provider, if one exists. */
  public Optional<ChamberProviderOfficeLinkEntity> getChambersHeadOffice(ProviderEntity provider) {
    return chamberProviderOfficeLinkRepository.findByProviderAndHeadOfficeFlagTrue(provider);
  }

  /** Returns the Advocate office link for the given provider, if one exists. */
  public Optional<AdvocateProviderOfficeLinkEntity> getAdvocateOfficeLink(ProviderEntity provider) {
    return advocateProviderOfficeLinkRepository.findByProviderAndHeadOfficeFlagTrue(provider);
  }

  /** Returns the parent firm links for the given provider (Advocates only). */
  public List<ProviderParentLinkEntity> getParentLinks(ProviderEntity provider) {
    return providerParentLinkRepository.findByProvider(provider);
  }

  /**
   * Returns a list of practitioners (Advocates) assigned to the given Chambers.
   *
   * @param chambersGUIDorFirmNumber Chambers GUID or firm number
   * @return list of {@link ProviderParentLinkEntity} representing the practitioners
   * @throws IllegalArgumentException if the identifier does not correspond to a Chambers
   * @throws ItemNotFoundException if no provider matches the given identifier
   */
  public List<ProviderParentLinkEntity> getPractitionersByChambers(
      String chambersGUIDorFirmNumber) {
    ProviderEntity provider = getProvider(chambersGUIDorFirmNumber);

    if (!FirmType.CHAMBERS.equals(provider.getFirmType())) {
      throw new IllegalArgumentException(
          "Provider is not a Chambers: " + chambersGUIDorFirmNumber);
    }

    return providerParentLinkRepository.findByParentOrderByProviderNameAsc(provider);
  }
}
