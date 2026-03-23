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

### SQL-inserted data

The `E2eDatabaseExtension` JUnit extension connects directly to the PostgreSQL database
and runs SQL scripts before and after the test suite:

- **`insert-test-data.sql`** — inserts test data with fixed `e2e00000-*` UUIDs before
  all tests run. A preceding delete ensures idempotency.
- **`delete-test-data.sql`** — removes the inserted data after all tests complete.

This data is separate from the `LocalDataSeeder` and uses distinct identifiers
(e.g. `E2E-LSP-001`, `E2E-CHM-001`) to avoid conflicts.

### POST-created data

Modifying tests create data via POST endpoints, verify it via GET, then clean up via
JDBC in `@AfterAll`. Each test uses timestamp-suffixed names to avoid conflicts between
runs.

## Configuration

### Environment property files

Each environment has a `.properties` file under `src/test/resources/`.

The `local.properties` file includes:

- API base URI (`e2e.baseUri`)
- Database connection details (`db.url`, `db.username`, `db.password`)
- Test data identifiers matching `insert-test-data.sql`

For non-local environments, database credentials must be supplied via environment
variables: `E2E_DB_URL`, `E2E_DB_USERNAME`, `E2E_DB_PASSWORD`.

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

To verify E2E-inserted data:

```sql
SELECT guid, firm_number, name FROM provider WHERE firm_number LIKE 'E2E%';
```

## Gradle notes

- The `e2eTest` task is separate from the main `test` task. Running `./gradlew build`
  will not execute E2E tests.
- Modifying tests always run after read-only tests.
- Modifying tests are blocked from running against non-local environments.
