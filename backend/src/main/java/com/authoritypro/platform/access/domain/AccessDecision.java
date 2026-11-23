package com.authoritypro.platform.access.domain;

import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Resolves the roles effective for a selection: the assignment must match the tenant, the pinned
 * authorization revision, and stay within the selected company/site subtree. A stale revision or an
 * empty selection of assignments yields no role at all.
 */
public final class AccessDecision {
    private final PermissionEvaluator evaluator;

    public AccessDecision(PermissionEvaluator evaluator) {
        if (evaluator == null) throw new IllegalArgumentException("EVALUATOR_REQUIRED");
        this.evaluator = evaluator;
    }

    public Set<RoleCode> rolesFor(List<MembershipAssignment> assignments, MembershipSelection selection) {
        if (assignments == null || selection == null) return Set.of();
        var roles = EnumSet.noneOf(RoleCode.class);
        for (MembershipAssignment assignment : assignments) {
            if (assignment != null && applies(assignment, selection)) roles.add(assignment.role());
        }
        return Collections.unmodifiableSet(roles);
    }

    public Set<Permission> permissionsFor(List<MembershipAssignment> assignments, MembershipSelection selection) {
        return evaluator.permissionsOf(rolesFor(assignments, selection));
    }

    public boolean allows(List<MembershipAssignment> assignments, MembershipSelection selection,
            Permission permission) {
        return evaluator.allows(rolesFor(assignments, selection), permission);
    }

    private static boolean applies(MembershipAssignment assignment, MembershipSelection selection) {
        if (!assignment.tenantId().equals(selection.tenantId())) return false;
        if (assignment.authorizationRevision() != selection.authorizationRevision()) return false;
        if (assignment.companyId() == null) return true;
        if (!assignment.companyId().equals(selection.companyId())) return false;
        return assignment.siteId() == null || assignment.siteId().equals(selection.siteId());
    }
}
