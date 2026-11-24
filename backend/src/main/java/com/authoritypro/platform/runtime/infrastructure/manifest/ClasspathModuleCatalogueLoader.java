package com.authoritypro.platform.runtime.infrastructure.manifest;

import com.authoritypro.platform.runtime.application.ManifestValidationException;
import com.authoritypro.platform.runtime.application.ModuleCatalogue;
import com.authoritypro.platform.runtime.application.ModuleCatalogueAssembler;
import com.authoritypro.platform.runtime.application.ModuleManifestValidator;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.TreeMap;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import tools.jackson.core.StreamReadFeature;
import tools.jackson.databind.json.JsonMapper;

/**
 * Discovers only compiled first-party descriptors from the packaged classpath. Any defect makes the
 * whole catalogue unavailable; no partial or permissive fallback exists.
 */
public final class ClasspathModuleCatalogueLoader {
    static final String LOCATION = "classpath*:authority-pro/modules/*.json";

    public ModuleCatalogue load() {
        try {
            Resource[] resources = new PathMatchingResourcePatternResolver(
                    ClasspathModuleCatalogueLoader.class.getClassLoader()).getResources(LOCATION);
            Map<String, byte[]> descriptors = new TreeMap<>();
            for (Resource resource : resources) {
                descriptors.put(resource.getFilename(), readBounded(resource));
            }
            JsonMapper mapper = JsonMapper.builder()
                    .enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION).build();
            return new ModuleCatalogueAssembler().assemble(descriptors, mapper);
        } catch (ManifestValidationException invalid) {
            return ModuleCatalogue.unavailable(invalid.code().name());
        } catch (IOException | RuntimeException unreadable) {
            return ModuleCatalogue.unavailable("DESCRIPTORS_UNREADABLE");
        }
    }

    private static byte[] readBounded(Resource resource) throws IOException {
        try (InputStream input = resource.getInputStream()) {
            byte[] bytes = input.readNBytes(ModuleManifestValidator.MAX_DESCRIPTOR_BYTES + 1);
            if (bytes.length > ModuleManifestValidator.MAX_DESCRIPTOR_BYTES) {
                throw new ManifestValidationException(ManifestValidationException.Code.OVERSIZED,
                        resource.getFilename());
            }
            return bytes;
        }
    }
}
