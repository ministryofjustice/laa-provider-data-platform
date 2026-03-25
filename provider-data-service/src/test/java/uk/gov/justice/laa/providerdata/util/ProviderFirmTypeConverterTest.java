package uk.gov.justice.laa.providerdata.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.justice.laa.providerdata.model.ProviderFirmTypeV2;

class ProviderFirmTypeConverterTest {

  private ProviderFirmTypeConverter converter;

  @BeforeEach
  void setUp() {
    converter = new ProviderFirmTypeConverter();
  }

  @Test
  void convert_withExactName_shouldReturnEnum() {
    ProviderFirmTypeV2 result = converter.convert("Advocate");
    assertEquals(ProviderFirmTypeV2.ADVOCATE, result);

    result = converter.convert("Chambers");
    assertEquals(ProviderFirmTypeV2.CHAMBERS, result);

    result = converter.convert("Legal Services Provider");
    assertEquals(ProviderFirmTypeV2.LEGAL_SERVICES_PROVIDER, result);
  }

  @Test
  void convert_withDifferentCase_shouldReturnEnum() {
    ProviderFirmTypeV2 result = converter.convert("advocate");
    assertEquals(ProviderFirmTypeV2.ADVOCATE, result);

    result = converter.convert("CHAMBERS");
    assertEquals(ProviderFirmTypeV2.CHAMBERS, result);

    result = converter.convert("legal services provider");
    assertEquals(ProviderFirmTypeV2.LEGAL_SERVICES_PROVIDER, result);
  }

  @Test
  void convert_withEnumName_shouldReturnEnum() {
    // Using enum constant names (ADVOCATE, CHAMBERS, LEGAL_SERVICES_PROVIDER)
    ProviderFirmTypeV2 result = converter.convert("ADVOCATE");
    assertEquals(ProviderFirmTypeV2.ADVOCATE, result);

    result = converter.convert("CHAMBERS");
    assertEquals(ProviderFirmTypeV2.CHAMBERS, result);

    result = converter.convert("LEGAL_SERVICES_PROVIDER");
    assertEquals(ProviderFirmTypeV2.LEGAL_SERVICES_PROVIDER, result);
  }

  @Test
  void convert_withInvalidValue_shouldThrowException() {
    IllegalArgumentException ex =
        assertThrows(IllegalArgumentException.class, () -> converter.convert("InvalidType"));
    String expectedMessage =
        "Invalid provider firm type: 'InvalidType'. Allowed values: "
            + "Legal Services Provider, Chambers, Advocate";
    assertEquals(expectedMessage, ex.getMessage());
  }

  @Test
  void convert_withEmptyOrNullValue_shouldThrowException() {
    IllegalArgumentException ex1 =
        assertThrows(IllegalArgumentException.class, () -> converter.convert(""));
    assertEquals(
        "Invalid provider firm type: ''. Allowed values:"
            + " Legal Services Provider, Chambers, Advocate",
        ex1.getMessage());

    IllegalArgumentException ex2 =
        assertThrows(IllegalArgumentException.class, () -> converter.convert(null));
    assertEquals(
        "Invalid provider firm type: 'null'. Allowed values:"
            + " Legal Services Provider, Chambers, Advocate",
        ex2.getMessage());
  }
}
