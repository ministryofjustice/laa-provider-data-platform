# Copilot instructions

## Context and communication

- UK Ministry of Justice (LAA Digital) repository.
- Use UK English in all user-facing text (docs, comments, logs, errors, UI).
- Keep prose concise and technical.
- This is a Java 25 / Spring Boot 4 / Gradle project with three sub-projects:
  - `provider-data-api` - OpenAPI spec (`laa-data-pda.yml`) and generated interfaces/models,
    published as a library to GitHub Packages.
  - `provider-data-service` - Spring Boot application (controller-service-repository, JPA, Flyway).
  - `provider-data-e2e` - REST Assured end-to-end tests; no Spring context.
- Many StackOverflow answers, AI training data, and third-party documentation examples target
  Spring Boot 2/3 or Jackson 2. Check the sections below for differences that have already
  caused incorrect work in this repo.

## Code quality

- Prioritise correctness, maintainability, and readability over novelty.
- Follow existing conventions (style, formatting, structure).
- Follow OpenAPI spec-first design; maintain backward compatibility for public APIs.
- Use Conventional Commits for commit messages.
- Make small, reviewable changes with accurate descriptions.
- Update tests when behaviour changes; keep tests deterministic and fast.
- Three test levels exist:
  - `src/test/java` - unit and Spring MVC slice tests (`@WebMvcTest`, `@DataJpaTest`).
    `@WebMvcTest` still exists in Spring Boot 4; do not replace it with standalone
    `MockMvcBuilders` — stale advice sometimes suggests this incorrectly.
  - `src/integrationTest/java` - full-context tests using Testcontainers PostgreSQL
    via `PostgresqlSpringBootTest`; run with `./gradlew integrationTest`. Only tests
    that depend on an external system (i.e. Testcontainers) belong here — a plain
    `@SpringBootTest` without Testcontainers stays in `src/test/java`.
  - `provider-data-e2e` - REST Assured tests tagged `@ReadOnlyTest` / `@ModifyingTest`;
    run with `./gradlew :provider-data-e2e:e2eReadOnly` (or `e2eTest`).

## Dependencies and configuration

- Avoid unnecessary dependencies; justify additions.
- The `laa-spring-boot-gradle-plugin` Gradle plugin imports Spring Boot and defines
  an additional source-set `src/integrationTest/java` and task (`integrationTest`).
  It is hosted in a **private GitHub Packages registry**
  (`maven.pkg.github.com/ministryofjustice/laa-spring-boot-common`) and requires
  `GITHUB_ACTOR` and `GITHUB_TOKEN` environment variables (or `gitPackageUser` /
  `gitPackageKey` in `~/.gradle/gradle.properties`) — every `./gradlew` command fails
  with an authentication error without these, even tasks that do not use the plugin directly.
- `GET /trace/{depth}` (`TraceController`) is a local-profile-only debug endpoint that walks
  provider relationships. It is not in the OpenAPI spec and is not for production use.
- Do not modify CI, build scripts, or dependency versions without explicit request.
- When proposing build/release changes, explain impact on versioning and compatibility.
- Spring Boot 4 uses **Jackson 3**: hand-written code uses `tools.jackson.*` (core, databind).
  The exception is `com.fasterxml.jackson.annotation.*` (`@JsonProperty`, `@JsonSubTypes`,
  `@JsonTypeInfo`, etc.) — the annotation package was not moved in Jackson 3 and remains in
  `com.fasterxml`. The generated OpenAPI model classes use it. Do not replace these annotation
  imports with `tools.jackson`.
- Entities extend `AuditableEntity` and use Lombok: `@SuperBuilder`, `@NoArgsConstructor`,
  `@Getter`, `@Setter`. Do not use records or plain constructors for JPA entities.
- Entity-to-API-model mapping uses MapStruct (`componentModel = "spring"`) in the `mapper`
  package. Add new mappings there; do not map manually in services or controllers.

## Security

- Treat data as potentially sensitive; never log secrets or personal data.
- Prefer least-privilege configuration and secure defaults.
- No Spring Security dependency is present; all endpoints are unauthenticated at runtime.
  The OpenAPI spec defines `bearerAuth` and `AzureAD` security schemes, but they are not
  enforced. This is temporary — Entra ID OAuth2 authentication and authorisation are planned
  but not yet implemented.

## Code review

- Prioritise issues that genuinely matter: bugs, security vulnerabilities, logic errors,
  and API breaking changes.
- Do not comment on code formatting, that is enforced using Spotless and Checkstyle.

## Documentation

The `tech-docs/` folder contains a Middleman technical documentation site that describes
[this repository](https://github.com/ministryofjustice/laa-provider-data-platform)
and [another repository](https://github.com/ministryofjustice/laa-data-provider-data).
Content for this repository belongs in the `tech-docs/source/pda-r2/` directory.

- Keep documentation brief and scannable.
- Link to authoritative sources like the
  [Cloud Platform user guide](https://user-guide.cloud-platform.service.justice.gov.uk/)
  or the [Spring Boot reference](https://docs.spring.io/spring-boot/reference/index.html)
  rather than duplicating their content.
- Use headings consistently: `#` for page title, `##` for sections, `###` for subsections.
- Prefer sentence case in titles and headings.
- Documentation files use `.html.md` extension. Use only YAML frontmatter and Markdown for
  content in documentation files; no ERB templating.
- Every page must declare an explicit `source_url` in its YAML front matter — the gem's
  auto-generation produces the wrong path in a monorepo (missing the `tech-docs/` prefix).
- Line-wrap documentation source files at 100 characters so Git diffs are readable.
