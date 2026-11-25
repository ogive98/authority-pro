package com.authoritypro.http;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.authoritypro.platform.access.application.AuditAccessPolicy;
import com.authoritypro.platform.access.infrastructure.http.AccessProblems;
import com.authoritypro.platform.access.infrastructure.http.AuditAuthorizationFilter;
import com.authoritypro.platform.access.infrastructure.http.ScopeHeaders;
import com.authoritypro.platform.audit.application.AuditRecordReader;
import com.authoritypro.platform.audit.domain.AuditActorKind;
import com.authoritypro.platform.audit.domain.AuditRecordView;
import com.authoritypro.platform.audit.infrastructure.http.AuditController;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import tools.jackson.databind.json.JsonMapper;

/** Proves the access filter and the audit controller share one validated-scope contract. */
class AuditScopeContractTest {
    private static final JsonMapper MAPPER = JsonMapper.builder().build();
    private static final UUID USER = UUID.randomUUID();
    private static final UUID TENANT = UUID.randomUUID();
    private static final UUID COMPANY = UUID.randomUUID();

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void filterAttributesAreTheOnlyScopeTheControllerReads() throws Exception {
        AtomicReference<UUID> tenant = new AtomicReference<>();
        AtomicReference<UUID> company = new AtomicReference<>();
        AuditRecordReader reader = (userId, tenantId, companyId, query) -> {
            tenant.set(tenantId);
            company.set(companyId);
            return List.of(new AuditRecordView(UUID.randomUUID(), Instant.parse("2026-11-25T10:00:00Z"),
                    "tenancy.settings.updated", 1, USER, AuditActorKind.USER, "settings", "settings-1",
                    null, null, 1, "{\"changedKeys\":[\"displayName\"],\"fromRevision\":1,\"toRevision\":2}",
                    "committed", TENANT, COMPANY, null));
        };
        AccessProblems problems = new AccessProblems(MAPPER);
        AuditAuthorizationFilter filter = new AuditAuthorizationFilter(
                (userId, tenantId, companyId) -> AuditAccessPolicy.Decision.GRANTED, problems);
        MockMvc mvc = MockMvcBuilders.standaloneSetup(new AuditController(reader, MAPPER))
                .addFilters(filter)
                .setMessageConverters(new JacksonJsonHttpMessageConverter(MAPPER))
                .build();
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(USER.toString(), null, List.of()));
        mvc.perform(get("/api/v1/audit/tenant-records")
                        .header(ScopeHeaders.TENANT, TENANT.toString())
                        .header(ScopeHeaders.COMPANY, COMPANY.toString()))
                .andExpect(status().isOk());
        assertEquals(TENANT, tenant.get());
        assertEquals(COMPANY, company.get());
    }
}
