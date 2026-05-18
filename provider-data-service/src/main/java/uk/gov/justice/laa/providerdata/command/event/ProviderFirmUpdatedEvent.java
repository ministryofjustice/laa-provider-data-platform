package uk.gov.justice.laa.providerdata.command.event;

import java.time.OffsetDateTime;
import java.util.UUID;
import uk.gov.justice.laa.providerdata.model.ProviderPatchV2;

/**
 * Domain event published when a provider firm is successfully updated via a command.
 *
 * <p>Published synchronously within the originating transaction via Spring's
 * {@link org.springframework.context.ApplicationEventPublisher}. Consumed by
 * {@link uk.gov.justice.laa.providerdata.command.event.CommandAuditEventListener} after commit
 * to write a durable audit record.
 *
 * <p>Does not carry the full before/after state — audit listeners can reload from the database
 * if a full snapshot is required in a later phase.
 */
public record ProviderFirmUpdatedEvent(
    UUID providerFirmGUID,
    String firmNumber,
    String commandType,
    ProviderPatchV2 patch,
    OffsetDateTime occurredAt) {

  /** Convenience factory that stamps the event with the current time. */
  public static ProviderFirmUpdatedEvent of(
      UUID providerFirmGUID, String firmNumber, ProviderPatchV2 patch) {
    return new ProviderFirmUpdatedEvent(
        providerFirmGUID,
        firmNumber,
        "UpdateProviderFirm",
        patch,
        OffsetDateTime.now());
  }
}

