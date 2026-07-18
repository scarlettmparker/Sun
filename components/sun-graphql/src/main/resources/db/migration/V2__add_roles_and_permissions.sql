-- V2 roles and permissions: roles bundle permission strings; accounts hold
-- roles and/or direct permissions. Permission strings may use * as a glob
-- (e.g. graphql.icarus.*, graphql.*, *). Hibernate (ddl-auto=update) is a
-- no-op on these tables because the columns and constraint names match the
-- entities exactly.

CREATE TABLE gaia_roles (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name             TEXT NOT NULL,
    description      TEXT,
    created_at       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_updated_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by       UUID,
    last_updated_by  UUID,
    CONSTRAINT gaia_roles_name_key UNIQUE (name)
);

CREATE TABLE gaia_role_permissions (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    role_id          UUID NOT NULL,
    permission       TEXT NOT NULL,
    created_at       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_updated_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by       UUID,
    last_updated_by  UUID,
    CONSTRAINT gaia_role_permissions_role_id_permission_key UNIQUE (role_id, permission),
    CONSTRAINT fk_role_permissions_role FOREIGN KEY (role_id)
        REFERENCES gaia_roles (id) ON DELETE CASCADE
);

CREATE TABLE gaia_account_roles (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    account_id       UUID NOT NULL,
    role_id          UUID NOT NULL,
    created_at       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_updated_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by       UUID,
    last_updated_by  UUID,
    CONSTRAINT gaia_account_roles_account_id_role_id_key UNIQUE (account_id, role_id),
    CONSTRAINT fk_account_roles_account FOREIGN KEY (account_id)
        REFERENCES gaia_accounts (id) ON DELETE CASCADE,
    CONSTRAINT fk_account_roles_role FOREIGN KEY (role_id)
        REFERENCES gaia_roles (id) ON DELETE CASCADE
);

CREATE TABLE gaia_account_permissions (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    account_id       UUID NOT NULL,
    permission       TEXT NOT NULL,
    created_at       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_updated_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by       UUID,
    last_updated_by  UUID,
    CONSTRAINT gaia_account_permissions_account_id_permission_key UNIQUE (account_id, permission),
    CONSTRAINT fk_account_permissions_account FOREIGN KEY (account_id)
        REFERENCES gaia_accounts (id) ON DELETE CASCADE
);

CREATE INDEX idx_role_permissions_role ON gaia_role_permissions (role_id);
CREATE INDEX idx_account_roles_account ON gaia_account_roles (account_id);
CREATE INDEX idx_account_permissions_account ON gaia_account_permissions (account_id);

-- Super Admin role: matches every permission via the * glob.
INSERT INTO gaia_roles (id, name, description)
VALUES ('11111111-1111-1111-1111-111111111111', 'Super Admin', 'Wildcard access (*)')
ON CONFLICT (name) DO NOTHING;

INSERT INTO gaia_role_permissions (role_id, permission)
SELECT id, '*' FROM gaia_roles WHERE name = 'Super Admin'
ON CONFLICT (role_id, permission) DO NOTHING;
