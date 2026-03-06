package uk.gov.justice.laa.providerdata.service;

import java.util.UUID;

/** Result of an LSP office creation, containing the identifiers for the created resources. */
public record OfficeCreationResult(
    UUID providerGUID, String firmNumber, UUID officeGUID, String accountNumber) {}
