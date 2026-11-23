package com.authoritypro.audit;

import com.authoritypro.platform.audit.domain.AuditAction;
import com.authoritypro.platform.audit.domain.AuditActorKind;
import com.authoritypro.platform.audit.domain.AuditErrorCode;
import com.authoritypro.platform.audit.domain.AuditEvent;
import com.authoritypro.platform.audit.domain.AuditFamily;
import com.authoritypro.platform.audit.domain.AuditMetadata;
import com.authoritypro.platform.audit.domain.AuditQuery;
import com.authoritypro.platform.audit.domain.AuditRejectedException;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AuditEventTest {
    private static final UUID USER = UUID.fromString("20000000-0000-0000-0000-000000000001");
    private static final UUID TENANT = UUID.fromString("10000000-0000-0000-0000-000000000001");
    private static final UUID COMPANY = UUID.fromString("30000000-0000-0000-0000-000000000001");
    private static final AuditMetadata SETTINGS = AuditMetadata.of(AuditAction.TENANCY_SETTINGS_UPDATED,
            Map.of("changedKeys", List.of("displayName"), "fromRevision", 1, "toRevision", 2));

    @Test
    void tenantAndControlPlaneFamiliesCannotBeConfused() {
        AuditEvent tenant = new AuditEvent(AuditAction.TENANCY_SETTINGS_UPDATED, USER, AuditActorKind.USER,
                "settings-1", null, null, TENANT, COMPANY, null, SETTINGS);
        assertEquals(AuditFamily.TENANT, tenant.family());

        AuditMetadata module = AuditMetadata.of(AuditAction.RUNTIME_MODULE_LIFECYCLE_CHANGED,
                Map.of("moduleId", "orders", "fromState", "STOPPED", "toState", "STARTED",
                        "descriptorDigest", "a".repeat(64)));
        AuditEvent control = new AuditEvent(AuditAction.RUNTIME_MODULE_LIFECYCLE_CHANGED, USER,
                AuditActorKind.OPERATOR, "orders-1", null, null, null, null, null, module);
        assertEquals(AuditFamily.CONTROL_PLANE, control.family());

        AuditRejectedException tenantActionWithoutTenant = assertThrows(AuditRejectedException.class,
                () -> new AuditEvent(AuditAction.TENANCY_SETTINGS_UPDATED, USER, AuditActorKind.USER,
                        "settings-1", null, null, null, null, null, SETTINGS));
        assertEquals(AuditErrorCode.AUDIT_SCOPE_MISMATCH, tenantActionWithoutTenant.code());

        AuditRejectedException controlActionInTenant = assertThrows(AuditRejectedException.class,
                () -> new AuditEvent(AuditAction.RUNTIME_MODULE_LIFECYCLE_CHANGED, USER,
                        AuditActorKind.OPERATOR, "orders-1", null, null, TENANT, null, null, module));
        assertEquals(AuditErrorCode.AUDIT_SCOPE_MISMATCH, controlActionInTenant.code());
    }

    @Test
    void siteRequiresCompanyAndTargetMustBeOpaque() {
        assertThrows(AuditRejectedException.class, () -> new AuditEvent(AuditAction.TENANCY_SETTINGS_UPDATED,
                USER, AuditActorKind.USER, "settings-1", null, null, TENANT, null, COMPANY, SETTINGS));
        assertThrows(AuditRejectedException.class, () -> new AuditEvent(AuditAction.TENANCY_SETTINGS_UPDATED,
                USER, AuditActorKind.USER, "not a target", null, null, TENANT, null, null, SETTINGS));
    }

    @Test
    void queriesStayBoundedAndCursorIsAtomic() {
        assertEquals(100, AuditQuery.firstPage(100).pageSize());
        assertThrows(IllegalArgumentException.class, () -> AuditQuery.firstPage(0));
        assertThrows(IllegalArgumentException.class, () -> AuditQuery.firstPage(101));
        assertThrows(IllegalArgumentException.class, () -> new AuditQuery(null, "settings", null, null,
                null, null, null, null, null, 10));
        Instant now = Instant.parse("2026-11-21T10:00:00Z");
        assertThrows(IllegalArgumentException.class, () -> new AuditQuery(null, null, null, null, now,
                now.minusSeconds(1), null, null, null, 10));
        assertThrows(IllegalArgumentException.class, () -> new AuditQuery(null, null, null, null, null,
                null, null, now, null, 10));
        assertThrows(IllegalArgumentException.class, () -> new AuditQuery(AuditAction.ACCESS_GRANT_CREATED,
                "settings", "settings-1", null, null, null, null, null, null, 10));
        AuditQuery page = new AuditQuery(AuditAction.TENANCY_SETTINGS_UPDATED, "settings", "settings-1",
                USER, now.minusSeconds(60), now, COMPANY, now, UUID.randomUUID(), 25);
        assertEquals(25, page.pageSize());
    }
}
