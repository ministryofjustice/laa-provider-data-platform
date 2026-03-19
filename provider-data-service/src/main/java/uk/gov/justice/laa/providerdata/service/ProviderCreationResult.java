package uk.gov.justice.laa.providerdata.service;

import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * Result of a provider firm creation, containing the identifiers for the created resources. By
 * returning this record rather than a generated model class, the service layer remains independent
 * of the OpenAPI-generated model classes.
 *
 * <p>{@code headOfficeGUID} and {@code headOfficeAccountNumber} are non-null for LSP and Chambers
 * firms (which always create a head office); they are {@code null} for Practitioners.
 */
public record ProviderCreationResult(
    UUID providerFirmGUID,
    String firmNumber,
    @Nullable UUID headOfficeGUID,
    @Nullable String headOfficeAccountNumber) {

  /** Convenience factory for Practitioners, which have no head office. */
  public static ProviderCreationResult withoutOffice(UUID providerFirmGUID, String firmNumber) {
    return new ProviderCreationResult(providerFirmGUID, firmNumber, null, null);
  }
}
