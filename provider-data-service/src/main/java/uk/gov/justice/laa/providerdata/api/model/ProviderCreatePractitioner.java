package uk.gov.justice.laa.providerdata.api.model;

/**
 * Request model for creating a Practitioner provider.
 *
 * <p>This record wraps a {@link ProviderCreateBase} instance, which contains the common attributes
 * required for creating any provider type (e.g. name, firm number, contact details depending on
 * your base model design).
 *
 * <p>Practitioners typically represent individual advocates or barristers within the provider data
 * domain.
 *
 * @param base the shared provider creation details used for building a practitioner entity
 */
public record ProviderCreatePractitioner(ProviderCreateBase base) {}
