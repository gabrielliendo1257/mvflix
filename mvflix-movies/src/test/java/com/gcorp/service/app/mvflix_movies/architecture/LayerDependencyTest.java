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
    static final ArchRule controllers_do_not_depend_on_repositories = noClasses()
            .that()
            .areAnnotatedWith(RestController.class)
            .should()
            .dependOnClassesThat()
            .haveSimpleNameEndingWith("Repository")
            .because("los controllers deben invocar casos de uso o queries de Application");
}
