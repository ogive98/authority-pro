package com.authoritypro.platform.access.domain;

import java.util.Arrays;

/** v1 role catalogue; no wildcard, inheritance or implicit administrator (D6). */
public enum RoleCode {
    TENANT_OWNER,
    TENANT_ADMIN,
    TENANT_AUDITOR;

    public static RoleCode of(String code) {
        return Arrays.stream(values()).filter(role -> role.name().equals(code)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("ROLE_UNKNOWN"));
    }
}
