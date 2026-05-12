package uk.gov.justice.laa.providerdata;

import lombok.extern.slf4j.Slf4j;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;
import uk.gov.justice.laa.providerdata.usecase.ProviderFirmChangedSnapshotEvent;

/**
 * Logs each {@link ProviderFirmChangedSnapshotEvent} after the publishing transaction commits.
 *
 * <p>This listener uses Spring Modulith's {@link ApplicationModuleListener}, which wraps the
 * handler in a new transaction and marks the corresponding {@code event_publication} row as
 * complete once the method returns successfully.
 */
@Slf4j
@Component
public class DomainEventLogger {

  /**
   * Receives a published snapshot event and writes a structured INFO log entry.
   *
   * @param event the snapshot event
   */
  @ApplicationModuleListener
  public void on(ProviderFirmChangedSnapshotEvent event) {
    log.info(
        "ProviderFirmChangedSnapshotEvent published: eventGuid={} correlationId={} traceId={}"
            + " triggeredBy={} occurredAt={}",
        event.eventGuid(),
        event.correlationId(),
        event.traceId(),
        event.triggeredBy(),
        event.occurredAt());
  }
}
