---
source_url: https://github.com/ministryofjustice/laa-provider-data-platform/blob/main/tech-docs/source/pdp-docs/design/architecture-proposal.html.md
title: Proposed architecture
weight: 5
---

# Proposed architecture

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

This document proposes **modular layered architecture** (Option 5 from
[Architecture patterns](architecture-patterns.html)) as the target architecture: domain-based
modules enforced by Spring Modulith, command/query separation at the service level, explicit
domain events driving audit and outbox delivery, and ArchUnit rules to keep conventions enforceable.
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

A ports-and-adapters architecture pattern would not simplify a possible ERP migration. An ERP
integration would need to translate the domain models in a way that no abstract repository interface
would isolate you from. The stable API and event contracts are the best bet.

If some part of the API accumulates enough complex business logic to need a pure domain model, it
can be made into a module and done in that one module. It's not necessary for the whole service to
be made more complex when most of it doesn't need that complexity.

### Not full CQRS with separate read stores

CQRS at the level of separate read and write databases, with eventual consistency and dedicated
read models, seems uncalled-for given the scale of PDA. Query complexity is filtering, pagination,
and multi-aggregate combination - all can be handled by a single PostgreSQL database. Command/query
separation in the code alone provides many of the advantages without the operational overhead.

### Not event sourcing

Event sourcing uses events as the primary store of record, with current state derived by replaying
an aggregate's history. It introduces significant complexity for collection endpoints, which have
no current-state table to query. Hence it is not proposed here. The
[async event patterns](event-patterns.html) document considers it as a future option if audit
requirements grow significantly.

### Not simple layered architecture (Option 4)

Option 4 organises by layer: all entities together, all repositories together, all services
together. A change to "offices" could touch four separate packages (controllers, services,
repositories and entities).

However, more significantly, Option 4 has no boundary between the domains. Nothing prevents
`OfficeService` importing any of the repositories or entities. Spring Modulith enforces that
sub-packages of `{module-name}/` cannot be accessed by other modules to allow this constraint to
be implemented.

## Current state

The codebase uses a flat layered package structure under `uk.gov.justice.laa.providerdata`:

```
config/       FlywayCleanMigrationStrategy, JacksonConfig, JpaAuditingConfig, LocalDataSeeder
controller/   ProviderFirmController, ProviderFirmOfficesController,
              ProviderFirmBankAccountsController, ProviderContractManagersController,
              ProviderFirmOfficeContractManagersController,
              ProviderFirmOfficesLiaisonManagersController,
              ChamberOfficePractitionersController, TraceController
entity/       ~20 JPA entity classes, all extending AuditableEntity; Lombok @SuperBuilder,
              @Getter, @Setter, @NoArgsConstructor
exception/    GlobalExceptionHandler, ItemNotFoundException
mapper/       BankAccountMapper, ContractManagerMapper, OfficeMapper, ProviderMapper
              (MapStruct, componentModel = "spring")
repository/   ~15 Spring Data JPA repositories; ContractManagerSpecifications,
              ProviderSpecification (JPA Specification implementations)
service/      ProviderService (@Transactional(readOnly=true) but contains patchProvider()),
              ProviderCreationService, OfficeService, BankDetailsService,
              OfficeLiaisonManagerService, OfficeContractManagerAssignmentService,
              ProviderContractManagersService; result records ProviderCreationResult,
              OfficeCreationResult
util/         PageLinks, PageMetadata, PageParamValidator, Pagination,
              ProviderFirmTypeConverter, ReferenceNumberUtils, SearchCriteria, UuidUtils
```

There is no event publishing, no transactional outbox, and no audit trail. All operations are
synchronous. The command/query split is partial: `ProviderService` is declared
`@Transactional(readOnly = true)` but contains `patchProvider()`, a mutation.

Build: Java 25, Spring Boot 4, Gradle 9 (Groovy DSL),
`uk.gov.laa.springboot.laa-spring-boot-gradle-plugin` (imports Spring Boot BOM, defines
`integrationTest` source set and task). MapStruct 1.6.3 with annotation processor. Lombok via
`io.freefair.lombok`. Spotless + Checkstyle. JaCoCo (60% line/instruction minimum). Testcontainers
PostgreSQL (`PostgresqlSpringBootTest`) for integration tests.

## Target architecture

### Module structure

The service is reorganised into domain-based top-level packages under
`uk.gov.justice.laa.providerdata`. Spring Modulith
(`org.springframework.modulith:spring-modulith-starter-core`) treats each top-level package as a
module and enforces that all sub-packages are module-internal: packages under the module root cannot
be accessed by other modules. Cross-module interaction goes via a module's public types or via
`ApplicationEventPublisher`.

A module structure test using `ApplicationModulith.of(Application.class).verify()` (or
`@ApplicationModuleTest`) should run as part of the standard Gradle `test` task.

Proposed layout:

```
uk.gov.justice.laa.providerdata

provider/
  web/                        ProviderFirmController, ChamberOfficePractitionersController
                              (package-private; migrated from web/)
  ProviderCommandService        public; mutations: create, patch
  ProviderQueryService          public; reads: get, search, head-office lookups, parent links
  entity/                     ProviderEntity + subtypes (LspProviderEntity,
                              ChamberProviderEntity, PractitionerEntity,
                              AdvocatePractitionerEntity, BarristerPractitionerEntity),
                              ProviderParentLinkEntity, ProviderBankAccountLinkEntity
  repository/                 ProviderRepository, ProviderFirmRepository,
                              ProviderParentLinkRepository,
                              ProviderBankAccountLinkRepository,
                              spec/ProviderSpecification
  mapper/                     ProviderMapper

office/
  web/                        ProviderFirmOfficesController, ProviderFirmBankAccountsController,
                              ProviderFirmOfficeContractManagersController,
                              ProviderFirmOfficesLiaisonManagersController,
                              ProviderContractManagersController
  OfficeCommandService          public; mutations: create, update, liaison manager assignment,
                                contract manager assignment, bank account linking
  OfficeQueryService            public; reads: get, search, liaison managers, contract managers,
                                bank details
  entity/                     OfficeEntity, ProviderOfficeLinkEntity + subtypes
                              (LspProviderOfficeLinkEntity, ChamberProviderOfficeLinkEntity,
                              AdvocateProviderOfficeLinkEntity), OfficeBankAccountLinkEntity,
                              OfficeContractManagerLinkEntity, OfficeLiaisonManagerLinkEntity,
                              BankAccountEntity, LiaisonManagerEntity, ContractManagerEntity
  repository/                 OfficeRepository, OfficeBankAccountLinkRepository,
                              OfficeContractManagerLinkRepository,
                              OfficeLiaisonManagerLinkRepository, BankAccountRepository,
                              LiaisonManagerRepository, ContractManagerRepository,
                              AdvocateProviderOfficeLinkRepository,
                              LspProviderOfficeLinkRepository,
                              ChamberProviderOfficeLinkRepository,
                              ProviderOfficeLinkRepository,
                              ContractManagerSpecifications
  mapper/                     OfficeMapper, BankAccountMapper, ContractManagerMapper

contract/                       new module; built domain-first from the outset
  web/                        ContractController (and others TBD)
  ContractCommandService        public
  ContractQueryService          public
  ContractCreatedEvent          public domain event record
  ContractUpdatedEvent          public domain event record
  ScheduleCreatedEvent          public domain event record
  entity/                     ContractEntity (aggregate root), ScheduleEntity,
                              ScheduleLineEntity, NmsAuthorisationEntity
  repository/                 ContractRepository (aggregate root only; no ScheduleRepository,
                              ScheduleLineRepository, or NmsAuthorisationRepository)
  mapper/                     ContractMapper

shared/
  entity/                       AuditableEntity
  exception/                    GlobalExceptionHandler, ItemNotFoundException
  util/                         PageLinks, PageMetadata, PageParamValidator, Pagination,
                                ProviderFirmTypeConverter, ReferenceNumberUtils,
                                SearchCriteria, UuidUtils

web/                            transitional; existing controllers until migrated to {module}/web/

config/                         Spring configuration; FlywayCleanMigrationStrategy,
                                JacksonConfig, JpaAuditingConfig, LocalDataSeeder
```

Notes:

- The `contract/` module is new and can be built on this structure from the start.
- Existing modules (`provider/`, `office/`) could migrate incrementally alongside other work,
  without requiring a dedicated refactoring sprint.
- **Controller placement:** controllers live in a `web/` sub-package within their module
  (e.g. `provider/web/`), alongside any web-layer DTOs and web-specific configuration. They are
  declared **package-private** (no `public` modifier): Spring discovers them via component scan
  and invokes handler methods via reflection, both of which bypass Java visibility. Spring Modulith
  already treats `web/` as module-internal (all sub-packages are); package-private adds a second
  layer that prevents even intra-module references. This `{module}/web/` convention follows
  [spring-restbucks](https://github.com/odrotbohm/spring-restbucks), a reference application by
  the Spring Modulith author, and is confirmed by community projects.
- BankAccount, LiaisonManager, and ContractManager are separate aggregates per the
  [domain model](domain-model.html) but are placed in `office/` for practical reasons, since they
  have no standalone creation endpoints and are accessed via office operations (in fact contract
  managers are managed outside PDA). They can be moved to their own modules as needed.
- The exact boundary between `provider/` and `office/` for shared entities (such as
  `ProviderOfficeLinkEntity`) may not be exactly as above, but each entity should live in exactly
  one module's sub-packages. Multi-aggregate creation (`POST /provider-firms`) spans boundaries
  and will need resolving.

### Command/query separation

Within each module, read and write operations are handled by separate service classes.

**Command services** are not annotated `@Transactional(readOnly = true)` on their class. They
publish events after each successful write.

**Query services** are annotated `@Transactional(readOnly = true)` and must not invoke any
repository method that causes a change (`save`, `delete`, `deleteAll`, `deleteById`).

Changes to existing services:

| Current class                            | Issue                                              | Action                                                                                                 |
|------------------------------------------|----------------------------------------------------|--------------------------------------------------------------------------------------------------------|
| `ProviderCreationService`                | Correctly write-only                               | Move to `provider/`; rename `ProviderCommandService`; absorb `patchProvider()`                         |
| `ProviderService`                        | Declared `readOnly` but contains `patchProvider()` | Move `patchProvider()` to `ProviderCommandService`; rename `ProviderQueryService`; move to `provider/` |
| `OfficeService`                          | Mixed read and write                               | Split into `OfficeCommandService` and `OfficeQueryService`; move both to `office/`                     |
| `BankDetailsService`                     | Mixed                                              | Split; move to `office/`                                                                               |
| `OfficeLiaisonManagerService`            | Write-only, reads move to `OfficeQueryService`     | Rename `OfficeLiaisonManagerCommandService`; move to `office/`                                         |
| `OfficeContractManagerAssignmentService` | Write-only                                         | Rename `OfficeContractManagerCommandService`; move to `office/`                                        |
| `ProviderContractManagersService`        | Read-only                                          | Rename `ContractManagerQueryService`; move to `office/`                                                |

Explicit command objects - Java records carrying the validated input for a single update,
instantiated by the web layer - should exist for all write operations. They make the intent of a
call explicit and provide a natural place to put together the event payload.

### Domain aggregates

The [domain model](domain-model.html) document describes the current aggregate structure. The contract
module would introduce a new aggregate (needs more BA input, but something like):

- `ContractEntity` - aggregate root; linked to a `ProviderOfficeLinkEntity` by GUID
- `ScheduleEntity` - member of `Contract`; multiple per contract
- `ScheduleLineEntity` - member of `Schedule`; multiple per schedule
- `NmsAuthorisationEntity` - member of `Schedule`

**One repository per aggregate root.** `ScheduleRepository`, `ScheduleLineRepository`, and
`NmsAuthorisationRepository` must not exist as Spring-managed beans accessible outside the
`contract/` module. All access to schedules and schedule-lines goes via `ContractRepository`
and the `ContractEntity` aggregate. Module boundaries and ArchUnit rules would enforce this.

jMolecules annotations could be applied to the contract module from the start:

- `@org.jmolecules.ddd.annotation.AggregateRoot` on `ContractEntity`
- `@org.jmolecules.ddd.annotation.Entity` on `ScheduleEntity`, `ScheduleLineEntity`,
  `NmsAuthorisationEntity`

Gradle dependencies would be something like this (check for Spring Boot 4 compatibility on
[jMolecules releases](https://github.com/xmolecules/jmolecules/releases)):

```groovy
implementation 'org.jmolecules:jmolecules-ddd'
implementation 'org.jmolecules.integrations:jmolecules-spring'
```

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

Publishing an event from a command service:

```java
// ContractCommandService.java
@Transactional
public ContractCreatedResult createContract(CreateContractCommand command) {
    ContractEntity contract = buildEntity(command);
    contractRepository.save(contract);
    // Published inside the transaction. Spring Modulith records it atomically
    applicationEventPublisher.publishEvent(
        new ContractCreatedEvent(contract.getGuid(), command.officeId(), /* more fields */));
    return new ContractCreatedResult(contract.getGuid());
}
```

Two `@ApplicationModuleListener` methods consume each event independently:

```java
@ApplicationModuleListener
void on(ContractCreatedEvent event) {
    sqsTemplate.send(outboundQueue, event);  // retried by Spring Modulith on failure
}

@ApplicationModuleListener
void on(ContractCreatedEvent event) {
    auditRepository.save(AuditRecord.from(event));
}
```

Event payloads follow these rules:

- Event classes are Java records, placed in the module root (public, not `internal/`).
- Payloads carry meaningful identifiers (firm numbers, office codes), not unexposed database keys.
- Payloads carry a full snapshot of aggregate state after the change, so consumers do not need to
  call back to the API (very large payloads can be less granular).
- Event names are past-tense domain facts: `ContractCreatedEvent`, `ScheduleUpdatedEvent`.

The event catalogue in [Async event patterns](event-patterns.html) should be extended to cover
the contract module events as they get defined.

## The API and event contracts for continuity

The OpenAPI specification (`provider-data-api/src/main/resources/laa-data-pda.yml`), also published
as a versioned library to the GitHub Package Registry, is the stable API that PDA consumers depend
on. The event schema serves the same purpose. Both should be treated as more long-lived than any
internal implementation details. When thinking about a possible backend migration, these contracts
could survive even a rewrite.

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

- No class in `..web..` or `..controller..` may be imported by a class in any service or module
  root package.
- No class in any `..repository..` package may be imported directly by a class in `..web..` or
  `..controller..`.

### Command/query split

- Classes annotated `@Transactional(readOnly = true)` at the class level must not call any method
  named `save`, `delete`, `deleteAll`, or `deleteById` on a repository.
- Classes whose simple name ends in `QueryService` must be annotated
  `@Transactional(readOnly = true)` at the class level.
- Classes whose simple name ends in `CommandService` must not be annotated
  `@Transactional(readOnly = true)` at the class level.

### Module boundaries

- No class outside `uk.gov.justice.laa.providerdata.{module}` may import a class from
  `uk.gov.justice.laa.providerdata.{module}.internal`.
- Spring Modulith's module structure test also enforces this. ArchUnit catches it in the standard
  Gradle `test` task without a separate test run.
- Spring Modulith's `@ApplicationModule(allowedDependencies = ...)` annotation should be used to
  declare permitted cross-module dependencies.

### Mapper placement

- Classes annotated `@org.mapstruct.Mapper` must reside in a package named `mapper` or ending in
  `.mapper`.

### Aggregate root discipline

- No Spring Data `Repository` sub-interface may be declared for `ScheduleEntity`,
  `ScheduleLineEntity`, or `NmsAuthorisationEntity` (or any future non-root member entity).
- These types are accessed only via their aggregate root's repository.

## Next steps

The most useful next step is adding ArchUnit with a baseline set of layering and command/query
rules. This requires only a test dependency and no refactoring changes, but immediately makes the
existing conventions visible and enforceable.

The command/query split can then be completed for the existing provider and office services -
`patchProvider()` moves out of `ProviderService`, and `OfficeService` is split - and the split
pattern is used in any new modules.

The contract module is the natural place for the larger structural changes. It should be built
domain-first from the outset: modular package structure, jMolecules annotations, aggregate root
discipline, and event publication. Building one module correctly means there is an example to point
developers at when reorganising existing modules.

Spring Modulith can be introduced alongside the contract module work. Once at least two domain-based
top-level packages exist, its module structure test starts to have an effect.

The event publication registry, audit listener, and SQS/SNS delivery listener are all additions
beyond that. They should not require changes to the existing API contract or entity model.

The architecture described here is achievable and can be reached using incremental steps, where
each step can be an independent PR, and can be verified using the existing unit tests, integration
tests and end-to-end tests.
