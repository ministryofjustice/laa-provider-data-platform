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
 * Marks an e2e test class or method as data-modifying (i.e. it creates, modifies, or deletes data).
 *
 * <p>Modifying tests may only run against the {@code local} environment. The Gradle {@code
 * e2eModifying} task enforces this at runtime by failing the build for any other environment.
 *
 * <p>Also applies {@link E2eRestAssuredExtension} to configure RestAssured for the test class.
 *
 * @see ReadOnlyTest
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
@Tag("modifying")
@ExtendWith({E2eDatabaseExtension.class, E2eRestAssuredExtension.class})
public @interface ModifyingTest {}
