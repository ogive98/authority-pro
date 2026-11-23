package com.authoritypro.audit;

import com.authoritypro.platform.audit.domain.AuditAction;
import com.authoritypro.platform.audit.domain.AuditErrorCode;
import com.authoritypro.platform.audit.domain.AuditMetadata;
import com.authoritypro.platform.audit.domain.AuditRejectedException;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuditMetadataTest {
    private static Map<String, Object> keys(Object... pairs) {
        Map<String, Object> values = new LinkedHashMap<>();
        for (int i = 0; i < pairs.length; i += 2) values.put((String) pairs[i], pairs[i + 1]);
        return values;
    }

    @Test
    void settingsMetadataIsCanonicalAndAllowlisted() {
        AuditMetadata metadata = AuditMetadata.of(AuditAction.TENANCY_SETTINGS_UPDATED,
                keys("toRevision", 3, "changedKeys", List.of("displayName"), "fromRevision", 2));
        assertEquals("{\"changedKeys\":[\"displayName\"],\"fromRevision\":2,\"toRevision\":3}",
                metadata.encoded());
        assertEquals(2, metadata.values().get("fromRevision"));
    }

    @Test
    void missingOrUnknownKeysAreRejected() {
        rejected(AuditAction.TENANCY_SETTINGS_UPDATED, keys("changedKeys", List.of("a"), "toRevision", 2));
        rejected(AuditAction.TENANCY_SETTINGS_UPDATED,
                keys("changedKeys", List.of("a"), "fromRevision", 1, "toRevision", 2, "payload", "x"));
    }

    @Test
    void nestedAndSensitiveValuesAreRejected() {
        rejected(AuditAction.TENANCY_SETTINGS_UPDATED,
                keys("changedKeys", List.of("a"), "fromRevision", Map.of("secret", "x"), "toRevision", 2));
        rejected(AuditAction.IDENTITY_SESSION_REVOKED,
                keys("sessionRef", "Bearer abcdefghijklmnopqrstuvwxyz", "reasonCode", "manual"));
        rejected(AuditAction.IDENTITY_SESSION_REVOKED,
                keys("sessionRef", "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ4In0", "reasonCode", "manual"));
        rejected(AuditAction.IDENTITY_SESSION_REVOKED,
                keys("sessionRef", "ok", "reasonCode", "token=abcdef"));
    }

    @Test
    void countsAndRevisionBoundsAreEnforced() {
        rejected(AuditAction.ACCESS_MEMBERSHIP_CREATED,
                keys("membershipRef", "m-1", "companyRef", "c-1", "roleCode", "ADMIN", "revision", 0));
        rejected(AuditAction.ACCESS_MEMBERSHIP_CREATED,
                keys("membershipRef", "m-1", "companyRef", "c-1", "roleCode", "ADMIN", "revision", 1.5));
        AuditMetadata retention = AuditMetadata.of(AuditAction.AUDIT_RETENTION_EXECUTED,
                keys("policyRevision", 4, "windowStart", "2026-11-01T00:00:00Z",
                        "windowEnd", "2026-11-02T00:00:00Z", "removedCount", 0));
        assertTrue(retention.encoded().contains("\"removedCount\":0"));
        rejected(AuditAction.AUDIT_RETENTION_EXECUTED,
                keys("policyRevision", 4, "windowStart", "2026-11-02T00:00:00Z",
                        "windowEnd", "2026-11-01T00:00:00Z", "removedCount", 1));
    }

    private static void rejected(AuditAction action, Map<String, Object> values) {
        AuditRejectedException failure = assertThrows(AuditRejectedException.class,
                () -> AuditMetadata.of(action, values));
        assertEquals(AuditErrorCode.AUDIT_METADATA_REJECTED, failure.code());
    }
}
