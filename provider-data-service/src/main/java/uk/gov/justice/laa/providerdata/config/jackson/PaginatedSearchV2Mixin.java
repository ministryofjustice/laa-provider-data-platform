package uk.gov.justice.laa.providerdata.config.jackson;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.springframework.boot.jackson.JacksonMixin;
import uk.gov.justice.laa.providerdata.model.PaginatedSearchV2;
import uk.gov.justice.laa.providerdata.model.SortV2;

/**
 * Suppresses the null {@code sort} field when serialising {@link PaginatedSearchV2}.
 *
 * <p>Sort is omitted from the response when no sort order has been specified, rather than emitting
 * {@code "sort": null}.
 */
@JacksonMixin(PaginatedSearchV2.class)
abstract class PaginatedSearchV2Mixin {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  abstract SortV2 getSort();
}
