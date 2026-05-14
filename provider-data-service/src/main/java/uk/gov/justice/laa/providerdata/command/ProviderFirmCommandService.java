package uk.gov.justice.laa.providerdata.command;

import uk.gov.justice.laa.providerdata.service.ProviderCreationResult;

/**
 * Service for dispatching commands to their respective handlers.
 *
 * <p>Part of Phase 2 CQRS migration. Decouples command submission from command handling, allowing
 * for future event sourcing, audit logging, or async processing without changing the public API.
 */
public interface ProviderFirmCommandService {

  /**
   * Dispatches an UpdateProviderFirmCommand to the appropriate handler.
   *
   * @param command the update command
   * @return the result of the update operation
   */
  ProviderCreationResult handle(UpdateProviderFirmCommand command);
}
