package uk.gov.justice.laa.providerdata.application.providerfirm.command;

import uk.gov.justice.laa.providerdata.service.ProviderCreationResult;

/**
 * Use case boundary for updating provider firms.
 */
public interface UpdateProviderFirmUseCase {

  ProviderCreationResult execute(UpdateProviderFirmCommand command);
}

