package uk.gov.justice.laa.providerdata.application.outbox;

/** Use case for dispatching pending outbox events to messaging infrastructure. */
public interface OutboxDispatcherUseCase {

  void dispatchPendingEvents(int batchSize);
}

