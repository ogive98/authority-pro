package com.authoritypro.platform.access.infrastructure.http;

import com.authoritypro.platform.access.application.AccessStoreUnavailableException;
import com.authoritypro.platform.access.application.ScopedAccess;
import com.authoritypro.platform.access.application.ScopedAccessReader;
import com.authoritypro.platform.access.domain.Permission;
import com.authoritypro.platform.access.domain.RoleCode;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;

/**
 * Post-auth bootstrap for one selected scope only. It never lists other memberships and never
 * exposes the user's global grants; permissions are effective for the validated selection.
 */
@ConditionalOnBean(DataSource.class)
@RestController
@RequestMapping("/api/v1/access")
public final class ScopedAccessController {
    private final ScopedAccessReader reader;
    private final AccessProblems problems;

    public ScopedAccessController(ScopedAccessReader reader, AccessProblems problems) {
        this.reader = reader;
        this.problems = problems;
    }

    @GetMapping("/bootstrap")
    public ResponseEntity<?> bootstrap(HttpServletRequest request) {
        UUID userId = userId();
        if (userId == null) {
            return problems.response(AccessProblems.Code.AUTHENTICATION_REQUIRED);
        }
        if (!blank(request.getHeader(ScopeHeaders.SITE))) {
            return problems.response(AccessProblems.Code.SCOPE_INVALID);
        }
        UUID tenantId = uuid(request.getHeader(ScopeHeaders.TENANT));
        if (tenantId == null) {
            return problems.response(AccessProblems.Code.SCOPE_INVALID);
        }
        UUID companyId = null;
        if (!blank(request.getHeader(ScopeHeaders.COMPANY))) {
            companyId = uuid(request.getHeader(ScopeHeaders.COMPANY));
            if (companyId == null) {
                return problems.response(AccessProblems.Code.SCOPE_INVALID);
            }
        }
        Optional<ScopedAccess> access;
        try {
            access = reader.read(userId, tenantId, companyId);
        } catch (AccessStoreUnavailableException unavailable) {
            return problems.response(AccessProblems.Code.ACCESS_UNAVAILABLE);
        }
        if (access.isEmpty() || access.get().roles().isEmpty()) {
            return problems.response(AccessProblems.Code.AUTHORIZATION_DENIED);
        }
        ScopedAccess scope = access.get();
        BootstrapResponse body = new BootstrapResponse(scope.tenantId(), scope.tenantName(),
                scope.companyId(), scope.companyName(), scope.authorizationRevision(),
                scope.roles().stream().map(RoleCode::name).sorted().toList(),
                scope.permissions().stream().map(Permission::code).sorted().toList());
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .cacheControl(CacheControl.noStore())
                .header("X-Content-Type-Options", "nosniff")
                .body(body);
    }

    public record BootstrapResponse(UUID tenantId, String tenantName, UUID companyId, String companyName,
            int authorizationRevision, List<String> roles, List<String> permissions) { }

    private static UUID userId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) return null;
        try {
            return UUID.fromString(authentication.getName());
        } catch (IllegalArgumentException malformed) {
            return null;
        }
    }

    private static UUID uuid(String value) {
        if (blank(value)) return null;
        try {
            return UUID.fromString(value.trim());
        } catch (IllegalArgumentException malformed) {
            return null;
        }
    }

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }
}
