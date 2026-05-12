package uk.gov.justice.laa.providerdata.service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import uk.gov.justice.laa.providerdata.entity.AdvocateProviderOfficeLinkEntity;
import uk.gov.justice.laa.providerdata.entity.ChamberProviderOfficeLinkEntity;
import uk.gov.justice.laa.providerdata.entity.LspProviderOfficeLinkEntity;
import uk.gov.justice.laa.providerdata.entity.ProviderEntity;
import uk.gov.justice.laa.providerdata.entity.ProviderParentLinkEntity;
import uk.gov.justice.laa.providerdata.mapper.ProviderMapper;
import uk.gov.justice.laa.providerdata.model.ProviderFirmTypeV2;
import uk.gov.justice.laa.providerdata.model.ProviderV2;
import uk.gov.justice.laa.providerdata.projection.ProviderFirmReadModel;
import uk.gov.justice.laa.providerdata.projection.ProviderFirmReadStore;
import uk.gov.justice.laa.providerdata.util.UuidUtils;

/**
 * Read-only service responsible for provider firm query operations.
 *
 * <p>Single-firm look-ups ({@link #getProviderFirm}) consult the Redis read store first and fall
 * back to the primary PostgreSQL database on a cache miss, populating the cache for subsequent
 * requests. List searches ({@link #searchProviderFirms}) always hit the database as Redis does not
 * hold a queryable index.
 *
 * <p>The read store is populated asynchronously by {@link
 * uk.gov.justice.laa.providerdata.projection.ProviderFirmProjector} after each write event.
 */
@Service
public class ProviderFirmQueryService {

  private static final Logger log = LoggerFactory.getLogger(ProviderFirmQueryService.class);

  private final ProviderService providerService;
  private final ProviderMapper providerMapper;
  private final ProviderFirmReadStore readStore;

  /**
   * Inject dependencies.
   *
   * @param providerService underlying provider entity read service (database)
   * @param providerMapper maps provider entities to response models
   * @param readStore Redis-backed read store for cached provider firm projections
   */
  public ProviderFirmQueryService(
      ProviderService providerService,
      ProviderMapper providerMapper,
      ProviderFirmReadStore readStore) {
    this.providerService = providerService;
    this.providerMapper = providerMapper;
    this.readStore = readStore;
  }

  /**
   * Returns a single provider firm as a {@link ProviderV2} read model.
   *
   * <p>Checks Redis first. On a cache miss the entity is loaded from PostgreSQL and the result is
   * written back to the read store before returning.
   *
   * @param providerFirmGUIDorFirmNumber UUID string (primary key) or firm number (unique key)
   * @return the matching read model
   * @throws uk.gov.justice.laa.providerdata.exception.ItemNotFoundException if no provider matches
   */
  public ProviderV2 getProviderFirm(String providerFirmGUIDorFirmNumber) {
    Optional<ProviderFirmReadModel> cached = lookupInCache(providerFirmGUIDorFirmNumber);
    if (cached.isPresent()) {
      log.debug("Cache hit for provider firm: {}", providerFirmGUIDorFirmNumber);
      return toReadModelFromProjection(cached.get());
    }

    log.debug(
        "Cache miss for provider firm: {}; loading from database", providerFirmGUIDorFirmNumber);
    ProviderEntity entity = providerService.getProvider(providerFirmGUIDorFirmNumber);
    warmCache(entity);
    return toReadModel(entity);
  }

  /**
   * Searches provider firms and returns a paginated page of {@link ProviderV2} read models.
   *
   * <p>Always queries the PostgreSQL primary store; Redis does not hold a queryable index suitable
   * for filtered, paginated searches.
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

  private Optional<ProviderFirmReadModel> lookupInCache(String guidOrFirmNumber) {
    try {
      Optional<UUID> uuid = UuidUtils.parseUuid(guidOrFirmNumber);
      if (uuid.isPresent()) {
        return readStore.findByGuid(uuid.get());
      }
      return readStore.findByFirmNumber(guidOrFirmNumber);
    } catch (Exception ex) {
      log.warn("Redis read failed for {}: {}", guidOrFirmNumber, ex.getMessage());
      return Optional.empty();
    }
  }

  private void warmCache(ProviderEntity entity) {
    try {
      readStore.save(
          new ProviderFirmReadModel(
              entity.getGuid(), entity.getFirmNumber(), entity.getName(), entity.getFirmType()));
    } catch (Exception ex) {
      log.warn("Failed to warm Redis cache for guid={}: {}", entity.getGuid(), ex.getMessage());
    }
  }

  /** Builds a full {@link ProviderV2} from an entity, including head office and parent links. */
  private ProviderV2 toReadModel(ProviderEntity entity) {
    return providerMapper.toProviderV2(
        entity,
        providerService.getLspHeadOffice(entity).orElse(null),
        providerService.getChambersHeadOffice(entity).orElse(null),
        providerService.getAdvocateOfficeLink(entity).orElse(null),
        providerService.getParentLinks(entity));
  }

  /**
   * Builds a {@link ProviderV2} from a cached {@link ProviderFirmReadModel}.
   *
   * <p>The projection holds only summary fields; office and parent link sub-objects are omitted. If
   * richer data is required from the cache, extend {@link ProviderFirmReadModel} and the projector
   * to store them.
   */
  private ProviderV2 toReadModelFromProjection(ProviderFirmReadModel projection) {
    ProviderFirmTypeV2 firmType =
        projection.getFirmType() != null
            ? ProviderFirmTypeV2.fromValue(projection.getFirmType())
            : null;
    return new ProviderV2()
        .guid(projection.getGuid())
        .firmNumber(projection.getFirmNumber())
        .name(projection.getName())
        .firmType(firmType);
  }
}
