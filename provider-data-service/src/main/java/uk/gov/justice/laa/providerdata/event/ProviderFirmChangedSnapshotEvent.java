package uk.gov.justice.laa.providerdata.event;

import java.time.OffsetDateTime;
import java.util.UUID;
import uk.gov.justice.laa.providerdata.model.ProviderFirmChangedSnapshotEventV2Payload;

/**
 * Spring Modulith domain event published after every provider firm write operation.
 *
 * @param eventGuid unique identifier for this event occurrence
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
