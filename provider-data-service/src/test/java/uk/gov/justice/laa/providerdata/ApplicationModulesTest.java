package uk.gov.justice.laa.providerdata;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

/** Verifies that Spring Modulith can analyse the application module structure. */
class ApplicationModulesTest {

  @Test
  void modulesVerify() {
    ApplicationModules.of(Application.class).verify();
  }
}
