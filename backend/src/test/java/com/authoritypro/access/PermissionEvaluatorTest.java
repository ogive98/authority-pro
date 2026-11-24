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

import com.authoritypro.platform.access.domain.Permission;
import com.authoritypro.platform.access.domain.PermissionEvaluator;
import com.authoritypro.platform.access.domain.RoleCode;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class PermissionEvaluatorTest {
    private static final Set<Permission> OWNER = EnumSet.allOf(Permission.class);
    private static final Set<Permission> ADMIN =
            EnumSet.of(TENANCY_SETTINGS_MANAGE, ACCESS_MEMBERSHIP_MANAGE);
    private static final Set<Permission> AUDITOR =
            EnumSet.of(AUDIT_READ_TENANT, AUDIT_READ_CONTROL_PLANE, SECURITY_ATTEMPTS_READ);

    private final PermissionEvaluator evaluator = new PermissionEvaluator(Map.of(
            RoleCode.TENANT_OWNER, OWNER,
            RoleCode.TENANT_ADMIN, ADMIN,
            RoleCode.TENANT_AUDITOR, AUDITOR));

    @Test
    void eachRoleGrantsExactlyItsCatalogue() {
        assertEquals(6, evaluator.permissionsOf(Set.of(RoleCode.TENANT_OWNER)).size());
        assertEquals(ADMIN, evaluator.permissionsOf(Set.of(RoleCode.TENANT_ADMIN)));
        assertEquals(AUDITOR, evaluator.permissionsOf(Set.of(RoleCode.TENANT_AUDITOR)));
        assertTrue(evaluator.allows(Set.of(RoleCode.TENANT_AUDITOR), AUDIT_READ_TENANT));
        assertFalse(evaluator.allows(Set.of(RoleCode.TENANT_ADMIN), AUDIT_READ_TENANT));
        assertFalse(evaluator.allows(Set.of(RoleCode.TENANT_OWNER), null));
        assertTrue(evaluator.allows(Set.of(RoleCode.TENANT_OWNER), Permission.PLATFORM_INVENTORY_READ));
        assertFalse(evaluator.allows(Set.of(RoleCode.TENANT_ADMIN), Permission.PLATFORM_INVENTORY_READ));
        assertFalse(evaluator.allows(Set.of(RoleCode.TENANT_AUDITOR), Permission.PLATFORM_INVENTORY_READ));
    }

    @Test
    void deniesByDefaultWithoutRoles() {
        assertEquals(Set.of(), evaluator.permissionsOf(Set.of()));
        assertEquals(Set.of(), evaluator.permissionsOf(null));
        assertFalse(evaluator.allows(null, TENANCY_SETTINGS_MANAGE));
        assertFalse(evaluator.allows(Set.of(), TENANCY_SETTINGS_MANAGE));
    }

    @Test
    void unionsRolesAndResistsCatalogueMutation() {
        assertEquals(5, evaluator.permissionsOf(EnumSet.of(RoleCode.TENANT_ADMIN, RoleCode.TENANT_AUDITOR)).size());
        assertThrows(UnsupportedOperationException.class,
                () -> evaluator.permissionsOf(Set.of(RoleCode.TENANT_OWNER)).add(AUDIT_READ_TENANT));
    }

    @Test
    void parsingRejectsUnknownCodes() {
        assertThrows(IllegalArgumentException.class, () -> Permission.of("admin.*"));
        assertThrows(IllegalArgumentException.class, () -> RoleCode.of("TENANT_ROOT"));
        assertEquals(Permission.AUDIT_READ_TENANT, Permission.of("audit.read.tenant"));
        assertEquals(RoleCode.TENANT_AUDITOR, RoleCode.of("TENANT_AUDITOR"));
    }
}
