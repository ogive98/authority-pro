package com.authoritypro.platform.access.infrastructure.http;

import com.authoritypro.platform.access.application.InventoryAccessPolicy;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Runs inside the inventory chain only. Missing authentication or permission is refused here so
 * that no request reaches the catalogue without a database-resolved {@code platform.inventory.read}.
 */
public final class InventoryAuthorizationFilter extends OncePerRequestFilter {
    private final InventoryAccessPolicy policy;
    private final InventoryProblems problems;

    public InventoryAuthorizationFilter(InventoryAccessPolicy policy, InventoryProblems problems) {
        this.policy = policy;
        this.problems = problems;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain chain) throws ServletException, IOException {
        UUID userId = userId();
        if (userId == null) {
            problems.write(response, InventoryProblems.sessionPresented(request)
                    ? InventoryProblems.Code.SESSION_EXPIRED
                    : InventoryProblems.Code.AUTHENTICATION_REQUIRED);
            return;
        }
        try {
            if (policy.decide(userId) != InventoryAccessPolicy.Decision.GRANTED) {
                problems.write(response, InventoryProblems.Code.AUTHORIZATION_DENIED);
                return;
            }
        } catch (RuntimeException unavailable) {
            problems.write(response, InventoryProblems.Code.INVENTORY_UNAVAILABLE);
            return;
        }
        chain.doFilter(request, response);
    }

    private static UUID userId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) return null;
        try {
            return UUID.fromString(authentication.getName());
        } catch (IllegalArgumentException malformed) {
            return null;
        }
    }
}
