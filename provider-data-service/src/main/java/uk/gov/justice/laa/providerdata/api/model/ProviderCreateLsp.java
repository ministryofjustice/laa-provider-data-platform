package uk.gov.justice.laa.providerdata.api.model;

/**
 * Request model for creating a Legal Services Provider (LSP).
 *
 * <p>This record wraps a {@link ProviderCreateBase} instance, which contains the core attributes
 * required for creating any provider type—such as firm number, provider name, and other common
 * metadata defined in the base model.
 *
 * <p>Used when onboarding or registering new Legal Services Providers into the system.
 *
 * @param base the shared provider creation details used to construct an LSP entity
 */
public record ProviderCreateLsp(ProviderCreateBase base) {}
