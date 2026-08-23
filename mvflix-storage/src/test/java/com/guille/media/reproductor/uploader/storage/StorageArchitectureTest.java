package com.guille.media.reproductor.uploader.storage;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/**
 * Fronteras arquitectónicas del módulo storage. Reglas mínimas que protegen lo
 * ya corregido; se amplían con cada movimiento estructural.
 */
@AnalyzeClasses(packages = "com.guille.media.reproductor.uploader.storage", importOptions = ImportOption.DoNotIncludeTests.class)
class StorageArchitectureTest {

  @ArchTest
  static final ArchRule domain_does_not_depend_on_outer_layers =
      noClasses()
          .that()
          .resideInAPackage("..storage.domain..")
          .should()
          .dependOnClassesThat()
          .resideInAnyPackage(
              "..storage.app..", "..storage.infrastructure..", "..storage.presenter..");

  @ArchTest
  static final ArchRule domain_is_spring_free =
      noClasses().that().resideInAPackage("..storage.domain..").should().dependOnClassesThat()
          .resideInAnyPackage("org.springframework..");
  // Mono/Flux sí están permitidos en domain: los puertos exponen contratos
  // reactivos porque TODO el servicio es reactivo (WebFlux + R2DBC). Esa es la
  // razón arquitectónica concreta; si el dominio empezara a depender de
  // anotaciones o infraestructura de Spring, esta regla lo detecta.

  @ArchTest
  static final ArchRule application_does_not_depend_on_infrastructure_web_or_presenter =
      noClasses()
          .that()
          .resideInAPackage("..storage.app..")
          .should()
          .dependOnClassesThat()
          .resideInAnyPackage(
              "..storage.presenter..", "..storage.infrastructure.http..",
              "..storage.infrastructure.security..");

  @ArchTest
  static final ArchRule bounded_contexts_are_acyclic =
      slices().matching("com.guille.media.reproductor.uploader.storage.(*)..")
          .should().beFreeOfCycles();
}
