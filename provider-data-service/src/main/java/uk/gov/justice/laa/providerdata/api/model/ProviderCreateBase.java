package uk.gov.justice.laa.providerdata.api.model;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Base request structure containing the common fields required when creating any type of provider
 * (e.g. Legal Services Provider, Chambers, Practitioner).
 *
 * <p>All provider creation models wrap this base record to ensure consistency in core attributes
 * across different provider types.
 *
 * <p>The {@link JsonInclude} annotation ensures that only non-null values are included during JSON
 * serialization, keeping request payloads compact.
 *
 * @param firmNumber an optional firm number; if not supplied, the service layer may generate one
 *     depending on the business rules
 * @param name the provider's display or registered name
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ProviderCreateBase(
    String firmNumber, // optional; if not provided, your service can generate it
    String name) {}
