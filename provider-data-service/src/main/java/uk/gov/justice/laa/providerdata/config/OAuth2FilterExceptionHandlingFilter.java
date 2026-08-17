package uk.gov.justice.laa.providerdata.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import uk.gov.justice.laa.providerdata.model.ErrorResponseError;

/**
 * Handles exceptions thrown in the OAuth2 authentication filter chain and converts them to proper
 * error responses.
 *
 * <p>Exceptions occurring before DispatcherServlet (e.g., JWKS resolution failures in
 * BearerTokenAuthenticationFilter) are not caught by GlobalExceptionHandler. This filter
 * intercepts them and returns RFC 7807 ProblemDetail responses formatted according to the API
 * spec.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
@ConditionalOnProperty(name = "app.security.oauth2.enabled", havingValue = "true")
@Slf4j
public class OAuth2FilterExceptionHandlingFilter extends OncePerRequestFilter {

  private final ObjectMapper objectMapper;

  public OAuth2FilterExceptionHandlingFilter(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    try {
      filterChain.doFilter(request, response);
    } catch (Exception ex) {
      log.error("Unhandled exception in OAuth2 filter chain: {}", ex.getMessage(), ex);
      handleFilterException(response, ex);
    }
  }

  private void handleFilterException(HttpServletResponse response, Exception ex)
      throws IOException {
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);

    Map<String, Object> errorBody = new HashMap<>();
    errorBody.put("type", "about:blank");
    errorBody.put("title", "Internal Server Error");
    errorBody.put("status", HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    errorBody.put("detail", "An unexpected error occurred during authentication processing.");
    errorBody.put("instance", null);
    errorBody.put("timestamp", DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(ZonedDateTime.now()));

    ErrorResponseError error = new ErrorResponseError();
    error.errorCode("P00SE");
    errorBody.put("error", error);

    response.getWriter().write(objectMapper.writeValueAsString(errorBody));
  }
}
