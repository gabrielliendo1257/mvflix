package com.guille.media.bff.architecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

/**
 * Fronteras internas de la experiencia Playback en el BFF: application no
 * conoce HTTP ni la capa web; web solo habla con application.
 */
@AnalyzeClasses(packages = "com.guille.media.bff.experience.playback", importOptions = ImportOption.DoNotIncludeTests.class)
class PlaybackLayerTest {

  @ArchTest
  static final ArchRule application_is_http_free =
      noClasses()
          .that()
          .resideInAPackage("..playback.application..")
          .should()
          .dependOnClassesThat()
          .resideInAnyPackage(
              "..playback.web..",
              "..playback.infrastructure..",
              "org.springframework.http..",
              "org.springframework.web..");

  @ArchTest
  static final ArchRule application_does_not_carry_swagger =
      noClasses()
          .that()
          .resideInAPackage("..playback.application..")
          .should()
          .dependOnClassesThat()
          .resideInAPackage("io.swagger..");

  @ArchTest
  static final ArchRule web_only_speaks_to_application =
      classes()
          .that()
          .resideInAPackage("..playback.web..")
          .should()
          .onlyDependOnClassesThat()
          .resideInAnyPackage(
              "java..", "com.guille.media.bff..",
              "org.springframework..", "io.swagger.v3.oas.annotations..",
              "reactor.core..", "lombok..");
}
