package uk.gov.justice.laa.providerdata.config;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import uk.gov.justice.laa.providerdata.entity.FirmType;

/**
 * Configuration for application-level Prometheus metrics.
 *
 * <p>Defines counters and timers exposed via the {@code /actuator/prometheus} endpoint for
 * monitoring provider and office creation operations, including latency and failure rates.
 */
@Configuration
public class MetricsConfiguration {

  /**
   * Creates a counter for LSP firm creation events.
   *
   * @param meterRegistry the configured meter registry
   * @return a counter incremented for each LSP firm creation
   */
  @Bean
  public Counter lspFirmCreationCounter(MeterRegistry meterRegistry) {
    return Counter.builder("provider.creation.total")
        .description("Total number of provider firms created")
        .tag("firmware_type", FirmType.LEGAL_SERVICES_PROVIDER)
        .tag("application", "provider-data-platform")
        .register(meterRegistry);
  }

  /**
   * Creates a counter for Chambers firm creation events.
   *
   * @param meterRegistry the configured meter registry
   * @return a counter incremented for each Chambers firm creation
   */
  @Bean
  public Counter chambersFirmCreationCounter(MeterRegistry meterRegistry) {
    return Counter.builder("provider.creation.total")
        .description("Total number of provider firms created")
        .tag("firmware_type", FirmType.CHAMBERS)
        .tag("application", "provider-data-platform")
        .register(meterRegistry);
  }

  /**
   * Creates a counter for Practitioner (Advocate) firm creation events.
   *
   * @param meterRegistry the configured meter registry
   * @return a counter incremented for each Practitioner firm creation
   */
  @Bean
  public Counter practitionerFirmCreationCounter(MeterRegistry meterRegistry) {
    return Counter.builder("provider.creation.total")
        .description("Total number of provider firms created")
        .tag("firmware_type", FirmType.ADVOCATE)
        .tag("application", "provider-data-platform")
        .register(meterRegistry);
  }

  /**
   * Creates a timer for LSP firm creation latency.
   *
   * @param meterRegistry the configured meter registry
   * @return a timer for LSP firm creation operations
   */
  @Bean
  public Timer lspFirmCreationTimer(MeterRegistry meterRegistry) {
    return Timer.builder("provider.creation.duration")
        .description("Latency of provider firm creation operations")
        .tag("firmware_type", FirmType.LEGAL_SERVICES_PROVIDER)
        .tag("application", "provider-data-platform")
        .register(meterRegistry);
  }

  /**
   * Creates a timer for Chambers firm creation latency.
   *
   * @param meterRegistry the configured meter registry
   * @return a timer for Chambers firm creation operations
   */
  @Bean
  public Timer chambersFirmCreationTimer(MeterRegistry meterRegistry) {
    return Timer.builder("provider.creation.duration")
        .description("Latency of provider firm creation operations")
        .tag("firmware_type", FirmType.CHAMBERS)
        .tag("application", "provider-data-platform")
        .register(meterRegistry);
  }

  /**
   * Creates a timer for Practitioner firm creation latency.
   *
   * @param meterRegistry the configured meter registry
   * @return a timer for Practitioner firm creation operations
   */
  @Bean
  public Timer practitionerFirmCreationTimer(MeterRegistry meterRegistry) {
    return Timer.builder("provider.creation.duration")
        .description("Latency of provider firm creation operations")
        .tag("firmware_type", FirmType.ADVOCATE)
        .tag("application", "provider-data-platform")
        .register(meterRegistry);
  }

  /**
   * Creates a counter for office creation events.
   *
   * @param meterRegistry the configured meter registry
   * @return a counter incremented for each office creation
   */
  @Bean
  public Counter officeCreationCounter(MeterRegistry meterRegistry) {
    return Counter.builder("office.creation.total")
        .description("Total number of offices created")
        .tag("application", "provider-data-platform")
        .register(meterRegistry);
  }

  /**
   * Creates a timer for office creation latency.
   *
   * @param meterRegistry the configured meter registry
   * @return a timer for office creation operations
   */
  @Bean
  public Timer officeCreationTimer(MeterRegistry meterRegistry) {
    return Timer.builder("office.creation.duration")
        .description("Latency of office creation operations")
        .tag("application", "provider-data-platform")
        .register(meterRegistry);
  }

  /**
   * Creates a counter for bank account creation events.
   *
   * @param meterRegistry the configured meter registry
   * @return a counter incremented for each bank account creation
   */
  @Bean
  public Counter bankAccountCreationCounter(MeterRegistry meterRegistry) {
    return Counter.builder("bank.account.creation.total")
        .description("Total number of bank accounts created")
        .tag("application", "provider-data-platform")
        .register(meterRegistry);
  }

  /**
   * Creates a timer for bank account creation latency.
   *
   * @param meterRegistry the configured meter registry
   * @return a timer for bank account creation operations
   */
  @Bean
  public Timer bankAccountCreationTimer(MeterRegistry meterRegistry) {
    return Timer.builder("bank.account.creation.duration")
        .description("Latency of bank account creation operations")
        .tag("application", "provider-data-platform")
        .register(meterRegistry);
  }

  /**
   * Creates a counter for bank account link events.
   *
   * @param meterRegistry the configured meter registry
   * @return a counter incremented for each bank account link operation
   */
  @Bean
  public Counter bankAccountLinkCounter(MeterRegistry meterRegistry) {
    return Counter.builder("bank.account.link.total")
        .description("Total number of bank accounts linked")
        .tag("application", "provider-data-platform")
        .register(meterRegistry);
  }

  /**
   * Creates a timer for bank account link latency.
   *
   * @param meterRegistry the configured meter registry
   * @return a timer for bank account link operations
   */
  @Bean
  public Timer bankAccountLinkTimer(MeterRegistry meterRegistry) {
    return Timer.builder("bank.account.link.duration")
        .description("Latency of bank account link operations")
        .tag("application", "provider-data-platform")
        .register(meterRegistry);
  }
}
