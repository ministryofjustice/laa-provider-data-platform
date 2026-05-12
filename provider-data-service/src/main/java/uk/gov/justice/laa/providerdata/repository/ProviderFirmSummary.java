package uk.gov.justice.laa.providerdata.repository;

import java.util.UUID;

/**
 * JPA projection interface for lightweight provider firm reads.
 *
 * <p>Used by {@link uk.gov.justice.laa.providerdata.service.ProviderFirmQueryService} to avoid
 * loading the full entity graph when only summary fields are needed.
 */
public interface ProviderFirmSummary {

  UUID getGuid();

  String getFirmNumber();

  String getName();

  String getFirmType();
}
