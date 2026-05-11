package uk.gov.justice.laa.providerdata.command;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.justice.laa.providerdata.command.event.ProviderFirmUpdatedEvent;
import uk.gov.justice.laa.providerdata.service.ProviderCreationResult;
import uk.gov.justice.laa.providerdata.service.ProviderService;

/**
 * Default implementation of {@link ProviderFirmCommandService}.
 *
 * <p>Dispatches update commands to the provider service and publishes a
 * {@link ProviderFirmUpdatedEvent} within the same transaction so that
 * {@link uk.gov.justice.laa.providerdata.command.event.CommandAuditEventListener}
 * can write a durable audit record after commit.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class DefaultProviderFirmCommandService implements ProviderFirmCommandService {

  private final ProviderService providerService;
  private final ApplicationEventPublisher eventPublisher;

  @Override
  public ProviderCreationResult handle(UpdateProviderFirmCommand command) {
    command.validate();

    log.debug(
        "Handling UpdateProviderFirmCommand for provider: {}", command.providerFirmId());

    ProviderCreationResult result =
        providerService.patchProvider(command.providerFirmId(), command.patch());

    eventPublisher.publishEvent(
        ProviderFirmUpdatedEvent.of(
            result.providerFirmGUID(), result.firmNumber(), command.patch()));

    return result;
  }
}

