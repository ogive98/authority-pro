package com.authoritypro.platform.runtime.application;

import com.authoritypro.platform.runtime.application.ManifestValidationException.Code;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Builds the immutable catalogue from exact compiled bytes. Each descriptor's digest covers those
 * bytes, so any later byte change is visible without trusting the manifest's own claims.
 */
public final class ModuleCatalogueAssembler {

    public ModuleCatalogue assemble(Map<String, byte[]> descriptors, JsonMapper mapper) {
        if (descriptors == null || descriptors.isEmpty()) {
            throw new ManifestValidationException(Code.EMPTY_CATALOGUE, "classpath");
        }
        List<ModuleManifest> manifests = new ArrayList<>();
        Map<String, String> digests = new TreeMap<>();
        for (Map.Entry<String, byte[]> descriptor : new TreeMap<>(descriptors).entrySet()) {
            String name = descriptor.getKey();
            byte[] bytes = descriptor.getValue();
            if (bytes == null) throw new ManifestValidationException(Code.MALFORMED_JSON, name);
            if (bytes.length > ModuleManifestValidator.MAX_DESCRIPTOR_BYTES) {
                throw new ManifestValidationException(Code.OVERSIZED, name);
            }
            JsonNode root;
            try {
                root = mapper.readTree(bytes);
            } catch (JacksonException malformed) {
                throw new ManifestValidationException(Code.MALFORMED_JSON, name);
            }
            manifests.add(ModuleManifestValidator.structural(root, name));
            digests.put(manifests.get(manifests.size() - 1).id(), digest(bytes));
        }
        ModuleManifestValidator.semantic(manifests);
        manifests.sort(Comparator.comparing(ModuleManifest::id));
        List<ModuleCatalogue.Entry> entries = new ArrayList<>();
        for (ModuleManifest manifest : manifests) {
            entries.add(new ModuleCatalogue.Entry(manifest, digests.get(manifest.id())));
        }
        return new ModuleCatalogue(entries, null);
    }

    public static String digest(byte[] bytes) {
        try {
            return "sha256:" + HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException unavailable) {
            throw new IllegalStateException("DIGEST_UNAVAILABLE");
        }
    }
}
