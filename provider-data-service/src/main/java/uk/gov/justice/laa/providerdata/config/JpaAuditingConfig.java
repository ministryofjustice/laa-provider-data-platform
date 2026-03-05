package uk.gov.justice.laa.providerdata.config;

import java.time.OffsetDateTime;
import java.util.Optional;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.auditing.DateTimeProvider;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/** Configures Spring Data JPA auditing for all entities. */
@Configuration
@EnableJpaAuditing(dateTimeProviderRef = "auditingDateTimeProvider")
public class JpaAuditingConfig {

  /**
   * Provides the current auditor identifier for {@code @CreatedBy} and {@code @LastModifiedBy}
   * fields.
   *
   * <p>TODO: replace with Spring Security principal when authentication is implemented.
   *
   * @return an {@link AuditorAware} that always returns {@code "SYSTEM"}
   */
  @Bean
  public AuditorAware<String> auditorProvider() {
    return () -> Optional.of("SYSTEM");
  }

  /**
   * Provides the current date/time as {@link OffsetDateTime} for {@code @CreatedDate} and
   * {@code @LastModifiedDate} fields.
   *
   * @return a {@link DateTimeProvider} returning {@link OffsetDateTime#now()}
   */
  @Bean
  public DateTimeProvider auditingDateTimeProvider() {
    return () -> Optional.of(OffsetDateTime.now());
  }
}
