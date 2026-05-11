package uk.gov.justice.laa.providerdata.provider;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.justice.laa.providerdata.model.ProviderFirmTypeV2;
import uk.gov.justice.laa.providerdata.provider.repository.ProviderFirmRepository;
import uk.gov.justice.laa.providerdata.provider.repository.ProviderParentLinkRepository;
import uk.gov.justice.laa.providerdata.provider.repository.ProviderRepository;
import uk.gov.justice.laa.providerdata.provider.repository.ProviderSpecification;
import uk.gov.justice.laa.providerdata.shared.FirmType;
import uk.gov.justice.laa.providerdata.shared.ItemNotFoundException;
import uk.gov.justice.laa.providerdata.shared.UuidUtils;

/** Service responsible for provider firm read operations. */
@Service("providerModuleQueryService")
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ProviderQueryService {

  private final ProviderRepository providerRepository;
  private final ProviderParentLinkRepository providerParentLinkRepository;
  private final ProviderFirmRepository providerFirmRepository;

  /** Returns whether a provider with the given firm number exists. */
  public boolean existsByFirmNumber(String firmNumber) {
    return providerRepository.findByFirmNumber(firmNumber).isPresent();
  }

  /** Returns the child links (practitioners) associated with the given parent provider. */
  public List<ProviderParentLinkEntity> getChildLinks(ProviderEntity parent) {
    return providerParentLinkRepository.findByParent(parent);
  }

  /**
   * Returns the provider matching the given GUID or firm number.
   *
   * @param providerFirmGUIDorFirmNumber UUID string (primary key) or firm number (unique key)
   * @return the matching {@link ProviderEntity}
   * @throws ItemNotFoundException if no provider matches the given identifier
   */
  public ProviderEntity getProvider(String providerFirmGUIDorFirmNumber) {
    var guid = UuidUtils.parseUuid(providerFirmGUIDorFirmNumber);
    return (guid.isPresent()
            ? providerRepository.findById(guid.get())
            : providerRepository.findByFirmNumber(providerFirmGUIDorFirmNumber))
        .orElseThrow(
            () -> new ItemNotFoundException("Provider not found: " + providerFirmGUIDorFirmNumber));
  }

  /**
   * Searches provider firms using optional filter criteria with pagination support.
   *
   * <p>Applies filters such as GUID, firm number, name, active status, and provider type. Only
   * non-null filters are considered, and all conditions are combined using AND logic.
   *
   * @param guids list of provider firm GUIDs to filter
   * @param firmNumbers list of provider firm numbers to filter
   * @param name provider name (partial match)
   * @param activeStatus active status filter
   * @param types list of provider firm types to filter
   * @param pageable pagination information
   * @return a paginated list of matching {@link ProviderEntity}
   */
  public Page<ProviderEntity> searchProviders(
      List<String> guids,
      List<String> firmNumbers,
      String name,
      String activeStatus,
      List<ProviderFirmTypeV2> types,
      Pageable pageable) {
    return providerFirmRepository.findAll(
        ProviderSpecification.filter(guids, firmNumbers, name, types), pageable);
  }

  /** Returns the parent firm links for the given provider (Advocates only). */
  public List<ProviderParentLinkEntity> getParentLinks(ProviderEntity provider) {
    return providerParentLinkRepository.findByProvider(provider);
  }

  /**
   * Returns a page of practitioners (Advocates) assigned to the given Chambers.
   *
   * @param chambersGUIDorFirmNumber Chambers GUID or firm number
   * @param pageable pagination information
   * @return page of {@link ProviderParentLinkEntity} representing the practitioners
   * @throws IllegalArgumentException if the identifier does not correspond to a Chambers
   * @throws ItemNotFoundException if no provider matches the given identifier
   */
  public Page<ProviderParentLinkEntity> getPractitionersByChambers(
      String chambersGUIDorFirmNumber, Pageable pageable) {
    ProviderEntity provider = getProvider(chambersGUIDorFirmNumber);
    if (!FirmType.CHAMBERS.equals(provider.getFirmType())) {
      throw new IllegalArgumentException("Provider is not a Chambers: " + chambersGUIDorFirmNumber);
    }
    return providerParentLinkRepository.findByParentOrderByProviderNameAsc(provider, pageable);
  }
}
