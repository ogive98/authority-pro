package com.authoritypro.platform.access.infrastructure.persistence;

import com.authoritypro.platform.access.domain.MembershipAssignment;
import com.authoritypro.platform.access.domain.Permission;
import com.authoritypro.platform.access.domain.RoleCode;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Reads the v1 grant catalogue and active membership assignments; unknown codes grant nothing. */
public final class JdbcAuthorizationCatalog {
    public Map<RoleCode, Set<Permission>> catalogue(Connection connection) throws SQLException {
        var grants = new EnumMap<RoleCode, Set<Permission>>(RoleCode.class);
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT role_code, permission_code FROM access.role_permissions")) {
            statement.setQueryTimeout(5);
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    RoleCode role = parse(RoleCode::of, result.getString(1));
                    Permission permission = parse(Permission::of, result.getString(2));
                    if (role != null && permission != null) {
                        grants.computeIfAbsent(role, key -> EnumSet.noneOf(Permission.class)).add(permission);
                    }
                }
            }
        }
        var copy = new EnumMap<RoleCode, Set<Permission>>(RoleCode.class);
        grants.forEach((role, permissions) -> copy.put(role, Collections.unmodifiableSet(permissions)));
        return Collections.unmodifiableMap(copy);
    }

    public List<MembershipAssignment> assignments(Connection connection, UUID userId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT mr.tenant_id, mr.company_id, mr.site_id, mr.role_code, mr.authorization_revision "
                        + "FROM access.membership_roles mr "
                        + "JOIN tenancy.memberships m ON m.user_id = mr.user_id AND m.tenant_id = mr.tenant_id "
                        + "WHERE mr.user_id = ? AND m.active AND m.disabled_at IS NULL "
                        + "ORDER BY mr.tenant_id, mr.role_code")) {
            statement.setQueryTimeout(5);
            statement.setObject(1, userId);
            try (ResultSet result = statement.executeQuery()) {
                var assignments = new ArrayList<MembershipAssignment>();
                while (result.next()) {
                    RoleCode role = parse(RoleCode::of, result.getString(4));
                    if (role == null) continue;
                    assignments.add(new MembershipAssignment(result.getObject(1, UUID.class),
                            result.getObject(2, UUID.class), result.getObject(3, UUID.class), role,
                            result.getInt(5)));
                }
                return List.copyOf(assignments);
            }
        }
    }

    private static <T> T parse(java.util.function.Function<String, T> parser, String code) {
        try {
            return code == null ? null : parser.apply(code);
        } catch (IllegalArgumentException unknown) {
            return null;
        }
    }
}
