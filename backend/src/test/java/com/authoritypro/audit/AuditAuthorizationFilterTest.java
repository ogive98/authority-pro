package com.authoritypro.audit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.authoritypro.platform.access.application.AccessStoreUnavailableException;
import com.authoritypro.platform.access.application.AuditAccessPolicy;
import com.authoritypro.platform.access.infrastructure.http.AccessProblems;
import com.authoritypro.platform.access.infrastructure.http.AuditAuthorizationFilter;
import com.authoritypro.platform.access.infrastructure.http.ScopeHeaders;
import jakarta.servlet.http.Cookie;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import tools.jackson.databind.json.JsonMapper;

class AuditAuthorizationFilterTest {
    private static final JsonMapper MAPPER = JsonMapper.builder().build();
    private static final UUID USER = UUID.randomUUID();
    private static final UUID TENANT = UUID.randomUUID();
    private static final UUID COMPANY = UUID.randomUUID();

    private final AccessProblems problems = new AccessProblems(MAPPER);

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    private static void authenticate() {
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(USER.toString(), null, List.of()));
    }

    private MockHttpServletResponse run(AuditAccessPolicy policy, boolean authenticated, boolean cookie,
            String tenant, String company, String site) throws Exception {
        if (authenticated) authenticate();
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/audit/tenant-records");
        if (tenant != null) request.addHeader(ScopeHeaders.TENANT, tenant);
        if (company != null) request.addHeader(ScopeHeaders.COMPANY, company);
        if (site != null) request.addHeader(ScopeHeaders.SITE, site);
        if (cookie) request.setCookies(new Cookie(AccessProblems.SESSION_COOKIE, "stale"));
        MockHttpServletResponse response = new MockHttpServletResponse();
        new AuditAuthorizationFilter(policy, problems).doFilter(request, response, new MockFilterChain());
        return response;
    }

    @Test
    void grantedScopeIsPassedAsValidatedAttributesOnly() throws Exception {
        AtomicReference<UUID> seenTenant = new AtomicReference<>();
        AtomicReference<UUID> seenCompany = new AtomicReference<>();
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/audit/tenant-records");
        request.addHeader(ScopeHeaders.TENANT, TENANT.toString());
        request.addHeader(ScopeHeaders.COMPANY, COMPANY.toString());
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();
        authenticate();
        new AuditAuthorizationFilter((userId, tenantId, companyId) -> {
            assertEquals(USER, userId);
            seenTenant.set(tenantId);
            seenCompany.set(companyId);
            return AuditAccessPolicy.Decision.GRANTED;
        }, problems).doFilter(request, response, chain);
        assertEquals(TENANT, seenTenant.get());
        assertEquals(COMPANY, seenCompany.get());
        assertEquals(TENANT, chain.getRequest().getAttribute(ScopeHeaders.TENANT));
        assertEquals(COMPANY, chain.getRequest().getAttribute(ScopeHeaders.COMPANY));
        assertNull(response.getHeader("X-Content-Type-Options"));
    }

    @Test
    void missingOrUnsupportedSelectorIsRefusedBeforeTheCatalogue() throws Exception {
        AtomicInteger calls = new AtomicInteger();
        AuditAccessPolicy policy = (userId, tenantId, companyId) -> {
            calls.incrementAndGet();
            return AuditAccessPolicy.Decision.GRANTED;
        };
        for (MockHttpServletResponse response : List.of(
                run(policy, true, false, null, null, null),
                run(policy, true, false, "not-a-uuid", null, null),
                run(policy, true, false, TENANT.toString(), "not-a-uuid", null),
                run(policy, true, false, TENANT.toString(), null, UUID.randomUUID().toString()))) {
            assertEquals(400, response.getStatus());
            assertTrue(response.getContentAsString().contains("SCOPE_INVALID"));
            assertTrue(response.getHeader("Cache-Control").contains("no-store"));
        }
        assertEquals(0, calls.get());
    }

    @Test
    void anonymousAndExpiredSessionsAreDistinguished() throws Exception {
        MockHttpServletResponse anonymous = run((u, t, c) -> AuditAccessPolicy.Decision.GRANTED,
                false, false, TENANT.toString(), null, null);
        assertEquals(401, anonymous.getStatus());
        assertTrue(anonymous.getContentAsString().contains("AUTHENTICATION_REQUIRED"));

        MockHttpServletResponse expired = run((u, t, c) -> AuditAccessPolicy.Decision.GRANTED,
                false, true, TENANT.toString(), null, null);
        assertEquals(401, expired.getStatus());
        assertTrue(expired.getContentAsString().contains("SESSION_EXPIRED"));
    }

    @Test
    void deniedScopeDoesNotReachTheController() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/audit/tenant-records");
        request.addHeader(ScopeHeaders.TENANT, TENANT.toString());
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();
        authenticate();
        new AuditAuthorizationFilter((u, t, c) -> AuditAccessPolicy.Decision.DENIED, problems)
                .doFilter(request, response, chain);
        assertEquals(403, response.getStatus());
        assertTrue(response.getContentAsString().contains("AUTHORIZATION_DENIED"));
        assertNull(chain.getRequest());
        assertNull(request.getAttribute(ScopeHeaders.TENANT));
    }

    @Test
    void unavailableStoreFailsClosedWithTheAuditCode() throws Exception {
        MockHttpServletResponse response = run((u, t, c) -> {
            throw new AccessStoreUnavailableException();
        }, true, false, TENANT.toString(), null, null);
        assertEquals(503, response.getStatus());
        String body = response.getContentAsString();
        assertTrue(body.contains("AUDIT_UNAVAILABLE"), body);
        assertTrue(body.contains("traceId"), body);
        assertTrue(response.getContentType().startsWith("application/problem+json"));
    }

    @Test
    void malformedPrincipalIsNotAuthenticated() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated("not-a-uuid", null, List.of()));
        MockHttpServletResponse response = run((u, t, c) -> AuditAccessPolicy.Decision.GRANTED,
                false, false, TENANT.toString(), null, null);
        assertEquals(401, response.getStatus());
        assertTrue(response.getContentAsString().contains("AUTHENTICATION_REQUIRED"));
    }

    @Test
    void tenantWideSelectionPassesANullCompany() throws Exception {
        AtomicReference<UUID> seenCompany = new AtomicReference<>(UUID.randomUUID());
        MockHttpServletResponse response = run((u, t, c) -> {
            seenCompany.set(c);
            return AuditAccessPolicy.Decision.GRANTED;
        }, true, false, TENANT.toString(), null, null);
        assertEquals(200, response.getStatus());
        assertNull(seenCompany.get());
    }
}
