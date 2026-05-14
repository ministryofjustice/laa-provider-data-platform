---
title: CQRS, clean architecture
date: 2026-05-14
status: completed
---

## Clean architecture in this project

<p>Clean Architecture structures the project and nice and easy to read way. Over `46` files were refactored in order to achieve a working provider-firms ID GET endpoint. For a complete clean architecture migration of the service (not just provider-firm command flow), typical remaining effort is:
Medium to large</p>

<p>Clean architecture organises code so business rules are central and frameworks/databases/HTTP are outer details.
In our context:</p>

- Inner layers: domain, application (use cases, policies, ports)
- Outer layers: adapters.in.web (controllers), adapters.out.* (JPA, messaging), infrastructure (Spring wiring/scheduling)
- Core rule: dependencies point inwards only.

<p>So controllers, repositories, and messaging adapt to use cases — not the other way round.</p>

<p>Remaining effort usually includes:</p>

- Moving remaining CRUD/query flows to use cases + ports
- Replacing leaked generated/JPA models at boundaries
- Tightening ArchUnit rules

## Clean architecture scope in POC

### Structural guardrails (project-level)

- Added clean architecture package scaffold:
  - `domain`
  - `application`
  - `adapters.in.web`
  - `adapters.out.persistence`
  - `adapters.out.messaging`
  - `infrastructure`
- Added ArchUnit rules to enforce dependency direction and prevent inward-layer leakage.

### Application boundary introduced (feature-level)

- Introduced an application use-case path for provider update commands:
  - `UpdateProviderFirmUseCase`
  - `UpdateProviderFirmCommand`
- Added outbound ports used by the use case:
  - patch persistence port
  - event/outbox-related ports
- Added adapters implementing those ports:
  - persistence adapter(s)
  - messaging/publisher adapter(s)

### Controller refactor (targeted)

- `ProviderFirmController` update operations were rewired to use the use-case boundary.
- This affected:
  - `POST /provider-firms/{providerFirmGUIDorFirmNumber}` (command endpoint)
  - `PATCH /provider-firms/{providerFirmGUIDorFirmNumber}` (existing update endpoint behaviour aligned)


### Pros

| Area | Benefit |
|---|---|
| Dependency direction | Prevents framework leakage into core use cases |
| Testability | Ports/adapters allow cheap isolation in unit tests |
| Maintainability | Clear boundaries for domain/application/infrastructure |
| Extensibility | Easier swap from fake to real integrations |
| Governance | ArchUnit makes architecture enforceable |

### Cons

| Area | Cost |
|---|---|
| Complexity | More classes/interfaces to navigate |
| Migration overhead | Temporary dual-path behaviour during refactor |
| Boundary friction | Generated API/JPA models still coupled in places |
| Team discipline | Requires consistent adherence to rules |
| Spring Boot traditional layout | Moves away from the layout and approuch that Java Springboot projects normally follow |
| Build/test size | Larger test matrix and runtime |

---
