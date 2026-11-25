package com.authoritypro.platform.audit.infrastructure.http;

import com.authoritypro.platform.audit.application.AuditReadUnavailableException;
import com.authoritypro.platform.audit.application.AuditRecordReader;
import com.authoritypro.platform.audit.domain.AuditAction;
import com.authoritypro.platform.audit.domain.AuditQuery;
import com.authoritypro.platform.audit.domain.AuditRecordView;
import com.authoritypro.platform.audit.domain.AuditRejectedException;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import javax.sql.DataSource;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Tenant audit read. Scope authorization is performed by the access filter before this controller;
 * the controller consumes only the validated request attributes, never the raw headers.
 */
@ConditionalOnBean(DataSource.class)
@RestController
@RequestMapping("/api/v1/audit")
public final class AuditController {
    public static final String TENANT_ATTRIBUTE = "X-Authority-Tenant";
    public static final String COMPANY_ATTRIBUTE = "X-Authority-Company";
    public static final String SITE_ATTRIBUTE = "X-Authority-Site";
    private static final int DEFAULT_PAGE_SIZE = 50;

    private final AuditRecordReader reader;
    private final JsonMapper mapper;

    public AuditController(AuditRecordReader reader, JsonMapper mapper) {
        this.reader = reader;
        this.mapper = mapper;
    }

    @GetMapping("/tenant-records")
    public ResponseEntity<?> tenantRecords(HttpServletRequest request) {
        UUID callerId = principal();
        if (callerId == null) {
            return AuditHttpProblems.response(AuditHttpProblems.Code.AUTHENTICATION_REQUIRED);
        }
        UUID tenantId = attribute(request, TENANT_ATTRIBUTE);
        if (tenantId == null) return AuditHttpProblems.response(AuditHttpProblems.Code.SCOPE_INVALID);
        UUID companyId = attribute(request, COMPANY_ATTRIBUTE);
        if (companyId == null && !blank(request.getHeader(COMPANY_ATTRIBUTE))) {
            return AuditHttpProblems.response(AuditHttpProblems.Code.SCOPE_INVALID);
        }
        if (!blank(request.getHeader(SITE_ATTRIBUTE))) {
            return AuditHttpProblems.response(AuditHttpProblems.Code.SCOPE_INVALID);
        }

        AuditQuery query;
        try {
            query = query(request, companyId);
        } catch (InvalidQuery invalid) {
            return AuditHttpProblems.response(AuditHttpProblems.Code.AUDIT_QUERY_INVALID);
        }
        List<AuditRecordView> records;
        try {
            records = reader.read(callerId, tenantId, companyId, query);
        } catch (AuditReadUnavailableException unavailable) {
            return AuditHttpProblems.response(AuditHttpProblems.Code.AUDIT_UNAVAILABLE);
        } catch (AuditRejectedException rejected) {
            return AuditHttpProblems.response(AuditHttpProblems.Code.AUDIT_QUERY_INVALID);
        } catch (RuntimeException failure) {
            return AuditHttpProblems.response(AuditHttpProblems.Code.AUDIT_UNAVAILABLE);
        }
        try {
            return page(records);
        } catch (RuntimeException malformed) {
            return AuditHttpProblems.response(AuditHttpProblems.Code.AUDIT_UNAVAILABLE);
        }
    }

    private ResponseEntity<AuditPage> page(List<AuditRecordView> records) {
        List<AuditItem> items = records.stream().map(this::item).toList();
        if (items.isEmpty()) {
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .cacheControl(CacheControl.noStore())
                    .header("X-Content-Type-Options", "nosniff")
                    .body(new AuditPage(List.of(), null, null));
        }
        AuditRecordView last = records.getLast();
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .cacheControl(CacheControl.noStore())
                .header("X-Content-Type-Options", "nosniff")
                .body(new AuditPage(items, last.occurredAt(), last.recordId()));
    }

    private AuditItem item(AuditRecordView view) {
        JsonNode metadata;
        try {
            metadata = mapper.readTree(view.metadata());
        } catch (RuntimeException malformed) {
            throw new IllegalStateException("AUDIT_METADATA_INVALID");
        }
        return new AuditItem(view.recordId(), view.occurredAt(), view.actionCode(), view.actionVersion(),
                view.actorId(), view.actorKind().databaseValue(), view.targetType(), view.targetId(),
                view.operationId(), view.correlationId(), view.schemaVersion(), metadata, view.outcome(),
                view.tenantId(), view.companyId(), view.siteId());
    }

    private static AuditQuery query(HttpServletRequest request, UUID companyId) {
        int pageSize = DEFAULT_PAGE_SIZE;
        String limit = blankToNull(request.getParameter("limit"));
        if (limit != null) pageSize = integer(limit);
        String action = blankToNull(request.getParameter("action"));
        AuditAction parsedAction = null;
        if (action != null) {
            try {
                parsedAction = AuditAction.fromCode(action);
            } catch (AuditRejectedException invalid) {
                throw new InvalidQuery();
            }
        }
        String targetType = blankToNull(request.getParameter("targetType"));
        String targetId = blankToNull(request.getParameter("targetId"));
        UUID actorId = uuid(blankToNull(request.getParameter("actorId")));
        Instant from = instant(blankToNull(request.getParameter("from")));
        Instant to = instant(blankToNull(request.getParameter("to")));
        Instant cursorOccurredAt = instant(blankToNull(request.getParameter("beforeOccurredAt")));
        UUID cursorRecordId = uuid(blankToNull(request.getParameter("beforeRecordId")));
        try {
            return new AuditQuery(parsedAction, targetType, targetId, actorId, from, to, companyId,
                    cursorOccurredAt, cursorRecordId, pageSize);
        } catch (IllegalArgumentException invalid) {
            throw new InvalidQuery();
        }
    }

    private static int integer(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException invalid) {
            throw new InvalidQuery();
        }
    }

    private static UUID uuid(String value) {
        if (value == null) return null;
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException invalid) {
            throw new InvalidQuery();
        }
    }

    private static Instant instant(String value) {
        if (value == null) return null;
        try {
            return Instant.parse(value);
        } catch (RuntimeException invalid) {
            throw new InvalidQuery();
        }
    }

    private static UUID attribute(HttpServletRequest request, String name) {
        Object value = request.getAttribute(name);
        if (value instanceof UUID uuid) return uuid;
        if (value instanceof String text) {
            try {
                return UUID.fromString(text);
            } catch (IllegalArgumentException invalid) {
                return null;
            }
        }
        return null;
    }

    private static UUID principal() {
        var authentication = org.springframework.security.core.context.SecurityContextHolder.getContext()
                .getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) return null;
        try {
            return UUID.fromString(authentication.getName());
        } catch (IllegalArgumentException malformed) {
            return null;
        }
    }

    private static String blankToNull(String value) {
        return blank(value) ? null : value;
    }

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }

    public record AuditPage(List<AuditItem> items, Instant nextOccurredAt, UUID nextRecordId) { }

    public record AuditItem(UUID recordId, Instant occurredAt, String actionCode, int actionVersion,
            UUID actorId, String actorKind, String targetType, String targetId, UUID operationId,
            UUID correlationId, int schemaVersion, JsonNode metadata, String outcome, UUID tenantId,
            UUID companyId, UUID siteId) { }

    private static final class InvalidQuery extends RuntimeException { }
}
