package com.authoritypro.platform.identity.infrastructure.persistence;

import com.authoritypro.platform.identity.application.ProvisioningStore;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

/** Bootstrap SQL. Only the provisioner role holds these grants; the runtime role cannot mutate identity. */
public final class JdbcProvisioningStore implements ProvisioningStore {
    @Override
    public boolean guardPresent(Connection connection) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT EXISTS (SELECT 1 FROM identity.provisioning_guard)")) {
            statement.setQueryTimeout(5);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() && result.getBoolean(1);
            }
        }
    }

    @Override
    public void insertGuard(Connection connection, UUID operatorUserId) throws SQLException {
        update(connection, "INSERT INTO identity.provisioning_guard (operator_user_id, revision) VALUES (?, 1)",
                operatorUserId);
    }

    @Override
    public void insertUser(Connection connection, UUID userId, String loginNormalized, String displayName)
            throws SQLException {
        update(connection, "INSERT INTO identity.users (user_id, login_normalized, display_name) VALUES (?, ?, ?)",
                userId, loginNormalized, displayName);
    }

    @Override
    public void insertCredential(Connection connection, UUID userId, String passwordHash) throws SQLException {
        update(connection, "INSERT INTO identity.password_credentials (user_id, password_hash, scheme) "
                + "VALUES (?, ?, 'bcrypt')", userId, passwordHash);
    }

    private static void update(Connection connection, String sql, Object... values) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setQueryTimeout(5);
            for (int index = 0; index < values.length; index++) statement.setObject(index + 1, values[index]);
            if (statement.executeUpdate() != 1) throw new SQLException("Unexpected provisioning write count");
        }
    }
}
