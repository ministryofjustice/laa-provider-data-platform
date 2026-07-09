---
source_url: https://github.com/ministryofjustice/laa-provider-data-platform/blob/main/tech-docs/source/pda-r1/index.html.md
title: Provider Details API r1
weight: 10
---

# Provider Details API r1

The **Provider Details API r1** is a read-only REST API that retrieves provider data from
Oracle database snapshots. It is deployed to Cloud Platform and serves live traffic.

## Who is this documentation for?

This is technical documentation for LAA Digital teams at the Ministry of Justice who use or develop
the Provider Details API r1 and related services.

## Documentation

### Getting started

- [Getting started](getting-started.html)

### API & services

- [API reference](api-reference.html)
- [Configuration](configuration.html)
- [Deployment](deployment.html)

### Development

- [Development guide](development.html)
- [GitHub workflows](github-workflows.html)

### Testing

- [End-to-end tests](end-to-end-tests.html)

### Operations

- [Zero-downtime infrastructure](zero-downtime-infrastructure.html) - Building blocks for
  zero-downtime operations
- [Zero-downtime cache rotation](zero-downtime-cache-rotation.html) - Reload cache without service
  interruption
- [Zero-downtime database switchover](zero-downtime-database-switchover.html) - Switch between
  database snapshots

### Reference

- [Spring Boot 4.0 migration](spring-boot-4-migration.html)

## Source code

[laa-data-provider-data](https://github.com/ministryofjustice/laa-data-provider-data) (private
repository)
