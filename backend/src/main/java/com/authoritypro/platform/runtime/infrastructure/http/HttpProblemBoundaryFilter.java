package com.authoritypro.platform.runtime.infrastructure.http;

import java.io.IOException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.firewall.RequestRejectedException;
import org.springframework.web.filter.OncePerRequestFilter;

/** Installed only in the security chain, never also registered as a servlet filter. */
public final class HttpProblemBoundaryFilter extends OncePerRequestFilter {

    private final HttpProblems problems;

    public HttpProblemBoundaryFilter(HttpProblems problems) {
        this.problems = problems;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain chain) throws ServletException, IOException {
        try {
            chain.doFilter(request, response);
        } catch (ServletException | RuntimeException exception) {
            // A committed response cannot be replaced with an honest error envelope.
            if (response.isCommitted()) {
                throw exception;
            }
            if (exception instanceof RequestRejectedException rejected) {
                problems.handle(request, response, rejected);
            } else if (exception instanceof AccessDeniedException denied) {
                problems.handle(request, response, denied);
            } else if (exception instanceof AuthenticationException unauthenticated) {
                problems.commence(request, response, unauthenticated);
            } else {
                problems.write(response, HttpStatus.INTERNAL_SERVER_ERROR, HttpProblems.Kind.INTERNAL_ERROR);
            }
        }
    }
}
