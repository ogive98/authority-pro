package com.authoritypro.platform.audit.infrastructure.http;

import java.net.URI;
import java.util.UUID;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;

/** Bounded RFC 9457 errors for the audit read surface; no query or SQL detail is exposed. */
final class AuditHttpProblems {

    enum Code {
        AUTHENTICATION_REQUIRED(HttpStatus.UNAUTHORIZED, "authentication-required", "Authentication is required."),
        AUDIT_QUERY_INVALID(HttpStatus.BAD_REQUEST, "audit-query-invalid", "The audit query is invalid."),
        SCOPE_INVALID(HttpStatus.BAD_REQUEST, "scope-invalid", "The selected scope is invalid."),
        AUDIT_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "audit-unavailable",
                "The audit service is unavailable.");

        private final HttpStatus status;
        private final String type;
        private final String detail;

        Code(HttpStatus status, String type, String detail) {
            this.status = status;
            this.type = type;
            this.detail = detail;
        }
    }

    private AuditHttpProblems() { }

    static ResponseEntity<ProblemDetail> response(Code code) {
        UUID occurrence = UUID.randomUUID();
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(code.status, code.detail);
        problem.setTitle(code.status.getReasonPhrase());
        problem.setType(URI.create("urn:authority-pro:problem:" + code.type));
        problem.setInstance(URI.create("urn:uuid:" + occurrence));
        problem.setProperty("code", code.name());
        problem.setProperty("traceId", occurrence.toString().replace("-", ""));
        return ResponseEntity.status(code.status)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .cacheControl(CacheControl.noStore())
                .header("X-Content-Type-Options", "nosniff")
                .body(problem);
    }
}
