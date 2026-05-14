package uk.gov.justice.laa.providerdata.event;

import lombok.extern.slf4j.Slf4j;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

/** Logs each {@link ProviderFirmChangedSnapshotEvent} as a placeholder for SNS publication. */
@Slf4j
@Component
public class SnsEventPublicationListener {

  /**
   * Receives a published snapshot event and logs a placeholder for future SNS delivery.
   *
   * @param event the snapshot event
   */
  @ApplicationModuleListener
  public void on(ProviderFirmChangedSnapshotEvent event) {
    log.info(
        "ProviderFirmChangedSnapshotEvent [{}]: would publish to SNS here"
            + " (correlationId={} triggeredBy={} occurredAt={})",
        event.eventGuid(),
        event.correlationId(),
        event.triggeredBy(),
        event.occurredAt());
  }
}
