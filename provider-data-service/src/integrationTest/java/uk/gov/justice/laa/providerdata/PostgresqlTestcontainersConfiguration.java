package uk.gov.justice.laa.providerdata;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Test configuration that provides the shared PostgreSQL Testcontainers service connection.
 *
 * <p>Importing this configuration registers a {@link PostgreSQLContainer} bean annotated with
 * {@link ServiceConnection}, allowing Spring Boot to autoconfigure the test datasource from the
 * container for full-context tests.
 */
@TestConfiguration(proxyBeanMethods = false)
public class PostgresqlTestcontainersConfiguration {

  @Bean
  @ServiceConnection
  PostgreSQLContainer postgresqlContainer() {
    return new PostgreSQLContainer("postgres:17-alpine");
  }
}
