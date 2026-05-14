package uk.gov.justice.laa.providerdata;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaField;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import org.mapstruct.Mapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.repository.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RestController;

@AnalyzeClasses(
    packages = "uk.gov.justice.laa.providerdata",
    importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

  /** Services must not depend on web controllers. */
  @ArchTest
  static final ArchRule services_do_not_depend_on_web =
      noClasses()
          .that()
          .resideInAPackage("..service..")
          .should()
          .dependOnClassesThat()
          .resideInAPackage("..web..");

  /** Web controllers must not depend on repositories directly. */
  @ArchTest
  static final ArchRule web_do_not_depend_on_repositories =
      noClasses()
          .that()
          .resideInAPackage("..web..")
          .should()
          .dependOnClassesThat()
          .resideInAPackage("..repository..")
          .because("web-layer classes must go via a service, not access repositories directly");

  /** REST controllers must reside in a {@code ..web..} sub-package. */
  @ArchTest
  static final ArchRule rest_controllers_reside_in_web_packages =
      classes()
          .that()
          .areAnnotatedWith(RestController.class)
          .should()
          .resideInAPackage("..web..")
          .because("REST controllers must live in a ..web.. sub-package");

  /** Spring Data repositories must reside in a {@code ..repository..} sub-package. */
  @ArchTest
  static final ArchRule repositories_reside_in_repository_packages =
      classes()
          .that()
          .areAssignableTo(Repository.class)
          .should()
          .resideInAPackage("..repository..")
          .because("Spring Data repositories must live in a ..repository.. sub-package");

  /** No production class may use {@code @Autowired} field injection; use constructor injection. */
  @ArchTest static final ArchRule no_field_injection = noClasses().should(haveAutowiredFields());

  /** Mappers must reside in a package named 'mapper'. */
  @ArchTest
  static final ArchRule mappers_reside_in_mapper_package =
      classes().that().areAnnotatedWith(Mapper.class).should().resideInAPackage("..mapper..");

  /**
   * Query services must declare {@code @Transactional(readOnly = true)} at the class level.
   * Enforced after Phase 2 renames; has no effect until classes named {@code *QueryService} exist.
   */
  @ArchTest
  static final ArchRule query_services_are_read_only =
      classes()
          .that()
          .haveSimpleNameEndingWith("QueryService")
          .should()
          .beAnnotatedWith(Transactional.class)
          .andShould(haveReadOnlyTransactional())
          .because(
              "QueryService classes must be @Transactional(readOnly = true) at the class level")
          .allowEmptyShould(true);

  /**
   * Command services must not declare {@code @Transactional(readOnly = true)} at the class level.
   * Enforced after Phase 2 renames; has no effect until classes named {@code *CommandService}
   * exist.
   */
  @ArchTest
  static final ArchRule command_services_are_not_read_only =
      classes()
          .that()
          .haveSimpleNameEndingWith("CommandService")
          .should(notHaveReadOnlyTransactional())
          .because(
              "CommandService classes must not be @Transactional(readOnly = true) at the class"
                  + " level")
          .allowEmptyShould(true);

  /**
   * A class annotated {@code @Transactional(readOnly = true)} must not contain methods that
   * override that with a write transaction.
   */
  @ArchTest
  static final ArchRule no_write_transaction_in_readonly_class =
      classes()
          .should(noWriteTransactionMethods())
          .because(
              "write operations must not override a class-level @Transactional(readOnly = true);"
                  + " extract them to a command service");

  /**
   * Classes annotated {@code @Transactional(readOnly = true)} must not call any {@code save*} or
   * {@code delete*} method on a Spring Data repository.
   */
  @ArchTest
  static final ArchRule readonly_classes_must_not_call_repository_mutations =
      classes()
          .that(isAnnotatedWithReadOnlyTransactional())
          .should(notCallRepositoryMutationMethods())
          .because(
              "classes annotated @Transactional(readOnly = true) must not call repository"
                  + " save* or delete* methods; extract writes to a CommandService");

  private static DescribedPredicate<JavaClass> isAnnotatedWithReadOnlyTransactional() {
    return new DescribedPredicate<>("annotated @Transactional(readOnly = true)") {
      @Override
      public boolean test(JavaClass clazz) {
        return clazz.isAnnotatedWith(Transactional.class)
            && clazz.getAnnotationOfType(Transactional.class).readOnly();
      }
    };
  }

  private static ArchCondition<JavaClass> notCallRepositoryMutationMethods() {
    return new ArchCondition<>("not call repository save* or delete* methods") {
      @Override
      public void check(JavaClass clazz, ConditionEvents events) {
        clazz.getMethodCallsFromSelf().stream()
            .filter(
                call -> {
                  String name = call.getTarget().getName();
                  return name.startsWith("save") || name.startsWith("delete");
                })
            .filter(call -> call.getTargetOwner().isAssignableTo(Repository.class))
            .forEach(
                call ->
                    events.add(
                        SimpleConditionEvent.violated(
                            clazz,
                            call.getDescription()
                                + " calls a repository mutation method from a"
                                + " @Transactional(readOnly = true) class")));
      }
    };
  }

  private static ArchCondition<JavaClass> haveAutowiredFields() {
    return new ArchCondition<>("not have @Autowired field injection") {
      @Override
      public void check(JavaClass clazz, ConditionEvents events) {
        for (JavaField field : clazz.getFields()) {
          if (field.isAnnotatedWith(Autowired.class)) {
            events.add(
                SimpleConditionEvent.violated(
                    clazz,
                    field.getDescription()
                        + " uses @Autowired field injection; use constructor injection instead"));
          }
        }
      }
    };
  }

  private static ArchCondition<JavaClass> haveReadOnlyTransactional() {
    return new ArchCondition<>("be annotated @Transactional(readOnly = true)") {
      @Override
      public void check(JavaClass clazz, ConditionEvents events) {
        boolean satisfied =
            clazz.isAnnotatedWith(Transactional.class)
                && clazz.getAnnotationOfType(Transactional.class).readOnly();
        if (!satisfied) {
          events.add(
              SimpleConditionEvent.violated(
                  clazz, clazz.getName() + " is not annotated @Transactional(readOnly = true)"));
        }
      }
    };
  }

  private static ArchCondition<JavaClass> notHaveReadOnlyTransactional() {
    return new ArchCondition<>("not be annotated @Transactional(readOnly = true)") {
      @Override
      public void check(JavaClass clazz, ConditionEvents events) {
        boolean violated =
            clazz.isAnnotatedWith(Transactional.class)
                && clazz.getAnnotationOfType(Transactional.class).readOnly();
        if (violated) {
          events.add(
              SimpleConditionEvent.violated(
                  clazz,
                  clazz.getName()
                      + " is annotated @Transactional(readOnly = true) but must not"
                      + " be"));
        }
      }
    };
  }

  private static ArchCondition<JavaClass> noWriteTransactionMethods() {
    return new ArchCondition<>(
        "not override @Transactional(readOnly = true) with write-transaction methods") {
      @Override
      public void check(JavaClass clazz, ConditionEvents events) {
        if (!clazz.isAnnotatedWith(Transactional.class)
            || !clazz.getAnnotationOfType(Transactional.class).readOnly()) {
          return;
        }
        for (JavaMethod method : clazz.getMethods()) {
          if (method.isAnnotatedWith(Transactional.class)
              && !method.getAnnotationOfType(Transactional.class).readOnly()) {
            events.add(
                SimpleConditionEvent.violated(
                    method,
                    method.getDescription()
                        + " overrides @Transactional(readOnly = true) with a write transaction"));
          }
        }
      }
    };
  }
}
