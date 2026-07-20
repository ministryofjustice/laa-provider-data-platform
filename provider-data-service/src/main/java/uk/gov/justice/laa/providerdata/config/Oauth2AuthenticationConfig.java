package uk.gov.justice.laa.providerdata.config;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AuthenticationManagerResolver;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.oauth2.server.resource.OAuth2ResourceServerConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;

/**
 * Enforces bearer-token authentication when OAuth2 is enabled without API key mode.
 *
 * <p>The API-key and OAuth2 starters both narrow their filter chains to requests that include
 * specific auth headers. Without this fallback chain, requests with no auth header can bypass both
 * chains when API key mode is disabled.
 */
@Configuration
public class Oauth2AuthenticationConfig {

  /**
   * Builds a fallback filter chain that requires a bearer token for all non-whitelisted routes when
   * OAuth2 mode is enabled without API-key mode.
   */
  @Bean
  @Order(Ordered.HIGHEST_PRECEDENCE + 2)
  @ConditionalOnProperty(name = "app.security.oauth2.enabled", havingValue = "true")
  @ConditionalOnProperty(
      name = "app.security.apikey.enabled",
      havingValue = "false",
      matchIfMissing = true)
  public SecurityFilterChain oauth2RequiredSecurityFilterChain(
      HttpSecurity http,
      ObjectProvider<AuthenticationManagerResolver<HttpServletRequest>> resolverProvider)
      throws Exception {
    AuthenticationManagerResolver<HttpServletRequest> resolver = resolverProvider.getIfAvailable();

    http.securityMatcher(PathPatternRequestMatcher.withDefaults().matcher("/**"))
        .csrf(AbstractHttpConfigurer::disable)
        .httpBasic(AbstractHttpConfigurer::disable)
        .formLogin(AbstractHttpConfigurer::disable)
        .logout(AbstractHttpConfigurer::disable)
        .authorizeHttpRequests(
            auth ->
                auth.requestMatchers(
                        PathPatternRequestMatcher.withDefaults().matcher("/error"),
                        PathPatternRequestMatcher.withDefaults().matcher("/actuator/**"))
                    .permitAll()
                    .anyRequest()
                    .authenticated())
        .oauth2ResourceServer(oauth2 -> configureResourceServer(oauth2, resolver));

    return http.build();
  }

  private static void configureResourceServer(
      OAuth2ResourceServerConfigurer<HttpSecurity> oauth2,
      AuthenticationManagerResolver<HttpServletRequest> resolver) {
    if (resolver != null) {
      oauth2.authenticationManagerResolver(resolver);
    } else {
      oauth2.jwt(Customizer.withDefaults());
    }
  }
}
