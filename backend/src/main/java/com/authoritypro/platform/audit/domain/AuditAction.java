package com.authoritypro.platform.audit.domain;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/** Approved v1 taxonomy. Unknown codes cannot be invented at runtime. */
public enum AuditAction {
    IDENTITY_OPERATOR_PROVISIONED("identity.operator.provisioned", Scope.CONTROL_PLANE, "operator",
            "operatorRef", "revision"),
    IDENTITY_SESSION_REVOKED("identity.session.revoked", Scope.BOTH, "session",
            "sessionRef", "reasonCode"),
    IDENTITY_SESSIONS_REVOKED_ALL("identity.sessions.revoked_all", Scope.TENANT, "subject",
            "subjectRef"),
    ACCESS_MEMBERSHIP_CREATED("access.membership.created", Scope.TENANT, "membership",
            "membershipRef", "companyRef", "roleCode", "revision"),
    ACCESS_MEMBERSHIP_UPDATED("access.membership.updated", Scope.TENANT, "membership",
            "membershipRef", "changedFields", "revision"),
    ACCESS_MEMBERSHIP_REVOKED("access.membership.revoked", Scope.TENANT, "membership",
            "membershipRef", "revision"),
    ACCESS_GRANT_CREATED("access.grant.created", Scope.TENANT, "grant",
            "grantRef", "permissionCode", "resourceType", "revision"),
    ACCESS_GRANT_REVOKED("access.grant.revoked", Scope.TENANT, "grant",
            "grantRef", "permissionCode", "resourceType", "revision"),
    TENANCY_COMPANY_CREATED("tenancy.company.created", Scope.TENANT, "company",
            "companyRef", "revision"),
    TENANCY_COMPANY_UPDATED("tenancy.company.updated", Scope.TENANT, "company",
            "companyRef", "changedFields", "revision"),
    TENANCY_SETTINGS_UPDATED("tenancy.settings.updated", Scope.TENANT, "settings",
            "changedKeys", "fromRevision", "toRevision"),
    RUNTIME_MODULE_LIFECYCLE_CHANGED("runtime.module.lifecycle.changed", Scope.CONTROL_PLANE, "module",
            "moduleId", "fromState", "toState", "descriptorDigest"),
    SECURITY_CONFIGURATION_CHANGED("security.configuration.changed", Scope.CONTROL_PLANE, "configuration",
            "keyNames", "fromRevision", "toRevision"),
    AUDIT_RETENTION_EXECUTED("audit.retention.executed", Scope.CONTROL_PLANE, "retention",
            "policyRevision", "windowStart", "windowEnd", "removedCount");

    public enum Scope { TENANT, CONTROL_PLANE, BOTH }

    private final String code;
    private final Scope scope;
    private final String targetType;
    private final Set<String> requiredMetadata;

    AuditAction(String code, Scope scope, String targetType, String... requiredMetadata) {
        this.code = code;
        this.scope = scope;
        this.targetType = targetType;
        this.requiredMetadata = Collections.unmodifiableSet(
                new LinkedHashSet<>(Arrays.asList(requiredMetadata)));
    }

    public String code() {
        return code;
    }

    public int version() {
        return 1;
    }

    public Scope scope() {
        return scope;
    }

    public String targetType() {
        return targetType;
    }

    public Set<String> requiredMetadata() {
        return requiredMetadata;
    }

    public boolean permits(AuditFamily family) {
        return scope == Scope.BOTH
                || (scope == Scope.TENANT && family == AuditFamily.TENANT)
                || (scope == Scope.CONTROL_PLANE && family == AuditFamily.CONTROL_PLANE);
    }

    public static AuditAction fromCode(String code) {
        for (AuditAction action : values()) {
            if (action.code.equals(code)) return action;
        }
        throw new AuditRejectedException(AuditErrorCode.AUDIT_INVALID_ACTION);
    }
}
