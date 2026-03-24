package uk.gov.justice.laa.providerdata.util;

import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;
import uk.gov.justice.laa.providerdata.model.ProviderFirmTypeV2;

/**
 * Converts a query parameter string to a {@link ProviderFirmTypeV2} enum. Used by Spring to map
 * request parameters to the enum automatically.
 */
@Component
public class ProviderFirmTypeConverter implements Converter<String, ProviderFirmTypeV2> {
  /**
   * Converts the given string to a {@link ProviderFirmTypeV2}.
   *
   * @param source the string value from the request
   * @return the matching {@link ProviderFirmTypeV2} enum
   */
  @Override
  public ProviderFirmTypeV2 convert(String source) {
    return ProviderFirmTypeV2.fromValue(source);
  }
}
