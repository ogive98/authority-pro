package com.authoritypro.platform.runtime.infrastructure.http;

import io.micrometer.core.instrument.MeterRegistry;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Bounded, non-authoritative request correlation. The identifier is transport metadata: it never
 * grants access, replaces the server trace identifier, or carries user or tenant data.
 */
public final class RequestCorrelationFilter extends OncePerRequestFilter {
    public static final String HEADER = "X-Correlation-Id";
    public static final String MDC_KEY = "correlationId";

    private static final Pattern ACCEPTED = Pattern.compile("[0-9a-fA-F]{32}");
    private static final Set<String> METHODS = Set.of("GET", "HEAD", "POST", "PUT", "PATCH", "DELETE",
            "OPTIONS", "TRACE", "CONNECT");
    private static final Logger ACCESS =
            LoggerFactory.getLogger("com.authoritypro.platform.runtime.http.access");
    private static final int MAX_PATH = 256;

    private final MeterRegistry registry;

    public RequestCorrelationFilter(MeterRegistry registry) {
        this.registry = registry;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain chain) throws ServletException, IOException {
        long started = System.nanoTime();
        String correlation = correlation(request.getHeader(HEADER));
        String previous = MDC.get(MDC_KEY);
        MDC.put(MDC_KEY, correlation);
        response.setHeader(HEADER, correlation);
        try {
            chain.doFilter(request, response);
        } finally {
            int status = response.getStatus();
            String method = method(request.getMethod());
            registry.counter("authority.http.requests", "method", method, "status", statusFamily(status))
                    .increment();
            String path = path(request.getRequestURI());
            if (!path.startsWith("/actuator/")) {
                ACCESS.atInfo()
                        .addKeyValue("http.method", method)
                        .addKeyValue("http.path", path)
                        .addKeyValue("http.status", status)
                        .addKeyValue("duration.ms", (System.nanoTime() - started) / 1_000_000L)
                        .log("request");
            }
            if (previous == null) {
                MDC.remove(MDC_KEY);
            } else {
                MDC.put(MDC_KEY, previous);
            }
        }
    }

    static String correlation(String candidate) {
        if (candidate != null && ACCEPTED.matcher(candidate).matches()) {
            return candidate.toLowerCase(Locale.ROOT);
        }
        return UUID.randomUUID().toString().replace("-", "");
    }

    static String method(String value) {
        return value != null && METHODS.contains(value) ? value : "OTHER";
    }

    static String statusFamily(int status) {
        return status >= 100 && status < 600 ? (status / 100) + "xx" : "OTHER";
    }

    static String path(String uri) {
        if (uri == null) return "";
        StringBuilder printable = new StringBuilder(Math.min(uri.length(), MAX_PATH));
        for (int index = 0; index < uri.length() && printable.length() < MAX_PATH; index++) {
            char value = uri.charAt(index);
            printable.append(value >= 0x20 && value <= 0x7e ? value : '?');
        }
        return printable.toString();
    }
}
