package uk.gov.justice.laa.providerdata.application.providerfirm.command;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.justice.laa.providerdata.application.providerfirm.port.out.ProviderFirmPatchPort;
import uk.gov.justice.laa.providerdata.application.providerfirm.port.out.ProviderFirmUpdatedOutboxPort;
import uk.gov.justice.laa.providerdata.service.ProviderCreationResult;

/**
 * Default application use case implementation for provider-firm updates.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class DefaultUpdateProviderFirmUseCase implements UpdateProviderFirmUseCase {

  private final ProviderFirmPatchPort providerFirmPatchPort;
  private final ProviderFirmUpdatedOutboxPort providerFirmUpdatedOutboxPort;

  @Override
  public ProviderCreationResult execute(UpdateProviderFirmCommand command) {
    command.validate();

    log.info("Command received: UpdateProviderFirm providerId={}", command.providerFirmId());

    ProviderCreationResult result =
        providerFirmPatchPort.patchProvider(command.providerFirmId(), command.patch());

    log.info(
        "Provider update persisted: providerFirmGuid={} firmNumber={}",
        result.providerFirmGUID(),
        result.firmNumber());

    providerFirmUpdatedOutboxPort.enqueue(
        result.providerFirmGUID(), result.firmNumber(), command.patch());

    log.info(
        "Outbox event enqueued: providerFirmGuid={} firmNumber={} command=UpdateProviderFirm",
        result.providerFirmGUID(),
        result.firmNumber());

    return result;
  }
}
