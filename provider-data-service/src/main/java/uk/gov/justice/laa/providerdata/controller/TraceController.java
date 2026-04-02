package uk.gov.justice.laa.providerdata.controller;

import io.micrometer.observation.ObservationRegistry;
import jakarta.annotation.Nullable;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/**
 * Trace endpoint to show that incoming trace context is propagated.
 *
 * <p>This controller will not be available in production deployments as it is conditional on the
 * `feature.trace-demo.enabled` application property (which is intended for local/test profiles).
 */
@Slf4j
@RestController
@ConditionalOnProperty(name = "feature.trace-demo.enabled", havingValue = "true")
public class TraceController {

  private static final int MAX_DEPTH = 10;

  private final ObservationRegistry observationRegistry;

  /**
   * The observation registry is injected because we have NOT added a dependency on
   * `spring-boot-starter-restclient`. If that dependency were added, then an injected
   * `RestClient.Builder` would have the `ObservationRegistry` already autoconfigured.
   */
  public TraceController(ObservationRegistry observationRegistry) {
    this.observationRegistry = observationRegistry;
  }

  /**
   * Recursive trace GET endpoint enabled for local development and integration testing only.
   *
   * @param depth How deep the recursive call chain should get (max 10).
   * @param traceparent Makes the trace context header available to the controller.
   * @return TraceResponse information with diagnostic information.
   */
  @GetMapping("/trace/{depth}")
  public TraceResponse trace(
      @PathVariable int depth,
      @RequestHeader(value = "traceparent", required = false) @Nullable String traceparent) {
    validateDepth(depth);
    String endpoint = "/trace/" + depth;
    log.info("Trace endpoint {} called", endpoint);

    if (depth == 1) {
      // recurse no more.
      return currentTrace(endpoint, traceparent, null);
    }

    TraceResponse downstream =
        Objects.requireNonNull(
            // Creates a Spring framework `RestClient` directly, so the shared `ObservationRegistry`
            // must be added explicitly to enable child spans and `traceparent` header propagation.
            // If `spring-boot-starter-restclient` is used later, then inject Spring Boot's own
            // `RestClient.Builder` where the registry gets autoconfigured.
            RestClient.builder()
                .observationRegistry(observationRegistry)
                .build()
                .get()
                .uri(
                    ServletUriComponentsBuilder.fromCurrentContextPath()
                        .path("/trace/{depth}")
                        .build(depth - 1)
                        .toString())
                .retrieve()
                .body(TraceResponse.class),
            "Downstream trace response must not be null");

    return currentTrace(endpoint, traceparent, downstream);
  }

  private static void validateDepth(int depth) {
    if (depth < 1 || depth > MAX_DEPTH) {
      throw new IllegalArgumentException("trace depth must be between 1 and " + MAX_DEPTH);
    }
  }

  private static TraceResponse currentTrace(
      String endpoint, @Nullable String traceparent, @Nullable TraceResponse downstream) {
    return new TraceResponse(
        endpoint, traceparent, MDC.get("traceId"), MDC.get("spanId"), downstream);
  }

  /** JSON response used by the trace endpoint. */
  public record TraceResponse(
      String endpoint,
      @Nullable String receivedTraceparent,
      @Nullable String traceId,
      @Nullable String spanId,
      @Nullable TraceResponse downstream) {}
}
