package com.authoritypro.platform.access.domain;

import java.util.Arrays;

/** Deny-by-default catalogue; v1 has exactly these five named permissions (D6). */
public enum Permission {
    AUDIT_READ_TENANT("audit.read.tenant"),
    AUDIT_READ_CONTROL_PLANE("audit.read.control_plane"),
    SECURITY_ATTEMPTS_READ("security.attempts.read"),
    TENANCY_SETTINGS_MANAGE("tenancy.settings.manage"),
    ACCESS_MEMBERSHIP_MANAGE("access.membership.manage");

    private final String code;

    Permission(String code) {
        this.code = code;
    }

    public String code() {
        return code;
    }

    public static Permission of(String code) {
        return Arrays.stream(values()).filter(permission -> permission.code.equals(code)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("PERMISSION_UNKNOWN"));
    }
}
