---
source_url: https://github.com/ministryofjustice/laa-provider-data-platform/blob/main/tech-docs/source/pdp-docs/index.html.md
title: Provider data API (persistent)
weight: 10
---

# Provider data API (persistent)

The **provider data API (persistent)** implements a RESTful API for retrieving and updating provider firm and office data, 
with planned support for contract and schedule data. The service maintains its own dedicated database, which acts as the 
system of record for all LAA provider data. 

PDA Release 1 (Legacy) demonstrated the potential for accessing provider data but also exposed significant limitations, including lack of 
scalability, governance, and ownership, as well as reliance on legacy systems like ECP CWA. To address these gaps, PDP 
(Provider Data Platform – Persistent) was introduced as an evolution of PDA into a production-ready, enterprise-grade 
service. Unlike PDA, PDP acts as a system of records with its own managed database, enabling full read and write 
capabilities and ownership of a canonical provider data model. 

It removes dependency on CWA and introduces a scalable, 
API-first, event-driven architecture supported by strong engineering, governance, and security controls.

## Project structure

The repository includes the following subprojects:

- **provider-data-api** - OpenAPI specification used for generating API stub interfaces and
  documentation.
- **provider-data-service** - Spring Boot REST API backed by a PostgreSQL database.
- **provider-data-e2e** - End-to-end tests to target a running system.

## Documentation

- [Getting started](getting-started.html) - Build and run the application locally
- [Run book](runbook.html.md) - Operational run book for the application
- [Play book]() - How to use the application
- [API reference](api/) - OpenAPI specification rendered in multiple formats
- [Design](design/) - Domain model, architecture, and async events options
- [End-to-end tests](end-to-end-tests.html) - E2E test setup, data management, and usage
- [GitHub workflows](github-workflows.html) - CI/CD pipeline documentation
- [Spring Boot 4.0 migration](spring-boot-4-migration.html) - Migration notes

## Source code

- [GitHub repository](https://github.com/ministryofjustice/laa-provider-data-platform)
