package uk.gov.justice.laa.providerdata.service;

import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import uk.gov.justice.laa.providerdata.entity.ProviderEntity;
import uk.gov.justice.laa.providerdata.event.EventContext;
import uk.gov.justice.laa.providerdata.event.ProviderFirmChangedSnapshotEvent;
import uk.gov.justice.laa.providerdata.model.ProviderFirmChangedSnapshotEventV2Payload;

/**
 * Assembles and publishes a {@link ProviderFirmChangedSnapshotEvent} after a provider firm write
 * operation.
 *
 * <p>Must be called within the calling transaction. Persistence of the event to {@code
 * provider_event} is handled by {@link
 * uk.gov.justice.laa.providerdata.event.ProviderEventPersistenceListener} after the transaction
 * commits.
 */
@Component
@RequiredArgsConstructor
public class ProviderEventPublisher {

  private final ProviderSnapshotAssembler snapshotAssembler;
  private final ApplicationEventPublisher applicationEventPublisher;

  /**
   * Assembles a snapshot of the provider firm's current state and publishes a {@link
   * ProviderFirmChangedSnapshotEvent}.
   *
   * @param provider the provider entity after the write has been applied
   * @param context correlation and trace identifiers from the incoming request
   */
  public void publishAfterWrite(ProviderEntity provider, EventContext context) {
    ProviderFirmChangedSnapshotEventV2Payload payload = snapshotAssembler.assemble(provider);

    UUID eventGuid = UUID.randomUUID();
    OffsetDateTime now = OffsetDateTime.now();
    String triggeredBy =
        provider.getLastUpdatedBy() != null ? provider.getLastUpdatedBy() : "system";

    ProviderFirmChangedSnapshotEvent event =
        new ProviderFirmChangedSnapshotEvent(
            eventGuid, context.correlationId(), context.traceId(), triggeredBy, now, payload);
    applicationEventPublisher.publishEvent(event);
  }
}
