package uk.gov.justice.laa.providerdata.usecase;

import java.time.OffsetDateTime;
import java.util.UUID;
import uk.gov.justice.laa.providerdata.model.ProviderFirmChangedSnapshotEventV2Payload;

/**
 * Spring Modulith domain event published after every provider firm write operation.
 *
 * <p>Published via {@code ApplicationEventPublisher} within the use-case write transaction. Spring
 * Modulith serialises this event to {@code event_publication} (transient outbox) for future SQS/SNS
 * delivery. A corresponding {@link ProviderEventEntity} is written in the same transaction for the
 * permanent query API.
 *
 * @param eventGuid unique identifier for this event occurrence; also the {@link
 *     ProviderEventEntity} primary key
 * @param correlationId value of the incoming {@code x-correlation-id} request header (nullable)
 * @param traceId value of the incoming {@code traceparent} request header (nullable)
 * @param triggeredBy username of the authenticated user who made the write request
 * @param occurredAt timestamp of the write transaction
 * @param payload full denormalised provider firm snapshot
 */
public record ProviderFirmChangedSnapshotEvent(
    UUID eventGuid,
    String correlationId,
    String traceId,
    String triggeredBy,
    OffsetDateTime occurredAt,
    ProviderFirmChangedSnapshotEventV2Payload payload) {}
