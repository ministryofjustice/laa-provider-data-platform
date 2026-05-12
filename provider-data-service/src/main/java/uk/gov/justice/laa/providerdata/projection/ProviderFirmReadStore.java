package uk.gov.justice.laa.providerdata.projection;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

/**
 * Redis-backed read store for provider firm projections.
 *
 * <p>Keyed by provider firm GUID ({@code provider-firm:{guid}}) and firm number ({@code
 * provider-firm:number:{firmNumber}}) for O(1) look-ups on either identifier. TTL is set to 1 hour;
 * the projector refreshes entries after every mutation event.
 */
@Repository
public class ProviderFirmReadStore {

  static final String KEY_PREFIX = "provider-firm:";
  static final String NUMBER_KEY_PREFIX = "provider-firm:number:";
  private static final long TTL_HOURS = 1L;

  private final RedisTemplate<String, ProviderFirmReadModel> redisTemplate;

  /**
   * Inject dependencies.
   *
   * @param redisTemplate typed Redis template for {@link ProviderFirmReadModel}
   */
  public ProviderFirmReadStore(RedisTemplate<String, ProviderFirmReadModel> redisTemplate) {
    this.redisTemplate = redisTemplate;
  }

  /**
   * Stores a provider firm read model, keyed by both GUID and firm number.
   *
   * @param model the read model to store
   */
  public void save(ProviderFirmReadModel model) {
    String guidKey = KEY_PREFIX + model.getGuid();
    String numberKey = NUMBER_KEY_PREFIX + model.getFirmNumber();
    redisTemplate.opsForValue().set(guidKey, model, TTL_HOURS, TimeUnit.HOURS);
    redisTemplate.opsForValue().set(numberKey, model, TTL_HOURS, TimeUnit.HOURS);
  }

  /**
   * Looks up a provider firm by GUID.
   *
   * @param guid provider firm UUID
   * @return the cached read model, or empty if not in cache
   */
  public Optional<ProviderFirmReadModel> findByGuid(UUID guid) {
    return Optional.ofNullable(redisTemplate.opsForValue().get(KEY_PREFIX + guid));
  }

  /**
   * Looks up a provider firm by firm number.
   *
   * @param firmNumber the firm number string
   * @return the cached read model, or empty if not in cache
   */
  public Optional<ProviderFirmReadModel> findByFirmNumber(String firmNumber) {
    return Optional.ofNullable(redisTemplate.opsForValue().get(NUMBER_KEY_PREFIX + firmNumber));
  }

  /**
   * Removes all cache entries for the given provider firm.
   *
   * @param guid provider firm UUID
   * @param firmNumber the firm number string
   */
  public void evict(UUID guid, String firmNumber) {
    redisTemplate.delete(KEY_PREFIX + guid);
    redisTemplate.delete(NUMBER_KEY_PREFIX + firmNumber);
  }
}
