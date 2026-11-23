package com.authoritypro.platform.identity.infrastructure.persistence;

import com.authoritypro.platform.identity.application.ProvisioningAudit;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.UUID;

/**
 * Writes the pre-approved control-plane action code directly: identity cannot depend on the audit
 * module, and only controlled UUID values enter the metadata object.
 */
public final class JdbcProvisioningAudit implements ProvisioningAudit {
    private static final String ACTION_CODE = "identity.operator.provisioned";

    @Override
    public void operatorProvisioned(Connection connection, UUID operatorUserId, UUID operationId)
            throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO audit.control_plane_records (record_id, action_code, action_version, actor_id,
                    actor_kind, target_type, target_id, operation_id, correlation_id, schema_version, metadata)
                VALUES (?, ?, 1, ?, 'operator', 'operator', ?, ?, NULL, 1, ?::jsonb)
                RETURNING occurred_at
                """)) {
            statement.setQueryTimeout(5);
            statement.setObject(1, UUID.randomUUID());
            statement.setString(2, ACTION_CODE);
            statement.setObject(3, operatorUserId);
            statement.setString(4, operatorUserId.toString());
            statement.setObject(5, operationId);
            statement.setString(6, "{\"operatorRef\":\"" + operatorUserId + "\",\"revision\":\"1\"}");
            try (var result = statement.executeQuery()) {
                if (!result.next()) throw new SQLException("Missing provisioning audit result");
            }
        }
    }
}
