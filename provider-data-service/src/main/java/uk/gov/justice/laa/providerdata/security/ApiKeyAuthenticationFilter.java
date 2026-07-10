package uk.gov.justice.laa.providerdata.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.ObjectMapper;
import uk.gov.laa.springboot.auth.TokenDetailsManager;

/** API key auth filter for enabled mode in PDA-r2, matching PDA-r1 behaviour. */
public class ApiKeyAuthenticationFilter extends OncePerRequestFilter {

  private static final Logger log = LoggerFactory.getLogger(ApiKeyAuthenticationFilter.class);

  private final TokenDetailsManager tokenDetailsManager;
  private final ObjectMapper objectMapper;

  public ApiKeyAuthenticationFilter(
      TokenDetailsManager tokenDetailsManager, ObjectMapper objectMapper) {
    this.tokenDetailsManager = tokenDetailsManager;
    this.objectMapper = objectMapper;
  }

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    return List.of(tokenDetailsManager.getUnprotectedUris()).stream()
        .anyMatch(uri -> PathPatternRequestMatcher.withDefaults().matcher(uri).matches(request));
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    String accessToken = request.getHeader(tokenDetailsManager.getAuthenticationHeader());
    if (accessToken == null || accessToken.isBlank()) {
      log.warn(
          "Authentication failure for {} {}: no API access token provided",
          request.getMethod(),
          request.getRequestURI());
      writeError(
          response,
          HttpServletResponse.SC_UNAUTHORIZED,
          "UNAUTHORIZED",
          "No API access token provided.");
      return;
    }

    if (!tokenDetailsManager.clientExistsForToken(accessToken)) {
      log.warn(
          "Authentication failure for {} {}: invalid API access token provided",
          request.getMethod(),
          request.getRequestURI());
      writeError(
          response,
          HttpServletResponse.SC_UNAUTHORIZED,
          "UNAUTHORIZED",
          "Invalid API access token provided.");
      return;
    }

    Authentication authentication = buildAuthentication(accessToken);
    if (!tokenDetailsManager.isRequestAuthorized(authentication.getAuthorities(), request)) {
      log.warn(
          "Authorisation failure for {} {}: access denied",
          request.getMethod(),
          request.getRequestURI());
      writeError(response, HttpServletResponse.SC_FORBIDDEN, "FORBIDDEN", "Access Denied");
      return;
    }

    SecurityContext context = SecurityContextHolder.createEmptyContext();
    context.setAuthentication(authentication);
    SecurityContextHolder.setContext(context);
    try {
      filterChain.doFilter(request, response);
    } finally {
      SecurityContextHolder.clearContext();
    }
  }

  private Authentication buildAuthentication(String accessToken) {
    Collection<? extends GrantedAuthority> authorities =
        tokenDetailsManager.getClientRoles(accessToken).stream()
            .map(role -> "ROLE_" + role)
            .map(SimpleGrantedAuthority::new)
            .toList();
    return UsernamePasswordAuthenticationToken.authenticated(
        tokenDetailsManager.getPrincipal(accessToken), accessToken, authorities);
  }

  private void writeError(HttpServletResponse response, int status, String error, String message)
      throws IOException {
    response.setStatus(status);
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    response
        .getWriter()
        .write(
            objectMapper.writeValueAsString(
                Map.of("status", status, "error", error, "message", message)));
  }
}
