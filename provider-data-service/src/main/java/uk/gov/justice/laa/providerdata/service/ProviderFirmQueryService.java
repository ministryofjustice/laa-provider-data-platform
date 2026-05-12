package uk.gov.justice.laa.providerdata.service;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import uk.gov.justice.laa.providerdata.entity.AdvocateProviderOfficeLinkEntity;
import uk.gov.justice.laa.providerdata.entity.ChamberProviderOfficeLinkEntity;
import uk.gov.justice.laa.providerdata.entity.LspProviderOfficeLinkEntity;
import uk.gov.justice.laa.providerdata.entity.ProviderEntity;
import uk.gov.justice.laa.providerdata.entity.ProviderParentLinkEntity;
import uk.gov.justice.laa.providerdata.model.ProviderFirmTypeV2;
import uk.gov.justice.laa.providerdata.model.ProviderV2;

/**
 * Read-only service responsible for provider firm query operations.
 *
 * <p>Delegates to {@link ProviderService} and produces {@link ProviderV2} read models via the
 * mapper. Consumers should prefer this service over {@link ProviderService} for new read
 * operations, as it enforces the query/command separation and returns API-typed read models.
 */
@Service
public class ProviderFirmQueryService {

  private final ProviderService providerService;
  private final uk.gov.justice.laa.providerdata.mapper.ProviderMapper providerMapper;

  /**
   * Inject dependencies.
   *
   * @param providerService underlying provider entity read service
   * @param providerMapper maps provider entities to response models
   */
  public ProviderFirmQueryService(
      ProviderService providerService,
      uk.gov.justice.laa.providerdata.mapper.ProviderMapper providerMapper) {
    this.providerService = providerService;
    this.providerMapper = providerMapper;
  }

  /**
   * Returns a single provider firm as a {@link ProviderV2} read model.
   *
   * @param providerFirmGUIDorFirmNumber UUID string (primary key) or firm number (unique key)
   * @return the matching read model
   * @throws uk.gov.justice.laa.providerdata.exception.ItemNotFoundException if no provider matches
   */
  public ProviderV2 getProviderFirm(String providerFirmGUIDorFirmNumber) {
    ProviderEntity entity = providerService.getProvider(providerFirmGUIDorFirmNumber);
    return toReadModel(entity);
  }

  /**
   * Searches provider firms and returns a paginated page of {@link ProviderV2} read models.
   *
   * @param guids optional list of GUIDs to filter by
   * @param firmNumbers optional list of firm numbers to filter by
   * @param name optional name (partial match) to filter by
   * @param types optional list of firm types to filter by
   * @param pageable pagination information
   * @return page of {@link ProviderV2} read models
   */
  public Page<ProviderV2> searchProviderFirms(
      List<String> guids,
      List<String> firmNumbers,
      String name,
      List<ProviderFirmTypeV2> types,
      Pageable pageable) {

    return providerService
        .searchProviders(guids, firmNumbers, name, null, types, pageable)
        .map(this::toReadModel);
  }

  /**
   * Returns the LSP head office link for the given provider entity.
   *
   * @param entity provider entity
   * @return optional LSP head office link
   */
  public Optional<LspProviderOfficeLinkEntity> getLspHeadOffice(ProviderEntity entity) {
    return providerService.getLspHeadOffice(entity);
  }

  /**
   * Returns the Chambers head office link for the given provider entity.
   *
   * @param entity provider entity
   * @return optional Chambers head office link
   */
  public Optional<ChamberProviderOfficeLinkEntity> getChambersHeadOffice(ProviderEntity entity) {
    return providerService.getChambersHeadOffice(entity);
  }

  /**
   * Returns the Advocate office link for the given provider entity.
   *
   * @param entity provider entity
   * @return optional Advocate office link
   */
  public Optional<AdvocateProviderOfficeLinkEntity> getAdvocateOfficeLink(ProviderEntity entity) {
    return providerService.getAdvocateOfficeLink(entity);
  }

  /**
   * Returns the parent firm links for the given provider entity.
   *
   * @param entity provider entity
   * @return list of parent links (empty for non-practitioners)
   */
  public List<ProviderParentLinkEntity> getParentLinks(ProviderEntity entity) {
    return providerService.getParentLinks(entity);
  }

  private ProviderV2 toReadModel(ProviderEntity entity) {
    return providerMapper.toProviderV2(
        entity,
        providerService.getLspHeadOffice(entity).orElse(null),
        providerService.getChambersHeadOffice(entity).orElse(null),
        providerService.getAdvocateOfficeLink(entity).orElse(null),
        providerService.getParentLinks(entity));
  }
}
