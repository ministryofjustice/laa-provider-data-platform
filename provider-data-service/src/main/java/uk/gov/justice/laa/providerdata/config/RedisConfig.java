package uk.gov.justice.laa.providerdata.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.JdkSerializationRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.scheduling.annotation.EnableAsync;
import uk.gov.justice.laa.providerdata.projection.ProviderFirmReadModel;

/**
 * Spring configuration for the Redis read store and async event dispatch.
 *
 * <p>{@link EnableAsync} activates {@link org.springframework.scheduling.annotation.Async} support,
 * required by {@link uk.gov.justice.laa.providerdata.projection.ProviderFirmProjector} to handle
 * projection updates off the write-transaction thread.
 *
 * <p>The {@link RedisTemplate} uses string keys and JDK serialisation for values. JDK serialisation
 * is chosen over JSON here because {@link ProviderFirmReadModel} is {@link java.io.Serializable}
 * and this avoids any Jackson 3 / Spring Data Redis compatibility shims.
 */
@Configuration
@EnableAsync
public class RedisConfig {

  /**
   * Produces a {@link RedisTemplate} typed for {@link ProviderFirmReadModel}, using string keys and
   * JDK-serialised values.
   *
   * @param connectionFactory Redis connection factory auto-configured by Spring Boot
   * @return the configured template
   */
  @Bean
  public RedisTemplate<String, ProviderFirmReadModel> providerFirmRedisTemplate(
      RedisConnectionFactory connectionFactory) {
    RedisTemplate<String, ProviderFirmReadModel> template = new RedisTemplate<>();
    template.setConnectionFactory(connectionFactory);
    template.setKeySerializer(new StringRedisSerializer());
    template.setHashKeySerializer(new StringRedisSerializer());
    JdkSerializationRedisSerializer valueSerializer = new JdkSerializationRedisSerializer();
    template.setValueSerializer(valueSerializer);
    template.setHashValueSerializer(valueSerializer);
    return template;
  }
}
