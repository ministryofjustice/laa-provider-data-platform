package uk.gov.justice.laa.providerdata.util;

import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;
import uk.gov.justice.laa.providerdata.model.ProviderFirmTypeV2;

/**
 * Converts a query parameter string to a {@link ProviderFirmTypeV2} enum. Supports case-insensitive
 * matching and allows Spring to map request parameters to the enum automatically.
 */
@Component
public class ProviderFirmTypeConverter implements Converter<String, ProviderFirmTypeV2> {

  /**
   * Converts the given string to a {@link ProviderFirmTypeV2}.
   *
   * @param source the string value from the request
   * @return the matching {@link ProviderFirmTypeV2} enum
   * @throws IllegalArgumentException if the value is invalid
   */
  @Override
  public ProviderFirmTypeV2 convert(String source) {
    for (ProviderFirmTypeV2 v : ProviderFirmTypeV2.values()) {
      if (v.getValue().equalsIgnoreCase(source) || v.name().equalsIgnoreCase(source)) {
        return v;
      }
    }
    throw new IllegalArgumentException(
        "Invalid provider firm type: '"
            + source
            + "'. Allowed values: Legal Services Provider, Chambers, Advocate");
  }
}
