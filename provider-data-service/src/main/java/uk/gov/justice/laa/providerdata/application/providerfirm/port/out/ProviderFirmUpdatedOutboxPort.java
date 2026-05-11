package uk.gov.justice.laa.providerdata.application.providerfirm.port.out;

import java.util.UUID;
import uk.gov.justice.laa.providerdata.model.ProviderPatchV2;

/** Outbound port for writing provider update events to the transactional outbox. */
public interface ProviderFirmUpdatedOutboxPort {

  void enqueue(UUID providerFirmGuid, String firmNumber, ProviderPatchV2 patch);
}

