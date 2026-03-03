package uk.gov.justice.laa.providerdata.api.model;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;

class ProviderFirmCreateRequestValidationTest {

  @Test
  void accepts_when_exactly_one_variant_present() {
    var validator = Validation.buildDefaultValidatorFactory().getValidator();

    var req =
        new ProviderFirmCreateRequest(
            new ProviderCreateLsp(new ProviderCreateBase("PF1", "LSP")), null, null);

    var violations = validator.validate(req);
    assertThat(violations).isEmpty();
  }

  @Test
  void rejects_when_no_variants_present() {
    var validator = Validation.buildDefaultValidatorFactory().getValidator();

    var req = new ProviderFirmCreateRequest(null, null, null);

    var violations = validator.validate(req);
    assertThat(violations).isNotEmpty();
  }
}
