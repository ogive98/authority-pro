package com.authoritypro.architecture;

import java.util.Set;
import java.util.TreeSet;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import org.springframework.modulith.core.ApplicationModules;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The production checks and their adversarial fixtures use these same rules. */
final class BoundaryRules {
    static final Set<String> L02_ROOTS = Set.of("identity", "tenancy", "access", "audit", "runtime");

    private BoundaryRules() {
    }

    static void assertExactModules(ApplicationModules modules, String base, Set<String> expected) {
        var names = modules.stream().map(module -> module.getIdentifier().toString())
                .collect(Collectors.toCollection(TreeSet::new));
        assertEquals(new TreeSet<>(expected), names,
                "Discovered module names differ; annotated package-only roots must be discovered, not synthesized");
        modules.forEach(module -> {
            String actual = module.getBasePackage().getName();
            assertEquals(base + "." + module.getIdentifier(), actual,
                    "Not an immediate module root: " + actual);
            assertFalse(modules.hasParent(module), "Nested module root: " + actual);
        });
        assertTrue(modules.getSharedModules().isEmpty(), "No shared module grants are approved");
    }

    static void assertEmptyGraph(ApplicationModules modules) {
        var edges = modules.stream().flatMap(module -> module.getDirectDependencies(modules).stream()
                        .map(edge -> module.getIdentifier() + " -> " + edge.getTargetModule().getIdentifier()
                                + " via " + edge.getSourceType().getName() + " -> " + edge.getTargetType().getName()))
                .sorted().toList();
        assertTrue(edges.isEmpty(), "Expected zero intermodule dependencies, found: " + edges);
    }

    static ArchRule noCrossRootDependencies(String base) {
        return dependencyRule("have zero cross-root dependencies", (source, target) -> {
            String from = root(base, source);
            String to = root(base, target);
            return from != null && to != null && !from.equals(to)
                    ? "Forbidden intermodule edge " + from + " -> " + to : null;
        });
    }

    static ArchRule privatePackagesStayOwned(String base) {
        return dependencyRule("keep non-api packages private to their owner", (source, target) -> {
            String from = root(base, source);
            String to = root(base, target);
            return from != null && to != null && !from.equals(to) && !inLayer(base, target, "api")
                    ? "Cross-module private import " + from + " -> " + to : null;
        });
    }

    // These guards do not require empty domain/application/api packages to be invented.
    static ArchRule layersPointInward(String base) {
        return dependencyRule("keep domain framework-free and layers pointing inward", (source, target) -> {
            if (inLayer(base, source, "domain")) {
                String name = target.getName();
                if (name.startsWith("org.springframework.") || name.startsWith("jakarta.")
                        || name.startsWith("javax.persistence.") || name.startsWith("org.hibernate.")
                        || name.startsWith("com.fasterxml.jackson.")) {
                    return "Domain depends on framework";
                }
                if (inLayer(base, target, "application") || inLayer(base, target, "infrastructure")) {
                    return "Domain points outward";
                }
            }
            if (inLayer(base, source, "application") && inLayer(base, target, "infrastructure")) {
                return "Application depends on infrastructure";
            }
            if (inLayer(base, source, "api") && root(base, target) != null
                    && !inLayer(base, target, "api")) {
                return "API leaks private ownership";
            }
            return null;
        });
    }

    static ArchRule onlyImmediateRoots(String base, Set<String> roots, String entrypoint) {
        return classes().should(new ArchCondition<JavaClass>("belong to approved immediate module roots") {
            @Override
            public void check(JavaClass type, ConditionEvents events) {
                String owner = root(base, type);
                if (owner == null || !roots.contains(owner)) {
                    if (!belongsToEntrypoint(type, entrypoint)) {
                        events.add(SimpleConditionEvent.violated(type,
                                "Unapproved module ownership: " + type.getName()));
                    }
                } else if (type.isAnnotatedWith(org.springframework.modulith.ApplicationModule.class)
                        && !type.getPackageName().equals(base + "." + owner)) {
                    events.add(SimpleConditionEvent.violated(type,
                            "Not an immediate module root: " + type.getPackageName()));
                }
            }
        });
    }

    private static ArchRule dependencyRule(String description,
            BiFunction<JavaClass, JavaClass, String> violation) {
        return classes().should(new ArchCondition<JavaClass>(description) {
            @Override
            public void check(JavaClass source, ConditionEvents events) {
                for (var dependency : source.getDirectDependenciesFromSelf()) {
                    JavaClass target = dependency.getTargetClass();
                    if (target.isArray()) {
                        target = target.getBaseComponentType();
                    }
                    String reason = violation.apply(source, target);
                    if (reason != null) {
                        events.add(SimpleConditionEvent.violated(dependency,
                                reason + ": " + dependency.getDescription()));
                    }
                }
            }
        });
    }

    private static boolean belongsToEntrypoint(JavaClass type, String entrypoint) {
        JavaClass current = type;
        while (!current.getName().equals(entrypoint)) {
            var enclosing = current.getEnclosingClass();
            if (enclosing.isEmpty()) {
                return false;
            }
            current = enclosing.get();
        }
        return true;
    }

    private static String root(String base, JavaClass type) {
        String name = type.getPackageName();
        if (!name.startsWith(base + ".")) {
            return null;
        }
        String relative = name.substring(base.length() + 1);
        int separator = relative.indexOf('.');
        return separator < 0 ? relative : relative.substring(0, separator);
    }

    private static boolean inLayer(String base, JavaClass type, String layer) {
        String owner = root(base, type);
        if (owner == null) {
            return false;
        }
        String prefix = base + "." + owner + "." + layer;
        return type.getPackageName().equals(prefix) || type.getPackageName().startsWith(prefix + ".");
    }
}
