package com.authoritypro.openapi;

import com.authoritypro.platform.AuthorityProApplication;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import org.junit.jupiter.api.Test;
import org.springdoc.webmvc.api.OpenApiWebMvcResource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.annotation.DirtiesContext;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = {AuthorityProApplication.class, LocalOpenApiTest.Contract.class},
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"spring.config.location=classpath:/authority-skeleton.properties",
                "springdoc.api-docs.enabled=true", "springdoc.show-actuator=false"})
@DirtiesContext
class LocalOpenApiTest {
    @Autowired OpenApiWebMvcResource resource;
    @Value("${local.server.port}") int port;

    @Test
    void springdocExportsOnlyVerifiedProbesWithoutOpeningDocumentation() throws Exception {
        byte[] bytes = resource.openapiJson(new MockHttpServletRequest("GET", "/v3/api-docs"),
                "/v3/api-docs", Locale.ROOT);
        var document = new ObjectMapper().readTree(bytes);
        assertThat(document.path("openapi").asText()).startsWith("3.");
        var paths = document.path("paths");
        assertThat(paths.size()).isEqualTo(2);
        assertThat(paths.has("/actuator/health/liveness")).isTrue();
        assertThat(paths.has("/actuator/health/readiness")).isTrue();
        try (var client = HttpClient.newHttpClient()) {
            for (String probe : List.of("liveness", "readiness")) {
                String path = "/actuator/health/" + probe;
                var response = client.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path))
                        .timeout(Duration.ofSeconds(5)).header("Accept", "application/json").GET().build(),
                        HttpResponse.BodyHandlers.ofString());
                assertThat(paths.path(path).path("get").path("responses").has(String.valueOf(response.statusCode()))).isTrue();
                String expectedStatus = probe.equals("liveness") ? "UP" : "OUT_OF_SERVICE";
                assertThat(response.body()).isEqualTo("{\"status\":\"" + expectedStatus + "\"}");
            }
            var denied = client.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/v3/api-docs"))
                    .timeout(Duration.ofSeconds(5)).GET().build(), HttpResponse.BodyHandlers.ofString());
            assertThat(denied.statusCode()).isEqualTo(403);
        }
        Files.write(Path.of("target", "openapi.json"), bytes);
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class Contract {
        @Bean
        OpenAPI localProbeContract() {
            return new OpenAPI().info(new Info().title("Authority Pro local skeleton").version("0.0.1")
                    .description("Loopback probes only; all other requests are denied. No identity or tenant API."))
                    .paths(new Paths()
                            .addPathItem("/actuator/health/liveness", probe("200", "UP"))
                            .addPathItem("/actuator/health/readiness", probe("503", "OUT_OF_SERVICE")));
        }

        private static PathItem probe(String status, String health) {
            var schema = new ObjectSchema().addProperty("status", new StringSchema()._enum(List.of(health)))
                    .required(List.of("status")).additionalProperties(false);
            return new PathItem().get(new Operation().responses(new ApiResponses().addApiResponse(status,
                    new ApiResponse().description(health).content(new Content().addMediaType("application/json",
                            new MediaType().schema(schema))))));
        }
    }
}
