package uk.gov.justice.laa.providerdata.event;

import lombok.RequiredArgsConstructor;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;
import uk.gov.justice.laa.providerdata.entity.ProviderEventEntity;
import uk.gov.justice.laa.providerdata.repository.ProviderEventRepository;

/**
 * Persists a {@link ProviderEventEntity} to {@code provider_event} after each {@link
 * ProviderFirmChangedSnapshotEvent} is published.
 *
 * <p>Runs in its own transaction after the publishing transaction commits, backed by the Spring
 * Modulith {@code event_publication} outbox for at-least-once delivery.
 */
@Component
@RequiredArgsConstructor
public class ProviderEventPersistenceListener {

  private final ProviderEventRepository providerEventRepository;
  private final JsonMapper objectMapper;

  /**
   * Receives a published snapshot event and persists a permanent record to {@code provider_event}.
   *
   * @param event the snapshot event
   */
  @ApplicationModuleListener
  public void on(ProviderFirmChangedSnapshotEvent event) {
    // JacksonException extends RuntimeException in Jackson 3 — no checked exception handling needed
    String payloadJson = objectMapper.writeValueAsString(event.payload());

    ProviderEventEntity entity =
        ProviderEventEntity.builder()
            .guid(event.eventGuid())
            .version(0L)
            .createdBy(event.triggeredBy())
            .createdTimestamp(event.occurredAt())
            .lastUpdatedBy(event.triggeredBy())
            .lastUpdatedTimestamp(event.occurredAt())
            .eventType("ProviderFirmChangedSnapshotEvent")
            .eventSource("apiV2")
            .correlationId(event.correlationId())
            .traceId(event.traceId())
            .payload(payloadJson)
            .build();
    providerEventRepository.save(entity);
  }
}
