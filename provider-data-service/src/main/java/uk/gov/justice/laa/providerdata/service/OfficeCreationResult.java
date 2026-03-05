package uk.gov.justice.laa.providerdata.service;

import java.util.UUID;

/**
 * Result of an LSP office creation, containing the identifiers for the created resources. By
 * returning this record, rather than returning the OpenAPI-generated model class, we allow the
 * service layer to be independent of the generated model classes.
 */
public record OfficeCreationResult(
    UUID providerGUID, String firmNumber, UUID officeGUID, String accountNumber) {}
