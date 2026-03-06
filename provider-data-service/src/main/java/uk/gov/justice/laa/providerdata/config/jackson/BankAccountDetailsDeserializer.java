package uk.gov.justice.laa.providerdata.config.jackson;

import org.springframework.boot.jackson.JacksonComponent;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.deser.std.StdDeserializer;
import uk.gov.justice.laa.providerdata.model.BankAccountProviderOfficeCreateV2;
import uk.gov.justice.laa.providerdata.model.BankAccountProviderOfficeLinkV2;
import uk.gov.justice.laa.providerdata.model.PaymentDetailsCreateOrLinkV2BankAccountDetails;

/**
 * Deserializes {@link PaymentDetailsCreateOrLinkV2BankAccountDetails}, which is a {@code oneOf} of
 * {@link BankAccountProviderOfficeCreateV2} and {@link BankAccountProviderOfficeLinkV2}.
 *
 * <p>The spec defines no discriminator, so the type is inferred from field presence: {@code
 * bankAccountGUID} is unique to {@link BankAccountProviderOfficeLinkV2}; its absence implies {@link
 * BankAccountProviderOfficeCreateV2}.
 */
@JacksonComponent(type = PaymentDetailsCreateOrLinkV2BankAccountDetails.class)
class BankAccountDetailsDeserializer
    extends StdDeserializer<PaymentDetailsCreateOrLinkV2BankAccountDetails> {

  BankAccountDetailsDeserializer() {
    super(PaymentDetailsCreateOrLinkV2BankAccountDetails.class);
  }

  @Override
  public PaymentDetailsCreateOrLinkV2BankAccountDetails deserialize(
      JsonParser p, DeserializationContext ctx) throws JacksonException {
    JsonNode node = p.readValueAsTree();
    Class<? extends PaymentDetailsCreateOrLinkV2BankAccountDetails> targetType =
        node.has("bankAccountGUID")
            ? BankAccountProviderOfficeLinkV2.class
            : BankAccountProviderOfficeCreateV2.class;
    return ctx.readTreeAsValue(node, targetType);
  }
}
