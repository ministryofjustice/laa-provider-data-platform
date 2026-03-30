package uk.gov.justice.laa.providerdata.config;

import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;
import uk.gov.justice.laa.providerdata.model.ProviderFirmTypeV2;

/**
 * Converts a query parameter string to a {@link ProviderFirmTypeV2} enum. Supports case-insensitive
 * matching and allows matching by enum value or name.
 */
@Component
public class ProviderFirmTypeConverter implements Converter<String, ProviderFirmTypeV2> {

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
