package uk.gov.justice.laa.providerdata.entity;

/** Lifecycle status for an outbox event. */
public enum OutboxEventStatus {
  PENDING,
  SENT,
  FAILED
}

