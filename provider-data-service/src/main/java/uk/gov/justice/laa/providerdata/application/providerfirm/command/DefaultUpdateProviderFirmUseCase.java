package uk.gov.justice.laa.providerdata.application.providerfirm.command;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.justice.laa.providerdata.application.providerfirm.port.out.ProviderFirmPatchPort;
import uk.gov.justice.laa.providerdata.application.providerfirm.port.out.ProviderFirmUpdatedOutboxPort;
import uk.gov.justice.laa.providerdata.application.providerfirm.port.out.ProviderFirmUpdatedEventPort;
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
  private final ProviderFirmUpdatedEventPort providerFirmUpdatedEventPort;

  @Override
  public ProviderCreationResult execute(UpdateProviderFirmCommand command) {
    command.validate();

    log.debug("Handling UpdateProviderFirmCommand for provider: {}", command.providerFirmId());

    ProviderCreationResult result =
        providerFirmPatchPort.patchProvider(command.providerFirmId(), command.patch());

    providerFirmUpdatedOutboxPort.enqueue(
        result.providerFirmGUID(), result.firmNumber(), command.patch());

    providerFirmUpdatedEventPort.publish(result.providerFirmGUID(), result.firmNumber(), command.patch());

    return result;
  }
}

