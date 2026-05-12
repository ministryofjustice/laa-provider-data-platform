package uk.gov.justice.laa.providerdata.event;

import java.util.UUID;

/**
 * Application event published after a provider firm is successfully created or patched.
 *
 * <p>The event carries the provider firm GUID so that downstream listeners (projectors) can reload
 * the entity from the primary store and update the read store accordingly.
 *
 * <p>Published via {@link org.springframework.context.ApplicationEventPublisher}; consumed by
 * {@link uk.gov.justice.laa.providerdata.projection.ProviderFirmProjector}.
 *
 * @param providerFirmGUID the GUID of the created or updated provider firm
 */
public record ProviderFirmUpdatedEvent(UUID providerFirmGUID) {}
