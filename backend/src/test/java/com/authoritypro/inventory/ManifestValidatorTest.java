package com.authoritypro.inventory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.authoritypro.platform.runtime.application.ManifestValidationException;
import com.authoritypro.platform.runtime.application.ManifestValidationException.Code;
import com.authoritypro.platform.runtime.application.ModuleCatalogue;
import com.authoritypro.platform.runtime.application.ModuleCatalogueAssembler;
import com.authoritypro.platform.runtime.application.ModuleManifestValidator;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import tools.jackson.core.StreamReadFeature;
import tools.jackson.databind.json.JsonMapper;

class ManifestValidatorTest {
    private static final JsonMapper MAPPER =
            JsonMapper.builder().enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION).build();
    private static final ModuleCatalogueAssembler ASSEMBLER = new ModuleCatalogueAssembler();

    private static final String BASE = """
            {
              "schemaVersion": "0.1.0",
              "id": "identity",
              "version": "0.1.0",
              "kind": "platform-module",
              "trust": "first-party",
              "execution": "compiled-first-party",
              "dependencies": [],
              "capabilities": { "provides": [], "requires": [] },
              "permissions": [],
              "dataOwnership": { "resources": [] },
              "ui": { "contributions": [] }
            }
            """;

    private static String module(String id) {
        return BASE.replace("\"identity\"", "\"" + id + "\"");
    }

    private static ModuleCatalogue assemble(String... descriptors) {
        Map<String, byte[]> bytes = new LinkedHashMap<>();
        for (int index = 0; index < descriptors.length; index++) {
            bytes.put("module-" + index + ".json", descriptors[index].getBytes(StandardCharsets.UTF_8));
        }
        return ASSEMBLER.assemble(bytes, MAPPER);
    }

    private static Code rejected(String... descriptors) {
        ManifestValidationException failure = assertThrows(ManifestValidationException.class,
                () -> assemble(descriptors));
        return failure.code();
    }

    private static String dependency(String id, String version) {
        return "{\"id\":\"" + id + "\",\"version\":\"" + version + "\"}";
    }

    @Test
    void acceptsTheFrozenSubset() {
        ModuleCatalogue catalogue = assemble(module("identity"));
        assertEquals(1, catalogue.modules().size());
        assertEquals("identity", catalogue.modules().getFirst().manifest().id());
        assertEquals(ModuleCatalogueAssembler.digest(module("identity").getBytes(StandardCharsets.UTF_8)),
                catalogue.modules().getFirst().digest());
    }

    @Test
    void rejectsNonObjectUnknownAndMissingFields() {
        assertEquals(Code.NOT_OBJECT, rejected("[]"));
        assertEquals(Code.FIELD_UNKNOWN, rejected(BASE.replace("\"trust\": \"first-party\",",
                "\"trust\": \"first-party\", \"publisher\": \"x\",")));
        assertEquals(Code.FIELD_MISSING, rejected(BASE.replace("\"trust\": \"first-party\",", "")));
        assertEquals(Code.FIELD_MISSING, rejected(BASE.replace(",\n  \"ui\": { \"contributions\": [] }", "")));
    }

    @Test
    void rejectsEveryInvalidConstantAndVersion() {
        assertEquals(Code.CONSTANT_INVALID, rejected(BASE.replace("0.1.0\",\n  \"id", "0.2.0\",\n  \"id")));
        assertEquals(Code.CONSTANT_INVALID, rejected(BASE.replace("platform-module", "business-module")));
        assertEquals(Code.CONSTANT_INVALID, rejected(BASE.replace("first-party", "third-party")));
        assertEquals(Code.CONSTANT_INVALID, rejected(BASE.replace("compiled-first-party", "dynamic")));
        assertEquals(Code.VALUE_INVALID, rejected(BASE.replace("\"identity\"", "\"Identity\"")));
        assertEquals(Code.VALUE_INVALID, rejected(BASE.replace("\"version\": \"0.1.0\"", "\"version\": \"1.0\"")));
        assertEquals(Code.VALUE_INVALID,
                rejected(BASE.replace("\"version\": \"0.1.0\"", "\"version\": \"1.0.0-rc\"")));
        assertEquals(Code.VALUE_INVALID, rejected(BASE.replace("\"version\": \"0.1.0\"", "\"version\": \"01.0.0\"")));
    }

    @Test
    void rejectsMalformedOversizedAndDuplicateKeyInput() {
        assertEquals(Code.MALFORMED_JSON, rejected("{"));
        assertEquals(Code.OVERSIZED, rejected(" ".repeat(ModuleManifestValidator.MAX_DESCRIPTOR_BYTES + 1)));
        assertEquals(Code.MALFORMED_JSON, rejected(BASE.replaceFirst("\"schemaVersion\": \"0.1.0\",",
                "\"schemaVersion\": \"0.1.0\", \"schemaVersion\": \"0.1.0\",")));
        assertEquals(Code.EMPTY_CATALOGUE, assertThrows(ManifestValidationException.class,
                () -> ASSEMBLER.assemble(Map.of(), MAPPER)).code());
    }

    @Test
    void rejectsWrongNestedShapesAndValues() {
        assertEquals(Code.TYPE_INVALID, rejected(BASE.replace("\"dependencies\": [],", "\"dependencies\": {},")));
        assertEquals(Code.TYPE_INVALID, rejected(BASE.replace("\"permissions\": [],", "\"permissions\": 3,")));
        assertEquals(Code.FIELD_UNKNOWN, rejected(BASE.replace("\"dependencies\": [],",
                "\"dependencies\": [{ \"id\": \"tenancy\", \"version\": \"0.1.0\", \"optional\": true }],")));
        assertEquals(Code.CONSTANT_INVALID, rejected(BASE.replace("\"capabilities\": { \"provides\": [], \"requires\": [] }",
                "\"capabilities\": { \"provides\": [{ \"id\": \"a.b\", \"version\": \"0.1.0\","
                        + " \"contractType\": \"stream\" }], \"requires\": [] }")));
        assertEquals(Code.CONSTANT_INVALID, rejected(BASE.replace("\"permissions\": [],",
                "\"permissions\": [{ \"id\": \"a.b\", \"scope\": \"global\" }],")));
        assertEquals(Code.CONSTANT_INVALID, rejected(BASE.replace("\"dataOwnership\": { \"resources\": [] }",
                "\"dataOwnership\": { \"resources\": [{ \"name\": \"users\", \"scope\": \"company\" }] }")));
        assertEquals(Code.VALUE_INVALID, rejected(BASE.replace("\"dataOwnership\": { \"resources\": [] }",
                "\"dataOwnership\": { \"resources\": [{ \"name\": \"Users\", \"scope\": \"tenant\" }] }")));
        assertEquals(Code.FIELD_UNKNOWN, rejected(BASE.replace("\"ui\": { \"contributions\": [] }",
                "\"ui\": { \"contributions\": [], \"theme\": \"dark\" }")));
    }

    @Test
    void rejectsOversizedArraysBeforeSemanticChecks() {
        StringBuilder dependencies = new StringBuilder();
        for (int index = 0; index < 33; index++) {
            if (index > 0) dependencies.append(',');
            dependencies.append(dependency("tenancy", "0.1.0"));
        }
        assertEquals(Code.ARRAY_TOO_LONG, rejected(BASE.replace("\"dependencies\": [],",
                "\"dependencies\": [" + dependencies + "],")));
    }

    @Test
    void rejectsDuplicateManifestFacts() {
        assertEquals(Code.DUPLICATE_MODULE, rejected(module("identity"), module("identity")));
        assertEquals(Code.DUPLICATE_DEPENDENCY, rejected(BASE.replace("\"dependencies\": [],",
                "\"dependencies\": [" + dependency("tenancy", "0.1.0") + "," + dependency("tenancy", "0.1.0")
                        + "],")));
        assertEquals(Code.PERMISSION_DUPLICATE, rejected(BASE.replace("\"permissions\": [],",
                "\"permissions\": [{ \"id\": \"a.b\", \"scope\": \"tenant\" },"
                        + " { \"id\": \"a.b\", \"scope\": \"tenant\" }],")));
        assertEquals(Code.RESOURCE_DUPLICATE, rejected(BASE.replace("\"dataOwnership\": { \"resources\": [] }",
                "\"dataOwnership\": { \"resources\": [{ \"name\": \"users\", \"scope\": \"tenant\" },"
                        + " { \"name\": \"users\", \"scope\": \"control-plane\" }] }")));
        assertEquals(Code.RESOURCE_DUPLICATE, rejected(
                BASE.replace("\"dataOwnership\": { \"resources\": [] }",
                        "\"dataOwnership\": { \"resources\": [{ \"name\": \"users\", \"scope\": \"tenant\" }] }")
                        .replace("\"id\": \"identity\"", "\"id\": \"tenancy\""),
                BASE.replace("\"dataOwnership\": { \"resources\": [] }",
                        "\"dataOwnership\": { \"resources\": [{ \"name\": \"users\", \"scope\": \"control-plane\" }] }")));
    }

    @Test
    void rejectsBrokenDependencyGraphs() {
        assertEquals(Code.DEPENDENCY_SELF, rejected(BASE.replace("\"dependencies\": [],",
                "\"dependencies\": [" + dependency("identity", "0.1.0") + "],")));
        assertEquals(Code.DEPENDENCY_MISSING, rejected(BASE.replace("\"dependencies\": [],",
                "\"dependencies\": [" + dependency("tenancy", "0.1.0") + "],")));
        assertEquals(Code.DEPENDENCY_VERSION_MISMATCH, rejected(
                module("identity").replace("\"dependencies\": [],",
                        "\"dependencies\": [" + dependency("tenancy", "0.2.0") + "],"),
                module("tenancy")));
        assertEquals(Code.DEPENDENCY_CYCLE, rejected(
                module("identity").replace("\"dependencies\": [],",
                        "\"dependencies\": [" + dependency("tenancy", "0.1.0") + "],"),
                module("tenancy").replace("\"dependencies\": [],",
                        "\"dependencies\": [" + dependency("identity", "0.1.0") + "],")));
    }

    @Test
    void rejectsUnresolvedOrAmbiguousCapabilities() {
        String provider = BASE.replace("\"id\": \"identity\"", "\"id\": \"tenancy\"")
                .replace("\"provides\": []",
                        "\"provides\": [{ \"id\": \"tenancy.membership.read\", \"version\": \"0.1.0\","
                                + " \"contractType\": \"query\" }]");
        String requires = "\"requires\": [{ \"id\": \"tenancy.membership.read\", \"version\": \"0.1.0\","
                + " \"contractType\": \"query\" }]";
        assertEquals(Code.CAPABILITY_MISSING, rejected(BASE.replace("\"requires\": []", requires)));
        assertEquals(Code.CAPABILITY_MISSING, rejected(BASE.replace("\"requires\": []", requires), provider));
        assertEquals(Code.CAPABILITY_VERSION_MISMATCH, rejected(
                module("identity").replace("\"dependencies\": [],",
                                "\"dependencies\": [" + dependency("tenancy", "0.1.0") + "],")
                        .replace("\"requires\": []",
                                "\"requires\": [{ \"id\": \"tenancy.membership.read\", \"version\": \"0.2.0\","
                                        + " \"contractType\": \"query\" }]"),
                provider));
        assertEquals(Code.CAPABILITY_AMBIGUOUS, rejected(provider,
                provider.replace("\"id\": \"tenancy\"", "\"id\": \"audit\"")));
        String self = BASE.replace("\"provides\": []",
                "\"provides\": [{ \"id\": \"identity.session.read\", \"version\": \"0.1.0\","
                        + " \"contractType\": \"query\" }]")
                .replace("\"requires\": []",
                        "\"requires\": [{ \"id\": \"identity.session.read\", \"version\": \"0.1.0\","
                                + " \"contractType\": \"query\" }]");
        assertEquals(Code.CAPABILITY_SELF_REQUIRED, rejected(self));
    }

    @Test
    void rejectsUnresolvedUiReferences() {
        String withPermission = BASE.replace("\"permissions\": [],",
                "\"permissions\": [{ \"id\": \"platform.inventory.read\", \"scope\": \"control-plane\" }],");
        assertEquals(Code.UI_PERMISSION_UNKNOWN, rejected(withPermission.replace("\"contributions\": []",
                "\"contributions\": [{ \"id\": \"platform.inventory\", \"kind\": \"navigation\","
                        + " \"routeKey\": \"inventory.home\", \"permission\": \"platform.inventory.view\" }]")));
        assertEquals(Code.ROUTE_UNRESOLVED, rejected(withPermission.replace("\"contributions\": []",
                "\"contributions\": [{ \"id\": \"platform.inventory\", \"kind\": \"navigation\","
                        + "\"routeKey\": \"inventory.home\", \"permission\": \"platform.inventory.read\" }]")));
    }

    @Test
    void publishedTemplateIsStructurallyValidAndTheInvalidFixtureIsRejected() throws IOException {
        ModuleCatalogue template = assemble(read("MODULE-MANIFEST-TEMPLATE.json"));
        assertEquals("diagnostics-example", template.modules().getFirst().manifest().id());
        assertEquals(Code.FIELD_MISSING, rejected(read("MODULE-MANIFEST-INVALID.json")));
    }

    @Test
    void digestCoversTheExactBytes() {
        byte[] original = module("identity").getBytes(StandardCharsets.UTF_8);
        byte[] reformatted = BASE.replace("\"identity\"", "\"identity\"").replace("  ", " ")
                .getBytes(StandardCharsets.UTF_8);
        assertEquals(ModuleCatalogueAssembler.digest(original), ModuleCatalogueAssembler.digest(original));
        assertNotEquals(ModuleCatalogueAssembler.digest(original), ModuleCatalogueAssembler.digest(reformatted));
        assertEquals(71, ModuleCatalogueAssembler.digest(original).length());
    }

    private static String read(String fixture) throws IOException {
        for (Path candidate : new Path[]{Path.of("..", "docs", "14-TEMPLATES", fixture),
                Path.of("docs", "14-TEMPLATES", fixture)}) {
            if (Files.exists(candidate)) return Files.readString(candidate, StandardCharsets.UTF_8);
        }
        throw new IOException("fixture not found: " + fixture);
    }
}
