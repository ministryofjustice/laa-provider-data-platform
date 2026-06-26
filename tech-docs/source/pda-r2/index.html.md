---
source_url: https://github.com/ministryofjustice/laa-provider-data-platform/blob/main/tech-docs/source/pda-r2/index.html.md
title: Provider Data API (R2)
weight: 10
---

# Provider Data API (R2)

The **Provider Data API (R2)** implements a REST API for retrieving and updating provider
firm and office (and, later on, contract and schedule) data.

## Project structure

The repository includes the following subprojects:

- **provider-data-api** - OpenAPI specification used for generating API stub interfaces and
  documentation.
- **provider-data-service** - Spring Boot REST API backed by a PostgreSQL database.
- **provider-data-e2e** - End-to-end tests to target a running system.

## Documentation

- [Getting started](getting-started.html) - Build and run the application locally
- [API reference](api/) - OpenAPI specification rendered in multiple formats
- [Design](design/) - Domain model, architecture, and async events options
- [End-to-end tests](end-to-end-tests.html) - E2E test setup, data management, and usage
- [GitHub workflows](github-workflows.html) - CI/CD pipeline documentation
- [Spring Boot 4.0 migration](spring-boot-4-migration.html) - Migration notes

## Source code

- [GitHub repository](https://github.com/ministryofjustice/laa-provider-data-platform)
