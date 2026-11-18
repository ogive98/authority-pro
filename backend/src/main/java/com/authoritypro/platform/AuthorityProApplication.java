package com.authoritypro.platform;

import java.util.ArrayList;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import org.springframework.boot.Banner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration;
import org.springframework.core.env.MapPropertySource;
import org.springframework.web.context.support.StandardServletEnvironment;

@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
public class AuthorityProApplication {
    public record SkeletonSettings(Mode mode) {
        public SkeletonSettings {
            Objects.requireNonNull(mode);
        }

        public enum Mode { SKELETON_LOCAL }

        public static SkeletonSettings from(Map<String, String> environment, Properties system, String[] arguments) {
            if (arguments.length != 0) {
                throw new IllegalArgumentException("COMMAND_LINE_DISABLED");
            }
            for (String key : environment.keySet()) {
                if (key.startsWith("SPRING_") || key.startsWith("SERVER_") || key.startsWith("MANAGEMENT_")
                        || (key.startsWith("AUTHORITY_PRO_") && !key.equals("AUTHORITY_PRO_MODE"))) {
                    throw new IllegalArgumentException("ENVIRONMENT_OVERRIDE_FORBIDDEN");
                }
            }
            for (String key : system.stringPropertyNames()) {
                if (key.startsWith("spring.") || key.startsWith("server.") || key.startsWith("management.")
                        || key.startsWith("authority.")) {
                    throw new IllegalArgumentException("SYSTEM_OVERRIDE_FORBIDDEN");
                }
            }
            String mode = environment.get("AUTHORITY_PRO_MODE");
            if (mode == null) {
                throw new IllegalArgumentException("MODE_REQUIRED");
            }
            if (!mode.equals("skeleton-local")) {
                throw new IllegalArgumentException("MODE_INVALID");
            }
            return new SkeletonSettings(Mode.SKELETON_LOCAL);
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
        application(settings).run();
    }

    public static SpringApplication application(SkeletonSettings settings) {
        var environment = new StandardServletEnvironment();
        var names = new ArrayList<String>();
        environment.getPropertySources().forEach(source -> names.add(source.getName()));
        names.forEach(environment.getPropertySources()::remove);
        environment.getPropertySources().addFirst(new MapPropertySource("locked-skeleton", Map.of(
                "spring.config.location", "classpath:/authority-skeleton.properties",
                "server.address", "127.0.0.1",
                "server.port", "8080")));
        var application = new SpringApplication(AuthorityProApplication.class);
        application.setEnvironment(environment);
        application.setAddCommandLineProperties(false);
        application.setBannerMode(Banner.Mode.OFF);
        application.setLogStartupInfo(false);
        application.addInitializers(context -> context.getBeanFactory().registerSingleton("skeletonSettings", settings));
        return application;
    }
}
