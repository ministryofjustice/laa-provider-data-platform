---
source_url: https://github.com/ministryofjustice/laa-provider-data-platform/blob/main/tech-docs/source/pdp-docs/end-to-end-tests.html.md
title: End-to-end tests
weight: 30
---

# End-to-end (E2E) API tests

The `provider-data-e2e` module contains end-to-end API tests for the service.

Tests are written in **Java + JUnit 5** using **REST Assured** for API calls and the
**Atlassian Swagger Request Validator** for OpenAPI response validation.

## Test categories

Tests are split into two categories using custom annotations:

- **`@ReadOnlyTest`** — only reads data, safe to run against any environment.
- **`@ModifyingTest`** — creates data via POST endpoints. Restricted to the `local`
  environment by the Gradle task.

## Test data

Test data is provided by the `LocalDataSeeder`, which runs on application startup with
the `local` profile. It populates the database with providers, offices, bank accounts,
contract managers, liaison managers, and their relationships.

Modifying tests create additional data via POST endpoints and verify it via GET. Each
test uses timestamp-suffixed names to avoid conflicts between runs.

## Configuration

### Environment property files

Each environment has a `.properties` file under `src/test/resources/`.

The `local.properties` file includes:

- API base URI (`e2e.baseUri`)
- Test data identifiers matching the `LocalDataSeeder`

### Auth token

The API requires the header `X-Authorization`. Supply the token via system property
or environment variable:

```bash
# System property
./gradlew :provider-data-e2e:e2eTest -De2e.authToken=your-token

# Environment variable
export E2E_AUTHTOKEN=your-token
```

## Running tests

### Prerequisites

1. Start the local PostgreSQL database:

   ```bash
   docker compose up -d
   ```

2. Start the application:

   ```bash
   ./gradlew bootRun
   ```

### Run all E2E tests (read-only + modifying)

```bash
./gradlew :provider-data-e2e:e2eTest -Pe2e.env=local -De2e.authToken=Dummy1
```

### Run read-only tests only

```bash
./gradlew :provider-data-e2e:e2eReadOnly -Pe2e.env=local -De2e.authToken=Dummy1
```

### Run modifying tests only

```bash
./gradlew :provider-data-e2e:e2eModifying -Pe2e.env=local -De2e.authToken=Dummy1
```

### Run a single test class

```bash
./gradlew :provider-data-e2e:e2eReadOnly -Pe2e.env=local -De2e.authToken=Dummy1 \
  --tests "uk.gov.justice.laa.providerdata.e2e.ProviderFirmE2eTest"
```

## Inspecting test data in the database

To query the local database directly:

```bash
docker exec -it laa-provider-data-platform-db-1 psql -U provider -d provider_data
```

## Gradle notes

- The `e2eTest` task is separate from the main `test` task. Running `./gradlew build`
  will not execute E2E tests.
- Modifying tests always run after read-only tests.
- Modifying tests are blocked from running against non-local environments.
