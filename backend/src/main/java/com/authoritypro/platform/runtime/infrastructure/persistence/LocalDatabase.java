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
    private static final String MIGRATOR = "authority_pro_migrator";
    private static final Set<String> DATABASES = Set.of(DATABASE, "authority_pro_l03_app_test",
            "authority_pro_l03_app_restore");

    private LocalDatabase() {}

    public static String readPassword(InputStream input) {
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
            if (length == 0 || input.read() != -1) {
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
        HikariDataSource source = pool(database, password, false);
        try {
            verifyRuntime(source);
            return source;
        } catch (RuntimeException exception) {
            source.close();
            throw refused();
        }
    }

    public static HikariDataSource migrationDataSource(String database, String password) {
        HikariDataSource source = pool(database, password, true);
        try {
            verifyMigration(source);
            return source;
        } catch (RuntimeException exception) {
            source.close();
            throw refused();
        }
    }

    public static int migrate(String password) {
        try (HikariDataSource source = migrationDataSource(DATABASE, password)) {
            Flyway migration = flyway(source);
            migration.validate();
            int count = migration.migrate().migrationsExecuted;
            verifySchema(source);
            return count;
        } catch (RuntimeException exception) {
            throw refused();
        }
    }

    public static Flyway flyway(DataSource source) {
        silenceDependencies();
        try {
            verifyMigration(source);
            return Flyway.configure().dataSource(source).locations("classpath:db/migration")
                    .defaultSchema("public").schemas("public").table("flyway_schema_history")
                    .createSchemas(false).cleanDisabled(true).baselineOnMigrate(false).outOfOrder(false)
                    .validateMigrationNaming(true).validateOnMigrate(true).ignoreMigrationPatterns("*:pending")
                    .connectRetries(0).loggers("slf4j").load();
        } catch (RuntimeException exception) {
            throw refused();
        }
    }

    public static void verifyRuntime(DataSource source) {
        silenceDependencies();
        try (Connection connection = source.getConnection()) {
            verifyIdentity(connection, false);
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
            verifySchema(connection);
        } catch (SQLException | RuntimeException exception) {
            throw refused();
        }
    }

    private static HikariDataSource pool(String database, String password, boolean migration) {
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
            postgres.setUser(migration ? MIGRATOR : RUNTIME);
            postgres.setPassword(password);
            postgres.setConnectTimeout(3);
            postgres.setSocketTimeout(5);
            postgres.setLoginTimeout(5);
            postgres.setQueryTimeout(5);
            postgres.setCancelSignalTimeout(2);
            postgres.setLogServerErrorDetail(false);
            postgres.setOptions("-c statement_timeout=5000 -c lock_timeout=3000 -c row_security=on -c search_path=pg_catalog,public");
            postgres.setApplicationName(migration ? "authority-pro-migration" : "authority-pro-runtime");
            var config = new HikariConfig();
            config.setDataSource(postgres);
            config.setPoolName(migration ? "local-migration" : "local-runtime");
            config.setMaximumPoolSize(2);
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
            verifyIdentity(connection, true);
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
                verifySchema(connection);
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

    private static void verifyIdentity(Connection connection, boolean migration) throws SQLException {
        try (var statement = connection.createStatement()) {
            statement.setQueryTimeout(5);
            try (var result = statement.executeQuery("SELECT current_database(), current_user, session_user")) {
                if (!result.next() || !DATABASES.contains(result.getString(1))
                        || !(migration ? OWNER : RUNTIME).equals(result.getString(2))
                        || !(migration ? MIGRATOR : RUNTIME).equals(result.getString(3))) {
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
                        AND rolconnlimit = CASE WHEN session_user = 'authority_pro_runtime' THEN 5 ELSE 2 END)
                    AND EXISTS (SELECT 1 FROM pg_catalog.pg_roles WHERE rolname = 'authority_pro_owner'
                        AND NOT rolcanlogin AND NOT rolsuper AND NOT rolbypassrls AND NOT rolcreatedb
                        AND NOT rolcreaterole AND NOT rolreplication AND NOT rolinherit)
                    AND EXISTS (SELECT 1 FROM pg_catalog.pg_database WHERE datname = current_database()
                        AND pg_catalog.pg_get_userbyid(datdba) = 'authority_pro_owner')
                """);
    }

    private static void verifySchema(DataSource source) {
        try (Connection connection = source.getConnection()) {
            verifySchema(connection);
        } catch (SQLException | RuntimeException exception) {
            throw refused();
        }
    }

    private static void verifySchema(Connection connection) throws SQLException {
        require(connection, """
                SELECT count(*) = 2 AND bool_and(pg_catalog.pg_get_userbyid(nspowner) = 'authority_pro_owner')
                    AND bool_and(nspname IN ('public', 'tenancy'))
                FROM pg_catalog.pg_namespace WHERE nspname !~ '^pg_' AND nspname <> 'information_schema'
                """);
        require(connection, """
                SELECT count(*) = 5 AND bool_and(pg_catalog.pg_get_userbyid(c.relowner) = 'authority_pro_owner')
                    AND bool_and(c.relkind = 'r' AND (
                        (n.nspname = 'public' AND c.relname = 'flyway_schema_history') OR
                        (n.nspname = 'tenancy' AND c.relname IN ('tenants','companies','sites','memberships')
                            AND c.relrowsecurity AND c.relforcerowsecurity)))
                FROM pg_catalog.pg_class c JOIN pg_catalog.pg_namespace n ON n.oid = c.relnamespace
                WHERE n.nspname IN ('public','tenancy') AND c.relkind IN ('r','p','v','m','f','S')
                """);
        require(connection, """
                SELECT count(*) = 1 AND bool_and(version = '1' AND success AND type = 'SQL'
                    AND script LIKE 'V1\\_\\_%' ESCAPE '\\' AND checksum IS NOT NULL)
                FROM public.flyway_schema_history
                """);
        require(connection, """
                SELECT count(*) = 4 AND bool_and(columns = CASE relname
                    WHEN 'tenants' THEN 'name,tenant_id'
                    WHEN 'companies' THEN 'company_id,name,tenant_id'
                    WHEN 'sites' THEN 'company_id,name,site_id,tenant_id'
                    WHEN 'memberships' THEN 'active,tenant_id,user_id' END)
                FROM (
                    SELECT c.relname, string_agg(a.attname, ',' ORDER BY a.attname) AS columns
                    FROM pg_catalog.pg_attribute a JOIN pg_catalog.pg_class c ON c.oid = a.attrelid
                    JOIN pg_catalog.pg_namespace n ON n.oid = c.relnamespace
                    WHERE n.nspname = 'tenancy' AND c.relkind = 'r' AND a.attnum > 0 AND NOT a.attisdropped
                    GROUP BY c.relname
                ) expected
                """);
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
