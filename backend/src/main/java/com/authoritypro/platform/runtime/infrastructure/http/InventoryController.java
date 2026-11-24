package com.authoritypro.platform.runtime.infrastructure.http;

import com.authoritypro.platform.runtime.application.BuildRevision;
import com.authoritypro.platform.runtime.application.ModuleCatalogue;
import com.authoritypro.platform.runtime.application.ModuleManifest;
import java.util.ArrayList;
import java.util.List;
import javax.sql.DataSource;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Protected read-only inventory of the compiled catalogue (MODULE-RUNTIME §7). Authentication and
 * {@code platform.inventory.read} authorization are enforced by the access boundary before MVC.
 */
@ConditionalOnBean(DataSource.class)
@RestController
@RequestMapping("/api/v1/platform/inventory")
public final class InventoryController {
    static final String LIFECYCLE = "REGISTERED";
    static final String AVAILABILITY_REASON = "COMPILED_FIRST_PARTY";

    private final ModuleCatalogue catalogue;
    private final BuildRevision buildRevision;
    private final HttpProblems problems;

    public InventoryController(ModuleCatalogue catalogue, BuildRevision buildRevision, HttpProblems problems) {
        this.catalogue = catalogue;
        this.buildRevision = buildRevision;
        this.problems = problems;
    }

    @GetMapping
    ResponseEntity<?> inventory() {
        if (!catalogue.available()) {
            ProblemDetail problem = problems.create(HttpStatus.SERVICE_UNAVAILABLE,
                    HttpProblems.Kind.INVENTORY_UNAVAILABLE);
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                    .cacheControl(CacheControl.noStore())
                    .header("X-Content-Type-Options", "nosniff")
                    .body(problem);
        }
        List<ModuleView> modules = new ArrayList<>();
        for (ModuleCatalogue.Entry entry : catalogue.modules()) {
            ModuleManifest manifest = entry.manifest();
            modules.add(new ModuleView(manifest.id(), manifest.version(), entry.digest(), LIFECYCLE,
                    AVAILABILITY_REASON, manifest.dependencies(), manifest.capabilities(), manifest.permissions(),
                    manifest.dataOwnership().resources()));
        }
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(new InventoryView(buildRevision.value(), modules));
    }

    public record InventoryView(String buildRevision, List<ModuleView> modules) { }

    public record ModuleView(String id, String version, String digest, String lifecycle,
            String availabilityReason, List<ModuleManifest.Dependency> dependencies,
            ModuleManifest.Capabilities capabilities, List<ModuleManifest.Permission> permissions,
            List<ModuleManifest.OwnedResource> dataOwnership) { }
}
