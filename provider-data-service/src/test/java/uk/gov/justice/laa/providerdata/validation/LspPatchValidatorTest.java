package uk.gov.justice.laa.providerdata.validation;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import uk.gov.justice.laa.providerdata.entity.LspProviderEntity;
import uk.gov.justice.laa.providerdata.entity.LspProviderOfficeLinkEntity;
import uk.gov.justice.laa.providerdata.model.DXPatchV2;
import uk.gov.justice.laa.providerdata.model.LSPDetailsPatchV2;
import uk.gov.justice.laa.providerdata.model.LSPHeadOfficeDetailsPatchV2;
import uk.gov.justice.laa.providerdata.model.PaymentDetailsPatchOrLinkV2;
import uk.gov.justice.laa.providerdata.model.PaymentDetailsPaymentMethodV2;

class LspPatchValidatorTest {

  private static final String VALID_GUID = "12345678-1234-1234-1234-123456789012";

  @Test
  void testValidateFirmIntervenedFairPaired() {
    // Both provided together - should pass
    LSPDetailsPatchV2 patch = new LSPDetailsPatchV2();
    patch.firmIntervenedFlag(true);
    patch.firmIntervenedDate(LocalDate.now());

    LspProviderEntity provider = LspProviderEntity.builder().build();
    assertDoesNotThrow(() -> LspPatchValidator.validateLspPatch(provider, patch, null));
  }

  @Test
  void testValidateFirmIntervenedFlagWithoutDate() {
    // Flag without date - should fail
    LSPDetailsPatchV2 patch = new LSPDetailsPatchV2();
    patch.firmIntervenedFlag(true);

    LspProviderEntity provider = LspProviderEntity.builder().build();
    assertThatThrownBy(() -> LspPatchValidator.validateLspPatch(provider, patch, null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining(
            "firmIntervenedFlag and firmIntervenedDate must be provided together");
  }

  @Test
  void testValidateFirmIntervenedDateWithoutFlag() {
    // Date without flag - should fail
    LSPDetailsPatchV2 patch = new LSPDetailsPatchV2();
    patch.firmIntervenedDate(LocalDate.now());

    LspProviderEntity provider = LspProviderEntity.builder().build();
    assertThatThrownBy(() -> LspPatchValidator.validateLspPatch(provider, patch, null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining(
            "firmIntervenedFlag and firmIntervenedDate must be provided together");
  }

  @Test
  void testValidateHoldAllPaymentsFlagPaired() {
    // Both provided together - should pass
    LSPDetailsPatchV2 patch = new LSPDetailsPatchV2();
    patch.holdAllPaymentsFlag(true);
    patch.holdAllPaymentsReason("Legal dispute");

    LspProviderEntity provider = LspProviderEntity.builder().build();
    assertDoesNotThrow(() -> LspPatchValidator.validateLspPatch(provider, patch, null));
  }

  @Test
  void testValidateHoldAllPaymentsFlagWithoutReason() {
    // Flag without reason - should fail
    LSPDetailsPatchV2 patch = new LSPDetailsPatchV2();
    patch.holdAllPaymentsFlag(true);

    LspProviderEntity provider = LspProviderEntity.builder().build();
    assertThatThrownBy(() -> LspPatchValidator.validateLspPatch(provider, patch, null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining(
            "holdAllPaymentsReason must be provided when holdAllPaymentsFlag is true");
  }

  @Test
  void testValidateHoldAllPaymentsReasonWithoutFlag() {
    // Reason without flag - should fail
    LSPDetailsPatchV2 patch = new LSPDetailsPatchV2();
    patch.holdAllPaymentsReason("Legal dispute");

    LspProviderEntity provider = LspProviderEntity.builder().build();
    assertThatThrownBy(() -> LspPatchValidator.validateLspPatch(provider, patch, null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining(
            "holdAllPaymentsFlag and holdAllPaymentsReason must be provided together");
  }

  @Test
  void testValidateInactiveDateMustBeToday() {
    // Trying to set inactive date to tomorrow - should fail
    LSPHeadOfficeDetailsPatchV2 headOfficePatch = new LSPHeadOfficeDetailsPatchV2();
    headOfficePatch.activeDateTo(LocalDate.now().plusDays(1));

    LSPDetailsPatchV2 patch = new LSPDetailsPatchV2();
    patch.headOffice(headOfficePatch);

    LspProviderEntity provider = LspProviderEntity.builder().build();
    assertThatThrownBy(() -> LspPatchValidator.validateLspPatch(provider, patch, null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Inactive Date must be set to today's date only");
  }

  @Test
  void testValidateInactiveDateCanBeSetToToday() {
    // Setting inactive date to today - should pass
    LSPHeadOfficeDetailsPatchV2 headOfficePatch = new LSPHeadOfficeDetailsPatchV2();
    headOfficePatch.activeDateTo(LocalDate.now());

    LSPDetailsPatchV2 patch = new LSPDetailsPatchV2();
    patch.headOffice(headOfficePatch);

    LspProviderEntity provider = LspProviderEntity.builder().build();
    assertDoesNotThrow(() -> LspPatchValidator.validateLspPatch(provider, patch, null));
  }

  @Test
  void testValidateInactiveDateCannotBeChanged() {
    // Trying to change existing inactive date - should fail
    LSPHeadOfficeDetailsPatchV2 headOfficePatch = new LSPHeadOfficeDetailsPatchV2();
    headOfficePatch.activeDateTo(LocalDate.now());

    LspProviderOfficeLinkEntity existingHeadOffice =
        LspProviderOfficeLinkEntity.builder().activeDateTo(LocalDate.now().minusDays(1)).build();

    LSPDetailsPatchV2 patch = new LSPDetailsPatchV2();
    patch.headOffice(headOfficePatch);

    LspProviderEntity provider = LspProviderEntity.builder().build();
    assertThatThrownBy(
            () -> LspPatchValidator.validateLspPatch(provider, patch, existingHeadOffice))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Inactive Date cannot be amended once set");
  }

  @Test
  void testValidateFalseBalanceRequiresInactiveDate() {
    // Setting false balance without inactive date - should fail
    LSPHeadOfficeDetailsPatchV2 headOfficePatch = new LSPHeadOfficeDetailsPatchV2();
    headOfficePatch.falseBalanceFlag(true);

    LSPDetailsPatchV2 patch = new LSPDetailsPatchV2();
    patch.headOffice(headOfficePatch);

    LspProviderEntity provider = LspProviderEntity.builder().build();
    assertThatThrownBy(() -> LspPatchValidator.validateLspPatch(provider, patch, null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("False Balance flag can only be applied to inactive");
  }

  @Test
  void testValidateFalseBalanceWithInactiveDate() {
    // Setting false balance with inactive date set - should pass
    LSPHeadOfficeDetailsPatchV2 headOfficePatch = new LSPHeadOfficeDetailsPatchV2();
    headOfficePatch.falseBalanceFlag(true);

    LspProviderOfficeLinkEntity existingHeadOffice =
        LspProviderOfficeLinkEntity.builder().activeDateTo(LocalDate.now().minusDays(1)).build();

    LSPDetailsPatchV2 patch = new LSPDetailsPatchV2();
    patch.headOffice(headOfficePatch);

    LspProviderEntity provider = LspProviderEntity.builder().build();
    assertDoesNotThrow(
        () -> LspPatchValidator.validateLspPatch(provider, patch, existingHeadOffice));
  }

  @Test
  void testValidateDxBothProvided() {
    // Both DX Number and DX Centre provided - should pass
    LSPHeadOfficeDetailsPatchV2 headOfficePatch = new LSPHeadOfficeDetailsPatchV2();
    DXPatchV2 dx = new DXPatchV2();
    dx.dxNumber("DX123");
    dx.dxCentre("London");
    headOfficePatch.dxDetails(dx);

    LSPDetailsPatchV2 patch = new LSPDetailsPatchV2();
    patch.headOffice(headOfficePatch);

    LspProviderEntity provider = LspProviderEntity.builder().build();
    assertDoesNotThrow(() -> LspPatchValidator.validateLspPatch(provider, patch, null));
  }

  @Test
  void testValidateDxNeitherProvided() {
    // Neither DX Number nor DX Centre - should pass
    LSPHeadOfficeDetailsPatchV2 headOfficePatch = new LSPHeadOfficeDetailsPatchV2();
    headOfficePatch.dxDetails(new DXPatchV2());

    LSPDetailsPatchV2 patch = new LSPDetailsPatchV2();
    patch.headOffice(headOfficePatch);

    LspProviderEntity provider = LspProviderEntity.builder().build();
    assertDoesNotThrow(() -> LspPatchValidator.validateLspPatch(provider, patch, null));
  }

  @Test
  void testValidateDxNumberWithoutCentre() {
    // DX Number without DX Centre - should fail
    LSPHeadOfficeDetailsPatchV2 headOfficePatch = new LSPHeadOfficeDetailsPatchV2();
    DXPatchV2 dx = new DXPatchV2();
    dx.dxNumber("DX123");
    headOfficePatch.dxDetails(dx);

    LSPDetailsPatchV2 patch = new LSPDetailsPatchV2();
    patch.headOffice(headOfficePatch);

    LspProviderEntity provider = LspProviderEntity.builder().build();
    assertThatThrownBy(() -> LspPatchValidator.validateLspPatch(provider, patch, null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("DX Number and DX Centre must be provided together");
  }

  @Test
  void testValidateDxCentreWithoutNumber() {
    // DX Centre without DX Number - should fail
    LSPHeadOfficeDetailsPatchV2 headOfficePatch = new LSPHeadOfficeDetailsPatchV2();
    DXPatchV2 dx = new DXPatchV2();
    dx.dxCentre("London");
    headOfficePatch.dxDetails(dx);

    LSPDetailsPatchV2 patch = new LSPDetailsPatchV2();
    patch.headOffice(headOfficePatch);

    LspProviderEntity provider = LspProviderEntity.builder().build();
    assertThatThrownBy(() -> LspPatchValidator.validateLspPatch(provider, patch, null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("DX Number and DX Centre must be provided together");
  }

  @Test
  void testValidatePaymentMethodEftRequiresBankAccount() {
    // AC3: EFT payment requires bank account
    LSPHeadOfficeDetailsPatchV2 headOfficePatch = new LSPHeadOfficeDetailsPatchV2();
    PaymentDetailsPatchOrLinkV2 payment = new PaymentDetailsPatchOrLinkV2();
    payment.paymentMethod(PaymentDetailsPaymentMethodV2.EFT);
    payment.bankAccountDetails(null); // No bank account
    headOfficePatch.payment(payment);

    LSPDetailsPatchV2 patch = new LSPDetailsPatchV2();
    patch.headOffice(headOfficePatch);

    LspProviderEntity provider = LspProviderEntity.builder().build();
    assertThatThrownBy(() -> LspPatchValidator.validateLspPatch(provider, patch, null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Bank Account must be provided when Payment Method is EFT");
  }

  @Test
  void testValidateSuccessfulUpdateWithAllFields() {
    // AC1: Successful update with all fields properly set
    LSPDetailsPatchV2 patch = new LSPDetailsPatchV2();
    patch.firmIntervenedFlag(true);
    patch.firmIntervenedDate(LocalDate.now());
    patch.holdAllPaymentsFlag(true);
    patch.holdAllPaymentsReason("Under review");
    patch.referredToDebtRecoveryFlag(false);

    LSPHeadOfficeDetailsPatchV2 headOfficePatch = new LSPHeadOfficeDetailsPatchV2();
    DXPatchV2 dx = new DXPatchV2();
    dx.dxNumber("DX456");
    dx.dxCentre("Manchester");
    headOfficePatch.dxDetails(dx);
    patch.headOffice(headOfficePatch);

    LspProviderEntity provider = LspProviderEntity.builder().build();
    // Should not throw any exception
    assertDoesNotThrow(() -> LspPatchValidator.validateLspPatch(provider, patch, null));
  }

  @Test
  void testValidateSuccessfulUpdateMinimal() {
    // AC1: Successful minimal update
    LSPDetailsPatchV2 patch = new LSPDetailsPatchV2();

    LspProviderEntity provider = LspProviderEntity.builder().build();
    // Should not throw any exception
    assertDoesNotThrow(() -> LspPatchValidator.validateLspPatch(provider, patch, null));
  }
}
