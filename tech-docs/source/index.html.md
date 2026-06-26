---
source_url: https://github.com/ministryofjustice/laa-provider-data-platform/blob/main/tech-docs/source/index.html.md
title: LAA Digital Provider Data APIs documentation
---

<div class="app-masthead">
  <div class="app-width-container">
    <div class="govuk-grid-row">
      <div class="govuk-grid-column-full-from-desktop">
        <p class="govuk-heading-xl app-masthead__title">
          LAA Digital Provider Data APIs documentation
        </p>
        <p class="app-masthead__description">
          Technical documentation for the Provider Data APIs and services
        </p>
      </div>
    </div>
  </div>
</div>

# Provider Data APIs

## Who is this documentation for?

This is technical documentation for LAA Digital teams at the Ministry of Justice who use or develop
the Provider Data APIs and related services.

## What is this documentation about?

The generic term **Provider Data APIs** - also sometimes abbreviated as **PDA** - can refer to:

* The **[Provider Details API (R1)](#provider-details-api-r1)** exposes data about
  provider firms, offices, contracts and schedules from CWA
* The **[Provider Data API (R2)](#provider-data-api-r2)**  will own data about
  provider firms and offices (and, later on, contracts and schedules)

---

## Provider Details API (R1)

The **[Provider Details API (R1)](pda-r1/)** exposes data about provider firms, offices,
contracts and schedules from CWA.

Most data is currently sourced from the CWA database (a few entities are augmented with a CCMS ID,
sourced from the CCMS database), so it follows ECP operational hours.

Also known as **[PDA-r1](pda-r1/)**, the API is a collection of read-only REST endpoints
that retrieve provider data from CWA Oracle EBS database snapshots. It is deployed to the MoJ Cloud
Platform and serves live traffic to a variety of consumer LAA services.

---

## Provider Data API (R2)

The **[Provider Data API (R2)](pda-r2/)** will own data about provider firms and offices
(and, later on, contracts and schedules).

Also known as **[PDA-r2](pda-r2/)**, the service has its own data store, and a
collection of REST endpoints for creating, retrieving and updating provider firm and office data
from that data store (which will initially be synchronized to CWA). It is under active development
and its primary initial consumer will be the **Manage a provider's data** service.

---

## Contributing

If there's something missing, please either let us know and we'll add a new article, or if you're
comfortable writing one yourself, PRs will be gratefully received.

The "GitHub" link at the top right of this page will take you to the repository for this
documentation.
