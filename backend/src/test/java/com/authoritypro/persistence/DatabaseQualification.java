package com.authoritypro.persistence;

import com.authoritypro.platform.runtime.infrastructure.persistence.LocalDatabase;
import com.authoritypro.platform.tenancy.infrastructure.persistence.TenantTransactions;
import com.zaxxer.hikari.HikariDataSource;
import org.flywaydb.core.Flyway;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
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
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/** Explicit executable only: no JUnit discovery, provisioning, credentials on disk, or server startup. */
public final class DatabaseQualification {
    private static final String OWNER = "authority_pro_owner";
    private static final String RUNTIME = "authority_pro_runtime";
    private static final String MIGRATION = "V1__tenancy_foundation.sql";
    private static final String[] TABLES = {"tenants", "companies", "sites", "memberships"};
    private static final String[] MUTABLE = {"tenants", "companies", "sites"};
    private static final UUID TA = UUID.fromString("10000000-0000-0000-0000-000000000001");
    private static final UUID TB = UUID.fromString("10000000-0000-0000-0000-000000000002");
    private static final UUID TT = UUID.fromString("10000000-0000-0000-0000-000000000009");
    private static final UUID UA = UUID.fromString("20000000-0000-0000-0000-000000000001");
    private static final UUID UB = UUID.fromString("20000000-0000-0000-0000-000000000002");
    private static final UUID CA = UUID.fromString("30000000-0000-0000-0000-000000000001");
    private static final UUID CB = UUID.fromString("30000000-0000-0000-0000-000000000002");
    private static final UUID CT = UUID.fromString("30000000-0000-0000-0000-000000000009");
    private static final UUID SA = UUID.fromString("40000000-0000-0000-0000-000000000001");
    private static final UUID SB = UUID.fromString("40000000-0000-0000-0000-000000000002");
    private static final UUID ST = UUID.fromString("40000000-0000-0000-0000-000000000009");
    private static PrintStream report;
    private static String activeCheck = "input_contract";
    private static int checks;

    private DatabaseQualification() { }

    public static void main(String[] args) {
        // Only this executable suppresses dependency diagnostics; production logging is untouched.
        report = System.out;
        System.setOut(new PrintStream(OutputStream.nullOutputStream()));
        System.setErr(new PrintStream(OutputStream.nullOutputStream()));
        java.util.logging.LogManager.getLogManager().reset();
        System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "off");
        try {
            require(args.length == 1);
            boolean restore = "verify-restore".equals(args[0]);
            require(restore || "qualify".equals(args[0]));
            BufferedReader input = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));
            String migratorPassword = input.readLine();
            String runtimePassword = input.readLine();
            String database = input.readLine();
            require(migratorPassword != null && !migratorPassword.isEmpty());
            require(runtimePassword != null && !runtimePassword.isEmpty());
            require((restore ? "authority_pro_l03_app_restore" : "authority_pro_l03_app_test").equals(database));
            require(input.readLine() == null);
            passed("input_contract");
            run(database, migratorPassword, runtimePassword, restore);
            report.println("DATA_QUALIFICATION_OK checks=" + checks + " database=" + database);
        } catch (Throwable failure) {
            SQLException sql = sqlException(failure);
            String state = sql == null ? "none" : sql.getSQLState();
            if (state == null || !state.matches("[A-Z0-9]{5}")) state = "none";
            String type = (sql == null ? failure : sql).getClass().getSimpleName();
            if (!type.matches("[A-Za-z0-9_$]+")) type = "Failure";
            report.println("DATA_QUALIFICATION_FAILED check=" + activeCheck + " type=" + type + " sqlState=" + state);
            System.exit(1);
        }
    }

    private static void run(String database, String migratorPassword, String runtimePassword,
                            boolean restore) throws Exception {
        activeCheck = "migrator_pool";
        try (HikariDataSource migrator = LocalDatabase.migrationDataSource(database, migratorPassword)) {
            check("effective_owner_and_fixed_endpoint", () -> raw(migrator, true, false, c -> {
                endpoint(c, database);
                require(OWNER.equals(text(c, "SELECT current_user")));
                require("authority_pro_migrator".equals(text(c, "SELECT session_user")));
                eq(1, number(c, "SELECT count(*) FROM pg_roles WHERE rolname = current_user AND NOT rolsuper AND NOT rolbypassrls"));
                require(OWNER.equals(text(c, "SELECT pg_get_userbyid(datdba) FROM pg_database WHERE datname = current_database()")));
                return null;
            }));
            Flyway original = LocalDatabase.flyway(migrator);
            if (!restore) {
                check("fresh_empty_database", () -> raw(migrator, true, false, c -> {
                    require(text(c, "SELECT to_regclass('public.flyway_schema_history')::text") == null);
                    require(text(c, "SELECT to_regnamespace('tenancy')::text") == null);
                    eq(0, number(c, "SELECT count(*) FROM pg_class c JOIN pg_namespace n ON n.oid = c.relnamespace "
                            + "WHERE n.nspname NOT LIKE 'pg_%' AND n.nspname <> 'information_schema' AND c.relkind IN ('r','p','v','m','S','f')"));
                    return null;
                }));
                check("flyway_validate_before_first_migration", original::validate);
                check("flyway_first_migration", () -> {
                    var result = original.migrate();
                    require(result.success);
                    eq(1, result.migrationsExecuted);
                });
            }
            check(restore ? "restore_validate_without_migrate" : "flyway_validate", original::validate);
            check("single_successful_history", () -> history(migrator));
            if (!restore) {
                check("flyway_repeat_noop", () -> {
                    var result = original.migrate();
                    require(result.success);
                    eq(0, result.migrationsExecuted);
                });
                check("checksum_mismatch_rejected", () -> {
                    var result = Flyway.configure().configuration(original.getConfiguration())
                            .locations("classpath:persistence/mismatch").load().validateWithResult();
                    require(!result.validationSuccessful);
                    eq(1, result.invalidMigrations.size());
                    require("CHECKSUM_MISMATCH".equals(String.valueOf(result.invalidMigrations.getFirst().errorDetails.errorCode)));
                });
                check("failed_v2_expected_sqlstate", () -> expectSql("22012", () ->
                        Flyway.configure().configuration(original.getConfiguration())
                                .locations("classpath:db/migration", "classpath:persistence/failure").load().migrate()));
                check("failed_v2_ddl_history_rolled_back", () -> {
                    history(migrator);
                    raw(migrator, true, false, c -> {
                        require(text(c, "SELECT to_regclass('tenancy.failed_v2_marker')::text") == null);
                        return null;
                    });
                    original.validate();
                });
                seed(migrator);
                ownerDenials(migrator);
            }
            check("force_rls_owner_without_scope", () -> raw(migrator, true, false, c -> {
                blank(c);
                for (String table : TABLES) eq(0, number(c, "SELECT count(*) FROM tenancy." + table));
                return null;
            }));
            activeCheck = "runtime_pool";
            try (HikariDataSource runtime = LocalDatabase.runtimeDataSource(database, runtimePassword)) {
                // The factory validates first; retain one session for deterministic reset/reuse evidence.
                runtime.setMaximumPoolSize(1);
                runtime.setMinimumIdle(0);
                TenantTransactions tx = new TenantTransactions(runtime);
                check("production_runtime_validation", () -> LocalDatabase.verifyRuntime(runtime));
                commonChecks(runtime, tx, database);
                if (!restore) {
                    denialChecks(runtime, tx);
                    mutationChecks(runtime, tx);
                    transactionChecks(runtime, tx);
                }
                check("final_seed_a", () -> tx.scoped(UA, TA, true, c -> { seedBaseline(c, true); return null; }));
                check("final_seed_b", () -> tx.scoped(UB, TB, true, c -> { seedBaseline(c, false); return null; }));
                check("temporary_technical_rows_absent", () -> tx.scoped(UA, TT, true, c -> {
                    for (String table : MUTABLE) eq(0, number(c, "SELECT count(*) FROM tenancy." + table));
                    return null;
                }));
                check("final_runtime_pool_reset", () -> poolBlank(runtime));
            }
            if (!restore) {
                activeCheck = "parallel_pool";
                try (HikariDataSource runtime = LocalDatabase.runtimeDataSource(database, runtimePassword)) {
                    runtime.setMaximumPoolSize(2);
                    runtime.setMinimumIdle(0);
                    parallelChecks(runtime, new TenantTransactions(runtime));
                }
            }
            check("final_migration_validation", () -> { original.validate(); history(migrator); });
        }
    }

    private static void endpoint(Connection c, String database) throws SQLException {
        require(database.equals(text(c, "SELECT current_database()")));
        // Docker can expose local port 5434 while PostgreSQL reports internal port 5432.
        String url = c.getMetaData().getURL();
        String endpoint = url.contains("?") ? url.substring(0, url.indexOf('?')) : url;
        require(("jdbc:postgresql://127.0.0.1:5434/" + database).equals(endpoint)
                || ("jdbc:postgresql://localhost:5434/" + database).equals(endpoint));
    }

    private static void history(DataSource source) throws SQLException {
        raw(source, true, false, c -> {
            require(OWNER.equals(text(c, "SELECT pg_get_userbyid(relowner) FROM pg_class WHERE oid = 'public.flyway_schema_history'::regclass")));
            eq(1, number(c, "SELECT count(*) FROM public.flyway_schema_history"));
            eq(1, number(c, "SELECT count(*) FROM public.flyway_schema_history WHERE version = '1' AND success AND type = 'SQL' AND script = ? AND checksum IS NOT NULL", MIGRATION));
            return null;
        });
    }

    private static void seed(DataSource migrator) throws Exception {
        for (boolean a : new boolean[]{true, false}) {
            check("commit_fixture_seed_" + (a ? "a" : "b"), () -> raw(migrator, false, false, c -> {
                context(c, a ? UA : UB, a ? TA : TB);
                UUID tenant = a ? TA : TB;
                UUID company = a ? CA : CB;
                String suffix = a ? "a" : "b";
                eq(1, update(c, "INSERT INTO tenancy.tenants (tenant_id, name) VALUES (?, ?)", tenant, "fixture-tenant-" + suffix));
                eq(1, update(c, "INSERT INTO tenancy.companies (tenant_id, company_id, name) VALUES (?, ?, ?)", tenant, company, "fixture-company-" + suffix));
                eq(1, update(c, "INSERT INTO tenancy.sites (tenant_id, company_id, site_id, name) VALUES (?, ?, ?, ?)", tenant, company, a ? SA : SB, "fixture-site-" + suffix));
                eq(1, update(c, "INSERT INTO tenancy.memberships (user_id, tenant_id) VALUES (?, ?)", a ? UA : UB, tenant));
                return null;
            }));
        }
    }

    private static void ownerDenials(DataSource source) throws Exception {
        check("force_rls_owner_unscoped_insert_denied", () -> expectSql("42501", () -> raw(source, false, true,
                c -> update(c, "INSERT INTO tenancy.tenants VALUES (?, 'forbidden')", TT))));
        check("force_rls_owner_membership_wrong_user_denied", () -> expectSql("42501", () -> raw(source, false, true, c -> {
            context(c, UA, TA);
            return update(c, "INSERT INTO tenancy.memberships (user_id, tenant_id) VALUES (?, ?)", UB, TA);
        })));
        check("force_rls_owner_membership_wrong_tenant_denied", () -> expectSql("42501", () -> raw(source, false, true, c -> {
            context(c, UA, TA);
            return update(c, "INSERT INTO tenancy.memberships (user_id, tenant_id) VALUES (?, ?)", UA, TB);
        })));
        check("force_rls_owner_membership_update_check", () -> expectSql("42501", () -> raw(source, false, true, c -> {
            context(c, UA, TA);
            return update(c, "UPDATE tenancy.memberships SET tenant_id = ? WHERE user_id = ?", TB, UA);
        })));
        check("force_rls_owner_membership_delete_filters", () -> raw(source, false, true, c -> {
            context(c, UA, TB);
            eq(0, update(c, "DELETE FROM tenancy.memberships WHERE user_id = ?", UA));
            eq(0, update(c, "UPDATE tenancy.memberships SET active = false WHERE user_id = ?", UA));
            return null;
        }));
        check("owner_membership_write_with_scope_rollback", () -> raw(source, false, true, c -> {
            context(c, UA, TA);
            eq(1, update(c, "UPDATE tenancy.memberships SET active = false WHERE user_id = ?", UA));
            eq(1, update(c, "DELETE FROM tenancy.memberships WHERE user_id = ?", UA));
            eq(1, update(c, "INSERT INTO tenancy.memberships (user_id, tenant_id) VALUES (?, ?)", UA, TA));
            return null;
        }));
    }

    private static void commonChecks(DataSource source, TenantTransactions tx, String database) throws Exception {
        check("runtime_identity_no_elevated_role", () -> raw(source, true, false, c -> {
            endpoint(c, database);
            require(RUNTIME.equals(text(c, "SELECT current_user")));
            require(RUNTIME.equals(text(c, "SELECT session_user")));
            eq(1, number(c, "SELECT count(*) FROM pg_roles WHERE rolname = current_user AND rolcanlogin AND NOT rolsuper AND NOT rolbypassrls AND NOT rolcreaterole AND NOT rolcreatedb AND NOT rolreplication"));
            eq(0, number(c, "SELECT count(*) FROM pg_roles WHERE rolname <> current_user AND (rolsuper OR rolbypassrls OR rolcreaterole OR rolcreatedb OR rolname IN ('authority_pro_owner','authority_pro_migrator','pg_database_owner','pg_read_all_data','pg_write_all_data')) AND pg_has_role(current_user, oid, 'MEMBER')"));
            eq(0, number(c, "SELECT count(*) FROM pg_database WHERE datname = current_database() AND datdba = (SELECT oid FROM pg_roles WHERE rolname = current_user)"));
            return null;
        }));
        check("schema_owners_force_rls_precise_grants", () -> raw(source, true, false, c -> {
            eq(4, number(c, "SELECT count(*) FROM pg_class WHERE relnamespace = 'tenancy'::regnamespace AND relkind IN ('r','p')"));
            require(OWNER.equals(text(c, "SELECT pg_get_userbyid(nspowner) FROM pg_namespace WHERE nspname = 'tenancy'")));
            eq(0, number(c, "SELECT count(*) FROM pg_namespace WHERE nspname IN ('tenancy','public') AND has_schema_privilege(current_user, oid, 'CREATE')"));
            eq(0, number(c, "SELECT count(*) FROM pg_database WHERE datname = current_database() AND (has_database_privilege(current_user, oid, 'CREATE') OR has_database_privilege(current_user, oid, 'TEMP'))"));
            for (String table : TABLES) {
                eq(1, number(c, "SELECT count(*) FROM pg_class WHERE oid = to_regclass(?) AND relkind = 'r' AND relrowsecurity AND relforcerowsecurity AND pg_get_userbyid(relowner) = ?", "tenancy." + table, OWNER));
                for (String privilege : new String[]{"SELECT", "INSERT", "UPDATE", "DELETE", "TRUNCATE", "REFERENCES", "TRIGGER"}) {
                    boolean allowed = "SELECT".equals(privilege) || (!"memberships".equals(table) && Set.of("INSERT", "UPDATE", "DELETE").contains(privilege));
                    require(Boolean.toString(allowed).equals(text(c, "SELECT has_table_privilege(current_user, ?, ?)::text", "tenancy." + table, privilege)));
                }
            }
            require("true".equals(text(c, "SELECT has_table_privilege(current_user, 'public.flyway_schema_history', 'SELECT')::text")));
            for (String privilege : new String[]{"INSERT", "UPDATE", "DELETE", "TRUNCATE", "REFERENCES", "TRIGGER"}) {
                require("false".equals(text(c, "SELECT has_table_privilege(current_user, 'public.flyway_schema_history', ?)::text", privilege)));
            }
            eq(0, number(c, "SELECT count(*) FROM pg_class c CROSS JOIN LATERAL aclexplode(coalesce(c.relacl, acldefault('r', c.relowner))) a "
                    + "WHERE (c.relnamespace = 'tenancy'::regnamespace OR c.oid = 'public.flyway_schema_history'::regclass) AND a.grantee = 0"));
            eq(0, number(c, "SELECT count(*) FROM pg_namespace n CROSS JOIN LATERAL aclexplode(coalesce(n.nspacl, acldefault('n', n.nspowner))) a WHERE n.nspname = 'tenancy' AND a.grantee = 0"));
            eq(0, number(c, "SELECT count(*) FROM pg_class c CROSS JOIN LATERAL aclexplode(c.relacl) a WHERE "
                    + "(c.relnamespace = 'tenancy'::regnamespace OR c.oid = 'public.flyway_schema_history'::regclass) "
                    + "AND a.grantee = (SELECT oid FROM pg_roles WHERE rolname = current_user) AND a.is_grantable"));
            eq(0, number(c, "SELECT count(*) FROM pg_default_acl d CROSS JOIN LATERAL aclexplode(d.defaclacl) a "
                    + "WHERE d.defaclrole = (SELECT oid FROM pg_roles WHERE rolname = ?) "
                    + "AND d.defaclnamespace = 'tenancy'::regnamespace AND d.defaclobjtype IN ('r','S') "
                    + "AND (a.grantee = 0 OR a.grantee = (SELECT oid FROM pg_roles WHERE rolname = current_user))", OWNER));
            require(text(c, "SELECT to_regclass('tenancy.failed_v2_marker')::text") == null);
            return null;
        }));
        check("columns_primary_keys_composite_foreign_keys", () -> raw(source, true, false, c -> {
            eq(12, number(c, "SELECT count(*) FROM pg_attribute a JOIN pg_class c ON c.oid = a.attrelid WHERE c.relnamespace = 'tenancy'::regnamespace AND c.relkind = 'r' AND a.attnum > 0 AND NOT a.attisdropped AND a.attnotnull"));
            eq(4, number(c, "SELECT count(*) FROM pg_constraint WHERE connamespace = 'tenancy'::regnamespace AND contype = 'p'"));
            eq(4, number(c, "SELECT count(*) FROM pg_constraint WHERE connamespace = 'tenancy'::regnamespace AND contype = 'p' AND conkey = CASE "
                    + "WHEN conrelid = 'tenancy.tenants'::regclass THEN ARRAY[1]::smallint[] "
                    + "WHEN conrelid = 'tenancy.sites'::regclass THEN ARRAY[1,2,3]::smallint[] ELSE ARRAY[1,2]::smallint[] END"));
            eq(8, number(c, "SELECT count(*) FROM information_schema.columns WHERE table_schema = 'tenancy' AND data_type = 'uuid'"));
            eq(3, number(c, "SELECT count(*) FROM information_schema.columns WHERE table_schema = 'tenancy' AND column_name = 'name' AND data_type = 'text'"));
            eq(1, number(c, "SELECT count(*) FROM information_schema.columns WHERE table_schema = 'tenancy' AND table_name = 'memberships' AND column_name = 'active' AND data_type = 'boolean'"));
            eq(3, number(c, "SELECT count(*) FROM pg_constraint WHERE connamespace = 'tenancy'::regnamespace AND contype = 'f' AND convalidated AND NOT condeferrable"));
            eq(1, number(c, "SELECT count(*) FROM pg_constraint WHERE conrelid = 'tenancy.companies'::regclass AND confrelid = 'tenancy.tenants'::regclass AND contype = 'f' AND conkey = ARRAY[1]::smallint[] AND confkey = ARRAY[1]::smallint[]"));
            eq(1, number(c, "SELECT count(*) FROM pg_constraint WHERE conrelid = 'tenancy.sites'::regclass AND confrelid = 'tenancy.companies'::regclass AND contype = 'f' AND conkey = ARRAY[1,2]::smallint[] AND confkey = ARRAY[1,2]::smallint[]"));
            eq(1, number(c, "SELECT count(*) FROM pg_constraint WHERE conrelid = 'tenancy.memberships'::regclass AND confrelid = 'tenancy.tenants'::regclass AND contype = 'f' AND conkey = ARRAY[2]::smallint[] AND confkey = ARRAY[1]::smallint[]"));
            eq(3, number(c, "SELECT count(*) FROM pg_constraint WHERE connamespace = 'tenancy'::regnamespace AND contype = 'c'"));
            require("true".equals(text(c, "SELECT column_default FROM information_schema.columns WHERE table_schema = 'tenancy' AND table_name = 'memberships' AND column_name = 'active'")));
            return null;
        }));
        check("policy_shape_no_bypass", () -> raw(source, true, false, c -> {
            eq(7, number(c, "SELECT count(*) FROM pg_policies WHERE schemaname = 'tenancy'"));
            eq(3, number(c, "SELECT count(*) FROM pg_policies WHERE schemaname = 'tenancy' AND tablename IN ('tenants','companies','sites') AND cmd = 'ALL' AND qual = with_check AND qual LIKE '%app.tenant_id%' AND qual LIKE '%app.user_id%'"));
            eq(1, number(c, "SELECT count(*) FROM pg_policies WHERE schemaname = 'tenancy' AND tablename = 'memberships' AND cmd = 'SELECT' AND qual LIKE '%app.user_id%' AND qual NOT LIKE '%app.tenant_id%'"));
            eq(3, number(c, "SELECT count(*) FROM pg_policies WHERE schemaname = 'tenancy' AND tablename = 'memberships' AND cmd IN ('INSERT','UPDATE','DELETE') AND roles = ARRAY['authority_pro_owner']::name[]"));
            eq(0, number(c, "SELECT count(*) FROM pg_proc WHERE pronamespace = 'tenancy'::regnamespace"));
            return null;
        }));
        check("missing_scope_reads_nothing", () -> raw(source, true, false, c -> {
            blank(c);
            for (String table : TABLES) eq(0, number(c, "SELECT count(*) FROM tenancy." + table));
            return null;
        }));
        check("tenant_without_user_reads_nothing", () -> raw(source, true, false, c -> {
            text(c, "SELECT set_config('app.tenant_id', ?, true)", TA.toString());
            for (String table : TABLES) eq(0, number(c, "SELECT count(*) FROM tenancy." + table));
            return null;
        }));
        for (boolean a : new boolean[]{true, false}) {
            UUID user = a ? UA : UB;
            UUID tenant = a ? TA : TB;
            check("pretenant_own_membership_" + (a ? "a" : "b"), () -> tx.userScoped(user, true, c -> {
                require(empty(text(c, "SELECT current_setting('app.tenant_id', true)")));
                eq(1, number(c, "SELECT count(*) FROM tenancy.memberships"));
                eq(1, number(c, "SELECT count(*) FROM tenancy.memberships WHERE user_id = ? AND tenant_id = ? AND active", user, tenant));
                eq(0, number(c, "SELECT count(*) FROM tenancy.memberships WHERE user_id <> ?", user));
                for (String table : MUTABLE) eq(0, number(c, "SELECT count(*) FROM tenancy." + table));
                return null;
            }));
            check("scoped_seed_integrity_" + (a ? "a" : "b"), () -> tx.scoped(user, tenant, true, c -> {
                seedBaseline(c, a);
                require("on".equals(text(c, "SELECT current_setting('transaction_read_only')")));
                eq(0, number(c, "SELECT count(*) FROM tenancy.sites s LEFT JOIN tenancy.companies p USING (tenant_id,company_id) WHERE p.company_id IS NULL"));
                return null;
            }));
        }
        // These deliberately malformed direct-JDBC settings are not production wrapper inputs.
        for (String setting : new String[]{"app.user_id", "app.tenant_id"}) {
            check("malformed_context_" + setting.replace('.', '_'), () -> expectSql("22P02", () -> raw(source, true, false, c -> {
                context(c, UA, TA);
                text(c, "SELECT set_config(?, 'not-a-uuid', true)", setting);
                return number(c, "SELECT count(*) FROM tenancy.tenants");
            })));
        }
        check("pool_reset_after_readonly_and_errors", () -> poolBlank(source));
    }

    private static void denialChecks(DataSource source, TenantTransactions tx) throws Exception {
        for (String role : new String[]{OWNER, "authority_pro_migrator", "pg_database_owner"}) {
            check("cannot_assume_" + role, () -> expectSql("42501", () -> raw(source, false, true, c -> {
                execute(c, "SET LOCAL ROLE " + role); return null;
            })));
        }
        String[][] forbidden = {
                {"create_tenancy_table", "CREATE TABLE tenancy.forbidden (id uuid)"},
                {"create_public_table", "CREATE TABLE public.forbidden (id uuid)"},
                {"create_schema", "CREATE SCHEMA forbidden"},
                {"create_temp_table", "CREATE TEMP TABLE forbidden (id uuid)"},
                {"disable_rls", "ALTER TABLE tenancy.tenants DISABLE ROW LEVEL SECURITY"},
                {"remove_force_rls", "ALTER TABLE tenancy.tenants NO FORCE ROW LEVEL SECURITY"},
                {"change_history", "UPDATE public.flyway_schema_history SET checksum = 0"}
        };
        for (String[] denied : forbidden) {
            check("runtime_" + denied[0] + "_denied", () -> expectSql("42501", () -> raw(source, false, true, c -> {
                execute(c, denied[1]); return null;
            })));
        }
        for (String table : TABLES) {
            check("truncate_denied_" + table, () -> expectSql("42501", () -> raw(source, false, true, c -> {
                execute(c, "TRUNCATE tenancy." + table + " CASCADE"); return null;
            })));
        }
        check("row_security_off_is_not_bypass", () -> expectSql("42501", () -> raw(source, true, false, c -> {
            execute(c, "SET LOCAL row_security = off");
            return number(c, "SELECT count(*) FROM tenancy.tenants");
        })));
        check("missing_scope_write_denied", () -> expectSql("42501", () -> raw(source, false, true,
                c -> update(c, "INSERT INTO tenancy.tenants VALUES (?, 'forbidden')", TT))));
        check("missing_user_write_denied", () -> expectSql("42501", () -> raw(source, false, true, c -> {
            text(c, "SELECT set_config('app.tenant_id', ?, true)", TT.toString());
            return update(c, "INSERT INTO tenancy.tenants VALUES (?, 'forbidden')", TT);
        })));
        check("missing_scope_update_delete_invisible", () -> raw(source, false, true, c -> {
            for (String table : MUTABLE) {
                eq(0, update(c, "UPDATE tenancy." + table + " SET name = 'forbidden'"));
                eq(0, update(c, "DELETE FROM tenancy." + table));
            }
            return null;
        }));
        check("pretenant_write_denied", () -> expectSql("42501", () -> tx.userScoped(UA, false,
                c -> update(c, "INSERT INTO tenancy.tenants VALUES (?, 'forbidden')", TT))));
        for (boolean a : new boolean[]{true, false}) {
            UUID user = a ? UA : UB;
            UUID tenant = a ? TA : TB;
            UUID other = a ? TB : TA;
            String suffix = a ? "a" : "b";
            for (String table : MUTABLE) {
                check("cross_tenant_insert_denied_" + table + "_" + suffix, () -> expectSql("42501", () ->
                        rollback(source, tx, user, tenant, c -> {
                            return switch (table) {
                                case "tenants" -> update(c, "INSERT INTO tenancy.tenants VALUES (?, 'forbidden')", TT);
                                case "companies" -> update(c, "INSERT INTO tenancy.companies VALUES (?, ?, 'forbidden')", other, CT);
                                default -> update(c, "INSERT INTO tenancy.sites VALUES (?, ?, ?, 'forbidden')", other, a ? CB : CA, ST);
                            };
                        })));
                check("cross_tenant_update_delete_hidden_" + table + "_" + suffix, () -> rollback(source, tx, user, tenant, c -> {
                    eq(0, number(c, "SELECT count(*) FROM tenancy." + table + " WHERE tenant_id = ?", other));
                    eq(0, update(c, "UPDATE tenancy." + table + " SET name = 'forbidden' WHERE tenant_id = ?", other));
                    eq(0, update(c, "DELETE FROM tenancy." + table + " WHERE tenant_id = ?", other));
                    return null;
                }));
                check("cross_tenant_update_check_" + table + "_" + suffix, () -> expectSql("42501", () ->
                        rollback(source, tx, user, tenant, c -> update(c, "UPDATE tenancy." + table + " SET tenant_id = ? WHERE tenant_id = ?", TT, tenant))));
            }
            check("composite_company_fk_insert_denied_" + suffix, () -> expectSql("23503", () ->
                    rollback(source, tx, user, tenant, c -> update(c, "INSERT INTO tenancy.sites VALUES (?, ?, ?, 'forbidden')", tenant, a ? CB : CA, ST))));
            check("composite_company_fk_update_denied_" + suffix, () -> expectSql("23503", () ->
                    rollback(source, tx, user, tenant, c -> update(c, "UPDATE tenancy.sites SET company_id = ? WHERE site_id = ?", a ? CB : CA, a ? SA : SB))));
            check("referenced_tenant_delete_denied_" + suffix, () -> expectSql("23503", () ->
                    rollback(source, tx, user, tenant, c -> update(c, "DELETE FROM tenancy.tenants WHERE tenant_id = ?", tenant))));
            check("referenced_company_delete_denied_" + suffix, () -> expectSql("23503", () ->
                    rollback(source, tx, user, tenant, c -> update(c, "DELETE FROM tenancy.companies WHERE company_id = ?", a ? CA : CB))));
        }
        check("company_missing_root_fk_denied", () -> expectSql("23503", () -> tx.scoped(UA, TT, false,
                c -> update(c, "INSERT INTO tenancy.companies VALUES (?, ?, 'forbidden')", TT, CT))));
        for (String operation : new String[]{"insert", "update", "delete"}) {
            check("runtime_membership_" + operation + "_denied", () -> expectSql("42501", () -> tx.scoped(UA, TA, false, c -> {
                return switch (operation) {
                    case "insert" -> update(c, "INSERT INTO tenancy.memberships (user_id, tenant_id) VALUES (?, ?)", UA, TB);
                    case "update" -> update(c, "UPDATE tenancy.memberships SET active = false WHERE user_id = ?", UA);
                    default -> update(c, "DELETE FROM tenancy.memberships WHERE user_id = ?", UA);
                };
            })));
        }
        for (String table : MUTABLE) {
            check("nonempty_name_" + table, () -> expectSql("23514", () -> rollback(source, tx, UA, TA,
                    c -> update(c, "UPDATE tenancy." + table + " SET name = '   '"))));
            check("nonnull_name_" + table, () -> expectSql("23502", () -> rollback(source, tx, UA, TA,
                    c -> update(c, "UPDATE tenancy." + table + " SET name = NULL"))));
        }
    }

    private static void mutationChecks(DataSource source, TenantTransactions tx) throws Exception {
        check("runtime_all_dml_commit", () -> tx.scoped(UA, TT, false, c -> {
            eq(1, update(c, "INSERT INTO tenancy.tenants VALUES (?, 'temporary')", TT));
            eq(1, update(c, "INSERT INTO tenancy.companies VALUES (?, ?, 'temporary')", TT, CT));
            eq(1, update(c, "INSERT INTO tenancy.sites VALUES (?, ?, ?, 'temporary')", TT, CT, ST));
            for (String table : MUTABLE) {
                eq(1, number(c, "SELECT count(*) FROM tenancy." + table));
                eq(1, update(c, "UPDATE tenancy." + table + " SET name = 'committed-technical-row' WHERE tenant_id = ?", TT));
            }
            return null;
        }));
        check("runtime_commit_durable_next_transaction", () -> tx.scoped(UA, TT, true, c -> {
            for (String table : MUTABLE) eq(1, number(c, "SELECT count(*) FROM tenancy." + table + " WHERE tenant_id = ? AND name = 'committed-technical-row'", TT));
            return null;
        }));
        check("runtime_delete_commit", () -> tx.scoped(UA, TT, false, c -> {
            for (String table : new String[]{"sites", "companies", "tenants"}) eq(1, update(c, "DELETE FROM tenancy." + table + " WHERE tenant_id = ?", TT));
            return null;
        }));
        check("runtime_delete_durable_next_transaction", () -> tx.scoped(UA, TT, true, c -> {
            for (String table : MUTABLE) eq(0, number(c, "SELECT count(*) FROM tenancy." + table));
            return null;
        }));
        check("explicit_outer_rollback", () -> {
            rollback(source, tx, UA, TA, c -> update(c, "INSERT INTO tenancy.companies VALUES (?, ?, 'rollback')", TA, CT));
            tx.scoped(UA, TA, true, c -> { seedBaseline(c, true); return null; });
        });
        check("sql_error_rolls_back_insert", () -> {
            expectSql("22012", () -> tx.scoped(UA, TA, false, c -> {
                eq(1, update(c, "INSERT INTO tenancy.companies VALUES (?, ?, 'error-rollback')", TA, CT));
                return number(c, "SELECT 1 / 0");
            }));
            tx.scoped(UA, TA, true, c -> { seedBaseline(c, true); return null; });
        });
        check("unchecked_failure_rolls_back_insert", () -> {
            expectIllegalState(() -> tx.scoped(UA, TA, false, c -> {
                eq(1, update(c, "INSERT INTO tenancy.companies VALUES (?, ?, 'error-rollback')", TA, CT));
                throw new IllegalStateException();
            }));
            tx.scoped(UA, TA, true, c -> { seedBaseline(c, true); return null; });
        });
        check("readonly_write_enforced", () -> expectSql("25006", () -> tx.scoped(UA, TA, true,
                c -> update(c, "INSERT INTO tenancy.companies VALUES (?, ?, 'readonly')", TA, CT))));
        check("reset_after_dml_commit_rollback_readonly_error", () -> poolBlank(source));
    }

    private static void transactionChecks(DataSource source, TenantTransactions tx) throws Exception {
        check("pool_one_same_backend_and_context_reset", () -> {
            long pid = tx.scoped(UA, TA, false, c -> { seedBaseline(c, true); return number(c, "SELECT pg_backend_pid()"); });
            eq(pid, raw(source, true, false, c -> { blank(c); return number(c, "SELECT pg_backend_pid()"); }));
            eq(pid, tx.scoped(UB, TB, true, c -> { seedBaseline(c, false); return number(c, "SELECT pg_backend_pid()"); }));
            eq(pid, raw(source, true, false, c -> { blank(c); return number(c, "SELECT pg_backend_pid()"); }));
            expectSql("22012", () -> tx.scoped(UA, TA, false, c -> {
                eq(pid, number(c, "SELECT pg_backend_pid()"));
                return number(c, "SELECT 1 / 0");
            }));
            eq(pid, raw(source, true, false, c -> { blank(c); return number(c, "SELECT pg_backend_pid()"); }));
            poolBlank(source);
        });
        check("joined_same_scope_exact_connection", () -> tx.scoped(UA, TA, false, outer -> {
            long pid = number(outer, "SELECT pg_backend_pid()");
            return tx.scoped(UA, TA, false, inner -> {
                require(outer == inner);
                eq(pid, number(inner, "SELECT pg_backend_pid()"));
                Connection again = DataSourceUtils.getConnection(source);
                try { require(inner == again); } finally { DataSourceUtils.releaseConnection(again, source); }
                seedBaseline(inner, true);
                return null;
            });
        }));
        check("joined_scope_change_rejected_parent_usable", () -> tx.scoped(UA, TA, false, c -> {
            expectIllegal(() -> tx.scoped(UA, TB, false, ignored -> { throw new AssertionError(); }));
            expectIllegal(() -> tx.scoped(UB, TA, false, ignored -> { throw new AssertionError(); }));
            expectIllegal(() -> tx.userScoped(UA, false, ignored -> { throw new AssertionError(); }));
            seedBaseline(c, true);
            return null;
        }));
        check("joined_readonly_mode_cannot_change", () -> {
            tx.scoped(UA, TA, true, c -> {
                expectIllegalState(() -> tx.scoped(UA, TA, false, ignored -> { throw new AssertionError(); }));
                return null;
            });
            tx.scoped(UA, TA, false, c -> {
                expectIllegalState(() -> tx.scoped(UA, TA, true, ignored -> { throw new AssertionError(); }));
                return null;
            });
        });
        check("external_transaction_marker_lasts_until_completion", () -> {
            TransactionTemplate outer = template(source);
            outer.execute(status -> {
                status.setRollbackOnly();
                tx.scoped(UA, TA, false, c -> null);
                expectIllegal(() -> tx.scoped(UB, TB, false, c -> { throw new AssertionError(); }));
                // Separate wrappers cannot silently switch the same physical transaction's context.
                TenantTransactions another = new TenantTransactions(source);
                expectIllegal(() -> another.scoped(UB, TB, false, c -> { throw new AssertionError(); }));
                another.scoped(UA, TA, false, c -> { seedBaseline(c, true); return null; });
                return null;
            });
            tx.scoped(UB, TB, true, c -> { seedBaseline(c, false); return null; });
        });
        check("unmanaged_ambient_context_rejected", () -> {
            TransactionTemplate outer = template(source);
            outer.execute(status -> {
                status.setRollbackOnly();
                Connection c = DataSourceUtils.getConnection(source);
                try { context(c, UA, TA); }
                catch (SQLException failure) { throw new IllegalStateException("Qualification SQL failed", failure); }
                finally { DataSourceUtils.releaseConnection(c, source); }
                expectIllegalState(() -> tx.scoped(UA, TA, false, ignored -> { throw new AssertionError(); }));
                return null;
            });
            poolBlank(source);
        });
    }

    private static void parallelChecks(DataSource source, TenantTransactions tx) throws Exception {
        check("requires_new_separate_connection_restores_parent", () -> rollback(source, tx, UA, TA, outer -> {
            long outerPid = number(outer, "SELECT pg_backend_pid()");
            eq(1, update(outer, "INSERT INTO tenancy.companies VALUES (?, ?, 'outer-uncommitted')", TA, CT));
            tx.requiresNew(UB, TB, true, inner -> {
                require(inner != outer);
                require(outerPid != number(inner, "SELECT pg_backend_pid()"));
                seedBaseline(inner, false);
                return null;
            });
            tx.requiresNew(UA, TA, true, inner -> {
                eq(0, number(inner, "SELECT count(*) FROM tenancy.companies WHERE company_id = ?", CT));
                return null;
            });
            eq(outerPid, number(outer, "SELECT pg_backend_pid()"));
            eq(1, number(outer, "SELECT count(*) FROM tenancy.companies WHERE company_id = ?", CT));
            require(UA.toString().equals(text(outer, "SELECT current_setting('app.user_id')")));
            require(TA.toString().equals(text(outer, "SELECT current_setting('app.tenant_id')")));
            return null;
        }));
        check("concurrent_two_connection_isolation", () -> {
            CyclicBarrier barrier = new CyclicBarrier(2);
            var executor = Executors.newFixedThreadPool(2);
            try {
                var a = executor.submit(() -> concurrent(source, tx, true, barrier));
                var b = executor.submit(() -> concurrent(source, tx, false, barrier));
                require(!a.get(30, TimeUnit.SECONDS).equals(b.get(30, TimeUnit.SECONDS)));
            } finally {
                executor.shutdownNow();
                require(executor.awaitTermination(35, TimeUnit.SECONDS));
            }
            try (Connection a = source.getConnection(); Connection b = source.getConnection()) {
                reset(a); reset(b);
                require(number(a, "SELECT pg_backend_pid()") != number(b, "SELECT pg_backend_pid()"));
            }
            tx.scoped(UA, TA, true, c -> { seedBaseline(c, true); return null; });
            tx.scoped(UB, TB, true, c -> { seedBaseline(c, false); return null; });
        });
    }

    private static long concurrent(DataSource source, TenantTransactions tx, boolean a, CyclicBarrier barrier) {
        UUID tenant = a ? TA : TB;
        return rollback(source, tx, a ? UA : UB, tenant, c -> {
            seedBaseline(c, a);
            long pid = number(c, "SELECT pg_backend_pid()");
            eq(1, update(c, "INSERT INTO tenancy.companies VALUES (?, ?, 'concurrent')", tenant, CT));
            await(barrier);
            for (int i = 0; i < 5; i++) {
                eq(2, number(c, "SELECT count(*) FROM tenancy.companies"));
                eq(0, number(c, "SELECT count(*) FROM tenancy.companies WHERE tenant_id <> ?", tenant));
                eq(pid, number(c, "SELECT pg_backend_pid()"));
            }
            await(barrier);
            return pid;
        });
    }

    private static void await(CyclicBarrier barrier) {
        try { barrier.await(10, TimeUnit.SECONDS); }
        catch (InterruptedException failure) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Qualification interrupted", failure);
        } catch (Exception failure) { throw new IllegalStateException("Qualification barrier failed", failure); }
    }

    private static void seedBaseline(Connection c, boolean a) throws SQLException {
        UUID tenant = a ? TA : TB;
        String suffix = a ? "a" : "b";
        for (String table : TABLES) eq(1, number(c, "SELECT count(*) FROM tenancy." + table));
        for (String table : MUTABLE) eq(0, number(c, "SELECT count(*) FROM tenancy." + table + " WHERE tenant_id <> ?", tenant));
        eq(1, number(c, "SELECT count(*) FROM tenancy.tenants WHERE tenant_id = ? AND name = ?", tenant, "fixture-tenant-" + suffix));
        eq(1, number(c, "SELECT count(*) FROM tenancy.companies WHERE tenant_id = ? AND company_id = ? AND name = ?", tenant, a ? CA : CB, "fixture-company-" + suffix));
        eq(1, number(c, "SELECT count(*) FROM tenancy.sites WHERE tenant_id = ? AND company_id = ? AND site_id = ? AND name = ?", tenant, a ? CA : CB, a ? SA : SB, "fixture-site-" + suffix));
        eq(1, number(c, "SELECT count(*) FROM tenancy.memberships WHERE user_id = ? AND tenant_id = ? AND active", a ? UA : UB, tenant));
    }

    private static TransactionTemplate template(DataSource source) {
        DataSourceTransactionManager manager = new DataSourceTransactionManager(source);
        manager.setEnforceReadOnly(true);
        manager.setValidateExistingTransaction(true);
        TransactionTemplate template = new TransactionTemplate(manager);
        template.setTimeout(25);
        return template;
    }

    private static <T> T rollback(DataSource source, TenantTransactions tx, UUID user, UUID tenant,
                                  TenantTransactions.Work<T> work) {
        return template(source).execute(status -> {
            status.setRollbackOnly();
            return tx.scoped(user, tenant, false, work);
        });
    }

    private static <T> T raw(DataSource source, boolean readOnly, boolean rollback,
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

    private static void poolBlank(DataSource source) throws SQLException {
        try (Connection c = source.getConnection()) { reset(c); }
    }

    private static void reset(Connection c) throws SQLException {
        require(c.getAutoCommit());
        require(!c.isReadOnly());
        blank(c);
        require("off".equals(text(c, "SELECT current_setting('transaction_read_only')")));
        require("on".equals(text(c, "SELECT current_setting('row_security')")));
        require(RUNTIME.equals(text(c, "SELECT current_user")));
    }

    private static void blank(Connection c) throws SQLException {
        require(empty(text(c, "SELECT current_setting('app.user_id', true)")));
        require(empty(text(c, "SELECT current_setting('app.tenant_id', true)")));
    }

    private static boolean empty(String value) { return value == null || value.isEmpty(); }

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

    private static void expectIllegal(Runnable work) {
        try { work.run(); }
        catch (IllegalArgumentException expected) { return; }
        throw new AssertionError();
    }

    private static void expectIllegalState(Runnable work) {
        try { work.run(); }
        catch (IllegalStateException expected) { return; }
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
