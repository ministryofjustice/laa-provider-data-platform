package uk.gov.justice.laa.providerdata.command;

import java.util.List;

/**
 * Query service for the command audit log.
 *
 * <p>Provides read-only access to audit records written by consumer-side outbox processing.
 * Implementing classes must not modify any state.
 */
public interface CommandAuditLogQueryService {

  /**
   * Returns all audit entries for the given provider firm, ordered chronologically.
   *
   * @param providerFirmGUIDorFirmNumber provider GUID or firm number
   * @return ordered list of audit entries; empty if none exist
   */
  List<CommandAuditLogEntry> getAuditLog(String providerFirmGUIDorFirmNumber);
}

