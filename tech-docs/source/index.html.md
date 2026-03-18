---
source_url: https://github.com/ministryofjustice/laa-provider-data-platform/blob/main/tech-docs/source/index.html.md
title: LAA provider data APIs documentation
---

<div class="app-masthead">
  <div class="app-width-container">
    <div class="govuk-grid-row">
      <div class="govuk-grid-column-full-from-desktop">
        <p class="govuk-heading-xl app-masthead__title">
          LAA provider data APIs documentation
        </p>
        <p class="app-masthead__description">
          Technical documentation for LAA provider data APIs and services
        </p>
      </div>
    </div>
  </div>
</div>

# Provider data APIs

## Who is this documentation for?

This is technical documentation for Digital teams at the Ministry of Justice who use or develop
LAA provider data APIs and related services.

## What is this documentation about?

The generic term **provider data APIs** - also sometimes abbreviated as **PDA** - can refer to:

* The **[provider details API (legacy)](#provider-details-api-legacy)** exposes data about
  provider firms, offices, contracts and schedules from CWA
* The **[provider data API (persistent)](#provider-data-api-persistent)**  will own data about
  provider firms and offices (and, later on, contracts and schedules)

---

## Provider details API (legacy)

The **[provider details API (legacy)](pdl-docs/)** exposes data about provider firms, offices,
contracts and schedules from CWA.

Most data is currently sourced from the CWA database (a few entities are augmented with a CCMS ID,
sourced from the CCMS database), so it follows ECP operational hours.

Also known as **[PDA (legacy)](pdl-docs/)**, the API is a collection of read-only REST endpoints
that retrieve provider data from CWA Oracle EBS database snapshots. It is deployed to the MoJ Cloud
Platform and serves live traffic to a variety of consumer LAA services.

---

## Provider data API (persistent)

The **[provider data API (persistent)](pdp-docs/)** will own data about provider firms and offices
(and, later on, contracts and schedules).

Also known as **[PDA (persistent)](pdp-docs/)**, the service has its own data store, and a
collection of REST endpoints for creating, retrieving and updating provider firm and office data
from that data store (which will initially be synchronized to CWA). It is under active development
and its primary initial consumer will be the **Manage a provider's data** service.

---

## Contributing

If there's something missing, please either let us know and we'll add a new article, or if you're
comfortable writing one yourself, PRs will be gratefully received.

The "GitHub" link at the top right of this page will take you to the repository for this
documentation.
