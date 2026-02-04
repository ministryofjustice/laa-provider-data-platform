package uk.gov.justice.laa.providerdata.e2e;

import org.aeonbits.owner.Config;

/** Configuration for end-to-end tests. */
@Config.Sources({
  "classpath:${env}.properties", // e.g. staging.properties / prod.properties
  "classpath:staging.properties", // fallback if -Denv not set
  "system:properties", // -D overrides
  "system:env" // ENV vars (e.g. AUTH_TOKEN)
})
public interface E2eConfig extends Config {
  // --- API settings ---
  @Key("base.url")
  String baseUrl();

  @Key("base.path")
  String basePath();

  @Key("auth.token")
  String authToken();
}
