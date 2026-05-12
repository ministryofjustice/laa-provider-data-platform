package uk.gov.justice.laa.providerdata.projection;

import java.io.Serial;
import java.io.Serializable;
import java.util.UUID;

/**
 * Redis-cached read model for a provider firm.
 *
 * <p>Stored by {@link ProviderFirmProjector} after every create/patch event and read by {@link
 * uk.gov.justice.laa.providerdata.service.ProviderFirmQueryService}. Contains only the fields
 * required to fulfil the provider firm GET responses; the full entity graph is never loaded on the
 * read path once the projection is warm.
 */
public class ProviderFirmReadModel implements Serializable {

  @Serial private static final long serialVersionUID = 1L;

  private UUID guid;
  private String firmNumber;
  private String name;
  private String firmType;

  /** Required by Java serialisation. */
  public ProviderFirmReadModel() {}

  /** Full constructor. */
  public ProviderFirmReadModel(UUID guid, String firmNumber, String name, String firmType) {
    this.guid = guid;
    this.firmNumber = firmNumber;
    this.name = name;
    this.firmType = firmType;
  }

  public UUID getGuid() {
    return guid;
  }

  public void setGuid(UUID guid) {
    this.guid = guid;
  }

  public String getFirmNumber() {
    return firmNumber;
  }

  public void setFirmNumber(String firmNumber) {
    this.firmNumber = firmNumber;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getFirmType() {
    return firmType;
  }

  public void setFirmType(String firmType) {
    this.firmType = firmType;
  }
}
