package com.authoritypro.persistence;

import com.authoritypro.platform.access.domain.AccessDecision;
import com.authoritypro.platform.access.domain.MembershipSelection;
import com.authoritypro.platform.access.domain.Permission;
import com.authoritypro.platform.access.domain.PermissionEvaluator;
import com.authoritypro.platform.access.domain.RoleCode;
import com.authoritypro.platform.access.infrastructure.persistence.JdbcAuthorizationCatalog;
import com.authoritypro.platform.identity.application.IdentityAccessException;
import com.authoritypro.platform.identity.application.OperatorProvisioning;
import com.authoritypro.platform.identity.domain.CredentialAuthenticator;
import com.authoritypro.platform.identity.domain.LoginName;
import com.authoritypro.platform.identity.domain.ProvisioningRefusedException;
import com.authoritypro.platform.identity.infrastructure.persistence.JdbcIdentityDirectory;
import com.authoritypro.platform.identity.infrastructure.persistence.JdbcProvisioningAudit;
import com.authoritypro.platform.identity.infrastructure.persistence.JdbcProvisioningStore;
import com.authoritypro.platform.identity.infrastructure.security.SpringPasswordHasher;
import com.authoritypro.platform.identity.infrastructure.security.SpringPasswordVerifier;
import com.authoritypro.platform.identity.infrastructure.session.CappedSessionRepository;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.session.FindByIndexNameSessionRepository;
import org.springframework.session.Session;
import org.springframework.session.jdbc.JdbcIndexedSessionRepository;
import org.springframework.transaction.support.TransactionTemplate;

/** Explicit executable only: isolated L05 database, no JUnit discovery and no provisioning. */
public final class IdentityQualification {
    private static final String DATABASE = "authority_pro_l05_identity_test";
    private static final String OWNER = "authority_pro_owner";
    private static final String RUNTIME = "authority_pro_runtime";
    private static final String MIGRATION_V3 = "V3__identity_access_foundation.sql";
    private static final String LOGIN_A = "qualification.owner." + UUID.randomUUID() + "@authority-pro.test";
    private static final String LOGIN_B = "qualification.auditor." + UUID.randomUUID() + "@authority-pro.test";
    private static final String LOGIN_C = "qualification.provisioned." + UUID.randomUUID() + "@authority-pro.test";
    private static final String LOGIN_D = "qualification.rollback." + UUID.randomUUID() + "@authority-pro.test";
    private static final String PROVISIONED_PASSWORD = "provisioned-qualification-password";
    private static final UUID USER_A = UUID.randomUUID();
    private static final UUID USER_B = UUID.randomUUID();
    private static final UUID TENANT_A = UUID.randomUUID();
    private static final UUID TENANT_B = UUID.randomUUID();
    private static final UUID COMPANY_A = UUID.randomUUID();
    private static final UUID COMPANY_B = UUID.randomUUID();
    private static final UUID SITE_A = UUID.randomUUID();
    private static PrintStream report;
    private static String activeCheck = "input_contract";
    private static int checks;

    private IdentityQualification() { }

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
            String provisionerPassword = input.readLine();
            String database = input.readLine();
            require(migratorPassword != null && !migratorPassword.isEmpty());
            require(runtimePassword != null && !runtimePassword.isEmpty());
            require(provisionerPassword != null && !provisionerPassword.isEmpty());
            require(DATABASE.equals(database));
            require(input.readLine() == null);
            passed("input_contract");
            run(migratorPassword, runtimePassword, provisionerPassword);
            report.println("IDENTITY_QUALIFICATION_OK checks=" + checks + " database=" + DATABASE);
        } catch (Throwable failure) {
            SQLException sql = sqlException(failure);
            String state = sql == null ? "none" : sql.getSQLState();
            if (state == null || !state.matches("[A-Z0-9]{5}")) state = "none";
            String type = (sql == null ? failure : sql).getClass().getSimpleName();
            if (!type.matches("[A-Za-z0-9_$]+")) type = "Failure";
            report.println("IDENTITY_QUALIFICATION_FAILED check=" + activeCheck + " type=" + type
                    + " sqlState=" + state);
            System.exit(1);
        }
    }

    private static void run(String migratorPassword, String runtimePassword, String provisionerPassword)
            throws Exception {
        activeCheck = "migration";
        int executed = LocalDatabase.migrateIdentityTest(migratorPassword);
        require(executed >= 0 && executed <= 4);
        try (HikariDataSource migrator = LocalDatabase.migrationDataSource(DATABASE, migratorPassword)) {
            check("effective_owner_and_history", () -> raw(migrator, false, false, c -> {
                require(DATABASE.equals(text(c, "SELECT current_database()")));
                require(OWNER.equals(text(c, "SELECT current_user")));
                require("authority_pro_migrator".equals(text(c, "SELECT session_user")));
                eq(4, number(c, "SELECT count(*) FROM public.flyway_schema_history"));
                eq(1, number(c, "SELECT count(*) FROM public.flyway_schema_history WHERE version = '3' "
                        + "AND success AND script = ?", MIGRATION_V3));
                eq(1, number(c, "SELECT count(*) FROM public.flyway_schema_history WHERE version = '4' "
                        + "AND success"));
                eq(0, number(c, "SELECT count(*) FROM pg_namespace WHERE nspname NOT LIKE 'pg_%' "
                        + "AND nspname <> 'information_schema' "
                        + "AND nspname NOT IN ('public','tenancy','audit','identity','access')"));
                return null;
            }));
            resetFixtures(migrator);
            seed(migrator);
            credentialChecks(migrator);
            constraintChecks(migrator);
            activeCheck = "runtime_pool";
            try (HikariDataSource runtime = LocalDatabase.runtimeDataSource(DATABASE, runtimePassword)) {
                runtime.setMaximumPoolSize(1);
                runtime.setMinimumIdle(0);
                check("production_runtime_validation", () -> LocalDatabase.verifyRuntime(runtime));
                schemaChecks(runtime);
                repositoryChecks(runtime, migrator);
                sessionChecks(runtime);
                capCheck(runtime);
                labelChecks(runtime, migrator);
                isolationChecks(runtime);
                rlsConcurrencyChecks(runtime, migrator);
                provisioningChecks(runtime, migrator, provisionerPassword);
                denialChecks(runtime);
                check("final_runtime_validation", () -> LocalDatabase.verifyRuntime(runtime));
            }
        }
    }

    /** The isolated database is retained across runs; only this qualifier's own global rows are reset. */
    private static void resetFixtures(HikariDataSource migrator) throws Exception {
        check("fixture_reset", () -> raw(migrator, false, false, c -> {
            execute(c, "DELETE FROM audit.control_plane_records");
            execute(c, "DELETE FROM audit.security_attempts");
            execute(c, "DELETE FROM identity.provisioning_guard");
            execute(c, "DELETE FROM identity.password_credentials");
            execute(c, "DELETE FROM identity.users");
            return null;
        }));
    }

    private static void seed(HikariDataSource migrator) throws Exception {
        check("seed_fixtures", () -> raw(migrator, false, false, c -> {
            context(c, USER_A, TENANT_A);
            update(c, "INSERT INTO tenancy.tenants (tenant_id, name) VALUES (?, 'l05-tenant-a')", TENANT_A);
            update(c, "INSERT INTO tenancy.companies (tenant_id, company_id, name) VALUES (?, ?, 'l05-company-a')",
                    TENANT_A, COMPANY_A);
            update(c, "INSERT INTO tenancy.sites (tenant_id, company_id, site_id, name) VALUES (?, ?, ?, 'l05-site-a')",
                    TENANT_A, COMPANY_A, SITE_A);
            update(c, "INSERT INTO tenancy.memberships (user_id, tenant_id) VALUES (?, ?)", USER_A, TENANT_A);
            update(c, "INSERT INTO identity.users (user_id, login_normalized, display_name) VALUES (?, ?, 'Owner A')",
                    USER_A, LOGIN_A);
            update(c, "INSERT INTO access.membership_roles (user_id, tenant_id, role_code, company_id) "
                    + "VALUES (?, ?, 'TENANT_OWNER', ?)", USER_A, TENANT_A, COMPANY_A);
            context(c, USER_B, TENANT_B);
            update(c, "INSERT INTO tenancy.tenants (tenant_id, name) VALUES (?, 'l05-tenant-b')", TENANT_B);
            update(c, "INSERT INTO tenancy.companies (tenant_id, company_id, name) VALUES (?, ?, 'l05-company-b')",
                    TENANT_B, COMPANY_B);
            update(c, "INSERT INTO tenancy.memberships (user_id, tenant_id) VALUES (?, ?)", USER_B, TENANT_B);
            update(c, "INSERT INTO identity.users (user_id, login_normalized, display_name) VALUES (?, ?, 'Auditor B')",
                    USER_B, LOGIN_B);
            update(c, "INSERT INTO access.membership_roles (user_id, tenant_id, role_code) "
                    + "VALUES (?, ?, 'TENANT_AUDITOR')", USER_B, TENANT_B);
            return null;
        }));
    }

    private static void credentialChecks(HikariDataSource migrator) throws Exception {
        String hash = new BCryptPasswordEncoder(12).encode("qualification-password");
        require(hash.matches("^\\$2[aby]\\$1[2-4]\\$[./A-Za-z0-9]{53}$"));
        check("credential_hash_contract", () -> raw(migrator, false, false, c -> {
            update(c, "INSERT INTO identity.password_credentials (user_id, password_hash, scheme) "
                    + "VALUES (?, ?, 'bcrypt')", USER_A, hash);
            eq(1, number(c, "SELECT count(*) FROM identity.password_credentials WHERE user_id = ?", USER_A));
            return null;
        }));
        check("malformed_credential_rejected", () -> expectSql("23514", () -> raw(migrator, false, true,
                c -> update(c, "INSERT INTO identity.password_credentials (user_id, password_hash, scheme) "
                        + "VALUES (?, 'plain-text', 'bcrypt')", USER_B))));
        check("low_cost_credential_rejected", () -> expectSql("23514", () -> raw(migrator, false, true,
                c -> update(c, "INSERT INTO identity.password_credentials (user_id, password_hash, scheme) "
                        + "VALUES (?, ?, 'bcrypt')", USER_B,
                        new BCryptPasswordEncoder(10).encode("qualification-password")))));
        check("provisioning_guard_empty", () -> raw(migrator, true, false, c -> {
            eq(0, number(c, "SELECT count(*) FROM identity.provisioning_guard"));
            return null;
        }));
        check("provisioning_guard_single_use", () -> raw(migrator, false, true, c -> {
            update(c, "INSERT INTO identity.provisioning_guard (operator_user_id, revision) VALUES (?, 1)", USER_A);
            expectSql("23505", () -> update(c,
                    "INSERT INTO identity.provisioning_guard (operator_user_id, revision) VALUES (?, 1)", USER_B));
            return null;
        }));
    }

    private static void constraintChecks(HikariDataSource migrator) throws Exception {
        check("membership_role_site_requires_company", () -> expectSql("23514", () -> raw(migrator, false, true,
                c -> update(c, "INSERT INTO access.membership_roles (user_id, tenant_id, role_code, site_id) "
                        + "VALUES (?, ?, 'TENANT_AUDITOR', ?)", USER_A, TENANT_A, SITE_A))));
        check("membership_role_unknown_role_rejected", () -> expectSql("23503", () -> raw(migrator, false, true,
                c -> update(c, "INSERT INTO access.membership_roles (user_id, tenant_id, role_code) "
                        + "VALUES (?, ?, 'TENANT_VIEWER')", USER_A, TENANT_A))));
        check("membership_role_requires_membership", () -> expectSql("23503", () -> raw(migrator, false, true,
                c -> update(c, "INSERT INTO access.membership_roles (user_id, tenant_id, role_code) "
                        + "VALUES (?, ?, 'TENANT_AUDITOR')", USER_A, TENANT_B))));
    }

    private static void schemaChecks(HikariDataSource runtime) throws Exception {
        check("identity_schema_privileges", () -> raw(runtime, true, false, c -> {
            eq(5, number(c, "SELECT count(*) FROM pg_namespace WHERE nspname IN "
                    + "('public','tenancy','audit','identity','access')"));
            eq(3, number(c, "SELECT count(*) FROM access.roles"));
            eq(10, number(c, "SELECT count(*) FROM access.role_permissions"));
            require("true".equals(text(c, "SELECT has_table_privilege(current_user, 'identity.users', 'SELECT')::text")));
            require("false".equals(text(c,
                    "SELECT has_table_privilege(current_user, 'identity.provisioning_guard', 'SELECT')::text")));
            for (String table : new String[]{"identity.users", "identity.password_credentials", "access.roles",
                    "access.role_permissions", "access.membership_roles"}) {
                for (String privilege : new String[]{"INSERT", "UPDATE", "DELETE", "TRUNCATE", "REFERENCES", "TRIGGER"}) {
                    require("false".equals(text(c, "SELECT has_table_privilege(current_user, ?, ?)::text",
                            table, privilege)));
                }
            }
            return null;
        }));
    }

    private static void repositoryChecks(HikariDataSource runtime, HikariDataSource migrator) throws Exception {
        check("directory_credential_lookup", () -> raw(runtime, true, false, c -> {
            JdbcIdentityDirectory directory = new JdbcIdentityDirectory();
            var credential = directory.findCredential(c, LOGIN_A);
            require(credential.isPresent());
            require(USER_A.equals(credential.get().userId()));
            require("Owner A".equals(credential.get().displayName()));
            require(credential.get().active());
            eq(1, credential.get().authorizationRevision());
            require("bcrypt".equals(credential.get().scheme()));
            require(credential.get().passwordHash().startsWith("$2"));
            require(directory.findCredential(c, LOGIN_B).isEmpty());
            require(directory.findCredential(c, "absent." + UUID.randomUUID()).isEmpty());
            return null;
        }));
        check("credential_verification", () -> raw(runtime, true, false, c -> {
            var credential = new JdbcIdentityDirectory().findCredential(c, LOGIN_A).orElseThrow();
            CredentialAuthenticator authenticator = new CredentialAuthenticator(new SpringPasswordVerifier());
            require(authenticator.authenticate(credential.active(), credential.scheme(), credential.passwordHash(),
                    "qualification-password".toCharArray())
                    == CredentialAuthenticator.Outcome.AUTHENTICATED);
            require(authenticator.authenticate(credential.active(), credential.scheme(), credential.passwordHash(),
                    "wrong-password".toCharArray())
                    == CredentialAuthenticator.Outcome.INVALID_CREDENTIALS);
            return null;
        }));
        check("authorization_catalogue", () -> raw(runtime, true, false, c -> {
            var catalogue = new JdbcAuthorizationCatalog().catalogue(c);
            eq(3, catalogue.size());
            eq(5, catalogue.get(RoleCode.TENANT_OWNER).size());
            eq(2, catalogue.get(RoleCode.TENANT_ADMIN).size());
            eq(3, catalogue.get(RoleCode.TENANT_AUDITOR).size());
            PermissionEvaluator evaluator = new PermissionEvaluator(catalogue);
            require(evaluator.allows(Set.of(RoleCode.TENANT_OWNER), Permission.AUDIT_READ_TENANT));
            require(!evaluator.allows(Set.of(RoleCode.TENANT_ADMIN), Permission.AUDIT_READ_TENANT));
            return null;
        }));
        check("authorization_decision", () -> raw(runtime, true, false, c -> {
            var catalogue = new JdbcAuthorizationCatalog().catalogue(c);
            AccessDecision decision = new AccessDecision(new PermissionEvaluator(catalogue));
            context(c, USER_A, null);
            var assignmentsA = new JdbcAuthorizationCatalog().assignments(c, USER_A);
            require(decision.allows(assignmentsA, new MembershipSelection(TENANT_A, COMPANY_A, null, 1),
                    Permission.AUDIT_READ_TENANT));
            require(!decision.allows(assignmentsA, new MembershipSelection(TENANT_A, null, null, 1),
                    Permission.AUDIT_READ_TENANT));
            require(!decision.allows(assignmentsA, new MembershipSelection(TENANT_A, COMPANY_B, null, 1),
                    Permission.AUDIT_READ_TENANT));
            require(!decision.allows(assignmentsA, new MembershipSelection(TENANT_A, COMPANY_A, null, 2),
                    Permission.AUDIT_READ_TENANT));
            context(c, USER_B, null);
            var assignmentsB = new JdbcAuthorizationCatalog().assignments(c, USER_B);
            require(decision.allows(assignmentsB, new MembershipSelection(TENANT_B, null, null, 1),
                    Permission.AUDIT_READ_TENANT));
            require(!decision.allows(assignmentsB, new MembershipSelection(TENANT_B, null, null, 1),
                    Permission.TENANCY_SETTINGS_MANAGE));
            require(!decision.allows(assignmentsB, new MembershipSelection(TENANT_A, null, null, 1),
                    Permission.AUDIT_READ_TENANT));
            return null;
        }));
        check("assignments_require_membership", () -> {
            raw(runtime, true, false, c -> {
                context(c, USER_A, null);
                var assignments = new JdbcAuthorizationCatalog().assignments(c, USER_A);
                eq(1, assignments.size());
                require(TENANT_A.equals(assignments.getFirst().tenantId()));
                require(COMPANY_A.equals(assignments.getFirst().companyId()));
                require(assignments.getFirst().siteId() == null);
                require(assignments.getFirst().role() == RoleCode.TENANT_OWNER);
                eq(1, assignments.getFirst().authorizationRevision());
                return null;
            });
            raw(migrator, false, false, c -> {
                context(c, USER_B, TENANT_B);
                update(c, "UPDATE tenancy.memberships SET disabled_at = clock_timestamp() "
                        + "WHERE user_id = ? AND tenant_id = ?", USER_B, TENANT_B);
                return null;
            });
            try {
                raw(runtime, true, false, c -> {
                    context(c, USER_B, null);
                    eq(0, new JdbcAuthorizationCatalog().assignments(c, USER_B).size());
                    return null;
                });
            } finally {
                raw(migrator, false, false, c -> {
                    context(c, USER_B, TENANT_B);
                    update(c, "UPDATE tenancy.memberships SET disabled_at = NULL "
                            + "WHERE user_id = ? AND tenant_id = ?", USER_B, TENANT_B);
                    return null;
                });
            }
        });
    }

    private static void sessionChecks(HikariDataSource runtime) throws Exception {
        check("session_repository", () -> {
            var repository = new JdbcIndexedSessionRepository(new JdbcTemplate(runtime),
                    new TransactionTemplate(new DataSourceTransactionManager(runtime)));
            repository.setTableName("identity.spring_session");
            repository.setDefaultMaxInactiveInterval(java.time.Duration.ofMinutes(30));
            repository.afterPropertiesSet();
            @SuppressWarnings("unchecked")
            FindByIndexNameSessionRepository<org.springframework.session.Session> sessions =
                    (FindByIndexNameSessionRepository<org.springframework.session.Session>)
                            (FindByIndexNameSessionRepository<?>) repository;
            try {
                org.springframework.session.Session session = sessions.createSession();
                session.setAttribute("authority.userId", USER_A.toString());
                session.setAttribute(FindByIndexNameSessionRepository.PRINCIPAL_NAME_INDEX_NAME, LOGIN_A);
                sessions.save(session);
                org.springframework.session.Session loaded = sessions.findById(session.getId());
                require(loaded != null);
                require(USER_A.toString().equals(loaded.getAttribute("authority.userId")));
                require(LOGIN_A.equals(loaded.getAttribute(FindByIndexNameSessionRepository.PRINCIPAL_NAME_INDEX_NAME)));
                require(sessions.findByPrincipalName(LOGIN_A).containsKey(session.getId()));
                sessions.deleteById(session.getId());
                require(sessions.findById(session.getId()) == null);
            } finally {
                repository.destroy();
            }
        });
        check("session_privileges", () -> raw(runtime, true, false, c -> {
            require("true".equals(text(c,
                    "SELECT has_table_privilege(current_user, 'identity.spring_session', 'INSERT')::text")));
            require("true".equals(text(c,
                    "SELECT has_table_privilege(current_user, 'identity.spring_session_attributes', 'DELETE')::text")));
            require("false".equals(text(c,
                    "SELECT has_table_privilege(current_user, 'identity.spring_session', 'TRUNCATE')::text")));
            require("false".equals(text(c,
                    "SELECT has_table_privilege(current_user, 'identity.provisioning_guard', 'INSERT')::text")));
            return null;
        }));
    }

    private static void capCheck(HikariDataSource runtime) throws Exception {
        check("session_cap_concurrent", () -> {
            var repository = new JdbcIndexedSessionRepository(new JdbcTemplate(runtime),
                    new TransactionTemplate(new DataSourceTransactionManager(runtime)));
            repository.setTableName("identity.spring_session");
            repository.setDefaultMaxInactiveInterval(java.time.Duration.ofMinutes(30));
            repository.afterPropertiesSet();
            runtime.setMaximumPoolSize(4);
            try {
                var capped = new CappedSessionRepository(runtime, repository,
                        new TransactionTemplate(new DataSourceTransactionManager(runtime)));
                String principal = "cap-" + UUID.randomUUID();
                int logins = 4;
                List<Session> created = Collections.synchronizedList(new ArrayList<>());
                var failures = new ConcurrentLinkedQueue<Throwable>();
                var ready = new CyclicBarrier(logins);
                var executor = Executors.newFixedThreadPool(logins);
                try {
                    for (int index = 0; index < logins; index++) {
                        int delay = index;
                        executor.execute(() -> {
                            try {
                                ready.await(10, TimeUnit.SECONDS);
                                Thread.sleep(delay * 25L);
                                Session session = capped.createSession();
                                session.setAttribute(
                                        FindByIndexNameSessionRepository.PRINCIPAL_NAME_INDEX_NAME, principal);
                                session.setAttribute("authority.userId", USER_A.toString());
                                capped.save(session);
                                created.add(session);
                            } catch (Throwable failure) {
                                failures.add(failure);
                            }
                        });
                    }
                } finally {
                    executor.shutdown();
                    require(executor.awaitTermination(30, TimeUnit.SECONDS));
                }
                require(failures.isEmpty());
                eq(logins, created.size());
                Map<String, Session> live = capped.findByPrincipalName(principal);
                eq(CappedSessionRepository.MAX_CONCURRENT, live.size());
                Comparator<Session> age = Comparator.comparing(Session::getLastAccessedTime)
                        .thenComparing(Session::getCreationTime)
                        .thenComparing(Session::getId);
                Set<String> newest = created.stream().sorted(age.reversed())
                        .limit(CappedSessionRepository.MAX_CONCURRENT)
                        .map(Session::getId).collect(Collectors.toSet());
                require(newest.equals(live.keySet()));
                for (String id : new ArrayList<>(live.keySet())) capped.deleteById(id);
                eq(0, capped.findByPrincipalName(principal).size());
            } finally {
                repository.destroy();
                runtime.setMaximumPoolSize(1);
            }
        });
        check("session_cap_lock_timeout", () -> {
            var repository = new JdbcIndexedSessionRepository(new JdbcTemplate(runtime),
                    new TransactionTemplate(new DataSourceTransactionManager(runtime)));
            repository.setTableName("identity.spring_session");
            repository.setDefaultMaxInactiveInterval(java.time.Duration.ofMinutes(30));
            repository.afterPropertiesSet();
            runtime.setMaximumPoolSize(4);
            try {
                var capped = new CappedSessionRepository(runtime, repository,
                        new TransactionTemplate(new DataSourceTransactionManager(runtime)));
                String principal = "cap-timeout-" + UUID.randomUUID();
                try (Connection holder = runtime.getConnection()) {
                    holder.setAutoCommit(false);
                    try (PreparedStatement lock = holder.prepareStatement(
                            "SELECT pg_advisory_xact_lock(hashtextextended(?, 0))")) {
                        lock.setString(1, principal);
                        lock.execute();
                    }
                    Session blocked = capped.createSession();
                    blocked.setAttribute(FindByIndexNameSessionRepository.PRINCIPAL_NAME_INDEX_NAME, principal);
                    blocked.setAttribute("authority.userId", USER_A.toString());
                    long started = System.nanoTime();
                    try {
                        capped.save(blocked);
                        throw new AssertionError("SESSION_CAP_LOCK_NOT_ENFORCED");
                    } catch (IdentityAccessException refusal) {
                        require(refusal.kind() == IdentityAccessException.Kind.STORE_UNAVAILABLE);
                    }
                    long elapsed = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started);
                    require(elapsed >= 1_500 && elapsed < 20_000);
                    eq(0, capped.findByPrincipalName(principal).size());
                    holder.rollback();
                }
                Session recovered = capped.createSession();
                recovered.setAttribute(FindByIndexNameSessionRepository.PRINCIPAL_NAME_INDEX_NAME, principal);
                recovered.setAttribute("authority.userId", USER_A.toString());
                capped.save(recovered);
                Map<String, Session> live = capped.findByPrincipalName(principal);
                eq(1, live.size());
                require(live.containsKey(recovered.getId()));
                for (String id : new ArrayList<>(live.keySet())) capped.deleteById(id);
            } finally {
                repository.destroy();
                runtime.setMaximumPoolSize(1);
            }
        });
        check("session_cap_rotation", () -> {
            var repository = new JdbcIndexedSessionRepository(new JdbcTemplate(runtime),
                    new TransactionTemplate(new DataSourceTransactionManager(runtime)));
            repository.setTableName("identity.spring_session");
            repository.setDefaultMaxInactiveInterval(java.time.Duration.ofMinutes(30));
            repository.afterPropertiesSet();
            runtime.setMaximumPoolSize(4);
            try {
                var capped = new CappedSessionRepository(runtime, repository,
                        new TransactionTemplate(new DataSourceTransactionManager(runtime)));
                String principal = "cap-rotation-" + UUID.randomUUID();
                List<Session> initial = new ArrayList<>();
                for (int index = 0; index < CappedSessionRepository.MAX_CONCURRENT; index++) {
                    Session session = capped.createSession();
                    session.setAttribute(FindByIndexNameSessionRepository.PRINCIPAL_NAME_INDEX_NAME, principal);
                    session.setAttribute("authority.userId", USER_A.toString());
                    capped.save(session);
                    initial.add(session);
                    Thread.sleep(20);
                }
                // Rotation saves a new identifier while the previous row still exists.
                Session rotated = capped.createSession();
                rotated.setAttribute(FindByIndexNameSessionRepository.PRINCIPAL_NAME_INDEX_NAME, principal);
                rotated.setAttribute("authority.userId", USER_A.toString());
                capped.save(rotated);
                Map<String, Session> live = capped.findByPrincipalName(principal);
                eq(CappedSessionRepository.MAX_CONCURRENT, live.size());
                require(live.containsKey(rotated.getId()));
                require(!live.containsKey(initial.get(0).getId()));
                require(live.containsKey(initial.get(1).getId()));
                require(live.containsKey(initial.get(2).getId()));
                for (String id : new ArrayList<>(live.keySet())) capped.deleteById(id);
                eq(0, capped.findByPrincipalName(principal).size());
            } finally {
                repository.destroy();
                runtime.setMaximumPoolSize(1);
            }
        });
    }

    private static void provisioningChecks(HikariDataSource runtime, HikariDataSource migrator,
            String provisionerPassword) throws Exception {
        check("provisioner_privileges", () -> raw(migrator, true, false, c -> {
            require("true".equals(text(c,
                    "SELECT has_table_privilege('authority_pro_provisioner', 'identity.users', 'INSERT')::text")));
            require("false".equals(text(c,
                    "SELECT has_table_privilege('authority_pro_provisioner', 'identity.users', 'SELECT')::text")));
            require("true".equals(text(c,
                    "SELECT has_table_privilege('authority_pro_provisioner', 'identity.provisioning_guard', 'SELECT')::text")));
            require("false".equals(text(c,
                    "SELECT has_table_privilege('authority_pro_provisioner', 'tenancy.memberships', 'SELECT')::text")));
            require("false".equals(text(c,
                    "SELECT has_table_privilege('authority_pro_provisioner', 'identity.spring_session', 'SELECT')::text")));
            require("true".equals(text(c, "SELECT (rolconnlimit = 1)::text FROM pg_roles "
                    + "WHERE rolname = 'authority_pro_provisioner' AND rolcanlogin "
                    + "AND NOT rolsuper AND NOT rolcreatedb AND NOT rolcreaterole AND NOT rolbypassrls "
                    + "AND NOT rolinherit")));
            eq(0, number(c, "SELECT count(*) FROM pg_auth_members m JOIN pg_roles r ON r.oid = m.member "
                    + "WHERE r.rolname = 'authority_pro_provisioner'"));
            return null;
        }));
        check("provisioning_audit_rollback", () -> {
            try (HikariDataSource provisioner = LocalDatabase.provisionerDataSource(DATABASE, provisionerPassword)) {
                var service = new OperatorProvisioning(new JdbcProvisioningStore(), new SpringPasswordHasher(),
                        (connection, userId, operationId) -> {
                            throw new SQLException("injected audit failure");
                        });
                try (Connection connection = provisioner.getConnection()) {
                    connection.setAutoCommit(false);
                    try {
                        service.provision(connection, LoginName.of(LOGIN_D), LOGIN_D,
                                PROVISIONED_PASSWORD.toCharArray(), UUID.randomUUID());
                        throw new IllegalStateException("PROVISIONING_NOT_REFUSED");
                    } catch (ProvisioningRefusedException expected) {
                        connection.rollback();
                    }
                }
            }
            raw(migrator, true, false, c -> {
                eq(0, number(c, "SELECT count(*) FROM identity.provisioning_guard"));
                eq(0, number(c, "SELECT count(*) FROM identity.users WHERE login_normalized = ?", LOGIN_D));
                eq(0, number(c, "SELECT count(*) FROM audit.control_plane_records "
                        + "WHERE action_code = 'identity.operator.provisioned'"));
                return null;
            });
        });
        check("operator_provisioning", () -> {
            UUID[] created = new UUID[1];
            try (HikariDataSource provisioner = LocalDatabase.provisionerDataSource(DATABASE, provisionerPassword)) {
                var service = new OperatorProvisioning(new JdbcProvisioningStore(), new SpringPasswordHasher(),
                        new JdbcProvisioningAudit());
                try (Connection connection = provisioner.getConnection()) {
                    connection.setAutoCommit(false);
                    try {
                        created[0] = service.provision(connection, LoginName.of(LOGIN_C), LoginName.of(LOGIN_C).value(),
                                PROVISIONED_PASSWORD.toCharArray(), UUID.randomUUID());
                        connection.commit();
                    } catch (RuntimeException failure) {
                        connection.rollback();
                        throw failure;
                    }
                }
            }
            raw(migrator, true, false, c -> {
                eq(1, number(c, "SELECT count(*) FROM identity.provisioning_guard WHERE operator_user_id = ?",
                        created[0]));
                eq(1, number(c, "SELECT count(*) FROM identity.users WHERE user_id = ? AND login_normalized = ?",
                        created[0], LOGIN_C));
                eq(1, number(c, "SELECT count(*) FROM identity.password_credentials "
                        + "WHERE user_id = ? AND scheme = 'bcrypt'", created[0]));
                eq(1, number(c, "SELECT count(*) FROM audit.control_plane_records "
                        + "WHERE action_code = 'identity.operator.provisioned' AND action_version = 1 "
                        + "AND actor_id = ? AND actor_kind = 'operator' AND target_type = 'operator' "
                        + "AND target_id = ? AND metadata ->> 'operatorRef' = ?",
                        created[0], created[0].toString(), created[0].toString()));
                return null;
            });
            raw(runtime, true, false, c -> {
                var credential = new JdbcIdentityDirectory().findCredential(c, LOGIN_C).orElseThrow();
                require(credential.userId().equals(created[0]));
                require(new SpringPasswordVerifier().matches(PROVISIONED_PASSWORD.toCharArray(),
                        credential.passwordHash()));
                return null;
            });
        });
        check("provisioning_replay_refused", () -> {
            try (HikariDataSource provisioner = LocalDatabase.provisionerDataSource(DATABASE, provisionerPassword)) {
                var service = new OperatorProvisioning(new JdbcProvisioningStore(), new SpringPasswordHasher(),
                        new JdbcProvisioningAudit());
                try (Connection connection = provisioner.getConnection()) {
                    connection.setAutoCommit(false);
                    try {
                        service.provision(connection, LoginName.of(LOGIN_D), LOGIN_D,
                                PROVISIONED_PASSWORD.toCharArray(), UUID.randomUUID());
                        throw new IllegalStateException("PROVISIONING_NOT_REFUSED");
                    } catch (ProvisioningRefusedException expected) {
                        connection.rollback();
                    }
                }
            }
            raw(migrator, true, false, c -> {
                eq(1, number(c, "SELECT count(*) FROM identity.provisioning_guard"));
                eq(0, number(c, "SELECT count(*) FROM identity.users WHERE login_normalized = ?", LOGIN_D));
                eq(1, number(c, "SELECT count(*) FROM audit.control_plane_records "
                        + "WHERE action_code = 'identity.operator.provisioned'"));
                return null;
            });
        });
    }

    private static void labelChecks(HikariDataSource runtime, HikariDataSource migrator) throws Exception {
        check("label_read_by_membership", () -> raw(runtime, false, true, c -> {
            context(c, USER_A, null);
            eq(1, number(c, "SELECT count(*) FROM tenancy.tenants"));
            require("l05-tenant-a".equals(text(c, "SELECT name FROM tenancy.tenants")));
            eq(1, number(c, "SELECT count(*) FROM tenancy.memberships"));
            context(c, USER_B, null);
            eq(1, number(c, "SELECT count(*) FROM tenancy.tenants"));
            require("l05-tenant-b".equals(text(c, "SELECT name FROM tenancy.tenants")));
            eq(1, number(c, "SELECT count(*) FROM tenancy.companies"));
            require("l05-company-b".equals(text(c, "SELECT name FROM tenancy.companies")));
            return null;
        }));
        check("missing_user_reads_nothing", () -> raw(runtime, false, true, c -> {
            blank(c);
            eq(0, number(c, "SELECT count(*) FROM tenancy.tenants"));
            eq(0, number(c, "SELECT count(*) FROM tenancy.memberships"));
            eq(0, number(c, "SELECT count(*) FROM tenancy.companies"));
            return null;
        }));
        check("disabled_membership_hides_labels", () -> {
            raw(migrator, false, false, c -> {
                context(c, USER_B, TENANT_B);
                update(c, "UPDATE tenancy.memberships SET disabled_at = clock_timestamp() "
                        + "WHERE user_id = ? AND tenant_id = ?", USER_B, TENANT_B);
                return null;
            });
            try {
                raw(runtime, false, true, c -> {
                    context(c, USER_B, null);
                    eq(0, number(c, "SELECT count(*) FROM tenancy.tenants"));
                    eq(0, number(c, "SELECT count(*) FROM tenancy.companies"));
                    return null;
                });
            } finally {
                raw(migrator, false, false, c -> {
                    context(c, USER_B, TENANT_B);
                    update(c, "UPDATE tenancy.memberships SET disabled_at = NULL "
                            + "WHERE user_id = ? AND tenant_id = ?", USER_B, TENANT_B);
                    return null;
                });
            }
        });
        check("membership_roles_scoped_by_query", () -> raw(runtime, true, false, c -> {
            eq(2, number(c, "SELECT count(*) FROM access.membership_roles WHERE user_id IN (?, ?)", USER_A, USER_B));
            eq(1, number(c, "SELECT count(*) FROM access.membership_roles WHERE user_id = ?", USER_A));
            require("TENANT_OWNER".equals(text(c,
                    "SELECT role_code FROM access.membership_roles WHERE user_id = ?", USER_A)));
            return null;
        }));
    }

    private static void isolationChecks(HikariDataSource runtime) throws Exception {
        check("pool_reuse_has_no_tenant_context", () -> {
            raw(runtime, false, false, c -> {
                context(c, USER_A, null);
                eq(1, number(c, "SELECT count(*) FROM tenancy.tenants"));
                return null;
            });
            raw(runtime, true, false, c -> {
                blank(c);
                eq(0, number(c, "SELECT count(*) FROM tenancy.tenants"));
                eq(0, number(c, "SELECT count(*) FROM tenancy.companies"));
                return null;
            });
        });
        check("rollback_clears_tenant_context", () -> {
            try {
                raw(runtime, false, false, c -> {
                    context(c, USER_B, null);
                    eq(1, number(c, "SELECT count(*) FROM tenancy.tenants"));
                    throw new IllegalStateException("qualification rollback");
                });
                require(false);
            } catch (IllegalStateException expected) {
                require("qualification rollback".equals(expected.getMessage()));
            }
            raw(runtime, true, false, c -> {
                blank(c);
                eq(0, number(c, "SELECT count(*) FROM tenancy.tenants"));
                return null;
            });
        });
        check("cross_tenant_write_denied", () -> {
            raw(runtime, false, false, c -> {
                context(c, USER_A, null);
                eq(0, update(c, "UPDATE tenancy.tenants SET name = name WHERE tenant_id = ?", TENANT_B));
                eq(0, update(c, "UPDATE tenancy.companies SET name = name WHERE tenant_id = ?", TENANT_B));
                return null;
            });
            expectSql("42501", () -> raw(runtime, false, true, c -> {
                context(c, USER_A, null);
                return update(c, "INSERT INTO tenancy.companies (tenant_id, company_id, name) "
                        + "VALUES (?, ?, 'cross-tenant')", TENANT_B, UUID.randomUUID());
            }));
        });
    }

    private static void rlsConcurrencyChecks(HikariDataSource runtime, HikariDataSource migrator) throws Exception {
        check("rls_parallel_read_isolation", () -> {
            runtime.setMaximumPoolSize(4);
            try {
                UUID[] users = {USER_A, USER_B, USER_A, USER_B};
                UUID[] tenants = {TENANT_A, TENANT_B, TENANT_A, TENANT_B};
                UUID[] companies = {COMPANY_A, COMPANY_B, COMPANY_A, COMPANY_B};
                String[] names = {"l05-company-a", "l05-company-b", "l05-company-a", "l05-company-b"};
                var failures = new ConcurrentLinkedQueue<Throwable>();
                var ready = new CyclicBarrier(users.length);
                var interleave = new CyclicBarrier(users.length);
                var executor = Executors.newFixedThreadPool(users.length);
                try {
                    for (int worker = 0; worker < users.length; worker++) {
                        int index = worker;
                        executor.execute(() -> {
                            try (Connection c = runtime.getConnection()) {
                                c.setAutoCommit(false);
                                try {
                                    context(c, users[index], null);
                                    ready.await(10, TimeUnit.SECONDS);
                                    for (int round = 0; round < 25; round++) {
                                        interleave.await(10, TimeUnit.SECONDS);
                                        eq(1, number(c, "SELECT count(*) FROM tenancy.tenants"));
                                        eq(1, number(c, "SELECT count(*) FROM tenancy.companies"));
                                        require(names[index].equals(text(c, "SELECT name FROM tenancy.companies")));
                                        eq(0, number(c, "SELECT count(*) FROM tenancy.companies WHERE tenant_id = ?",
                                                tenants[1 - index % 2]));
                                        eq(1, number(c, "SELECT count(*) FROM tenancy.companies "
                                                + "WHERE company_id = ?", companies[index]));
                                        eq(0, number(c, "SELECT count(*) FROM tenancy.companies "
                                                + "WHERE company_id = ?", companies[1 - index % 2]));
                                    }
                                } finally {
                                    c.rollback();
                                }
                            } catch (Throwable failure) {
                                failures.add(failure);
                            }
                        });
                    }
                } finally {
                    executor.shutdown();
                    require(executor.awaitTermination(30, TimeUnit.SECONDS));
                }
                require(failures.isEmpty());
            } finally {
                runtime.setMaximumPoolSize(1);
            }
        });
        check("rls_parallel_write_isolation", () -> {
            runtime.setMaximumPoolSize(2);
            try {
                UUID[] users = {USER_A, USER_B};
                UUID[] foreignTenants = {TENANT_B, TENANT_A};
                var failures = new ConcurrentLinkedQueue<Throwable>();
                var ready = new CyclicBarrier(users.length);
                var executor = Executors.newFixedThreadPool(users.length);
                try {
                    for (int worker = 0; worker < users.length; worker++) {
                        int index = worker;
                        executor.execute(() -> {
                            try (Connection c = runtime.getConnection()) {
                                c.setAutoCommit(false);
                                try {
                                    context(c, users[index], null);
                                    ready.await(10, TimeUnit.SECONDS);
                                    eq(0, update(c, "UPDATE tenancy.companies SET name = name "
                                            + "WHERE tenant_id = ?", foreignTenants[index]));
                                    try {
                                        update(c, "INSERT INTO tenancy.companies (tenant_id, company_id, name) "
                                                + "VALUES (?, ?, 'parallel-cross-tenant')",
                                                foreignTenants[index], UUID.randomUUID());
                                        throw new AssertionError("PARALLEL_CROSS_TENANT_INSERT_NOT_DENIED");
                                    } catch (SQLException denied) {
                                        require("42501".equals(denied.getSQLState()));
                                    }
                                } finally {
                                    c.rollback();
                                }
                            } catch (Throwable failure) {
                                failures.add(failure);
                            }
                        });
                    }
                } finally {
                    executor.shutdown();
                    require(executor.awaitTermination(30, TimeUnit.SECONDS));
                }
                require(failures.isEmpty());
                raw(migrator, true, true, c -> {
                    eq(0, number(c, "SELECT count(*) FROM tenancy.companies "
                            + "WHERE name = 'parallel-cross-tenant'"));
                    return null;
                });
            } finally {
                runtime.setMaximumPoolSize(1);
            }
        });
    }

    private static void denialChecks(HikariDataSource runtime) throws Exception {
        for (String table : new String[]{"identity.users", "identity.password_credentials", "access.membership_roles"}) {
            check("runtime_insert_denied_" + table.replace('.', '_'), () -> expectSql("42501",
                    () -> raw(runtime, false, true, c -> update(c, "INSERT INTO " + table + " DEFAULT VALUES"))));
        }
        check("runtime_guard_read_denied", () -> expectSql("42501",
                () -> raw(runtime, true, false, c -> number(c, "SELECT count(*) FROM identity.provisioning_guard"))));
        check("runtime_cannot_change_history", () -> expectSql("42501", () -> raw(runtime, false, true,
                c -> update(c, "UPDATE public.flyway_schema_history SET checksum = 0"))));
        check("runtime_cannot_alter_identity", () -> expectSql("42501", () -> raw(runtime, false, true,
                c -> { execute(c, "ALTER TABLE identity.users DISABLE ROW LEVEL SECURITY"); return null; })));
    }

    private static void context(Connection c, UUID user, UUID tenant) throws SQLException {
        require(!c.getAutoCommit());
        text(c, "SELECT set_config('app.user_id', ?, true)", user.toString());
        if (tenant != null) text(c, "SELECT set_config('app.tenant_id', ?, true)", tenant.toString());
    }

    private static void blank(Connection c) throws SQLException {
        require(empty(text(c, "SELECT current_setting('app.user_id', true)")));
        require(empty(text(c, "SELECT current_setting('app.tenant_id', true)")));
    }

    private static boolean empty(String value) { return value == null || value.isEmpty(); }

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
