---
source_url: https://github.com/ministryofjustice/laa-provider-data-platform/blob/main/tech-docs/source/pdp-docs/design/events.html.md
title: Async commands and events
weight: 30
---

# Async commands and events

Options for introducing asynchronous command processing and domain event publishing in
`provider-data-service`. The team has not yet decided which approach to take.

## Patterns

> If you know these patterns, skip to [Constraints](#constraints).

### Domain events

A domain event records that something happened in the domain. It's raised by a use case after a
state change: e.g. `ProviderFirmCreated` after `CreateProviderFirmUseCase` saves an entity. Events
are named in the past tense and carry enough data for consumers to act without querying back.

Further reading:

- Fowler, [Domain Event](https://martinfowler.com/eaaDev/DomainEvent.html)
- Vernon, *[Implementing Domain-Driven Design](https://www.informit.com/store/implementing-domain-driven-design-9780321834577)* (2013, ch. 8)

### Integration events

Integration events are domain events published to a message broker for consumption by other
services. They may map 1:1 to domain events or be translated at the boundary.

Further reading:

- Richardson, [Domain event pattern](https://microservices.io/patterns/data/domain-event.html)

### Transactional outbox

The outbox pattern solves the dual-write problem: saving an entity and publishing an event are two
separate operations, and either can fail. Instead, the event is written to an `outbox` table in
the same database transaction as the entity change. A background job reads the outbox and publishes
to the broker, guaranteeing at-least-once delivery without a distributed transaction.

Further reading:

- Richardson, [Transactional Outbox](https://microservices.io/patterns/data/transactional-outbox.html)

### Command inbox

The inbox pattern is the inbound complement to the outbox. A producer writes commands to a queue
or an `inbox` table. The receiving service processes them from the queue, decoupling the API
response from the processing. It enables backpressure, replay, and independent scaling of the worker.

Further reading:

- Richardson, [Idempotent Consumer](https://microservices.io/patterns/communication-style/idempotent-consumer.html)

### Audit log

An audit log is an append-only record of each mutation: what changed, when, and (optionally) who
triggered it. If domain events carry the full aggregate state after each change, the event log can
double as the audit trail. Otherwise, a dedicated `audit_log` table captures before/after snapshots
alongside the normal entity write.

Further reading:

- Fowler, [Audit Log](https://martinfowler.com/eaaDev/AuditLog.html)

### Event sourcing

Event sourcing uses events as the primary store. Current state is derived by replaying an
aggregate's event history, so audit trail and state reconstruction come for free. Queries are the
main complexity: a single aggregate loads fine, but filtered collections (e.g.
`GET /provider-firms?name=Smith`) have no current-state table and need a separately maintained
read model. Deep streams also need periodic snapshots. Not proposed here, but if audit needs
grow beyond Envers, it's an option to consider.

Further reading:

- Fowler, [Event Sourcing](https://martinfowler.com/eaaDev/EventSourcing.html)
- Young, [CQRS Documents](https://cqrs.files.wordpress.com/2010/11/cqrs_documents.pdf) (2010) - sections on event sourcing

### Hibernate Envers

Envers is a Hibernate module that maintains revision history for JPA entities automatically.
Annotate an entity with `@Audited` and Envers writes a snapshot of every insert, update, and
delete to a `*_AUD` shadow table, keyed by a revision number and timestamp. Historical state at
any revision is queryable via the Envers API - no event replay needed. Envers ships with
Hibernate 6 and is already on the classpath.

Further reading:

- [Hibernate Envers documentation](https://docs.jboss.org/hibernate/orm/current/userguide/html_single/Hibernate_User_Guide.html#envers)

## Current state

There's no event publishing and no command queue. All operations are synchronous: the HTTP
response is returned after the entity is saved.

## Constraints

- Events are integration events for consumption by other LAA services. Spring
  `ApplicationEventPublisher` may be used internally to raise domain events in-process, but is
  not the delivery mechanism for integration events.
- The event broker technology is not yet decided (candidates include AWS SQS/SNS, Amazon
  EventBridge, and Apache Kafka). All three options below are broker-agnostic.
- Consumers must handle at-least-once delivery (idempotent consumers).

### Audit and state reconstruction

Provider data changes may be subject to audit requirements: knowing what changed, when, and what
an aggregate looked like at a point in time. Two approaches are available without full event
sourcing.

**Hibernate Envers** - add `@Audited` to JPA entities and Envers automatically writes a snapshot
of every change to `*_AUD` shadow tables. Historical state at any revision is queryable directly
via the Envers API. This works regardless of which option is chosen and requires no changes to the
event or outbox design.

**Outbox payload as full snapshot** (Options 2 and 3) - if each outbox event payload carries the
complete aggregate state after the change (not just a delta), the outbox is also a point-in-time
history, provided rows are retained after publishing. Reconstruction is a single query: find the
latest outbox row for the aggregate before the target timestamp. The `created_at` timestamp
records when the change occurred. `published_at` records when it was forwarded to the broker and
should not be used as the event time.

Envers gives in-service queryable history. The outbox snapshot gives consumers the data they need
to reconstruct state without calling back.

Option 1 (direct publish) leaves no persistent record in the service. Audit would need a separate
mechanism - Envers, a dedicated `audit_log` table, or capturing events at the broker.

### Event catalogue (sketch)

| Event                     | Trigger                                         |
|---------------------------|-------------------------------------------------|
| `ProviderFirmCreated`     | `POST /provider-firms`                          |
| `ProviderFirmUpdated`     | `PATCH /provider-firms/{id}`                    |
| `OfficeCreated`           | `POST /provider-firms/{id}/offices`             |
| `OfficeUpdated`           | `PATCH /provider-firms/{id}/offices/{officeId}` |
| `LiaisonManagerAssigned`  | `POST .../offices/{officeId}/liaison-managers`  |
| `ContractManagerAssigned` | `POST .../offices/{officeId}/contract-managers` |
| `BankAccountUpdated`      | `PATCH .../bank-accounts/{id}`                  |

## Options

These options are independent of the architecture choice in [Architecture](architecture.html) -
the `EventPublisher` port and outbox adapter slot into Onion, Clean, and Hexagonal equally. Option
3 is the one exception: if using Hexagonal, the command queue worker fits as `adapter/in/queue`
alongside `adapter/in/web`.

### Option 1: Direct publish (simplest)

Commands are handled synchronously. After the entity is saved, the use case publishes an event
directly to the broker via an `EventPublisher` port. No outbox table, no background job.

```
HTTP request -> controller -> use case
  -> saves entity
  -> publishes event directly to broker
-> 201 Created
```

**Pros:** Simplest to implement. No extra tables, no background job, no worker.

**Cons:** No guaranteed delivery. If the broker is unavailable at publish time, the event is
lost - the entity change is persisted but the event is not. Couples the database commit and broker
publish in the critical path.

### Option 2: Transactional outbox (synchronous API)

The API continues to return synchronous responses (201/200). After saving an entity, the use case
writes a domain event to an `outbox` table in the same transaction. A background job reads
undelivered rows and publishes them to the broker.

```
HTTP request -> controller -> use case
  -> saves entity        \
  -> writes outbox row    > same DB transaction
-> 201 Created

[async] job: reads outbox -> publishes to broker -> marks published
```

The outbox table:

```sql
CREATE TABLE outbox (
  id           UUID      PRIMARY KEY,
  event_type   TEXT      NOT NULL,
  aggregate_id UUID      NOT NULL,
  payload      JSONB     NOT NULL,
  created_at   TIMESTAMP NOT NULL,
  published_at TIMESTAMP
);
```

The background job can be a polling `@Scheduled` bean or an external change-data-capture tool.
That choice is independent of the option selected here.

Package additions per architecture option (see [Architecture](architecture.html)):

|                            | Onion                  | Clean              | Hexagonal              |
|----------------------------|------------------------|--------------------|------------------------|
| `EventPublisher` interface | `domain/service`       | `usecase/boundary` | `application/port/out` |
| Outbox writer adapter      | `infrastructure/event` | `adapter/event`    | `adapter/out/event`    |

**Pros:** API contracts unchanged. Reliable at-least-once delivery. No distributed transaction.

**Cons:** Background job needed. Small delay between entity save and event publish.

### Option 3: Async command queue + outbox

The API accepts a command, writes it to an `inbox` table, and returns 202 Accepted immediately.
A worker reads commands from the inbox, calls the use case, and the use case writes both entity
and outbox event in a single transaction. The background job then publishes the outbox event.

```
HTTP request -> controller -> writes command to inbox -> 202 Accepted

[async] worker: reads command -> use case
  -> saves entity        \
  -> writes outbox row    > same DB transaction

[async] job: reads outbox -> publishes to broker -> marks published
```

The inbox table:

```sql
CREATE TABLE command_inbox (
  id           UUID      PRIMARY KEY,
  command_type TEXT      NOT NULL,
  payload      JSONB     NOT NULL,
  created_at   TIMESTAMP NOT NULL,
  processed_at TIMESTAMP,
  failed_at    TIMESTAMP,
  error        TEXT
);
```

A status check endpoint lets callers confirm whether a command has been processed.

**Pros:** Decouples API from processing. Handles backpressure. Commands can be replayed or
inspected. API and worker can be scaled independently.

**Cons:** API contracts change to 202 - callers must poll for status or accept eventual
consistency. More components to operate and debug. Events arrive later end-to-end.

## Migration approach

Steps are the same regardless of which option is chosen. Only Option 3 has additional steps.

1. Define an `EventPublisher` outbound port interface in the application layer.
2. Implement a no-op adapter for tests and local development.
3. Update use cases to raise domain events via the port after entity saves.
4. Add the `outbox` table migration and implement the outbox writer adapter (Options 2 and 3).
5. Deploy the background job to read and publish outbox events (Options 2 and 3).
6. (Option 3) Add the `command_inbox` table, update the web adapter to write commands to it,
   and introduce the worker.
