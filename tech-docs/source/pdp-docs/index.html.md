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
- **provider-data-service** - REST API service with CRUD operations interfacing a JPA repository.
- **provider-data-e2e** - End-to-end tests to target a running system.

## Documentation

- [API reference](api/) - OpenAPI specification rendered in multiple formats
- [Getting started](getting-started.html) - Build and run the application locally
- [GitHub workflows](github-workflows.html) - CI/CD pipeline documentation
- [Spring Boot 4.0 migration](spring-boot-4-migration.html) - Migration notes

## Source code

- [GitHub repository](https://github.com/ministryofjustice/laa-provider-data-platform)
