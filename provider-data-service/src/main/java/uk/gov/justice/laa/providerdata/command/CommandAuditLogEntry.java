package uk.gov.justice.laa.providerdata.command;

import java.time.OffsetDateTime;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * Read-model entry returned by the audit log query endpoint.
 *
 * <p>Intentionally a lightweight projection of {@link
 * uk.gov.justice.laa.providerdata.entity.CommandAuditLogEntity} — it does not expose JPA audit
 * columns (createdBy, version, etc.) which are internal implementation details.
 */
public record CommandAuditLogEntry(
    UUID guid,
    UUID providerFirmGuid,
    String firmNumber,
    String commandType,
    OffsetDateTime occurredAt,
    @Nullable String changedFields) {}
