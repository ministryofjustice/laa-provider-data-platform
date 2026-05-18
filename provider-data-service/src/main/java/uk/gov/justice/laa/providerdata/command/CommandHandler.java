package uk.gov.justice.laa.providerdata.command;

import uk.gov.justice.laa.providerdata.service.ProviderCreationResult;

/**
 * Handles execution of a command and returns the result.
 *
 * <p>Part of Phase 2 CQRS migration. Implementations dispatch to appropriate services based on
 * command type and provider subtype.
 *
 * @param <C> the command type
 * @param <R> the result type
 */
public interface CommandHandler<C, R> {

  /**
   * Executes the command.
   *
   * @param command the command to execute
   * @return the result of command execution
   */
  R handle(C command);
}

