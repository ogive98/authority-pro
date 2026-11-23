-- Technical identity/access only. No business module and no tenant business table.
CREATE SCHEMA identity AUTHORIZATION authority_pro_owner;
REVOKE ALL ON SCHEMA identity FROM PUBLIC;
REVOKE ALL ON SCHEMA identity FROM authority_pro_runtime;
GRANT USAGE ON SCHEMA identity TO authority_pro_runtime;

CREATE SCHEMA access AUTHORIZATION authority_pro_owner;
REVOKE ALL ON SCHEMA access FROM PUBLIC;
REVOKE ALL ON SCHEMA access FROM authority_pro_runtime;
GRANT USAGE ON SCHEMA access TO authority_pro_runtime;

ALTER DEFAULT PRIVILEGES FOR ROLE authority_pro_owner IN SCHEMA identity
    REVOKE ALL ON TABLES FROM PUBLIC, authority_pro_runtime;
ALTER DEFAULT PRIVILEGES FOR ROLE authority_pro_owner IN SCHEMA identity
    REVOKE ALL ON SEQUENCES FROM PUBLIC, authority_pro_runtime;
ALTER DEFAULT PRIVILEGES FOR ROLE authority_pro_owner IN SCHEMA access
    REVOKE ALL ON TABLES FROM PUBLIC, authority_pro_runtime;
ALTER DEFAULT PRIVILEGES FOR ROLE authority_pro_owner IN SCHEMA access
    REVOKE ALL ON SEQUENCES FROM PUBLIC, authority_pro_runtime;

-- Global control plane: no tenant column, no tenant RLS.
CREATE TABLE identity.users (
    user_id uuid PRIMARY KEY,
    login_normalized text NOT NULL UNIQUE
        CHECK (login_normalized = lower(login_normalized)
            AND login_normalized ~ '^[a-z0-9][a-z0-9._@-]{2,127}$'),
    display_name text NOT NULL CHECK (length(btrim(display_name)) BETWEEN 1 AND 128),
    status text NOT NULL DEFAULT 'active' CHECK (status IN ('active', 'disabled')),
    authorization_revision integer NOT NULL DEFAULT 1 CHECK (authorization_revision > 0),
    created_at timestamptz NOT NULL DEFAULT clock_timestamp()
);

CREATE TABLE identity.password_credentials (
    user_id uuid PRIMARY KEY REFERENCES identity.users (user_id),
    password_hash text NOT NULL CHECK (password_hash ~ '^\$2[aby]\$1[2-4]\$[./A-Za-z0-9]{53}$'),
    scheme text NOT NULL CHECK (scheme = 'bcrypt'),
    changed_at timestamptz NOT NULL DEFAULT clock_timestamp()
);

-- Single-use bootstrap guard: the boolean primary key admits one row at most.
CREATE TABLE identity.provisioning_guard (
    singleton boolean PRIMARY KEY DEFAULT true CHECK (singleton),
    provisioned_at timestamptz NOT NULL DEFAULT clock_timestamp(),
    operator_user_id uuid NOT NULL REFERENCES identity.users (user_id),
    revision integer NOT NULL CHECK (revision = 1)
);

CREATE TABLE access.roles (
    role_code text PRIMARY KEY
        CHECK (role_code IN ('TENANT_OWNER', 'TENANT_ADMIN', 'TENANT_AUDITOR'))
);

CREATE TABLE access.role_permissions (
    role_code text NOT NULL REFERENCES access.roles (role_code),
    permission_code text NOT NULL
        CHECK (permission_code IN ('audit.read.tenant', 'audit.read.control_plane', 'security.attempts.read',
            'tenancy.settings.manage', 'access.membership.manage')),
    PRIMARY KEY (role_code, permission_code)
);

INSERT INTO access.roles (role_code) VALUES ('TENANT_OWNER'), ('TENANT_ADMIN'), ('TENANT_AUDITOR');
INSERT INTO access.role_permissions (role_code, permission_code) VALUES
    ('TENANT_OWNER', 'audit.read.tenant'),
    ('TENANT_OWNER', 'audit.read.control_plane'),
    ('TENANT_OWNER', 'security.attempts.read'),
    ('TENANT_OWNER', 'tenancy.settings.manage'),
    ('TENANT_OWNER', 'access.membership.manage'),
    ('TENANT_ADMIN', 'tenancy.settings.manage'),
    ('TENANT_ADMIN', 'access.membership.manage'),
    ('TENANT_AUDITOR', 'audit.read.tenant'),
    ('TENANT_AUDITOR', 'audit.read.control_plane'),
    ('TENANT_AUDITOR', 'security.attempts.read');

-- Membership gains an authorization revision and an explicit disable marker.
ALTER TABLE tenancy.memberships
    ADD COLUMN disabled_at timestamptz,
    ADD COLUMN authorization_revision integer NOT NULL DEFAULT 1
        CHECK (authorization_revision > 0);

CREATE TABLE access.membership_roles (
    user_id uuid NOT NULL,
    tenant_id uuid NOT NULL,
    role_code text NOT NULL REFERENCES access.roles (role_code),
    company_id uuid,
    site_id uuid,
    authorization_revision integer NOT NULL DEFAULT 1 CHECK (authorization_revision > 0),
    PRIMARY KEY (user_id, tenant_id, role_code),
    FOREIGN KEY (user_id, tenant_id) REFERENCES tenancy.memberships (user_id, tenant_id),
    FOREIGN KEY (tenant_id, company_id) REFERENCES tenancy.companies (tenant_id, company_id),
    FOREIGN KEY (tenant_id, company_id, site_id)
        REFERENCES tenancy.sites (tenant_id, company_id, site_id),
    CHECK (site_id IS NULL OR company_id IS NOT NULL)
);

-- Pre-selection labels: SELECT-only, gated by an active membership of the current internal user.
CREATE POLICY member_tenant_label_read ON tenancy.tenants
    FOR SELECT TO authority_pro_owner, authority_pro_runtime
    USING (EXISTS (
        SELECT 1 FROM tenancy.memberships m
        WHERE m.tenant_id = tenants.tenant_id
            AND m.user_id = nullif(current_setting('app.user_id', true), '')::uuid
            AND m.active AND m.disabled_at IS NULL));

CREATE POLICY member_company_label_read ON tenancy.companies
    FOR SELECT TO authority_pro_owner, authority_pro_runtime
    USING (EXISTS (
        SELECT 1 FROM tenancy.memberships m
        WHERE m.tenant_id = companies.tenant_id
            AND m.user_id = nullif(current_setting('app.user_id', true), '')::uuid
            AND m.active AND m.disabled_at IS NULL));

ALTER TABLE identity.users OWNER TO authority_pro_owner;
ALTER TABLE identity.password_credentials OWNER TO authority_pro_owner;
ALTER TABLE identity.provisioning_guard OWNER TO authority_pro_owner;
ALTER TABLE access.roles OWNER TO authority_pro_owner;
ALTER TABLE access.role_permissions OWNER TO authority_pro_owner;
ALTER TABLE access.membership_roles OWNER TO authority_pro_owner;

REVOKE ALL ON identity.users, identity.password_credentials, identity.provisioning_guard
    FROM PUBLIC, authority_pro_runtime;
REVOKE ALL ON access.roles, access.role_permissions, access.membership_roles
    FROM PUBLIC, authority_pro_runtime;

-- Login needs read access to the credential plane; no runtime write path exists in L05 yet.
GRANT SELECT ON identity.users, identity.password_credentials TO authority_pro_runtime;
GRANT SELECT ON access.roles, access.role_permissions, access.membership_roles TO authority_pro_runtime;
