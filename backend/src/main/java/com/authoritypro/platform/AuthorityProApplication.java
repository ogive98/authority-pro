package com.authoritypro.platform;

import com.authoritypro.platform.runtime.infrastructure.persistence.LocalDatabase;
import com.zaxxer.hikari.HikariDataSource;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.boot.Banner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration;
import org.springframework.core.env.MapPropertySource;
import org.springframework.web.context.support.StandardServletEnvironment;

@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
public class AuthorityProApplication {
    public interface DatabaseBootstrap {
        HikariDataSource runtime(InputStream input);
        int migrate(InputStream input);
        String[] provisionerSecrets(InputStream input);
        HikariDataSource provisioner(String password);
    }

    /** Identity owns the bootstrap use case; the entrypoint only supplies the provisioner transaction. */
    public interface OperatorProvisioner {
        void provision(Connection connection, String login, char[] password);
    }

    private static DatabaseBootstrap databaseBootstrap() {
        return singleService(DatabaseBootstrap.class, "DATABASE_STARTUP_REFUSED");
    }

    private static OperatorProvisioner operatorProvisioner() {
        return singleService(OperatorProvisioner.class, "PROVISIONING_REFUSED");
    }

    private static <T> T singleService(Class<T> type, String refusal) {
        try {
            var providers = ServiceLoader.load(type).iterator();
            if (!providers.hasNext()) throw new IllegalStateException(refusal);
            var provider = providers.next();
            if (providers.hasNext()) throw new IllegalStateException(refusal);
            return provider;
        } catch (ServiceConfigurationError failure) {
            throw new IllegalStateException(refusal);
        }
    }

    public record SkeletonSettings(Mode mode) {
        public SkeletonSettings {
            Objects.requireNonNull(mode);
        }

        public enum Mode { SKELETON_LOCAL, DATA_LOCAL, MIGRATE_LOCAL, PROVISION_LOCAL }

        public static SkeletonSettings from(Map<String, String> environment, Properties system, String[] arguments) {
            if (arguments.length != 0) {
                throw new IllegalArgumentException("COMMAND_LINE_DISABLED");
            }
            for (String key : environment.keySet()) {
                if (key.equals("AUTHORITY_PRO_DATABASE")) continue;
                if (key.startsWith("SPRING_") || key.startsWith("SERVER_") || key.startsWith("MANAGEMENT_")
                        || key.startsWith("PG") || key.startsWith("FLYWAY_") || key.startsWith("JDBC_")
                        || key.startsWith("HIKARI_") || key.startsWith("DATABASE_") || key.startsWith("DB_")
                        || (key.startsWith("AUTHORITY_PRO_") && !key.equals("AUTHORITY_PRO_MODE"))) {
                    throw new IllegalArgumentException("ENVIRONMENT_OVERRIDE_FORBIDDEN");
                }
            }
            for (String key : system.stringPropertyNames()) {
                if (key.startsWith("spring.") || key.startsWith("server.") || key.startsWith("management.")
                        || key.startsWith("authority.") || key.startsWith("flyway.") || key.startsWith("jdbc.")
                        || key.startsWith("hikari.") || key.startsWith("com.zaxxer.hikari.")
                        || key.startsWith("org.postgresql.") || key.startsWith("database.") || key.startsWith("db.")) {
                    throw new IllegalArgumentException("SYSTEM_OVERRIDE_FORBIDDEN");
                }
            }
            String selection = environment.get("AUTHORITY_PRO_DATABASE");
            if (selection != null && !LocalDatabase.DISPOSABLE_TEST_DATABASE.equals(selection)) {
                throw new IllegalArgumentException("DATABASE_SELECTION_INVALID");
            }
            String mode = environment.get("AUTHORITY_PRO_MODE");
            if (mode == null) {
                throw new IllegalArgumentException("MODE_REQUIRED");
            }
            return new SkeletonSettings(switch (mode) {
                case "skeleton-local" -> Mode.SKELETON_LOCAL;
                case "data-local" -> Mode.DATA_LOCAL;
                case "migrate-local" -> Mode.MIGRATE_LOCAL;
                case "provision-local" -> Mode.PROVISION_LOCAL;
                default -> throw new IllegalArgumentException("MODE_INVALID");
            });
        }
    }

    public static void main(String[] args) {
        SkeletonSettings settings;
        try {
            settings = SkeletonSettings.from(System.getenv(), System.getProperties(), args);
        } catch (IllegalArgumentException exception) {
            System.err.println("Skeleton startup refused: " + exception.getMessage());
            System.exit(2);
            return;
        }
        if (settings.mode() == SkeletonSettings.Mode.SKELETON_LOCAL) {
            application(settings).run();
            return;
        }
        try {
            if (settings.mode() == SkeletonSettings.Mode.MIGRATE_LOCAL) {
                int count = databaseBootstrap().migrate(System.in);
                System.out.println("MIGRATION_OK count=" + count);
            } else if (settings.mode() == SkeletonSettings.Mode.PROVISION_LOCAL) {
                provisionOperator();
            } else {
                application(settings).run();
            }
        } catch (RuntimeException exception) {
            System.err.println(settings.mode() == SkeletonSettings.Mode.PROVISION_LOCAL
                    ? "PROVISIONING_REFUSED" : "DATABASE_STARTUP_REFUSED");
            System.exit(2);
        }
    }

    /** Reads provisioner secret, operator login and password; one transaction creates the first operator. */
    private static void provisionOperator() {
        DatabaseBootstrap bootstrap = databaseBootstrap();
        String[] secrets = bootstrap.provisionerSecrets(System.in);
        char[] password = secrets[2].toCharArray();
        try (HikariDataSource source = bootstrap.provisioner(secrets[0]);
                Connection connection = source.getConnection()) {
            connection.setAutoCommit(false);
            try {
                operatorProvisioner().provision(connection, secrets[1], password);
                connection.commit();
            } catch (RuntimeException | SQLException failure) {
                connection.rollback();
                throw new IllegalStateException("PROVISIONING_REFUSED");
            } finally {
                java.util.Arrays.fill(password, '\0');
            }
        } catch (SQLException failure) {
            throw new IllegalStateException("PROVISIONING_REFUSED");
        }
        System.out.println("OPERATOR_PROVISIONED");
    }

    public static SpringApplication application(SkeletonSettings settings) {
        if (settings.mode() == SkeletonSettings.Mode.MIGRATE_LOCAL) {
            throw new IllegalArgumentException("MIGRATION_CONTEXT_FORBIDDEN");
        }
        var environment = new StandardServletEnvironment();
        var names = new ArrayList<String>();
        environment.getPropertySources().forEach(source -> names.add(source.getName()));
        names.forEach(environment.getPropertySources()::remove);
        var locked = new LinkedHashMap<String, Object>(Map.of(
                "spring.config.location", "classpath:/authority-skeleton.properties",
                "server.address", "127.0.0.1",
                "server.port", "8080"));
        if (settings.mode() == SkeletonSettings.Mode.DATA_LOCAL) {
            locked.put("logging.level.root", "OFF");
        }
        environment.getPropertySources().addFirst(new MapPropertySource("locked-skeleton", locked));
        var application = new SpringApplication(AuthorityProApplication.class);
        application.setEnvironment(environment);
        application.setAddCommandLineProperties(false);
        application.setBannerMode(Banner.Mode.OFF);
        application.setLogStartupInfo(false);
        application.addInitializers(context -> {
            var beans = (DefaultListableBeanFactory) context.getBeanFactory();
            beans.registerSingleton("skeletonSettings", settings);
            if (settings.mode() == SkeletonSettings.Mode.DATA_LOCAL) {
                var source = databaseBootstrap().runtime(System.in);
                try {
                    beans.registerSingleton("dataSource", source);
                    beans.registerDisposableBean("dataSource", source::close);
                } catch (RuntimeException exception) {
                    source.close();
                    throw exception;
                }
            }
        });
        return application;
    }
}
