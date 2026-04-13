package uk.gov.justice.laa.providerdata.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import uk.gov.justice.laa.providerdata.model.ProviderFirmTypeV2;

class ProviderFirmTypeConverterTest {

  private final ProviderFirmTypeConverter converter = new ProviderFirmTypeConverter();

  @Test
  void convert_legalServicesProvider_returnsEnum() {
    assertThat(converter.convert("Legal Services Provider"))
        .isEqualTo(ProviderFirmTypeV2.LEGAL_SERVICES_PROVIDER);
  }

  @Test
  void convert_chambers_returnsEnum() {
    assertThat(converter.convert("Chambers")).isEqualTo(ProviderFirmTypeV2.CHAMBERS);
  }

  @Test
  void convert_advocate_returnsEnum() {
    assertThat(converter.convert("Advocate")).isEqualTo(ProviderFirmTypeV2.ADVOCATE);
  }

  @Test
  void convert_unknownValue_throwsIllegalArgumentException() {
    assertThatThrownBy(() -> converter.convert("unknown"))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
