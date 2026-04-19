---
source_url: https://github.com/ministryofjustice/laa-provider-data-platform/blob/main/tech-docs/source/pdp-docs/index.html.md
title: Provider data API (persistent)
weight: 10
---

# Provider data API (persistent)

The **provider data API (persistent)** implements a REST API for retrieving and updating provider
firm and office (and, later on, contract and schedule) data.

## Project structure

The repository includes the following subprojects:

- **provider-data-api** - OpenAPI specification used for generating API stub interfaces and
  documentation.
- **provider-data-service** - Spring Boot REST API backed by a PostgreSQL database.
- **provider-data-e2e** - End-to-end tests to target a running system.

## Documentation

- [Getting started](getting-started.html) - Build and run the application locally
- [API reference](api/index.html.md) - OpenAPI specification rendered in multiple formats
- [Data model](data-model.html) - Key entity and identifier relationships
- [Design](design/index.html.md) - Domain model, architecture, and async events options
- [End-to-end tests](end-to-end-tests.html) - E2E test setup, data management, and usage
- [GitHub workflows](github-workflows.html) - CI/CD pipeline documentation
- [Spring Boot 4.0 migration](spring-boot-4-migration.html) - Migration notes

## Source code

- [GitHub repository](https://github.com/ministryofjustice/laa-provider-data-platform)
