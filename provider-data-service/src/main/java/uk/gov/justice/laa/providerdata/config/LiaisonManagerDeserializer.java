package uk.gov.justice.laa.providerdata.config;

import org.springframework.boot.jackson.JacksonComponent;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.deser.std.StdDeserializer;
import uk.gov.justice.laa.providerdata.model.LSPOfficeLiaisonManagerCreateOrLinkV2;
import uk.gov.justice.laa.providerdata.model.LiaisonManagerCreateV2;
import uk.gov.justice.laa.providerdata.model.LiaisonManagerLinkHeadOfficeV2;

/**
 * Deserializes {@link LSPOfficeLiaisonManagerCreateOrLinkV2}, which is a {@code oneOf} of {@link
 * LiaisonManagerCreateV2} and {@link LiaisonManagerLinkHeadOfficeV2}.
 *
 * <p>The spec defines no discriminator, so the type is inferred from field presence: {@code
 * useHeadOfficeLiaisonManager} is unique to {@link LiaisonManagerLinkHeadOfficeV2}; its absence
 * implies {@link LiaisonManagerCreateV2}.
 */
@JacksonComponent(type = LSPOfficeLiaisonManagerCreateOrLinkV2.class)
class LiaisonManagerDeserializer extends StdDeserializer<LSPOfficeLiaisonManagerCreateOrLinkV2> {

  LiaisonManagerDeserializer() {
    super(LSPOfficeLiaisonManagerCreateOrLinkV2.class);
  }

  @Override
  public LSPOfficeLiaisonManagerCreateOrLinkV2 deserialize(JsonParser p, DeserializationContext ctx)
      throws JacksonException {
    JsonNode node = p.readValueAsTree();
    Class<? extends LSPOfficeLiaisonManagerCreateOrLinkV2> targetType =
        node.has("useHeadOfficeLiaisonManager")
            ? LiaisonManagerLinkHeadOfficeV2.class
            : LiaisonManagerCreateV2.class;
    return ctx.readTreeAsValue(node, targetType);
  }
}
