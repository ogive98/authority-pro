package com.authoritypro.platform.access.domain;

import java.util.UUID;

/** Tenant-scoped working selection; a site selection always names its company, and the revision pins the grant state. */
public record MembershipSelection(UUID tenantId, UUID companyId, UUID siteId, int authorizationRevision) {
    public MembershipSelection {
        if (tenantId == null) throw new IllegalArgumentException("TENANT_REQUIRED");
        if (siteId != null && companyId == null) throw new IllegalArgumentException("COMPANY_REQUIRED");
        if (authorizationRevision < 1) throw new IllegalArgumentException("REVISION_INVALID");
    }
}
