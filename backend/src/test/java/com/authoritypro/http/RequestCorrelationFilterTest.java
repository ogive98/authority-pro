package com.authoritypro.http;

import com.authoritypro.platform.runtime.infrastructure.http.RequestCorrelationFilter;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import jakarta.servlet.http.HttpServletResponse;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RequestCorrelationFilterTest {
    private final SimpleMeterRegistry registry = new SimpleMeterRegistry();
    private final RequestCorrelationFilter filter = new RequestCorrelationFilter(registry);

    @Test
    void generatesA32HexCorrelationWhenAbsent() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/auth/me");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<String> mdcInside = new AtomicReference<>();
        filter.doFilter(request, response, (incoming, outgoing) -> {
            mdcInside.set(MDC.get(RequestCorrelationFilter.MDC_KEY));
            assertEquals(response.getHeader(RequestCorrelationFilter.HEADER), mdcInside.get());
            ((HttpServletResponse) outgoing).setStatus(401);
        });
        String correlation = response.getHeader(RequestCorrelationFilter.HEADER);
        assertNotNull(correlation);
        assertTrue(correlation.matches("[0-9a-f]{32}"), correlation);
        assertEquals(correlation, mdcInside.get());
        assertNull(MDC.get(RequestCorrelationFilter.MDC_KEY));
        assertEquals(1.0, registry.get("authority.http.requests")
                .tags("method", "GET", "status", "4xx").counter().count());
    }

    @Test
    void acceptsAndNormalizesAClientCorrelation() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/auth/login");
        request.addHeader(RequestCorrelationFilter.HEADER, "0123456789ABCDEF0123456789ABCDEF");
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response,
                (incoming, outgoing) -> ((HttpServletResponse) outgoing).setStatus(204));
        assertEquals("0123456789abcdef0123456789abcdef",
                response.getHeader(RequestCorrelationFilter.HEADER));
        assertNull(MDC.get(RequestCorrelationFilter.MDC_KEY));
    }

    @Test
    void replacesEveryValueOutsideTheBoundedAlphabet() throws Exception {
        for (String candidate : new String[] {"short", "../../etc/passwd", "0".repeat(33),
                "0123456789abcdef0123456789abcdeZ"}) {
            MockHttpServletRequest request = new MockHttpServletRequest("GET", "/");
            request.addHeader(RequestCorrelationFilter.HEADER, candidate);
            MockHttpServletResponse response = new MockHttpServletResponse();
            filter.doFilter(request, response,
                    (incoming, outgoing) -> ((HttpServletResponse) outgoing).setStatus(403));
            String correlation = response.getHeader(RequestCorrelationFilter.HEADER);
            assertTrue(correlation.matches("[0-9a-f]{32}"), candidate + " -> " + correlation);
            assertTrue(!correlation.equals(candidate), candidate);
        }
    }

    @Test
    void keepsMetricLabelsBounded() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("PROPFIND", "/api/v1/access/bootstrap");
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response,
                (incoming, outgoing) -> ((HttpServletResponse) outgoing).setStatus(503));
        assertEquals(1.0, registry.get("authority.http.requests")
                .tags("method", "OTHER", "status", "5xx").counter().count());
    }

    @Test
    void restoresAPreExistingCorrelationAfterTheRequest() throws Exception {
        MDC.put(RequestCorrelationFilter.MDC_KEY, "outer");
        try {
            MockHttpServletRequest request = new MockHttpServletRequest("GET", "/");
            MockHttpServletResponse response = new MockHttpServletResponse();
            filter.doFilter(request, response, (incoming, outgoing) -> { });
            assertEquals("outer", MDC.get(RequestCorrelationFilter.MDC_KEY));
        } finally {
            MDC.remove(RequestCorrelationFilter.MDC_KEY);
        }
    }
}
