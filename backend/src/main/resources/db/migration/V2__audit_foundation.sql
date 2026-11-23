-- Technical audit only. No business module, no Orders/Inventory/invoicing relation.
CREATE SCHEMA audit AUTHORIZATION authority_pro_owner;
REVOKE ALL ON SCHEMA audit FROM PUBLIC;
REVOKE ALL ON SCHEMA audit FROM authority_pro_runtime;
GRANT USAGE ON SCHEMA audit TO authority_pro_runtime;

ALTER DEFAULT PRIVILEGES FOR ROLE authority_pro_owner IN SCHEMA audit
    REVOKE ALL ON TABLES FROM PUBLIC, authority_pro_runtime;
ALTER DEFAULT PRIVILEGES FOR ROLE authority_pro_owner IN SCHEMA audit
    REVOKE ALL ON SEQUENCES FROM PUBLIC, authority_pro_runtime;

-- Family A: tenant critical records. Family B/C deliberately have no tenant column.
CREATE TABLE audit.tenant_records (
    record_id uuid PRIMARY KEY,
    occurred_at timestamptz NOT NULL DEFAULT clock_timestamp(),
    action_code text NOT NULL CHECK (length(btrim(action_code)) > 0 AND length(action_code) <= 96),
    action_version integer NOT NULL CHECK (action_version > 0),
    actor_id uuid NOT NULL,
    actor_kind text NOT NULL CHECK (actor_kind IN ('user', 'operator', 'service')),
    target_type text NOT NULL CHECK (length(btrim(target_type)) > 0 AND length(target_type) <= 64),
    target_id text NOT NULL CHECK (length(btrim(target_id)) > 0 AND length(target_id) <= 128),
    operation_id uuid,
    correlation_id uuid,
    schema_version integer NOT NULL CHECK (schema_version > 0),
    metadata jsonb NOT NULL CHECK (jsonb_typeof(metadata) = 'object'
        AND jsonb_array_length(jsonb_path_query_array(metadata, '$.*')) <= 20
        AND octet_length(metadata::text) <= 4096),
    outcome text NOT NULL DEFAULT 'committed' CHECK (outcome = 'committed'),
    tenant_id uuid NOT NULL,
    company_id uuid,
    site_id uuid,
    CHECK (site_id IS NULL OR company_id IS NOT NULL)
);

CREATE TABLE audit.control_plane_records (
    record_id uuid PRIMARY KEY,
    occurred_at timestamptz NOT NULL DEFAULT clock_timestamp(),
    action_code text NOT NULL CHECK (length(btrim(action_code)) > 0 AND length(action_code) <= 96),
    action_version integer NOT NULL CHECK (action_version > 0),
    actor_id uuid NOT NULL,
    actor_kind text NOT NULL CHECK (actor_kind IN ('user', 'operator', 'service')),
    target_type text NOT NULL CHECK (length(btrim(target_type)) > 0 AND length(target_type) <= 64),
    target_id text NOT NULL CHECK (length(btrim(target_id)) > 0 AND length(target_id) <= 128),
    operation_id uuid,
    correlation_id uuid,
    schema_version integer NOT NULL CHECK (schema_version > 0),
    metadata jsonb NOT NULL CHECK (jsonb_typeof(metadata) = 'object'
        AND jsonb_array_length(jsonb_path_query_array(metadata, '$.*')) <= 20
        AND octet_length(metadata::text) <= 4096),
    outcome text NOT NULL DEFAULT 'committed' CHECK (outcome = 'committed')
);

CREATE TABLE audit.security_attempts (
    attempt_id uuid PRIMARY KEY,
    occurred_at timestamptz NOT NULL DEFAULT clock_timestamp(),
    reason_code text NOT NULL CHECK (reason_code IN ('invalid_credentials', 'account_locked',
        'session_expired', 'rate_limited', 'csrf_rejected', 'authorization_denied')),
    correlation_id uuid,
    source_class text NOT NULL CHECK (length(btrim(source_class)) > 0 AND length(source_class) <= 32)
);

CREATE INDEX tenant_records_order
    ON audit.tenant_records (tenant_id, occurred_at DESC, record_id DESC);
CREATE UNIQUE INDEX tenant_records_operation
    ON audit.tenant_records (tenant_id, operation_id, action_code) WHERE operation_id IS NOT NULL;
CREATE INDEX control_plane_records_order
    ON audit.control_plane_records (occurred_at DESC, record_id DESC);
CREATE UNIQUE INDEX control_plane_records_operation
    ON audit.control_plane_records (operation_id, action_code) WHERE operation_id IS NOT NULL;
CREATE INDEX security_attempts_order
    ON audit.security_attempts (occurred_at DESC, attempt_id DESC);

ALTER TABLE audit.tenant_records OWNER TO authority_pro_owner;
ALTER TABLE audit.control_plane_records OWNER TO authority_pro_owner;
ALTER TABLE audit.security_attempts OWNER TO authority_pro_owner;

-- Only family A carries tenant scope; B and C must stay free of copied tenant data.
ALTER TABLE audit.tenant_records ENABLE ROW LEVEL SECURITY;
ALTER TABLE audit.tenant_records FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_scope ON audit.tenant_records
    FOR ALL TO authority_pro_owner, authority_pro_runtime
    USING (tenant_id = nullif(current_setting('app.tenant_id', true), '')::uuid
        AND nullif(current_setting('app.user_id', true), '')::uuid IS NOT NULL)
    WITH CHECK (tenant_id = nullif(current_setting('app.tenant_id', true), '')::uuid
        AND nullif(current_setting('app.user_id', true), '')::uuid IS NOT NULL);

REVOKE ALL ON audit.tenant_records, audit.control_plane_records, audit.security_attempts
    FROM PUBLIC, authority_pro_runtime;
GRANT SELECT, INSERT ON audit.tenant_records, audit.control_plane_records, audit.security_attempts
    TO authority_pro_runtime;
