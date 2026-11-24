package com.authoritypro.inventory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.authoritypro.platform.access.application.InventoryAccessPolicy;
import com.authoritypro.platform.access.application.InventoryStoreUnavailableException;
import com.authoritypro.platform.access.infrastructure.http.InventoryAuthorizationFilter;
import com.authoritypro.platform.access.infrastructure.http.InventoryProblems;
import jakarta.servlet.http.Cookie;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import tools.jackson.databind.json.JsonMapper;

class InventoryAuthorizationFilterTest {
    private static final JsonMapper MAPPER = JsonMapper.builder().build();
    private static final UUID USER = UUID.randomUUID();

    private final InventoryProblems problems = new InventoryProblems(MAPPER);

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    private MockHttpServletResponse run(InventoryAccessPolicy policy, boolean authenticated, boolean cookie)
            throws Exception {
        if (authenticated) {
            SecurityContextHolder.getContext().setAuthentication(
                    UsernamePasswordAuthenticationToken.authenticated(USER.toString(), null, List.of()));
        }
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/platform/inventory");
        if (cookie) request.setCookies(new Cookie(InventoryProblems.SESSION_COOKIE, "stale"));
        MockHttpServletResponse response = new MockHttpServletResponse();
        new InventoryAuthorizationFilter(policy, problems).doFilter(request, response, new MockFilterChain());
        return response;
    }

    @Test
    void refusesAnonymousRequestsWithoutTouchingTheCatalogue() throws Exception {
        MockHttpServletResponse response = run(userId -> InventoryAccessPolicy.Decision.GRANTED, false, false);
        assertEquals(401, response.getStatus());
        assertTrue(response.getContentAsString().contains("AUTHENTICATION_REQUIRED"));
        assertTrue(response.getHeader("Cache-Control").contains("no-store"));
        assertEquals("nosniff", response.getHeader("X-Content-Type-Options"));
    }

    @Test
    void distinguishesAnExpiredPresentedSession() throws Exception {
        MockHttpServletResponse response = run(userId -> InventoryAccessPolicy.Decision.GRANTED, false, true);
        assertEquals(401, response.getStatus());
        assertTrue(response.getContentAsString().contains("SESSION_EXPIRED"));
    }

    @Test
    void refusesDeniedUsersAndPassesGrantedOnes() throws Exception {
        MockHttpServletResponse denied = run(userId -> InventoryAccessPolicy.Decision.DENIED, true, false);
        assertEquals(403, denied.getStatus());
        assertTrue(denied.getContentAsString().contains("AUTHORIZATION_DENIED"));

        AtomicInteger calls = new AtomicInteger();
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/platform/inventory");
        MockHttpServletResponse granted = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(USER.toString(), null, List.of()));
        new InventoryAuthorizationFilter(userId -> {
            calls.incrementAndGet();
            assertEquals(USER, userId);
            return InventoryAccessPolicy.Decision.GRANTED;
        }, problems).doFilter(request, granted, chain);
        assertEquals(1, calls.get());
        assertNotNull(chain.getRequest());
        assertNull(granted.getHeader("X-Content-Type-Options"));
    }

    @Test
    void failsClosedWhenTheAuthorizationStoreIsUnavailable() throws Exception {
        MockHttpServletResponse response = run(userId -> {
            throw new InventoryStoreUnavailableException();
        }, true, false);
        assertEquals(503, response.getStatus());
        String body = response.getContentAsString();
        assertTrue(body.contains("INVENTORY_UNAVAILABLE"), body);
        assertTrue(body.contains("traceId"), body);
        assertTrue(response.getContentType().startsWith("application/problem+json"), response.getContentType());
    }

    @Test
    void malformedPrincipalIsNotAuthenticated() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated("not-a-uuid", null, List.of()));
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/platform/inventory");
        MockHttpServletResponse response = new MockHttpServletResponse();
        new InventoryAuthorizationFilter(userId -> InventoryAccessPolicy.Decision.GRANTED, problems)
                .doFilter(request, response, new MockFilterChain());
        assertEquals(401, response.getStatus());
        assertTrue(response.getContentAsString().contains("AUTHENTICATION_REQUIRED"));
    }
}
