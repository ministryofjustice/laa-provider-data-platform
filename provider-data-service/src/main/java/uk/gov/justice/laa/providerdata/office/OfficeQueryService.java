package uk.gov.justice.laa.providerdata.office;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.justice.laa.providerdata.office.repository.AdvocateProviderOfficeLinkRepository;
import uk.gov.justice.laa.providerdata.office.repository.ChamberProviderOfficeLinkRepository;
import uk.gov.justice.laa.providerdata.office.repository.LspProviderOfficeLinkRepository;
import uk.gov.justice.laa.providerdata.office.repository.ProviderOfficeLinkRepository;
import uk.gov.justice.laa.providerdata.provider.ProviderEntity;
import uk.gov.justice.laa.providerdata.provider.ProviderQueryService;
import uk.gov.justice.laa.providerdata.shared.ItemNotFoundException;
import uk.gov.justice.laa.providerdata.shared.UuidUtils;

/** Provides read-only queries for provider firm offices. */
@Service("officeModuleQueryService")
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class OfficeQueryService {

  private final ProviderQueryService providerQueryService;
  private final LspProviderOfficeLinkRepository lspProviderOfficeLinkRepository;
  private final ProviderOfficeLinkRepository providerOfficeLinkRepository;
  private final AdvocateProviderOfficeLinkRepository advocateProviderOfficeLinkRepository;
  private final ChamberProviderOfficeLinkRepository chamberProviderOfficeLinkRepository;

  /**
   * Returns a paginated page of LSP offices for the given provider.
   *
   * @param providerFirmGUIDorFirmNumber GUID or firm number identifying the parent provider.
   * @param pageable the page being requested.
   * @return page of {@link LspProviderOfficeLinkEntity} for the provider.
   * @throws ItemNotFoundException if no provider matches the given identifier.
   */
  public Page<LspProviderOfficeLinkEntity> getLspOffices(
      String providerFirmGUIDorFirmNumber, Pageable pageable) {
    ProviderEntity provider = findProvider(providerFirmGUIDorFirmNumber);
    return lspProviderOfficeLinkRepository.findByProvider(provider, pageable);
  }

  /**
   * Returns a paginated page of offices for the given provider, across all firm types.
   *
   * @param providerFirmGUIDorFirmNumber GUID or firm number identifying the provider.
   * @param pageable the page being requested.
   * @return page of {@link ProviderOfficeLinkEntity} for the provider.
   * @throws ItemNotFoundException if no provider matches the given identifier.
   */
  public Page<ProviderOfficeLinkEntity> getOffices(
      String providerFirmGUIDorFirmNumber, Pageable pageable) {
    ProviderEntity provider = findProvider(providerFirmGUIDorFirmNumber);
    return providerOfficeLinkRepository.findByProvider(provider, pageable);
  }

  /**
   * Returns a paginated page of offices across all providers, with optional filtering by office
   * GUID or account number.
   *
   * <p>When {@code officeGUIDs} and {@code officeCodes} are both empty, all offices are returned.
   * When either filter list is non-empty, only offices matching those GUIDs or codes are returned,
   * unless {@code allProviderOffices} is {@code true} — in which case all offices belonging to the
   * providers that own any matching office are returned instead.
   *
   * @param officeGUIDs list of office-link GUID strings to filter by; may be null or empty
   * @param officeCodes list of office account-number strings to filter by; may be null or empty
   * @param allProviderOffices when {@code true}, expand results to all offices of the matched
   *     providers
   * @param pageable the page being requested
   * @return a page of {@link ProviderOfficeLinkEntity}
   */
  public Page<ProviderOfficeLinkEntity> getOfficesGlobal(
      @Nullable List<String> officeGUIDs,
      @Nullable List<String> officeCodes,
      @Nullable Boolean allProviderOffices,
      Pageable pageable) {

    List<UUID> guids =
        officeGUIDs != null ? officeGUIDs.stream().map(UUID::fromString).toList() : List.of();
    List<String> codes = officeCodes != null ? officeCodes : List.of();

    if (guids.isEmpty() && codes.isEmpty()) {
      return providerOfficeLinkRepository.findAll(pageable);
    }

    if (Boolean.TRUE.equals(allProviderOffices)) {
      Collection<ProviderOfficeLinkEntity> matching =
          providerOfficeLinkRepository.findByGuidInOrAccountNumberIn(guids, codes);
      if (matching.isEmpty()) {
        return Page.empty(pageable);
      }
      Set<ProviderEntity> providers =
          matching.stream().map(ProviderOfficeLinkEntity::getProvider).collect(Collectors.toSet());
      return providerOfficeLinkRepository.findByProviderIn(providers, pageable);
    }

    return providerOfficeLinkRepository.findByGuidInOrAccountNumberIn(guids, codes, pageable);
  }

  /**
   * Returns a single LSP office by the {@link LspProviderOfficeLinkEntity} GUID or account number.
   *
   * <p>The {@code officeGUIDorCode} parameter is first tried as a UUID (the {@code
   * ProviderOfficeLinkEntity.guid}); if that fails it is treated as an account number.
   *
   * @param providerFirmGUIDorFirmNumber GUID or firm number identifying the parent provider
   * @param officeGUIDorCode {@link LspProviderOfficeLinkEntity} GUID or account number
   * @return the matching {@link LspProviderOfficeLinkEntity}
   * @throws ItemNotFoundException if no matching office is found
   */
  public LspProviderOfficeLinkEntity getLspOfficeLink(
      String providerFirmGUIDorFirmNumber, String officeGUIDorCode) {
    ProviderEntity provider = findProvider(providerFirmGUIDorFirmNumber);
    return findLspOfficeLink(provider, officeGUIDorCode)
        .orElseThrow(() -> new ItemNotFoundException("Office not found: " + officeGUIDorCode));
  }

  /**
   * Returns a single office by GUID or account number, regardless of firm type (LSP, Chambers, or
   * Advocate/Practitioner).
   *
   * <p>The {@code officeGUIDorCode} parameter is first tried as a UUID (the {@code
   * ProviderOfficeLinkEntity.guid}); if that fails it is treated as an account number.
   *
   * @param providerFirmGUIDorFirmNumber GUID or firm number identifying the parent provider
   * @param officeGUIDorCode {@link ProviderOfficeLinkEntity} GUID or account number
   * @return the matching {@link ProviderOfficeLinkEntity}
   * @throws ItemNotFoundException if no provider or office matches the given identifiers
   */
  public ProviderOfficeLinkEntity getProviderOfficeLink(
      String providerFirmGUIDorFirmNumber, String officeGUIDorCode) {
    ProviderEntity provider = findProvider(providerFirmGUIDorFirmNumber);
    return findProviderOfficeLink(provider, officeGUIDorCode)
        .orElseThrow(() -> new ItemNotFoundException("Office not found: " + officeGUIDorCode));
  }

  /**
   * Returns the office link for the given provider and office identifier, regardless of firm type.
   *
   * <p>The {@code officeGUIDorCode} parameter is first tried as a UUID (the {@code
   * ProviderOfficeLinkEntity.guid}); if that fails it is treated as an account number.
   *
   * @param provider the provider to look up the office for
   * @param officeGUIDorCode {@link ProviderOfficeLinkEntity} GUID or account number
   * @return the matching {@link ProviderOfficeLinkEntity}
   * @throws ItemNotFoundException if no matching office is found
   */
  public ProviderOfficeLinkEntity getProviderOfficeLink(
      ProviderEntity provider, String officeGUIDorCode) {
    return findProviderOfficeLink(provider, officeGUIDorCode)
        .orElseThrow(() -> new ItemNotFoundException("Office not found: " + officeGUIDorCode));
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

  /** Returns all active Advocate office links for the given provider. */
  public List<AdvocateProviderOfficeLinkEntity> getAdvocateOfficeLinks(ProviderEntity provider) {
    return advocateProviderOfficeLinkRepository.findByProvider(provider);
  }

  /** Returns {@code true} if the given Chambers provider has any active linked practitioners. */
  public boolean existsActivePractitionerForChambers(ProviderEntity chambers) {
    return advocateProviderOfficeLinkRepository.existsActivePractitionerForChambers(chambers);
  }

  /**
   * Returns all active non-head LSP office links for the given provider (used for cascade
   * deactivation).
   */
  public List<LspProviderOfficeLinkEntity> getLspActiveChildOffices(ProviderEntity provider) {
    return lspProviderOfficeLinkRepository
        .findByProviderAndHeadOfficeFlagFalseAndActiveDateToIsNull(provider);
  }

  /** Returns the head office link for the given provider across all firm types, if one exists. */
  public Optional<ProviderOfficeLinkEntity> getProviderHeadOfficeLink(ProviderEntity provider) {
    return providerOfficeLinkRepository.findByProviderAndHeadOfficeFlagTrue(provider);
  }

  /**
   * Returns the office link for the given provider and office identifier, if one exists. Returns
   * empty if the identifier matches neither a GUID nor an account number.
   */
  public Optional<ProviderOfficeLinkEntity> findProviderOfficeLink(
      ProviderEntity provider, String officeGUIDorCode) {
    return UuidUtils.parseUuid(officeGUIDorCode)
        .flatMap(guid -> providerOfficeLinkRepository.findByProviderAndGuid(provider, guid))
        .or(
            () ->
                providerOfficeLinkRepository.findByProviderAndAccountNumber(
                    provider, officeGUIDorCode));
  }

  private Optional<LspProviderOfficeLinkEntity> findLspOfficeLink(
      ProviderEntity provider, String officeGUIDorCode) {
    return UuidUtils.parseUuid(officeGUIDorCode)
        .flatMap(guid -> lspProviderOfficeLinkRepository.findByProviderAndGuid(provider, guid))
        .or(
            () ->
                lspProviderOfficeLinkRepository.findByProviderAndAccountNumber(
                    provider, officeGUIDorCode));
  }

  private ProviderEntity findProvider(String providerFirmGUIDorFirmNumber) {
    return providerQueryService.getProvider(providerFirmGUIDorFirmNumber);
  }
}
