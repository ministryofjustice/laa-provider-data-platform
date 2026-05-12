package uk.gov.justice.laa.providerdata.projection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import uk.gov.justice.laa.providerdata.entity.ProviderEntity;
import uk.gov.justice.laa.providerdata.event.ProviderFirmUpdatedEvent;
import uk.gov.justice.laa.providerdata.service.ProviderService;

/**
 * Listens for {@link ProviderFirmUpdatedEvent} and updates the Redis read store.
 *
 * <p>The projector runs {@link Async asynchronously} so that it does not block the write
 * transaction that published the event. Failures here are logged but do not propagate to callers;
 * the primary store (PostgreSQL) remains the source of truth, and a cache miss will fall back to a
 * direct database read.
 */
@Component
public class ProviderFirmProjector {

  private static final Logger log = LoggerFactory.getLogger(ProviderFirmProjector.class);

  private final ProviderService providerService;
  private final ProviderFirmReadStore readStore;

  /**
   * Inject dependencies.
   *
   * @param providerService to reload the full entity after an event
   * @param readStore the Redis-backed read store to write projections into
   */
  public ProviderFirmProjector(ProviderService providerService, ProviderFirmReadStore readStore) {
    this.providerService = providerService;
    this.readStore = readStore;
  }

  /**
   * Handles a {@link ProviderFirmUpdatedEvent} by reloading the entity from the primary store and
   * writing an updated projection to Redis.
   *
   * @param event the event carrying the provider firm GUID
   */
  @Async
  @EventListener
  public void onProviderFirmUpdated(ProviderFirmUpdatedEvent event) {
    try {
      ProviderEntity entity = providerService.getProvider(event.providerFirmGUID().toString());
      ProviderFirmReadModel projection =
          new ProviderFirmReadModel(
              entity.getGuid(), entity.getFirmNumber(), entity.getName(), entity.getFirmType());
      readStore.save(projection);
      log.debug(
          "Provider firm projection updated in read store: guid={}", event.providerFirmGUID());
    } catch (Exception ex) {
      log.error(
          "Failed to update provider firm projection for guid={}: {}",
          event.providerFirmGUID(),
          ex.getMessage(),
          ex);
    }
  }
}
