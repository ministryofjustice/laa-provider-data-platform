package uk.gov.justice.laa.providerdata.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import uk.gov.justice.laa.providerdata.security.ApiKeyAuthenticationFilter;
import uk.gov.laa.springboot.auth.AuthenticationProperties;
import uk.gov.laa.springboot.auth.TokenDetailsManager;

/** Configures optional API key auth using the same starter components as PDA-r1. */
@Configuration
public class ApiKeyAuthenticationConfig {

  /**
   * Keep endpoints open when API key authentication is disabled.
   *
   * @param httpSecurity the security configurer
   * @return a permissive filter chain
   * @throws Exception when filter chain creation fails
   */
  @Bean
  @Order(Ordered.HIGHEST_PRECEDENCE)
  @ConditionalOnProperty(
      name = "app.security.apikey.enabled",
      havingValue = "false",
      matchIfMissing = true)
  public SecurityFilterChain permitAllSecurityFilterChain(HttpSecurity httpSecurity)
      throws Exception {
    httpSecurity
        .csrf(AbstractHttpConfigurer::disable)
        .httpBasic(AbstractHttpConfigurer::disable)
        .formLogin(AbstractHttpConfigurer::disable)
        .logout(AbstractHttpConfigurer::disable)
        .sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(authz -> authz.anyRequest().permitAll());
    return httpSecurity.build();
  }

  /** Enabled-mode API key chain using starter token parsing and role mapping components. */
  @Configuration
  @ConditionalOnProperty(name = "app.security.apikey.enabled", havingValue = "true")
  @EnableConfigurationProperties(AuthenticationProperties.class)
  static class EnabledApiKeyAuthenticationConfiguration {

    @Bean
    TokenDetailsManager tokenDetailsManager(AuthenticationProperties properties) {
      return new TokenDetailsManager(properties);
    }

    @Bean
    ApiKeyAuthenticationFilter apiKeyAuthenticationFilter(
        TokenDetailsManager tokenDetailsManager, tools.jackson.databind.ObjectMapper objectMapper) {
      return new ApiKeyAuthenticationFilter(tokenDetailsManager, objectMapper);
    }

    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE + 1)
    SecurityFilterChain apiKeySecurityFilterChain(
        HttpSecurity httpSecurity, ApiKeyAuthenticationFilter apiKeyAuthenticationFilter)
        throws Exception {
      httpSecurity
          .csrf(AbstractHttpConfigurer::disable)
          .httpBasic(AbstractHttpConfigurer::disable)
          .formLogin(AbstractHttpConfigurer::disable)
          .logout(AbstractHttpConfigurer::disable)
          .sessionManagement(
              session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
          .authorizeHttpRequests(authz -> authz.anyRequest().permitAll())
          .addFilterBefore(apiKeyAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
      return httpSecurity.build();
    }
  }
}
