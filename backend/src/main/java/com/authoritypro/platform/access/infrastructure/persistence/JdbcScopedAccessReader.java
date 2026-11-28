package com.authoritypro.platform.access.infrastructure.persistence;

import com.authoritypro.platform.access.application.AccessStoreUnavailableException;
import com.authoritypro.platform.access.application.ScopedAccess;
import com.authoritypro.platform.access.application.ScopedAccessReader;
import com.authoritypro.platform.access.application.ScopedGrant;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;
import javax.sql.DataSource;

/** Read-only scoped projection; the selector is validated before any label is returned. */
public final class JdbcScopedAccessReader implements ScopedAccessReader {
    private final DataSource dataSource;
    private final JdbcScopedGrantResolver resolver = new JdbcScopedGrantResolver();

    public JdbcScopedAccessReader(DataSource dataSource) {
        if (dataSource == null) throw new IllegalArgumentException("DATA_SOURCE_REQUIRED");
        this.dataSource = dataSource;
    }

    @Override
    public Optional<ScopedAccess> read(UUID userId, UUID tenantId, UUID companyId) {
        if (userId == null || tenantId == null) throw new AccessStoreUnavailableException();
        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            try {
                installUserContext(connection, userId);
                Optional<ScopedGrant> grant = resolver.resolve(connection, userId, tenantId, companyId);
                if (grant.isEmpty()) {
                    connection.rollback();
                    return Optional.empty();
                }
                String tenantName = tenantName(connection, userId, tenantId);
                if (tenantName == null) {
                    connection.rollback();
                    return Optional.empty();
                }
                String companyName = null;
                if (companyId != null) {
                    companyName = companyName(connection, tenantId, companyId);
                    if (companyName == null) {
                        connection.rollback();
                        return Optional.empty();
                    }
                }
                connection.commit();
                ScopedGrant resolved = grant.get();
                return Optional.of(new ScopedAccess(resolved.tenantId(), tenantName, resolved.companyId(),
                        companyName, resolved.authorizationRevision(), resolved.roles(), resolved.permissions()));
            } catch (SQLException | RuntimeException failure) {
                rollbackQuietly(connection);
                throw failure instanceof AccessStoreUnavailableException unavailable
                        ? unavailable
                        : new AccessStoreUnavailableException();
            }
        } catch (SQLException failure) {
            throw new AccessStoreUnavailableException();
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

    private static String tenantName(Connection connection, UUID userId, UUID tenantId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT t.name FROM tenancy.memberships m "
                        + "JOIN identity.users u ON u.user_id = m.user_id AND u.status = 'active' "
                        + "JOIN tenancy.tenants t ON t.tenant_id = m.tenant_id "
                        + "WHERE m.user_id = ? AND m.tenant_id = ? AND m.active AND m.disabled_at IS NULL")) {
            statement.setQueryTimeout(5);
            statement.setObject(1, userId);
            statement.setObject(2, tenantId);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? result.getString(1) : null;
            }
        }
    }

    private static String companyName(Connection connection, UUID tenantId, UUID companyId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT name FROM tenancy.companies WHERE tenant_id = ? AND company_id = ?")) {
            statement.setQueryTimeout(5);
            statement.setObject(1, tenantId);
            statement.setObject(2, companyId);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? result.getString(1) : null;
            }
        }
    }

    private static void rollbackQuietly(Connection connection) {
        try {
            connection.rollback();
        } catch (SQLException ignored) {
            // The original failure is reported; a lost rollback cannot be recovered here.
        }
    }
}
