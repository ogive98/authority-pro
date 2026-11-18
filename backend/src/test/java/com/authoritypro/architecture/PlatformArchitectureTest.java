package com.authoritypro.architecture;

import com.authoritypro.platform.AuthorityProApplication;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.Test;
import org.springframework.modulith.ApplicationModule;
import org.springframework.modulith.NamedInterface;
import org.springframework.modulith.core.ApplicationModules;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlatformArchitectureTest {
    private static final String BASE = "com.authoritypro.platform";

    @Test
    void discoversExactlyFiveClosedRootsThenVerifiesTheEmptyGraph() throws ClassNotFoundException {
        // Do not replace discovery with a manually constructed module list or fixture scan.
        var modules = ApplicationModules.of(AuthorityProApplication.class);
        BoundaryRules.assertExactModules(modules, BASE, BoundaryRules.L02_ROOTS);

        for (var module : modules) {
            var metadataClass = Class.forName(module.getBasePackage().getName() + ".package-info",
                    false, AuthorityProApplication.class.getClassLoader());
            var declaration = metadataClass.getPackage().getAnnotation(ApplicationModule.class);
            assertNotNull(declaration, "Missing compiled package metadata for " + module.getIdentifier());
            assertEquals(ApplicationModule.Type.CLOSED, declaration.type());
            // The annotation default is OPEN_TOKEN, NOT an empty array. Explicit {} denies all edges.
            assertEquals(0, declaration.allowedDependencies().length,
                    "L02 must explicitly deny all intermodule dependencies");
            assertFalse(module.isOpen(), "Open module: " + module.getIdentifier());
            assertTrue(module.getAllowedDependencies(modules).isEmpty(),
                    "Unexpected allowed dependency: " + module.getIdentifier());
            assertFalse(module.getNamedInterfaces().hasExplicitInterfaces(),
                    "L02 has no approved named exports: " + module.getIdentifier());
            assertTrue(module.getNamedInterfaces().getUnnamedInterface().asJavaClasses().findAny().isEmpty(),
                    "Do not publish implicit root-package APIs: " + module.getIdentifier());
        }

        BoundaryRules.assertEmptyGraph(modules);
        modules.verify();
    }

    @Test
    void bytecodeBoundariesHaveNoCrossRootEdgesOrPrivateLayerLeaks() {
        var classes = new ClassFileImporter().withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages(BASE);
        assertTrue(classes.stream().anyMatch(type -> type.isEquivalentTo(AuthorityProApplication.class)),
                "Production bytecode scan did not import the application");
        for (String root : BoundaryRules.L02_ROOTS) {
            assertTrue(classes.stream().anyMatch(type -> type.getName().equals(BASE + "." + root + ".package-info")),
                    "Production bytecode scan omitted package metadata: " + root);
        }
        assertFalse(classes.stream().anyMatch(type -> type.isAnnotatedWith(NamedInterface.class)),
                "L02 has no approved production named exports");

        BoundaryRules.onlyImmediateRoots(BASE, BoundaryRules.L02_ROOTS,
                AuthorityProApplication.class.getName()).check(classes);
        BoundaryRules.noCrossRootDependencies(BASE).check(classes);
        BoundaryRules.privatePackagesStayOwned(BASE).check(classes);
        BoundaryRules.layersPointInward(BASE).check(classes);
    }
}
