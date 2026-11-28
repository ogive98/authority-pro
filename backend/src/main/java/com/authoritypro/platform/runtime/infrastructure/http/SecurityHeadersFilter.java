package com.authoritypro.platform.runtime.infrastructure.http;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Applies response hardening to every path, including requests rejected before the
 * security chains. HSTS stays out of this filter: it belongs to the production HTTPS
 * profile, where the scheme is actually trusted.
 */
final class SecurityHeadersFilter extends OncePerRequestFilter {

    static final String CONTENT_SECURITY_POLICY = "frame-ancestors 'none'";
    static final String PERMISSIONS_POLICY =
            "camera=(), geolocation=(), microphone=(), payment=(), usb=()";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain chain) throws ServletException, IOException {
        response.setHeader("X-Frame-Options", "DENY");
        response.setHeader("Content-Security-Policy", CONTENT_SECURITY_POLICY);
        response.setHeader("X-Content-Type-Options", "nosniff");
        response.setHeader("Referrer-Policy", "no-referrer");
        response.setHeader("Permissions-Policy", PERMISSIONS_POLICY);
        chain.doFilter(request, response);
    }
}
