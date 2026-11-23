package com.authoritypro.platform.audit.domain;

/** Tenant and control-plane records are physically separate families. */
public enum AuditFamily {
    TENANT,
    CONTROL_PLANE
}
