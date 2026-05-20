package uk.gov.justice.laa.providerdata.application.providerfirm.port.out;

import uk.gov.justice.laa.providerdata.model.ProviderPatchV2;
import uk.gov.justice.laa.providerdata.service.ProviderCreationResult;

/**
 * Outbound persistence port for patching provider firms.
 */
public interface ProviderFirmPatchPort {

  ProviderCreationResult patchProvider(String providerFirmId, ProviderPatchV2 patch);
}

