---
source_url: https://github.com/ministryofjustice/laa-provider-data-platform/blob/main/tech-docs/source/pda-r1/api-reference.html.md
title: API reference
weight: 4
---

# API reference

The Provider Details API r1 provides RESTful endpoints for accessing provider data.

The primary documentation for the REST endpoints is the Swagger UI - which is available at each of
the following PDA-r1 environment URLs:

- PDA-r1
  **[dev](https://laa-provider-details-api-dev.apps.live.cloud-platform.service.justice.gov.uk)**
  environment
- PDA-r1
  **[uat](https://laa-provider-details-api-dev.apps.live.cloud-platform.service.justice.gov.uk)**
  environment
- PDA-r1
  **[preprod](https://laa-provider-details-api-dev.apps.live.cloud-platform.service.justice.gov.uk)**
  environment
- PDA-r1 **prod** environment (no Swagger UI)

## Health check

### GET /actuator/health

Returns the health status of the application.

**Response:**

```json
{
  "status": "UP",
  "components": {
    "db": {
      "status": "UP"
    },
    "redis": {
      "status": "UP"
    }
  }
}
```

## Rate limiting

The API enforces rate limiting:

- **RPS**: 100 requests per second
- **RPM**: 6000 requests per minute

Rate limiting is handled by the Cloud Platform ingress.

## Operational hours

The PDA-r1 environments are dependent on connectivity to the ECP environment, so their
operational hours are similar to ECP's. The service is available 07:00 until 21:30.

## More information

For detailed implementation, see [Development](development.html).
