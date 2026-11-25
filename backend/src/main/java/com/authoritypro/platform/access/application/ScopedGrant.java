package com.authoritypro.platform.access.application;

import com.authoritypro.platform.access.domain.Permission;
import com.authoritypro.platform.access.domain.RoleCode;
import java.util.Set;
import java.util.UUID;

/** Effective roles and permissions for one validated tenant/company selection. */
public record ScopedGrant(
        UUID tenantId,
        UUID companyId,
        int authorizationRevision,
        Set<RoleCode> roles,
        Set<Permission> permissions) {

    public ScopedGrant {
        if (tenantId == null) throw new IllegalArgumentException("TENANT_REQUIRED");
        if (authorizationRevision < 1) throw new IllegalArgumentException("REVISION_INVALID");
        roles = Set.copyOf(roles == null ? Set.of() : roles);
        permissions = Set.copyOf(permissions == null ? Set.of() : permissions);
    }
}
