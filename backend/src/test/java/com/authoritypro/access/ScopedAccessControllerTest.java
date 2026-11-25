package com.authoritypro.access;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.authoritypro.platform.access.application.AccessStoreUnavailableException;
import com.authoritypro.platform.access.application.ScopedAccess;
import com.authoritypro.platform.access.application.ScopedAccessReader;
import com.authoritypro.platform.access.domain.Permission;
import com.authoritypro.platform.access.domain.RoleCode;
import com.authoritypro.platform.access.infrastructure.http.AccessProblems;
import com.authoritypro.platform.access.infrastructure.http.ScopeHeaders;
import com.authoritypro.platform.access.infrastructure.http.ScopedAccessController;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import tools.jackson.databind.json.JsonMapper;

class ScopedAccessControllerTest {
    private static final JsonMapper MAPPER = JsonMapper.builder().build();
    private static final UUID USER = UUID.randomUUID();
    private static final UUID TENANT = UUID.randomUUID();
    private static final UUID COMPANY = UUID.randomUUID();

    private final AtomicReference<UUID> seenTenant = new AtomicReference<>();
    private final AtomicReference<UUID> seenCompany = new AtomicReference<>();
    private Optional<ScopedAccess> result = Optional.of(access());
    private RuntimeException failure;

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    private static ScopedAccess access() {
        return new ScopedAccess(TENANT, "Tenant A", COMPANY, "Company A", 4,
                EnumSet.of(RoleCode.TENANT_AUDITOR, RoleCode.TENANT_OWNER),
                EnumSet.of(Permission.AUDIT_READ_TENANT, Permission.SECURITY_ATTEMPTS_READ));
    }

    private MockMvc mvc() {
        ScopedAccessReader reader = (userId, tenantId, companyId) -> {
            assertEquals(USER, userId);
            seenTenant.set(tenantId);
            seenCompany.set(companyId);
            if (failure != null) throw failure;
            return result;
        };
        ScopedAccessController controller = new ScopedAccessController(reader, new AccessProblems(MAPPER));
        return MockMvcBuilders.standaloneSetup(controller)
                .setMessageConverters(new JacksonJsonHttpMessageConverter(MAPPER))
                .build();
    }

    private static void authenticate() {
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(USER.toString(), null, List.of()));
    }

    @Test
    void returnsOnlyTheValidatedScopeProjection() throws Exception {
        authenticate();
        MvcResult response = mvc().perform(get("/api/v1/access/bootstrap")
                        .header(ScopeHeaders.TENANT, TENANT.toString())
                        .header(ScopeHeaders.COMPANY, COMPANY.toString()))
                .andExpect(status().isOk()).andReturn();
        String json = response.getResponse().getContentAsString();
        assertTrue(json.contains("\"tenantName\":\"Tenant A\""), json);
        assertTrue(json.contains("\"companyName\":\"Company A\""), json);
        assertTrue(json.contains("\"authorizationRevision\":4"), json);
        assertTrue(json.contains("\"roles\":[\"TENANT_AUDITOR\",\"TENANT_OWNER\"]"), json);
        assertTrue(json.contains("\"permissions\":[\"audit.read.tenant\",\"security.attempts.read\"]"), json);
        assertFalse(json.contains("\"login\""), json);
        assertFalse(json.contains("\"memberships\""), json);
        assertEquals(TENANT, seenTenant.get());
        assertEquals(COMPANY, seenCompany.get());
        assertTrue(response.getResponse().getHeader("Cache-Control").contains("no-store"));
        assertEquals("nosniff", response.getResponse().getHeader("X-Content-Type-Options"));
    }

    @Test
    void tenantOnlySelectionIsSupported() throws Exception {
        authenticate();
        result = Optional.of(new ScopedAccess(TENANT, "Tenant A", null, null, 2,
                EnumSet.of(RoleCode.TENANT_OWNER), EnumSet.of(Permission.AUDIT_READ_TENANT)));
        MvcResult response = mvc().perform(get("/api/v1/access/bootstrap")
                        .header(ScopeHeaders.TENANT, TENANT.toString()))
                .andExpect(status().isOk()).andReturn();
        assertTrue(response.getResponse().getContentAsString().contains("\"companyName\":null"));
        assertEquals(null, seenCompany.get());
    }

    @Test
    void missingMalformedOrUnsupportedScopesAreRefused() throws Exception {
        authenticate();
        for (var request : List.of(
                get("/api/v1/access/bootstrap"),
                get("/api/v1/access/bootstrap").header(ScopeHeaders.TENANT, "not-a-uuid"),
                get("/api/v1/access/bootstrap").header(ScopeHeaders.TENANT, TENANT.toString())
                        .header(ScopeHeaders.COMPANY, "not-a-uuid"),
                get("/api/v1/access/bootstrap").header(ScopeHeaders.TENANT, TENANT.toString())
                        .header(ScopeHeaders.SITE, UUID.randomUUID().toString()))) {
            MvcResult response = mvc().perform(request).andExpect(status().isBadRequest()).andReturn();
            assertProblem(response, "SCOPE_INVALID");
        }
    }

    @Test
    void unknownScopeOrEmptyRolesAreNonEnumeratingDenials() throws Exception {
        authenticate();
        result = Optional.empty();
        MvcResult unknown = mvc().perform(get("/api/v1/access/bootstrap")
                        .header(ScopeHeaders.TENANT, TENANT.toString()))
                .andExpect(status().isForbidden()).andReturn();
        assertProblem(unknown, "AUTHORIZATION_DENIED");

        result = Optional.of(new ScopedAccess(TENANT, "Tenant A", null, null, 1,
                java.util.Set.of(), java.util.Set.of()));
        MvcResult empty = mvc().perform(get("/api/v1/access/bootstrap")
                        .header(ScopeHeaders.TENANT, TENANT.toString()))
                .andExpect(status().isForbidden()).andReturn();
        assertProblem(empty, "AUTHORIZATION_DENIED");
    }

    @Test
    void unavailableStoreFailsClosed() throws Exception {
        authenticate();
        failure = new AccessStoreUnavailableException();
        MvcResult response = mvc().perform(get("/api/v1/access/bootstrap")
                        .header(ScopeHeaders.TENANT, TENANT.toString()))
                .andExpect(status().isServiceUnavailable()).andReturn();
        assertProblem(response, "ACCESS_UNAVAILABLE");
    }

    @Test
    void unauthenticatedCallIsRefusedWithoutReadingTheStore() throws Exception {
        MvcResult response = mvc().perform(get("/api/v1/access/bootstrap")
                        .header(ScopeHeaders.TENANT, TENANT.toString()))
                .andExpect(status().isUnauthorized()).andReturn();
        assertProblem(response, "AUTHENTICATION_REQUIRED");
        assertEquals(null, seenTenant.get());
    }

    private static void assertProblem(MvcResult result, String code) throws Exception {
        assertTrue(result.getResponse().getContentType().startsWith(MediaType.APPLICATION_PROBLEM_JSON_VALUE));
        String body = result.getResponse().getContentAsString();
        assertTrue(body.contains(code), body);
        assertTrue(body.contains("traceId"), body);
        assertTrue(result.getResponse().getHeader("Cache-Control").contains("no-store"));
    }
}
