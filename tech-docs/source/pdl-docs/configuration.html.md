---
source_url: https://github.com/ministryofjustice/laa-provider-data-platform/blob/main/tech-docs/source/pdl-docs/configuration.html.md
title: Configuration
weight: 3
---

# Configuration

## Environment variables

Configure the application using environment variables.
In the Cloud Platform, these variables are provided by the Kubernetes deployment resource,
generally populated from secrets.

### Application settings

- `TARGET_ENVIRONMENT`: Deployed environment (dev, staging, prod, local) - used for Sentry and active Spring profile

### Database configuration

- `CCMS_DB_URL`: Database connection URL
- `CCMS_DB_USER`: Database username
- `CCMS_DB_PASSWORD`: Database password
- `CWA_DB_URL`: Database connection URL
- `CWA_DB_USER`: Database username
- `CWA_DB_PASSWORD`: Database password

### Redis configuration

- `REDIS_ENDPOINT`: Redis server host and port (6379)
- `REDIS_PASSWORD`: Redis password

## Application properties

### Local development

For local development, the Spring Boot `local` profile is activated.
So, some configuration overrides for local development are in `application-local.yml`.

## Gradle build properties

Configure GitHub package repository access in `~/.gradle/gradle.properties` to allow
authenticated access to the GitHub Package Registry which contains the LAA Spring Boot
Gradle plugin.

```properties
project.ext.gitPackageUser=YOUR_GITHUB_USERNAME
project.ext.gitPackageKey=YOUR_GITHUB_PERSONAL_ACCESS_TOKEN
```

## Rate limiting configuration

Rate limiting is configured in the Helm values files:

- `helm_deploy/providers-app/values-prod.yaml`
- `helm_deploy/providers-app/values-staging.yaml`
- `helm_deploy/providers-app/values-uat.yaml`

Example configuration:

```yaml
rateLimit:
  rps: "100"    # requests per second
  rpm: "6000"   # requests per minute
  burst: "1"    # burst size
```

## More information

See the main [README.md](https://github.com/ministryofjustice/laa-data-provider-data) for more details.
