package com.authoritypro.platform.identity.infrastructure.persistence;

import com.authoritypro.platform.identity.application.CredentialReader;
import com.authoritypro.platform.identity.application.IdentityAccessException;
import com.authoritypro.platform.identity.application.OwnProfile;
import com.authoritypro.platform.identity.application.OwnProfileReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import javax.sql.DataSource;

/**
 * Read-only facade over the runtime datasource. Own-membership discovery runs inside one
 * transaction that installs the internal user identifier for the V3 label policies; the HTTP
 * selector is never trusted here.
 */
public final class JdbcIdentityGateway implements CredentialReader, OwnProfileReader {
    private static final int MAX_MEMBERSHIPS = 25;
    private static final int MAX_COMPANIES = 100;
    private static final int MAX_ROLES = 100;
    private static final String MEMBERSHIPS = """
            SELECT m.tenant_id, t.name
            FROM tenancy.memberships m
            JOIN tenancy.tenants t ON t.tenant_id = m.tenant_id
            WHERE m.active AND m.disabled_at IS NULL
            ORDER BY lower(t.name), m.tenant_id
            LIMIT %d
            """.formatted(MAX_MEMBERSHIPS);
    private static final String COMPANIES = """
            SELECT c.tenant_id, c.company_id, c.name
            FROM tenancy.companies c
            WHERE EXISTS (SELECT 1 FROM tenancy.memberships m
                WHERE m.tenant_id = c.tenant_id AND m.active AND m.disabled_at IS NULL)
            ORDER BY lower(c.name), c.company_id
            LIMIT %d
            """.formatted(MAX_COMPANIES);
    private static final String ROLES = """
            SELECT mr.tenant_id, mr.role_code
            FROM access.membership_roles mr
            WHERE mr.user_id = ?
                AND EXISTS (SELECT 1 FROM tenancy.memberships m
                    WHERE m.user_id = mr.user_id AND m.tenant_id = mr.tenant_id
                        AND m.active AND m.disabled_at IS NULL)
            ORDER BY mr.tenant_id, mr.role_code
            LIMIT %d
            """.formatted(MAX_ROLES);

    private final DataSource dataSource;
    private final JdbcIdentityDirectory directory = new JdbcIdentityDirectory();

    public JdbcIdentityGateway(DataSource dataSource) {
        if (dataSource == null) throw new IllegalArgumentException("DATA_SOURCE_REQUIRED");
        this.dataSource = dataSource;
    }

    @Override
    public Optional<Credential> find(String normalizedLogin) {
        try (Connection connection = dataSource.getConnection()) {
            return directory.findCredential(connection, normalizedLogin)
                    .map(credential -> new Credential(credential.userId(), credential.displayName(),
                            credential.active(), credential.passwordHash(), credential.scheme()));
        } catch (SQLException failure) {
            throw IdentityAccessException.storeUnavailable(failure);
        }
    }

    @Override
    public Optional<OwnProfile> read(UUID userId) {
        if (userId == null) throw IdentityAccessException.refused();
        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            try {
                installUserContext(connection, userId);
                Optional<String[]> user = user(connection, userId);
                if (user.isEmpty()) {
                    connection.rollback();
                    return Optional.empty();
                }
                List<OwnProfile.Membership> memberships = memberships(connection,
                        roles(connection, userId), companies(connection));
                connection.commit();
                return Optional.of(new OwnProfile(userId, user.get()[1], user.get()[0], memberships));
            } catch (SQLException failure) {
                rollbackQuietly(connection);
                throw IdentityAccessException.storeUnavailable(failure);
            }
        } catch (SQLException failure) {
            throw IdentityAccessException.storeUnavailable(failure);
        }
    }

    private static void installUserContext(Connection connection, UUID userId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT set_config('app.user_id', ?, true)")) {
            statement.setQueryTimeout(5);
            statement.setString(1, userId.toString());
            statement.execute();
        }
    }

    private static Optional<String[]> user(Connection connection, UUID userId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT login_normalized, display_name FROM identity.users WHERE user_id = ? AND status = 'active'")) {
            statement.setQueryTimeout(5);
            statement.setObject(1, userId);
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) return Optional.empty();
                return Optional.of(new String[]{result.getString(1), result.getString(2)});
            }
        }
    }

    private static List<OwnProfile.Membership> memberships(Connection connection, Map<UUID, List<String>> roles,
            Map<UUID, List<OwnProfile.Company>> companies) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(MEMBERSHIPS)) {
            statement.setQueryTimeout(5);
            try (ResultSet result = statement.executeQuery()) {
                List<OwnProfile.Membership> memberships = new ArrayList<>();
                while (result.next()) {
                    UUID tenantId = result.getObject(1, UUID.class);
                    memberships.add(new OwnProfile.Membership(tenantId, result.getString(2),
                            roles.getOrDefault(tenantId, List.of()),
                            companies.getOrDefault(tenantId, List.of())));
                }
                return memberships;
            }
        }
    }

    private static Map<UUID, List<OwnProfile.Company>> companies(Connection connection) throws SQLException {
        Map<UUID, List<OwnProfile.Company>> byTenant = new LinkedHashMap<>();
        try (PreparedStatement statement = connection.prepareStatement(COMPANIES)) {
            statement.setQueryTimeout(5);
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    byTenant.computeIfAbsent(result.getObject(1, UUID.class), ignored -> new ArrayList<>())
                            .add(new OwnProfile.Company(result.getObject(2, UUID.class), result.getString(3)));
                }
            }
        }
        return byTenant;
    }

    private static Map<UUID, List<String>> roles(Connection connection, UUID userId) throws SQLException {
        Map<UUID, List<String>> byTenant = new HashMap<>();
        try (PreparedStatement statement = connection.prepareStatement(ROLES)) {
            statement.setQueryTimeout(5);
            statement.setObject(1, userId);
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    byTenant.computeIfAbsent(result.getObject(1, UUID.class), ignored -> new ArrayList<>())
                            .add(result.getString(2));
                }
            }
        }
        return byTenant;
    }

    private static void rollbackQuietly(Connection connection) {
        try {
            connection.rollback();
        } catch (SQLException ignored) {
            // The original failure is reported; a lost rollback cannot be recovered here.
        }
    }
}
