-- Technical tenancy only. Provisioned roles and Flyway's public history must already exist.
-- The migrator connects as authority_pro_migrator with effective role authority_pro_owner.
CREATE SCHEMA tenancy AUTHORIZATION authority_pro_owner;
REVOKE ALL ON SCHEMA tenancy FROM PUBLIC;
REVOKE ALL ON SCHEMA tenancy FROM authority_pro_runtime;
GRANT USAGE ON SCHEMA tenancy TO authority_pro_runtime;

-- Future tenancy relations receive no ambient PUBLIC or runtime access.
-- Keep defaults owner- and schema-scoped; provisioner owns database/role hardening.
ALTER DEFAULT PRIVILEGES FOR ROLE authority_pro_owner IN SCHEMA tenancy
    REVOKE ALL ON TABLES FROM PUBLIC, authority_pro_runtime;
ALTER DEFAULT PRIVILEGES FOR ROLE authority_pro_owner IN SCHEMA tenancy
    REVOKE ALL ON SEQUENCES FROM PUBLIC, authority_pro_runtime;

CREATE TABLE tenancy.tenants (
    tenant_id uuid PRIMARY KEY,
    name text NOT NULL CHECK (length(btrim(name)) > 0)
);

CREATE TABLE tenancy.companies (
    tenant_id uuid NOT NULL,
    company_id uuid NOT NULL,
    name text NOT NULL CHECK (length(btrim(name)) > 0),
    PRIMARY KEY (tenant_id, company_id),
    FOREIGN KEY (tenant_id) REFERENCES tenancy.tenants (tenant_id)
);

CREATE TABLE tenancy.sites (
    tenant_id uuid NOT NULL,
    company_id uuid NOT NULL,
    site_id uuid NOT NULL,
    name text NOT NULL CHECK (length(btrim(name)) > 0),
    PRIMARY KEY (tenant_id, company_id, site_id),
    FOREIGN KEY (tenant_id, company_id) REFERENCES tenancy.companies (tenant_id, company_id)
);

CREATE TABLE tenancy.memberships (
    user_id uuid NOT NULL,
    tenant_id uuid NOT NULL,
    active boolean NOT NULL DEFAULT true,
    PRIMARY KEY (user_id, tenant_id),
    FOREIGN KEY (tenant_id) REFERENCES tenancy.tenants (tenant_id)
);

ALTER TABLE tenancy.tenants OWNER TO authority_pro_owner;
ALTER TABLE tenancy.companies OWNER TO authority_pro_owner;
ALTER TABLE tenancy.sites OWNER TO authority_pro_owner;
ALTER TABLE tenancy.memberships OWNER TO authority_pro_owner;

ALTER TABLE tenancy.tenants ENABLE ROW LEVEL SECURITY;
ALTER TABLE tenancy.tenants FORCE ROW LEVEL SECURITY;
ALTER TABLE tenancy.companies ENABLE ROW LEVEL SECURITY;
ALTER TABLE tenancy.companies FORCE ROW LEVEL SECURITY;
ALTER TABLE tenancy.sites ENABLE ROW LEVEL SECURITY;
ALTER TABLE tenancy.sites FORCE ROW LEVEL SECURITY;
ALTER TABLE tenancy.memberships ENABLE ROW LEVEL SECURITY;
ALTER TABLE tenancy.memberships FORCE ROW LEVEL SECURITY;

-- Context is an internal JDBC parameter, not a trusted HTTP/principal authorization claim.
-- Empty/missing settings see no rows; malformed UUID settings fail closed at the cast.
CREATE POLICY tenant_scope ON tenancy.tenants
    FOR ALL TO authority_pro_owner, authority_pro_runtime
    USING (tenant_id = nullif(current_setting('app.tenant_id', true), '')::uuid
        AND nullif(current_setting('app.user_id', true), '')::uuid IS NOT NULL)
    WITH CHECK (tenant_id = nullif(current_setting('app.tenant_id', true), '')::uuid
        AND nullif(current_setting('app.user_id', true), '')::uuid IS NOT NULL);

CREATE POLICY tenant_scope ON tenancy.companies
    FOR ALL TO authority_pro_owner, authority_pro_runtime
    USING (tenant_id = nullif(current_setting('app.tenant_id', true), '')::uuid
        AND nullif(current_setting('app.user_id', true), '')::uuid IS NOT NULL)
    WITH CHECK (tenant_id = nullif(current_setting('app.tenant_id', true), '')::uuid
        AND nullif(current_setting('app.user_id', true), '')::uuid IS NOT NULL);

CREATE POLICY tenant_scope ON tenancy.sites
    FOR ALL TO authority_pro_owner, authority_pro_runtime
    USING (tenant_id = nullif(current_setting('app.tenant_id', true), '')::uuid
        AND nullif(current_setting('app.user_id', true), '')::uuid IS NOT NULL)
    WITH CHECK (tenant_id = nullif(current_setting('app.tenant_id', true), '')::uuid
        AND nullif(current_setting('app.user_id', true), '')::uuid IS NOT NULL);

-- Membership discovery precedes tenant choice and reveals only the current internal user's rows.
CREATE POLICY own_membership_read ON tenancy.memberships
    FOR SELECT TO authority_pro_owner, authority_pro_runtime
    USING (user_id = nullif(current_setting('app.user_id', true), '')::uuid);

-- FORCE also constrains fixture seeding through the owner; runtime never receives these DML grants.
CREATE POLICY own_membership_insert ON tenancy.memberships
    FOR INSERT TO authority_pro_owner
    WITH CHECK (tenant_id = nullif(current_setting('app.tenant_id', true), '')::uuid
        AND user_id = nullif(current_setting('app.user_id', true), '')::uuid);
CREATE POLICY own_membership_update ON tenancy.memberships
    FOR UPDATE TO authority_pro_owner
    USING (tenant_id = nullif(current_setting('app.tenant_id', true), '')::uuid
        AND user_id = nullif(current_setting('app.user_id', true), '')::uuid)
    WITH CHECK (tenant_id = nullif(current_setting('app.tenant_id', true), '')::uuid
        AND user_id = nullif(current_setting('app.user_id', true), '')::uuid);
CREATE POLICY own_membership_delete ON tenancy.memberships
    FOR DELETE TO authority_pro_owner
    USING (tenant_id = nullif(current_setting('app.tenant_id', true), '')::uuid
        AND user_id = nullif(current_setting('app.user_id', true), '')::uuid);

REVOKE ALL ON tenancy.tenants, tenancy.companies, tenancy.sites, tenancy.memberships
    FROM PUBLIC, authority_pro_runtime;
GRANT SELECT, INSERT, UPDATE, DELETE ON tenancy.tenants, tenancy.companies, tenancy.sites
    TO authority_pro_runtime;
GRANT SELECT ON tenancy.memberships TO authority_pro_runtime;

-- Flyway creates history before executing V1; runtime can validate it but cannot change it.
REVOKE ALL ON public.flyway_schema_history FROM PUBLIC, authority_pro_runtime;
GRANT SELECT ON public.flyway_schema_history TO authority_pro_runtime;
