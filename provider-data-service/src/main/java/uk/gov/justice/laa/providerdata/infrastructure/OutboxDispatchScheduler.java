package uk.gov.justice.laa.providerdata.infrastructure;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import uk.gov.justice.laa.providerdata.application.outbox.OutboxDispatcherUseCase;

/** Scheduler that periodically dispatches pending outbox events. */
@Component
@RequiredArgsConstructor
public class OutboxDispatchScheduler {

  private final OutboxDispatcherUseCase outboxDispatcherUseCase;

  @Scheduled(fixedDelayString = "${outbox.dispatcher.fixed-delay-ms:5000}")
  public void dispatchPendingOutboxEvents() {
    outboxDispatcherUseCase.dispatchPendingEvents(100);
  }
}

