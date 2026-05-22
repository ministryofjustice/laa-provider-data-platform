package uk.gov.justice.laa.providerdata.validation;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import uk.gov.justice.laa.providerdata.entity.LspProviderEntity;
import uk.gov.justice.laa.providerdata.entity.ProviderOfficeLinkEntity;
import uk.gov.justice.laa.providerdata.model.LSPDetailsPatchV2;
import uk.gov.justice.laa.providerdata.model.LSPHeadOfficeDetailsPatchV2;

/** Validator for LSP Legal Organisation PATCH operations implementing DSTEW-1574 business rules. */
public class LspPatchValidator {

  /**
   * Validates an LSP patch request against all DSTEW-1574 business rules.
   *
   * @param provider the existing LSP provider entity
   * @param patch the patch request
   * @param headOfficeLink the existing head office link (may be null initially)
   * @throws IllegalArgumentException if validation fails
   */
  public static void validateLspPatch(
      LspProviderEntity provider,
      LSPDetailsPatchV2 patch,
      ProviderOfficeLinkEntity headOfficeLink) {
    List<String> errors = new ArrayList<>();

    // Validate firm intervened flag and date are paired (if either is provided)
    validateFirmIntervenedPair(patch, errors);

    // Validate hold all payments flag and reason are paired (if either is provided)
    validateHoldAllPaymentsPair(patch, errors);

    // Validate inactive date if provided
    if (patch.getHeadOffice() != null) {
      validateInactiveDateRule(patch.getHeadOffice(), headOfficeLink, errors);

      // Validate false balance eligibility (BR-15)
      validateFalseBalanceEligibility(patch.getHeadOffice(), headOfficeLink, provider, errors);

      // Validate DX conditional rule (BR-11)
      validateDxConditionalRule(patch.getHeadOffice(), errors);

      // Validate payment method vs bank account rule (BR-08)
      validatePaymentMethodRule(patch.getHeadOffice(), errors);
    }

    if (!errors.isEmpty()) {
      throw new IllegalArgumentException(
          "LSP patch validation failed: " + String.join("; ", errors));
    }
  }

  private static void validateFirmIntervenedPair(LSPDetailsPatchV2 patch, List<String> errors) {
    boolean hasFirmIntervenedFlag = patch.getFirmIntervenedFlag() != null;
    boolean hasFirmIntervenedDate = patch.getFirmIntervenedDate() != null;

    if (hasFirmIntervenedFlag != hasFirmIntervenedDate) {
      errors.add(
          "firmIntervenedFlag and firmIntervenedDate must be provided together or not at all");
    }
  }

  private static void validateHoldAllPaymentsPair(LSPDetailsPatchV2 patch, List<String> errors) {
    boolean hasHoldFlag = patch.getHoldAllPaymentsFlag() != null;
    boolean hasHoldReason = patch.getHoldAllPaymentsReason() != null;

    if (hasHoldFlag != hasHoldReason) {
      errors.add(
          "holdAllPaymentsFlag and holdAllPaymentsReason must be provided together or not at all");
    }

    // If hold flag is set to true, reason must be provided
    if (Boolean.TRUE.equals(patch.getHoldAllPaymentsFlag())
        && patch.getHoldAllPaymentsReason() == null) {
      errors.add("holdAllPaymentsReason must be provided when holdAllPaymentsFlag is true");
    }
  }

  private static void validateInactiveDateRule(
      LSPHeadOfficeDetailsPatchV2 headOfficePatch,
      ProviderOfficeLinkEntity existingHeadOffice,
      List<String> errors) {
    LocalDate inactiveDateToSet = headOfficePatch.getActiveDateTo();
    Boolean clearActiveDateTo = headOfficePatch.getClearActiveDateTo();

    // If trying to clear the inactive date
    if (Boolean.TRUE.equals(clearActiveDateTo)) {
      if (existingHeadOffice != null && existingHeadOffice.getActiveDateTo() != null) {
        errors.add("Inactive Date cannot be removed once set");
      }
      return;
    }

    // If trying to set an inactive date
    if (inactiveDateToSet != null) {
      LocalDate today = LocalDate.now();

      // Inactive date must be today
      if (!inactiveDateToSet.equals(today)) {
        errors.add("Inactive Date must be set to today's date only");
      }

      // Cannot change existing inactive date
      if (existingHeadOffice != null && existingHeadOffice.getActiveDateTo() != null) {
        if (!existingHeadOffice.getActiveDateTo().equals(inactiveDateToSet)) {
          errors.add("Inactive Date cannot be amended once set");
        }
      }
    }
  }

  private static void validateFalseBalanceEligibility(
      LSPHeadOfficeDetailsPatchV2 headOfficePatch,
      ProviderOfficeLinkEntity existingHeadOffice,
      LspProviderEntity provider,
      List<String> errors) {
    // Check if false balance flag is being set
    if (Boolean.TRUE.equals(headOfficePatch.getFalseBalanceFlag())) {
      // False balance can only be applied if inactive date exists or is being set
      boolean hasInactiveDate =
          existingHeadOffice != null && existingHeadOffice.getActiveDateTo() != null;
      boolean isSettingInactiveDate = headOfficePatch.getActiveDateTo() != null;

      if (!hasInactiveDate && !isSettingInactiveDate) {
        errors.add(
            "False Balance flag can only be applied to inactive Legal Organisations (BR-15)");
      }
    }
  }

  private static void validateDxConditionalRule(
      LSPHeadOfficeDetailsPatchV2 headOfficePatch, List<String> errors) {
    boolean hasDxNumber =
        headOfficePatch.getDxDetails() != null
            && headOfficePatch.getDxDetails().getDxNumber() != null;
    boolean hasDxCentre =
        headOfficePatch.getDxDetails() != null
            && headOfficePatch.getDxDetails().getDxCentre() != null;

    // Valid: both provided or neither provided
    // Invalid: one without the other
    if (hasDxNumber != hasDxCentre) {
      errors.add("DX Number and DX Centre must be provided together or not at all (BR-11)");
    }
  }

  private static void validatePaymentMethodRule(
      LSPHeadOfficeDetailsPatchV2 headOfficePatch, List<String> errors) {
    if (headOfficePatch.getPayment() == null) {
      return;
    }

    var paymentMethod = headOfficePatch.getPayment().getPaymentMethod();

    // If payment method is being set to EFT (Electronic Funds Transfer), bank account must exist
    if (paymentMethod != null && paymentMethod.toString().equals("EFT")) {
      boolean hasBankAccount = headOfficePatch.getPayment().getBankAccountDetails() != null;

      if (!hasBankAccount) {
        errors.add("Bank Account must be provided when Payment Method is EFT (BR-08)");
      }
    }
  }
}
