package uk.gov.justice.laa.providerdata.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.flyway.autoconfigure.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Registers a {@link FlywayMigrationStrategy} that cleans the database before migrating. */
@Configuration
public class FlywayCleanMigrationStrategy {

  /**
   * Replaces the default migrate-only strategy with clean + migrate when {@code
   * spring.flyway.clean-on-start=true}. Requires {@code spring.flyway.clean-disabled=false}.
   */
  @Bean
  @ConditionalOnProperty("spring.flyway.clean-on-start")
  FlywayMigrationStrategy cleanOnStart() {
    return flyway -> {
      flyway.clean();
      flyway.migrate();
    };
  }
}
