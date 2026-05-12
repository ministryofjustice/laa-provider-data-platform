package uk.gov.justice.laa.providerdata.usecase;

import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;
import uk.gov.justice.laa.providerdata.model.ProviderFirmChangedSnapshotEventV2Payload;
import uk.gov.justice.laa.providerdata.provider.ProviderEntity;
import uk.gov.justice.laa.providerdata.usecase.repository.ProviderEventRepository;

/**
 * Assembles, persists, and publishes a {@link ProviderFirmChangedSnapshotEvent} after a provider
 * firm write operation.
 *
 * <p>Must be called within the calling transaction. The {@link ProviderEventEntity} is written and
 * the Spring event is published in the same transaction as the triggering write.
 */
@Component
@RequiredArgsConstructor
public class ProviderEventPublisher {

  private final ProviderSnapshotAssembler snapshotAssembler;
  private final ProviderEventRepository providerEventRepository;
  private final ApplicationEventPublisher applicationEventPublisher;
  private final JsonMapper objectMapper;

  /**
   * Assembles a snapshot of the provider firm's current state, persists a {@link
   * ProviderEventEntity}, and publishes a {@link ProviderFirmChangedSnapshotEvent}.
   *
   * @param provider the provider entity after the write has been applied
   * @param context correlation and trace identifiers from the incoming request
   */
  public void publishAfterWrite(ProviderEntity provider, EventContext context) {
    ProviderFirmChangedSnapshotEventV2Payload payload = snapshotAssembler.assemble(provider);

    // JacksonException extends RuntimeException in Jackson 3 — no checked exception handling needed
    String payloadJson = objectMapper.writeValueAsString(payload);

    UUID eventGuid = UUID.randomUUID();
    OffsetDateTime now = OffsetDateTime.now();
    String triggeredBy =
        provider.getLastUpdatedBy() != null ? provider.getLastUpdatedBy() : "system";

    ProviderEventEntity entity =
        ProviderEventEntity.builder()
            .guid(eventGuid)
            .version(0L)
            .createdBy(triggeredBy)
            .createdTimestamp(now)
            .lastUpdatedBy(triggeredBy)
            .lastUpdatedTimestamp(now)
            .eventType("ProviderFirmChangedSnapshotEvent")
            .eventSource("apiV2")
            .correlationId(context.correlationId())
            .traceId(context.traceId())
            .payload(payloadJson)
            .build();
    providerEventRepository.save(entity);

    ProviderFirmChangedSnapshotEvent event =
        new ProviderFirmChangedSnapshotEvent(
            eventGuid, context.correlationId(), context.traceId(), triggeredBy, now, payload);
    applicationEventPublisher.publishEvent(event);
  }
}
