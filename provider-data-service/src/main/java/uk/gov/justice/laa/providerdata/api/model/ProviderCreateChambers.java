package uk.gov.justice.laa.providerdata.api.model;

/**
 * Request model for creating a Chambers provider.
 *
 * <p>This record wraps a {@link ProviderCreateBase} instance, which holds the shared provider
 * creation attributes such as firm number, name, and any other common metadata defined in the base
 * model.
 *
 * <p>Used when registering or onboarding new Chambers into the provider data system.
 *
 * @param base the shared provider creation details used to construct a Chambers entity
 */
public record ProviderCreateChambers(ProviderCreateBase base) {}
