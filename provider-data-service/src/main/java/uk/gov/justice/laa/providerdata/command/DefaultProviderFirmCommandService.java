package uk.gov.justice.laa.providerdata.command;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.justice.laa.providerdata.service.ProviderCreationResult;
import uk.gov.justice.laa.providerdata.service.ProviderService;

/**
 * Default implementation of {@link ProviderFirmCommandService}.
 *
 * <p>Dispatches update commands to the provider service. Provides a single point of entry for all
 * provider command processing, enabling future enhancements such as:
 *
 * <ul>
 *   <li>Event sourcing and event publication
 *   <li>Audit logging and command history
 *   <li>Async command processing
 *   <li>Command validation and authorisation frameworks
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class DefaultProviderFirmCommandService implements ProviderFirmCommandService {

  private final ProviderService providerService;

  @Override
  public ProviderCreationResult handle(UpdateProviderFirmCommand command) {
    command.validate();

    log.debug(
        "Handling UpdateProviderFirmCommand for provider: {}", command.providerFirmId());

    return providerService.patchProvider(command.providerFirmId(), command.patch());
  }
}

