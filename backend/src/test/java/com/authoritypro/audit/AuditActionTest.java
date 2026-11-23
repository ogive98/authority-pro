package com.authoritypro.audit;

import com.authoritypro.platform.audit.domain.AuditAction;
import com.authoritypro.platform.audit.domain.AuditErrorCode;
import com.authoritypro.platform.audit.domain.AuditFamily;
import com.authoritypro.platform.audit.domain.AuditRejectedException;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuditActionTest {
    @Test
    void taxonomyHasFourteenUniqueCodesAtVersionOne() {
        Set<String> codes = new HashSet<>();
        for (AuditAction action : AuditAction.values()) {
            assertTrue(codes.add(action.code()));
            assertEquals(1, action.version());
            assertFalse(action.requiredMetadata().isEmpty());
        }
        assertEquals(14, codes.size());
        assertEquals(14, AuditAction.values().length);
    }

    @Test
    void scopePermitsOnlyTheApprovedFamilies() {
        assertTrue(AuditAction.TENANCY_SETTINGS_UPDATED.permits(AuditFamily.TENANT));
        assertFalse(AuditAction.TENANCY_SETTINGS_UPDATED.permits(AuditFamily.CONTROL_PLANE));
        assertTrue(AuditAction.SECURITY_CONFIGURATION_CHANGED.permits(AuditFamily.CONTROL_PLANE));
        assertFalse(AuditAction.SECURITY_CONFIGURATION_CHANGED.permits(AuditFamily.TENANT));
        assertTrue(AuditAction.IDENTITY_SESSION_REVOKED.permits(AuditFamily.TENANT));
        assertTrue(AuditAction.IDENTITY_SESSION_REVOKED.permits(AuditFamily.CONTROL_PLANE));
    }

    @Test
    void unknownCodeIsRejectedBeforeAnyWrite() {
        AuditRejectedException failure = assertThrows(AuditRejectedException.class,
                () -> AuditAction.fromCode("business.order.created"));
        assertEquals(AuditErrorCode.AUDIT_INVALID_ACTION, failure.code());
        assertEquals("AUDIT_INVALID_ACTION", failure.getMessage());
    }
}
