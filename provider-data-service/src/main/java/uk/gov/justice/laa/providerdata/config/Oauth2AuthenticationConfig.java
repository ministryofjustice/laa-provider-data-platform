package uk.gov.justice.laa.providerdata.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManagerResolver;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.oauth2.server.resource.OAuth2ResourceServerConfigurer;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import uk.gov.justice.laa.providerdata.model.ErrorResponseError;

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
   * Custom AuthenticationEntryPoint that returns RFC 7807 ProblemDetail error responses for
   * authentication failures.
   */
  @Bean
  public AuthenticationEntryPoint oauth2AuthenticationEntryPoint(ObjectMapper objectMapper) {
    return (request, response, authException) ->
        handleAuthenticationException(response, authException, objectMapper);
  }

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
      ObjectProvider<AuthenticationManagerResolver<HttpServletRequest>> resolverProvider,
      AuthenticationEntryPoint entryPoint)
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
                        PathPatternRequestMatcher.withDefaults().matcher("/actuator/**"),
                        PathPatternRequestMatcher.withDefaults().matcher("/swagger-ui/**"),
                        PathPatternRequestMatcher.withDefaults().matcher("/swagger-ui.html"),
                        PathPatternRequestMatcher.withDefaults().matcher("/v3/api-docs"),
                        PathPatternRequestMatcher.withDefaults().matcher("/v3/api-docs/**"))
                    .permitAll()
                    .anyRequest()
                    .authenticated())
        .oauth2ResourceServer(oauth2 -> configureResourceServer(oauth2, resolver, entryPoint));

    return http.build();
  }

  private static void configureResourceServer(
      OAuth2ResourceServerConfigurer<HttpSecurity> oauth2,
      AuthenticationManagerResolver<HttpServletRequest> resolver,
      AuthenticationEntryPoint entryPoint) {
    if (resolver != null) {
      oauth2.authenticationManagerResolver(resolver);
    } else {
      oauth2.jwt(Customizer.withDefaults());
    }
    oauth2.authenticationEntryPoint(entryPoint);
  }

  private static void handleAuthenticationException(
      HttpServletResponse response, AuthenticationException ex, ObjectMapper objectMapper)
      throws IOException {
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);

    Map<String, Object> errorBody = new HashMap<>();
    errorBody.put("type", "about:blank");
    errorBody.put("title", "Unauthorized");
    errorBody.put("status", HttpServletResponse.SC_UNAUTHORIZED);
    errorBody.put("detail", "Authentication failed: " + ex.getMessage());
    errorBody.put("instance", null);
    errorBody.put("timestamp", DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(ZonedDateTime.now()));

    ErrorResponseError error = new ErrorResponseError();
    error.errorCode("P00UA");
    errorBody.put("error", error);

    response.getWriter().write(objectMapper.writeValueAsString(errorBody));
  }
}
