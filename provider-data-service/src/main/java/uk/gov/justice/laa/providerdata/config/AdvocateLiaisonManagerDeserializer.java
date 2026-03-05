package uk.gov.justice.laa.providerdata.config;

import org.springframework.boot.jackson.JacksonComponent;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.deser.std.StdDeserializer;
import uk.gov.justice.laa.providerdata.model.AdvocateOfficeLiaisonManagerCreateOrLinkV2;
import uk.gov.justice.laa.providerdata.model.LiaisonManagerCreateV2;
import uk.gov.justice.laa.providerdata.model.LiaisonManagerLinkChambersV2;

/**
 * Deserializes {@link AdvocateOfficeLiaisonManagerCreateOrLinkV2}, which is a {@code oneOf} of
 * {@link LiaisonManagerLinkChambersV2} and {@link LiaisonManagerCreateV2}.
 *
 * <p>The spec defines no discriminator, so the type is inferred from field presence: {@code
 * useChambersLiaisonManager} is unique to {@link LiaisonManagerLinkChambersV2}; its absence implies
 * {@link LiaisonManagerCreateV2}.
 */
@JacksonComponent(type = AdvocateOfficeLiaisonManagerCreateOrLinkV2.class)
class AdvocateLiaisonManagerDeserializer
    extends StdDeserializer<AdvocateOfficeLiaisonManagerCreateOrLinkV2> {

  AdvocateLiaisonManagerDeserializer() {
    super(AdvocateOfficeLiaisonManagerCreateOrLinkV2.class);
  }

  @Override
  public AdvocateOfficeLiaisonManagerCreateOrLinkV2 deserialize(
      JsonParser p, DeserializationContext ctx) throws JacksonException {
    JsonNode node = p.readValueAsTree();
    Class<? extends AdvocateOfficeLiaisonManagerCreateOrLinkV2> targetType =
        node.has("useChambersLiaisonManager")
            ? LiaisonManagerLinkChambersV2.class
            : LiaisonManagerCreateV2.class;
    return ctx.readTreeAsValue(node, targetType);
  }
}
