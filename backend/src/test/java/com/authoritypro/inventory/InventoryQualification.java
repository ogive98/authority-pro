package com.authoritypro.inventory;

import com.authoritypro.platform.access.application.AuditAccessPolicy;
import com.authoritypro.platform.access.application.InventoryAccessPolicy;
import com.authoritypro.platform.access.application.ScopedAccess;
import com.authoritypro.platform.access.application.ScopedAccessReader;
import com.authoritypro.platform.access.domain.Permission;
import com.authoritypro.platform.access.domain.RoleCode;
import com.authoritypro.platform.access.infrastructure.persistence.JdbcAuditAccessPolicy;
import com.authoritypro.platform.access.infrastructure.persistence.JdbcInventoryAccessPolicy;
import com.authoritypro.platform.access.infrastructure.persistence.JdbcScopedAccessReader;
import com.authoritypro.platform.audit.application.AuditRecordReader;
import com.authoritypro.platform.audit.domain.AuditAction;
import com.authoritypro.platform.audit.domain.AuditQuery;
import com.authoritypro.platform.audit.domain.AuditRecordView;
import com.authoritypro.platform.audit.infrastructure.persistence.JdbcAuditRecordReader;
import com.authoritypro.platform.runtime.infrastructure.persistence.LocalDatabase;
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
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Explicit executable only: isolated L06 database, no JUnit discovery and no main database access. */
public final class InventoryQualification {
    private static final String DATABASE = "authority_pro_l06_inventory_test";
    private static final String OWNER = "authority_pro_owner";
    private static final String RUNTIME = "authority_pro_runtime";
    private static final String MIGRATOR = "authority_pro_migrator";
    private static final String MIGRATION_V5 = "V5__platform_inventory_permission.sql";
    private static final UUID USER_OWNER = UUID.randomUUID();
    private static final UUID USER_AUDITOR = UUID.randomUUID();
    private static final UUID USER_MEMBER = UUID.randomUUID();
    private static final UUID TENANT = UUID.randomUUID();
    private static final UUID COMPANY = UUID.randomUUID();
    private static final UUID RECORD = UUID.randomUUID();
    private static final String LOGIN_PREFIX = "qualification.l06." + UUID.randomUUID() + ".";
    private static PrintStream report;
    private static String activeCheck = "input_contract";
    private static int checks;

    private InventoryQualification() { }

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
            report.println("INVENTORY_QUALIFICATION_OK checks=" + checks + " database=" + DATABASE);
        } catch (Throwable failure) {
            SQLException sql = sqlException(failure);
            String state = sql == null ? "none" : sql.getSQLState();
            if (state == null || !state.matches("[A-Z0-9]{5}")) state = "none";
            String type = (sql == null ? failure : sql).getClass().getSimpleName();
            if (!type.matches("[A-Za-z0-9_$]+")) type = "Failure";
            report.println("INVENTORY_QUALIFICATION_FAILED check=" + activeCheck + " type=" + type
                    + " sqlState=" + state);
            System.exit(1);
        }
    }

    private static void run(String migratorPassword, String runtimePassword) throws Exception {
        activeCheck = "migration";
        int executed = LocalDatabase.migrateInventoryTest(migratorPassword);
        require(executed >= 0 && executed <= 5);
        try (HikariDataSource migrator = LocalDatabase.migrationDataSource(DATABASE, migratorPassword)) {
            check("effective_owner_and_history", () -> raw(migrator, false, false, c -> {
                require(DATABASE.equals(text(c, "SELECT current_database()")));
                require(OWNER.equals(text(c, "SELECT current_user")));
                require(MIGRATOR.equals(text(c, "SELECT session_user")));
                eq(5, number(c, "SELECT count(*) FROM public.flyway_schema_history"));
                eq(1, number(c, "SELECT count(*) FROM public.flyway_schema_history WHERE version = '5' "
                        + "AND success AND script = ?", MIGRATION_V5));
                eq(0, number(c, "SELECT count(*) FROM pg_namespace WHERE nspname NOT LIKE 'pg_%' "
                        + "AND nspname <> 'information_schema' "
                        + "AND nspname NOT IN ('public','tenancy','audit','identity','access')"));
                return null;
            }));
            check("inventory_permission_catalogue", () -> raw(migrator, true, false, c -> {
                eq(11, number(c, "SELECT count(*) FROM access.role_permissions"));
                eq(6, number(c, "SELECT count(*) FROM access.role_permissions "
                        + "WHERE role_code = 'TENANT_OWNER'"));
                eq(2, number(c, "SELECT count(*) FROM access.role_permissions "
                        + "WHERE role_code = 'TENANT_ADMIN'"));
                eq(3, number(c, "SELECT count(*) FROM access.role_permissions "
                        + "WHERE role_code = 'TENANT_AUDITOR'"));
                eq(1, number(c, "SELECT count(*) FROM access.role_permissions "
                        + "WHERE role_code = 'TENANT_OWNER' AND permission_code = 'platform.inventory.read'"));
                eq(0, number(c, "SELECT count(*) FROM access.role_permissions "
                        + "WHERE permission_code = 'platform.inventory.read' AND role_code <> 'TENANT_OWNER'"));
                return null;
            }));
            check("unknown_inventory_permission_rejected", () -> expectSql("23514", () -> raw(migrator, false, true,
                    c -> update(c, "INSERT INTO access.role_permissions (role_code, permission_code) "
                            + "VALUES ('TENANT_ADMIN', 'platform.inventory.write')"))));
            seed(migrator);
            activeCheck = "runtime_pool";
            try (HikariDataSource runtime = LocalDatabase.runtimeDataSource(DATABASE, runtimePassword)) {
                runtime.setMaximumPoolSize(1);
                runtime.setMinimumIdle(0);
                check("production_runtime_validation", () -> LocalDatabase.verifyRuntime(runtime));
                check("runtime_reads_permission_catalogue", () -> raw(runtime, true, false, c -> {
                    require("true".equals(text(c, "SELECT has_table_privilege(current_user, "
                            + "'access.role_permissions', 'SELECT')::text")));
                    require("false".equals(text(c, "SELECT has_table_privilege(current_user, "
                            + "'access.role_permissions', 'INSERT')::text")));
                    eq(1, number(c, "SELECT count(*) FROM access.role_permissions "
                            + "WHERE role_code = 'TENANT_OWNER' AND permission_code = 'platform.inventory.read'"));
                    return null;
                }));
                check("policy_grants_inventory_owner", () -> {
                    require(new JdbcInventoryAccessPolicy(runtime).decide(USER_OWNER)
                            == InventoryAccessPolicy.Decision.GRANTED);
                });
                check("policy_denies_auditor_and_records_attempt", () -> {
                    long before = attempts(migrator);
                    require(new JdbcInventoryAccessPolicy(runtime).decide(USER_AUDITOR)
                            == InventoryAccessPolicy.Decision.DENIED);
                    eq(before + 1, attempts(migrator));
                });
                check("policy_denies_member_and_records_attempt", () -> {
                    long before = attempts(migrator);
                    require(new JdbcInventoryAccessPolicy(runtime).decide(USER_MEMBER)
                            == InventoryAccessPolicy.Decision.DENIED);
                    eq(before + 1, attempts(migrator));
                });
                check("runtime_cannot_write_permission_catalogue", () -> expectSql("42501", () -> raw(runtime, false,
                        true, c -> update(c, "INSERT INTO access.role_permissions (role_code, permission_code) "
                                + "VALUES ('TENANT_AUDITOR', 'platform.inventory.read')"))));
                check("scoped_reader_resolves_owner_labels_and_grants", () -> {
                    ScopedAccessReader reader = new JdbcScopedAccessReader(runtime);
                    ScopedAccess tenant = reader.read(USER_OWNER, TENANT, null).orElseThrow();
                    require("l06-tenant".equals(tenant.tenantName()));
                    require(tenant.companyId() == null && tenant.companyName() == null);
                    require(tenant.roles().contains(RoleCode.TENANT_OWNER));
                    require(tenant.permissions().contains(Permission.AUDIT_READ_TENANT));
                    require(tenant.authorizationRevision() == 1);
                    ScopedAccess company = reader.read(USER_OWNER, TENANT, COMPANY).orElseThrow();
                    require(COMPANY.equals(company.companyId()));
                    require("l06-company".equals(company.companyName()));
                });
                check("scoped_reader_resolves_auditor_and_hides_unknown_scopes", () -> {
                    ScopedAccessReader reader = new JdbcScopedAccessReader(runtime);
                    ScopedAccess auditor = reader.read(USER_AUDITOR, TENANT, null).orElseThrow();
                    require(auditor.roles().contains(RoleCode.TENANT_AUDITOR));
                    require(auditor.permissions().contains(Permission.AUDIT_READ_TENANT));
                    require(reader.read(USER_OWNER, TENANT, UUID.randomUUID()).isEmpty());
                    require(reader.read(USER_OWNER, UUID.randomUUID(), null).isEmpty());
                    require(reader.read(USER_MEMBER, TENANT, null).orElseThrow().roles().isEmpty());
                });
                check("audit_policy_grants_owner_and_auditor", () -> {
                    AuditAccessPolicy policy = new JdbcAuditAccessPolicy(runtime);
                    require(policy.decide(USER_OWNER, TENANT, null) == AuditAccessPolicy.Decision.GRANTED);
                    require(policy.decide(USER_OWNER, TENANT, COMPANY) == AuditAccessPolicy.Decision.GRANTED);
                    require(policy.decide(USER_AUDITOR, TENANT, null) == AuditAccessPolicy.Decision.GRANTED);
                });
                check("audit_policy_denies_member_and_records_attempt", () -> {
                    long before = auditAttempts(migrator);
                    require(new JdbcAuditAccessPolicy(runtime).decide(USER_MEMBER, TENANT, null)
                            == AuditAccessPolicy.Decision.DENIED);
                    eq(before + 1, auditAttempts(migrator));
                });
                check("audit_policy_denies_unknown_company_and_records_attempt", () -> {
                    long before = auditAttempts(migrator);
                    require(new JdbcAuditAccessPolicy(runtime).decide(USER_OWNER, TENANT, UUID.randomUUID())
                            == AuditAccessPolicy.Decision.DENIED);
                    eq(before + 1, auditAttempts(migrator));
                });
                check("audit_reader_returns_the_seeded_tenant_record", () -> {
                    AuditRecordReader reader = new JdbcAuditRecordReader(runtime);
                    AuditQuery query = new AuditQuery(AuditAction.TENANCY_SETTINGS_UPDATED, null, null, null,
                            null, null, COMPANY, null, null, 10);
                    List<AuditRecordView> records = reader.read(USER_OWNER, TENANT, COMPANY, query);
                    eq(1, records.size());
                    require(RECORD.equals(records.getFirst().recordId()));
                    require(TENANT.equals(records.getFirst().tenantId()));
                });
                check("audit_reader_is_tenant_isolated", () -> {
                    AuditRecordReader reader = new JdbcAuditRecordReader(runtime);
                    AuditQuery query = new AuditQuery(null, null, null, null, null, null, null, null, null, 10);
                    eq(0, reader.read(USER_OWNER, UUID.randomUUID(), null, query).size());
                });
                check("final_runtime_validation", () -> LocalDatabase.verifyRuntime(runtime));
            }
        }
    }

    private static void seed(HikariDataSource migrator) throws Exception {
        check("seed_fixtures", () -> raw(migrator, false, false, c -> {
            context(c, USER_OWNER, TENANT);
            update(c, "INSERT INTO tenancy.tenants (tenant_id, name) VALUES (?, 'l06-tenant')", TENANT);
            update(c, "INSERT INTO tenancy.companies (tenant_id, company_id, name) VALUES (?, ?, 'l06-company')",
                    TENANT, COMPANY);
            insertMember(c, USER_OWNER, "owner");
            update(c, "INSERT INTO access.membership_roles (user_id, tenant_id, role_code) "
                    + "VALUES (?, ?, 'TENANT_OWNER')", USER_OWNER, TENANT);
            context(c, USER_AUDITOR, TENANT);
            insertMember(c, USER_AUDITOR, "auditor");
            update(c, "INSERT INTO access.membership_roles (user_id, tenant_id, role_code) "
                    + "VALUES (?, ?, 'TENANT_AUDITOR')", USER_AUDITOR, TENANT);
            context(c, USER_MEMBER, TENANT);
            insertMember(c, USER_MEMBER, "member");
            context(c, USER_OWNER, TENANT);
            update(c, "INSERT INTO audit.tenant_records (record_id, action_code, action_version, actor_id, "
                    + "actor_kind, target_type, target_id, schema_version, metadata, tenant_id, company_id) "
                    + "VALUES (?, 'tenancy.settings.updated', 1, ?, 'user', 'settings', 'l06-settings', 1, "
                    + "?::jsonb, ?, ?)", RECORD, USER_OWNER,
                    "{\"changedKeys\":[\"displayName\"],\"fromRevision\":1,\"toRevision\":2}", TENANT, COMPANY);
            return null;
        }));
    }

    private static void insertMember(Connection c, UUID user, String label) throws SQLException {
        update(c, "INSERT INTO identity.users (user_id, login_normalized, display_name) VALUES (?, ?, ?)",
                user, LOGIN_PREFIX + label + "@authority-pro.test", "L06 " + label);
        update(c, "INSERT INTO tenancy.memberships (user_id, tenant_id) VALUES (?, ?)", user, TENANT);
    }

    private static long attempts(HikariDataSource migrator) throws Exception {
        return raw(migrator, true, false, c -> number(c, "SELECT count(*) FROM audit.security_attempts "
                + "WHERE reason_code = 'authorization_denied' AND source_class = 'inventory'"));
    }

    private static long auditAttempts(HikariDataSource migrator) throws Exception {
        return raw(migrator, true, false, c -> number(c, "SELECT count(*) FROM audit.security_attempts "
                + "WHERE reason_code = 'authorization_denied' AND source_class = 'audit'"));
    }

    private static void context(Connection c, UUID user, UUID tenant) throws SQLException {
        require(!c.getAutoCommit());
        text(c, "SELECT set_config('app.user_id', ?, true)", user.toString());
        if (tenant != null) text(c, "SELECT set_config('app.tenant_id', ?, true)", tenant.toString());
    }

    private static <T> T raw(javax.sql.DataSource source, boolean readOnly, boolean rollback, Work<T> work)
            throws Exception {
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

    private static SQLException sqlException(Throwable failure) {
        Set<Throwable> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        for (Throwable next = failure; next != null && seen.add(next); next = next.getCause()) {
            if (next instanceof SQLException sql && sql.getSQLState() != null) return sql;
        }
        return null;
    }

    @FunctionalInterface
    private interface Checked { void run() throws Exception; }

    @FunctionalInterface
    private interface Work<T> { T run(Connection c) throws Exception; }
}
