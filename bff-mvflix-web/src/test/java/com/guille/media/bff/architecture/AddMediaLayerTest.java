package com.guille.media.bff.architecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

/**
 * Fronteras internas del contexto Add Media en el BFF:
 * application no conoce HTTP ni la capa web; model es puro; web solo
 * habla con application.
 */
@AnalyzeClasses(packages = "com.guille.media.bff.experience.addmedia", importOptions = ImportOption.DoNotIncludeTests.class)
class AddMediaLayerTest {

  @ArchTest
  static final ArchRule application_is_http_free =
      noClasses()
          .that()
          .resideInAPackage("..addmedia.application..")
          .should()
          .dependOnClassesThat()
          .resideInAnyPackage(
              "..addmedia.web..",
              "org.springframework.http..",
              "org.springframework.web..");

  @ArchTest
  static final ArchRule application_does_not_carry_swagger =
      noClasses()
          .that()
          .resideInAPackage("..addmedia.application..")
          .should()
          .dependOnClassesThat()
          .resideInAPackage("io.swagger..");

  @ArchTest
  static final ArchRule model_is_pure =
      noClasses()
          .that()
          .resideInAPackage("..addmedia.model..")
          .should()
          .dependOnClassesThat()
          .resideInAnyPackage(
              "..addmedia.application..",
              "..addmedia.infrastructure..",
              "..addmedia.web..",
              "org.springframework..",
              "io.swagger..");

  @ArchTest
  static final ArchRule web_only_speaks_to_application =
      classes()
          .that()
          .resideInAPackage("..addmedia.web..")
          .should()
          .onlyDependOnClassesThat()
          .resideInAnyPackage(
              "java..", "com.guille.media.bff..",
              "org.springframework..", "jakarta.validation..", "org.hibernate.validator..",
              "io.swagger.v3.oas.annotations..",
              "reactor.core..", "lombok..");
}
