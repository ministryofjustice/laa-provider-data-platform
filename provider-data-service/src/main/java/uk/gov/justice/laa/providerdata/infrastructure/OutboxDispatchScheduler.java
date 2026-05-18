package uk.gov.justice.laa.providerdata.infrastructure;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import uk.gov.justice.laa.providerdata.application.outbox.OutboxDispatcherUseCase;

/** Scheduler that periodically dispatches pending outbox events. */
@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxDispatchScheduler {

  private final OutboxDispatcherUseCase outboxDispatcherUseCase;

  @Scheduled(fixedDelayString = "${outbox.dispatcher.fixed-delay-ms:5000}")
  public void dispatchPendingOutboxEvents() {
    log.info("Scheduler tick: dispatching pending outbox events");
    outboxDispatcherUseCase.dispatchPendingEvents(100);
  }
}
