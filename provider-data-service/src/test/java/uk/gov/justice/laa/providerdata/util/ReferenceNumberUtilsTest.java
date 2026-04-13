package uk.gov.justice.laa.providerdata.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import uk.gov.justice.laa.providerdata.entity.AdvocateType;
import uk.gov.justice.laa.providerdata.entity.FirmType;

class ReferenceNumberUtilsTest {

  @Test
  void generateFirmNumber_singleArg_isNotBlank() {
    assertThat(ReferenceNumberUtils.generateFirmNumber(FirmType.LEGAL_SERVICES_PROVIDER))
        .isNotBlank();
  }

  @Test
  void generateFirmNumber_twoArgs_isNotBlank() {
    assertThat(ReferenceNumberUtils.generateFirmNumber(FirmType.ADVOCATE, AdvocateType.BARRISTER))
        .isNotBlank();
  }

  @Test
  void generateFirmNumber_returnsUniqueValues() {
    String a = ReferenceNumberUtils.generateFirmNumber(FirmType.LEGAL_SERVICES_PROVIDER);
    String b = ReferenceNumberUtils.generateFirmNumber(FirmType.LEGAL_SERVICES_PROVIDER);
    assertThat(a).isNotEqualTo(b);
  }

  @Test
  void generateAccountNumber_hasSixCharacters() {
    assertThat(ReferenceNumberUtils.generateAccountNumber(FirmType.LEGAL_SERVICES_PROVIDER, null))
        .hasSize(6);
  }

  @Test
  void generateAccountNumber_isNotBlank() {
    assertThat(ReferenceNumberUtils.generateAccountNumber(FirmType.LEGAL_SERVICES_PROVIDER, null))
        .isNotBlank();
  }

  @Test
  void generateAccountNumber_returnsUniqueValues() {
    assertThat(ReferenceNumberUtils.generateAccountNumber(FirmType.LEGAL_SERVICES_PROVIDER, null))
        .isNotEqualTo(
            ReferenceNumberUtils.generateAccountNumber(FirmType.LEGAL_SERVICES_PROVIDER, null));
  }
}
