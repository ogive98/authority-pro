package com.authoritypro.http;

import com.authoritypro.platform.identity.application.AuthenticationService;
import com.authoritypro.platform.identity.application.CredentialReader;
import com.authoritypro.platform.identity.application.OwnProfile;
import com.authoritypro.platform.identity.application.OwnProfileReader;
import com.authoritypro.platform.identity.application.SessionLimiter;
import com.authoritypro.platform.identity.domain.LoginThrottle;
import com.authoritypro.platform.identity.domain.PasswordVerifier;import com.authoritypro.platform.identity.infrastructure.http.AuthController;
import com.authoritypro.platform.identity.infrastructure.http.AuthProblems;
import jakarta.servlet.http.Cookie;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.session.FindByIndexNameSessionRepository;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import tools.jackson.databind.json.JsonMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthControllerTest {
    private static final JsonMapper MAPPER = JsonMapper.builder().build();
    private static final String PASSWORD = "correct-password";
    private static final Instant NOW = Instant.parse("2026-11-23T10:00:00Z");
    private static final UUID USER = UUID.randomUUID();
    private static final UUID TENANT = UUID.randomUUID();
    private static final UUID COMPANY = UUID.randomUUID();

    private final PasswordVerifier verifier = (raw, encoded) ->
            encoded != null && PASSWORD.equals(new String(raw));
    private final HttpSessionSecurityContextRepository contextRepository =
            new HttpSessionSecurityContextRepository();
    private final CookieCsrfTokenRepository csrfRepository = csrfRepository();
    private final AuthProblems problems = new AuthProblems(MAPPER);

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    private static CookieCsrfTokenRepository csrfRepository() {
        CookieCsrfTokenRepository repository = CookieCsrfTokenRepository.withHttpOnlyFalse();
        repository.setCookiePath("/");
        repository.setCookieCustomizer(cookie -> cookie.httpOnly(false).sameSite("Lax"));
        return repository;
    }

    private MockMvc mvc() {
        CredentialReader credentials = login -> "operator".equals(login)
                ? Optional.of(new CredentialReader.Credential(USER, "Operator", true, "stored-hash", "bcrypt"))
                : Optional.empty();
        OwnProfileReader profiles = userId -> userId.equals(USER)
                ? Optional.of(new OwnProfile(USER, "Operator", "operator", List.of(
                    new OwnProfile.Membership(TENANT, "Tenant A", List.of("TENANT_OWNER"),
                        List.of(new OwnProfile.Company(COMPANY, "Company A"))))))
                : Optional.empty();
        SessionLimiter sessions = userId -> { };
        AuthController controller = new AuthController(
                new AuthenticationService(credentials, verifier, new LoginThrottle(),
                        Clock.fixed(NOW, ZoneOffset.UTC)),
                profiles, sessions, contextRepository, csrfRepository, problems,
                Clock.fixed(NOW, ZoneOffset.UTC));
        return MockMvcBuilders.standaloneSetup(controller)
                .setMessageConverters(new JacksonJsonHttpMessageConverter(MAPPER))
                .build();
    }

    private static String body(String login, String password) {
        return "{\"login\":\"" + login + "\",\"password\":\"" + password + "\"}";
    }

    @Test
    void csrfEndpointReturns204WithAReadableCookieAndNoSession() throws Exception {
        MvcResult result = mvc().perform(get("/api/v1/auth/csrf"))
                .andExpect(status().isNoContent()).andReturn();
        Cookie cookie = result.getResponse().getCookie("XSRF-TOKEN");
        assertNotNull(cookie);
        assertFalse(cookie.isHttpOnly());
        assertEquals("Lax", cookie.getAttribute("SameSite"));
        assertEquals("/", cookie.getPath());
        assertNull(result.getRequest().getSession(false));
    }

    @Test
    void loginRotatesTheSessionAndStoresOnlyTheInternalUser() throws Exception {
        MockHttpSession session = new MockHttpSession();
        String before = session.getId();
        mvc().perform(post("/api/v1/auth/login").session(session)
                        .contentType(MediaType.APPLICATION_JSON).content(body("Operator", PASSWORD)))
                .andExpect(status().isNoContent());
        assertNotEquals(before, session.getId());
        SecurityContext saved = (SecurityContext) session.getAttribute(
                HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY);
        assertNotNull(saved);
        assertEquals(USER.toString(), saved.getAuthentication().getName());
        assertEquals(USER.toString(),
                session.getAttribute(FindByIndexNameSessionRepository.PRINCIPAL_NAME_INDEX_NAME));
    }

    @Test
    void invalidLoginIsAGeneric401AndCreatesNoSession() throws Exception {
        MvcResult result = mvc().perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON).content(body("operator", "wrong-password")))
                .andExpect(status().isUnauthorized()).andReturn();
        assertProblem(result, "AUTHENTICATION_REQUIRED");
        assertTrue(result.getResponse().getHeaders("Set-Cookie").stream()
                .noneMatch(value -> value.startsWith(AuthProblems.SESSION_COOKIE)));
        assertNull(result.getRequest().getSession(false));
    }

    @Test
    void repeatedFailuresAreThrottled() throws Exception {
        MockMvc mvc = mvc();
        for (int attempt = 0; attempt < LoginThrottle.MAX_FAILURES; attempt++) {
            mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                    .content(body("operator", "wrong-password"))).andExpect(status().isUnauthorized());
        }
        MvcResult throttled = mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(body("operator", PASSWORD)))
                .andExpect(status().isTooManyRequests()).andReturn();
        assertProblem(throttled, "AUTHENTICATION_THROTTLED");
    }

    @Test
    void ownProfileIsReturnedForTheAuthenticatedUserOnly() throws Exception {
        authenticate();
        MvcResult result = mvc().perform(get("/api/v1/auth/me").session(new MockHttpSession()))
                .andExpect(status().isOk()).andReturn();
        String json = result.getResponse().getContentAsString();
        assertTrue(json.contains("\"displayName\":\"Operator\""), json);
        assertTrue(json.contains("\"tenantName\":\"Tenant A\""), json);
        assertTrue(json.contains("\"roles\":[\"TENANT_OWNER\"]"), json);
        assertFalse(result.getResponse().getHeaders("Cache-Control").isEmpty());
    }

    @Test
    void ownProfileRequiresAuthentication() throws Exception {
        MvcResult result = mvc().perform(get("/api/v1/auth/me"))
                .andExpect(status().isUnauthorized()).andReturn();
        assertProblem(result, "AUTHENTICATION_REQUIRED");
    }

    @Test
    void logoutInvalidatesTheSession() throws Exception {
        authenticate();
        MockHttpSession session = new MockHttpSession();
        mvc().perform(post("/api/v1/auth/logout").session(session)).andExpect(status().isNoContent());
        assertTrue(session.isInvalid());
    }

    private static void authenticate() {
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(USER.toString(), null, List.of()));
        SecurityContextHolder.setContext(context);
    }

    private static void assertProblem(MvcResult result, String code) throws Exception {
        assertTrue(result.getResponse().getContentType().startsWith(MediaType.APPLICATION_PROBLEM_JSON_VALUE));
        String body = result.getResponse().getContentAsString();
        assertTrue(body.contains(code), body);
        assertTrue(body.contains("traceId"), body);
    }
}
