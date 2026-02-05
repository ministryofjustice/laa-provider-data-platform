# Copilot instructions

## Context and communication

- UK Ministry of Justice (LAA Digital) repository.
- Use UK English in all user-facing text (docs, comments, logs, errors, UI).
- Keep prose concise and technical.
- This is a Java/Spring Boot project using Gradle.

## Code quality

- Prioritise correctness, maintainability, and readability over novelty.
- Follow existing conventions (style, formatting, structure).
- Follow OpenAPI spec-first design; maintain backward compatibility for public APIs.
- Use Conventional Commits for commit messages.
- Make small, reviewable changes with accurate descriptions.
- Update tests when behaviour changes; keep tests deterministic and fast.

## Dependencies and configuration

- Avoid unnecessary dependencies; justify additions.
- The `laa-spring-boot-gradle-plugin` Gradle plugin imports Spring Boot and defines
  an additional source-set `src/integrationTest/java` and task (`integrationTest`).
- Do not modify CI, build scripts, or dependency versions without explicit request.
- When proposing build/release changes, explain impact on versioning and compatibility.

## Security

- Treat data as potentially sensitive; never log secrets or personal data.
- Prefer least-privilege configuration and secure defaults.

## Code review

- Prioritise issues that genuinely matter: bugs, security vulnerabilities, logic errors,
  and API breaking changes.
- Do not comment on code formatting, that is enforced using Spotless and Checkstyle.
