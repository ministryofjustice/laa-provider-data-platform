---
source_url: https://github.com/ministryofjustice/laa-provider-data-platform/blob/main/tech-docs/source/pdp-docs/design/architecture-patterns.html.md
title: Architecture patterns
weight: 20
---

# Architecture patterns

Five architectural options for `provider-data-service`. Options 1 to 3 enforce the same
inward-dependency rule and are compatible with use-case-level CQRS. No framework or technology
changes are required for any of them. Options 4 and 5 are layered variants that do not invert
the dependency direction. See [Technical layers proposal](technical-layers-proposal.html) for a recommended
approach.

## Patterns

> If you know these patterns, skip to [Pre-refactoring structure](#pre-refactoring-structure).

### Layered architecture

The current codebase uses **layered architecture** (*n-tier* or *three-tier*) - controllers,
services, repositories, each depending only on the layer below. It's the default Spring
structure and described in Fowler's
*[Patterns of Enterprise Application Architecture](https://martinfowler.com/books/eaa.html)*
(2002). Its main drawback is that business logic can become coupled to persistence, making it
harder to test in isolation.

### Hexagonal architecture

**Hexagonal architecture** (also *ports and adapters*) was introduced by Alistair Cockburn in
[2005](https://alistair.cockburn.us/hexagonal-architecture/). It inverts the layered model:
the application core defines **ports** (interfaces) and external concerns - HTTP, databases,
messaging - plug in as **adapters**. Dependencies point inward. The core has no knowledge of
any adapter.

Further reading:

- Cockburn, [Hexagonal Architecture](https://alistair.cockburn.us/hexagonal-architecture/) (2005)
- Tom Hombergs, *[Get Your Hands Dirty on Clean Architecture](https://www.packtpub.com/en-gb/product/get-your-hands-dirty-on-clean-architecture-9781805128373)* (2nd ed., 2023)
- Netflix Tech Blog, [Ready for changes with Hexagonal Architecture](https://netflixtechblog.com/ready-for-changes-with-hexagonal-architecture-b315ec967749) (2020)

### Onion architecture

**Onion architecture** was introduced by Jeffrey Palermo in
[2008](https://jeffreypalermo.com/2008/07/the-onion-architecture-part-1/) and expresses the
same inward-dependency rule using concentric rings (Domain Model -> Domain Services -> Application
Services -> Infrastructure) rather than the port/adapter metaphor. See
[Naming conventions](#naming-conventions) below for a comparison of the three vocabularies.

Further reading:

- Palermo, [The Onion Architecture](https://jeffreypalermo.com/2008/07/the-onion-architecture-part-1/) (parts 1-4, 2008)

### Clean architecture

**Clean architecture** was described by Robert C. Martin in a
[2012 blog post](https://blog.cleancoder.com/uncle-bob/2012/08/13/the-clean-architecture.html)
and expanded in his book (2017). Martin credits Cockburn and Palermo as predecessors. Like
Onion, it uses concentric rings. The vocabulary used:

- **Entities** (innermost) - enterprise-wide business rules, domain objects.
- **Use Cases** - application-specific rules. The ring also contains the input and output
  **boundary** interfaces between the use case and its callers/dependencies.
- **Interface Adapters** - **Controllers** (map HTTP requests to use-case inputs), **Presenters**
  (map use-case output for delivery), and **Gateways** (concrete repository implementations).
- **Frameworks & Drivers** (outermost) - Spring, Hibernate, the database itself.

For a REST API there's no Presenter in the traditional sense. The Controller both invokes the
use case and formats the response.

Further reading:

- Martin, [The Clean Architecture](https://blog.cleancoder.com/uncle-bob/2012/08/13/the-clean-architecture.html) (blog, 2012)
- Martin, *[Clean Architecture: A Craftsman's Guide to Software Structure and Design](https://www.oreilly.com/library/view/clean-architecture-a/9780134494272/)* (2017)

### CQRS

CQRS (Command Query Responsibility Segregation) keeps reads and writes separate, each with its
own model. It ranges from a simple code organisation pattern within a single service up to
separate read/write databases with eventual consistency. Here it's applied only at the use-case
level.

Further reading:

- Martin Fowler, [CQRS](https://martinfowler.com/bliki/CQRS.html)
- Greg Young, [CQRS Documents](https://cqrs.files.wordpress.com/2010/11/cqrs_documents.pdf) (2010)
- Microsoft, [CQRS pattern](https://learn.microsoft.com/en-us/azure/architecture/patterns/cqrs) - Azure Architecture Center

### Spring Modulith

[Spring Modulith](https://docs.spring.io/spring-modulith/reference/) treats top-level packages as
application modules and verifies their boundaries at test time. Types in the module root package
are accessible to other modules; types in any sub-package are module-private. Cross-module
interaction goes via the module's root-package types or via Spring `ApplicationEvent`.

Spring Modulith also provides an **event publication registry**: domain events published inside a
`@Transactional` method are recorded in an `event_publication` table in the same transaction. A
background process retries any events whose listeners did not acknowledge them, providing a
transactional outbox without a custom implementation.

[jMolecules](https://github.com/xmolecules/jmolecules) is a companion library providing
annotations for DDD building blocks (`@AggregateRoot`, `@Entity`, `@ValueObject`) that integrate
with Spring Modulith's architecture verification.

Further reading:

- [Spring Modulith reference documentation](https://docs.spring.io/spring-modulith/reference/)
- [jMolecules](https://github.com/xmolecules/jmolecules) - DDD annotations for Java

## Naming conventions

Options 1, 2 and 3 have patterns that enforce the same rule - dependencies point inward - but use
different vocabularies. Options 4 and 5 have no port abstractions; several rows in the table below
are not applicable to them.

**Onion architecture** (Jeffrey Palermo,
[2008](https://jeffreypalermo.com/2008/07/the-onion-architecture-part-1/)) expresses the same
constraints using concentric rings. There's no directional port concept, so repository interfaces
live in the Domain Services ring.

**Clean architecture** (Robert C. Martin,
[2012](https://blog.cleancoder.com/uncle-bob/2012/08/13/the-clean-architecture.html)) is also
ring-based and introduces specific terms: **Interactor** (use case implementation), **Gateway**
(repository interface and implementation), and **Boundary** (the interface between rings).

**Hexagonal architecture** (Alistair Cockburn,
[2005](https://alistair.cockburn.us/hexagonal-architecture/)) organises code around explicit
**ports** (interfaces at the application boundary) and **adapters** (concrete implementations
that connect external actors to those ports). The `in`/`out` qualifier used in the package
names below - `adapter/in`, `adapter/out`, `port/in`, `port/out` - follows the convention
established in Tom Hombergs'
[Get Your Hands Dirty on Clean Architecture](https://www.packtpub.com/en-gb/product/get-your-hands-dirty-on-clean-architecture-9781805128373)
(2nd ed., 2023) and indicates direction relative to the application core: `in` adapters drive
the application (HTTP requests), `out` adapters are driven by it (database calls).

Concept mapping across all five options:

| Concept                        | Onion (Option 1)                           | Clean architecture (Option 2)      | Hexagonal (Option 3)                       | Layered (Option 4)                   | Modular layered (Option 5) |
|--------------------------------|--------------------------------------------|------------------------------------|--------------------------------------------|--------------------------------------|----------------------------|
| HTTP controllers + web mappers | `infrastructure/web`                       | `adapter/web`                      | `adapter/in/web`                           | `web/`                               | `{module}/web/`            |
| Command and query objects      | `application/command`, `application/query` | `usecase/command`, `usecase/query` | `application/command`, `application/query` | `service/`                           | `{module}/`                |
| Inbound port interfaces        | `application` (no sub-package)             | `usecase/boundary`                 | `application/port/in`                      | -                                    | -                          |
| Outbound port interfaces       | `domain/service`                           | `usecase/boundary`                 | `application/port/out`                     | -                                    | -                          |
| Use case implementations       | `application`                              | `usecase/interactor`               | `application/usecase`                      | `service/`                           | `{module}/`                |
| JPA entity classes             | `domain/model`                             | `entity/`                          | `domain/`                                  | `entity/`                            | `{module}/`                |
| Spring Data repositories       | `infrastructure/persistence`               | `adapter/persistence`              | `adapter/out/persistence`                  | `repository/`                        | `{module}/repository/`     |

Notes on the table:

- Onion's `domain/service` holds repository **interfaces** because the domain depends on them.
  Their JPA **implementations** are in `infrastructure/persistence`. Palermo's four canonical
  rings are: Domain Model -> Domain Services -> Application Services -> Infrastructure.
- Palermo's original diagrams label the outermost ring "UI". For a REST API there's no UI,
  so the outer ring splits into `infrastructure/web` and `infrastructure/persistence`.
- In clean architecture, both input and output boundaries are in `usecase/boundary` (the Use
  Cases ring). Gateway implementations sit in `adapter/persistence` (Interface Adapters +
  Frameworks & Drivers).
- In layered architecture (Option 4), there are no port abstractions. Controllers call service
  classes directly; services call Spring Data repositories directly. Command and query objects
  act as the data contract between the web and service layers.
- In modular layered architecture (Option 5), `{module}` is a domain-oriented top-level package
  such as `provider`, `office`, or `contract`. There are no port abstractions; Spring Modulith
  module boundaries take their place. Command and query service classes in `{module}/` form the
  public API of each module. Spring Modulith treats every sub-package as module-internal:
  packages under the module root cannot be accessed by other modules.

## Pre-refactoring structure

The codebase previously followed a conventional layered arrangement:

```
uk.gov.justice.laa.providerdata
  config/        Spring configuration
  controller/    Spring MVC controllers
  entity/        JPA entities
  exception/     exception handler, custom exception types
  mapper/        MapStruct mappers (entity <-> OpenAPI model)
  repository/    Spring Data JPA repositories
  service/       Application services
  util/          Pagination, search criteria helpers
```

Controllers call services directly. Services call repositories and mappers. Mappers translate
between JPA entities and the generated OpenAPI model classes. The layer boundaries are implicit
 - there are no interfaces between controller and service, or between service and repository
(beyond the Spring Data interfaces themselves).

## Constraints

These apply regardless of which option is chosen.

### Pragmatic entity model

Each option is presented in its pragmatic form: JPA entity classes serve as the domain model.
They carry JPA annotations, live in a domain-accessible package, and use cases may reference them
directly. There's no mapping layer between domain objects and JPA entities.

The strict alternative - separate pure domain model objects with a mapper at the persistence
boundary - adds significant boilerplate for limited benefit here. The domain model is
straightforward, entity relationships are well-understood, and there's no requirement to swap the
persistence technology. It's not being considered.

For Options 1, 2 and 3, use cases must not import Spring Data interfaces or JPA annotations
directly - only entity classes and plain result types. Repository interfaces are defined in terms
of entity classes and projections. The persistence layer implements them. For Options 4 and 5,
services call Spring Data repositories directly; no repository abstraction layer is introduced.

### CQRS at use-case level

Each API endpoint maps to either a command use case (mutation) or a query use case (read) -
no separate read/write databases. Command use cases can be tested without asserting on query
responses, and query use cases have no side effects.

**Commands** - carry the validated input for a single mutation, constructed by the web layer
and passed to the use case. Examples:

| Command                     | Use case                     |
|-----------------------------|------------------------------|
| `CreateProviderFirmCommand` | `POST /provider-firms`       |
| `UpdateProviderFirmCommand` | `PATCH /provider-firms/{id}` |

**Queries** - carry the parameters for a read. Results are entity classes, projections, or plain
types, and the web layer maps them to the generated OpenAPI response model. Examples:

| Query                     | Use case                           |
|---------------------------|------------------------------------|
| `ProviderFirmSearchQuery` | `GET /provider-firms`              |
| `OfficeSearchQuery`       | `GET /provider-firms/{id}/offices` |

## Options

### Option 1: Onion architecture

```
uk.gov.justice.laa.providerdata
  domain/
    model/             JPA entity classes (innermost ring)
    service/           repository interfaces (domain services ring)
  application/         use case classes + inbound use-case interfaces
    command/
    query/
  infrastructure/
    web/               controllers, request/response mappers
    persistence/       Spring Data repositories
  config/
  exception/
  util/
```

Repository **interfaces** sit in `domain/service` (the domain depends on them). Their Spring
Data **implementations** sit in `infrastructure/persistence`. Use cases in `application` depend
on `domain/service` interfaces only. Controllers in `infrastructure/web` call `application`
use-case interfaces.

**Pros:** the ring metaphor is intuitive, `domain/service` clearly signals where repository
interfaces live, and it's well-known in the Java/Spring ecosystem.

**Cons:** no directional distinction between web and persistence - both are "infrastructure".

### Option 2: Clean architecture

```
uk.gov.justice.laa.providerdata
  entity/              JPA entity classes (Entities ring)
  usecase/
    boundary/          input + output boundary interfaces
    interactor/        use case implementations
    command/
    query/
  adapter/
    web/               controllers, request/response mappers
    persistence/       Spring Data repositories (gateway implementations)
  config/
  exception/
  util/
```

Use case implementations (*Interactors*) live in `usecase/interactor` and depend only on the
boundary interfaces in `usecase/boundary`. Repository interfaces (*output boundaries* /
*Gateways*) are defined in `usecase/boundary`. Their implementations sit in
`adapter/persistence`. Controllers in `adapter/web` call the input boundaries. Entity classes in
`entity/` (Martin's Entities ring) are shared across use cases and adapters.

**Pros:** specific vocabulary (Interactor, Gateway, Boundary) makes the role of each class
unambiguous, and it's widely recognised via Martin's book.

**Cons:** the Presenter concept has no equivalent in a REST API.

### Option 3: Hexagonal (ports and adapters)

```
uk.gov.justice.laa.providerdata
  adapter/
    in/
      web/             controllers, request/response mappers
    out/
      persistence/     Spring Data repositories
  application/
    command/           command objects
    query/             query objects
    port/
      in/              inbound port interfaces (one per use case)
      out/             outbound port interfaces (one per aggregate repository)
    usecase/           use case implementations
  domain/              JPA entity classes
  config/
  exception/
  util/
```

Use cases implement the `port/in` interfaces. Controllers call them via those interfaces.
Repository interfaces in `port/out` are implemented by the persistence adapter. Entity classes in
`domain/` are shared between use cases and the persistence adapter. Example:

```java
// application/port/out/ProviderFirmRepository.java
public interface ProviderFirmRepository {
    Optional<ProviderEntity> findById(UUID id);
    Page<ProviderFirmSummary> search(ProviderFirmSearchQuery query, Pageable pageable);
    ProviderEntity save(ProviderEntity entity);
}
```

The existing service classes map directly to use case implementations.

**Pros:** the `in`/`out` distinction makes dependency direction explicit, and it's easy to add
further delivery mechanisms (messaging, CLI) as additional `adapter/in/...` packages.

**Cons:** `adapter/in`, `adapter/out`, `port/in`, `port/out` are unfamiliar to developers who
haven't encountered Cockburn's terminology. Packages are deeply nested.

### Option 4: Layered architecture (technical layers)

```
uk.gov.justice.laa.providerdata
  config/
  entity/
  event/        event types, listeners, event query service
  exception/
  mapper/
  repository/
  service/      command and query services, result types
  util/
  web/
```

Retains the flat technical layer package structure. Command and query services would coexist in
`service/`, distinguished by naming convention (`CommandService`, `QueryService`) and enforced by
ArchUnit. An `event/` package would hold event types and listeners; command services would publish
events after writes, and listeners would handle downstream delivery (for example, to SQS or SNS).
[Spring Modulith](https://docs.spring.io/spring-modulith/reference/) can treat each top-level
package as a module: `ApplicationModulesTest` would enforce that `web` does not call `repository`
directly, and sub-packages within each layer would be module-private. The event publication
registry would record domain events atomically with entity changes, providing a transactional
outbox without a custom implementation. Hibernate Envers handles audit history independently.

**Pros:** minimal structural change from the current codebase. Familiar to all Spring developers.
Spring Modulith's module concept applies equally to technical layer packages.

**Cons:** as the domain grows, `entity/`, `repository/`, and `service/` packages grow without
internal structure. Coarser domain modules can be introduced later if genuine bounded contexts
emerge.

Further reading:

- Fowler, *[Patterns of Enterprise Application Architecture](https://martinfowler.com/books/eaa.html)* (2002) - chapter 1 introduces the layered architecture
- [Spring MVC reference documentation](https://docs.spring.io/spring-framework/reference/web/webmvc.html) - the conventional Spring layered structure

### Option 5: Modular layered architecture

```
uk.gov.justice.laa.providerdata
  shared/          cross-cutting utilities and base types at ROOT (accessible to all modules).
                   config/ and web/ module-internal
  provider/        domain module. entity types and command/query services at ROOT.
                   repository/ module-internal
  office/          domain module. entity types and command/query services at ROOT.
                   repository/ and web/ module-internal
  bankaccount/     domain module. entity types, command/query services, and mapper at ROOT.
                   repository/ and web/ module-internal
  liaisonmanager/  domain module. entity types and command/query services at ROOT.
                   repository/ and web/ module-internal
  contractmanager/ domain module. entity types and command/query services at ROOT.
                   mapper/, repository/, and web/ module-internal
  usecase/         orchestration module. use-case orchestrators and mappers at ROOT.
                   web/ module-internal
  contract/        new module. built domain-first from the outset
```

Sub-packages of a module (e.g. `provider/internal/`) are module-internal, meaning Spring Modulith
prevents other modules from accessing them. Using sub-packages is a per-module decision. In a small
and simple module, it might make sense to keep everything in the top level module package.

When a single API operation creates or updates entities across multiple modules, an orchestration
module (like `usecase/` above) can coordinate those calls within a single `@Transactional` call.
This keeps the domain modules as single-responsibility while maintaining atomic behaviour.

[Spring Modulith](https://docs.spring.io/spring-modulith/reference/) treats each top-level package
as a module and enforces that all sub-packages are module-internal. Cross-module calls go to a
module's public types or via Spring `ApplicationEvent`. The event publication registry records
domain events atomically with entity changes, providing a transactional outbox without needing a
custom implementation.

**Pros:** module boundaries enforced at the framework level. New domain areas are self-contained.
The event publication registry provides the transactional outbox. Scales naturally as the domain
grows.

**Cons:** requires package reorganisation from the flat layer structure. Per-entity module
granularity can be counterproductive when the API specification is designed around user workflows
rather than aggregate boundaries: an endpoint that spans multiple aggregates forces an
orchestration module that depends on all others, re-creating the coupling the boundaries were
supposed to prevent. Module boundaries at this granularity generate observability noise
(module-crossing spans in distributed tracing) disproportionate to the isolation benefit. Module
granularity should reflect genuine bounded contexts, not entity boundaries. For an API designed
around user workflows, coarser modules (two or three) or flat technical layers may be more
appropriate.

Further reading:

- Evans, *[Domain-Driven Design](https://www.domainlanguage.com/ddd/)* (2003) - bounded contexts
  and aggregate roots are the conceptual basis for domain-oriented modules
- Vernon, *[Implementing Domain-Driven Design](https://www.oreilly.com/library/view/implementing-domain-driven-design/9780133039900/)*
  (2013) - practical guidance on applying DDD in Java
- Drotbohm, [Sliced Onion Architecture](https://odrotbohm.de/2023/07/sliced-onion-architecture/)
  (2023) - application modules as a practical bounded-context implementation (Spring Modulith
  author's own framing of Option 5)
- [Spring Modulith reference documentation](https://docs.spring.io/spring-modulith/reference/)

## Migration approach

**Options 1, 2 and 3**

The migration steps are the same regardless of which is chosen. Only the destination package names
differ. The existing service classes map naturally to use cases in all three structures without
changing their internal logic.

1. Introduce repository interfaces (the outbound port/domain service/output boundary),
   backed by thin wrappers around the existing Spring Data repositories.
2. Update services to depend on those interfaces instead of Spring Data directly.
3. Introduce command and query objects, and update controllers to map to/from them.
4. Define inbound use-case interfaces, have services implement them, and update controllers to
   call those interfaces rather than service classes.
5. Relocate packages to match the chosen structure.
6. Move web mappers to the web adapter (`infrastructure/web`, `adapter/web`, or `adapter/in/web`
   depending on the option).

Each step can be committed independently and verified by the existing integration and
end-to-end test suites.

**Option 4**

1. Separate the mutation methods and read-only methods into dedicated `CommandService` and
   `QueryService` service classes, and use appropriate `@Transactional` annotations on them.
2. Rename the `controller/` package to `web/`.
3. Introduce command and query objects (see [Constraints](#constraints)) and update controllers
   to construct and pass them.
4. Introduce ArchUnit rules enforcing layer boundaries and the command/query split.
5. Introduce [Spring Modulith](https://docs.spring.io/spring-modulith/reference/), add an
   `ApplicationModulesTest`, and create an `event/` package for event types and listeners.
6. Publish domain events from command services using the event publication registry.

**Option 5**

Option 5 can be adopted incrementally alongside other work. New modules (for example,
`contract/`) can be built domain-first immediately; existing modules migrate as time allows.

1. Complete Option 4, or skip directly to module reorganisation.
2. Introduce Spring Modulith and add a module structure test.
3. Create domain-oriented top-level packages. Where domain boundaries are unclear, combine related
   domains in a single module initially and split later once the seams are visible.
4. Move all classes for each domain into the module package. Sub-packages are optional; if used,
   they are module-internal and cannot be accessed from outside.
5. Where operations span multiple domain modules, extract an orchestration module above them.
6. Once domain services are single-responsibility, split combined modules into finer-grained ones.
7. Introduce domain events and the event publication registry for cross-cutting concerns.

Each step can be committed independently and verified by the existing integration and
end-to-end test suites.
