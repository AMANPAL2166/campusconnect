package com.campusconnect.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

/** Phase D rules, executable. If a rule breaks, the BUILD fails. */
class ModuleBoundaryTest {

    private static final String BASE = "com.campusconnect";
    private static final List<String> MODULES = List.of("auth", "profile");   // add each new module here
    private static final List<String> INTERNAL_LAYERS =
            List.of("web", "service", "repository", "domain", "security", "config");

    private static JavaClasses classes;

    @BeforeAll
    static void load() {
        classes = new ClassFileImporter().withImportOption(new ImportOption.DoNotIncludeTests()).importPackages(BASE);
    }

    @Test
    void controllersNeverTouchRepositories() {
        noClasses().that().resideInAPackage("..web..")
                .should().dependOnClassesThat().resideInAPackage("..repository..")
                .because("controllers must go through services").check(classes);
    }

    @Test
    void controllersNeverUseEntities() {
        noClasses().that().resideInAPackage("..web..")
                .should().dependOnClassesThat().resideInAPackage("..domain..")
                .because("the API speaks DTOs, never entities").check(classes);
    }

    @Test
    void domainDoesNotDependOnWebOrService() {
        noClasses().that().resideInAPackage("..domain..")
                .should().dependOnClassesThat().resideInAnyPackage("..web..", "..service..")
                .check(classes);
    }

    @Test
    void modulesHaveNoCycles() {
        slices().matching(BASE + ".(*)..").should().beFreeOfCycles().check(classes);
    }

    @Test
    void otherModulesAreReachedOnlyThroughTheirApiPackage() {
        for (String from : MODULES) {
            for (String to : MODULES) {
                if (from.equals(to)) continue;
                String[] internals = INTERNAL_LAYERS.stream()
                        .map(layer -> BASE + "." + to + "." + layer + "..").toArray(String[]::new);
                noClasses().that().resideInAPackage(BASE + "." + from + "..")
                        .should().dependOnClassesThat().resideInAnyPackage(internals)
                        .because(from + " may use " + to + " only via " + to + ".api")
                        .check(classes);
            }
        }
    }

    @Test
    void commonDependsOnNoModule() {
        String[] modules = MODULES.stream().map(m -> BASE + "." + m + "..").toArray(String[]::new);
        noClasses().that().resideInAPackage(BASE + ".common..")
                .should().dependOnClassesThat().resideInAnyPackage(modules).check(classes);
    }
}