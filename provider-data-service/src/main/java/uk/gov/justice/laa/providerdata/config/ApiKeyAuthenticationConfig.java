package uk.gov.justice.laa.providerdata.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;

/** Configures optional API key auth toggle behaviour. */
@Configuration
public class ApiKeyAuthenticationConfig {

  /** Disable Spring Security entirely when API key auth is turned off. */
  @Bean
  @ConditionalOnProperty(
      name = "app.security.apikey.enabled",
      havingValue = "false",
      matchIfMissing = true)
  public WebSecurityCustomizer permitAllWebSecurityCustomizer() {
    return web ->
        web.ignoring()
            .requestMatchers(PathPatternRequestMatcher.withDefaults().matcher("/**"));
  }
}
