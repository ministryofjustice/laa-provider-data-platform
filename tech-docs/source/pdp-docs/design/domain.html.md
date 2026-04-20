---
source_url: https://github.com/ministryofjustice/laa-provider-data-platform/blob/main/tech-docs/source/pdp-docs/design/domain.html.md
title: Domain model
weight: 10
---

# Domain model

How the domain aggregates map to the underlying JPA entities and database tables, and how the
existing API endpoints interact with them.

## Domain-driven design

> If you know DDD, skip to [Aggregates](#aggregates).

Domain-driven design (DDD) is an approach to software modelling in which the structure of the
code reflects the language and boundaries of the business domain. Key concepts used in this
document are: **aggregate** (a cluster of objects treated as a unit for data changes, with a
single **aggregate root** through which all access flows), **bounded context** (an explicit
boundary within which a domain model applies), and **ubiquitous language** (shared terminology
used consistently by both developers and domain experts).

Definitive sources:

- Eric Evans, *[Domain-Driven Design: Tackling Complexity in the Heart of Software](https://www.oreilly.com/library/view/domain-driven-design-tackling/0321125215/)* (2003) - the original text
- Vaughn Vernon, *[Implementing Domain-Driven Design](https://www.oreilly.com/library/view/implementing-domain-driven-design/9780133039900/)* (2013) - practical guidance
- Martin Fowler, [DomainDrivenDesign](https://martinfowler.com/bliki/DomainDrivenDesign.html) - concise overview
- Domain Language, [DDD Reference](https://www.domainlanguage.com/ddd/reference/) - pattern glossary

## Aggregates

The domain is organised around five aggregates. Each has a single root entity. Other aggregates
refer to it by GUID only, with no object references crossing boundaries in the read model.

Write operations are an exception to this. Creation endpoints such as `POST /provider-firms`
and `POST /provider-firms/{id}/offices` span multiple aggregate boundaries in a single database
transaction to maintain atomicity. `BankAccount` and `LiaisonManager` are modelled as separate
aggregates because a single instance can be shared across two distinct `ProviderOffice`
instances (for example, an advocate inherits its parent chambers' liaison manager). Neither has
a standalone creation endpoint - both are always written as a side effect of another aggregate's
operation.

| Aggregate         | Root entity / table                                 | Member entities / tables                                                                                                                                                                         |
|-------------------|-----------------------------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `ProviderFirm`    | `ProviderEntity` / `PROVIDER`                       | `ProviderParentLinkEntity` / `PROVIDER_PARENT_LINK`, `ProviderBankAccountLinkEntity` / `PROVIDER_BANK_ACCOUNT_LINK`                                                                              |
| `ProviderOffice`  | `ProviderOfficeLinkEntity` / `PROVIDER_OFFICE_LINK` | `OfficeBankAccountLinkEntity` / `OFFICE_BANK_ACCOUNT_LINK`, `OfficeContractManagerLinkEntity` / `OFFICE_CONTRACT_MANAGER_LINK`, `OfficeLiaisonManagerLinkEntity` / `OFFICE_LIAISON_MANAGER_LINK` |
| `BankAccount`     | `BankAccountEntity` / `BANK_ACCOUNT`                | -                                                                                                                                                                                                |
| `LiaisonManager`  | `LiaisonManagerEntity` / `LIAISON_MANAGER`          | -                                                                                                                                                                                                |
| `ContractManager` | `ContractManagerEntity` / `CONTRACT_MANAGER`        | -                                                                                                                                                                                                |

`OfficeEntity` / `OFFICE` is shared reference data, not an aggregate root. See
[OfficeEntity as reference data](#officeentity-as-reference-data) below.

## ProviderFirm

`ProviderFirm` represents a legal services provider, chambers, or advocate practitioner. The
root entity `ProviderEntity` uses JPA single-table inheritance with a `@DiscriminatorFormula`
that combines the `FIRM_TYPE` and `ADVOCATE_TYPE` columns. The advocate subtypes share an
intermediate abstract class `PractitionerEntity`. Concrete subtypes are `LspProviderEntity`,
`ChamberProviderEntity`, `AdvocatePractitionerEntity`, and `BarristerPractitionerEntity`.

Members:

- `ProviderParentLinkEntity` - the parent chambers for an advocate or barrister practitioner.
  Has no meaning outside the context of the child provider, always accessed via `ProviderFirm`.
- `ProviderBankAccountLinkEntity` - records that a bank account has been registered against a
  provider. This is the provider-level association. Office-level associations are in
  `ProviderOffice`.

`ProviderFirm` holds references by GUID to any `BankAccount` aggregates registered against it.

### API endpoints

`POST /provider-firms` creates a complete provider firm record in a single transaction.
Depending on the firm type, this may also create a `ProviderOffice` (LSP and Chambers always
have a head office), and optionally a `BankAccount` and a `LiaisonManager` - all as side
effects of the `ProviderFirm` creation use case.

`GET /provider-firms` returns a paginated list of providers, with optional filters for GUID,
firm number, name, active status, type, account number, practitioner roll number, and parent
firm. `GET /provider-firms/{id}` returns the full record for a single provider, including
type-specific sub-objects for the head office relationship and, for practitioners, parent firms.

`PATCH /provider-firms/{id}` is partially implemented. It supports updating firm name, LSP
basic details, and practitioner parent firm (including re-linking the office and liaison manager
on parent change). Some use cases described in the spec - such as changing the head office of
an LSP - are still in development.

`GET /provider-firms/{id}/practitioners` returns a paginated list of practitioners belonging to
a given chambers.

## ProviderOffice

`ProviderOffice` represents the relationship between a provider firm and a physical office
location. Despite the name, most data the business considers "office data" is stored here, not
in `OfficeEntity`. The root entity `ProviderOfficeLinkEntity` uses single-table inheritance with
subtypes `LspProviderOfficeLinkEntity`, `ChamberProviderOfficeLinkEntity`, and
`AdvocateProviderOfficeLinkEntity`, which hold firm-type-specific fields such as payment method,
VAT registration, and intervention flags.

Members:

- `OfficeBankAccountLinkEntity` - which bank account is active for this office association,
  including effective date range and primary flag. References `BankAccount` by GUID.
- `OfficeContractManagerLinkEntity` - the contract manager assigned to this office. References
  `ContractManager` by GUID. LSP offices only.
- `OfficeLiaisonManagerLinkEntity` - current and historical liaison managers for this office.
  References `LiaisonManager` by GUID. Includes effective date range to support the
  active/historical query pattern.

`ProviderOffice` also holds a reference by GUID to the `OfficeEntity` address record. The
`ProviderOffice` GUID is the primary identifier for an office in API requests and responses. The
underlying `OfficeEntity` GUID is used only internally when creating and linking the address
record.

### API endpoints

`POST /provider-firms/{id}/offices` creates a new office for an LSP provider, creating the
`OfficeEntity` address record and the `LspProviderOfficeLinkEntity` in a single transaction,
and optionally creating or linking a `LiaisonManager` and creating or linking a `BankAccount`.

`GET /provider-firms/{id}/offices` returns paginated offices for a provider.
`GET /provider-firms-offices` is a global office search across all providers.
`GET /provider-firms/{id}/offices/{officeId}` returns the full detail for a single office.

`PATCH /provider-firms/{id}/offices/{officeId}` updates contact details (address, telephone,
email, website, DX). Contact fields are split between `OfficeEntity` (address and contact data)
and `ProviderOfficeLinkEntity` (website). The patch is applied to both in the same transaction.
Financial fields (payment method, VAT, flags) and bank account assignment are still in
development.

## BankAccount

`BankAccountEntity` records bank sort code and account number. It's an aggregate root because
bank accounts can be shared: a single account can be linked to multiple providers via
`ProviderBankAccountLink` and to multiple office associations via `OfficeBankAccountLink`.

Bank accounts have no standalone creation endpoint. They're created only as a side effect of
firm creation (`POST /provider-firms`) or office creation
(`POST /provider-firms/{id}/offices`), when the request includes
`payment.paymentMethod=EFT` with `bankAccountDetails`. The creation flow saves the account,
creates the `ProviderBankAccountLink`, and creates the `OfficeBankAccountLink` in one
transaction. An existing account can be linked by GUID.

`GET /provider-firms/{id}/bank-details` retrieves bank accounts for a provider. For a Chambers
provider, this returns accounts belonging to all its member advocates, since advocates hold
their own accounts. `GET /provider-firms/{id}/offices/{officeId}/bank-details` retrieves bank
accounts for a specific office association.

Bank account mutation via PATCH (assigning an existing account to an office) is defined in the
spec but still in development.

## LiaisonManager

`LiaisonManagerEntity` holds name and contact details for an office liaison manager. It's an
aggregate root because a single liaison manager can be shared across offices: when assigning a
liaison manager to an LSP office, the request can specify `useHeadOfficeLiaisonManager=true` to
re-link the active liaison manager from the head office. An advocate's office can similarly
inherit the chambers' liaison manager. A `LiaisonManager` therefore can't be a private member
of one `ProviderOffice`.

There's no standalone creation endpoint. `LiaisonManager` records are created only as a side
effect of firm creation (`POST /provider-firms`) or via
`POST /provider-firms/{id}/offices/{officeId}/liaison-managers`. The latter either creates a new
`LiaisonManagerEntity` or resolves an existing one from another office, then end-dates any
current `OfficeLiaisonManagerLink` for the target office and creates a new active link. At most
one liaison manager is active per office at any time.

`GET /provider-firms/{id}/offices/{officeId}/liaison-managers` returns the full history of
liaison manager assignments for an office - both current and end-dated - in descending date
order.

## ContractManager

`ContractManagerEntity` records the name and a business-assigned `contractManagerId` for an LAA
contract manager. Unlike `LiaisonManager`, contract managers have an independent lifecycle -
they're LAA staff managed outside the provider domain, referenced by `contractManagerId` when
assigned to an office.

`GET /provider-contract-managers` returns a global list of contract managers for lookup.

`POST /provider-firms/{id}/offices/{officeId}/contract-managers` assigns a contract manager to
an LSP office, replacing any existing assignment. Only LSP offices support contract manager
assignment.

`GET /provider-firms/{id}/offices/{officeId}/contract-managers` retrieves the current
contract manager assignment for an office.

## OfficeEntity as reference data

`OfficeEntity` (table `OFFICE`) stores the physical address and contact details of a location.
It's not an aggregate root. The business domain operates on `ProviderOffice`
(`PROVIDER_OFFICE_LINK`), which owns the firm-specific attributes and is the primary identifier
for an "office" in the API.

`OfficeEntity` is shared reference data - a physical location record that can be
shared across multiple provider-office associations (for example, an advocate uses the same
`OFFICE` row as its parent chambers). All other link entities (`OFFICE_BANK_ACCOUNT_LINK`,
`OFFICE_CONTRACT_MANAGER_LINK`, `OFFICE_LIAISON_MANAGER_LINK`) reference `PROVIDER_OFFICE_LINK`
directly, not `OFFICE`.

On a PATCH, contact fields go to `OfficeEntity` and website to `ProviderOfficeLinkEntity`, but
the operation always goes through the `ProviderOffice` aggregate.
