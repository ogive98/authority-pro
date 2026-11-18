package com.authoritypro.launch;

import com.authoritypro.platform.AuthorityProApplication.SkeletonSettings;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SkeletonSettingsTest {
    @Test
    void acceptsOnlyTheExplicitLocalMode() {
        assertThat(SkeletonSettings.from(Map.of("AUTHORITY_PRO_MODE", "skeleton-local"),
                new Properties(), new String[0]).mode()).isEqualTo(SkeletonSettings.Mode.SKELETON_LOCAL);
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
                "SPRING_APPLICATION_JSON", "SPRING_CONFIG_LOCATION", "SPRING_PROFILES_ACTIVE"}) {
            var environment = new HashMap<>(Map.of("AUTHORITY_PRO_MODE", "skeleton-local"));
            environment.put(key, "private-input");
            assertThatThrownBy(() -> SkeletonSettings.from(environment, new Properties(), new String[0]))
                    .hasMessage("ENVIRONMENT_OVERRIDE_FORBIDDEN");
        }
    }

    @Test
    void rejectsJavaAndCommandLineSecurityOverrides() {
        for (String key : new String[]{"spring.config.import", "server.address", "management.server.port", "authority.mode"}) {
            var system = new Properties();
            system.setProperty(key, "private-input");
            assertThatThrownBy(() -> SkeletonSettings.from(Map.of("AUTHORITY_PRO_MODE", "skeleton-local"),
                    system, new String[0])).hasMessage("SYSTEM_OVERRIDE_FORBIDDEN");
        }
        assertThatThrownBy(() -> SkeletonSettings.from(Map.of("AUTHORITY_PRO_MODE", "skeleton-local"),
                new Properties(), new String[]{"--server.address=0.0.0.0"})).hasMessage("COMMAND_LINE_DISABLED");
    }
}
