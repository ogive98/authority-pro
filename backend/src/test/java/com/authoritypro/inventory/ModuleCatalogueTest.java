package com.authoritypro.inventory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.authoritypro.platform.runtime.application.ModuleCatalogue;
import com.authoritypro.platform.runtime.infrastructure.manifest.BuildRevisionReader;
import com.authoritypro.platform.runtime.infrastructure.manifest.ClasspathModuleCatalogueLoader;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class ModuleCatalogueTest {

    private final ModuleCatalogue catalogue = new ClasspathModuleCatalogueLoader().load();

    @Test
    void packagedCatalogueReflectsTheFiveCompiledRoots() {
        assertTrue(catalogue.available(), () -> "unavailable: " + catalogue.unavailableReason());
        List<String> ids = catalogue.modules().stream().map(entry -> entry.manifest().id()).toList();
        assertEquals(List.of("access", "audit", "identity", "runtime", "tenancy"), ids);
        for (ModuleCatalogue.Entry entry : catalogue.modules()) {
            assertEquals("0.1.0", entry.manifest().schemaVersion());
            assertEquals("0.1.0", entry.manifest().version());
            assertEquals("platform-module", entry.manifest().kind());
            assertEquals("first-party", entry.manifest().trust());
            assertEquals("compiled-first-party", entry.manifest().execution());
            assertTrue(entry.manifest().dependencies().isEmpty(), entry.manifest().id());
            assertTrue(entry.manifest().capabilities().provides().isEmpty(), entry.manifest().id());
            assertTrue(entry.manifest().capabilities().requires().isEmpty(), entry.manifest().id());
            assertTrue(entry.manifest().ui().contributions().isEmpty(), entry.manifest().id());
            assertTrue(entry.digest().matches("sha256:[0-9a-f]{64}"), entry.digest());
        }
        assertEquals(5, catalogue.modules().stream().map(ModuleCatalogue.Entry::digest)
                .collect(Collectors.toSet()).size());
    }

    @Test
    void permissionAndOwnershipDeclarationsMatchTheApprovedMap() {
        ModuleCatalogue.Entry access = entry("access");
        Set<String> permissions = access.manifest().permissions().stream()
                .map(permission -> permission.id() + "@" + permission.scope()).collect(Collectors.toSet());
        assertEquals(Set.of("audit.read.tenant@tenant", "audit.read.control_plane@control-plane",
                "security.attempts.read@control-plane", "tenancy.settings.manage@tenant",
                "access.membership.manage@tenant", "platform.inventory.read@control-plane"), permissions);
        assertEquals(Set.of("roles@control-plane", "role_permissions@control-plane", "membership_roles@tenant"),
                resources(access));
        assertEquals(Set.of("users@control-plane", "password_credentials@control-plane",
                "provisioning_guard@control-plane", "spring_session@control-plane",
                "spring_session_attributes@control-plane"), resources(entry("identity")));
        assertEquals(Set.of("tenants@tenant", "companies@tenant", "sites@tenant", "memberships@tenant"),
                resources(entry("tenancy")));
        assertEquals(Set.of("tenant_records@tenant", "control_plane_records@control-plane",
                "security_attempts@control-plane"), resources(entry("audit")));
        assertTrue(entry("runtime").manifest().dataOwnership().resources().isEmpty());
    }

    @Test
    void buildRevisionIsInjectedOrExplicitlyUnrecorded() {
        String revision = BuildRevisionReader.read().value();
        assertTrue(revision.matches("[A-Za-z0-9._-]{1,64}"), revision);
    }

    private ModuleCatalogue.Entry entry(String id) {
        return catalogue.modules().stream().filter(entry -> entry.manifest().id().equals(id)).findFirst()
                .orElseThrow();
    }

    private static Set<String> resources(ModuleCatalogue.Entry entry) {
        return entry.manifest().dataOwnership().resources().stream()
                .map(resource -> resource.name() + "@" + resource.scope()).collect(Collectors.toSet());
    }
}
