package com.authoritypro.platform.access.infrastructure.persistence;

import com.authoritypro.platform.access.application.InventoryAccessPolicy;
import com.authoritypro.platform.access.application.InventoryStoreUnavailableException;
import com.authoritypro.platform.access.domain.Permission;
import com.authoritypro.platform.access.domain.PermissionEvaluator;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.UUID;
import javax.sql.DataSource;

/**
 * Resolves {@code platform.inventory.read} from the database-loaded catalogue and active assignments.
 * A denial is recorded in {@code audit.security_attempts} in the same transaction; any read or
 * record failure fails closed as unavailable, never as granted.
 */
public final class JdbcInventoryAccessPolicy implements InventoryAccessPolicy {
    private static final String SOURCE_CLASS = "inventory";

    private final DataSource dataSource;
    private final JdbcAuthorizationCatalog catalog = new JdbcAuthorizationCatalog();

    public JdbcInventoryAccessPolicy(DataSource dataSource) {
        if (dataSource == null) throw new IllegalArgumentException("DATA_SOURCE_REQUIRED");
        this.dataSource = dataSource;
    }

    @Override
    public Decision decide(UUID userId) {
        if (userId == null) throw new InventoryStoreUnavailableException();
        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            try {
                installUserContext(connection, userId);
                PermissionEvaluator evaluator = new PermissionEvaluator(catalog.catalogue(connection));
                boolean granted = evaluator.anyAllows(catalog.assignments(connection, userId),
                        Permission.PLATFORM_INVENTORY_READ);
                if (!granted) recordAttempt(connection);
                connection.commit();
                return granted ? Decision.GRANTED : Decision.DENIED;
            } catch (SQLException | RuntimeException failure) {
                rollbackQuietly(connection);
                throw new InventoryStoreUnavailableException();
            }
        } catch (SQLException failure) {
            throw new InventoryStoreUnavailableException();
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
