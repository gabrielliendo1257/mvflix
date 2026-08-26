package com.guille.media.bff.architecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

/**
 * Fronteras internas de la experiencia Catalog en el BFF: application no
 * conoce HTTP ni la capa web; web solo habla con application.
 */
@AnalyzeClasses(packages = "com.guille.media.bff.experience.catalog", importOptions = ImportOption.DoNotIncludeTests.class)
class CatalogLayerTest {

  @ArchTest
  static final ArchRule application_is_http_free =
      noClasses()
          .that()
          .resideInAPackage("..catalog.application..")
          .should()
          .dependOnClassesThat()
          .resideInAnyPackage(
              "..catalog.web..",
              "..catalog.infrastructure..",
              "org.springframework.http..",
              "org.springframework.web..");

  @ArchTest
  static final ArchRule application_does_not_carry_swagger =
      noClasses()
          .that()
          .resideInAPackage("..catalog.application..")
          .should()
          .dependOnClassesThat()
          .resideInAPackage("io.swagger..");

  /** Application habla con puertos propios, no con los globales legacy. */
  @ArchTest
  static final ArchRule application_does_not_use_legacy_global_ports =
      noClasses()
          .that()
          .resideInAPackage("..catalog.application..")
          .should()
          .dependOnClassesThat()
          .resideInAPackage("com.guille.media.bff.app.ports..");

  /** El wire (Jackson) es infraestructura: application queda puro. */
  @ArchTest
  static final ArchRule application_is_jackson_free =
      noClasses()
          .that()
          .resideInAPackage("..catalog.application..")
          .should()
          .dependOnClassesThat()
          .resideInAnyPackage("com.fasterxml.jackson..");

  /** Application y web no saltan a servicios legacy: solo el adapter de infra lo hace. */
  @ArchTest
  static final ArchRule catalog_does_not_touch_legacy_services =
      noClasses()
          .that()
          .resideInAnyPackage("..experience.catalog.application..", "..experience.catalog.web..")
          .should()
          .dependOnClassesThat()
          .resideInAnyPackage(
              "com.guille.media.bff.app.service..",
              "com.guille.media.bff.presenter..");

  @ArchTest
  static final ArchRule web_only_speaks_to_application =
      classes()
          .that()
          .resideInAPackage("..catalog.web..")
          .should()
          .onlyDependOnClassesThat()
          .resideInAnyPackage(
              "java..", "com.guille.media.bff..",
              "org.springframework..", "io.swagger.v3.oas.annotations..",
              "reactor.core..", "lombok..");
}
