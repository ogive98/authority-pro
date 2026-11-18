package com.authoritypro.http;

import com.authoritypro.platform.AuthorityProApplication;
import com.authoritypro.platform.runtime.infrastructure.http.HttpProblemAdvice;
import com.authoritypro.platform.runtime.infrastructure.http.HttpProblemBoundaryFilter;
import com.authoritypro.platform.runtime.infrastructure.http.HttpProblems;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.net.HttpCookie;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.SQLException;
import java.time.Duration;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.context.WebServerApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.NullSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextHolderFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.XorCsrfTokenRequestAttributeHandler;
import org.springframework.security.web.savedrequest.NullRequestCache;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/** One real server; test-only controllers are exercised separately, never component-scanned. */
@SpringBootTest(classes = AuthorityProApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "spring.config.location=classpath:/authority-skeleton.properties")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class HttpBoundaryTest {

    private static final List<String> PROBES = List.of(
            "/actuator/health/liveness", "/actuator/health/readiness");
    private static final String LEAK = "secret-password SELECT * FROM credentials";
    private static final String CLIENT_TRACE = "11111111111111111111111111111111";
    private static final TypeReference<Map<String, Object>> OBJECT = new TypeReference<>() { };

    @Autowired
    private ConfigurableApplicationContext context;
    @Autowired
    private JsonMapper mapper;
    @Autowired
    private HttpProblems problems;
    @Autowired
    private HttpProblemAdvice advice;
    @Autowired
    private CookieCsrfTokenRepository csrfRepository;

    private HttpClient client;
    private String origin;
    private MockMvc isolatedMvc;

    @BeforeAll
    void setUp() {
        int port = ((WebServerApplicationContext) context).getWebServer().getPort();
        origin = "http://127.0.0.1:" + port;
        client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3))
                .followRedirects(HttpClient.Redirect.NEVER).version(HttpClient.Version.HTTP_1_1).build();
        isolatedMvc = MockMvcBuilders.standaloneSetup(new FixtureController())
                .setControllerAdvice(advice)
                .setValidator(context.getBean(org.springframework.validation.Validator.class))
                .setMessageConverters(new JacksonJsonHttpMessageConverter(mapper))
                .build();
    }

    @AfterAll
    void closeClient() {
        if (client != null) {
            client.close();
        }
    }

    @Test
    void exactGetProbesAreStatusOnlyAndDoNotBootstrapCookies() throws Exception {
        HttpResponse<String> live = send("GET", PROBES.get(0));
        assertEquals(200, live.statusCode());
        assertEquals(Map.of("status", "UP"), object(live.body()));
        HttpResponse<String> ready = send("GET", PROBES.get(1));
        assertEquals(503, ready.statusCode());
        assertEquals(Map.of("status", "OUT_OF_SERVICE"), object(ready.body()));
        for (HttpResponse<String> response : List.of(live, ready)) {
            MediaType contentType = MediaType.parseMediaType(response.headers().firstValue("Content-Type").orElseThrow());
            assertTrue(Set.of("application/json", "application/vnd.spring-boot.actuator.v3+json")
                    .contains(contentType.getType() + "/" + contentType.getSubtype()));
            assertTrue(response.headers().allValues("Set-Cookie").isEmpty());
            assertClosedHeaders(response);
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"/", "/actuator", "/actuator/health", "/actuator/health/",
            "/actuator/info", "/actuator/env", "/actuator/metrics", "/actuator/shutdown",
            "/actuator/health/liveness/", "/actuator/health/readiness/",
            "/actuator/health/liveness/extra", "/actuator/health/LIVENESS",
            "/actuator/health/%6civeness", "/v3/api-docs", "/v3/api-docs.yaml",
            "/v3/api-docs/swagger-config", "/swagger-ui/index.html", "/swagger-ui.html",
            "/api/v1/auth/login", "/auth/login", "/login", "/logout", "/bootstrap",
            "/api/v1/bootstrap", "/csrf", "/error", "/fixture/failure"})
    void everyOtherGetIsForbidden(String path) throws Exception {
        assertProblem(send("GET", path), 403, "ACCESS_DENIED");
    }

    @ParameterizedTest
    @ValueSource(strings = {"HEAD", "POST", "OPTIONS", "PUT", "PATCH", "DELETE", "TRACE"})
    void nonGetMethodsCannotUseProbeAllowlist(String method) throws Exception {
        for (String path : List.of(PROBES.get(0), PROBES.get(1), "/api/v1/auth/login")) {
            HttpResponse<String> response = send(method, path);
            assertEquals(403, response.statusCode(), method + " " + path);
            assertClosedHeaders(response);
            if (method.equals("HEAD")) {
                assertTrue(response.body().isEmpty());
                assertTrue(response.headers().firstValue("Content-Type").orElse("")
                        .startsWith(MediaType.APPLICATION_PROBLEM_JSON_VALUE));
            } else {
                String code = Set.of("POST", "PUT", "PATCH", "DELETE").contains(method)
                        ? "CSRF_REJECTED" : "ACCESS_DENIED";
                assertProblem(response, 403, code);
            }
        }
    }

    @Test
    void csrfRemainsActiveButAValidTokenGrantsNoMutationCapability() throws Exception {
        HttpResponse<String> missing = send("POST", PROBES.get(0));
        assertProblem(missing, 403, "CSRF_REJECTED");
        String setCookie = missing.headers().allValues("Set-Cookie").stream()
                .filter(value -> value.startsWith("XSRF-TOKEN=")).findFirst().orElseThrow();
        assertTrue(setCookie.contains("HttpOnly"));
        assertTrue(setCookie.contains("SameSite=Strict"));
        assertTrue(setCookie.contains("Path=/"));
        HttpCookie cookie = HttpCookie.parse(setCookie).getFirst();
        String cookieHeader = cookie.getName() + "=" + cookie.getValue();

        MockHttpServletRequest request = new MockHttpServletRequest("POST", PROBES.get(0));
        request.setCookies(new Cookie(cookie.getName(), cookie.getValue()));
        CsrfToken persistedToken = csrfRepository.loadToken(request);
        assertNotNull(persistedToken);
        XorCsrfTokenRequestAttributeHandler xor = new XorCsrfTokenRequestAttributeHandler();
        xor.handle(request, new MockHttpServletResponse(), () -> persistedToken);
        CsrfToken maskedToken = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
        String validHeader = maskedToken.getToken();
        assertNotEquals(persistedToken.getToken(), validHeader);
        assertNull(request.getSession(false));

        assertProblem(send("POST", PROBES.get(0), "Cookie", cookieHeader,
                persistedToken.getHeaderName(), "invalid-secret-password"), 403, "CSRF_REJECTED");
        for (String method : List.of("POST", "PUT", "PATCH", "DELETE")) {
            for (String path : List.of(PROBES.get(0), PROBES.get(1), "/api/v1/auth/login", "/bootstrap")) {
                // ACCESS_DENIED, not CSRF_REJECTED, proves the valid framework token passed CSRF.
                assertProblem(send(method, path, "Cookie", cookieHeader,
                        persistedToken.getHeaderName(), validHeader), 403, "ACCESS_DENIED");
            }
        }
    }

    @Test
    void credentialsCookiesAndCorsHeadersCannotOpenTheBoundary() throws Exception {
        assertProblem(send("GET", "/bootstrap", "Authorization", "Basic dXNlcjpwYXNzd29yZA==",
                "Cookie", "JSESSIONID=attacker-session"), 403, "ACCESS_DENIED");
        assertProblem(send("OPTIONS", PROBES.get(0), "Origin", "https://untrusted.invalid",
                "Access-Control-Request-Method", "GET"), 403, "ACCESS_DENIED");
        HttpResponse<String> live = send("GET", PROBES.get(0), "Origin", "https://untrusted.invalid",
                "Cookie", "JSESSIONID=attacker-session");
        assertEquals(200, live.statusCode());
        assertTrue(live.headers().allValues("Set-Cookie").isEmpty());
        assertClosedHeaders(live);
    }

    @ParameterizedTest
    @ValueSource(strings = {"/actuator/health/liveness;secret-password=credentials",
            "//actuator/health/liveness", "/actuator/health/%2eliveness"})
    void firewallRejectionUsesSanitizedProblemDetails(String path) throws Exception {
        assertProblem(send("GET", path), 400, "BAD_REQUEST");
    }

    @Test
    void denialUsesFreshServerOccurrenceIdsAndNeverReflectsClientDiagnostics() throws Exception {
        Set<String> traces = new HashSet<>();
        Set<String> occurrences = new HashSet<>();
        for (int i = 0; i < 3; i++) {
            HttpResponse<String> response = send("GET", "/secret-password?sql=SELECT&token=credentials",
                    "X-Trace-Id", CLIENT_TRACE,
                    "traceparent", "00-" + CLIENT_TRACE + "-1111111111111111-01");
            Map<String, Object> body = assertProblem(response, 403, "ACCESS_DENIED");
            traces.add((String) body.get("traceId"));
            occurrences.add((String) body.get("instance"));
        }
        assertEquals(3, traces.size());
        assertEquals(3, occurrences.size());
    }

    @Test
    void noDefaultUserProviderLoginOrSessionCapabilityIsInstalled() throws Exception {
        assertTrue(context.getBeansOfType(UserDetailsService.class).isEmpty());
        assertTrue(context.getBeansOfType(AuthenticationProvider.class).isEmpty());
        assertTrue(context.getBeansOfType(AuthenticationManager.class).isEmpty());
        assertNull(context.getBean(AuthenticationConfiguration.class).getAuthenticationManager());
        assertTrue(context.getBeansOfType(javax.sql.DataSource.class).isEmpty());

        FilterChainProxy proxy = context.getBean("springSecurityFilterChain", FilterChainProxy.class);
        assertEquals(1, proxy.getFilterChains().size());
        SecurityFilterChain chain = proxy.getFilterChains().getFirst();
        Set<String> forbiddenFilters = Set.of("BasicAuthenticationFilter", "UsernamePasswordAuthenticationFilter",
                "DefaultLoginPageGeneratingFilter", "DefaultLogoutPageGeneratingFilter", "LogoutFilter",
                "AnonymousAuthenticationFilter", "RememberMeAuthenticationFilter", "CorsFilter",
                "SecurityContextHolderAwareRequestFilter");
        for (var filter : chain.getFilters()) {
            assertFalse(forbiddenFilters.contains(filter.getClass().getSimpleName()), filter.getClass().getName());
            if (filter instanceof SecurityContextHolderFilter) {
                assertInstanceOf(NullSecurityContextRepository.class,
                        ReflectionTestUtils.getField(filter, "securityContextRepository"));
            }
            if (filter.getClass().getSimpleName().equals("RequestCacheAwareFilter")
                    || filter.getClass().getSimpleName().equals("ExceptionTranslationFilter")) {
                assertInstanceOf(NullRequestCache.class, ReflectionTestUtils.getField(filter, "requestCache"));
            }
        }
        CsrfFilter csrf = chain.getFilters().stream().filter(CsrfFilter.class::isInstance)
                .map(CsrfFilter.class::cast).findFirst().orElseThrow();
        assertEquals(csrfRepository, ReflectionTestUtils.getField(csrf, "tokenRepository"));
        assertTrue(chain.getFilters().stream().anyMatch(HttpProblemBoundaryFilter.class::isInstance));
        assertTrue(chain.getFilters().stream().anyMatch(SecurityContextHolderFilter.class::isInstance));
    }

    @Test
    void filterChainNeverCreatesSessionsForAllowedOrDeniedRequests() throws Exception {
        FilterChainProxy proxy = context.getBean("springSecurityFilterChain", FilterChainProxy.class);
        for (String path : List.of(PROBES.get(0), PROBES.get(1), "/api/v1/auth/login", "/csrf")) {
            for (String method : List.of("GET", "POST", "HEAD", "OPTIONS")) {
                var request = new MockHttpServletRequest(method, path);
                var response = new MockHttpServletResponse();
                var reachedHandler = new java.util.concurrent.atomic.AtomicBoolean();
                proxy.doFilter(request, response, (incoming, outgoing) -> reachedHandler.set(true));
                assertNull(request.getSession(false), method + " " + path);
                assertEquals(method.equals("GET") && PROBES.contains(path), reachedHandler.get());
            }
        }
    }

    @Test
    void fixturesAreNotDiscoveredByTheServingApplication() {
        assertTrue(context.getBeansOfType(FixtureController.class).isEmpty());
        for (RequestMappingHandlerMapping mapping : context.getBeansOfType(RequestMappingHandlerMapping.class).values()) {
            assertTrue(mapping.getHandlerMethods().values().stream()
                    .noneMatch(method -> method.getBeanType().equals(FixtureController.class)));
        }
    }

    @Test
    void standaloneControllerValidationAndUnexpectedFailuresAreSanitized() throws Exception {
        MockHttpServletResponse validation = isolatedMvc.perform(post("/fixture/validate")
                .contentType(MediaType.APPLICATION_JSON).content("{\"value\":\"\"}"))
                .andReturn().getResponse();
        assertMockProblem(validation, 400, "VALIDATION_FAILED");
        MockHttpServletResponse methodValidation = isolatedMvc.perform(get("/fixture/parameter")
                .param("count", "0")).andReturn().getResponse();
        assertMockProblem(methodValidation, 400, "VALIDATION_FAILED");
        MockHttpServletResponse malformed = isolatedMvc.perform(post("/fixture/validate")
                .contentType(MediaType.APPLICATION_JSON).content("{\"value\": secret-password"))
                .andReturn().getResponse();
        assertMockProblem(malformed, 400, "BAD_REQUEST");
        MockHttpServletResponse failure = isolatedMvc.perform(get("/fixture/failure")
                .queryParam("token", "secret-password").header("X-Trace-Id", CLIENT_TRACE))
                .andReturn().getResponse();
        assertMockProblem(failure, 500, "INTERNAL_ERROR");
        MockHttpServletResponse forbidden = isolatedMvc.perform(get("/fixture/security"))
                .andReturn().getResponse();
        assertMockProblem(forbidden, 403, "ACCESS_DENIED");
        assertNotEquals(object(validation.getContentAsString()).get("traceId"),
                object(failure.getContentAsString()).get("traceId"));
    }

    @Test
    void unexpectedFilterFailuresReplaceUncommittedContentWithoutLeakingCauses() throws Exception {
        for (boolean wrapped : List.of(false, true)) {
            MockHttpServletRequest request = new MockHttpServletRequest("GET", "/secret-password");
            request.setQueryString("token=credentials");
            request.addHeader("X-Trace-Id", CLIENT_TRACE);
            MockHttpServletResponse response = new MockHttpServletResponse();
            new HttpProblemBoundaryFilter(problems).doFilter(request, response, (incoming, outgoing) -> {
                outgoing.getOutputStream().write(LEAK.getBytes(java.nio.charset.StandardCharsets.UTF_8));
                if (wrapped) {
                    throw new ServletException(LEAK, new SQLException(LEAK));
                }
                throw new IllegalStateException(LEAK, new SQLException(LEAK));
            });
            assertMockProblem(response, 500, "INTERNAL_ERROR");
            assertNull(request.getSession(false));
        }
    }

    private HttpResponse<String> send(String method, String path, String... headers) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create(origin + path))
                .timeout(Duration.ofSeconds(5)).method(method, HttpRequest.BodyPublishers.noBody());
        for (int i = 0; i < headers.length; i += 2) {
            request.header(headers[i], headers[i + 1]);
        }
        return client.send(request.build(), HttpResponse.BodyHandlers.ofString());
    }

    private Map<String, Object> assertProblem(HttpResponse<String> response, int status, String code) {
        assertEquals(status, response.statusCode());
        assertTrue(response.headers().firstValue("Content-Type").orElse("")
                .startsWith(MediaType.APPLICATION_PROBLEM_JSON_VALUE));
        assertClosedHeaders(response);
        return assertEnvelope(response.body(), status, code);
    }

    private void assertMockProblem(MockHttpServletResponse response, int status, String code) throws Exception {
        assertEquals(status, response.getStatus());
        assertNotNull(response.getContentType());
        assertTrue(response.getContentType().startsWith(MediaType.APPLICATION_PROBLEM_JSON_VALUE));
        assertEnvelope(response.getContentAsString(), status, code);
    }

    private Map<String, Object> assertEnvelope(String json, int status, String code) {
        Map<String, Object> body = object(json);
        assertEquals(Set.of("type", "title", "status", "detail", "instance", "code", "traceId"), body.keySet());
        assertEquals(status, ((Number) body.get("status")).intValue());
        assertEquals(HttpStatus.valueOf(status).getReasonPhrase(), body.get("title"));
        assertEquals(code, body.get("code"));
        assertEquals("urn:authority-pro:problem:" + code.toLowerCase(java.util.Locale.ROOT).replace('_', '-'),
                body.get("type"));
        assertEquals(switch (code) {
            case "ACCESS_DENIED" -> "The request is not permitted.";
            case "CSRF_REJECTED" -> "The request could not be verified.";
            case "BAD_REQUEST" -> "The request could not be accepted.";
            case "VALIDATION_FAILED" -> "The request did not pass validation.";
            case "INTERNAL_ERROR" -> "An unexpected error occurred.";
            default -> throw new AssertionError("Unrecognized test problem code");
        }, body.get("detail"));
        String trace = (String) body.get("traceId");
        String instance = (String) body.get("instance");
        assertTrue(trace.matches("[0-9a-f]{32}"));
        assertTrue(instance.matches("urn:uuid:[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}"));
        assertEquals(trace, instance.substring("urn:uuid:".length()).replace("-", ""));
        assertNotEquals(CLIENT_TRACE, trace);
        for (String forbidden : List.of("secret-password", "SELECT", "credentials", "SQLException",
                "IllegalStateException", "stackTrace", "/fixture", "/actuator", "?token", "attacker-session")) {
            assertFalse(json.contains(forbidden), forbidden);
        }
        return body;
    }

    private Map<String, Object> object(String json) {
        return mapper.readValue(json, OBJECT);
    }

    private void assertClosedHeaders(HttpResponse<String> response) {
        assertTrue(response.headers().allValues("WWW-Authenticate").isEmpty());
        assertTrue(response.headers().allValues("Location").isEmpty());
        assertTrue(response.headers().map().keySet().stream()
                .noneMatch(name -> name.toLowerCase(java.util.Locale.ROOT).startsWith("access-control-allow-")));
        for (String cookie : response.headers().allValues("Set-Cookie")) {
            assertTrue(cookie.startsWith("XSRF-TOKEN="), cookie);
            assertFalse(cookie.toUpperCase(java.util.Locale.ROOT).contains("JSESSIONID"));
        }
    }

    @RestController
    static final class FixtureController {
        @PostMapping("/fixture/validate")
        Map<String, String> validate(@Valid @RequestBody Payload payload) {
            return Map.of("value", payload.value());
        }

        @GetMapping("/fixture/parameter")
        String parameter(@RequestParam("count") @Min(value = 1, message = LEAK) int count) {
            return Integer.toString(count);
        }

        @GetMapping("/fixture/failure")
        String failure() {
            throw new IllegalStateException(LEAK, new SQLException(LEAK));
        }

        @GetMapping("/fixture/security")
        String security() {
            throw new AccessDeniedException(LEAK);
        }
    }

    record Payload(@NotBlank(message = LEAK) String value) { }
}
