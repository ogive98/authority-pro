-- L06 approved extension: one control-plane permission granted to TENANT_OWNER only.
-- No business table, no tenant column and no privilege change on the grant catalogue.

ALTER TABLE access.role_permissions DROP CONSTRAINT role_permissions_permission_code_check;
ALTER TABLE access.role_permissions ADD CONSTRAINT role_permissions_permission_code_check
    CHECK (permission_code IN ('audit.read.tenant', 'audit.read.control_plane', 'security.attempts.read',
        'tenancy.settings.manage', 'access.membership.manage', 'platform.inventory.read'));

INSERT INTO access.role_permissions (role_code, permission_code)
VALUES ('TENANT_OWNER', 'platform.inventory.read');
