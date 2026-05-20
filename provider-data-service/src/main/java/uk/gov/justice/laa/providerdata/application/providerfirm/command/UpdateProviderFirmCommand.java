package uk.gov.justice.laa.providerdata.application.providerfirm.command;

import uk.gov.justice.laa.providerdata.model.ProviderPatchV2;

/**
 * Application command to update an existing provider firm's supported fields.
 */
public record UpdateProviderFirmCommand(String providerFirmId, ProviderPatchV2 patch) {

  /** Validates that mandatory command fields are present. */
  public void validate() {
    if (providerFirmId == null || providerFirmId.isBlank()) {
      throw new IllegalArgumentException("providerFirmId must be provided");
    }
    if (patch == null) {
      throw new IllegalArgumentException("patch must be provided");
    }
  }
}

