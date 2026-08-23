package com.guille.media.reproductor.uploader.storage;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/**
 * Fronteras del módulo storage tras la modularización en bounded contexts:
 *
 * <ul>
 *   <li>{@code managedstorage} - sesiones de upload, cuota, objetos MinIO;</li>
 *   <li>{@code library} - bibliotecas locales, escaneo y serving;</li>
 *   <li>{@code shared} - seguridad e errores transversales (hoja).</li>
 * </ul>
 */
@AnalyzeClasses(packages = "com.guille.media.reproductor.uploader.storage", importOptions = ImportOption.DoNotIncludeTests.class)
class StorageArchitectureTest {

  @ArchTest
  static final ArchRule managedstorage_domain_is_pure =
      noClasses()
          .that()
          .resideInAPackage("..storage.managedstorage.domain..")
          .should()
          .dependOnClassesThat()
          .resideInAnyPackage(
              "..storage.managedstorage.application..",
              "..storage.managedstorage.infrastructure..",
              "..storage.library..",
              "..storage.shared..",
              "org.springframework..");
  // Mono/Flux sí están permitidos en domain: los puertos exponen contratos
  // reactivos porque TODO el servicio es reactivo (WebFlux + R2DBC).

  @ArchTest
  static final ArchRule managedstorage_application_does_not_reach_infrastructure_or_library =
      noClasses()
          .that()
          .resideInAPackage("..storage.managedstorage.application..")
          .should()
          .dependOnClassesThat()
          .resideInAnyPackage("..storage.managedstorage.infrastructure..", "..storage.library..");

  @ArchTest
  static final ArchRule library_is_self_contained =
      noClasses()
          .that()
          .resideInAPackage("..storage.library..")
          .should()
          .dependOnClassesThat()
          .resideInAPackage("..storage.managedstorage..");

  @ArchTest
  static final ArchRule managedstorage_does_not_reach_into_library =
      noClasses()
          .that()
          .resideInAPackage("..storage.managedstorage..")
          .should()
          .dependOnClassesThat()
          .resideInAPackage("..storage.library..");

  @ArchTest
  static final ArchRule shared_is_a_leaf =
      noClasses()
          .that()
          .resideInAPackage("..storage.shared..")
          .should()
          .dependOnClassesThat()
          .resideInAnyPackage("..storage.managedstorage..", "..storage.library..");

  @ArchTest
  static final ArchRule bounded_contexts_are_acyclic =
      slices().matching("com.guille.media.reproductor.uploader.storage.(*)..")
          .should().beFreeOfCycles();
}
