package com.authoritypro.launch;

import com.authoritypro.platform.AuthorityProApplication.SkeletonSettings;
import com.authoritypro.platform.runtime.infrastructure.persistence.LocalDatabase;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SkeletonSettingsTest {
    @Test
    void acceptsOnlyTheThreeExplicitLocalModesWithoutAddingSecretSettings() {
        for (var entry : Map.of("skeleton-local", SkeletonSettings.Mode.SKELETON_LOCAL,
                "data-local", SkeletonSettings.Mode.DATA_LOCAL,
                "migrate-local", SkeletonSettings.Mode.MIGRATE_LOCAL).entrySet()) {
            assertThat(SkeletonSettings.from(Map.of("AUTHORITY_PRO_MODE", entry.getKey()),
                    new Properties(), new String[0]).mode()).isEqualTo(entry.getValue());
        }
        assertThat(SkeletonSettings.class.getRecordComponents()).hasSize(1);
        assertThat(SkeletonSettings.class.getRecordComponents()[0].getName()).isEqualTo("mode");
    }

    @Test
    void refusesMissingAndUnknownModesWithoutEchoingInput() {
        assertThatThrownBy(() -> SkeletonSettings.from(Map.of(), new Properties(), new String[0]))
                .hasMessage("MODE_REQUIRED");
        assertThatThrownBy(() -> SkeletonSettings.from(Map.of("AUTHORITY_PRO_MODE", "private-input"),
                new Properties(), new String[0])).hasMessage("MODE_INVALID");
    }

    @Test
    void rejectsUnknownApplicationKeysAndSpringOverrides() {
        for (String key : new String[]{"AUTHORITY_PRO_UNKNOWN", "SERVER_ADDRESS", "MANAGEMENT_SERVER_PORT",
                "SPRING_APPLICATION_JSON", "SPRING_CONFIG_LOCATION", "SPRING_PROFILES_ACTIVE",
                "PGHOST", "PGPORT", "PGDATABASE", "PGUSER", "PGPASSWORD", "PGSERVICE", "PGOPTIONS",
                "AUTHORITY_PRO_PASSWORD", "DATABASE_URL", "DB_PASSWORD",
                "JDBC_URL", "FLYWAY_URL", "HIKARI_PASSWORD"}) {
            var environment = new HashMap<>(Map.of("AUTHORITY_PRO_MODE", "skeleton-local"));
            environment.put(key, "private-input");
            assertThatThrownBy(() -> SkeletonSettings.from(environment, new Properties(), new String[0]))
                    .hasMessage("ENVIRONMENT_OVERRIDE_FORBIDDEN");
        }
    }

    @Test
    void rejectsJavaAndCommandLineSecurityOverrides() {
        for (String key : new String[]{"spring.config.import", "server.address", "management.server.port", "authority.mode",
                "jdbc.url", "flyway.password", "hikari.password", "com.zaxxer.hikari.blockUntilFilled",
                "org.postgresql.forcebinary", "database.url", "db.password"}) {
            var system = new Properties();
            system.setProperty(key, "private-input");
            assertThatThrownBy(() -> SkeletonSettings.from(Map.of("AUTHORITY_PRO_MODE", "skeleton-local"),
                    system, new String[0])).hasMessage("SYSTEM_OVERRIDE_FORBIDDEN");
        }
        assertThatThrownBy(() -> SkeletonSettings.from(Map.of("AUTHORITY_PRO_MODE", "skeleton-local"),
                new Properties(), new String[]{"--server.address=0.0.0.0"})).hasMessage("COMMAND_LINE_DISABLED");
    }

    @Test
    void allowsOnlyTheDisposableEvidenceDatabaseSelection() {
        for (var entry : Map.of("skeleton-local", SkeletonSettings.Mode.SKELETON_LOCAL,
                "data-local", SkeletonSettings.Mode.DATA_LOCAL,
                "migrate-local", SkeletonSettings.Mode.MIGRATE_LOCAL,
                "provision-local", SkeletonSettings.Mode.PROVISION_LOCAL).entrySet()) {
            var environment = new HashMap<>(Map.of("AUTHORITY_PRO_MODE", entry.getKey(),
                    "AUTHORITY_PRO_DATABASE", LocalDatabase.DISPOSABLE_TEST_DATABASE));
            assertThat(SkeletonSettings.from(environment, new Properties(), new String[0]).mode())
                    .isEqualTo(entry.getValue());
        }
        for (String selection : new String[]{"authority_pro", "postgres", "", "private-input",
                "authority_pro_l07_ui_test2", "Authority_pro_l07_ui_test", "authority_pro_l07_ui_test "}) {
            var environment = new HashMap<>(Map.of("AUTHORITY_PRO_MODE", "data-local",
                    "AUTHORITY_PRO_DATABASE", selection));
            assertThatThrownBy(() -> SkeletonSettings.from(environment, new Properties(), new String[0]))
                    .hasMessage("DATABASE_SELECTION_INVALID");
        }
    }
}
