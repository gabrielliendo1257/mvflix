package com.guille.media.bff.architecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

/**
 * Fronteras internas de la experiencia Media Detail en el BFF: application
 * puro (sin HTTP/Jackson/puertos legacy), web solo habla con application,
 * infraestructura es el único punto con wire details.
 */
@AnalyzeClasses(packages = "com.guille.media.bff.experience.media", importOptions = ImportOption.DoNotIncludeTests.class)
class MediaLayerTest {

  @ArchTest
  static final ArchRule application_is_http_free =
      noClasses()
          .that()
          .resideInAPackage("..media.application..")
          .should()
          .dependOnClassesThat()
          .resideInAnyPackage(
              "..media.web..",
              "..media.infrastructure..",
              "org.springframework.http..",
              "org.springframework.web..");

  @ArchTest
  static final ArchRule application_does_not_carry_swagger =
      noClasses()
          .that()
          .resideInAPackage("..media.application..")
          .should()
          .dependOnClassesThat()
          .resideInAPackage("io.swagger..");

  @ArchTest
  static final ArchRule application_is_jackson_free =
      noClasses()
          .that()
          .resideInAPackage("..media.application..")
          .should()
          .dependOnClassesThat()
          .resideInAnyPackage("com.fasterxml.jackson..");

  @ArchTest
  static final ArchRule application_does_not_use_legacy_global_ports =
      noClasses()
          .that()
          .resideInAPackage("..media.application..")
          .should()
          .dependOnClassesThat()
          .resideInAPackage("com.guille.media.bff.app.ports..");

  @ArchTest
  static final ArchRule media_slice_does_not_touch_legacy_services =
      noClasses()
          .that()
          .resideInAnyPackage("..media.application..", "..media.web..")
          .should()
          .dependOnClassesThat()
          .resideInAnyPackage(
              "com.guille.media.bff.app.service..",
              "com.guille.media.bff.presenter..");

  @ArchTest
  static final ArchRule web_only_speaks_to_application =
      classes()
          .that()
          .resideInAPackage("..media.web..")
          .should()
          .onlyDependOnClassesThat()
          .resideInAnyPackage(
              "java..", "com.guille.media.bff..",
              "org.springframework..", "io.swagger.v3.oas.annotations..",
              "reactor.core..", "lombok..");
}
