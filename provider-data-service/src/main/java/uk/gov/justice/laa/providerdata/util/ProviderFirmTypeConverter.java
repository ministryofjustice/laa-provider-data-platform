package uk.gov.justice.laa.providerdata.util;

import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;
import uk.gov.justice.laa.providerdata.model.ProviderFirmTypeV2;

/** Converts a {@code type} query parameter string to a {@link ProviderFirmTypeV2} enum. */
@Component
public class ProviderFirmTypeConverter implements Converter<String, ProviderFirmTypeV2> {

  @Override
  public ProviderFirmTypeV2 convert(String source) {
    return ProviderFirmTypeV2.fromValue(source);
  }
}
