package uk.gov.justice.laa.providerdata;

import org.junit.jupiter.api.Test;

/**
 * This test has to be in the `integrationTest` source-set as loading the Spring Boot
 * `ApplicationContext` which includes the Spring Data JPA repositories and so on.
 */
class ApplicationTest extends PostgresqlSpringBootTest {

  @Test
  void contextLoads() {
    // Verifies Spring context starts successfully
  }
}
