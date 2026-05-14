package uk.gov.justice.laa.providerdata.event;

import org.jspecify.annotations.Nullable;

/**
 * Carries per-request correlation and trace identifiers for event publication.
 *
 * @param correlationId value of the incoming {@code x-correlation-id} header; may be {@code null}
 * @param traceId value of the incoming {@code traceparent} header; may be {@code null}
 */
public record EventContext(@Nullable String correlationId, @Nullable String traceId) {
  public static EventContext of(@Nullable String correlationId, @Nullable String traceId) {
    return new EventContext(correlationId, traceId);
  }

  public static EventContext empty() {
    return new EventContext(null, null);
  }
}
