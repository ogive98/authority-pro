package com.authoritypro.platform.audit.infrastructure.persistence;

import com.authoritypro.platform.audit.application.AuditReadUnavailableException;
import com.authoritypro.platform.audit.application.AuditRecordReader;
import com.authoritypro.platform.audit.domain.AuditErrorCode;
import com.authoritypro.platform.audit.domain.AuditFamily;
import com.authoritypro.platform.audit.domain.AuditQuery;
import com.authoritypro.platform.audit.domain.AuditRecordView;
import com.authoritypro.platform.audit.domain.AuditRejectedException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;
import javax.sql.DataSource;

/** Installs the validated user/tenant context, then reads family A through the existing store. */
public final class JdbcAuditRecordReader implements AuditRecordReader {
    private final DataSource dataSource;
    private final JdbcAuditStore store = new JdbcAuditStore();

    public JdbcAuditRecordReader(DataSource dataSource) {
        if (dataSource == null) throw new IllegalArgumentException("DATA_SOURCE_REQUIRED");
        this.dataSource = dataSource;
    }

    @Override
    public List<AuditRecordView> read(UUID userId, UUID tenantId, UUID companyId, AuditQuery query) {
        if (userId == null || tenantId == null || query == null) {
            throw new AuditReadUnavailableException();
        }
        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            try {
                installContext(connection, userId, tenantId);
                List<AuditRecordView> records = store.read(connection, AuditFamily.TENANT, query);
                connection.commit();
                return records;
            } catch (AuditRejectedException rejected) {
                rollbackQuietly(connection);
                if (rejected.code() == AuditErrorCode.AUDIT_PERSISTENCE_FAILED) {
                    throw new AuditReadUnavailableException();
                }
                throw rejected;
            } catch (SQLException | RuntimeException failure) {
                rollbackQuietly(connection);
                throw new AuditReadUnavailableException();
            }
        } catch (SQLException failure) {
            throw new AuditReadUnavailableException();
        }
    }

    private static void installContext(Connection connection, UUID userId, UUID tenantId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT set_config('app.user_id', ?, true), set_config('app.tenant_id', ?, true)")) {
            statement.setQueryTimeout(5);
            statement.setString(1, userId.toString());
            statement.setString(2, tenantId.toString());
            statement.execute();
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
