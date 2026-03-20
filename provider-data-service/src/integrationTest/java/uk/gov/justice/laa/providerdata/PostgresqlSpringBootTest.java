package uk.gov.justice.laa.providerdata;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

/**
 * Base class for Spring-backed tests that need the shared PostgreSQL Testcontainers setup.
 *
 * <p>Tests apply this shared setup by extending this base class. Doing so starts a full {@link
 * SpringBootTest} with the {@code test} profile and imports {@link
 * PostgresqlTestcontainersConfiguration}, which provides the container-backed datasource used by
 * repository and integration tests.
 */
@SpringBootTest
@ActiveProfiles("test")
@Import(PostgresqlTestcontainersConfiguration.class)
public abstract class PostgresqlSpringBootTest {}
