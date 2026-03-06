package uk.gov.justice.laa.providerdata.config.jackson;

import org.springframework.boot.jackson.JacksonComponent;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.deser.std.StdDeserializer;
import uk.gov.justice.laa.providerdata.model.PractitionerDetailsParentUpdateV2;
import uk.gov.justice.laa.providerdata.model.PractitionerDetailsParentUpdateV2OneOf;
import uk.gov.justice.laa.providerdata.model.PractitionerDetailsParentUpdateV2OneOf1;

/**
 * Deserializes {@link PractitionerDetailsParentUpdateV2}, which is a {@code oneOf} of {@link
 * PractitionerDetailsParentUpdateV2OneOf} (identified by {@code parentGuid}) and {@link
 * PractitionerDetailsParentUpdateV2OneOf1} (identified by {@code parentFirmNumber}).
 */
@JacksonComponent(type = PractitionerDetailsParentUpdateV2.class)
class ParentFirmDeserializer extends StdDeserializer<PractitionerDetailsParentUpdateV2> {

  ParentFirmDeserializer() {
    super(PractitionerDetailsParentUpdateV2.class);
  }

  @Override
  public PractitionerDetailsParentUpdateV2 deserialize(JsonParser p, DeserializationContext ctx)
      throws JacksonException {
    JsonNode node = p.readValueAsTree();
    Class<? extends PractitionerDetailsParentUpdateV2> targetType =
        node.has("parentGuid")
            ? PractitionerDetailsParentUpdateV2OneOf.class
            : PractitionerDetailsParentUpdateV2OneOf1.class;
    return ctx.readTreeAsValue(node, targetType);
  }
}
