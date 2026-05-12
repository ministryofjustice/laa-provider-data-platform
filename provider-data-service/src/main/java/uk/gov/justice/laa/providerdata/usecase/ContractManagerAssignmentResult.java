package uk.gov.justice.laa.providerdata.usecase;

import java.util.UUID;

/**
 * Result of assigning a contract manager to a provider firm office.
 *
 * @param officeGuid GUID of the provider office link that received the assignment
 * @param contractManagerId business identifier of the assigned contract manager
 */
public record ContractManagerAssignmentResult(UUID officeGuid, String contractManagerId) {}
