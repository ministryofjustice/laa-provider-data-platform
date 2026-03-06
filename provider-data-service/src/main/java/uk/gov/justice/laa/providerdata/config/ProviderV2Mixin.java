package uk.gov.justice.laa.providerdata.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.springframework.boot.jackson.JacksonMixin;
import uk.gov.justice.laa.providerdata.model.ChamberDetailsV2;
import uk.gov.justice.laa.providerdata.model.LSPDetailsV2;
import uk.gov.justice.laa.providerdata.model.PractitionerDetailsV2;
import uk.gov.justice.laa.providerdata.model.ProviderV2;

/**
 * Suppresses null firm-type variant fields when serialising {@link ProviderV2}.
 *
 * <p>The generated class always includes {@code legalServicesProvider}, {@code chambers}, and
 * {@code practitioner} — even when two of the three are {@code null}. This mixin ensures only the
 * populated variant appears in the JSON response.
 */
@JacksonMixin(ProviderV2.class)
@JsonInclude(JsonInclude.Include.NON_NULL)
abstract class ProviderV2Mixin {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  abstract LSPDetailsV2 getLegalServicesProvider();

  @JsonInclude(JsonInclude.Include.NON_NULL)
  abstract ChamberDetailsV2 getChambers();

  @JsonInclude(JsonInclude.Include.NON_NULL)
  abstract PractitionerDetailsV2 getPractitioner();
}
