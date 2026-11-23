package com.authoritypro.platform.identity.infrastructure.http;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;
import org.springframework.dao.DataAccessException;
import org.springframework.transaction.TransactionException;
import org.springframework.web.filter.OncePerRequestFilter;

/** Fail-closed translation of session-store failures on the authentication surface only. */
public final class SessionStoreFailureFilter extends OncePerRequestFilter {
    private static final String PREFIX = "/api/v1/auth/";

    private final AuthProblems problems;

    public SessionStoreFailureFilter(AuthProblems problems) {
        this.problems = problems;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith(PREFIX);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        try {
            chain.doFilter(request, response);
        } catch (ServletException | IOException | RuntimeException failure) {
            if (response.isCommitted() || !causedByStoreFailure(failure)) throw failure;
            problems.write(response, AuthProblems.Code.SESSION_STORE_UNAVAILABLE);
        }
    }

    private static boolean causedByStoreFailure(Throwable failure) {
        Throwable current = failure;
        for (int depth = 0; current != null && depth < 16; depth++) {
            if (current instanceof DataAccessException || current instanceof SQLException
                    || current instanceof TransactionException) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }
}
