package com.authoritypro.platform.identity.application;

import java.util.List;
import java.util.UUID;

/**
 * The caller's own control-plane profile. Language/theme preferences and scope-evaluated
 * permissions are not part of this slice: neither the schema nor a selected scope exists yet.
 */
public record OwnProfile(UUID userId, String displayName, String login, List<Membership> memberships) {
    public OwnProfile {
        if (userId == null || displayName == null || login == null || memberships == null) {
            throw new IllegalArgumentException("PROFILE_INVALID");
        }
        memberships = List.copyOf(memberships);
    }

    public record Membership(UUID tenantId, String tenantName, List<String> roles, List<Company> companies) {
        public Membership {
            if (tenantId == null || tenantName == null || roles == null || companies == null) {
                throw new IllegalArgumentException("MEMBERSHIP_INVALID");
            }
            roles = List.copyOf(roles);
            companies = List.copyOf(companies);
        }
    }

    public record Company(UUID companyId, String name) {
        public Company {
            if (companyId == null || name == null) throw new IllegalArgumentException("COMPANY_INVALID");
        }
    }
}
