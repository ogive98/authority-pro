package com.authoritypro.platform.access.application;

import com.authoritypro.platform.access.domain.Permission;
import com.authoritypro.platform.access.domain.RoleCode;
import java.util.Set;
import java.util.UUID;

/** Validated selected scope with member-visible labels and effective grants. */
public record ScopedAccess(
        UUID tenantId,
        String tenantName,
        UUID companyId,
        String companyName,
        int authorizationRevision,
        Set<RoleCode> roles,
        Set<Permission> permissions) {

    public ScopedAccess {
        if (tenantId == null || tenantName == null || tenantName.isBlank()) {
            throw new IllegalArgumentException("TENANT_REQUIRED");
        }
        if ((companyId == null) != (companyName == null || companyName.isBlank())) {
            throw new IllegalArgumentException("COMPANY_INCOMPLETE");
        }
        if (authorizationRevision < 1) throw new IllegalArgumentException("REVISION_INVALID");
        roles = Set.copyOf(roles == null ? Set.of() : roles);
        permissions = Set.copyOf(permissions == null ? Set.of() : permissions);
    }
}
