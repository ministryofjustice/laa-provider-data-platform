# OUTBOX Pattern in LAA Provider Data Platform

## Overview

The OUTBOX pattern is a transactional outbox implementation used in the LAA Provider Data Platform to solve the **dual-write problem** — ensuring that domain events are reliably delivered to downstream consumers without losing data if the message broker is temporarily unavailable.

## CQRS workshop

As part of a CQRS workshop, outbox was explored with a message broker (SNS) to see how it would integrate into PDP and its value.

## The Problem It Solves

When a service needs to both:

1. Save a domain entity to the database
2. Publish an event to a message broker

Either operation can fail, and they cannot be wrapped in a single atomic distributed transaction. The outbox pattern eliminates this risk by guaranteeing at-least-once delivery.

### Without OUTBOX (Direct Publish)

```
1. Save entity to database ✓
2. Publish event to broker ✗ (broker is down)
→ Entity change is persisted, but consumers never learn about it
```

### With OUTBOX (This Project)

```
1. Save entity to database      }
2. Write event to OUTBOX table   } same transaction
→ Both succeed or both fail (atomic)

[async] Background job:
1. Read pending events from OUTBOX
2. Publish to broker
3. Mark as SENT in OUTBOX
```

## Implementation in This Project

### Database Schema

The `OUTBOX_EVENT` table stores durable event records alongside domain entities:

```sql
CREATE TABLE OUTBOX_EVENT (
    GUID UUID NOT NULL DEFAULT gen_random_uuid(),
    VERSION BIGINT,
    CREATED_BY VARCHAR(255),
    CREATED_TIMESTAMP TIMESTAMPTZ,
    LAST_UPDATED_BY VARCHAR(255),
    LAST_UPDATED_TIMESTAMP TIMESTAMPTZ,
    AGGREGATE_TYPE VARCHAR(100) NOT NULL,      -- e.g. "ProviderFirm"
    AGGREGATE_ID UUID NOT NULL,                 -- ID of the entity that changed
    EVENT_TYPE VARCHAR(100) NOT NULL,          -- e.g. "ProviderFirmUpdated"
    FIRM_NUMBER VARCHAR(50) NOT NULL,          -- Contextual data
    EVENT_PAYLOAD TEXT NOT NULL,               -- Change details as JSON
    STATUS VARCHAR(20) NOT NULL,               -- PENDING, SENT, or FAILED
    ATTEMPT_COUNT INTEGER NOT NULL DEFAULT 0, -- Retry counter
    OCCURRED_AT TIMESTAMPTZ NOT NULL,          -- When the event occurred
    SENT_AT TIMESTAMPTZ,                       -- When successfully published
    LAST_ERROR VARCHAR(1000),                  -- Error message if failed
    CONSTRAINT pk_outbox_event PRIMARY KEY (GUID)
);

CREATE INDEX idx_outbox_event_status_occurred_at ON OUTBOX_EVENT (STATUS, OCCURRED_AT);
CREATE INDEX idx_outbox_event_aggregate ON OUTBOX_EVENT (AGGREGATE_ID, OCCURRED_AT);
```

### Core Components

#### OutboxEventEntity (JPA Entity)

The domain object representing an event written to the outbox:

```java
@Entity
@Table(name = "OUTBOX_EVENT")
public class OutboxEventEntity extends AuditableEntity {
    private String aggregateType;      // Type of aggregate that changed
    private UUID aggregateId;          // ID of the aggregate
    private String eventType;          // Name of the event
    private String firmNumber;         // Related firm
    private String eventPayload;       // Event data as JSON
    private OutboxEventStatus status;  // PENDING | SENT | FAILED
    private Integer attemptCount;      // Number of publish attempts
    private OffsetDateTime occurredAt; // Event occurrence timestamp
    private OffsetDateTime sentAt;     // Publishing timestamp
    private String lastError;          // Last error message
}
```

#### OutboxEventStatus (Enum)

Tracks the lifecycle of an event:

- **PENDING**: Written to database, not yet published to broker
- **SENT**: Successfully published to broker
- **FAILED**: Publishing failed and exceeded retry limit

#### Persistence Adapters

##### ProviderFirmUpdatedOutboxPersistenceAdapter

This adapter writes provider update events to the outbox:

- Writes provider update events to the outbox
- Called during the same transaction as entity updates
- Builds a payload summarising which fields changed
- Stores with status = PENDING

##### OutboxEventStorePersistenceAdapter

This adapter manages the lifecycle of outbox events:

- Reads pending events for publication (implements OutboxEventStorePort)
- Fetches up to 100 pending events ordered by occurrence time
- Marks events as SENT when successfully published
- Marks events as FAILED when publishing fails
- Updates attempt count and error message

#### Repository

```java
public interface OutboxEventRepository extends JpaRepository<OutboxEventEntity, UUID> {
  List<OutboxEventEntity> findByStatusOrderByOccurredAtAsc(OutboxEventStatus status);
  List<OutboxEventEntity> findTop100ByStatusOrderByOccurredAtAsc(OutboxEventStatus status);
}
```

## Key Benefits

### Guaranteed Delivery

Events written to the database within a transaction are guaranteed to reach consumers eventually, even if the broker is temporarily unavailable. The background job will retry.

### No Distributed Transactions

Both the domain entity and the outbox event are written to the same database in a single transaction. No need for complex XA/2-phase commits with the message broker.

### Transactional Consistency

Entity changes and event publishing are atomic — either both occur or both are rolled back.

### Auditability

The outbox table serves as a persistent event log with timestamps, retry attempts, and error information. Combined with Hibernate Envers, it provides a complete audit trail.

### At-Least-Once Delivery Semantics

Events may be delivered more than once (for example, if the background job crashes after publishing but before marking SENT). Consumers must be idempotent.

### Decoupling of Concerns

The API response is independent of the broker's availability — entity saves and event publishing are decoupled. A slow or unavailable broker doesn't block API requests.

## How It Works

### During Request Processing

```
1. HTTP request arrives (e.g. PATCH /provider-firms/{id})
2. Use case saves the entity to the database
3. Same use case writes an outbox event (status = PENDING)
4. Transaction commits
→ API returns 200 OK immediately
```

### Asynchronous Publishing (Background Job)

```
[async] Background job (polling or triggered):
1. Query: SELECT * FROM OUTBOX_EVENT WHERE STATUS='PENDING' LIMIT 100
2. For each event:
   a. Serialize event payload
   b. Publish to message broker
   c. Update: STATUS = 'SENT', SENT_AT = now()
   d. On failure: STATUS = 'FAILED', LAST_ERROR = error message
```

## Lifecycle of an Event

The outbox event follows a simple state machine:

```
                    ┌──────────────┐
                    │   PENDING    │
                    │  (in OUTBOX) │
                    └──────┬───────┘
                           │
                   Publishing attempt
                           │
                ┌──────────┴──────────┐
                ▼                     ▼
         [Success]             [Failure]
             │                    │
        ┌────▼────┐         ┌─────▼──────┐
        │   SENT   │         │   FAILED   │
        │         │          │  (retry or│
        └─────────┘          │   manual)  │
                             └────────────┘
```

## Example: ProviderFirmUpdated Event

When a provider firm is updated via `PATCH /provider-firms/{id}`:

**Step 1: Request arrives**

An HTTP request is received with updated fields (name, legal status, etc.).

**Step 2: Entity is persisted**

The entity is saved to the PROVIDER_FIRM table.

**Step 3: Outbox event is written**

An event is written to the OUTBOX_EVENT table with:

- `aggregateType`: "ProviderFirm"
- `aggregateId`: UUID of the firm
- `eventType`: "ProviderFirmUpdated"
- `eventPayload`: Summary of changed fields (e.g., "name,legalServicesProvider")
- `status`: PENDING
- `occurredAt`: Current timestamp

**Step 4: Transaction commits**

Both entity and event are persisted atomically.

**Step 5: Response sent**

The service returns 200 OK to the client immediately.

**Step 6: Background job publishes**

Eventually, the background job:

- Fetches the PENDING event
- Publishes to message broker
- Marks as SENT if successful, FAILED otherwise

## Advantages Over Alternatives

### vs. Direct Publish

| Aspect | Direct Publish | OUTBOX |
|--------|---|---|
| Guaranteed delivery | ✗ No | ✓ Yes |
| Decouples API from broker | ✗ No | ✓ Yes |
| Requires background job | ✗ No | ✓ Yes |
| Latency | ✓ Low | ✗ Higher |

### vs. Polling Publisher (without dedicated outbox table)

| Aspect | Generic polling | Dedicated OUTBOX |
|--------|---|---|
| Provides audit trail | ✗ No | ✓ Yes |
| Performance-optimized | ✗ No | ✓ Yes |
| Explicit lifecycle tracking | ✗ No | ✓ Yes |

### vs. Event Sourcing

| Aspect | OUTBOX | Event Sourcing |
|--------|---|---|
| Simplicity | ✓ Yes | ✗ No |
| Query flexibility | ✓ Yes | ✗ No |
| Separate event storage | ✓ Yes | ✗ No |
| Audit trail required | ✗ Envers needed | ✓ Built-in |

## Design Decisions in This Project

### Payload Handling

The outbox payload carries a **summarised delta** (which fields changed) rather than a full snapshot. This approach is suitable for provider data where consumers can query for full details if needed.

Example payload:
```
providerFirmGuid=<uuid>;firmNumber=<firm>;changedFields=name,legalServicesProvider
```

### Indexing Strategy

Two key indices enable efficient querying:

1. **idx_outbox_event_status_occurred_at**: Fast lookup of pending events by occurrence order
    - Used by the background job to fetch events to publish

2. **idx_outbox_event_aggregate**: Retrieve all events for an aggregate within a time range
    - Used for audit queries and historical reconstruction

These indices support both the background job functionality and audit/historical queries.

### Status Lifecycle

A simple three-state model (PENDING → SENT or FAILED) provides:

- Straightforward background job logic
- Clear visibility into delivery status
- Capability for manual intervention if needed

## Future Considerations

### Change-Data-Capture (CDC)

Could replace the polling job with a CDC tool (e.g., Debezium) for lower latency and more efficient resource usage.

### Snapshots

If audit needs to grow beyond field-level changes, the outbox could be augmented with full aggregate snapshots for point-in-time state reconstruction.

### Schema Evolution

Payload format should be versioned if schema changes are expected, allowing consumers to handle multiple versions.

### Retention Policy

The team should decide how long to retain SENT events, balancing audit trail requirements with storage considerations.

## References

- [Transactional Outbox Pattern](https://microservices.io/patterns/data/transactional-outbox.html) - Chris Richardson
- [Domain Event Pattern](https://microservices.io/patterns/data/domain-event.html) - Chris Richardson
- [Audit Log](https://martinfowler.com/eaaDev/AuditLog.html) - Martin Fowler
