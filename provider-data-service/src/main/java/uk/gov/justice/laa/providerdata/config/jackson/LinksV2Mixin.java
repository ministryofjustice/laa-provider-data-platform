package uk.gov.justice.laa.providerdata.config.jackson;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.net.URI;
import org.springframework.boot.jackson.JacksonMixin;
import uk.gov.justice.laa.providerdata.model.LinksV2;

/**
 * Suppresses null {@code next} and {@code prev} fields when serialising {@link LinksV2}.
 *
 * <p>On the first and last pages respectively, these links are not set; this mixin omits them from
 * the JSON response rather than emitting {@code "next": null}.
 */
@JacksonMixin(LinksV2.class)
abstract class LinksV2Mixin {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  abstract URI getNext();

  @JsonInclude(JsonInclude.Include.NON_NULL)
  abstract URI getPrev();
}
