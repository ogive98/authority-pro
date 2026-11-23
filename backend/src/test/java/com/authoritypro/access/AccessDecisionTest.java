package com.authoritypro.access;

import static com.authoritypro.platform.access.domain.Permission.ACCESS_MEMBERSHIP_MANAGE;
import static com.authoritypro.platform.access.domain.Permission.AUDIT_READ_CONTROL_PLANE;
import static com.authoritypro.platform.access.domain.Permission.AUDIT_READ_TENANT;
import static com.authoritypro.platform.access.domain.Permission.SECURITY_ATTEMPTS_READ;
import static com.authoritypro.platform.access.domain.Permission.TENANCY_SETTINGS_MANAGE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.authoritypro.platform.access.domain.AccessDecision;
import com.authoritypro.platform.access.domain.MembershipAssignment;
import com.authoritypro.platform.access.domain.MembershipSelection;
import com.authoritypro.platform.access.domain.Permission;
import com.authoritypro.platform.access.domain.PermissionEvaluator;
import com.authoritypro.platform.access.domain.RoleCode;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AccessDecisionTest {
    private static final UUID TENANT = UUID.randomUUID();
    private static final UUID OTHER_TENANT = UUID.randomUUID();
    private static final UUID COMPANY = UUID.randomUUID();
    private static final UUID SIBLING_COMPANY = UUID.randomUUID();
    private static final UUID SITE = UUID.randomUUID();
    private static final UUID OTHER_SITE = UUID.randomUUID();

    private final AccessDecision decision = new AccessDecision(new PermissionEvaluator(Map.of(
            RoleCode.TENANT_OWNER, EnumSet.allOf(Permission.class),
            RoleCode.TENANT_ADMIN, EnumSet.of(TENANCY_SETTINGS_MANAGE, ACCESS_MEMBERSHIP_MANAGE),
            RoleCode.TENANT_AUDITOR,
            EnumSet.of(AUDIT_READ_TENANT, AUDIT_READ_CONTROL_PLANE, SECURITY_ATTEMPTS_READ))));

    private static MembershipAssignment assign(RoleCode role, UUID company, UUID site, int revision) {
        return new MembershipAssignment(TENANT, company, site, role, revision);
    }

    @Test
    void tenantWideRoleAppliesToEveryDescendantSelection() {
        var assignments = List.of(assign(RoleCode.TENANT_ADMIN, null, null, 1));
        assertTrue(decision.allows(assignments, new MembershipSelection(TENANT, null, null, 1),
                TENANCY_SETTINGS_MANAGE));
        assertTrue(decision.allows(assignments, new MembershipSelection(TENANT, COMPANY, null, 1),
                TENANCY_SETTINGS_MANAGE));
        assertTrue(decision.allows(assignments, new MembershipSelection(TENANT, COMPANY, SITE, 1),
                TENANCY_SETTINGS_MANAGE));
        assertFalse(decision.allows(assignments, new MembershipSelection(TENANT, COMPANY, SITE, 1),
                AUDIT_READ_TENANT));
    }

    @Test
    void companyRoleStaysInsideItsSubtree() {
        var assignments = List.of(assign(RoleCode.TENANT_OWNER, COMPANY, null, 1));
        assertFalse(decision.allows(assignments, new MembershipSelection(TENANT, null, null, 1),
                TENANCY_SETTINGS_MANAGE));
        assertTrue(decision.allows(assignments, new MembershipSelection(TENANT, COMPANY, null, 1),
                TENANCY_SETTINGS_MANAGE));
        assertTrue(decision.allows(assignments, new MembershipSelection(TENANT, COMPANY, SITE, 1),
                TENANCY_SETTINGS_MANAGE));
        assertFalse(decision.allows(assignments, new MembershipSelection(TENANT, SIBLING_COMPANY, null, 1),
                TENANCY_SETTINGS_MANAGE));
    }

    @Test
    void siteRoleOnlyAppliesAtItsSite() {
        var assignments = List.of(assign(RoleCode.TENANT_AUDITOR, COMPANY, SITE, 1));
        assertEquals(Set.of(RoleCode.TENANT_AUDITOR),
                decision.rolesFor(assignments, new MembershipSelection(TENANT, COMPANY, SITE, 1)));
        assertFalse(decision.permissionsFor(assignments, new MembershipSelection(TENANT, COMPANY, SITE, 1))
                .isEmpty());
        assertTrue(decision.rolesFor(assignments, new MembershipSelection(TENANT, COMPANY, null, 1)).isEmpty());
        assertTrue(decision.rolesFor(assignments, new MembershipSelection(TENANT, COMPANY, OTHER_SITE, 1))
                .isEmpty());
    }

    @Test
    void staleRevisionGrantsNothing() {
        var assignments = List.of(assign(RoleCode.TENANT_OWNER, null, null, 1));
        assertEquals(Set.of(), decision.rolesFor(assignments, new MembershipSelection(TENANT, null, null, 2)));
        assertFalse(decision.allows(assignments, new MembershipSelection(TENANT, null, null, 2),
                AUDIT_READ_TENANT));
    }

    @Test
    void foreignTenantAndEmptyStateAreDeniedByDefault() {
        var assignments = List.of(assign(RoleCode.TENANT_OWNER, null, null, 1));
        assertFalse(decision.allows(assignments, new MembershipSelection(OTHER_TENANT, null, null, 1),
                AUDIT_READ_TENANT));
        assertEquals(Set.of(), decision.permissionsFor(List.of(), new MembershipSelection(TENANT, null, null, 1)));
        assertEquals(Set.of(), decision.permissionsFor(null, null));
        assertFalse(decision.allows(null, null, AUDIT_READ_TENANT));
    }

    @Test
    void selectionAndAssignmentRejectInconsistentScopes() {
        assertThrows(IllegalArgumentException.class, () -> new MembershipSelection(null, null, null, 1));
        assertThrows(IllegalArgumentException.class, () -> new MembershipSelection(TENANT, null, SITE, 1));
        assertThrows(IllegalArgumentException.class, () -> new MembershipSelection(TENANT, null, null, 0));
        assertThrows(IllegalArgumentException.class,
                () -> new MembershipAssignment(TENANT, null, SITE, RoleCode.TENANT_OWNER, 1));
        assertThrows(IllegalArgumentException.class,
                () -> new MembershipAssignment(null, null, null, RoleCode.TENANT_OWNER, 1));
    }
}
