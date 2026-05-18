package uk.gov.justice.laa.providerdata.command;

import java.util.UUID;
import uk.gov.justice.laa.providerdata.model.ProviderPatchV2;

/**
 * Command to update an existing provider firm's basic details.
 *
 * <p>Part of Phase 2 CQRS migration. Commands encapsulate intent and are dispatched to handlers
 * that execute business logic and persist changes.
 */
public record UpdateProviderFirmCommand(String providerFirmId, ProviderPatchV2 patch) {

  /**
   * Validates that the command is well-formed.
   *
   * @throws IllegalArgumentException if the command is invalid
   */
  public void validate() {
    if (providerFirmId == null || providerFirmId.isBlank()) {
      throw new IllegalArgumentException("providerFirmId must be provided");
    }
    if (patch == null) {
      throw new IllegalArgumentException("patch must be provided");
    }
  }
}

