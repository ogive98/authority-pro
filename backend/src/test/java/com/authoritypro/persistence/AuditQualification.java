package com.authoritypro.persistence;

import com.authoritypro.platform.audit.domain.AttemptReason;
import com.authoritypro.platform.audit.domain.AuditAction;
import com.authoritypro.platform.audit.domain.AuditActorKind;
import com.authoritypro.platform.audit.domain.AuditErrorCode;
import com.authoritypro.platform.audit.domain.AuditEvent;
import com.authoritypro.platform.audit.domain.AuditFamily;
import com.authoritypro.platform.audit.domain.AuditMetadata;
import com.authoritypro.platform.audit.domain.AuditQuery;
import com.authoritypro.platform.audit.domain.AuditRecordView;
import com.authoritypro.platform.audit.domain.AuditRejectedException;
import com.authoritypro.platform.audit.domain.FailedAttempt;
import com.authoritypro.platform.audit.infrastructure.persistence.JdbcAuditStore;
import com.authoritypro.platform.runtime.infrastructure.persistence.LocalDatabase;
import com.authoritypro.platform.tenancy.infrastructure.persistence.TenantTransactions;
import com.zaxxer.hikari.HikariDataSource;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Explicit executable only: isolated L04 database, no JUnit discovery and no provisioning. */
public final class AuditQualification {
    private static final String DATABASE = "authority_pro_l04_audit_test";
    private static final String OWNER = "authority_pro_owner";
    private static final String RUNTIME = "authority_pro_runtime";
    private static final String MIGRATION_V2 = "V2__audit_foundation.sql";
    private static final UUID USER_A = UUID.randomUUID();
    private static final UUID USER_B = UUID.randomUUID();
    private static final UUID TENANT_A = UUID.randomUUID();
    private static final UUID TENANT_B = UUID.randomUUID();
    private static final UUID COMPANY_A = UUID.randomUUID();
    private static final UUID COMPANY_B = UUID.randomUUID();
    private static final UUID SITE_A = UUID.randomUUID();
    private static final UUID SITE_B = UUID.randomUUID();
    private static final UUID ACTOR = UUID.randomUUID();
    private static PrintStream report;
    private static String activeCheck = "input_contract";
    private static int checks;

    private AuditQualification() { }

    public static void main(String[] args) {
        report = System.out;
        System.setOut(new PrintStream(OutputStream.nullOutputStream()));
        System.setErr(new PrintStream(OutputStream.nullOutputStream()));
        java.util.logging.LogManager.getLogManager().reset();
        System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "off");
        try {
            require(args.length == 1 && "qualify".equals(args[0]));
            BufferedReader input = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));
            String migratorPassword = input.readLine();
            String runtimePassword = input.readLine();
            String database = input.readLine();
            require(migratorPassword != null && !migratorPassword.isEmpty());
            require(runtimePassword != null && !runtimePassword.isEmpty());
            require(DATABASE.equals(database));
            require(input.readLine() == null);
            passed("input_contract");
            run(migratorPassword, runtimePassword);
            report.println("AUDIT_QUALIFICATION_OK checks=" + checks + " database=" + DATABASE);
        } catch (Throwable failure) {
            SQLException sql = sqlException(failure);
            String state = sql == null ? "none" : sql.getSQLState();
            if (state == null || !state.matches("[A-Z0-9]{5}")) state = "none";
            String type = (sql == null ? failure : sql).getClass().getSimpleName();
            if (!type.matches("[A-Za-z0-9_$]+")) type = "Failure";
            report.println("AUDIT_QUALIFICATION_FAILED check=" + activeCheck + " type=" + type
                    + " sqlState=" + state);
            System.exit(1);
        }
    }

    private static void run(String migratorPassword, String runtimePassword) throws Exception {
        activeCheck = "migration";
        int executed = LocalDatabase.migrateAuditTest(migratorPassword);
        require(executed == 0 || executed == 1 || executed == 2);
        try (HikariDataSource migrator = LocalDatabase.migrationDataSource(DATABASE, migratorPassword)) {
            check("effective_owner_and_history", () -> raw(migrator, true, false, c -> {
                require(DATABASE.equals(text(c, "SELECT current_database()")));
                require(OWNER.equals(text(c, "SELECT current_user")));
                require("authority_pro_migrator".equals(text(c, "SELECT session_user")));
                eq(2, number(c, "SELECT count(*) FROM public.flyway_schema_history"));
                eq(1, number(c, "SELECT count(*) FROM public.flyway_schema_history WHERE version = '2' "
                        + "AND success AND script = ?", MIGRATION_V2));
                eq(0, number(c, "SELECT count(*) FROM pg_namespace WHERE nspname NOT LIKE 'pg_%' "
                        + "AND nspname <> 'information_schema' AND nspname NOT IN ('public','tenancy','audit')"));
                return null;
            }));
            seed(migrator);
            activeCheck = "runtime_pool";
            try (HikariDataSource runtime = LocalDatabase.runtimeDataSource(DATABASE, runtimePassword)) {
                runtime.setMaximumPoolSize(1);
                runtime.setMinimumIdle(0);
                TenantTransactions tx = new TenantTransactions(runtime);
                JdbcAuditStore store = new JdbcAuditStore();
                check("production_runtime_validation", () -> LocalDatabase.verifyRuntime(runtime));
                schemaChecks(runtime);
                appendChecks(tx, store);
                isolationChecks(runtime, tx, store);
                atomicityChecks(tx, store);
                controlPlaneChecks(tx, store);
                failedAttemptChecks(runtime, tx, store);
                denialChecks(runtime, tx, store);
                check("final_runtime_validation", () -> LocalDatabase.verifyRuntime(runtime));
            }
        }
    }

    private static void seed(HikariDataSource migrator) throws Exception {
        check("commit_tenant_fixtures", () -> raw(migrator, false, false, c -> {
            context(c, USER_A, TENANT_A);
            update(c, "INSERT INTO tenancy.tenants (tenant_id, name) VALUES (?, 'l04-tenant-a')", TENANT_A);
            update(c, "INSERT INTO tenancy.companies (tenant_id, company_id, name) VALUES (?, ?, 'l04-company-a')",
                    TENANT_A, COMPANY_A);
            update(c, "INSERT INTO tenancy.sites (tenant_id, company_id, site_id, name) VALUES (?, ?, ?, 'l04-site-a')",
                    TENANT_A, COMPANY_A, SITE_A);
            update(c, "INSERT INTO tenancy.memberships (user_id, tenant_id) VALUES (?, ?)", USER_A, TENANT_A);
            context(c, USER_B, TENANT_B);
            update(c, "INSERT INTO tenancy.tenants (tenant_id, name) VALUES (?, 'l04-tenant-b')", TENANT_B);
            update(c, "INSERT INTO tenancy.companies (tenant_id, company_id, name) VALUES (?, ?, 'l04-company-b')",
                    TENANT_B, COMPANY_B);
            update(c, "INSERT INTO tenancy.sites (tenant_id, company_id, site_id, name) VALUES (?, ?, ?, 'l04-site-b')",
                    TENANT_B, COMPANY_B, SITE_B);
            update(c, "INSERT INTO tenancy.memberships (user_id, tenant_id) VALUES (?, ?)", USER_B, TENANT_B);
            return null;
        }));
    }

    private static void schemaChecks(HikariDataSource runtime) throws Exception {
        check("audit_schema_force_rls_append_only", () -> raw(runtime, true, false, c -> {
            eq(3, number(c, "SELECT count(*) FROM pg_namespace WHERE nspname IN ('public','tenancy','audit')"));
            eq(1, number(c, "SELECT count(*) FROM pg_policies WHERE schemaname = 'audit' "
                    + "AND tablename = 'tenant_records' AND cmd = 'ALL' AND qual = with_check"));
            eq(1, number(c, "SELECT count(*) FROM pg_class WHERE oid = 'audit.tenant_records'::regclass "
                    + "AND relrowsecurity AND relforcerowsecurity"));
            eq(0, number(c, "SELECT count(*) FROM pg_class WHERE oid IN ('audit.control_plane_records'::regclass, "
                    + "'audit.security_attempts'::regclass) AND (relrowsecurity OR relforcerowsecurity)"));
            eq(0, number(c, "SELECT count(*) FROM information_schema.columns WHERE table_schema = 'audit' "
                    + "AND table_name IN ('control_plane_records','security_attempts') "
                    + "AND column_name IN ('tenant_id','company_id','site_id')"));
            for (String table : new String[]{"tenant_records", "control_plane_records", "security_attempts"}) {
                require("true".equals(text(c, "SELECT has_table_privilege(current_user, ?, 'SELECT')::text",
                        "audit." + table)));
                require("true".equals(text(c, "SELECT has_table_privilege(current_user, ?, 'INSERT')::text",
                        "audit." + table)));
                for (String privilege : new String[]{"UPDATE", "DELETE", "TRUNCATE", "REFERENCES", "TRIGGER"}) {
                    require("false".equals(text(c, "SELECT has_table_privilege(current_user, ?, ?)::text",
                            "audit." + table, privilege)));
                }
            }
            return null;
        }));
    }

    private static void appendChecks(TenantTransactions tx, JdbcAuditStore store) throws Exception {
        String target = "settings-" + UUID.randomUUID();
        UUID operation = UUID.randomUUID();
        UUID[] recordId = new UUID[1];
        check("tenant_append_in_caller_transaction", () -> tx.scoped(USER_A, TENANT_A, false, c -> {
            AuditRecordView view = store.appendCritical(c, settingsEvent(USER_A, TENANT_A, COMPANY_A, target, operation));
            require(view.recordId() != null && view.occurredAt() != null);
            require("committed".equals(view.outcome()));
            require("{\"changedKeys\":[\"displayName\"],\"fromRevision\":1,\"toRevision\":2}".equals(view.metadata()));
            eq(1, number(c, "SELECT count(*) FROM audit.tenant_records WHERE target_id = ?", target));
            recordId[0] = view.recordId();
            return null;
        }));
        check("tenant_read_after_commit", () -> tx.scoped(USER_A, TENANT_A, true, c -> {
            List<AuditRecordView> page = store.read(c, AuditFamily.TENANT,
                    query(AuditAction.TENANCY_SETTINGS_UPDATED, "settings", target, 10));
            eq(1, page.size());
            require(recordId[0].equals(page.getFirst().recordId()));
            require(TENANT_A.equals(page.getFirst().tenantId()));
            require(COMPANY_A.equals(page.getFirst().companyId()));
            return null;
        }));
        check("server_clock_is_utc", () -> tx.scoped(USER_A, TENANT_A, true, c -> {
            Instant occurred = store.read(c, AuditFamily.TENANT,
                    query(AuditAction.TENANCY_SETTINGS_UPDATED, "settings", target, 10)).getFirst().occurredAt();
            require(occurred.isAfter(Instant.parse("2025-01-01T00:00:00Z")));
            return null;
        }));
    }

    private static void isolationChecks(HikariDataSource runtime, TenantTransactions tx, JdbcAuditStore store)
            throws Exception {
        String target = "settings-" + UUID.randomUUID();
        check("company_scoped_append", () -> tx.scoped(USER_A, TENANT_A, false, c -> {
            store.appendCritical(c, settingsEvent(USER_A, TENANT_A, COMPANY_A, target, null));
            store.appendCritical(c, settingsEvent(USER_A, TENANT_A, null, target, null));
            return null;
        }));
        check("tenant_wide_reader_sees_all_companies", () -> tx.scoped(USER_A, TENANT_A, true, c -> {
            eq(2, store.read(c, AuditFamily.TENANT,
                    query(AuditAction.TENANCY_SETTINGS_UPDATED, "settings", target, 10)).size());
            return null;
        }));
        check("company_restricted_reader_sees_only_its_company", () -> tx.scoped(USER_A, TENANT_A, true, c -> {
            AuditQuery restricted = new AuditQuery(AuditAction.TENANCY_SETTINGS_UPDATED, "settings", target,
                    null, null, null, COMPANY_A, null, null, 10);
            List<AuditRecordView> page = store.read(c, AuditFamily.TENANT, restricted);
            eq(1, page.size());
            require(COMPANY_A.equals(page.getFirst().companyId()));
            AuditQuery foreign = new AuditQuery(AuditAction.TENANCY_SETTINGS_UPDATED, "settings", target,
                    null, null, null, COMPANY_B, null, null, 10);
            eq(0, store.read(c, AuditFamily.TENANT, foreign).size());
            return null;
        }));
        check("other_tenant_sees_nothing", () -> tx.scoped(USER_B, TENANT_B, true, c -> {
            eq(0, store.read(c, AuditFamily.TENANT,
                    query(AuditAction.TENANCY_SETTINGS_UPDATED, "settings", target, 10)).size());
            return null;
        }));
        check("missing_scope_reads_nothing", () -> raw(runtime, true, false, c -> {
            blank(c);
            eq(0, number(c, "SELECT count(*) FROM audit.tenant_records"));
            return null;
        }));
        check("malformed_scope_fails_closed", () -> {
            try (Connection c = runtime.getConnection()) {
                c.setAutoCommit(false);
                try {
                    text(c, "SELECT set_config('app.tenant_id', 'not-a-uuid', true)");
                    number(c, "SELECT count(*) FROM audit.tenant_records");
                    throw new AssertionError();
                } catch (SQLException failure) {
                    require("22P02".equals(failure.getSQLState()));
                } finally {
                    c.rollback();
                }
            }
        });
    }

    private static void atomicityChecks(TenantTransactions tx, JdbcAuditStore store) throws Exception {
        String rolledBack = "settings-" + UUID.randomUUID();
        check("audit_and_protected_write_rollback_together", () -> {
            try {
                tx.scoped(USER_A, TENANT_A, false, c -> {
                    update(c, "UPDATE tenancy.tenants SET name = 'rolled-back' WHERE tenant_id = ?", TENANT_A);
                    store.appendCritical(c, settingsEvent(USER_A, TENANT_A, COMPANY_A, rolledBack, null));
                    throw new IllegalStateException("fault-injection");
                });
                throw new AssertionError();
            } catch (IllegalStateException expected) {
                require("fault-injection".equals(expected.getMessage()));
            }
            tx.scoped(USER_A, TENANT_A, true, c -> {
                eq(0, number(c, "SELECT count(*) FROM audit.tenant_records WHERE target_id = ?", rolledBack));
                eq(1, number(c, "SELECT count(*) FROM tenancy.tenants WHERE tenant_id = ? AND name = 'l04-tenant-a'",
                        TENANT_A));
                return null;
            });
        });
        String duplicate = "settings-" + UUID.randomUUID();
        UUID operation = UUID.randomUUID();
        check("duplicate_operation_does_not_duplicate_effect", () -> {
            tx.scoped(USER_A, TENANT_A, false, c -> {
                store.appendCritical(c, settingsEvent(USER_A, TENANT_A, COMPANY_A, duplicate, operation));
                return null;
            });
            try {
                tx.scoped(USER_A, TENANT_A, false, c -> {
                    update(c, "UPDATE tenancy.tenants SET name = 'must-rollback' WHERE tenant_id = ?", TENANT_A);
                    store.appendCritical(c, settingsEvent(USER_A, TENANT_A, COMPANY_A, duplicate, operation));
                    return null;
                });
                throw new AssertionError();
            } catch (AuditRejectedException expected) {
                require(expected.code() == AuditErrorCode.AUDIT_PERSISTENCE_FAILED);
            }
            tx.scoped(USER_A, TENANT_A, true, c -> {
                eq(1, number(c, "SELECT count(*) FROM audit.tenant_records WHERE operation_id = ?", operation));
                eq(1, number(c, "SELECT count(*) FROM tenancy.tenants WHERE tenant_id = ? AND name = 'l04-tenant-a'",
                        TENANT_A));
                return null;
            });
        });
        check("invalid_scope_rejected_before_insert", () -> {
            long before = tx.scoped(USER_A, TENANT_A, true,
                    c -> number(c, "SELECT count(*) FROM audit.tenant_records"));
            expectAudit(AuditErrorCode.AUDIT_SCOPE_MISMATCH, () -> tx.scoped(USER_A, TENANT_A, false, c -> {
                store.appendCritical(c, settingsEvent(USER_B, TENANT_B, null, "settings-forbidden", null));
                return null;
            }));
            eq(before, tx.scoped(USER_A, TENANT_A, true,
                    c -> number(c, "SELECT count(*) FROM audit.tenant_records")));
        });
    }

    private static void controlPlaneChecks(TenantTransactions tx, JdbcAuditStore store) throws Exception {
        UUID actor = UUID.randomUUID();
        UUID[] ids = new UUID[3];
        check("control_plane_append_has_no_tenant_data", () -> tx.scoped(USER_A, TENANT_A, false, c -> {
            for (int i = 0; i < 3; i++) {
                ids[i] = store.appendCritical(c, moduleEvent(actor, "module-" + i + "-" + UUID.randomUUID())).recordId();
            }
            List<AuditRecordView> page = store.read(c, AuditFamily.CONTROL_PLANE,
                    new AuditQuery(null, null, null, actor, null, null, null, null, null, 2));
            eq(2, page.size());
            require(page.getFirst().tenantId() == null && page.getFirst().companyId() == null);
            return null;
        }));
        check("cursor_pagination_is_stable", () -> tx.scoped(USER_A, TENANT_A, true, c -> {
            List<AuditRecordView> first = store.read(c, AuditFamily.CONTROL_PLANE,
                    new AuditQuery(null, null, null, actor, null, null, null, null, null, 2));
            eq(2, first.size());
            AuditRecordView last = first.getLast();
            List<AuditRecordView> second = store.read(c, AuditFamily.CONTROL_PLANE,
                    new AuditQuery(null, null, null, actor, null, null, null, last.occurredAt(),
                            last.recordId(), 2));
            eq(1, second.size());
            require(ids[0].equals(second.getFirst().recordId()));
            return null;
        }));
        check("read_page_bound_and_company_filter", () -> {
            expectIllegal(() -> new AuditQuery(null, null, null, null, null, null, null, null, null, 101));
            tx.scoped(USER_A, TENANT_A, true, c -> {
                expectIllegal(() -> store.read(c, AuditFamily.CONTROL_PLANE,
                        new AuditQuery(null, null, null, null, null, null, COMPANY_A, null, null, 10)));
                return null;
            });
        });
    }

    private static void failedAttemptChecks(HikariDataSource runtime, TenantTransactions tx, JdbcAuditStore store)
            throws Exception {
        UUID correlation = UUID.randomUUID();
        check("failed_attempt_is_minimal", () -> tx.userScoped(USER_A, false, c -> {
            UUID id = store.recordFailedAttempt(c,
                    new FailedAttempt(AttemptReason.INVALID_CREDENTIALS, correlation, "login"));
            require(id != null);
            return null;
        }));
        check("failed_attempt_has_no_identity_fields", () -> raw(runtime, true, false, c -> {
            eq(1, number(c, "SELECT count(*) FROM audit.security_attempts WHERE correlation_id = ?", correlation));
            eq(0, number(c, "SELECT count(*) FROM information_schema.columns WHERE table_schema = 'audit' "
                    + "AND table_name = 'security_attempts' "
                    + "AND column_name IN ('username','email','ip','token','body','detail')"));
            return null;
        }));
    }

    private static void denialChecks(HikariDataSource runtime, TenantTransactions tx, JdbcAuditStore store)
            throws Exception {
        check("cross_tenant_insert_denied", () -> expectAudit(AuditErrorCode.AUDIT_SCOPE_MISMATCH,
                () -> tx.scoped(USER_B, TENANT_B, false, c -> {
                    store.appendCritical(c, settingsEvent(USER_A, TENANT_A, COMPANY_A, "settings-cross", null));
                    return null;
                })));
        check("non_member_tenant_append_denied", () -> expectAudit(AuditErrorCode.AUDIT_SCOPE_MISMATCH,
                () -> tx.scoped(USER_A, TENANT_A, false, c -> {
                    store.appendCritical(c, settingsEvent(USER_B, TENANT_B, null, "settings-nonmember", null));
                    return null;
                })));
        for (String table : new String[]{"tenant_records", "control_plane_records", "security_attempts"}) {
            String column = "security_attempts".equals(table) ? "reason_code" : "outcome";
            check("runtime_update_denied_" + table, () -> expectSql("42501", () -> raw(runtime, false, true,
                    c -> update(c, "UPDATE audit." + table + " SET " + column + " = "
                            + ("security_attempts".equals(table) ? "'rate_limited'" : "'committed'")))));
            check("runtime_delete_denied_" + table, () -> expectSql("42501", () -> raw(runtime, false, true,
                    c -> update(c, "DELETE FROM audit." + table + " WHERE false"))));
            check("runtime_truncate_denied_" + table, () -> expectSql("42501", () -> raw(runtime, false, true,
                    c -> { execute(c, "TRUNCATE audit." + table + " CASCADE"); return null; })));
        }
        check("runtime_cannot_disable_rls", () -> expectSql("42501", () -> raw(runtime, false, true,
                c -> { execute(c, "ALTER TABLE audit.tenant_records DISABLE ROW LEVEL SECURITY"); return null; })));
        check("runtime_cannot_change_history", () -> expectSql("42501", () -> raw(runtime, false, true,
                c -> update(c, "UPDATE public.flyway_schema_history SET checksum = 0"))));
    }

    private static AuditEvent settingsEvent(UUID user, UUID tenant, UUID company, String target, UUID operation) {
        return new AuditEvent(AuditAction.TENANCY_SETTINGS_UPDATED, user, AuditActorKind.USER, target,
                operation, null, tenant, company, null, AuditMetadata.of(AuditAction.TENANCY_SETTINGS_UPDATED,
                Map.of("changedKeys", List.of("displayName"), "fromRevision", 1, "toRevision", 2)));
    }

    private static AuditEvent moduleEvent(UUID actor, String target) {
        return new AuditEvent(AuditAction.RUNTIME_MODULE_LIFECYCLE_CHANGED, actor, AuditActorKind.OPERATOR,
                target, null, null, null, null, null, AuditMetadata.of(
                AuditAction.RUNTIME_MODULE_LIFECYCLE_CHANGED, Map.of("moduleId", "orders",
                        "fromState", "STOPPED", "toState", "STARTED", "descriptorDigest", "a".repeat(64))));
    }

    private static AuditQuery query(AuditAction action, String targetType, String target, int pageSize) {
        return new AuditQuery(action, targetType, target, null, null, null, null, null, null, pageSize);
    }

    private static <T> T raw(javax.sql.DataSource source, boolean readOnly, boolean rollback,
                             TenantTransactions.Work<T> work) throws SQLException {
        try (Connection c = source.getConnection()) {
            c.setReadOnly(readOnly);
            c.setAutoCommit(false);
            boolean completed = false;
            try {
                if (readOnly) execute(c, "SET TRANSACTION READ ONLY");
                T result = work.run(c);
                if (rollback) c.rollback(); else c.commit();
                completed = true;
                return result;
            } finally {
                if (!completed) c.rollback();
            }
        }
    }

    private static void context(Connection c, UUID user, UUID tenant) throws SQLException {
        require(!c.getAutoCommit());
        text(c, "SELECT set_config('app.user_id', ?, true)", user.toString());
        text(c, "SELECT set_config('app.tenant_id', ?, true)", tenant.toString());
    }

    private static void blank(Connection c) throws SQLException {
        require(empty(text(c, "SELECT current_setting('app.user_id', true)")));
        require(empty(text(c, "SELECT current_setting('app.tenant_id', true)")));
    }

    private static boolean empty(String value) { return value == null || value.isEmpty(); }

    private static long number(Connection c, String sql, Object... values) throws SQLException {
        try (PreparedStatement statement = prepared(c, sql, values); ResultSet result = statement.executeQuery()) {
            require(result.next());
            long value = result.getLong(1);
            require(!result.wasNull());
            require(!result.next());
            return value;
        }
    }

    private static String text(Connection c, String sql, Object... values) throws SQLException {
        try (PreparedStatement statement = prepared(c, sql, values); ResultSet result = statement.executeQuery()) {
            require(result.next());
            String value = result.getString(1);
            require(!result.next());
            return value;
        }
    }

    private static int update(Connection c, String sql, Object... values) throws SQLException {
        try (PreparedStatement statement = prepared(c, sql, values)) { return statement.executeUpdate(); }
    }

    private static void execute(Connection c, String sql) throws SQLException {
        try (PreparedStatement statement = c.prepareStatement(sql)) { statement.execute(); }
    }

    private static PreparedStatement prepared(Connection c, String sql, Object... values) throws SQLException {
        PreparedStatement statement = c.prepareStatement(sql);
        try {
            for (int i = 0; i < values.length; i++) statement.setObject(i + 1, values[i]);
            return statement;
        } catch (SQLException | RuntimeException failure) {
            statement.close();
            throw failure;
        }
    }

    private static void check(String name, Checked work) throws Exception {
        activeCheck = name;
        work.run();
        passed(name);
    }

    private static void passed(String name) { checks++; report.println("PASS " + name); }
    private static void require(boolean value) { if (!value) throw new AssertionError(); }
    private static void eq(long expected, long actual) { require(expected == actual); }

    private static void expectSql(String state, Checked work) throws Exception {
        try { work.run(); }
        catch (Exception failure) {
            SQLException sql = sqlException(failure);
            if (sql != null && state.equals(sql.getSQLState())) return;
            throw failure;
        }
        throw new AssertionError();
    }

    private static void expectAudit(AuditErrorCode code, Checked work) throws Exception {
        try { work.run(); }
        catch (AuditRejectedException expected) {
            require(expected.code() == code);
            return;
        }
        throw new AssertionError();
    }

    private static void expectIllegal(Runnable work) {
        try { work.run(); }
        catch (IllegalArgumentException expected) { return; }
        throw new AssertionError();
    }

    private static SQLException sqlException(Throwable failure) {
        Set<Throwable> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        for (Throwable next = failure; next != null && seen.add(next); next = next.getCause()) {
            if (next instanceof SQLException sql && sql.getSQLState() != null) return sql;
        }
        return null;
    }

    @FunctionalInterface
    private interface Checked { void run() throws Exception; }
}
