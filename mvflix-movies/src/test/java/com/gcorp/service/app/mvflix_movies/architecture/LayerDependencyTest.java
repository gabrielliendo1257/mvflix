package com.gcorp.service.app.mvflix_movies.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import org.springframework.web.bind.annotation.RestController;

@AnalyzeClasses(
        packages = "com.gcorp.service.app.mvflix_movies",
        importOptions = ImportOption.DoNotIncludeTests.class)
class LayerDependencyTest {

    @ArchTest
    static final ArchRule domain_does_not_depend_on_outer_layers = noClasses()
            .that()
            .resideInAPackage("..domain..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage(
                    "..application..",
                    "..infrastructure..",
                    "..presenter..",
                    "..advisors..",
                    "org.springframework..")
            .because("el dominio debe permanecer independiente de Spring y sus adapters");

    @ArchTest
    static final ArchRule application_does_not_depend_on_adapters = noClasses()
            .that()
            .resideInAPackage("..application..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage("..infrastructure..", "..presenter..", "..advisors..")
            .because("Application coordina dominio y puertos, no implementaciones externas");

    @ArchTest
    static final ArchRule shared_security_contract_does_not_depend_on_spring = noClasses()
            .that()
            .resideInAPackage("..shared.application.security..")
            .should()
            .dependOnClassesThat()
            .resideInAPackage("org.springframework..")
            .because("el contrato de usuario actual debe permanecer independiente de Spring Security");

    @ArchTest
    static final ArchRule controllers_do_not_depend_on_repositories = noClasses()
            .that()
            .areAnnotatedWith(RestController.class)
            .should()
            .dependOnClassesThat()
            .haveSimpleNameEndingWith("Repository")
            .because("los controllers deben invocar casos de uso o queries de Application");

    @ArchTest
    static final ArchRule library_adapters_are_internal = noClasses()
            .that()
            .resideOutsideOfPackage("..library..")
            .should()
            .dependOnClassesThat()
            .resideInAPackage("..library.infrastructure..")
            .because("otros módulos solo deben consumir dominio o contratos de Library");

    @ArchTest
    static final ArchRule catalog_adapters_are_internal = noClasses()
            .that()
            .resideOutsideOfPackage("..catalog..")
            .should()
            .dependOnClassesThat()
            .resideInAPackage("..catalog.infrastructure..")
            .because("otros módulos solo deben consumir dominio o capacidades de Catalog");

    @ArchTest
    static final ArchRule library_application_uses_catalog_ports = noClasses()
            .that()
            .resideInAPackage("..library.application..")
            .should()
            .dependOnClassesThat()
            .resideInAPackage("..catalog.application..")
            .because("Library debe expresar sus necesidades de Catalog mediante sus propios puertos");

    @ArchTest
    static final ArchRule catalog_domain_is_independent_from_library = noClasses()
            .that()
            .resideInAPackage("..catalog.domain..")
            .should()
            .dependOnClassesThat()
            .resideInAPackage("..library..")
            .because("las invariantes de Catalog no deben depender del módulo Library");

    @ArchTest
    static final ArchRule library_domain_is_independent_from_catalog = noClasses()
            .that()
            .resideInAPackage("..library.domain..")
            .should()
            .dependOnClassesThat()
            .resideInAPackage("..catalog..")
            .because("Library debe modelar localmente sus referencias a Catalog");

    @ArchTest
    static final ArchRule catalog_bulk_uses_its_library_port = noClasses()
            .that()
            .haveSimpleName("BulkVisibilityUseCase")
            .should()
            .dependOnClassesThat()
            .resideInAPackage("..library..")
            .because("Catalog debe resolver bibliotecas mediante un puerto definido por Catalog");

    @ArchTest
    static final ArchRule catalog_delete_uses_its_library_port = noClasses()
            .that()
            .haveSimpleName("DeleteMovieUseCase")
            .should()
            .dependOnClassesThat()
            .resideInAPackage("..library..")
            .because("Catalog debe desvincular assets mediante un puerto definido por Catalog");
}
