package uk.gov.justice.laa.providerdata.application.providerfirm.port.out;

import java.util.UUID;
import uk.gov.justice.laa.providerdata.model.ProviderPatchV2;

/**
 * Outbound port for publishing provider-firm-updated domain events.
 */
public interface ProviderFirmUpdatedEventPort {

  void publish(UUID providerFirmGuid, String firmNumber, ProviderPatchV2 patch);
}

