---
source_url: https://github.com/ministryofjustice/laa-provider-data-platform/blob/main/tech-docs/source/pdp-docs/data-model.html.md
title: Data model
weight: 15
---

# Data model

This page records schema rules that are easy to miss when reading only the entity and API names.

## Core entities and link tables

- `PROVIDER` stores the provider firm or practitioner record.
- `OFFICE` stores the reusable office record, such as address, contact, and DX details.
- `PROVIDER_OFFICE_LINK` stores the provider-specific relationship to an office.
- API-facing office identifiers usually refer to the link record, not the underlying office record.
- This means the same `OFFICE` record can have different meaning depending on which
  `PROVIDER_OFFICE_LINK` row is being used.

## Office identifiers

- `OFFICE.GUID` identifies the underlying office record.
- `PROVIDER_OFFICE_LINK.GUID` identifies the association between a provider and an office.
- In most API request and response fields, `officeGUID` means
  `PROVIDER_OFFICE_LINK.GUID`, not `OFFICE.GUID`.
- `OFFICE.GUID` is mainly used internally when creating an office record and linking it to a
  `ProviderOfficeLinkEntity`.

## Office manager link tables

- `OFFICE_CONTRACT_MANAGER_LINK.OFFICE_GUID` is a foreign key to `PROVIDER_OFFICE_LINK.GUID`.
- `OFFICE_LIAISON_MANAGER_LINK.OFFICE_GUID` is a foreign key to `PROVIDER_OFFICE_LINK.GUID`.
- These columns do not point to `OFFICE.GUID`, despite the column name.
- In JPA, both tables therefore map `OFFICE_GUID` to `ProviderOfficeLinkEntity officeLink`.

## Provider office links

- `PROVIDER_OFFICE_LINK` is the main join table between providers and offices.
- `PROVIDER_OFFICE_LINK.ACCOUNT_NUMBER` is the office code used in API requests alongside the
  provider-office-link GUID.
- `HEAD_OFFICE_FLAG` is stored on the link, not on the office.
- For advocates, the linked office record is a Chambers office, but the advocate still has its own
  provider-office-link row.

## Provider office link inheritance

- `PROVIDER_OFFICE_LINK` uses single-table inheritance with `FIRM_TYPE` as the discriminator.
- `LspProviderOfficeLinkEntity`, `ChamberProviderOfficeLinkEntity`, and
  `AdvocateProviderOfficeLinkEntity` all map to the same table.
- Some columns are only meaningful for certain firm types, but they still exist physically for all
  rows in the table.
- Required rules for type-specific fields are therefore mainly enforced in service logic and
  request validation rather than by `NOT NULL` constraints.

## Parent provider relationships

- `PROVIDER_PARENT_LINK` links a provider to a parent provider.
- This table is currently used for advocates and their parent Chambers.
- Parent-child relationships are separate from office links and should not be inferred only from
  `OFFICE` or `PROVIDER_OFFICE_LINK`.

## Bank account links

- `BANK_ACCOUNT` stores the reusable bank account details.
- `PROVIDER_BANK_ACCOUNT_LINK` links a bank account directly to a provider.
- `OFFICE_BANK_ACCOUNT_LINK` links a bank account to a `PROVIDER_OFFICE_LINK`, not to `OFFICE`.
- Office-level payment data is therefore attached to the provider-office-link layer.
- `OFFICE_BANK_ACCOUNT_LINK.PRIMARY_FLAG` identifies the current primary office bank account, with
  older links retained as historical records.

## Practical implications

- Resolve `officeGUIDorCode` using `ProviderOfficeLinkEntity.guid` or
  `ProviderOfficeLinkEntity.accountNumber`.
- When naming repository or service methods, prefer names that make the provider-office-link
  semantics explicit.
- Avoid using `OfficeEntity.guid` in API-facing identifiers unless an endpoint explicitly documents
  that behaviour.
