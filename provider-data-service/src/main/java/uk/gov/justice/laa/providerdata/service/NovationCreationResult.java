package uk.gov.justice.laa.providerdata.service;

import java.util.List;
import java.util.UUID;

/**
 * Result of a Novation creation, containing the identifiers for the created resources. By returning
 * this record rather than a generated model class, the service layer remains independent of the
 * OpenAPI-generated model classes.
 */
public record NovationCreationResult(UUID novationGUID, List<UUID> novationRelationshipGUIDs) {}
