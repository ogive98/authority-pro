package com.authoritypro.platform.access.domain;

import java.util.UUID;

/** One role granted to a user on a tenant, company or site node; null scope means the parent node. */
public record MembershipAssignment(UUID tenantId, UUID companyId, UUID siteId, RoleCode role,
        int authorizationRevision) {
    public MembershipAssignment {
        if (tenantId == null || role == null) throw new IllegalArgumentException("ASSIGNMENT_INVALID");
        if (siteId != null && companyId == null) throw new IllegalArgumentException("ASSIGNMENT_INVALID");
        if (authorizationRevision < 1) throw new IllegalArgumentException("ASSIGNMENT_INVALID");
    }
}
