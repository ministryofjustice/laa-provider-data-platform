package uk.gov.justice.laa.providerdata.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a generated model class as rejecting specific JSON properties that must not appear in a
 * request body (e.g. read-only or system-managed fields).
 *
 * <p>On its own this annotation has no runtime effect. It is read by
 * {@code JacksonConfig.rejectPropertiesModule()} which registers a Jackson
 * {@code DeserializationProblemHandler} that throws on any listed field when deserialising into an
 * annotated class, causing Spring Boot to return a 400 Bad Request.
 *
 * <p>Fields not listed in {@link #value()} are unaffected — they are either accepted or silently
 * ignored according to the normal Jackson deserialization rules.
 *
 * <h2>Usage in the OpenAPI spec</h2>
 * Add the following vendor extension to any schema that should reject specific fields:
 * <pre>{@code
 * MyPatchSchema:
 *   type: object
 *   x-class-extra-annotation: >-
 *     @uk.gov.justice.laa.providerdata.annotation.RejectProperties(
 *     {"fieldOne", "fieldTwo"})
 * }</pre>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface RejectProperties {

  /**
   * Names of JSON properties that must not be present in a request body targeting this class.
   * Any request containing one of these fields will result in a 400 Bad Request response.
   */
  String[] value();
}
