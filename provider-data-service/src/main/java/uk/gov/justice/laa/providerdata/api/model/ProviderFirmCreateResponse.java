package uk.gov.justice.laa.providerdata.api.model;

/**
 * Response wrapper for POST /provider-firms.
 *
 * <p>Matches the shape: { "data": { "providerFirmGuid": "...", "providerFirmNumber": "..." } }
 */
public record ProviderFirmCreateResponse(ProviderFirmCreateResponseData data) {

  public static ProviderFirmCreateResponse of(String providerFirmGuid, String providerFirmNumber) {
    return new ProviderFirmCreateResponse(
        new ProviderFirmCreateResponseData(providerFirmGuid, providerFirmNumber));
  }

  /**
   * Inner response model containing the identifiers of a newly created provider firm.
   *
   * @param providerFirmGuid the unique GUID representing the provider firm
   * @param providerFirmNumber the assigned provider firm number
   */
  public record ProviderFirmCreateResponseData(
      String providerFirmGuid, String providerFirmNumber) {}
}
