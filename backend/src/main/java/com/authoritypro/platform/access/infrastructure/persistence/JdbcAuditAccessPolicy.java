package com.authoritypro.platform.access.infrastructure.persistence;

import com.authoritypro.platform.access.application.AccessStoreUnavailableException;
import com.authoritypro.platform.access.application.AuditAccessPolicy;
import com.authoritypro.platform.access.application.ScopedGrant;
import com.authoritypro.platform.access.domain.Permission;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;
import javax.sql.DataSource;

/**
 * Resolves {@code audit.read.tenant} for one selected scope. A refusal is recorded in
 * {@code audit.security_attempts} in the same transaction; any failure fails closed.
 */
public final class JdbcAuditAccessPolicy implements AuditAccessPolicy {
    private static final String SOURCE_CLASS = "audit";

    private final DataSource dataSource;
    private final JdbcScopedGrantResolver resolver = new JdbcScopedGrantResolver();

    public JdbcAuditAccessPolicy(DataSource dataSource) {
        if (dataSource == null) throw new IllegalArgumentException("DATA_SOURCE_REQUIRED");
        this.dataSource = dataSource;
    }

    @Override
    public Decision decide(UUID userId, UUID tenantId, UUID companyId) {
        if (userId == null || tenantId == null) throw new AccessStoreUnavailableException();
        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            try {
                installUserContext(connection, userId);
                Optional<ScopedGrant> grant = resolver.resolve(connection, userId, tenantId, companyId);
                boolean granted = grant.isPresent()
                        && grant.get().permissions().contains(Permission.AUDIT_READ_TENANT);
                if (!granted) recordAttempt(connection);
                connection.commit();
                return granted ? Decision.GRANTED : Decision.DENIED;
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

    private static void recordAttempt(Connection connection) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO audit.security_attempts (attempt_id, reason_code, source_class) "
                        + "VALUES (?, 'authorization_denied', ?)")) {
            statement.setQueryTimeout(5);
            statement.setObject(1, UUID.randomUUID());
            statement.setString(2, SOURCE_CLASS);
            statement.executeUpdate();
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
