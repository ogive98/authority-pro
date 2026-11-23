package com.authoritypro.platform.access.domain;

import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/** Evaluates explicit role grants loaded from access.role_permissions; unknown roles grant nothing. */
public final class PermissionEvaluator {
    private final Map<RoleCode, Set<Permission>> grants;

    public PermissionEvaluator(Map<RoleCode, Set<Permission>> grants) {
        if (grants == null) throw new IllegalArgumentException("GRANTS_REQUIRED");
        var copy = new EnumMap<RoleCode, Set<Permission>>(RoleCode.class);
        grants.forEach((role, permissions) -> {
            if (role == null || permissions == null) throw new IllegalArgumentException("GRANTS_INVALID");
            copy.put(role, Collections.unmodifiableSet(EnumSet.copyOf(permissions)));
        });
        this.grants = Collections.unmodifiableMap(copy);
    }

    public boolean allows(Set<RoleCode> roles, Permission permission) {
        if (roles == null || permission == null) return false;
        for (RoleCode role : roles) {
            if (grants.getOrDefault(role, Set.of()).contains(permission)) return true;
        }
        return false;
    }

    public Set<Permission> permissionsOf(Set<RoleCode> roles) {
        var result = EnumSet.noneOf(Permission.class);
        if (roles != null) {
            for (RoleCode role : roles) {
                result.addAll(grants.getOrDefault(role, Set.of()));
            }
        }
        return Collections.unmodifiableSet(result);
    }
}
