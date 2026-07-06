package uk.gov.justice.laa.providerdata.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.JacksonModule;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.deser.DeserializationProblemHandler;
import tools.jackson.databind.deser.std.StdDeserializer;
import tools.jackson.databind.exc.MismatchedInputException;
import tools.jackson.databind.module.SimpleModule;
import uk.gov.justice.laa.providerdata.annotation.RejectProperties;
import uk.gov.justice.laa.providerdata.model.AdvocateOfficeLiaisonManagerCreateOrLinkV2;
import uk.gov.justice.laa.providerdata.model.AdvocateOfficePatchV2;
import uk.gov.justice.laa.providerdata.model.BankAccountProviderOfficeCreateV2;
import uk.gov.justice.laa.providerdata.model.BankAccountProviderOfficeLinkV2;
import uk.gov.justice.laa.providerdata.model.ChambersOfficeLiaisonManagerCreateOrLinkV2;
import uk.gov.justice.laa.providerdata.model.ChambersOfficePatchV2;
import uk.gov.justice.laa.providerdata.model.ContractManagerLinkByGUIDV2;
import uk.gov.justice.laa.providerdata.model.ContractManagerLinkDefaultV2;
import uk.gov.justice.laa.providerdata.model.ContractManagerLinkHeadOfficeV2;
import uk.gov.justice.laa.providerdata.model.HeadOfficeContractManagerLinkV2;
import uk.gov.justice.laa.providerdata.model.LSPOfficeContractManagerLinkV2;
import uk.gov.justice.laa.providerdata.model.LSPOfficeLiaisonManagerCreateOrLinkV2;
import uk.gov.justice.laa.providerdata.model.LSPOfficePatchV2;
import uk.gov.justice.laa.providerdata.model.LiaisonManagerCreateV2;
import uk.gov.justice.laa.providerdata.model.LiaisonManagerLinkByGUIDV2;
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
 * generated model that lack an OpenAPI discriminator, and enforces read-only field restrictions for
 * schemas annotated with {@link RejectProperties}.
 */
@Configuration
public class JacksonConfig {

  /**
   * Registers a {@link DeserializationProblemHandler} that rejects specific named JSON fields for
   * any model class annotated with {@link RejectProperties}. This enforces read-only field
   * restrictions declared in the OpenAPI schema, returning a 400 Bad Request when a caller sends a
   * field listed in {@link RejectProperties#names()}.
   *
   * <p>To apply this to a generated model, add to the schema in {@code laa-data-pda.yml}:
   * <pre>{@code
   * x-class-extra-annotation: >-
   *   @uk.gov.justice.laa.providerdata.annotation.RejectProperties(
   *   {"fieldOne", "fieldTwo"})
   * }</pre>
   */
  @Bean
  JacksonModule rejectPropertiesModule() {
    return new SimpleModule() {
      @Override
      public void setupModule(SetupContext context) {
        super.setupModule(context);
        context.addHandler(
            new DeserializationProblemHandler() {
              @Override
              public boolean handleUnknownProperty(
                  DeserializationContext ctxt,
                  JsonParser p,
                  ValueDeserializer<?> deserializer,
                  Object beanOrClass,
                  String propertyName)
                  throws JacksonException {
                if (beanOrClass != null) {
                  RejectProperties annotation =
                      beanOrClass.getClass().getAnnotation(RejectProperties.class);
                  if (annotation != null) {
                    for (String name : annotation.value()) {
                      if (name.equals(propertyName)) {
                        throw MismatchedInputException.from(
                            p,
                            beanOrClass.getClass(),
                            String.format("Field '%s' must not be provided", propertyName));
                      }
                    }
                  }
                }
                return false;
              }
            });
      }
    };
  }

  /** Registers deserializers for all {@code oneOf} types that lack an OpenAPI discriminator. */
  @Bean
  JacksonModule deserializeOneOfWithoutDiscriminator() {
    return new SimpleModule()
        .addDeserializer(
            LSPOfficeLiaisonManagerCreateOrLinkV2.class,
            new TypeResolvingDeserializer<>(
                LSPOfficeLiaisonManagerCreateOrLinkV2.class,
                (node, p) -> {
                  if (node.has("useHeadOfficeLiaisonManager")) {
                    rejectConflictingFields(
                        node,
                        p,
                        "useHeadOfficeLiaisonManager",
                        "liaisonManagerGUID",
                        "firstName",
                        "lastName",
                        "emailAddress",
                        "telephoneNumber");
                    return LiaisonManagerLinkHeadOfficeV2.class;
                  } else if (node.has("liaisonManagerGUID")) {
                    rejectConflictingFields(
                        node,
                        p,
                        "liaisonManagerGUID",
                        "firstName",
                        "lastName",
                        "emailAddress",
                        "telephoneNumber");
                    return LiaisonManagerLinkByGUIDV2.class;
                  } else {
                    return LiaisonManagerCreateV2.class;
                  }
                }))
        .addDeserializer(
            AdvocateOfficeLiaisonManagerCreateOrLinkV2.class,
            new TypeResolvingDeserializer<>(
                AdvocateOfficeLiaisonManagerCreateOrLinkV2.class,
                (node, p) -> {
                  if (node.has("useChambersLiaisonManager")) {
                    rejectConflictingFields(
                        node,
                        p,
                        "useChambersLiaisonManager",
                        "liaisonManagerGUID",
                        "firstName",
                        "lastName",
                        "emailAddress",
                        "telephoneNumber");
                    return LiaisonManagerLinkChambersV2.class;
                  } else if (node.has("liaisonManagerGUID")) {
                    rejectConflictingFields(
                        node,
                        p,
                        "liaisonManagerGUID",
                        "firstName",
                        "lastName",
                        "emailAddress",
                        "telephoneNumber");
                    return LiaisonManagerLinkByGUIDV2.class;
                  } else {
                    return LiaisonManagerCreateV2.class;
                  }
                }))
        .addDeserializer(
            ChambersOfficeLiaisonManagerCreateOrLinkV2.class,
            new TypeResolvingDeserializer<>(
                ChambersOfficeLiaisonManagerCreateOrLinkV2.class,
                (node, p) -> {
                  if (node.has("liaisonManagerGUID")) {
                    rejectConflictingFields(
                        node,
                        p,
                        "liaisonManagerGUID",
                        "firstName",
                        "lastName",
                        "emailAddress",
                        "telephoneNumber");
                    return LiaisonManagerLinkByGUIDV2.class;
                  } else {
                    return LiaisonManagerCreateV2.class;
                  }
                }))
        .addDeserializer(
            OfficeLiaisonManagerCreateOrLinkV2.class,
            new TypeResolvingDeserializer<>(
                OfficeLiaisonManagerCreateOrLinkV2.class,
                (node, p) -> {
                  if (node.has("useHeadOfficeLiaisonManager")) {
                    rejectConflictingFields(
                        node,
                        p,
                        "useHeadOfficeLiaisonManager",
                        "useChambersLiaisonManager",
                        "liaisonManagerGUID",
                        "firstName",
                        "lastName",
                        "emailAddress",
                        "telephoneNumber");
                    return LiaisonManagerLinkHeadOfficeV2.class;
                  } else if (node.has("useChambersLiaisonManager")) {
                    rejectConflictingFields(
                        node,
                        p,
                        "useChambersLiaisonManager",
                        "liaisonManagerGUID",
                        "firstName",
                        "lastName",
                        "emailAddress",
                        "telephoneNumber");
                    return LiaisonManagerLinkChambersV2.class;
                  } else if (node.has("liaisonManagerGUID")) {
                    rejectConflictingFields(
                        node,
                        p,
                        "liaisonManagerGUID",
                        "useHeadOfficeLiaisonManager",
                        "useChambersLiaisonManager",
                        "firstName",
                        "lastName",
                        "emailAddress",
                        "telephoneNumber");
                    return LiaisonManagerLinkByGUIDV2.class;
                  } else {
                    return LiaisonManagerCreateV2.class;
                  }
                }))
        .addDeserializer(
            LSPOfficeContractManagerLinkV2.class,
            new TypeResolvingDeserializer<>(
                LSPOfficeContractManagerLinkV2.class,
                (node, p) -> {
                  if (node.has("useHeadOfficeContractManager")) {
                    rejectConflictingFields(
                        node,
                        p,
                        "useHeadOfficeContractManager",
                        "useDefaultContractManager",
                        "contractManagerGUID");
                    return ContractManagerLinkHeadOfficeV2.class;
                  } else if (node.has("useDefaultContractManager")) {
                    rejectConflictingFields(
                        node,
                        p,
                        "useDefaultContractManager",
                        "useHeadOfficeContractManager",
                        "contractManagerGUID");
                    return ContractManagerLinkDefaultV2.class;
                  } else {
                    return ContractManagerLinkByGUIDV2.class;
                  }
                }))
        .addDeserializer(
            HeadOfficeContractManagerLinkV2.class,
            new TypeResolvingDeserializer<>(
                HeadOfficeContractManagerLinkV2.class,
                (node, p) -> {
                  if (node.has("useDefaultContractManager")) {
                    rejectConflictingFields(
                        node, p, "useDefaultContractManager", "contractManagerGUID");
                    return ContractManagerLinkDefaultV2.class;
                  } else {
                    return ContractManagerLinkByGUIDV2.class;
                  }
                }))
        .addDeserializer(
            PractitionerDetailsParentUpdateV2.class,
            new TypeResolvingDeserializer<>(
                PractitionerDetailsParentUpdateV2.class,
                (node, p) ->
                    node.has("parentGUID")
                        ? PractitionerDetailsParentUpdateV2OneOf.class
                        : PractitionerDetailsParentUpdateV2OneOf1.class))
        .addDeserializer(
            PaymentDetailsCreateOrLinkV2BankAccountDetails.class,
            new TypeResolvingDeserializer<>(
                PaymentDetailsCreateOrLinkV2BankAccountDetails.class,
                (node, p) ->
                    node.has("bankAccountGUID")
                        ? BankAccountProviderOfficeLinkV2.class
                        : BankAccountProviderOfficeCreateV2.class))
        .addDeserializer(
            OfficePatchV2.class,
            new TypeResolvingDeserializer<>(
                OfficePatchV2.class,
                (node, p) -> {
                  rejectForbiddenFields(node, p, "officeGUID", "providerFirmGUID", "accountNumber");
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
   * resolver, then deserializes into the resolved subtype. The resolver may throw a {@link
   * JacksonException} to reject conflicting field combinations before type resolution completes.
   */
  static final class TypeResolvingDeserializer<T> extends StdDeserializer<T> {

    @FunctionalInterface
    interface TypeResolver<T> {
      Class<? extends T> resolve(JsonNode node, JsonParser p) throws JacksonException;
    }

    private final TypeResolver<T> typeResolver;

    TypeResolvingDeserializer(Class<T> vc, TypeResolver<T> typeResolver) {
      super(vc);
      this.typeResolver = typeResolver;
    }

    @Override
    public T deserialize(JsonParser p, DeserializationContext ctx) throws JacksonException {
      JsonNode node = p.readValueAsTree();
      return ctx.readTreeAsValue(node, typeResolver.resolve(node, p));
    }
  }

  /**
   * Throws a {@link MismatchedInputException} if the JSON node contains any of {@code fields}. Used
   * to enforce read-only field restrictions before type resolution.
   */
  private static void rejectForbiddenFields(JsonNode node, JsonParser p, String... fields)
      throws JacksonException {
    for (String field : fields) {
      if (node.has(field)) {
        throw MismatchedInputException.from(
            p, Object.class, String.format("Field '%s' must not be provided", field));
      }
    }
  }

  /**
   * Throws a {@link MismatchedInputException} if the JSON node contains any of {@code
   * conflictingFields}. The {@code discriminator} names the field whose presence selected the
   * current type; it appears in the error message to explain what the conflicting field cannot be
   * combined with.
   */
  private static void rejectConflictingFields(
      JsonNode node, JsonParser p, String discriminator, String... conflictingFields)
      throws JacksonException {
    for (String field : conflictingFields) {
      if (node.has(field)) {
        throw MismatchedInputException.from(
            p,
            Object.class,
            "Field '" + field + "' cannot be combined with '" + discriminator + "'");
      }
    }
  }
}
