package com.authoritypro.platform.audit.infrastructure.persistence;

import com.authoritypro.platform.audit.application.AuditStore;
import com.authoritypro.platform.audit.domain.AuditActorKind;
import com.authoritypro.platform.audit.domain.AuditErrorCode;
import com.authoritypro.platform.audit.domain.AuditEvent;
import com.authoritypro.platform.audit.domain.AuditFamily;
import com.authoritypro.platform.audit.domain.AuditQuery;
import com.authoritypro.platform.audit.domain.AuditRecordView;
import com.authoritypro.platform.audit.domain.AuditRejectedException;
import com.authoritypro.platform.audit.domain.FailedAttempt;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.postgresql.util.PGobject;

/** JDBC adapter: no connection is opened here, so the caller's transaction and scope are kept. */
public final class JdbcAuditStore implements AuditStore {
    private static final String TENANT_INSERT = """
            INSERT INTO audit.tenant_records (record_id, action_code, action_version, actor_id, actor_kind,
                target_type, target_id, operation_id, correlation_id, schema_version, metadata, tenant_id,
                company_id, site_id)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            RETURNING occurred_at
            """;
    private static final String CONTROL_INSERT = """
            INSERT INTO audit.control_plane_records (record_id, action_code, action_version, actor_id,
                actor_kind, target_type, target_id, operation_id, correlation_id, schema_version, metadata)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            RETURNING occurred_at
            """;
    private static final String TENANT_SELECT = """
            SELECT record_id, occurred_at, action_code, action_version, actor_id, actor_kind, target_type,
                target_id, operation_id, correlation_id, schema_version, metadata::text, outcome,
                tenant_id, company_id, site_id
            FROM audit.tenant_records
            """;
    private static final String CONTROL_SELECT = """
            SELECT record_id, occurred_at, action_code, action_version, actor_id, actor_kind, target_type,
                target_id, operation_id, correlation_id, schema_version, metadata::text, outcome,
                NULL::uuid, NULL::uuid, NULL::uuid
            FROM audit.control_plane_records
            """;

    @Override
    public AuditRecordView appendCritical(Connection connection, AuditEvent event) {
        if (connection == null || event == null) {
            throw new IllegalArgumentException("AUDIT_INPUT_INVALID");
        }
        AuditFamily family = event.family();
        try {
            if (family == AuditFamily.TENANT) requireMembership(connection, event);
            UUID recordId = UUID.randomUUID();
            String sql = family == AuditFamily.TENANT ? TENANT_INSERT : CONTROL_INSERT;
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                int index = 1;
                statement.setObject(index++, recordId);
                statement.setString(index++, event.action().code());
                statement.setInt(index++, event.action().version());
                statement.setObject(index++, event.actorId());
                statement.setString(index++, event.actorKind().databaseValue());
                statement.setString(index++, event.action().targetType());
                statement.setString(index++, event.targetId());
                statement.setObject(index++, event.operationId());
                statement.setObject(index++, event.correlationId());
                statement.setInt(index++, AuditEvent.SCHEMA_VERSION);
                statement.setObject(index++, json(event.metadata().encoded()));
                if (family == AuditFamily.TENANT) {
                    statement.setObject(index++, event.tenantId());
                    statement.setObject(index++, event.companyId());
                    statement.setObject(index, event.siteId());
                }
                try (ResultSet result = statement.executeQuery()) {
                    if (!result.next()) throw new SQLException("Missing audit insert result");
                    OffsetDateTime occurredAt = result.getObject(1, OffsetDateTime.class);
                    return new AuditRecordView(recordId, occurredAt.toInstant(), event.action().code(),
                            event.action().version(), event.actorId(), event.actorKind(),
                            event.action().targetType(), event.targetId(), event.operationId(),
                            event.correlationId(), AuditEvent.SCHEMA_VERSION, event.metadata().encoded(),
                            "committed", event.tenantId(), event.companyId(), event.siteId());
                }
            }
        } catch (SQLException failure) {
            throw persistence(failure);
        }
    }

    @Override
    public List<AuditRecordView> read(Connection connection, AuditFamily family, AuditQuery query) {
        if (connection == null || family == null || query == null) {
            throw new IllegalArgumentException("AUDIT_INPUT_INVALID");
        }
        if (family == AuditFamily.CONTROL_PLANE && query.companyId() != null) {
            throw new IllegalArgumentException("AUDIT_COMPANY_FILTER_INVALID");
        }
        StringBuilder sql = new StringBuilder(family == AuditFamily.TENANT ? TENANT_SELECT : CONTROL_SELECT)
                .append(" WHERE 1 = 1");
        List<Object> values = new ArrayList<>();
        if (query.cursorOccurredAt() != null) {
            sql.append(" AND (occurred_at, record_id) < (?, ?)");
            values.add(OffsetDateTime.ofInstant(query.cursorOccurredAt(), ZoneOffset.UTC));
            values.add(query.cursorRecordId());
        }
        if (query.action() != null) {
            sql.append(" AND action_code = ?");
            values.add(query.action().code());
        }
        if (query.targetType() != null) {
            sql.append(" AND target_type = ? AND target_id = ?");
            values.add(query.targetType());
            values.add(query.targetId());
        }
        if (query.actorId() != null) {
            sql.append(" AND actor_id = ?");
            values.add(query.actorId());
        }
        if (query.from() != null) {
            sql.append(" AND occurred_at >= ?");
            values.add(OffsetDateTime.ofInstant(query.from(), ZoneOffset.UTC));
        }
        if (query.to() != null) {
            sql.append(" AND occurred_at <= ?");
            values.add(OffsetDateTime.ofInstant(query.to(), ZoneOffset.UTC));
        }
        if (query.companyId() != null) {
            sql.append(" AND company_id = ?");
            values.add(query.companyId());
        }
        sql.append(" ORDER BY occurred_at DESC, record_id DESC LIMIT ?");
        values.add(query.pageSize());
        try (PreparedStatement statement = connection.prepareStatement(sql.toString())) {
            for (int i = 0; i < values.size(); i++) statement.setObject(i + 1, values.get(i));
            try (ResultSet result = statement.executeQuery()) {
                List<AuditRecordView> records = new ArrayList<>();
                while (result.next()) records.add(view(result));
                return List.copyOf(records);
            }
        } catch (SQLException failure) {
            throw persistence(failure);
        }
    }

    @Override
    public UUID recordFailedAttempt(Connection connection, FailedAttempt attempt) {
        if (connection == null || attempt == null) {
            throw new IllegalArgumentException("AUDIT_INPUT_INVALID");
        }
        UUID attemptId = UUID.randomUUID();
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO audit.security_attempts (attempt_id, reason_code, correlation_id, source_class)
                VALUES (?, ?, ?, ?)
                """)) {
            statement.setObject(1, attemptId);
            statement.setString(2, attempt.reason().code());
            statement.setObject(3, attempt.correlationId());
            statement.setString(4, attempt.sourceClass());
            if (statement.executeUpdate() != 1) throw new SQLException("Unexpected attempt insert count");
            return attemptId;
        } catch (SQLException failure) {
            throw persistence(failure);
        }
    }

    private static void requireMembership(Connection connection, AuditEvent event) throws SQLException {
        if (event.actorKind() != AuditActorKind.USER) {
            throw new AuditRejectedException(AuditErrorCode.AUDIT_SCOPE_MISMATCH);
        }
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT count(*) FROM tenancy.memberships
                WHERE user_id = ? AND tenant_id = ? AND active
                """)) {
            statement.setObject(1, event.actorId());
            statement.setObject(2, event.tenantId());
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next() || result.getLong(1) != 1) {
                    throw new AuditRejectedException(AuditErrorCode.AUDIT_SCOPE_MISMATCH);
                }
            }
        }
    }

    private static AuditRecordView view(ResultSet result) throws SQLException {
        return new AuditRecordView(
                result.getObject(1, UUID.class),
                result.getObject(2, OffsetDateTime.class).toInstant(),
                result.getString(3),
                result.getInt(4),
                result.getObject(5, UUID.class),
                AuditActorKind.fromDatabase(result.getString(6)),
                result.getString(7),
                result.getString(8),
                result.getObject(9, UUID.class),
                result.getObject(10, UUID.class),
                result.getInt(11),
                result.getString(12),
                result.getString(13),
                result.getObject(14, UUID.class),
                result.getObject(15, UUID.class),
                result.getObject(16, UUID.class));
    }

    private static PGobject json(String value) throws SQLException {
        PGobject object = new PGobject();
        object.setType("jsonb");
        object.setValue(value);
        return object;
    }

    private static AuditRejectedException persistence(SQLException failure) {
        return new AuditRejectedException(AuditErrorCode.AUDIT_PERSISTENCE_FAILED, failure);
    }
}
