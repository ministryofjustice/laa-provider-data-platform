package uk.gov.justice.laa.providerdata.pact; // TODO: <base-package>.pact

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import uk.gov.justice.laa.providerdata.service.ProviderLiaisonManagerService;

/**
 * Base for provider verification: boots the Spring app with NO database and every collaborator
 * mocked, so each @State handler just stubs the relevant service.
 *
 * <p>Add a @MockitoBean for EVERY service + repository + infra bean your app wires at startup —
 * otherwise the context fails to load. Mirror the set your production @Configuration needs.
 */
@EnableAutoConfiguration(exclude = {DataSourceAutoConfiguration.class})
public abstract class AbstractProviderPactTests {

  // --- TODO: one @MockitoBean per service the controllers depend on ---
  @MockitoBean protected ProviderLiaisonManagerService service;

  // --- TODO: one @MockitoBean per repository ---
  // @MockitoBean protected ExampleRepository exampleRepository;

  // --- infra beans that would otherwise need a real DataSource ---
  // @MockitoBean protected org.flywaydb.core.Flyway flyway;
  // @MockitoBean protected jakarta.persistence.EntityManagerFactory entityManagerFactory;
  // @MockitoBean protected javax.sql.DataSource dataSource;
}
