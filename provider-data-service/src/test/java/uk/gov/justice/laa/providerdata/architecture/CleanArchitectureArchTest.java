package uk.gov.justice.laa.providerdata.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/**
 * Architectural constraints for the clean architecture migration.
 *
 * <p>Rules are configured to allow empty packages so they can be introduced incrementally.
 */
@AnalyzeClasses(packages = "uk.gov.justice.laa.providerdata")
class CleanArchitectureArchTest {

  private static final String BASE = "uk.gov.justice.laa.providerdata";

  @ArchTest
  static final ArchRule domainShouldOnlyDependOnDomainAndJdk =
      classes()
          .that()
          .resideInAPackage(BASE + ".domain..")
          .should()
          .onlyDependOnClassesThat()
          .resideInAnyPackage("java..", "jakarta..", "lombok..", BASE + ".domain..")
          .allowEmptyShould(true);

  @ArchTest
  static final ArchRule applicationShouldNotDependOnAdaptersOrInfrastructure =
      noClasses()
          .that()
          .resideInAPackage(BASE + ".application..")
          .should()
          .dependOnClassesThat()
          .resideInAnyPackage(
              BASE + ".adapters..",
              BASE + ".infrastructure..",
              "org.springframework.web..",
              "org.springframework.data..",
              "jakarta.persistence..")
          .allowEmptyShould(true);

  @ArchTest
  static final ArchRule inboundAdaptersShouldNotDependOnOutboundAdapters =
      noClasses()
          .that()
          .resideInAPackage(BASE + ".adapters.in..")
          .should()
          .dependOnClassesThat()
          .resideInAPackage(BASE + ".adapters.out..")
          .allowEmptyShould(true);

  @ArchTest
  static final ArchRule coreShouldNotDependOnInfrastructure =
      noClasses()
          .that()
          .resideInAnyPackage(BASE + ".domain..", BASE + ".application..")
          .should()
          .dependOnClassesThat()
          .resideInAPackage(BASE + ".infrastructure..")
          .allowEmptyShould(true);
}

