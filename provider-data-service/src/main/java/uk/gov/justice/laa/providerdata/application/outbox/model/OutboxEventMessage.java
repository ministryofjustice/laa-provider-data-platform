package uk.gov.justice.laa.providerdata.application.outbox.model;

import java.time.OffsetDateTime;
import java.util.UUID;

/** Application model representing an outbox event ready for dispatch. */
public record OutboxEventMessage(
    UUID guid,
    UUID aggregateId,
    String eventType,
    String firmNumber,
    String payload,
    int attemptCount,
    OffsetDateTime occurredAt) {}

