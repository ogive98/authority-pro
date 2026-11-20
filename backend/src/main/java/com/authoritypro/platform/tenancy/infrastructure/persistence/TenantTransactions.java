package com.authoritypro.platform.tenancy.infrastructure.persistence;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

import javax.sql.DataSource;

import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

/** UUIDs are trusted internal context, not HTTP input or authorization proof. */
public final class TenantTransactions {
    private final DataSource source;
    private final DataSourceTransactionManager manager;

    public TenantTransactions(DataSource source) {
        if (source == null) throw new IllegalArgumentException("Data source is required");
        this.source = source;
        this.manager = new DataSourceTransactionManager(source);
        this.manager.setEnforceReadOnly(true);
        this.manager.setValidateExistingTransaction(true);
    }

    public <T> T scoped(UUID user, UUID tenant, boolean readOnly, Work<T> work) {
        return execute(scope(user, tenant, false), readOnly, TransactionDefinition.PROPAGATION_REQUIRED, work);
    }

    public <T> T requiresNew(UUID user, UUID tenant, boolean readOnly, Work<T> work) {
        return execute(scope(user, tenant, false), readOnly, TransactionDefinition.PROPAGATION_REQUIRES_NEW, work);
    }

    /** Pre-tenant scope permits only the user's own membership lookup through RLS/grants. */
    public <T> T userScoped(UUID user, boolean readOnly, Work<T> work) {
        return execute(scope(user, null, true), readOnly, TransactionDefinition.PROPAGATION_REQUIRED, work);
    }

    /** Callbacks must not close, commit, roll back, or change their connection's role/context. */
    @FunctionalInterface
    public interface Work<T> {
        T run(Connection connection) throws SQLException;
    }

    private static Scope scope(UUID user, UUID tenant, boolean preTenant) {
        if (user == null || (!preTenant && tenant == null)) {
            throw new IllegalArgumentException("Scope UUIDs are required");
        }
        return new Scope(user.toString(), tenant == null ? "" : tenant.toString());
    }

    private <T> T execute(Scope scope, boolean readOnly, int propagation, Work<T> work) {
        if (work == null) throw new IllegalArgumentException("Work is required");
        if (propagation == TransactionDefinition.PROPAGATION_REQUIRED
                && TransactionSynchronizationManager.isActualTransactionActive()
                && !TransactionSynchronizationManager.hasResource(source)) {
            throw new IllegalStateException("Cannot join an unrelated JDBC transaction");
        }
        // Reject a scope switch before a participating template can mark the parent rollback-only.
        if (propagation == TransactionDefinition.PROPAGATION_REQUIRED
                && TransactionSynchronizationManager.isActualTransactionActive()
                && TransactionSynchronizationManager.hasResource(source)) {
            Connection joined = DataSourceUtils.getConnection(source);
            try {
                validateJoined(joined, scope, readOnly);
            } catch (SQLException failure) {
                throw new IllegalStateException("Cannot inspect transaction scope", failure);
            } finally {
                DataSourceUtils.releaseConnection(joined, source);
            }
        }
        TransactionTemplate template = new TransactionTemplate(manager);
        template.setPropagationBehavior(propagation);
        template.setReadOnly(readOnly);
        template.setTimeout(25);
        return template.execute(status -> {
            Connection connection = DataSourceUtils.getConnection(source);
            try {
                if (connection.getAutoCommit()
                        || !TransactionSynchronizationManager.isActualTransactionActive()
                        || !TransactionSynchronizationManager.isSynchronizationActive()) {
                    throw new IllegalStateException("An active JDBC transaction is required");
                }
                validateJoined(connection, scope, readOnly);
                Connection physical = physical(connection);
                if (marker(physical) == null) {
                    initialize(connection, scope);
                    TransactionSynchronizationManager.registerSynchronization(new ScopeMarker(physical, scope));
                }
                return work.run(connection);
            } catch (SQLException failure) {
                throw new IllegalStateException("Scoped JDBC transaction failed", failure);
            } finally {
                DataSourceUtils.releaseConnection(connection, source);
            }
        });
    }

    private static void validateJoined(Connection connection, Scope scope, boolean readOnly) throws SQLException {
        if (TransactionSynchronizationManager.isCurrentTransactionReadOnly() != readOnly) {
            throw new IllegalStateException("Joined transaction cannot change read-only mode");
        }
        ScopeMarker existing = marker(physical(connection));
        if (existing != null && !existing.scope.equals(scope)) {
            throw new IllegalArgumentException("Joined transaction cannot change scope");
        }
    }

    private static Connection physical(Connection connection) throws SQLException {
        return DataSourceUtils.getTargetConnection(connection).unwrap(Connection.class);
    }

    private static ScopeMarker marker(Connection physical) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) return null;
        for (TransactionSynchronization synchronization : TransactionSynchronizationManager.getSynchronizations()) {
            if (synchronization instanceof ScopeMarker marker && marker.connection == physical) return marker;
        }
        return null;
    }

    private static void initialize(Connection connection, Scope scope) throws SQLException {
        // Reject ambient session/local context instead of silently replacing an unknown scope.
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT nullif(current_setting('app.user_id', true), ''), "
                        + "nullif(current_setting('app.tenant_id', true), '')");
                ResultSet result = statement.executeQuery()) {
            if (!result.next() || result.getString(1) != null || result.getString(2) != null) {
                throw new IllegalStateException("Unexpected existing JDBC context");
            }
        }
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT set_config('app.user_id', ?, true), set_config('app.tenant_id', ?, true)")) {
            statement.setString(1, scope.user);
            statement.setString(2, scope.tenant);
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next() || !scope.user.equals(result.getString(1))
                        || !scope.tenant.equals(result.getString(2))) {
                    throw new IllegalStateException("Cannot initialize JDBC context");
                }
            }
        }
    }

    private record Scope(String user, String tenant) { }

    /** Spring suspends these markers with REQUIRES_NEW and clears them only at completion. */
    private static final class ScopeMarker implements TransactionSynchronization {
        private Connection connection;
        private final Scope scope;

        private ScopeMarker(Connection connection, Scope scope) {
            this.connection = connection;
            this.scope = scope;
        }

        @Override
        public void afterCompletion(int status) {
            connection = null;
        }
    }
}
