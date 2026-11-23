-- Technical session store and closed operator-provisioning privileges. No business module.
-- The provisioner role is created by the environment tooling before this migration runs.

CREATE TABLE identity.spring_session (
    primary_id char(36) PRIMARY KEY,
    session_id char(36) NOT NULL,
    creation_time bigint NOT NULL,
    last_access_time bigint NOT NULL,
    max_inactive_interval integer NOT NULL,
    expiry_time bigint NOT NULL,
    principal_name varchar(100)
);

CREATE UNIQUE INDEX spring_session_session_id ON identity.spring_session (session_id);
CREATE INDEX spring_session_expiry ON identity.spring_session (expiry_time);
CREATE INDEX spring_session_principal ON identity.spring_session (principal_name);

CREATE TABLE identity.spring_session_attributes (
    session_primary_id char(36) NOT NULL
        REFERENCES identity.spring_session (primary_id) ON DELETE CASCADE,
    attribute_name varchar(200) NOT NULL,
    attribute_bytes bytea NOT NULL,
    PRIMARY KEY (session_primary_id, attribute_name)
);

ALTER TABLE identity.spring_session OWNER TO authority_pro_owner;
ALTER TABLE identity.spring_session_attributes OWNER TO authority_pro_owner;

REVOKE ALL ON identity.spring_session, identity.spring_session_attributes
    FROM PUBLIC, authority_pro_runtime, authority_pro_provisioner;

-- The runtime session repository owns the session rows; identifiers are credential material.
GRANT SELECT, INSERT, UPDATE, DELETE
    ON identity.spring_session, identity.spring_session_attributes
    TO authority_pro_runtime;

-- Provisioning path: append-only guard, user and credential creation, control-plane audit.
GRANT USAGE ON SCHEMA identity, audit TO authority_pro_provisioner;
GRANT SELECT, INSERT ON identity.provisioning_guard TO authority_pro_provisioner;
GRANT INSERT ON identity.users, identity.password_credentials TO authority_pro_provisioner;
GRANT INSERT ON audit.control_plane_records TO authority_pro_provisioner;
GRANT SELECT (occurred_at) ON audit.control_plane_records TO authority_pro_provisioner;

DO $$
BEGIN
    EXECUTE format('GRANT CONNECT ON DATABASE %I TO authority_pro_provisioner', current_database());
END
$$;
