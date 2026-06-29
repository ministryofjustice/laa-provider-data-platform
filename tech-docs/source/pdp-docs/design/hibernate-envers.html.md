# Hibernate Envers in LAA Provider Data Platform

## Overview

Hibernate Envers is used in `provider-data-service` to maintain revision history for selected JPA
entities. It records snapshots of audited entities whenever they are inserted or updated, allowing
the service to inspect historical state without implementing full event sourcing.

In this project, Envers is used as an **internal audit and history mechanism**. It complements the
OUTBOX pattern rather than replacing it:

- **Envers** provides queryable historical state for audited entities inside the service
- **OUTBOX** provides durable integration events for downstream consumers

## Where Envers is configured

Envers is enabled by the dependency in `provider-data-service/build.gradle`:

```groovy
implementation 'org.hibernate.orm:hibernate-envers'
```

This makes Envers available to the Spring Boot JPA application through Hibernate.

## How Envers works

Envers tracks changes to entities annotated with `@Audited`. When an audited entity changes,
Hibernate writes audit data automatically as part of persistence.

At a high level, each audited transaction creates:

1. A row in a revision metadata table, `REVINFO`
2. A row in an entity audit table, typically named with a `*_AUD` suffix

This allows the application to query:

- which revisions exist for an entity
- what an entity looked like at a specific revision
- revision metadata such as timestamp and revision user

## How it is used in this project

## Audited entities

The project audits provider-related entities using `@Audited`.

Examples include:

- `provider-data-service/src/main/java/uk/gov/justice/laa/providerdata/entity/ProviderEntity.java`
- `provider-data-service/src/main/java/uk/gov/justice/laa/providerdata/entity/PractitionerEntity.java`
- `provider-data-service/src/main/java/uk/gov/justice/laa/providerdata/entity/LspProviderEntity.java`
- `provider-data-service/src/main/java/uk/gov/justice/laa/providerdata/entity/ChamberProviderEntity.java`
- `provider-data-service/src/main/java/uk/gov/justice/laa/providerdata/entity/AdvocatePractitionerEntity.java`
- `provider-data-service/src/main/java/uk/gov/justice/laa/providerdata/entity/BarristerPractitionerEntity.java`

For example, `ProviderEntity` is annotated like this:

```java
@Audited
@Entity
@Table(name = "PROVIDER")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
public class ProviderEntity extends AuditableEntity {
  // fields omitted
}
```

Because the provider model uses single-table inheritance, Envers tracks revisions across the entity
hierarchy while preserving the subtype structure used by Hibernate.

## Custom revision metadata

This project defines a custom revision metadata entity in
`provider-data-service/src/main/java/uk/gov/justice/laa/providerdata/entity/EnversRevisionEntity.java`.

That entity maps the `REVINFO` table and adds project-specific metadata:

```java
@Entity
@Table(name = "REVINFO")
@RevisionEntity(EnversRevisionListener.class)
public class EnversRevisionEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @RevisionNumber
  @Column(name = "REV")
  private Integer revision;

  @RevisionTimestamp
  @Column(name = "REVTSTMP", nullable = false)
  private Long revisionTimestamp;

  @Column(name = "REVISION_USER", nullable = false)
  private String revisionUser;
}
```

This means each revision stores:

- the revision number
- the revision timestamp
- the revision user

## Revision listener

The revision metadata is populated by
`provider-data-service/src/main/java/uk/gov/justice/laa/providerdata/config/EnversRevisionListener.java`.

```java
public class EnversRevisionListener implements RevisionListener {

  @Override
  public void newRevision(Object revisionEntity) {
    if (revisionEntity instanceof EnversRevisionEntity rev) {
      rev.setRevisionUser("SYSTEM");
    }
  }
}
```

At present, every revision is tagged with `revisionUser = "SYSTEM"`.

That is useful because it:

- proves the service can attach custom metadata to each revision
- creates a clear extension point for future user attribution
- avoids needing to duplicate audit metadata across domain entities

In future, this could be changed to capture the authenticated user or service identity.

## What Envers stores

## Revision metadata in `REVINFO`

Each audited transaction creates one row in `REVINFO`.

This table stores metadata about the revision itself, not the entity state. In this project it
contains:

- `REV`
- `REVTSTMP`
- `REVISION_USER`

A single revision can apply to one or more audited entity changes made in the same transaction.

## Historical snapshots in audit tables

For each audited entity, Envers stores historical rows in a matching audit table, typically using a
`*_AUD` naming convention.

These rows usually contain:

- the entity identifier
- the revision number
- the revision type
- the audited field values at that point in time

This means Envers stores **snapshots of state**, not business events.

## What it gives this project

## Queryable history

Envers allows the service to retrieve previous versions of an entity directly through Hibernate's
audit API. The application does not need to replay events to reconstruct state.

This is useful for provider data where historical inspection may be needed for audit, support, or
debugging.

## Low application complexity

After adding `@Audited`, audit rows are written automatically by Hibernate. Services and repositories
do not need to manually persist audit records for each change.

That keeps normal persistence logic cleaner.

## Structured revision metadata

Because the project uses `EnversRevisionEntity`, each revision can carry metadata such as
`revisionUser`. This is a useful foundation for better audit attribution later.

## Works alongside OUTBOX

The project documentation in `tech-docs/source/pdp-docs/design/events.html.md` treats Envers and
OUTBOX as separate concerns.

That distinction is important:

| Concern | Envers | OUTBOX |
|---|---|---|
| Internal audit/history | Yes | Limited |
| Query historical state | Yes | Not directly |
| Publish to other services | No | Yes |
| Delivery guarantees | No | Yes |
| Revision user metadata | Yes | No |

In this codebase:

- use **Envers** for internal history and audit queries
- use **OUTBOX** for reliable integration events

## Example flow

A simple provider update flow looks like this:

1. A provider entity is saved for the first time
2. Envers writes a revision row to `REVINFO`
3. Envers writes the first audited snapshot row
4. The provider is updated
5. Envers writes another revision row to `REVINFO`
6. Envers writes another audited snapshot row
7. The application can retrieve both versions later using the Envers API

## Verified behaviour in this repository

The integration test
`provider-data-service/src/integrationTest/java/uk/gov/justice/laa/providerdata/repository/ProviderFirmEnversRepositoryTest.java`
shows this behaviour directly.

## What the test verifies

The test:

1. Saves an `LspProviderEntity`
2. Updates its `name`
3. Reads revisions using `AuditReaderFactory`
4. Loads the entity snapshots for both revisions
5. Checks the historical values
6. Checks the custom revision metadata

## Relevant example

```java
List<Number> revisions =
    auditReader.getRevisions(LspProviderEntity.class, savedProvider.getGuid());

assertThat(revisions).hasSize(2);

Number firstRevision = revisions.get(0);
Number secondRevision = revisions.get(1);

ProviderEntity firstSnapshot =
    auditReader.find(LspProviderEntity.class, savedProvider.getGuid(), firstRevision);
ProviderEntity secondSnapshot =
    auditReader.find(LspProviderEntity.class, savedProvider.getGuid(), secondRevision);

assertThat(firstSnapshot.getName()).isEqualTo("Before update");
assertThat(secondSnapshot.getName()).isEqualTo("After update");

EnversRevisionEntity revisionMetadata =
    auditReader.findRevision(EnversRevisionEntity.class, secondRevision);
assertThat(revisionMetadata.getRevisionUser()).isEqualTo("SYSTEM");
```

This confirms that:

- revisions are being created
- historical snapshots can be queried
- custom revision metadata is being populated

## Why this is helpful in this project

## 1. Supports audit and traceability

Envers provides a built-in way to inspect prior state for audited entities. That is useful where the
service needs to understand what changed and when.

## 2. Reduces custom audit implementation

Without Envers, the service would need separate audit tables and explicit application code to write
to them. Envers removes most of that manual work.

## 3. Fits naturally with JPA

The project already uses Spring Data JPA and Hibernate. Envers extends that model rather than
introducing a separate persistence pattern.

## 4. Useful for debugging and support

If a provider record is changed unexpectedly, historical revisions can help identify the previous
state quickly.

## 5. Complements asynchronous event design

Envers gives internal history, while OUTBOX gives external notification. That separation keeps each
mechanism focused on a single concern.

## Limitations

Envers is helpful, but it does not solve every problem.

## It is not an event bus

Envers does not notify downstream services. It only stores historical database state.

## It is not event sourcing

Envers stores snapshots, not explicit business events. It can show what changed in data terms, but
not necessarily the business intent unless that is modelled elsewhere.

## Revision user is currently static

The current implementation sets `revisionUser` to `"SYSTEM"` for every revision. That demonstrates
the mechanism, but does not yet capture the real caller identity.

## Audit tables will grow

Every change to an audited entity creates more audit rows. Over time, audit data volume will need
operational consideration.

## Future improvements

Potential improvements in this codebase could include:

### Capture the real user or service principal

Update `EnversRevisionListener` to derive the revision user from security or request context.

### Add audit query use cases where needed

If the service needs administrative or support-facing history views, those can be built on top of
the Envers audit API.

### Be selective about audited entities

Audit should remain focused on entities with real operational or compliance value.

## Summary

Hibernate Envers is used in `provider-data-service` as an internal audit/history mechanism for JPA
entities.

In this project it:

- audits selected provider-related entities using `@Audited`
- stores revision metadata in `REVINFO`
- uses a custom `EnversRevisionEntity` to record `revisionUser`
- populates revision metadata through `EnversRevisionListener`
- allows historical entity state to be queried through the Envers API

The simplest way to view it in this codebase is:

- **Envers** answers: _what did this entity look like before?_
- **OUTBOX** answers: _how do we reliably tell other systems that something changed?_

## References

- `provider-data-service/build.gradle`
- `provider-data-service/src/main/java/uk/gov/justice/laa/providerdata/entity/ProviderEntity.java`
- `provider-data-service/src/main/java/uk/gov/justice/laa/providerdata/entity/PractitionerEntity.java`
- `provider-data-service/src/main/java/uk/gov/justice/laa/providerdata/entity/EnversRevisionEntity.java`
- `provider-data-service/src/main/java/uk/gov/justice/laa/providerdata/config/EnversRevisionListener.java`
- `provider-data-service/src/integrationTest/java/uk/gov/justice/laa/providerdata/repository/ProviderFirmEnversRepositoryTest.java`
- `tech-docs/source/pdp-docs/design/events.html.md`
