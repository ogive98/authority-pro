package com.authoritypro.audit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.authoritypro.platform.audit.application.AuditReadUnavailableException;
import com.authoritypro.platform.audit.application.AuditRecordReader;
import com.authoritypro.platform.audit.domain.AuditActorKind;
import com.authoritypro.platform.audit.domain.AuditQuery;
import com.authoritypro.platform.audit.domain.AuditRecordView;
import com.authoritypro.platform.audit.infrastructure.http.AuditController;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import tools.jackson.databind.json.JsonMapper;

class AuditControllerTest {
    private static final JsonMapper MAPPER = JsonMapper.builder().build();
    private static final UUID USER = UUID.randomUUID();
    private static final UUID TENANT = UUID.randomUUID();
    private static final UUID COMPANY = UUID.randomUUID();
    private static final UUID RECORD = UUID.randomUUID();
    private static final Instant OCCURRED = Instant.parse("2026-11-25T10:00:00Z");

    private final List<AuditQuery> queries = new ArrayList<>();
    private boolean empty;
    private RuntimeException failure;

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    private static AuditRecordView view() {
        return new AuditRecordView(RECORD, OCCURRED, "tenancy.settings.updated", 1, USER,
                AuditActorKind.USER, "settings", "settings-1", null, null, 1,
                "{\"changedKeys\":[\"displayName\"],\"fromRevision\":1,\"toRevision\":2}",
                "committed", TENANT, COMPANY, null);
    }

    private MockMvc mvc() {
        AuditRecordReader reader = (userId, tenantId, companyId, query) -> {
            assertEquals(USER, userId);
            assertEquals(TENANT, tenantId);
            queries.add(query);
            if (failure != null) throw failure;
            return empty ? List.of() : List.of(view());
        };
        AuditController controller = new AuditController(reader, MAPPER);
        return MockMvcBuilders.standaloneSetup(controller)
                .setMessageConverters(new JacksonJsonHttpMessageConverter(MAPPER))
                .build();
    }

    private static void authenticate() {
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(USER.toString(), null, List.of()));
    }

    private static MockHttpServletRequestBuilder scopedGet() {
        return get("/api/v1/audit/tenant-records")
                .requestAttr(AuditController.TENANT_ATTRIBUTE, TENANT)
                .requestAttr(AuditController.COMPANY_ATTRIBUTE, COMPANY);
    }

    @Test
    void returnsABoundedPageWithOnlyValidatedScopeFields() throws Exception {
        authenticate();
        MvcResult result = mvc().perform(scopedGet()).andExpect(status().isOk()).andReturn();
        String json = result.getResponse().getContentAsString();
        assertTrue(json.contains("\"recordId\":\"" + RECORD + "\""), json);
        assertTrue(json.contains("\"actionCode\":\"tenancy.settings.updated\""), json);
        assertTrue(json.contains("\"metadata\":{"), json);
        assertTrue(json.contains("\"changedKeys\":[\"displayName\"]"), json);
        assertTrue(json.contains("\"nextRecordId\":\"" + RECORD + "\""), json);
        assertTrue(json.contains("\"nextOccurredAt\":\"" + OCCURRED + "\""), json);
        assertFalse(json.contains("\"password\""), json);
        assertEquals(1, queries.size());
        assertEquals(COMPANY, queries.getFirst().companyId());
        assertEquals(50, queries.getFirst().pageSize());
        assertTrue(result.getResponse().getHeader("Cache-Control").contains("no-store"));
        assertEquals("nosniff", result.getResponse().getHeader("X-Content-Type-Options"));
    }

    @Test
    void emptyPageHasNoFabricatedCursor() throws Exception {
        authenticate();
        empty = true;
        MvcResult result = mvc().perform(scopedGet()).andExpect(status().isOk()).andReturn();
        String json = result.getResponse().getContentAsString();
        assertTrue(json.contains("\"items\":[]"), json);
        assertTrue(json.contains("\"nextOccurredAt\":null"), json);
        assertTrue(json.contains("\"nextRecordId\":null"), json);
    }

    @Test
    void buildsTheQueryFromBoundedFiltersAndCursor() throws Exception {
        authenticate();
        mvc().perform(scopedGet()
                        .param("limit", "100")
                        .param("action", "tenancy.settings.updated")
                        .param("targetType", "settings")
                        .param("targetId", "settings-1")
                        .param("actorId", USER.toString())
                        .param("from", "2026-11-01T00:00:00Z")
                        .param("to", "2026-11-30T23:59:59Z")
                        .param("beforeOccurredAt", "2026-11-25T09:00:00Z")
                        .param("beforeRecordId", UUID.randomUUID().toString()))
                .andExpect(status().isOk());
        AuditQuery query = queries.getFirst();
        assertEquals(100, query.pageSize());
        assertEquals("settings", query.targetType());
        assertEquals("settings-1", query.targetId());
        assertEquals(USER, query.actorId());
        assertEquals(Instant.parse("2026-11-01T00:00:00Z"), query.from());
        assertEquals(Instant.parse("2026-11-30T23:59:59Z"), query.to());
        assertTrue(query.cursorOccurredAt() != null && query.cursorRecordId() != null);
        assertEquals(COMPANY, query.companyId());
    }

    @Test
    void invalidQueriesAreAProblemWithoutSqlDetail() throws Exception {
        authenticate();
        for (MockHttpServletRequestBuilder request : List.of(
                scopedGet().param("limit", "0"),
                scopedGet().param("limit", "101"),
                scopedGet().param("limit", "not-a-number"),
                scopedGet().param("action", "unknown.action"),
                scopedGet().param("beforeOccurredAt", "2026-11-25T09:00:00Z"),
                scopedGet().param("beforeRecordId", UUID.randomUUID().toString()),
                scopedGet().param("targetType", "settings"),
                scopedGet().param("targetId", "settings-1"),
                scopedGet().param("actorId", "not-a-uuid"),
                scopedGet().param("from", "yesterday"))) {
            MvcResult result = mvc().perform(request).andExpect(status().isBadRequest()).andReturn();
            assertProblem(result, "AUDIT_QUERY_INVALID");
            assertFalse(result.getResponse().getContentAsString().contains("SELECT"), result.getResponse()
                    .getContentAsString());
        }
    }

    @Test
    void missingOrUnvalidatedScopeIsRefused() throws Exception {
        authenticate();
        MvcResult missing = mvc().perform(get("/api/v1/audit/tenant-records"))
                .andExpect(status().isBadRequest()).andReturn();
        assertProblem(missing, "SCOPE_INVALID");

        MvcResult companyHeaderOnly = mvc().perform(get("/api/v1/audit/tenant-records")
                        .requestAttr(AuditController.TENANT_ATTRIBUTE, TENANT)
                        .header(AuditController.COMPANY_ATTRIBUTE, COMPANY.toString()))
                .andExpect(status().isBadRequest()).andReturn();
        assertProblem(companyHeaderOnly, "SCOPE_INVALID");

        MvcResult site = mvc().perform(scopedGet().header(AuditController.SITE_ATTRIBUTE, UUID.randomUUID().toString()))
                .andExpect(status().isBadRequest()).andReturn();
        assertProblem(site, "SCOPE_INVALID");
    }

    @Test
    void unavailableStoreFailsClosed() throws Exception {
        authenticate();
        failure = new AuditReadUnavailableException();
        MvcResult result = mvc().perform(scopedGet()).andExpect(status().isServiceUnavailable()).andReturn();
        assertProblem(result, "AUDIT_UNAVAILABLE");
    }

    @Test
    void unauthenticatedControllerCallIsRefused() throws Exception {
        MvcResult result = mvc().perform(scopedGet()).andExpect(status().isUnauthorized()).andReturn();
        assertProblem(result, "AUTHENTICATION_REQUIRED");
    }

    private static void assertProblem(MvcResult result, String code) throws Exception {
        assertTrue(result.getResponse().getContentType().startsWith(MediaType.APPLICATION_PROBLEM_JSON_VALUE));
        String body = result.getResponse().getContentAsString();
        assertTrue(body.contains(code), body);
        assertTrue(body.contains("traceId"), body);
        assertTrue(result.getResponse().getHeader("Cache-Control").contains("no-store"));
    }
}
