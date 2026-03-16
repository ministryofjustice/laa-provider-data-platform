package uk.gov.justice.laa.providerdata.e2e;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Marks an e2e test class or method as read-only (i.e. it only reads data, never modifies it).
 *
 * <p>Read-only tests are safe to run against any environment, including production.
 *
 * <p>Also applies {@link E2eRestAssuredExtension} to configure RestAssured for the test class.
 *
 * @see DestructiveTest
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
@Tag("read-only")
@ExtendWith(E2eRestAssuredExtension.class)
public @interface ReadOnlyTest {}
