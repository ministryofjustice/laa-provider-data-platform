package uk.gov.justice.laa.providerdata.application.outbox.model;

import java.util.UUID;

/** Application model representing an outbox event ready for dispatch. */
public record OutboxEventMessage(
    UUID guid, String eventType, String firmNumber, String payload, int attemptCount) {}

