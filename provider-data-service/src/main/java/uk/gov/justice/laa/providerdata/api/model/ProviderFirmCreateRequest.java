package uk.gov.justice.laa.providerdata.api.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.AssertTrue;

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
    ProviderCreatePractitioner practitioner) {

  /**
   * Validates that exactly one provider creation option has been supplied.
   *
   * <p>This method is used by the {@link AssertTrue} constraint to enforce the rule that only one
   * of the following fields may be populated in a request:
   *
   * <ul>
   *   <li>{@code legalServicesProvider}
   *   <li>{@code chambers}
   *   <li>{@code practitioner}
   * </ul>
   *
   * <p>A valid request must provide exactly one of these fields. Supplying none or more than one
   * will cause validation to fail.
   *
   * @return {@code true} if exactly one field is non-null; {@code false} otherwise
   */
  @AssertTrue(
      message = "Exactly one of legalServicesProvider, chambers, practitioner must be provided")
  public boolean isExactlyOneProvided() {
    int count = 0;

    if (legalServicesProvider != null) {
      count++;
    }

    if (chambers != null) {
      count++;
    }

    if (practitioner != null) {
      count++;
    }

    if (count == 0) {
      return false;
    }

    return count == 1;
  }
}
