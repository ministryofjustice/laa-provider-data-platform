package uk.gov.justice.laa.providerdata.config.jackson;

import org.springframework.boot.jackson.JacksonComponent;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.deser.std.StdDeserializer;
import uk.gov.justice.laa.providerdata.model.LiaisonManagerCreateV2;
import uk.gov.justice.laa.providerdata.model.LiaisonManagerLinkChambersV2;
import uk.gov.justice.laa.providerdata.model.LiaisonManagerLinkHeadOfficeV2;
import uk.gov.justice.laa.providerdata.model.OfficeLiaisonManagerCreateOrLinkV2;

/**
 * Deserializes {@link OfficeLiaisonManagerCreateOrLinkV2} by discriminating on field presence,
 * matching the flat payload structure defined in the OpenAPI spec.
 *
 * <ul>
 *   <li>Presence of {@code useHeadOfficeLiaisonManager} → {@link LiaisonManagerLinkHeadOfficeV2}
 *   <li>Presence of {@code useChambersLiaisonManager} → {@link LiaisonManagerLinkChambersV2}
 *   <li>Otherwise → {@link LiaisonManagerCreateV2}
 * </ul>
 */
@JacksonComponent(type = OfficeLiaisonManagerCreateOrLinkV2.class)
class OfficeLiaisonManagerDeserializer extends StdDeserializer<OfficeLiaisonManagerCreateOrLinkV2> {

  OfficeLiaisonManagerDeserializer() {
    super(OfficeLiaisonManagerCreateOrLinkV2.class);
  }

  @Override
  public OfficeLiaisonManagerCreateOrLinkV2 deserialize(JsonParser p, DeserializationContext ctx)
      throws JacksonException {
    JsonNode node = p.readValueAsTree();

    if (node.has("useHeadOfficeLiaisonManager")) {
      return ctx.readTreeAsValue(node, LiaisonManagerLinkHeadOfficeV2.class);
    }

    if (node.has("useChambersLiaisonManager")) {
      return ctx.readTreeAsValue(node, LiaisonManagerLinkChambersV2.class);
    }

    return ctx.readTreeAsValue(node, LiaisonManagerCreateV2.class);
  }
}
