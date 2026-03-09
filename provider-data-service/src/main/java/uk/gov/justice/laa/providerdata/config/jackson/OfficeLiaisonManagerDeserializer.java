package uk.gov.justice.laa.providerdata.config.jackson;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import java.io.IOException;
import org.springframework.boot.jackson.JacksonComponent;
import uk.gov.justice.laa.providerdata.model.LiaisonManagerCreateV2;
import uk.gov.justice.laa.providerdata.model.LiaisonManagerLinkChambersV2;
import uk.gov.justice.laa.providerdata.model.LiaisonManagerLinkHeadOfficeV2;
import uk.gov.justice.laa.providerdata.model.OfficeLiaisonManagerCreateOrLinkV2;

/**
 * Deserializes {@link OfficeLiaisonManagerCreateOrLinkV2}, which is a {@code oneOf} of {@link
 * LiaisonManagerCreateV2}, {@link LiaisonManagerLinkHeadOfficeV2} and {@link
 * LiaisonManagerLinkChambersV2}.
 *
 * <p>The spec defines no discriminator, so the type is inferred from field presence.
 */
@JacksonComponent(type = OfficeLiaisonManagerCreateOrLinkV2.class)
class OfficeLiaisonManagerDeserializer extends StdDeserializer<OfficeLiaisonManagerCreateOrLinkV2> {

  OfficeLiaisonManagerDeserializer() {
    super(OfficeLiaisonManagerCreateOrLinkV2.class);
  }

  @Override
  public OfficeLiaisonManagerCreateOrLinkV2 deserialize(JsonParser p, DeserializationContext ctx)
      throws JacksonException {
    try {
      JsonNode node = p.readValueAsTree();

      Class<? extends OfficeLiaisonManagerCreateOrLinkV2> targetType =
          node.has("useHeadOfficeLiaisonManager")
              ? LiaisonManagerLinkHeadOfficeV2.class
              : node.has("useChambersLiaisonManager")
                  ? LiaisonManagerLinkChambersV2.class
                  : LiaisonManagerCreateV2.class;

      return ctx.readTreeAsValue(node, targetType);
    } catch (IOException e) {
      throw new JsonParseException(
          p, "Failed to deserialize OfficeLiaisonManagerCreateOrLinkV2", e);
    }
  }
}
