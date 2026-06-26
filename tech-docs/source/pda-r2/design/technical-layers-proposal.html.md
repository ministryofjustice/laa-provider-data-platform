---
source_url: https://github.com/ministryofjustice/laa-provider-data-platform/blob/main/tech-docs/source/pda-r2/design/technical-layers-proposal.html.md
title: Technical layers proposal
weight: 5
---

# Technical layers proposal

## Summary

`provider-data-service` is a Spring Boot REST API that manages provider data for the LAA. It
exposes a published OpenAPI contract consumed by other LAA services and persists to a PostgreSQL
database via JPA and Flyway.

The service works well in its current state, but we're aware of some near-future requirements that
will put pressure on the existing flat layered arrangement if they were introduced without a plan:
these include the addition of further domain entities (contracts, schedules, authorisations, lines),
the addition of an audit trail, transactional event publication to downstream consumers, and the
possibility of changing backend persistence to a third-party system (such as an ERP). See the
[Drivers](#drivers) section for more detail.

This document originally proposed **modular layered architecture** (Option 5 from
[Architecture patterns](architecture-patterns.html)) using per-entity domain modules enforced by
Spring Modulith. In practice this proved too fine-grained (see
[Module granularity](#module-granularity)). The revised recommendation is **flat technical layers**
(Option 4) enhanced with Spring Modulith's event publication infrastructure,
`spring-modulith-starter-insight` for Micrometer tracing at event boundaries, and
`ApplicationModulesTest` applied to the technical layer packages. Command/query separation at the
service level, explicit domain events driving audit and outbox delivery, and ArchUnit rules to keep
conventions enforceable remain part of the approach.

The OpenAPI contract and the event stream are treated as stable interfaces that we should aim to
maintain, even through fundamental changes like using an ERP for persistence.

This document does not propose Hexagonal, Onion, or Clean architecture, nor full CQRS with
separate read stores. The reasoning behind that can be found in
[What this proposal does not recommend](#what-this-proposal-does-not-recommend).

## Drivers

### Further domain entities

Four to five new entities are planned, scoped to the provider-office relationship: contracts,
schedules, NMS authorisations, and schedule-lines. Each will introduce new controllers, services,
repositories, and entities. Added to the existing flat package structure they will make the
`entity/`, `repository/`, and `service/` packages significantly harder to navigate, and increase
the risk that a change in one domain area inadvertently affects another.

### Audit trail

Provider data changes are likely subject to audit requirements: what changed, when, and the state
of the record at that point. Retrofitting this to an unstructured service layer is harder than
treating it as a first-class concern from the start.

### Event publication and the outbox

Downstream tools will need to be notified of provider data changes, delivered via SQS or SNS. The
transactional outbox pattern guarantees that events are never lost: the event record is written in
the same database transaction as the entity change, and a background process publishes the event to
the broker. Without an explicit architecture for domain events, this reliability guarantee is
difficult to enforce consistently as more write operations get added.

### Command/query pressure

Read and write operations are already diverging in complexity. Reads involve pagination, filtering,
and multi-aggregate combining. Writes are heading towards outbox event emission and audit entries.
Treating them the same with a mingled service layer makes each harder to reason about, test, and
extend independently.

### Possible future backend data store changes

There is a possibility (still in discussion) that the relational database persistence layer could
eventually be replaced by a third-party ERP system. Migrating the code to use an ERP would not be a
simple infrastructure swap. It would involve translating the domain models and a very different way
of integrating. Architecture cannot make that any easier. However, two things can help continuity:
a stable API contract and a stable event stream. Both could survive an internal rewrite.

## What this proposal does not recommend

### Not Hexagonal, Onion, or Clean architecture

All three patterns require separate pure domain objects alongside JPA entities, with a mapping
layer between them. For this service:

- The domain logic is relational in character - it manages relationships between entities that
  closely mirror the database schema, without complex business rules that benefit from isolation
  from persistence.
- There is no near-term requirement to swap the persistence technology (next paragraph explains why
  swapping persistence to an ERP is different).
- Testability is already addressed by `@DataJpaTest` slices and Testcontainers integration tests.
- The existing entity-to-API-model mapping via MapStruct already exists. Adding a
  domain-object-to-JPA layer on top would double the mapping overhead for no current benefit.

The project already has a meaningful separation between its two contracts:
the OpenAPI specification defines what the API exposes, the Flyway DDL defines what the database
stores, and the MapStruct mapping layer translates between the two. That translation is
non-trivial. This is the part of Hexagonal that actually makes it worthwhile.

What Hexagonal would add on top is a third model layer: pure domain objects with no JPA
annotations, sitting between the API model and the JPA entities, with two rounds of mapping
instead of one. For this service those pure domain objects would be identical to the JPA entities
in all but their annotations, because the entities already model the domain directly.

There is also a more fundamental issue with the Hexagonal framing: it treats the API as a
swappable input adapter, one that you might replace with a CLI or a message queue. That does not
describe this service. The REST API is the Data Stewardship contract. It is what downstream
systems are built against; the OpenAPI spec is versioned and published as a library. It encodes
domain business logic - which fields are required, what formats are valid, what relationships
between resources mean. It is not a port to be swapped.

A ports-and-adapters architecture pattern would not simplify a possible ERP migration. An ERP
integration would need to translate the domain models in a way that no abstract repository interface
would isolate you from. The stable API and event contracts are the best bet.

If some part of the API accumulates enough complex business logic to need a pure domain model, it
can be made into a module and done in that one module. It's not necessary for the whole service to
be made more complex when most of it doesn't need that complexity.

### Not full CQRS with separate read stores

CQRS at the level of separate read and write databases, with eventual consistency and dedicated
read models, seems uncalled-for given the scale of PDA-r2. Query complexity is filtering,
pagination, and multi-aggregate combination - all can be handled by a single PostgreSQL database.
Command/query separation in the code alone provides many of the advantages without the operational
overhead.

### Not event sourcing

Event sourcing uses events as the primary store of record, with current state derived by replaying
an aggregate's history. It introduces significant complexity for collection endpoints, which have
no current-state table to query. Hence it is not proposed here. The
[async event patterns](event-patterns.html) document considers it as a future option if audit
requirements grow significantly.

### Module granularity

The original proposal favoured per-entity domain modules on the grounds that module boundaries
would prevent cross-domain coupling.

In practice, the API specification is designed around user workflows rather than aggregate
boundaries. A single endpoint such as `POST /provider-firms` creates a provider firm, an office,
and a liaison manager atomically. Implementing this across per-entity module boundaries requires an
orchestration layer that ends up depending on every other module, which is the coupling the
boundaries were supposed to prevent.

Spring Modulith's module concept works equally well with technical layer packages.
`ApplicationModulesTest` applied to `web/`, `service/`, `repository/`, and related packages
enforces the rules that actually matter: no direct `web` -> `repository` access, and sub-packages
within each layer are module-private.

If genuine bounded contexts emerge as the service grows (for example, contracts becoming a distinct
lifecycle from provider firm structure), coarser modules (two or three) could be introduced at that
point.

## Pre-refactoring structure

The codebase originally used a flat layered package structure under
`uk.gov.justice.laa.providerdata`:

```
config/       Spring configuration, data seeder
controller/   Spring MVC controllers
entity/       ~20 JPA entity classes, all extending AuditableEntity; Lombok @SuperBuilder,
              @Getter, @Setter, @NoArgsConstructor
exception/    exception handler, custom exception types
mapper/       MapStruct mappers (entity <-> OpenAPI model), componentModel = "spring"
repository/   ~15 Spring Data JPA repositories, JPA Specification implementations
service/      application services (mixed read/write; command/query split partial),
              result record types
util/         pagination helpers, search criteria, type converters, utilities
```

There was no event publishing, no transactional outbox, and no audit trail. All operations were
synchronous. The command/query split was partial: the main query service class was declared
`@Transactional(readOnly = true)` but contained a mutation method.

Build: Java 25, Spring Boot 4, Gradle 9 (Groovy DSL),
`uk.gov.laa.springboot.laa-spring-boot-gradle-plugin` (imports Spring Boot BOM, defines
`integrationTest` source set and task). MapStruct 1.6.3 with annotation processor. Lombok via
`io.freefair.lombok`. Spotless + Checkstyle. JaCoCo (60% line/instruction minimum). Testcontainers
PostgreSQL (`PostgresqlSpringBootTest`) for integration tests.

## Target architecture

### Module structure

The service is organised into flat technical layer packages under
`uk.gov.justice.laa.providerdata`. Spring Modulith
(`org.springframework.modulith:spring-modulith-starter-*`) treats each top-level package as a
module. `ApplicationModulesTest` applied to these technical layer packages enforces the rules that
matter: the `web` package must not depend on `repository` directly, and sub-packages within each
layer are module-private.

Proposed layout:

```
uk.gov.justice.laa.providerdata
  config/       Spring configuration
  entity/       JPA entities
  event/        event types, publisher, snapshot assembler, event query service
  mapper/       MapStruct mappers (entity <-> OpenAPI model)
  repository/   Spring Data JPA repositories and Specification helpers
  service/      command and query services, use-case orchestrators, result types
  support/      cross-cutting utilities, base entity class, exception types, pagination helpers
  web/          controllers, type converters, web exception handling
```

`event/` is intentionally separate from `service/` because the transactional outbox is a distinct
responsibility: command services publish events, and the event listeners (SQS delivery) are
defined in `event/`.

A module structure test using `ApplicationModules.of(Application.class).verify()` runs as part of
the standard Gradle `test` task.

If coarser domain modules are introduced in future (for example, a `contract` module as that
lifecycle becomes distinct from provider firm structure), the flat layer structure is compatible
with that evolution.

### Command/query separation

Read and write operations are handled by separate service classes in `service/`.

**Command services** are not annotated `@Transactional(readOnly = true)` on their class. They
publish events after each successful write.

**Query services** are annotated `@Transactional(readOnly = true)` and must not invoke any
repository method whose name begins with `save` or `delete`.

Explicit command objects - Java records carrying the validated input for a single update,
instantiated by the web layer - should exist for all write operations. They make the intent of a
call explicit and provide a natural place to put together the event payload.

### Domain aggregates

The [domain model](domain-model.html) document describes the current aggregate structure.

**One repository per aggregate root.** Member entities (schedules, lines, authorisations) must not
have Spring-managed repositories accessible outside their containing service or package. All access
to member entities goes via the aggregate root's repository. This is a coding convention enforced
by code review (see [Aggregate root discipline](#aggregate-root-discipline) below).

### Events and the outbox

Events handle downstream queued delivery, audit history should use Hibernate Envers. A command
service publishes an event via `ApplicationEventPublisher` after each successful write. Spring
Modulith's event publication registry (`org.springframework.modulith:spring-modulith-events-jpa`)
records the event in an `event_publication` table within the same database transaction as the entity
change. This is the transactional outbox. No custom outbox table or `@Scheduled` job is needed. If
the downstream broker is unavailable, Spring Modulith retries the event automatically.

Gradle dependencies would be something like this (check for Spring Boot 4 compatibility on
[Spring Modulith releases](https://github.com/spring-projects/spring-modulith/releases)):

```groovy
implementation 'org.springframework.modulith:spring-modulith-starter-core'
implementation 'org.springframework.modulith:spring-modulith-events-jpa'
```

The `spring-modulith-events-jpa` module provides the `event_publication` table DDL. We could create
it using a Flyway migration. See the
[Spring Modulith schema reference](https://docs.spring.io/spring-modulith/reference/appendix.html).

Event payloads follow these rules:

- Event classes are Java records, defined in the `event/` package.
- Payloads carry meaningful identifiers (firm numbers, office codes), not unexposed database keys.
- Payloads carry a full snapshot of aggregate state after the change, so consumers do not need to
  call back to the API (very large payloads can be less granular).
- Event names are past-tense domain facts.

The event catalogue in [Async event patterns](event-patterns.html) should be extended to cover
the contract module events as they get defined.

## The API and event contracts for continuity

The OpenAPI specification (`provider-data-api/src/main/resources/laa-data-pda.yml`), also published
as a versioned library to the GitHub Package Registry, is the stable API that PDA-r2 consumers
depend on. The event schema serves the same purpose. Both should be treated as more long-lived than
any internal implementation details. When thinking about a possible backend migration, these
contracts could survive even a rewrite.

Practical rules:

- API response models should reflect the domain rather than, for example, the database tables. Do
  not expose link entity internal identifiers, join table artefacts, or JPA-shaped structures in the
  API.
- Event payloads should only include fields that are meaningful to the domain. Do not include JPA
  entity class names, internal package identifiers, or unexposed database keys in event payloads.
- The published OpenAPI spec should remain backward-compatible across changes; breaking changes
  should require a version increment.
- Event schema changes that remove or rename fields are breaking changes for consumers.

## Architectural enforcement

Without enforcement, any standards will start to be eroded over time. The following rules could be
expressed as ArchUnit tests in `provider-data-service/src/test/java`, running as part of the
standard Gradle `test` task.

Gradle dependencies would be something like this (check for Spring Boot 4 compatibility on
[ArchUnit releases](https://github.com/TNG/ArchUnit/releases)):

```groovy
testImplementation 'com.tngtech.archunit:archunit-junit5'
```

Example test class name: `uk.gov.justice.laa.providerdata.ArchitectureTest`.

### Layering

- No class in `..web..` may be imported by a class in any service package.
- No class in any `..repository..` package may be imported directly by a class in `..web..`.

### Command/query split

- Classes annotated `@Transactional(readOnly = true)` at the class level must not call any method
  whose name begins with `save` or `delete` on a repository.
- Classes whose simple name ends in `QueryService` must be annotated
  `@Transactional(readOnly = true)` at the class level.
- Classes whose simple name ends in `CommandService` must not be annotated
  `@Transactional(readOnly = true)` at the class level.

### Module boundaries

- No class in `web` may depend on a class in `repository` directly.
- Sub-packages within each technical layer are module-private and must not be imported by other
  layers.
- Spring Modulith's `ApplicationModulesTest` enforces these boundaries at CI time as part of the
  standard Gradle `test` task.

### Mapper placement

- Classes annotated `@org.mapstruct.Mapper` must reside in a package named `mapper` or ending in
  `.mapper`.

### Aggregate root discipline

This is a coding convention enforced by code review rather than ArchUnit. There is no marker
(annotation or naming convention) that mechanically distinguishes aggregate roots from member
entities, so a rule cannot be expressed in code at present.

- No Spring Data `Repository` sub-interface should be declared for aggregate member entities
  (schedules, lines, authorisations, or any future non-root member entity).
- Member entities should be accessed only via the aggregate root's repository.
