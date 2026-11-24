package com.authoritypro.platform.runtime.application;

import com.authoritypro.platform.runtime.application.ManifestValidationException.Code;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import tools.jackson.databind.JsonNode;

/**
 * Two-stage validation of the frozen {@code 0.1.0} subset: structural checks mirror the published
 * schema, semantic checks resolve IDs, dependencies, capabilities, ownership and UI references.
 * Rejection is fail-fast with a bounded code; descriptor content is never echoed.
 */
public final class ModuleManifestValidator {
    public static final String SCHEMA_VERSION = "0.1.0";
    public static final int MAX_DESCRIPTOR_BYTES = 64 * 1024;

    private static final Set<String> FIELDS = Set.of("schemaVersion", "id", "version", "kind", "trust",
            "execution", "dependencies", "capabilities", "permissions", "dataOwnership", "ui");
    private static final Set<String> MODULE_KINDS = Set.of("platform-module");
    private static final Set<String> TRUSTS = Set.of("first-party");
    private static final Set<String> EXECUTIONS = Set.of("compiled-first-party");
    private static final Set<String> CONTRACT_TYPES = Set.of("command", "query", "event");
    private static final Set<String> PERMISSION_SCOPES = Set.of("control-plane", "tenant", "company");
    private static final Set<String> RESOURCE_SCOPES = Set.of("control-plane", "tenant");
    private static final Set<String> UI_KINDS = Set.of("navigation");
    private static final Pattern MODULE_ID = Pattern.compile("^[a-z][a-z0-9]*(?:-[a-z0-9]+)*$");
    private static final Pattern CONTRACT_ID = Pattern.compile("^[a-z][a-z0-9]*(?:[.-][a-z0-9]+)+$");
    private static final Pattern PERMISSION_ID = Pattern.compile("^[a-z][a-z0-9_]*(?:\\.[a-z][a-z0-9_]*)+$");
    private static final Pattern RELEASE_VERSION = Pattern.compile("^(0|[1-9][0-9]*)\\.(0|[1-9][0-9]*)\\.(0|[1-9][0-9]*)$");
    private static final Pattern RESOURCE_NAME = Pattern.compile("^[a-z][a-z0-9]*(?:_[a-z0-9]+)*$");

    private ModuleManifestValidator() { }

    public static ModuleManifest structural(JsonNode root, String descriptor) {
        if (root == null || !root.isObject()) throw failure(Code.NOT_OBJECT, descriptor);
        for (Map.Entry<String, JsonNode> field : root.properties()) {
            if (!FIELDS.contains(field.getKey())) throw failure(Code.FIELD_UNKNOWN, descriptor);
        }
        for (String field : FIELDS) {
            if (root.get(field) == null) throw failure(Code.FIELD_MISSING, descriptor);
        }
        JsonNode capabilities = object(root, "capabilities", Set.of("provides", "requires"), descriptor);
        JsonNode ownership = object(root, "dataOwnership", Set.of("resources"), descriptor);
        JsonNode ui = object(root, "ui", Set.of("contributions"), descriptor);
        ModuleManifest manifest = new ModuleManifest(
                constant(root, "schemaVersion", SCHEMA_VERSION, descriptor),
                moduleId(root, "id", descriptor),
                releaseVersion(root, "version", descriptor),
                constant(root, "kind", MODULE_KINDS, descriptor),
                constant(root, "trust", TRUSTS, descriptor),
                constant(root, "execution", EXECUTIONS, descriptor),
                dependencies(array(root, "dependencies", 32, descriptor), descriptor),
                new ModuleManifest.Capabilities(
                        capabilities(array(capabilities, "provides", 64, descriptor), descriptor),
                        capabilities(array(capabilities, "requires", 64, descriptor), descriptor)),
                permissions(array(root, "permissions", 64, descriptor), descriptor),
                new ModuleManifest.DataOwnership(resources(array(ownership, "resources", 64, descriptor), descriptor)),
                new ModuleManifest.Ui(contributions(array(ui, "contributions", 16, descriptor), descriptor)));
        return manifest;
    }

    public static void semantic(List<ModuleManifest> manifests) {
        semantic(manifests, Set.of());
    }

    public static void semantic(List<ModuleManifest> manifests, Set<String> routeKeys) {
        Map<String, ModuleManifest> byId = new LinkedHashMap<>();
        for (ModuleManifest manifest : manifests) {
            if (byId.putIfAbsent(manifest.id(), manifest) != null) {
                throw failure(Code.DUPLICATE_MODULE, manifest.id());
            }
        }
        for (ModuleManifest manifest : manifests) {
            uniqueDependencies(manifest);
            uniqueCapabilities(manifest);
            uniqueContributions(manifest);
        }
        Map<String, Provider> providers = providers(manifests);
        uniqueOwnership(manifests);
        for (ModuleManifest manifest : manifests) {
            dependencies(manifest, byId);
            capabilities(manifest, providers);
            references(manifest, routeKeys);
        }
        acyclic(manifests);
    }

    private static void uniqueDependencies(ModuleManifest manifest) {
        Set<String> ids = new HashSet<>();
        for (ModuleManifest.Dependency dependency : manifest.dependencies()) {
            if (!ids.add(dependency.id())) throw failure(Code.DUPLICATE_DEPENDENCY, manifest.id());
        }
    }

    private static void uniqueCapabilities(ModuleManifest manifest) {
        for (List<ModuleManifest.Capability> list : List.of(manifest.capabilities().provides(),
                manifest.capabilities().requires())) {
            Set<String> keys = new HashSet<>();
            for (ModuleManifest.Capability capability : list) {
                if (!keys.add(capability.id() + "@" + capability.version() + "/" + capability.contractType())) {
                    throw failure(Code.DUPLICATE_CAPABILITY, manifest.id());
                }
            }
        }
    }

    private static void uniqueContributions(ModuleManifest manifest) {
        Set<String> ids = new HashSet<>();
        for (ModuleManifest.UiContribution contribution : manifest.ui().contributions()) {
            if (!ids.add(contribution.id())) throw failure(Code.DUPLICATE_UI, manifest.id());
        }
    }

    /** A provided capability has exactly one owning module in this compiled subset. */
    private static Map<String, Provider> providers(List<ModuleManifest> manifests) {
        Map<String, Provider> providers = new HashMap<>();
        for (ModuleManifest manifest : manifests) {
            for (ModuleManifest.Capability capability : manifest.capabilities().provides()) {
                if (providers.putIfAbsent(capability.id(), new Provider(manifest.id(), capability.version())) != null) {
                    throw failure(Code.CAPABILITY_AMBIGUOUS, manifest.id());
                }
            }
        }
        return providers;
    }

    private record Provider(String module, String version) { }

    private static void dependencies(ModuleManifest manifest, Map<String, ModuleManifest> byId) {
        for (ModuleManifest.Dependency dependency : manifest.dependencies()) {
            if (dependency.id().equals(manifest.id())) throw failure(Code.DEPENDENCY_SELF, manifest.id());
            ModuleManifest target = byId.get(dependency.id());
            if (target == null) throw failure(Code.DEPENDENCY_MISSING, manifest.id());
            if (!target.version().equals(dependency.version())) {
                throw failure(Code.DEPENDENCY_VERSION_MISMATCH, manifest.id());
            }
        }
    }

    /**
     * Each requirement resolves to the single compatible provider, and that provider is reachable
     * through a declared dependency of this module. Self-provided requirements are rejected.
     */
    private static void capabilities(ModuleManifest manifest, Map<String, Provider> providers) {
        Set<String> dependencies = new HashSet<>();
        for (ModuleManifest.Dependency dependency : manifest.dependencies()) dependencies.add(dependency.id());
        Map<String, ModuleManifest.Capability> own = new HashMap<>();
        for (ModuleManifest.Capability capability : manifest.capabilities().provides()) {
            own.put(capability.id(), capability);
        }
        for (ModuleManifest.Capability required : manifest.capabilities().requires()) {
            if (own.containsKey(required.id())) throw failure(Code.CAPABILITY_SELF_REQUIRED, manifest.id());
        }
        for (ModuleManifest.Capability required : manifest.capabilities().requires()) {
            Provider provider = providers.get(required.id());
            if (provider == null) throw failure(Code.CAPABILITY_MISSING, manifest.id());
            if (!dependencies.contains(provider.module())) throw failure(Code.CAPABILITY_MISSING, manifest.id());
            if (!provider.version().equals(required.version())) {
                throw failure(Code.CAPABILITY_VERSION_MISMATCH, manifest.id());
            }
        }
    }

    private static void references(ModuleManifest manifest, Set<String> routeKeys) {
        Set<String> permissions = new HashSet<>();
        for (ModuleManifest.Permission permission : manifest.permissions()) {
            if (!permissions.add(permission.id())) throw failure(Code.PERMISSION_DUPLICATE, manifest.id());
        }
        Set<String> resources = new HashSet<>();
        for (ModuleManifest.OwnedResource resource : manifest.dataOwnership().resources()) {
            if (!resources.add(resource.name())) throw failure(Code.RESOURCE_DUPLICATE, manifest.id());
        }
        for (ModuleManifest.UiContribution contribution : manifest.ui().contributions()) {
            if (!permissions.contains(contribution.permission())) {
                throw failure(Code.UI_PERMISSION_UNKNOWN, manifest.id());
            }
            if (!routeKeys.contains(contribution.routeKey())) throw failure(Code.ROUTE_UNRESOLVED, manifest.id());
        }
    }

    /** Global ownership: a logical resource has exactly one declaring module, whatever its scope. */
    private static void uniqueOwnership(List<ModuleManifest> manifests) {
        Set<String> names = new HashSet<>();
        for (ModuleManifest manifest : manifests) {
            for (ModuleManifest.OwnedResource resource : manifest.dataOwnership().resources()) {
                if (!names.add(resource.name())) throw failure(Code.RESOURCE_DUPLICATE, manifest.id());
            }
        }
    }

    private static void acyclic(List<ModuleManifest> manifests) {
        Map<String, Integer> state = new HashMap<>();
        for (ModuleManifest manifest : manifests) {
            if (state.getOrDefault(manifest.id(), 0) == 0) visit(manifest, state, manifests);
        }
    }

    private static void visit(ModuleManifest manifest, Map<String, Integer> state, List<ModuleManifest> manifests) {
        state.put(manifest.id(), 1);
        for (ModuleManifest.Dependency dependency : manifest.dependencies()) {
            int found = state.getOrDefault(dependency.id(), 0);
            if (found == 1) throw failure(Code.DEPENDENCY_CYCLE, manifest.id());
            if (found == 0) visit(byId(manifests, dependency.id()), state, manifests);
        }
        state.put(manifest.id(), 2);
    }

    private static ModuleManifest byId(List<ModuleManifest> manifests, String id) {
        for (ModuleManifest manifest : manifests) {
            if (manifest.id().equals(id)) return manifest;
        }
        throw failure(Code.DEPENDENCY_MISSING, id);
    }

    private static JsonNode object(JsonNode parent, String field, Set<String> fields, String descriptor) {
        JsonNode node = value(parent, field, descriptor);
        if (!node.isObject()) throw failure(Code.TYPE_INVALID, descriptor);
        for (Map.Entry<String, JsonNode> entry : node.properties()) {
            if (!fields.contains(entry.getKey())) throw failure(Code.FIELD_UNKNOWN, descriptor);
        }
        for (String required : fields) {
            if (node.get(required) == null) throw failure(Code.FIELD_MISSING, descriptor);
        }
        return node;
    }

    private static String constant(JsonNode parent, String field, String expected, String descriptor) {
        return constant(parent, field, Set.of(expected), descriptor);
    }

    private static String constant(JsonNode parent, String field, Set<String> expected, String descriptor) {
        String value = text(parent, field, descriptor);
        if (!expected.contains(value)) throw failure(Code.CONSTANT_INVALID, descriptor);
        return value;
    }

    private static String moduleId(JsonNode parent, String field, String descriptor) {
        return pattern(parent, field, MODULE_ID, 64, descriptor);
    }

    private static String releaseVersion(JsonNode parent, String field, String descriptor) {
        return pattern(parent, field, RELEASE_VERSION, 32, descriptor);
    }

    private static String contractId(JsonNode parent, String field, String descriptor) {
        String value = pattern(parent, field, CONTRACT_ID, 128, descriptor);
        if (value.length() < 3) throw failure(Code.VALUE_INVALID, descriptor);
        return value;
    }

    private static String permissionId(JsonNode parent, String field, String descriptor) {
        String value = pattern(parent, field, PERMISSION_ID, 128, descriptor);
        if (value.length() < 3) throw failure(Code.VALUE_INVALID, descriptor);
        return value;
    }

    private static String pattern(JsonNode parent, String field, Pattern pattern, int maxLength, String descriptor) {
        String value = text(parent, field, descriptor);
        if (value.isEmpty() || value.length() > maxLength || !pattern.matcher(value).matches()) {
            throw failure(Code.VALUE_INVALID, descriptor);
        }
        return value;
    }

    private static String text(JsonNode parent, String field, String descriptor) {
        JsonNode node = value(parent, field, descriptor);
        if (!node.isTextual()) throw failure(Code.TYPE_INVALID, descriptor);
        return node.asString();
    }

    private static JsonNode value(JsonNode parent, String field, String descriptor) {
        JsonNode node = parent.get(field);
        if (node == null) throw failure(Code.FIELD_MISSING, descriptor);
        if (node.isNull()) throw failure(Code.TYPE_INVALID, descriptor);
        return node;
    }

    private static List<JsonNode> array(JsonNode parent, String field, int maxItems, String descriptor) {
        JsonNode node = value(parent, field, descriptor);
        if (!node.isArray()) throw failure(Code.TYPE_INVALID, descriptor);
        List<JsonNode> items = new ArrayList<>();
        for (JsonNode item : node) items.add(item);
        if (items.size() > maxItems) throw failure(Code.ARRAY_TOO_LONG, descriptor);
        return items;
    }

    private static List<ModuleManifest.Dependency> dependencies(List<JsonNode> items, String descriptor) {
        List<ModuleManifest.Dependency> result = new ArrayList<>();
        for (JsonNode item : items) {
            if (!item.isObject()) throw failure(Code.TYPE_INVALID, descriptor);
            onlyFields(item, Set.of("id", "version"), descriptor);
            result.add(new ModuleManifest.Dependency(moduleId(item, "id", descriptor),
                    releaseVersion(item, "version", descriptor)));
        }
        return result;
    }

    private static List<ModuleManifest.Capability> capabilities(List<JsonNode> items, String descriptor) {
        List<ModuleManifest.Capability> result = new ArrayList<>();
        for (JsonNode item : items) {
            if (!item.isObject()) throw failure(Code.TYPE_INVALID, descriptor);
            onlyFields(item, Set.of("id", "version", "contractType"), descriptor);
            result.add(new ModuleManifest.Capability(contractId(item, "id", descriptor),
                    releaseVersion(item, "version", descriptor),
                    constant(item, "contractType", CONTRACT_TYPES, descriptor)));
        }
        return result;
    }

    private static List<ModuleManifest.Permission> permissions(List<JsonNode> items, String descriptor) {
        List<ModuleManifest.Permission> result = new ArrayList<>();
        for (JsonNode item : items) {
            if (!item.isObject()) throw failure(Code.TYPE_INVALID, descriptor);
            onlyFields(item, Set.of("id", "scope"), descriptor);
            result.add(new ModuleManifest.Permission(permissionId(item, "id", descriptor),
                    constant(item, "scope", PERMISSION_SCOPES, descriptor)));
        }
        return result;
    }

    private static List<ModuleManifest.OwnedResource> resources(List<JsonNode> items, String descriptor) {
        List<ModuleManifest.OwnedResource> result = new ArrayList<>();
        for (JsonNode item : items) {
            if (!item.isObject()) throw failure(Code.TYPE_INVALID, descriptor);
            onlyFields(item, Set.of("name", "scope"), descriptor);
            result.add(new ModuleManifest.OwnedResource(
                    pattern(item, "name", RESOURCE_NAME, 63, descriptor),
                    constant(item, "scope", RESOURCE_SCOPES, descriptor)));
        }
        return result;
    }

    private static List<ModuleManifest.UiContribution> contributions(List<JsonNode> items, String descriptor) {
        List<ModuleManifest.UiContribution> result = new ArrayList<>();
        for (JsonNode item : items) {
            if (!item.isObject()) throw failure(Code.TYPE_INVALID, descriptor);
            onlyFields(item, Set.of("id", "kind", "routeKey", "permission"), descriptor);
            result.add(new ModuleManifest.UiContribution(contractId(item, "id", descriptor),
                    constant(item, "kind", UI_KINDS, descriptor),
                    contractId(item, "routeKey", descriptor),
                    permissionId(item, "permission", descriptor)));
        }
        return result;
    }

    private static void onlyFields(JsonNode item, Set<String> fields, String descriptor) {
        for (Map.Entry<String, JsonNode> entry : item.properties()) {
            if (!fields.contains(entry.getKey())) throw failure(Code.FIELD_UNKNOWN, descriptor);
        }
        for (String field : fields) {
            if (item.get(field) == null) throw failure(Code.FIELD_MISSING, descriptor);
        }
    }

    private static ManifestValidationException failure(Code code, String descriptor) {
        return new ManifestValidationException(code, descriptor);
    }
}
