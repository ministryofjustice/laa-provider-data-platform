package uk.gov.justice.laa.providerdata.config;

import java.util.function.Function;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.JacksonModule;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.deser.std.StdDeserializer;
import tools.jackson.databind.module.SimpleModule;
import uk.gov.justice.laa.providerdata.model.AdvocateOfficeLiaisonManagerCreateOrLinkV2;
import uk.gov.justice.laa.providerdata.model.AdvocateOfficePatchV2;
import uk.gov.justice.laa.providerdata.model.BankAccountProviderOfficeCreateV2;
import uk.gov.justice.laa.providerdata.model.BankAccountProviderOfficeLinkV2;
import uk.gov.justice.laa.providerdata.model.ChambersOfficePatchV2;
import uk.gov.justice.laa.providerdata.model.LSPOfficeLiaisonManagerCreateOrLinkV2;
import uk.gov.justice.laa.providerdata.model.LSPOfficePatchV2;
import uk.gov.justice.laa.providerdata.model.LiaisonManagerCreateV2;
import uk.gov.justice.laa.providerdata.model.LiaisonManagerLinkChambersV2;
import uk.gov.justice.laa.providerdata.model.LiaisonManagerLinkHeadOfficeV2;
import uk.gov.justice.laa.providerdata.model.OfficeLiaisonManagerCreateOrLinkV2;
import uk.gov.justice.laa.providerdata.model.OfficePatchV2;
import uk.gov.justice.laa.providerdata.model.PaymentDetailsCreateOrLinkV2BankAccountDetails;
import uk.gov.justice.laa.providerdata.model.PractitionerDetailsParentUpdateV2;
import uk.gov.justice.laa.providerdata.model.PractitionerDetailsParentUpdateV2OneOf;
import uk.gov.justice.laa.providerdata.model.PractitionerDetailsParentUpdateV2OneOf1;

/**
 * Registers {@link TypeResolvingDeserializer} instances for all {@code oneOf} types in the
 * generated model that lack an OpenAPI discriminator.
 */
@Configuration
public class JacksonConfig {

  /** Registers deserializers for all {@code oneOf} types that lack an OpenAPI discriminator. */
  @Bean
  JacksonModule deserializeOneOfWithoutDiscriminator() {
    return new SimpleModule()
        .addDeserializer(
            LSPOfficeLiaisonManagerCreateOrLinkV2.class,
            new TypeResolvingDeserializer<>(
                LSPOfficeLiaisonManagerCreateOrLinkV2.class,
                node ->
                    node.has("useHeadOfficeLiaisonManager")
                        ? LiaisonManagerLinkHeadOfficeV2.class
                        : LiaisonManagerCreateV2.class))
        .addDeserializer(
            AdvocateOfficeLiaisonManagerCreateOrLinkV2.class,
            new TypeResolvingDeserializer<>(
                AdvocateOfficeLiaisonManagerCreateOrLinkV2.class,
                node ->
                    node.has("useChambersLiaisonManager")
                        ? LiaisonManagerLinkChambersV2.class
                        : LiaisonManagerCreateV2.class))
        .addDeserializer(
            OfficeLiaisonManagerCreateOrLinkV2.class,
            new TypeResolvingDeserializer<>(
                OfficeLiaisonManagerCreateOrLinkV2.class,
                node -> {
                  Class<? extends OfficeLiaisonManagerCreateOrLinkV2> result;
                  if (node.has("useHeadOfficeLiaisonManager")) {
                    result = LiaisonManagerLinkHeadOfficeV2.class;
                  } else if (node.has("useChambersLiaisonManager")) {
                    result = LiaisonManagerLinkChambersV2.class;
                  } else {
                    result = LiaisonManagerCreateV2.class;
                  }
                  return result;
                }))
        .addDeserializer(
            PractitionerDetailsParentUpdateV2.class,
            new TypeResolvingDeserializer<>(
                PractitionerDetailsParentUpdateV2.class,
                node ->
                    node.has("parentGuid")
                        ? PractitionerDetailsParentUpdateV2OneOf.class
                        : PractitionerDetailsParentUpdateV2OneOf1.class))
        .addDeserializer(
            PaymentDetailsCreateOrLinkV2BankAccountDetails.class,
            new TypeResolvingDeserializer<>(
                PaymentDetailsCreateOrLinkV2BankAccountDetails.class,
                node ->
                    node.has("bankAccountGUID")
                        ? BankAccountProviderOfficeLinkV2.class
                        : BankAccountProviderOfficeCreateV2.class))
        .addDeserializer(
            OfficePatchV2.class,
            new TypeResolvingDeserializer<>(
                OfficePatchV2.class,
                node -> {
                  boolean hasLspOrAdvocateField =
                      node.has("payment")
                          || node.has("vatRegistration")
                          || node.has("intervened")
                          || node.has("debtRecoveryFlag")
                          || node.has("falseBalanceFlag");
                  if (!hasLspOrAdvocateField) {
                    return ChambersOfficePatchV2.class;
                  }
                  boolean hasContactField =
                      node.has("address")
                          || node.has("telephoneNumber")
                          || node.has("emailAddress")
                          || node.has("website")
                          || node.has("dxDetails");
                  return hasContactField ? LSPOfficePatchV2.class : AdvocateOfficePatchV2.class;
                }));
  }

  /**
   * Deserializer for {@code oneOf} types that lack an OpenAPI discriminator. Reads the incoming
   * JSON as a tree so the content can be inspected, delegates type selection to the provided
   * resolver function, then deserializes into the resolved subtype.
   */
  static final class TypeResolvingDeserializer<T> extends StdDeserializer<T> {

    private final Function<JsonNode, Class<? extends T>> typeResolver;

    TypeResolvingDeserializer(Class<T> vc, Function<JsonNode, Class<? extends T>> typeResolver) {
      super(vc);
      this.typeResolver = typeResolver;
    }

    @Override
    public T deserialize(JsonParser p, DeserializationContext ctx) throws JacksonException {
      JsonNode node = p.readValueAsTree();
      return ctx.readTreeAsValue(node, typeResolver.apply(node));
    }
  }
}
