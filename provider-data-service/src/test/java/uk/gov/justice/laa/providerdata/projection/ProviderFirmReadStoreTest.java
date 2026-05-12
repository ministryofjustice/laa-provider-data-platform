package uk.gov.justice.laa.providerdata.projection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import uk.gov.justice.laa.providerdata.entity.FirmType;

/**
 * Unit tests for {@link ProviderFirmReadStore}.
 *
 * <p>Mocks {@link RedisTemplate} and its {@link ValueOperations} to test key construction, TTL
 * setting, and cache look-up logic without a running Redis instance.
 */
@ExtendWith(MockitoExtension.class)
class ProviderFirmReadStoreTest {

  @Mock private RedisTemplate<String, ProviderFirmReadModel> redisTemplate;
  @Mock private ValueOperations<String, ProviderFirmReadModel> valueOps;

  private ProviderFirmReadStore store;

  @BeforeEach
  void setUp() {
    store = new ProviderFirmReadStore(redisTemplate);
  }

  @Test
  void save_writesModelUnderGuidAndFirmNumberKeys() {
    when(redisTemplate.opsForValue()).thenReturn(valueOps);
    UUID guid = UUID.randomUUID();
    ProviderFirmReadModel model =
        new ProviderFirmReadModel(guid, "LSP-0001", "Test Firm", FirmType.LEGAL_SERVICES_PROVIDER);

    store.save(model);

    verify(valueOps)
        .set(eq(ProviderFirmReadStore.KEY_PREFIX + guid), eq(model), eq(1L), eq(TimeUnit.HOURS));
    verify(valueOps)
        .set(
            eq(ProviderFirmReadStore.NUMBER_KEY_PREFIX + "LSP-0001"),
            eq(model),
            eq(1L),
            eq(TimeUnit.HOURS));
  }

  @Test
  void findByGuid_returnsModel_whenPresent() {
    when(redisTemplate.opsForValue()).thenReturn(valueOps);
    UUID guid = UUID.randomUUID();
    ProviderFirmReadModel model =
        new ProviderFirmReadModel(guid, "LSP-0001", "Firm", FirmType.LEGAL_SERVICES_PROVIDER);
    when(valueOps.get(ProviderFirmReadStore.KEY_PREFIX + guid)).thenReturn(model);

    Optional<ProviderFirmReadModel> result = store.findByGuid(guid);

    assertThat(result).isPresent();
    assertThat(result.get().getGuid()).isEqualTo(guid);
    assertThat(result.get().getFirmNumber()).isEqualTo("LSP-0001");
  }

  @Test
  void findByGuid_returnsEmpty_whenAbsent() {
    when(redisTemplate.opsForValue()).thenReturn(valueOps);
    UUID guid = UUID.randomUUID();
    when(valueOps.get(ProviderFirmReadStore.KEY_PREFIX + guid)).thenReturn(null);

    Optional<ProviderFirmReadModel> result = store.findByGuid(guid);

    assertThat(result).isEmpty();
  }

  @Test
  void findByFirmNumber_returnsModel_whenPresent() {
    when(redisTemplate.opsForValue()).thenReturn(valueOps);
    UUID guid = UUID.randomUUID();
    ProviderFirmReadModel model =
        new ProviderFirmReadModel(guid, "CH-0001", "Chambers", FirmType.CHAMBERS);
    when(valueOps.get(ProviderFirmReadStore.NUMBER_KEY_PREFIX + "CH-0001")).thenReturn(model);

    Optional<ProviderFirmReadModel> result = store.findByFirmNumber("CH-0001");

    assertThat(result).isPresent();
    assertThat(result.get().getFirmNumber()).isEqualTo("CH-0001");
    assertThat(result.get().getFirmType()).isEqualTo(FirmType.CHAMBERS);
  }

  @Test
  void findByFirmNumber_returnsEmpty_whenAbsent() {
    when(redisTemplate.opsForValue()).thenReturn(valueOps);
    when(valueOps.get(ProviderFirmReadStore.NUMBER_KEY_PREFIX + "UNKNOWN")).thenReturn(null);

    Optional<ProviderFirmReadModel> result = store.findByFirmNumber("UNKNOWN");

    assertThat(result).isEmpty();
  }

  @Test
  void evict_deletesBothKeys() {
    UUID guid = UUID.randomUUID();

    store.evict(guid, "LSP-0001");

    verify(redisTemplate).delete(ProviderFirmReadStore.KEY_PREFIX + guid);
    verify(redisTemplate).delete(ProviderFirmReadStore.NUMBER_KEY_PREFIX + "LSP-0001");
  }
}
