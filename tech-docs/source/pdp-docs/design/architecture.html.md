---
source_url: https://github.com/ministryofjustice/laa-provider-data-platform/blob/main/tech-docs/source/pdp-docs/design/architecture.html.md
title: Architecture
weight: 20
---

# Architecture

Three architectural options for `provider-data-service`. All three enforce the same
inward-dependency rule and are compatible with use-case-level CQRS. No framework or technology
changes are required for any of them. The team has not yet decided which approach to take.

## Patterns

> If you know these patterns, skip to [Current structure](#current-structure).

### Layered architecture

The current codebase uses **layered architecture** (*n-tier* or *three-tier*)  -  controllers,
services, repositories, each depending only on the layer below. It's the default Spring
structure and described in Fowler's
*[Patterns of Enterprise Application Architecture](https://martinfowler.com/books/eaa.html)*
(2002). The main drawback is that business logic gets coupled to persistence, which makes it
harder to test in isolation.

### Hexagonal architecture

**Hexagonal architecture** (also *ports and adapters*) was introduced by Alistair Cockburn in
[2005](https://alistair.cockburn.us/hexagonal-architecture/). It inverts the layered model:
the application core defines **ports** (interfaces) and external concerns  -  HTTP, databases,
messaging  -  plug in as **adapters**. Dependencies point inward. The core has no knowledge of
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
[Naming conventions](#naming-conventions) below for a comparison of the two vocabularies.

Further reading:

- Palermo, [The Onion Architecture](https://jeffreypalermo.com/2008/07/the-onion-architecture-part-1/) (parts 1-4, 2008)

### Clean architecture

**Clean architecture** was described by Robert C. Martin in a
[2012 blog post](https://blog.cleancoder.com/uncle-bob/2012/08/13/the-clean-architecture.html)
and expanded in his book (2017). Martin credits Cockburn and Palermo as predecessors. Like
Onion, it uses concentric rings. The vocabulary it introduces:

- **Entities** (innermost)  -  enterprise-wide business rules, domain objects.
- **Use Cases**  -  application-specific rules. The ring also contains the input and output **boundary**
  interfaces between the use case and its callers/dependencies.
- **Interface Adapters**  -  **Controllers** (map HTTP requests to use-case inputs), **Presenters**
  (map use-case output for delivery), and **Gateways** (concrete repository implementations).
- **Frameworks & Drivers** (outermost)  -  Spring, Hibernate, the database itself.

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
- Microsoft, [CQRS pattern](https://learn.microsoft.com/en-us/azure/architecture/patterns/cqrs)  -  Azure Architecture Center

## Naming conventions

All three patterns enforce the same rule  -  dependencies point inward  -  but use different
vocabularies. The package naming here follows the hexagonal convention from Hombergs (2023).

**Hexagonal architecture** (Alistair Cockburn,
[2005](https://alistair.cockburn.us/hexagonal-architecture/)) organises code around explicit
**ports** (interfaces at the application boundary) and **adapters** (concrete implementations
that connect external actors to those ports). The `in`/`out` qualifier used in the package
names below  -  `adapter/in`, `adapter/out`, `port/in`, `port/out`  -  follows the convention
established in Tom Hombergs'
[Get Your Hands Dirty on Clean Architecture](https://www.packtpub.com/en-gb/product/get-your-hands-dirty-on-clean-architecture-9781805128373)
(2nd ed., 2023) and indicates direction relative to the application core: `in` adapters drive
the application (HTTP requests), `out` adapters are driven by it (database calls).

**Onion architecture** (Jeffrey Palermo,
[2008](https://jeffreypalermo.com/2008/07/the-onion-architecture-part-1/)) expresses the same
constraints using concentric rings. There's no directional port concept, so repository interfaces
live in the Domain Services ring.

**Clean architecture** (Robert C. Martin,
[2012](https://blog.cleancoder.com/uncle-bob/2012/08/13/the-clean-architecture.html)) is also
ring-based and introduces specific terms: **Interactor** (use case implementation), **Gateway**
(repository interface and implementation), and **Boundary** (the interface between rings).

Concept mapping across the three conventions:

| Concept | Hexagonal | Onion | Clean architecture |
|---|---|---|---|
| HTTP controllers + web mappers | `adapter/in/web` | `infrastructure/web` | `adapter/web` |
| Command and query objects | `application/command`, `application/query` | `application/command`, `application/query` | `usecase/command`, `usecase/query` |
| Inbound port interfaces | `application/port/in` | `application` (no sub-package) | `usecase/boundary` |
| Outbound port interfaces | `application/port/out` | `domain/service` | `usecase/boundary` |
| Use case implementations | `application/usecase` | `application` | `usecase/interactor` |
| JPA entity classes | `domain/` | `domain/model` | `entity/` |
| Spring Data repositories | `adapter/out/persistence` | `infrastructure/persistence` | `adapter/persistence` |

Notes on the table:

- Onion's `domain/service` holds repository **interfaces** because the domain depends on them.
  Their JPA **implementations** are in `infrastructure/persistence`. Palermo's four canonical
  rings are: Domain Model -> Domain Services -> Application Services -> Infrastructure.
- In clean architecture, both input and output boundaries are in `usecase/boundary` (the Use
  Cases ring). Gateway implementations sit in `adapter/persistence` (Interface Adapters +
  Frameworks & Drivers).
- Palermo's original diagrams label the outermost ring "UI". For a REST API there's no UI,
  so the outer ring splits into `infrastructure/web` and `infrastructure/persistence`.

## Current structure

The codebase follows a conventional layered arrangement:

```
uk.gov.justice.laa.providerdata
  config/        Spring configuration, LocalDataSeeder
  controller/    Spring MVC controllers
  entity/        JPA entities
  exception/     GlobalExceptionHandler, ItemNotFoundException
  mapper/        MapStruct mappers (entity <-> OpenAPI model)
  repository/    Spring Data JPA repositories
  service/       Application services
  util/          Pagination, search criteria helpers
```

Controllers call services directly. Services call repositories and mappers. Mappers translate
between JPA entities and the generated OpenAPI model classes. The layer boundaries are implicit
 -  there are no interfaces between controller and service, or between service and repository
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

Use cases must not import Spring Data interfaces or JPA annotations directly - only entity classes
and plain result types. Repository interfaces are defined in terms of entity classes and
projections. The persistence layer implements them.

### CQRS at use-case level

Each API endpoint maps to either a command use case (mutation) or a query use case (read)  - 
no separate read/write databases. Command use cases can be tested without asserting on query
responses, and query use cases have no side effects.

**Commands**  -  carry the validated input for a single mutation, constructed by the web layer
and passed to the use case. Examples:

| Command | Use case |
|---|---|
| `CreateProviderFirmCommand` | `POST /provider-firms` |
| `CreateOfficeCommand` | `POST /provider-firms/{id}/offices` |
| `AssignLiaisonManagerCommand` | `POST /provider-firms/{id}/offices/{officeId}/liaison-managers` |
| `AssignContractManagerCommand` | `POST /provider-firms/{id}/offices/{officeId}/contract-managers` |
| `UpdateProviderFirmCommand` | `PATCH /provider-firms/{id}` |
| `UpdateOfficeCommand` | `PATCH /provider-firms/{id}/offices/{officeId}` |

**Queries**  -  carry the parameters for a read. Results are entity classes, projections, or plain
types, and the web layer maps them to the generated OpenAPI response model. Examples:

| Query | Use case |
|---|---|
| `ProviderFirmSearchQuery` | `GET /provider-firms` |
| `OfficeSearchQuery` | `GET /provider-firms/{id}/offices`, `GET /provider-firms-offices` |
| `LiaisonManagerHistoryQuery` | `GET /provider-firms/{id}/offices/{officeId}/liaison-managers` |
| `ContractManagerSearchQuery` | `GET /provider-contract-managers` |

The controllers that currently omit `implements <Api>` (due to the `oneOf`/`@Valid` propagation
issue described in the Java instructions) continue to do so under any option.

## Options

### Option 1: Hexagonal (ports and adapters)

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
    Optional<ProviderFirmEntity> findById(UUID id);
    Page<ProviderFirmSummary> search(ProviderFirmSearchQuery query, Pageable pageable);
    ProviderFirmEntity save(ProviderFirmEntity entity);
}
```

The existing services map directly to use case classes: `ProviderCreationService` ->
`CreateProviderFirmUseCase`, `OfficeService` -> `CreateOfficeUseCase`, etc.

**Pros:** the `in`/`out` distinction makes dependency direction explicit, and it's easy to add
further delivery mechanisms (messaging, CLI) as additional `adapter/in/...` packages.

**Cons:** `adapter/in`, `adapter/out`, `port/in`, `port/out` are unfamiliar to developers who
haven't encountered Cockburn's terminology. Packages are deeply nested.

### Option 2: Onion architecture

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

**Cons:** no directional distinction between web and persistence  -  both are "infrastructure".

### Option 3: Clean architecture

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

## Migration approach

The migration steps are the same regardless of which option is chosen. Only the destination
package names differ. The existing services  -  `ProviderCreationService`, `OfficeService`,
`BankDetailsService`, `OfficeLiaisonManagerService`, `OfficeContractManagerAssignmentService`
 -  map naturally to use cases in all three structures without changing their internal logic.

1. Introduce repository interfaces (the outbound port/domain service/output boundary),
   backed by thin wrappers around the existing Spring Data repositories.
2. Update services to depend on those interfaces instead of Spring Data directly.
3. Introduce command and query objects, and update controllers to map to/from them.
4. Define inbound use-case interfaces, have services implement them, and update controllers to
   call those interfaces rather than service classes.
5. Relocate packages to match the chosen structure.
6. Move mappers to their respective layers.

Each step can be committed independently and verified by the existing integration and
end-to-end test suites.

