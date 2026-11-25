package com.authoritypro.platform.access.infrastructure.persistence;

import com.authoritypro.platform.access.application.ScopedGrant;
import com.authoritypro.platform.access.domain.AccessDecision;
import com.authoritypro.platform.access.domain.MembershipAssignment;
import com.authoritypro.platform.access.domain.MembershipSelection;
import com.authoritypro.platform.access.domain.PermissionEvaluator;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.UUID;

/**
 * Resolves one selection inside the caller's transaction. The caller installs
 * {@code app.user_id}; no label or grant is read without an active membership row.
 */
final class JdbcScopedGrantResolver {
    private final JdbcAuthorizationCatalog catalog = new JdbcAuthorizationCatalog();

    Optional<ScopedGrant> resolve(Connection connection, UUID userId, UUID tenantId, UUID companyId)
            throws SQLException {
        OptionalInt revision = catalog.activeMembershipRevision(connection, userId, tenantId);
        if (revision.isEmpty()) return Optional.empty();
        if (companyId != null && !companyExists(connection, tenantId, companyId)) return Optional.empty();

        PermissionEvaluator evaluator = new PermissionEvaluator(catalog.catalogue(connection));
        AccessDecision decision = new AccessDecision(evaluator);
        MembershipSelection selection = new MembershipSelection(tenantId, companyId, null, revision.getAsInt());
        List<MembershipAssignment> assignments = catalog.assignments(connection, userId);
        return Optional.of(new ScopedGrant(tenantId, companyId, revision.getAsInt(),
                decision.rolesFor(assignments, selection),
                decision.permissionsFor(assignments, selection)));
    }

    private static boolean companyExists(Connection connection, UUID tenantId, UUID companyId)
            throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT 1 FROM tenancy.companies WHERE tenant_id = ? AND company_id = ?")) {
            statement.setQueryTimeout(5);
            statement.setObject(1, tenantId);
            statement.setObject(2, companyId);
            try (ResultSet result = statement.executeQuery()) {
                return result.next();
            }
        }
    }
}
