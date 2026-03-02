package uk.gov.justice.laa.providerdata.api.model;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Request model representing the creation of a provider firm.
 *
 * <p>This request supports three mutually exclusive provider creation paths:
 *
 * <ul>
 *   <li>{@code legalServicesProvider} – for creating Legal Services Providers (LSPs)
 *   <li>{@code chambers} – for creating Chambers
 *   <li>{@code practitioner} – for creating individual Practitioners
 * </ul>
 *
 * <p>Only one of these fields should be populated in a valid request.
 *
 * <p>The {@link JsonInclude} annotation ensures null fields are omitted during serialisation,
 * keeping the API payload clean and concise.
 *
 * @param legalServicesProvider the LSP creation request, or {@code null} if not applicable
 * @param chambers the Chambers creation request, or {@code null} if not applicable
 * @param practitioner the Practitioner creation request, or {@code null} if not applicable
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ProviderFirmCreateRequest(
    ProviderCreateLsp legalServicesProvider,
    ProviderCreateChambers chambers,
    ProviderCreatePractitioner practitioner) {}
