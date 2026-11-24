package com.authoritypro.platform.access.infrastructure.http;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import tools.jackson.databind.json.JsonMapper;

/** Input-independent RFC 9457 envelope for the protected inventory boundary. */
public final class InventoryProblems implements AuthenticationEntryPoint, AccessDeniedHandler {
    public static final String SESSION_COOKIE = "AUTHORITY_PRO_SESSION";

    public enum Code {
        AUTHENTICATION_REQUIRED(HttpStatus.UNAUTHORIZED, "authentication-required", "Authentication is required."),
        SESSION_EXPIRED(HttpStatus.UNAUTHORIZED, "session-expired", "The session is no longer valid."),
        AUTHORIZATION_DENIED(HttpStatus.FORBIDDEN, "authorization-denied", "The request is not permitted."),
        INVENTORY_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "inventory-unavailable",
                "The authorization service is unavailable.");

        private final HttpStatus status;
        private final String type;
        private final String detail;

        Code(HttpStatus status, String type, String detail) {
            this.status = status;
            this.type = type;
            this.detail = detail;
        }
    }

    private final JsonMapper mapper;

    public InventoryProblems(JsonMapper mapper) {
        this.mapper = mapper;
    }

    public ProblemDetail create(Code code) {
        UUID occurrence = UUID.randomUUID();
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(code.status, code.detail);
        problem.setTitle(code.status.getReasonPhrase());
        problem.setType(URI.create("urn:authority-pro:problem:" + code.type));
        problem.setInstance(URI.create("urn:uuid:" + occurrence));
        problem.setProperty("code", code.name());
        problem.setProperty("traceId", occurrence.toString().replace("-", ""));
        return problem;
    }

    public ResponseEntity<ProblemDetail> response(Code code) {
        return ResponseEntity.status(code.status)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .cacheControl(CacheControl.noStore())
                .header("X-Content-Type-Options", "nosniff")
                .body(create(code));
    }

    public void write(HttpServletResponse response, Code code) throws IOException {
        byte[] body = mapper.writeValueAsBytes(create(code));
        response.resetBuffer();
        response.setStatus(code.status.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.setHeader("Cache-Control", "no-store");
        response.setHeader("X-Content-Type-Options", "nosniff");
        response.setContentLength(body.length);
        response.getOutputStream().write(body);
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
            AuthenticationException exception) throws IOException {
        write(response, sessionPresented(request) ? Code.SESSION_EXPIRED : Code.AUTHENTICATION_REQUIRED);
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
            AccessDeniedException exception) throws IOException {
        write(response, Code.AUTHORIZATION_DENIED);
    }

    static boolean sessionPresented(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (SESSION_COOKIE.equals(cookie.getName())) return true;
            }
        }
        String header = request.getHeader("Cookie");
        return header != null && header.contains(SESSION_COOKIE + "=");
    }
}
