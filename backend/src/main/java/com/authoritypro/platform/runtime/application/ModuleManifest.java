package com.authoritypro.platform.runtime.application;

import java.util.List;

/**
 * Immutable foundation manifest subset {@code 0.1.0}. Trust and execution remain declarations;
 * schema validity neither grants permissions nor authorizes installation.
 */
public record ModuleManifest(
        String schemaVersion,
        String id,
        String version,
        String kind,
        String trust,
        String execution,
        List<Dependency> dependencies,
        Capabilities capabilities,
        List<Permission> permissions,
        DataOwnership dataOwnership,
        Ui ui) {

    public ModuleManifest {
        dependencies = List.copyOf(dependencies);
        permissions = List.copyOf(permissions);
    }

    public record Dependency(String id, String version) { }

    public record Capability(String id, String version, String contractType) { }

    public record Capabilities(List<Capability> provides, List<Capability> requires) {
        public Capabilities {
            provides = List.copyOf(provides);
            requires = List.copyOf(requires);
        }
    }

    public record Permission(String id, String scope) { }

    public record OwnedResource(String name, String scope) { }

    public record DataOwnership(List<OwnedResource> resources) {
        public DataOwnership {
            resources = List.copyOf(resources);
        }
    }

    public record UiContribution(String id, String kind, String routeKey, String permission) { }

    public record Ui(List<UiContribution> contributions) {
        public Ui {
            contributions = List.copyOf(contributions);
        }
    }
}
