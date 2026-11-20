package com.authoritypro.architecture;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import com.authoritypro.boundaryfixtures.layers.identity.application.UseDomain;
import com.authoritypro.boundaryfixtures.layers.identity.domain.PureValue;
import com.authoritypro.boundaryfixtures.layers.identity.infrastructure.Adapter;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.Test;
import org.springframework.modulith.ApplicationModule;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.core.Violations;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BoundaryRulesTest {
    private static final String FIXTURES = "com.authoritypro.boundaryfixtures.";
    private static final Set<String> PAIR = Set.of("consumer", "producer");
    // Explicitly include test bytecode, but only inside each isolated fixture base package.
    private static final ImportOption INCLUDE_FIXTURES = location -> true;

    @Test
    void annotationDefaultIsNotAnEmptyDependencyDeclaration() throws NoSuchMethodException {
        assertArrayEquals(new String[] {ApplicationModule.OPEN_TOKEN},
                (String[]) ApplicationModule.class.getMethod("allowedDependencies").getDefaultValue());
    }

    @Test
    void annotatedPackageInfoAloneMustBeDiscoverableWithoutMarkerServices() {
        var fixture = fixture("packageonly", Set.of("alpha", "beta"),
                "alpha.package-info", "beta.package-info");
        assertEquals(Set.of(fixture.base() + ".alpha.package-info", fixture.base() + ".beta.package-info"),
                fixture.classes().stream().map(type -> type.getName()).collect(Collectors.toSet()),
                "Package-only discovery probe must contain no helper classes");
        BoundaryRules.assertEmptyGraph(fixture.modules());
        fixture.modules().verify();
        BoundaryRules.noCrossRootDependencies(fixture.base()).check(fixture.classes());
    }

    @Test
    void explicitlyDeclaredNamedContractPassesItsIndependentAllowedGraph() {
        var fixture = fixture("allowed", PAIR, "consumer.UseContract", "producer.api.Contract");
        assertNamedContract(fixture, "producer", "producer.api.Contract");
        assertEdge(fixture, "consumer", "producer", "consumer.UseContract", "producer.api.Contract");
        assertGraph(fixture, Set.of("consumer -> producer"));
        fixture.modules().verify();
        BoundaryRules.privatePackagesStayOwned(fixture.base()).check(fixture.classes());
        BoundaryRules.layersPointInward(fixture.base()).check(fixture.classes());
    }

    @Test
    void emptyDeclarationRejectsAnOtherwiseExportedContract() {
        var fixture = fixture("undeclared", PAIR, "consumer.UseContract", "producer.api.Contract");
        assertNamedContract(fixture, "producer", "producer.api.Contract");
        assertEdge(fixture, "consumer", "producer", "consumer.UseContract", "producer.api.Contract");
        assertGraph(fixture, Set.of("consumer -> producer"));

        var failure = assertThrows(Violations.class, fixture.modules()::verify);
        assertViolation(failure, "Module 'consumer' depends on", "producer :: contract", "Allowed targets: none",
                fixture.base() + ".consumer.UseContract", fixture.base() + ".producer.api.Contract");
        assertArchFailure(BoundaryRules.noCrossRootDependencies(fixture.base()), fixture.classes(),
                "Forbidden intermodule edge consumer -> producer", "UseContract", "Contract");
    }

    @Test
    void aDeclaredConsumerStillCannotImportAnUnexportedPrivateType() {
        var fixture = fixture("privateimport", PAIR,
                "consumer.PrivateImport", "producer.internal.HiddenState", "producer.api.Contract");
        assertNamedContract(fixture, "producer", "producer.api.Contract");
        assertEdge(fixture, "consumer", "producer", "consumer.PrivateImport", "producer.internal.HiddenState");
        assertGraph(fixture, Set.of("consumer -> producer"));
        var producer = fixture.modules().getModuleByName("producer").orElseThrow();
        var hidden = producer.getType(fixture.base() + ".producer.internal.HiddenState").orElseThrow();
        assertFalse(producer.isExposed(hidden), "Private fixture accidentally became an export");

        var failure = assertThrows(Violations.class, fixture.modules()::verify);
        assertViolation(failure, "Module 'consumer' depends on non-exposed type",
                fixture.base() + ".producer.internal.HiddenState", "within module 'producer'");
        assertArchFailure(BoundaryRules.privatePackagesStayOwned(fixture.base()), fixture.classes(),
                "Cross-module private import consumer -> producer", "PrivateImport", "HiddenState");
    }

    @Test
    void aCycleFailsEvenWhenBothNamedContractsAndConsumersAreDeclared() {
        var fixture = fixture("cycle", Set.of("alpha", "beta"), "alpha.api.AlphaContract", "beta.api.BetaContract");
        assertNamedContract(fixture, "alpha", "alpha.api.AlphaContract");
        assertNamedContract(fixture, "beta", "beta.api.BetaContract");
        assertEdge(fixture, "alpha", "beta", "alpha.api.AlphaContract", "beta.api.BetaContract");
        assertEdge(fixture, "beta", "alpha", "beta.api.BetaContract", "alpha.api.AlphaContract");
        assertGraph(fixture, Set.of("alpha -> beta", "beta -> alpha"));
        // Prove failure is a graph cycle, not an undeclared dependency or a private import.
        fixture.modules().forEach(module -> module.verifyDependencies(fixture.modules()));
        var failure = assertThrows(Violations.class, fixture.modules()::verify);
        assertViolation(failure, "Cycle detected", "alpha", "beta", "AlphaContract", "BetaContract");
    }

    @Test
    void sameL02RulesRejectEvenTheSeparatelyApprovedFixtureEdge() {
        var fixture = fixture("allowed", PAIR, "consumer.UseContract", "producer.api.Contract");
        assertEdge(fixture, "consumer", "producer", "consumer.UseContract", "producer.api.Contract");
        assertArchFailure(BoundaryRules.noCrossRootDependencies(fixture.base()), fixture.classes(),
                "Forbidden intermodule edge consumer -> producer", "UseContract", "Contract");
        var graphFailure = assertThrows(AssertionError.class, () -> BoundaryRules.assertEmptyGraph(fixture.modules()));
        assertMessageContains(graphFailure.getMessage(), "Expected zero intermodule dependencies", "consumer -> producer");
    }

    @Test
    void nestedModuleCannotMasqueradeAsAnImmediateRoot() {
        String base = FIXTURES + "wrongnested";
        var classes = new ClassFileImporter().importPackages(base);
        assertImported(classes, base, "producer.nested.NestedType", "producer.nested.package-info");
        var modules = ApplicationModules.of(base, INCLUDE_FIXTURES);
        // First establish genuine nested discovery so the expected failure cannot be an empty scan.
        assertEquals(Set.of("producer", "nested"), modules.stream()
                .map(module -> module.getIdentifier().toString()).collect(Collectors.toSet()));
        var nested = modules.getModuleByName("nested").orElseThrow();
        assertEquals(base + ".producer.nested", nested.getBasePackage().getName());
        assertTrue(modules.hasParent(nested));
        BoundaryRules.assertEmptyGraph(modules);

        var failure = assertThrows(AssertionError.class,
                () -> BoundaryRules.assertExactModules(modules, base, Set.of("producer", "nested")));
        assertMessageContains(failure.getMessage(), "Not an immediate module root", base + ".producer.nested");
        assertArchFailure(BoundaryRules.onlyImmediateRoots(base, Set.of("producer", "nested"), ""), classes,
                "Not an immediate module root", base + ".producer.nested");
    }

    @Test
    void futureLayerRulesRejectFrameworkCouplingOutwardEdgesAndApiLeaks() {
        String base = FIXTURES + "layers";
        var classes = new ClassFileImporter().importPackages(base);
        assertImported(classes, base, "identity.domain.FrameworkCoupled", "identity.domain.OutwardDependency",
                "identity.application.WrongDirection", "identity.api.LeakyContract", "identity.infrastructure.Adapter");
        assertArchFailure(BoundaryRules.layersPointInward(base), classes,
                "Domain depends on framework", "FrameworkCoupled", "org.springframework.context.ApplicationContext",
                "java.sql.Connection", "javax.sql.DataSource", "com.zaxxer.hikari.HikariDataSource",
                "org.flywaydb.core.Flyway", "org.postgresql.ds.PGSimpleDataSource",
                "Domain points outward", "OutwardDependency", "Application depends on infrastructure", "WrongDirection",
                "API leaks private ownership", "LeakyContract");
    }

    @Test
    void futureLayerRulesAllowPureDomainAndInwardApplicationDependencies() {
        String base = FIXTURES + "layers";
        var classes = new ClassFileImporter().importClasses(PureValue.class, UseDomain.class, Adapter.class);
        assertEquals(Set.of(PureValue.class.getName(), UseDomain.class.getName(), Adapter.class.getName()),
                classes.stream().map(type -> type.getName()).collect(Collectors.toSet()));
        assertTrue(classes.stream().filter(type -> type.isEquivalentTo(UseDomain.class))
                .flatMap(type -> type.getDirectDependenciesFromSelf().stream())
                .anyMatch(edge -> edge.getTargetClass().isEquivalentTo(PureValue.class)),
                "Positive fixture must exercise a real application-to-domain dependency");
        BoundaryRules.layersPointInward(base).check(classes);
        BoundaryRules.privatePackagesStayOwned(base).check(classes);
        BoundaryRules.noCrossRootDependencies(base).check(classes);
    }

    private static Fixture fixture(String name, Set<String> expectedRoots, String... expectedTypes) {
        String base = FIXTURES + name;
        var classes = new ClassFileImporter().importPackages(base);
        assertImported(classes, base, expectedTypes);
        var modules = ApplicationModules.of(base, INCLUDE_FIXTURES);
        BoundaryRules.assertExactModules(modules, base, expectedRoots);
        modules.forEach(module -> assertFalse(module.isOpen(), "Fixture module must be closed"));
        return new Fixture(base, modules, classes);
    }

    private static void assertImported(JavaClasses classes, String base, String... expectedTypes) {
        for (String expected : expectedTypes) {
            assertTrue(classes.stream().anyMatch(type -> type.getName().equals(base + "." + expected)),
                    "Fixture bytecode was not imported: " + base + "." + expected);
        }
    }

    private static void assertNamedContract(Fixture fixture, String moduleName, String relativeType) {
        var module = fixture.modules().getModuleByName(moduleName).orElseThrow();
        var contract = module.getType(fixture.base() + "." + relativeType).orElseThrow();
        var named = module.getNamedInterfaces().getByName("contract")
                .orElseThrow(() -> new AssertionError("Named fixture export was not discovered"));
        assertTrue(named.contains(contract), "Named fixture interface is empty or excludes its contract");
        assertTrue(module.isExposed(contract));
    }

    private static void assertEdge(Fixture fixture, String source, String target,
            String sourceType, String targetType) {
        var dependencies = fixture.modules().getModuleByName(source).orElseThrow()
                .getDirectDependencies(fixture.modules());
        assertTrue(dependencies.stream().anyMatch(edge -> edge.getTargetModule().getIdentifier().toString().equals(target)
                        && edge.getSourceType().getName().equals(fixture.base() + "." + sourceType)
                        && edge.getTargetType().getName().equals(fixture.base() + "." + targetType)),
                "Modulith did not discover the fixture edge: " + sourceType + " -> " + targetType);
    }

    private static void assertGraph(Fixture fixture, Set<String> expected) {
        var actual = fixture.modules().stream().flatMap(module -> module.getDirectDependencies(fixture.modules()).stream()
                        .map(edge -> module.getIdentifier() + " -> " + edge.getTargetModule().getIdentifier()))
                .collect(Collectors.toSet());
        assertEquals(expected, actual, "Fixture graph differs from the intended test case");
    }

    private static void assertViolation(Violations failure, String... fragments) {
        assertTrue(failure.getMessages().stream()
                        .anyMatch(message -> Arrays.stream(fragments).allMatch(message::contains)),
                "Expected a specific Modulith violation " + Arrays.toString(fragments) + "; got: " + failure.getMessages());
    }

    private static void assertArchFailure(ArchRule rule, JavaClasses classes, String... fragments) {
        var failure = assertThrows(AssertionError.class, () -> rule.check(classes));
        assertMessageContains(failure.getMessage(), fragments);
    }

    private static void assertMessageContains(String message, String... fragments) {
        for (String fragment : fragments) {
            assertTrue(message != null && message.contains(fragment),
                    "Expected failure reason '" + fragment + "'; got: " + message);
        }
    }

    private record Fixture(String base, ApplicationModules modules, JavaClasses classes) {
    }
}
