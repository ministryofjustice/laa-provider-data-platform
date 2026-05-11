package uk.gov.justice.laa.providerdata.adapters.out.persistence;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.justice.laa.providerdata.application.providerfirm.port.out.ProviderFirmPatchPort;
import uk.gov.justice.laa.providerdata.model.ProviderPatchV2;
import uk.gov.justice.laa.providerdata.service.ProviderCreationResult;
import uk.gov.justice.laa.providerdata.service.ProviderService;

/**
 * Persistence adapter that delegates provider patch operations to the existing service.
 */
@Component
@RequiredArgsConstructor
public class ProviderFirmPatchPersistenceAdapter implements ProviderFirmPatchPort {

  private final ProviderService providerService;

  @Override
  public ProviderCreationResult patchProvider(String providerFirmId, ProviderPatchV2 patch) {
    return providerService.patchProvider(providerFirmId, patch);
  }
}

