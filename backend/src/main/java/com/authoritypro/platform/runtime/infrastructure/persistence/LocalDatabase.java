package com.authoritypro.platform.runtime.infrastructure.persistence;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Set;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.postgresql.ds.PGSimpleDataSource;
import org.springframework.boot.logging.LogLevel;
import org.springframework.boot.logging.LoggingSystem;

public final class LocalDatabase {
    private static final String DATABASE = "authority_pro";
    private static final String OWNER = "authority_pro_owner";
    private static final String RUNTIME = "authority_pro_runtime";
    private static final String PROVISIONER = "authority_pro_provisioner";
    private static final String MIGRATOR = "authority_pro_migrator";
    private static final String AUDIT_TEST_DATABASE = "authority_pro_l04_audit_test";
    private static final String IDENTITY_TEST_DATABASE = "authority_pro_l05_identity_test";
    private static final Set<String> DATABASES = Set.of(DATABASE, "authority_pro_l03_app_test",
            "authority_pro_l03_app_restore", AUDIT_TEST_DATABASE, IDENTITY_TEST_DATABASE);

    private enum Profile { MAIN, AUDIT, IDENTITY }

    private LocalDatabase() {}

    public static String readPassword(InputStream input) {
        return readSecrets(input, 1)[0];
    }

    /** Reads exactly {@code count} bounded UTF-8 lines and requires end-of-stream after the last one. */
    public static String[] readSecrets(InputStream input, int count) {
        if (count < 1 || count > 8) throw refused();
        String[] lines = new String[count];
        for (int index = 0; index < count; index++) {
            lines[index] = readLine(input, index == count - 1);
        }
        return lines;
    }

    private static String readLine(InputStream input, boolean last) {
        byte[] bytes = new byte[256];
        try {
            int length = 0;
            int value;
            while ((value = input.read()) != '\n') {
                if (value == '\r') {
                    if (input.read() != '\n') {
                        throw refused();
                    }
                    break;
                }
                if (value < 0 || value == 0 || length == bytes.length) {
                    throw refused();
                }
                bytes[length++] = (byte) value;
            }
            if (length == 0 || (last && input.read() != -1)) {
                throw refused();
            }
            return StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(bytes, 0, length)).toString();
        } catch (IOException | RuntimeException exception) {
            throw refused();
        } finally {
            Arrays.fill(bytes, (byte) 0);
        }
    }

    public static HikariDataSource createRuntime(String password) {
        return runtimeDataSource(DATABASE, password);
    }

    public static HikariDataSource runtimeDataSource(String database, String password) {
        HikariDataSource source = pool(database, password, RUNTIME, false);
        try {
            verifyRuntime(source);
            return source;
        } catch (RuntimeException exception) {
            source.close();
            throw refused();
        }
    }

    public static HikariDataSource provisionerDataSource(String database, String password) {
        HikariDataSource source = pool(database, password, PROVISIONER, false);
        try {
            verifyProvisioner(source);
            return source;
        } catch (RuntimeException exception) {
            source.close();
            throw refused();
        }
    }

    public static HikariDataSource migrationDataSource(String database, String password) {
        HikariDataSource source = pool(database, password, MIGRATOR, true);
        try {
            verifyMigration(source);
            return source;
        } catch (RuntimeException exception) {
            source.close();
            throw refused();
        }
    }

    public static int migrate(String password) {
        return migrate(DATABASE, password);
    }

    public static int migrateAuditTest(String password) {
        return migrate(AUDIT_TEST_DATABASE, password);
    }

    public static int migrateIdentityTest(String password) {
        return migrate(IDENTITY_TEST_DATABASE, password);
    }

    private static int migrate(String database, String password) {
        // The main database stays at V1 until a separate L05 main-migration authorization.
        if (!AUDIT_TEST_DATABASE.equals(database) && !IDENTITY_TEST_DATABASE.equals(database)) throw refused();
        try (HikariDataSource source = migrationDataSource(database, password)) {
            Flyway migration = AUDIT_TEST_DATABASE.equals(database)
                    ? flyway(source, "2")
                    : flyway(source, null);
            migration.validate();
            int count = migration.migrate().migrationsExecuted;
            verifySchema(source, IDENTITY_TEST_DATABASE.equals(database) ? Profile.IDENTITY : Profile.AUDIT);
            return count;
        } catch (RuntimeException exception) {
            throw refused();
        }
    }

    public static Flyway flyway(DataSource source) {
        return flyway(source, null);
    }

    private static Flyway flyway(DataSource source, String target) {
        silenceDependencies();
        try {
            verifyMigration(source);
            var configuration = Flyway.configure().dataSource(source).locations("classpath:db/migration")
                    .defaultSchema("public").schemas("public").table("flyway_schema_history")
                    .createSchemas(false).cleanDisabled(true).baselineOnMigrate(false).outOfOrder(false)
                    .validateMigrationNaming(true).validateOnMigrate(true).ignoreMigrationPatterns("*:pending")
                    .connectRetries(0).loggers("slf4j");
            if (target != null) {
                configuration = configuration.target(org.flywaydb.core.api.MigrationVersion.fromVersion(target));
            }
            return configuration.load();
        } catch (RuntimeException exception) {
            throw refused();
        }
    }

    public static void verifyRuntime(DataSource source) {
        silenceDependencies();
        try (Connection connection = source.getConnection()) {
            verifyIdentity(connection, false, RUNTIME);
            require(connection, """
                    SELECT NOT EXISTS (
                        SELECT 1 FROM pg_catalog.pg_auth_members m
                        JOIN pg_catalog.pg_roles r ON r.oid = m.member
                        WHERE r.rolname = 'authority_pro_runtime')
                    AND NOT has_database_privilege(current_user, current_database(), 'CREATE')
                    AND NOT has_database_privilege(current_user, current_database(), 'TEMP')
                    AND NOT EXISTS (
                        SELECT 1 FROM pg_catalog.pg_namespace n
                        WHERE n.nspname !~ '^pg_' AND n.nspname <> 'information_schema'
                        AND has_schema_privilege(current_user, n.oid, 'CREATE'))
                    """);
            verifySchema(connection, profile(connection));
        } catch (SQLException | RuntimeException exception) {
            throw refused();
        }
    }

    private static void verifyProvisioner(DataSource source) {
        silenceDependencies();
        try (Connection connection = source.getConnection()) {
            verifyIdentity(connection, false, PROVISIONER);
            require(connection, """
                    SELECT NOT EXISTS (
                        SELECT 1 FROM pg_catalog.pg_auth_members m
                        JOIN pg_catalog.pg_roles r ON r.oid = m.member
                        WHERE r.rolname = 'authority_pro_provisioner')
                    AND NOT has_database_privilege(current_user, current_database(), 'CREATE')
                    AND NOT has_database_privilege(current_user, current_database(), 'TEMP')
                    AND NOT EXISTS (
                        SELECT 1 FROM pg_catalog.pg_namespace n
                        WHERE n.nspname !~ '^pg_' AND n.nspname <> 'information_schema'
                        AND has_schema_privilege(current_user, n.oid, 'CREATE'))
                    """);
        } catch (SQLException | RuntimeException exception) {
            throw refused();
        }
    }

    private static HikariDataSource pool(String database, String password, String user, boolean migration) {
        if (!DATABASES.contains(database == null ? "" : database) || password == null || password.isEmpty()
                || password.getBytes(StandardCharsets.UTF_8).length > 256
                || password.indexOf('\n') >= 0 || password.indexOf('\r') >= 0 || password.indexOf('\0') >= 0) {
            throw refused();
        }
        silenceDependencies();
        try {
            var postgres = new PGSimpleDataSource();
            postgres.setServerNames(new String[]{"127.0.0.1"});
            postgres.setPortNumbers(new int[]{5434});
            postgres.setDatabaseName(database);
            postgres.setUser(user);
            postgres.setPassword(password);
            postgres.setConnectTimeout(3);
            postgres.setSocketTimeout(5);
            postgres.setLoginTimeout(5);
            postgres.setQueryTimeout(5);
            postgres.setCancelSignalTimeout(2);
            postgres.setLogServerErrorDetail(false);
            postgres.setOptions("-c statement_timeout=5000 -c lock_timeout=3000 -c row_security=on -c search_path=pg_catalog,public");
            postgres.setApplicationName("authority-pro-" + (migration ? "migration" : user.substring("authority_pro_".length())));
            var config = new HikariConfig();
            config.setDataSource(postgres);
            config.setPoolName("local-" + (migration ? "migration" : user.substring("authority_pro_".length())));
            config.setMaximumPoolSize(PROVISIONER.equals(user) ? 1 : 2);
            config.setMinimumIdle(0);
            config.setConnectionTimeout(5000);
            config.setValidationTimeout(2000);
            config.setInitializationFailTimeout(-1);
            if (migration) {
                config.setConnectionInitSql("SET ROLE authority_pro_owner");
            }
            return new HikariDataSource(config);
        } catch (RuntimeException exception) {
            throw refused();
        }
    }

    private static void verifyMigration(DataSource source) {
        try (Connection connection = source.getConnection()) {
            verifyIdentity(connection, true, MIGRATOR);
            require(connection, """
                    SELECT EXISTS (SELECT 1 FROM pg_catalog.pg_namespace
                        WHERE nspname = 'public' AND pg_catalog.pg_get_userbyid(nspowner) = 'authority_pro_owner')
                    AND (SELECT count(*) = 1 AND bool_and(parent.rolname = 'authority_pro_owner'
                        AND NOT m.admin_option AND NOT m.inherit_option AND m.set_option)
                        FROM pg_catalog.pg_auth_members m
                        JOIN pg_catalog.pg_roles parent ON parent.oid = m.roleid
                        JOIN pg_catalog.pg_roles member ON member.oid = m.member
                        WHERE member.rolname = 'authority_pro_migrator')
                    """);
            boolean history;
            try (var statement = connection.createStatement()) {
                statement.setQueryTimeout(5);
                try (var result = statement.executeQuery("SELECT to_regclass('public.flyway_schema_history') IS NOT NULL")) {
                    history = result.next() && result.getBoolean(1);
                }
            }
            if (history) {
                // A database still at V1 is a valid pre-migration state; the profile follows the last applied version.
                boolean audited = exists(connection, "SELECT EXISTS (SELECT 1 FROM public.flyway_schema_history "
                        + "WHERE version = '2' AND success)");
                boolean identified = audited && exists(connection,
                        "SELECT EXISTS (SELECT 1 FROM public.flyway_schema_history WHERE version = '3' AND success)");
                boolean sessioned = identified && exists(connection,
                        "SELECT EXISTS (SELECT 1 FROM public.flyway_schema_history WHERE version = '4' AND success)");
                if (identified && !sessioned) {
                    verifyIdentityFoundationV3(connection);
                } else {
                    verifySchema(connection, sessioned ? Profile.IDENTITY : audited ? Profile.AUDIT : Profile.MAIN);
                }
            } else {
                require(connection, """
                        SELECT (SELECT count(*) = 1 AND bool_and(nspname = 'public')
                            FROM pg_catalog.pg_namespace WHERE nspname !~ '^pg_' AND nspname <> 'information_schema')
                        AND NOT EXISTS (SELECT 1 FROM pg_catalog.pg_class c
                            JOIN pg_catalog.pg_namespace n ON n.oid = c.relnamespace
                            WHERE n.nspname = 'public' AND c.relkind IN ('r','p','v','m','f','S'))
                        """);
            }
        } catch (SQLException | RuntimeException exception) {
            throw refused();
        }
    }

    /** Pre-migration state: L05 schema applied through V3, before the session store and grants. */
    private static void verifyIdentityFoundationV3(Connection connection) throws SQLException {
        require(connection, """
                SELECT count(*) = 5 AND bool_and(pg_catalog.pg_get_userbyid(nspowner) = 'authority_pro_owner')
                FROM pg_catalog.pg_namespace
                WHERE nspname IN ('public', 'tenancy', 'audit', 'identity', 'access')
                """);
        require(connection, """
                SELECT count(*) = 6 AND bool_and(pg_catalog.pg_get_userbyid(c.relowner) = 'authority_pro_owner')
                FROM pg_catalog.pg_class c JOIN pg_catalog.pg_namespace n ON n.oid = c.relnamespace
                WHERE n.nspname IN ('identity', 'access') AND c.relkind = 'r'
                """);
        require(connection, """
                SELECT count(*) = 3 AND bool_and(success)
                FROM public.flyway_schema_history
                """);
    }

    private static void verifyIdentity(Connection connection, boolean migration, String sessionRole)
            throws SQLException {
        try (var statement = connection.createStatement()) {
            statement.setQueryTimeout(5);
            try (var result = statement.executeQuery("SELECT current_database(), current_user, session_user")) {
                if (!result.next() || !DATABASES.contains(result.getString(1))
                        || !(migration ? OWNER : sessionRole).equals(result.getString(2))
                        || !(migration ? MIGRATOR : sessionRole).equals(result.getString(3))) {
                    throw refused();
                }
            }
        }
        require(connection, """
                SELECT current_setting('row_security') = 'on'
                    AND current_setting('statement_timeout')::interval > interval '0'
                    AND current_setting('statement_timeout')::interval <= interval '5 seconds'
                    AND current_setting('lock_timeout')::interval > interval '0'
                    AND current_setting('lock_timeout')::interval <= interval '3 seconds'
                    AND NOT EXISTS (
                        SELECT 1 FROM pg_catalog.pg_roles WHERE rolname IN (current_user, session_user)
                        AND (rolsuper OR rolbypassrls OR rolcreatedb OR rolcreaterole OR rolreplication OR rolinherit))
                    AND EXISTS (SELECT 1 FROM pg_catalog.pg_roles WHERE rolname = session_user AND rolcanlogin
                        AND rolconnlimit = CASE session_user WHEN 'authority_pro_runtime' THEN 5
                            WHEN 'authority_pro_provisioner' THEN 1 ELSE 2 END)
                    AND EXISTS (SELECT 1 FROM pg_catalog.pg_roles WHERE rolname = 'authority_pro_owner'
                        AND NOT rolcanlogin AND NOT rolsuper AND NOT rolbypassrls AND NOT rolcreatedb
                        AND NOT rolcreaterole AND NOT rolreplication AND NOT rolinherit)
                    AND EXISTS (SELECT 1 FROM pg_catalog.pg_database WHERE datname = current_database()
                        AND pg_catalog.pg_get_userbyid(datdba) = 'authority_pro_owner')
                """);
    }

    private static Profile profile(Connection connection) throws SQLException {
        String database = textValue(connection, "SELECT current_database()");
        if (IDENTITY_TEST_DATABASE.equals(database)) return Profile.IDENTITY;
        if (AUDIT_TEST_DATABASE.equals(database)) return Profile.AUDIT;
        return Profile.MAIN;
    }

    private static void verifySchema(DataSource source, Profile profile) {
        try (Connection connection = source.getConnection()) {
            verifySchema(connection, profile);
        } catch (SQLException | RuntimeException exception) {
            throw refused();
        }
    }

    private static void verifySchema(Connection connection, Profile profile) throws SQLException {
        boolean audit = profile != Profile.MAIN;
        boolean identity = profile == Profile.IDENTITY;
        require(connection, switch (profile) {
            case MAIN -> """
                SELECT count(*) = 2 AND bool_and(pg_catalog.pg_get_userbyid(nspowner) = 'authority_pro_owner')
                    AND bool_and(nspname IN ('public', 'tenancy'))
                FROM pg_catalog.pg_namespace WHERE nspname !~ '^pg_' AND nspname <> 'information_schema'
                """;
            case AUDIT -> """
                SELECT count(*) = 3 AND bool_and(pg_catalog.pg_get_userbyid(nspowner) = 'authority_pro_owner')
                    AND bool_and(nspname IN ('public', 'tenancy', 'audit'))
                FROM pg_catalog.pg_namespace WHERE nspname !~ '^pg_' AND nspname <> 'information_schema'
                """;
            case IDENTITY -> """
                SELECT count(*) = 5 AND bool_and(pg_catalog.pg_get_userbyid(nspowner) = 'authority_pro_owner')
                    AND bool_and(nspname IN ('public', 'tenancy', 'audit', 'identity', 'access'))
                FROM pg_catalog.pg_namespace WHERE nspname !~ '^pg_' AND nspname <> 'information_schema'
                """;
        });
        require(connection, switch (profile) {
            case MAIN -> """
                SELECT count(*) = 5 AND bool_and(pg_catalog.pg_get_userbyid(c.relowner) = 'authority_pro_owner')
                    AND bool_and(c.relkind = 'r' AND (
                        (n.nspname = 'public' AND c.relname = 'flyway_schema_history') OR
                        (n.nspname = 'tenancy' AND c.relname IN ('tenants','companies','sites','memberships')
                            AND c.relrowsecurity AND c.relforcerowsecurity)))
                FROM pg_catalog.pg_class c JOIN pg_catalog.pg_namespace n ON n.oid = c.relnamespace
                WHERE n.nspname IN ('public','tenancy') AND c.relkind IN ('r','p','v','m','f','S')
                """;
            case AUDIT -> """
                SELECT count(*) = 8 AND bool_and(pg_catalog.pg_get_userbyid(c.relowner) = 'authority_pro_owner')
                    AND bool_and(c.relkind = 'r' AND (
                        (n.nspname = 'public' AND c.relname = 'flyway_schema_history') OR
                        (n.nspname = 'tenancy' AND c.relname IN ('tenants','companies','sites','memberships')
                            AND c.relrowsecurity AND c.relforcerowsecurity) OR
                        (n.nspname = 'audit' AND c.relname IN ('tenant_records','control_plane_records','security_attempts')
                            AND ((c.relname = 'tenant_records' AND c.relrowsecurity AND c.relforcerowsecurity)
                                OR (c.relname <> 'tenant_records' AND NOT c.relrowsecurity AND NOT c.relforcerowsecurity)))))
                FROM pg_catalog.pg_class c JOIN pg_catalog.pg_namespace n ON n.oid = c.relnamespace
                WHERE n.nspname IN ('public','tenancy','audit') AND c.relkind IN ('r','p','v','m','f','S')
                """;
            case IDENTITY -> """
                SELECT count(*) = 16 AND bool_and(pg_catalog.pg_get_userbyid(c.relowner) = 'authority_pro_owner')
                    AND bool_and(c.relkind = 'r' AND (
                        (n.nspname = 'public' AND c.relname = 'flyway_schema_history') OR
                        (n.nspname = 'tenancy' AND c.relname IN ('tenants','companies','sites','memberships')
                            AND c.relrowsecurity AND c.relforcerowsecurity) OR
                        (n.nspname = 'audit' AND c.relname IN ('tenant_records','control_plane_records','security_attempts')
                            AND ((c.relname = 'tenant_records' AND c.relrowsecurity AND c.relforcerowsecurity)
                                OR (c.relname <> 'tenant_records' AND NOT c.relrowsecurity AND NOT c.relforcerowsecurity))) OR
                        (n.nspname = 'identity' AND c.relname IN ('users','password_credentials','provisioning_guard',
                            'spring_session','spring_session_attributes')
                            AND NOT c.relrowsecurity AND NOT c.relforcerowsecurity) OR
                        (n.nspname = 'access' AND c.relname IN ('roles','role_permissions','membership_roles')
                            AND NOT c.relrowsecurity AND NOT c.relforcerowsecurity)))
                FROM pg_catalog.pg_class c JOIN pg_catalog.pg_namespace n ON n.oid = c.relnamespace
                WHERE n.nspname IN ('public','tenancy','audit','identity','access') AND c.relkind IN ('r','p','v','m','f','S')
                """;
        });
        require(connection, switch (profile) {
            case MAIN -> """
                SELECT count(*) = 1 AND bool_and(version = '1' AND success AND type = 'SQL'
                    AND script LIKE 'V1\\_\\_%' ESCAPE '\\' AND checksum IS NOT NULL)
                FROM public.flyway_schema_history
                """;
            case AUDIT -> """
                SELECT count(*) = 2 AND bool_and(success AND type = 'SQL' AND checksum IS NOT NULL)
                    AND count(*) FILTER (WHERE version = '1' AND script LIKE 'V1\\_\\_%' ESCAPE '\\') = 1
                    AND count(*) FILTER (WHERE version = '2' AND script LIKE 'V2\\_\\_%' ESCAPE '\\') = 1
                FROM public.flyway_schema_history
                """;
            case IDENTITY -> """
                SELECT count(*) = 4 AND bool_and(success AND type = 'SQL' AND checksum IS NOT NULL)
                    AND count(*) FILTER (WHERE version = '1' AND script LIKE 'V1\\_\\_%' ESCAPE '\\') = 1
                    AND count(*) FILTER (WHERE version = '2' AND script LIKE 'V2\\_\\_%' ESCAPE '\\') = 1
                    AND count(*) FILTER (WHERE version = '3' AND script LIKE 'V3\\_\\_%' ESCAPE '\\') = 1
                    AND count(*) FILTER (WHERE version = '4' AND script LIKE 'V4\\_\\_%' ESCAPE '\\') = 1
                FROM public.flyway_schema_history
                """;
        });
        require(connection, """
                SELECT count(*) = 4 AND bool_and(columns = CASE relname
                    WHEN 'tenants' THEN 'name,tenant_id'
                    WHEN 'companies' THEN 'company_id,name,tenant_id'
                    WHEN 'sites' THEN 'company_id,name,site_id,tenant_id'
                    WHEN 'memberships' THEN '%s' END)
                FROM (
                    SELECT c.relname, string_agg(a.attname, ',' ORDER BY a.attname) AS columns
                    FROM pg_catalog.pg_attribute a JOIN pg_catalog.pg_class c ON c.oid = a.attrelid
                    JOIN pg_catalog.pg_namespace n ON n.oid = c.relnamespace
                    WHERE n.nspname = 'tenancy' AND c.relkind = 'r' AND a.attnum > 0 AND NOT a.attisdropped
                    GROUP BY c.relname
                ) expected
                """.formatted(identity ? "active,authorization_revision,disabled_at,tenant_id,user_id"
                        : "active,tenant_id,user_id"));
        if (audit) verifyAuditSchema(connection);
        if (identity) verifyIdentityAccessSchema(connection);
    }

    private static void verifyIdentityAccessSchema(Connection connection) throws SQLException {
        require(connection, """
                SELECT count(*) = 2 AND bool_and(pg_catalog.pg_get_userbyid(nspowner) = 'authority_pro_owner'
                    AND has_schema_privilege('authority_pro_runtime', nspname, 'USAGE')
                    AND NOT has_schema_privilege('authority_pro_runtime', nspname, 'CREATE'))
                FROM pg_catalog.pg_namespace WHERE nspname IN ('identity', 'access')
                """);
        require(connection, """
                SELECT count(*) = 8 AND bool_and(columns = CASE relname
                    WHEN 'users' THEN 'authorization_revision,created_at,display_name,login_normalized,status,user_id'
                    WHEN 'password_credentials' THEN 'changed_at,password_hash,scheme,user_id'
                    WHEN 'provisioning_guard' THEN 'operator_user_id,provisioned_at,revision,singleton'
                    WHEN 'spring_session' THEN
                        'creation_time,expiry_time,last_access_time,max_inactive_interval,primary_id,principal_name,session_id'
                    WHEN 'spring_session_attributes' THEN 'attribute_bytes,attribute_name,session_primary_id'
                    WHEN 'roles' THEN 'role_code'
                    WHEN 'role_permissions' THEN 'permission_code,role_code'
                    WHEN 'membership_roles' THEN
                        'authorization_revision,company_id,role_code,site_id,tenant_id,user_id' END)
                FROM (
                    SELECT c.relname, string_agg(a.attname, ',' ORDER BY a.attname) AS columns
                    FROM pg_catalog.pg_attribute a JOIN pg_catalog.pg_class c ON c.oid = a.attrelid
                    JOIN pg_catalog.pg_namespace n ON n.oid = c.relnamespace
                    WHERE n.nspname IN ('identity', 'access') AND c.relkind = 'r'
                        AND a.attnum > 0 AND NOT a.attisdropped
                    GROUP BY c.relname
                ) expected
                """);
        require(connection, """
                SELECT count(*) = 3 AND bool_and(role_code IN ('TENANT_OWNER', 'TENANT_ADMIN', 'TENANT_AUDITOR'))
                FROM access.roles
                """);
        require(connection, """
                SELECT count(*) = 10
                    AND count(*) FILTER (WHERE role_code = 'TENANT_OWNER') = 5
                    AND count(*) FILTER (WHERE role_code = 'TENANT_ADMIN') = 2
                    AND count(*) FILTER (WHERE role_code = 'TENANT_AUDITOR') = 3
                FROM access.role_permissions
                """);
        require(connection, """
                SELECT bool_and(has_table_privilege('authority_pro_runtime', 'identity.' || c.relname, 'SELECT'))
                FROM pg_catalog.pg_class c JOIN pg_catalog.pg_namespace n ON n.oid = c.relnamespace
                WHERE n.nspname = 'identity' AND c.relkind = 'r'
                    AND c.relname IN ('users', 'password_credentials')
                """);
        require(connection, """
                SELECT bool_and(has_table_privilege('authority_pro_runtime', 'access.' || c.relname, 'SELECT'))
                FROM pg_catalog.pg_class c JOIN pg_catalog.pg_namespace n ON n.oid = c.relnamespace
                WHERE n.nspname = 'access' AND c.relkind = 'r'
                """);
        require(connection, """
                SELECT NOT has_table_privilege('authority_pro_runtime', 'identity.provisioning_guard', 'SELECT')
                """);
        require(connection, """
                SELECT count(*) = 2 AND bool_and(
                    has_table_privilege('authority_pro_runtime', 'identity.' || c.relname, 'SELECT')
                    AND has_table_privilege('authority_pro_runtime', 'identity.' || c.relname, 'INSERT')
                    AND has_table_privilege('authority_pro_runtime', 'identity.' || c.relname, 'UPDATE')
                    AND has_table_privilege('authority_pro_runtime', 'identity.' || c.relname, 'DELETE')
                    AND NOT has_table_privilege('authority_pro_runtime', 'identity.' || c.relname, 'TRUNCATE')
                    AND NOT has_table_privilege('authority_pro_runtime', 'identity.' || c.relname, 'REFERENCES')
                    AND NOT has_table_privilege('authority_pro_runtime', 'identity.' || c.relname, 'TRIGGER'))
                FROM pg_catalog.pg_class c JOIN pg_catalog.pg_namespace n ON n.oid = c.relnamespace
                WHERE n.nspname = 'identity' AND c.relkind = 'r'
                    AND c.relname IN ('spring_session', 'spring_session_attributes')
                """);
        require(connection, """
                SELECT has_schema_privilege('authority_pro_provisioner', 'identity', 'USAGE')
                    AND has_schema_privilege('authority_pro_provisioner', 'audit', 'USAGE')
                    AND NOT has_schema_privilege('authority_pro_provisioner', 'access', 'USAGE')
                    AND has_table_privilege('authority_pro_provisioner', 'identity.provisioning_guard', 'SELECT')
                    AND has_table_privilege('authority_pro_provisioner', 'identity.provisioning_guard', 'INSERT')
                    AND has_table_privilege('authority_pro_provisioner', 'identity.users', 'INSERT')
                    AND NOT has_table_privilege('authority_pro_provisioner', 'identity.users', 'SELECT')
                    AND NOT has_table_privilege('authority_pro_provisioner', 'identity.users', 'UPDATE')
                    AND has_table_privilege('authority_pro_provisioner', 'identity.password_credentials', 'INSERT')
                    AND has_table_privilege('authority_pro_provisioner', 'audit.control_plane_records', 'INSERT')
                    AND has_column_privilege('authority_pro_provisioner', 'audit.control_plane_records',
                        'occurred_at', 'SELECT')
                    AND NOT has_table_privilege('authority_pro_provisioner', 'audit.control_plane_records', 'SELECT')
                    AND NOT has_table_privilege('authority_pro_provisioner', 'identity.spring_session', 'SELECT')
                    AND NOT has_table_privilege('authority_pro_provisioner', 'audit.tenant_records', 'INSERT')
                """);
        require(connection, """
                SELECT count(*) = 0
                FROM pg_catalog.pg_class c JOIN pg_catalog.pg_namespace n ON n.oid = c.relnamespace
                CROSS JOIN LATERAL aclexplode(coalesce(c.relacl, acldefault('r', c.relowner))) a
                WHERE n.nspname IN ('identity', 'access')
                    AND a.grantee = (SELECT oid FROM pg_catalog.pg_roles WHERE rolname = 'authority_pro_runtime')
                    AND a.privilege_type IN ('INSERT', 'UPDATE', 'DELETE', 'TRUNCATE', 'REFERENCES', 'TRIGGER')
                    AND c.relname NOT IN ('spring_session', 'spring_session_attributes')
                """);
        require(connection, """
                SELECT count(*) = 0
                FROM pg_catalog.pg_class c JOIN pg_catalog.pg_namespace n ON n.oid = c.relnamespace
                CROSS JOIN LATERAL aclexplode(coalesce(c.relacl, acldefault('r', c.relowner))) a
                WHERE n.nspname IN ('identity', 'access') AND (a.grantee = 0 OR a.is_grantable)
                """);
        require(connection, """
                SELECT count(*) = 0
                FROM pg_catalog.pg_namespace n
                CROSS JOIN LATERAL aclexplode(coalesce(n.nspacl, acldefault('n', n.nspowner))) a
                WHERE n.nspname IN ('identity', 'access') AND a.grantee = 0
                """);
        require(connection, """
                SELECT count(*) = 0
                FROM pg_catalog.pg_default_acl d
                CROSS JOIN LATERAL aclexplode(d.defaclacl) a
                WHERE d.defaclrole = (SELECT oid FROM pg_catalog.pg_roles WHERE rolname = 'authority_pro_owner')
                    AND d.defaclnamespace IN ('identity'::regnamespace, 'access'::regnamespace)
                    AND d.defaclobjtype IN ('r','S')
                    AND (a.grantee = 0 OR a.grantee = (SELECT oid FROM pg_catalog.pg_roles
                        WHERE rolname = 'authority_pro_runtime'))
                """);
        require(connection, """
                SELECT count(*) = 2 AND bool_and(cmd = 'SELECT' AND permissive = 'PERMISSIVE'
                    AND lower(qual) LIKE '%exists%' AND lower(qual) LIKE '%tenancy.memberships%'
                    AND lower(qual) LIKE '%app.user_id%' AND lower(qual) LIKE '%disabled_at is null%')
                FROM pg_catalog.pg_policies
                WHERE schemaname = 'tenancy' AND policyname IN ('member_tenant_label_read', 'member_company_label_read')
                """);
        require(connection, """
                SELECT count(*) = 0
                FROM pg_catalog.pg_proc p JOIN pg_catalog.pg_namespace n ON n.oid = p.pronamespace
                WHERE n.nspname IN ('identity', 'access')
                """);
        require(connection, """
                SELECT count(*) = 0
                FROM pg_catalog.pg_trigger t JOIN pg_catalog.pg_class c ON c.oid = t.tgrelid
                JOIN pg_catalog.pg_namespace n ON n.oid = c.relnamespace
                WHERE n.nspname IN ('identity', 'access') AND NOT t.tgisinternal
                """);
    }

    private static void verifyAuditSchema(Connection connection) throws SQLException {
        require(connection, """
                SELECT pg_catalog.pg_get_userbyid(nspowner) = 'authority_pro_owner'
                    AND has_schema_privilege('authority_pro_runtime', 'audit', 'USAGE')
                    AND NOT has_schema_privilege('authority_pro_runtime', 'audit', 'CREATE')
                FROM pg_catalog.pg_namespace WHERE nspname = 'audit'
                """);
        require(connection, """
                SELECT count(*) = 3 AND bool_and(pg_catalog.pg_get_userbyid(c.relowner) = 'authority_pro_owner')
                    AND bool_and(CASE c.relname
                        WHEN 'tenant_records' THEN c.relrowsecurity AND c.relforcerowsecurity
                        ELSE NOT c.relrowsecurity AND NOT c.relforcerowsecurity END)
                FROM pg_catalog.pg_class c JOIN pg_catalog.pg_namespace n ON n.oid = c.relnamespace
                WHERE n.nspname = 'audit' AND c.relkind = 'r'
                """);
        require(connection, """
                SELECT count(*) = 3 AND bool_and(columns = CASE relname
                    WHEN 'tenant_records' THEN
                        'action_code,action_version,actor_id,actor_kind,company_id,correlation_id,metadata,occurred_at,operation_id,outcome,record_id,schema_version,site_id,target_id,target_type,tenant_id'
                    WHEN 'control_plane_records' THEN
                        'action_code,action_version,actor_id,actor_kind,correlation_id,metadata,occurred_at,operation_id,outcome,record_id,schema_version,target_id,target_type'
                    WHEN 'security_attempts' THEN
                        'attempt_id,correlation_id,occurred_at,reason_code,source_class' END)
                FROM (
                    SELECT c.relname, string_agg(a.attname, ',' ORDER BY a.attname) AS columns
                    FROM pg_catalog.pg_attribute a JOIN pg_catalog.pg_class c ON c.oid = a.attrelid
                    JOIN pg_catalog.pg_namespace n ON n.oid = c.relnamespace
                    WHERE n.nspname = 'audit' AND c.relkind = 'r' AND a.attnum > 0 AND NOT a.attisdropped
                    GROUP BY c.relname
                ) expected
                """);
        require(connection, """
                SELECT bool_and(has_table_privilege('authority_pro_runtime', 'audit.' || c.relname, 'SELECT')
                    AND has_table_privilege('authority_pro_runtime', 'audit.' || c.relname, 'INSERT')
                    AND NOT has_table_privilege('authority_pro_runtime', 'audit.' || c.relname, 'UPDATE')
                    AND NOT has_table_privilege('authority_pro_runtime', 'audit.' || c.relname, 'DELETE')
                    AND NOT has_table_privilege('authority_pro_runtime', 'audit.' || c.relname, 'TRUNCATE')
                    AND NOT has_table_privilege('authority_pro_runtime', 'audit.' || c.relname, 'REFERENCES')
                    AND NOT has_table_privilege('authority_pro_runtime', 'audit.' || c.relname, 'TRIGGER'))
                FROM pg_catalog.pg_class c JOIN pg_catalog.pg_namespace n ON n.oid = c.relnamespace
                WHERE n.nspname = 'audit' AND c.relkind = 'r'
                """);
        require(connection, """
                SELECT count(*) = 0
                FROM pg_catalog.pg_class c JOIN pg_catalog.pg_namespace n ON n.oid = c.relnamespace
                CROSS JOIN LATERAL aclexplode(coalesce(c.relacl, acldefault('r', c.relowner))) a
                WHERE n.nspname = 'audit' AND (a.grantee = 0 OR a.is_grantable)
                """);
        require(connection, """
                SELECT count(*) = 0
                FROM pg_catalog.pg_namespace n
                CROSS JOIN LATERAL aclexplode(coalesce(n.nspacl, acldefault('n', n.nspowner))) a
                WHERE n.nspname = 'audit' AND a.grantee = 0
                """);
        require(connection, """
                SELECT count(*) = 0
                FROM pg_catalog.pg_default_acl d
                CROSS JOIN LATERAL aclexplode(d.defaclacl) a
                WHERE d.defaclrole = (SELECT oid FROM pg_catalog.pg_roles WHERE rolname = 'authority_pro_owner')
                    AND d.defaclnamespace = 'audit'::regnamespace AND d.defaclobjtype IN ('r','S')
                    AND (a.grantee = 0 OR a.grantee = (SELECT oid FROM pg_catalog.pg_roles
                        WHERE rolname = 'authority_pro_runtime'))
                """);
        require(connection, """
                SELECT count(*) = 1 AND bool_and(tablename = 'tenant_records' AND cmd = 'ALL'
                    AND qual LIKE '%app.tenant_id%' AND qual LIKE '%app.user_id%'
                    AND qual = with_check)
                FROM pg_catalog.pg_policies WHERE schemaname = 'audit'
                """);
        require(connection, """
                SELECT count(*) = 8 AND bool_and(indisvalid AND indisready)
                FROM pg_catalog.pg_index i JOIN pg_catalog.pg_class c ON c.oid = i.indexrelid
                JOIN pg_catalog.pg_namespace n ON n.oid = c.relnamespace
                WHERE n.nspname = 'audit'
                """);
        require(connection, """
                SELECT count(*) = 0
                FROM pg_catalog.pg_proc p JOIN pg_catalog.pg_namespace n ON n.oid = p.pronamespace
                WHERE n.nspname = 'audit'
                """);
        require(connection, """
                SELECT count(*) = 0
                FROM pg_catalog.pg_trigger t JOIN pg_catalog.pg_class c ON c.oid = t.tgrelid
                JOIN pg_catalog.pg_namespace n ON n.oid = c.relnamespace
                WHERE n.nspname = 'audit' AND NOT t.tgisinternal
                """);
    }

    private static String textValue(Connection connection, String sql) throws SQLException {
        try (var statement = connection.createStatement()) {
            statement.setQueryTimeout(5);
            try (var result = statement.executeQuery(sql)) {
                if (!result.next()) throw refused();
                return result.getString(1);
            }
        }
    }

    private static boolean exists(Connection connection, String sql) throws SQLException {
        try (var statement = connection.createStatement()) {
            statement.setQueryTimeout(5);
            try (var result = statement.executeQuery(sql)) {
                return result.next() && result.getBoolean(1);
            }
        }
    }

    private static void require(Connection connection, String sql) throws SQLException {
        try (var statement = connection.createStatement()) {
            statement.setQueryTimeout(5);
            try (var result = statement.executeQuery(sql)) {
                if (!result.next() || !result.getBoolean(1)) {
                    throw refused();
                }
            }
        }
    }

    private static void silenceDependencies() {
        var logging = LoggingSystem.get(LocalDatabase.class.getClassLoader());
        logging.setLogLevel("com.zaxxer.hikari", LogLevel.OFF);
        logging.setLogLevel("org.flywaydb", LogLevel.OFF);
        logging.setLogLevel("org.postgresql", LogLevel.OFF);
        java.util.logging.Logger.getLogger("org.postgresql").setLevel(java.util.logging.Level.OFF);
    }

    private static IllegalStateException refused() {
        return new IllegalStateException("DATABASE_STARTUP_REFUSED");
    }
}
