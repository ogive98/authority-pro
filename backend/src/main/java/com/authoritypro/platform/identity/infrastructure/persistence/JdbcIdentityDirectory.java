package com.authoritypro.platform.identity.infrastructure.persistence;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;

/** Login-plane read only: runtime has SELECT on identity.users and identity.password_credentials. */
public final class JdbcIdentityDirectory {
    public record Credential(UUID userId, String displayName, boolean active, int authorizationRevision,
            String passwordHash, String scheme) { }

    public Optional<Credential> findCredential(Connection connection, String normalizedLogin) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT u.user_id, u.display_name, u.status = 'active', u.authorization_revision, "
                        + "p.password_hash, p.scheme FROM identity.users u "
                        + "JOIN identity.password_credentials p ON p.user_id = u.user_id "
                        + "WHERE u.login_normalized = ?")) {
            statement.setQueryTimeout(5);
            statement.setString(1, normalizedLogin);
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) return Optional.empty();
                return Optional.of(new Credential(result.getObject(1, UUID.class), result.getString(2),
                        result.getBoolean(3), result.getInt(4), result.getString(5), result.getString(6)));
            }
        }
    }
}
