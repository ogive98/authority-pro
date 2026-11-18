package com.authoritypro.platform.runtime.infrastructure.http;

import jakarta.validation.ConstraintViolationException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.csrf.CsrfException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public final class HttpProblemAdvice extends ResponseEntityExceptionHandler {

    private final HttpProblems problems;

    public HttpProblemAdvice(HttpProblems problems) {
        this.problems = problems;
    }

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception exception, Object body,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        HttpProblems.Kind kind;
        if (status.is5xxServerError()) {
            kind = HttpProblems.Kind.INTERNAL_ERROR;
        } else if (exception instanceof MethodArgumentNotValidException
                || exception instanceof HandlerMethodValidationException) {
            kind = HttpProblems.Kind.VALIDATION_FAILED;
        } else {
            kind = status.value() == 400 ? HttpProblems.Kind.BAD_REQUEST : HttpProblems.Kind.REQUEST_REJECTED;
        }
        // Discard framework/exception bodies and headers which can contain rejected input.
        return response(status, kind);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    ResponseEntity<Object> validationFailure(ConstraintViolationException exception) {
        return response(HttpStatus.BAD_REQUEST, HttpProblems.Kind.VALIDATION_FAILED);
    }

    @ExceptionHandler({AccessDeniedException.class, AuthenticationException.class})
    ResponseEntity<Object> securityFailure(Exception exception) {
        return response(HttpStatus.FORBIDDEN, exception instanceof CsrfException
                ? HttpProblems.Kind.CSRF_REJECTED : HttpProblems.Kind.ACCESS_DENIED);
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<Object> unexpectedFailure(Exception exception) {
        return response(HttpStatus.INTERNAL_SERVER_ERROR, HttpProblems.Kind.INTERNAL_ERROR);
    }

    private ResponseEntity<Object> response(HttpStatusCode status, HttpProblems.Kind kind) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PROBLEM_JSON);
        headers.setCacheControl("no-store");
        headers.set("X-Content-Type-Options", "nosniff");
        return new ResponseEntity<>(problems.create(status, kind), headers, status);
    }
}
