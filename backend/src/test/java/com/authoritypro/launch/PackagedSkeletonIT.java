package com.authoritypro.launch;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.jar.JarFile;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class PackagedSkeletonIT {
    private static final Path JAR = Path.of(System.getProperty("skeleton.jar")).toAbsolutePath();
    private static final List<String> REQUIRED = List.of("spring-jdbc-7.0.10.jar", "HikariCP-7.0.2.jar",
            "postgresql-42.7.14.jar", "flyway-core-12.4.0.jar", "flyway-database-postgresql-12.4.0.jar",
            "spring-session-core-4.1.1.jar", "spring-session-jdbc-4.1.1.jar");
    private static final List<String> FORBIDDEN = List.of("spring-data-",
            "spring-boot-jdbc-", "spring-boot-flyway-", "spring-boot-data-jpa-",
            "hibernate-core-", "hibernate-envers-", "hibernate-spatial-", "h2-",
            "hsqldb-", "mysql-", "mariadb-", "r2dbc-",
            "jakarta.persistence-", "springdoc-", "swagger-", "archunit-", "spring-modulith-core-",
            "spring-modulith-events-", "junit-", "spring-boot-test-");

    @Test
    @Order(5)
    void servingArtifactPinsExplicitDatabaseLibrariesWithoutAutoconfigurationOrExcludedEngines() throws Exception {
        assertThat(JAR).isRegularFile();
        try (var archive = new JarFile(JAR.toFile())) {
            var names = archive.stream().map(entry -> entry.getName()).toList();
            assertThat(names).anyMatch(name -> name.equals("BOOT-INF/classes/com/authoritypro/platform/AuthorityProApplication.class"));
            for (String library : REQUIRED) {
                assertThat(names).contains("BOOT-INF/lib/" + library);
            }
            for (String prefix : List.of("spring-jdbc-", "HikariCP-", "postgresql-", "flyway-", "spring-session-")) {
                assertThat(names.stream().filter(name -> name.startsWith("BOOT-INF/lib/" + prefix)))
                        .allMatch(name -> REQUIRED.contains(name.substring("BOOT-INF/lib/".length())));
            }
            for (String prefix : FORBIDDEN) {
                assertThat(names).noneMatch(name -> name.startsWith("BOOT-INF/lib/" + prefix));
            }
            assertThat(names).noneMatch(name -> name.contains("boundaryfixtures") || name.contains("com/authoritypro/http/"));
        }
    }

    @Test
    @Order(6)
    void packagedContentMatchesCompiledClassesAndRecordsItsDigest() throws Exception {
        Path classes = JAR.getParent().resolve("classes");
        try (var archive = new JarFile(JAR.toFile()); var files = Files.walk(classes)) {
            var compiled = files.filter(Files::isRegularFile).toList();
            var expected = compiled.stream().map(path -> "BOOT-INF/classes/"
                    + classes.relativize(path).toString().replace('\\', '/')).toList();
            assertThat(archive.stream().filter(entry -> !entry.isDirectory())
                    .map(entry -> entry.getName()).filter(name -> name.startsWith("BOOT-INF/classes/")))
                    .containsExactlyInAnyOrderElementsOf(expected);
            for (Path path : compiled) {
                String name = "BOOT-INF/classes/" + classes.relativize(path).toString().replace('\\', '/');
                try (var input = archive.getInputStream(archive.getJarEntry(name))) {
                    assertThat(input.readAllBytes()).as(name).isEqualTo(Files.readAllBytes(path));
                }
            }
        }
        String digest = java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256")
                .digest(Files.readAllBytes(JAR)));
        Files.writeString(JAR.getParent().resolve("packaged-artifact.sha256"), digest + "\n",
                java.nio.file.StandardOpenOption.CREATE_NEW);
    }

    @Test
    @Order(4)
    void packagedLauncherRefusesMissingModeAndSecurityOverrides() throws Exception {
        assertRefused(Map.of(), List.of(), List.of(), "MODE_REQUIRED");
        assertRefused(Map.of("AUTHORITY_PRO_MODE", "private-input"), List.of(), List.of(), "MODE_INVALID");
        assertRefused(Map.of("AUTHORITY_PRO_MODE", "skeleton-local", "SPRING_APPLICATION_JSON", "private-input"),
                List.of(), List.of(), "ENVIRONMENT_OVERRIDE_FORBIDDEN");
        assertRefused(Map.of("AUTHORITY_PRO_MODE", "skeleton-local"), List.of("-Dserver.address=private-input"),
                List.of(), "SYSTEM_OVERRIDE_FORBIDDEN");
        assertRefused(Map.of("AUTHORITY_PRO_MODE", "skeleton-local"), List.of(),
                List.of("--server.address=private-input"), "COMMAND_LINE_DISABLED");
    }

    @Test
    @Order(3)
    void databaseModesRefuseTargetAndPasswordOverridesWithoutEchoingValues() throws Exception {
        for (String mode : List.of("data-local", "migrate-local")) {
            for (String key : List.of("AUTHORITY_PRO_DATABASE", "AUTHORITY_PRO_PASSWORD", "PGPASSWORD",
                    "PGHOST", "DATABASE_URL", "SPRING_DATASOURCE_URL", "FLYWAY_URL")) {
                assertRefused(Map.of("AUTHORITY_PRO_MODE", mode, key, "private-input"),
                        List.of(), List.of(), "ENVIRONMENT_OVERRIDE_FORBIDDEN");
            }
            assertRefused(Map.of("AUTHORITY_PRO_MODE", mode), List.of("-Djdbc.url=private-input"),
                    List.of(), "SYSTEM_OVERRIDE_FORBIDDEN");
            assertRefused(Map.of("AUTHORITY_PRO_MODE", mode), List.of(),
                    List.of("--password=private-input"), "COMMAND_LINE_DISABLED");
        }
    }

    @Test
    @Order(1)
    void databaseModesRefuseMalformedStdinBeforeAnyConnectionOrServlet() throws Exception {
        try (var socket = reservePort()) {
            for (String mode : List.of("data-local", "migrate-local")) {
                for (String input : List.of("", "\n", "private-input", "x".repeat(257) + "\n",
                        "private-input\nextra\n", "private-input\rwrong\n")) {
                    var run = start(Map.of("AUTHORITY_PRO_MODE", mode), List.of(), List.of(), false);
                    try {
                        try (var stdin = run.process().getOutputStream()) {
                            stdin.write(input.getBytes(java.nio.charset.StandardCharsets.UTF_8));
                        }
                        assertThat(run.process().waitFor(15, TimeUnit.SECONDS)).isTrue();
                        assertThat(run.process().exitValue()).isEqualTo(2);
                        assertThat(Files.readString(run.log()).strip()).isEqualTo("DATABASE_STARTUP_REFUSED");
                        assertThat(socket.isClosed()).isFalse();
                    } finally {
                        stop(run.process());
                    }
                }
            }
        }
    }

    @Test
    @Order(2)
    void occupiedPortFailsWithoutDisplacingItsOwner() throws Exception {
        try (var socket = reservePort()) {
            var run = start(Map.of("AUTHORITY_PRO_MODE", "skeleton-local"), List.of(), List.of(), false);
            try {
                assertThat(run.process().waitFor(35, TimeUnit.SECONDS)).as("Startup must fail on port collision").isTrue();
                assertThat(run.process().exitValue()).isNotZero();
                assertThat(socket.isClosed()).isFalse();
                assertThat(Files.readString(run.log())).contains("Port 8080 was already in use")
                        .doesNotContain("Using generated security password");
            } finally {
                stop(run.process());
            }
        }
    }

    @Test
    @Order(7)
    void actualJarIgnoresAmbientFilesAndServesOnlyClosedLocalProbes() throws Exception {
        try (var reservation = reservePort()) {
            assertThat(reservation.isBound()).isTrue();
        }
        var run = start(Map.of("AUTHORITY_PRO_MODE", "skeleton-local"), List.of(), List.of(), true);
        run.process().getOutputStream().write("private-input-without-newline".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        run.process().getOutputStream().flush();
        try (var client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(1))
                .followRedirects(HttpClient.Redirect.NEVER).build()) {
            await().atMost(Duration.ofSeconds(35)).pollInterval(Duration.ofMillis(150)).until(() -> {
                assertThat(run.process().isAlive()).withFailMessage(() -> readLog(run.log())).isTrue();
                try {
                    return get(client, "/actuator/health/liveness").statusCode() == 200;
                } catch (IOException exception) {
                    return false;
                }
            });
            var live = get(client, "/actuator/health/liveness");
            assertThat(live.body()).isEqualTo("{\"status\":\"UP\"}");
            assertThat(live.headers().allValues("set-cookie")).isEmpty();
            var ready = get(client, "/actuator/health/readiness");
            assertThat(ready.statusCode()).isEqualTo(503);
            assertThat(ready.body()).isEqualTo("{\"status\":\"OUT_OF_SERVICE\"}");
            assertThat(ready.headers().firstValue("X-Correlation-Id").orElse(""))
                    .matches("[0-9a-f]{32}");
            String inherited = "0123456789abcdef0123456789abcdef";
            var correlated = get(client, "/", "X-Correlation-Id", "0123456789ABCDEF0123456789ABCDEF");
            assertThat(correlated.statusCode()).isEqualTo(403);
            assertThat(correlated.headers().firstValue("X-Correlation-Id")).hasValue(inherited);
            var replaced = get(client, "/", "X-Correlation-Id", "not-a-correlation");
            String generated = replaced.headers().firstValue("X-Correlation-Id").orElseThrow();
            assertThat(generated).matches("[0-9a-f]{32}").isNotEqualTo("not-a-correlation");
            var queried = get(client, "/?token=query-secret-marker");
            assertThat(queried.statusCode()).isEqualTo(403);
            for (String path : List.of("/", "/actuator", "/actuator/health", "/actuator/env", "/v3/api-docs",
                    "/swagger-ui/index.html", "/api/v1/auth/me", "/api/v1/auth/bootstrap")) {
                var denied = get(client, path);
                assertThat(denied.statusCode()).as(path).isEqualTo(403);
                assertThat(denied.headers().firstValue("content-type").orElse("")).contains("application/problem+json");
                assertThat(denied.headers().firstValue("X-Correlation-Id").orElse("")).matches("[0-9a-f]{32}");
                assertThat(denied.headers().allValues("set-cookie")).isEmpty();
                assertThat(denied.headers().allValues("www-authenticate")).isEmpty();
                assertThat(denied.body()).contains("\"status\":403", "\"traceId\"").doesNotContain("private-input");
            }
            assertThat(Files.readString(run.log())).doesNotContain("Using generated security password")
                    .doesNotContain("query-secret-marker", "not-a-correlation")
                    .contains("\"correlationId\":\"" + inherited + "\"", "\"message\":\"request\"");
        } finally {
            stop(run.process());
        }
        assertThat(run.process().isAlive()).as("The packaged test process must be stopped").isFalse();
    }

    private static HttpResponse<String> get(HttpClient client, String path, String... headers)
            throws IOException, InterruptedException {
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:8080" + path))
                .timeout(Duration.ofSeconds(3)).GET();
        for (int index = 0; index < headers.length; index += 2) {
            request.header(headers[index], headers[index + 1]);
        }
        return client.send(request.build(), HttpResponse.BodyHandlers.ofString());
    }

    private static ServerSocket reservePort() throws IOException {
        try {
            return new ServerSocket(8080, 1, InetAddress.getByName("127.0.0.1"));
        } catch (IOException exception) {
            throw new IOException("L02 requires free loopback port 8080; existing services were not stopped", exception);
        }
    }

    private static void assertRefused(Map<String, String> environment, List<String> vmOptions,
            List<String> arguments, String category) throws Exception {
        var run = start(environment, vmOptions, arguments, false);
        try {
            assertThat(run.process().waitFor(15, TimeUnit.SECONDS)).isTrue();
            assertThat(run.process().exitValue()).isEqualTo(2);
            assertThat(Files.readString(run.log())).contains(category).doesNotContain("private-input");
        } finally {
            stop(run.process());
        }
    }

    private static Run start(Map<String, String> environment, List<String> vmOptions,
            List<String> arguments, boolean ambientFile) throws IOException {
        Path directory = Files.createTempDirectory(Path.of("target"), "packaged-").toAbsolutePath();
        if (ambientFile) {
            Files.writeString(directory.resolve("application.properties"),
                    "server.address=0.0.0.0\nserver.port=1\nspring.config.import=file:private-input\n");
        }
        Path log = directory.resolve("process.log");
        String executable = System.getProperty("os.name").startsWith("Windows") ? "java.exe" : "java";
        var command = new ArrayList<String>();
        command.add(Path.of(System.getProperty("java.home"), "bin", executable).toString());
        command.add("-Xmx256m");
        command.add("-Djava.io.tmpdir=" + directory);
        command.addAll(vmOptions);
        command.addAll(List.of("-jar", JAR.toString()));
        command.addAll(arguments);
        var builder = new ProcessBuilder(command).directory(directory.toFile())
                .redirectErrorStream(true).redirectOutput(log.toFile());
        builder.environment().clear();
        String systemRoot = System.getenv("SystemRoot");
        if (systemRoot != null) {
            builder.environment().put("SystemRoot", systemRoot);
        }
        builder.environment().putAll(environment);
        return new Run(builder.start(), log);
    }

    private static String readLog(Path log) {
        try {
            return Files.readString(log);
        } catch (IOException exception) {
            return "Packaged process stopped; test log unavailable";
        }
    }

    private static void stop(Process process) throws InterruptedException {
        process.destroy();
        if (!process.waitFor(10, TimeUnit.SECONDS)) {
            assertThat(process.destroyForcibly().waitFor(5, TimeUnit.SECONDS))
                    .as("The owned test process must terminate").isTrue();
        }
    }

    private record Run(Process process, Path log) {}
}
