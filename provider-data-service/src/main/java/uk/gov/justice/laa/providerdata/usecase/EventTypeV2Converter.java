package uk.gov.justice.laa.providerdata.usecase;

import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;
import uk.gov.justice.laa.providerdata.model.EventTypeV2;

/** Converts an {@code eventType} query parameter string to an {@link EventTypeV2} enum. */
@Component
public class EventTypeV2Converter implements Converter<String, EventTypeV2> {

  @Override
  public EventTypeV2 convert(String source) {
    return EventTypeV2.fromValue(source);
  }
}
