package com.authoritypro.platform.runtime.infrastructure.http;

import java.io.IOException;
import java.net.URI;
import java.util.UUID;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.csrf.CsrfException;
import org.springframework.security.web.firewall.RequestRejectedException;
import org.springframework.security.web.firewall.RequestRejectedHandler;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

/** One deliberately input-independent envelope for servlet and MVC failures. */
@Component
public final class HttpProblems implements AuthenticationEntryPoint, AccessDeniedHandler, RequestRejectedHandler {

    public enum Kind {
        ACCESS_DENIED("access-denied", "The request is not permitted."),
        CSRF_REJECTED("csrf-rejected", "The request could not be verified."),
        BAD_REQUEST("bad-request", "The request could not be accepted."),
        VALIDATION_FAILED("validation-failed", "The request did not pass validation."),
        REQUEST_REJECTED("request-rejected", "The request could not be completed."),
        INVENTORY_UNAVAILABLE("inventory-unavailable", "The module catalogue is unavailable."),
        INTERNAL_ERROR("internal-error", "An unexpected error occurred.");

        private final String type;
        private final String detail;

        Kind(String type, String detail) {
            this.type = type;
            this.detail = detail;
        }
    }

    private final JsonMapper mapper;

    public HttpProblems(JsonMapper mapper) {
        this.mapper = mapper;
    }

    public ProblemDetail create(HttpStatusCode status, Kind kind) {
        UUID occurrence = UUID.randomUUID();
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, kind.detail);
        HttpStatus knownStatus = HttpStatus.resolve(status.value());
        problem.setTitle(knownStatus == null ? "Request failed" : knownStatus.getReasonPhrase());
        problem.setType(URI.create("urn:authority-pro:problem:" + kind.type));
        // Neither occurrence identifiers nor diagnostics incorporate client-controlled data.
        problem.setInstance(URI.create("urn:uuid:" + occurrence));
        problem.setProperty("code", kind.name());
        problem.setProperty("traceId", occurrence.toString().replace("-", ""));
        return problem;
    }

    public void write(HttpServletResponse response, HttpStatusCode status, Kind kind) throws IOException {
        byte[] body = mapper.writeValueAsBytes(create(status, kind));
        response.resetBuffer();
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.setHeader("Cache-Control", "no-store");
        response.setHeader("X-Content-Type-Options", "nosniff");
        response.setContentLength(body.length);
        response.getOutputStream().write(body);
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
            AuthenticationException exception) throws IOException {
        write(response, HttpStatus.FORBIDDEN, Kind.ACCESS_DENIED);
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
            AccessDeniedException exception) throws IOException {
        write(response, HttpStatus.FORBIDDEN,
                exception instanceof CsrfException ? Kind.CSRF_REJECTED : Kind.ACCESS_DENIED);
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
            RequestRejectedException exception) throws IOException {
        write(response, HttpStatus.BAD_REQUEST, Kind.BAD_REQUEST);
    }
}
