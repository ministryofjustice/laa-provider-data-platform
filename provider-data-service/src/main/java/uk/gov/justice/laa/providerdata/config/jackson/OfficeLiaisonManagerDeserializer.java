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
 * Deserializes {@link OfficeLiaisonManagerCreateOrLinkV2} from the wrapper payload used by this
 * service:
 *
 * <ul>
 *   <li>{@code {"create": {...}}}
 *   <li>{@code {"linkHeadOffice": {...}}}
 *   <li>{@code {"linkChambers": {...}}}
 * </ul>
 *
 * <p>The OpenAPI spec defines no discriminator, so the type is inferred by wrapper field presence.
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

    JsonNode createNode = node.get("create");
    if (createNode != null && !createNode.isNull()) {
      return ctx.readTreeAsValue(createNode, LiaisonManagerCreateV2.class);
    }

    JsonNode linkHeadOfficeNode = node.get("linkHeadOffice");
    if (linkHeadOfficeNode != null && !linkHeadOfficeNode.isNull()) {
      return ctx.readTreeAsValue(linkHeadOfficeNode, LiaisonManagerLinkHeadOfficeV2.class);
    }

    JsonNode linkChambersNode = node.get("linkChambers");
    if (linkChambersNode != null && !linkChambersNode.isNull()) {
      return ctx.readTreeAsValue(linkChambersNode, LiaisonManagerLinkChambersV2.class);
    }

    throw new IllegalArgumentException(
        "Exactly one of create, linkHeadOffice, linkChambers must be provided");
  }
}
