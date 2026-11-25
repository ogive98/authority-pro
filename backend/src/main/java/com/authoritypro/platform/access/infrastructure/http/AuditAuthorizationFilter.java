package com.authoritypro.platform.access.infrastructure.http;

import com.authoritypro.platform.access.application.AuditAccessPolicy;
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
 * Runs inside the audit chain only. It validates the untrusted tenant/company selector against a
 * fresh membership before exposing it as request attributes; the controller never trusts a header.
 */
public final class AuditAuthorizationFilter extends OncePerRequestFilter {
    private final AuditAccessPolicy policy;
    private final AccessProblems problems;

    public AuditAuthorizationFilter(AuditAccessPolicy policy, AccessProblems problems) {
        this.policy = policy;
        this.problems = problems;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain chain) throws ServletException, IOException {
        UUID userId = userId();
        if (userId == null) {
            problems.write(response, AccessProblems.sessionPresented(request)
                    ? AccessProblems.Code.SESSION_EXPIRED
                    : AccessProblems.Code.AUTHENTICATION_REQUIRED);
            return;
        }
        if (!blank(request.getHeader(ScopeHeaders.SITE))) {
            problems.write(response, AccessProblems.Code.SCOPE_INVALID);
            return;
        }
        UUID tenantId = uuid(request.getHeader(ScopeHeaders.TENANT));
        if (tenantId == null) {
            problems.write(response, AccessProblems.Code.SCOPE_INVALID);
            return;
        }
        UUID companyId = null;
        if (!blank(request.getHeader(ScopeHeaders.COMPANY))) {
            companyId = uuid(request.getHeader(ScopeHeaders.COMPANY));
            if (companyId == null) {
                problems.write(response, AccessProblems.Code.SCOPE_INVALID);
                return;
            }
        }
        try {
            if (policy.decide(userId, tenantId, companyId) != AuditAccessPolicy.Decision.GRANTED) {
                problems.write(response, AccessProblems.Code.AUTHORIZATION_DENIED);
                return;
            }
        } catch (RuntimeException unavailable) {
            problems.write(response, AccessProblems.Code.AUDIT_UNAVAILABLE);
            return;
        }
        request.setAttribute(ScopeHeaders.TENANT, tenantId);
        if (companyId != null) request.setAttribute(ScopeHeaders.COMPANY, companyId);
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

    private static UUID uuid(String value) {
        if (blank(value)) return null;
        try {
            return UUID.fromString(value.trim());
        } catch (IllegalArgumentException malformed) {
            return null;
        }
    }

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }
}
