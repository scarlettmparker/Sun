-- V46 grants deletePropertyEntry permission anchored on upsertPropertyEntry.

INSERT INTO gaia_role_permissions (id, role_id, permission, createdat, lastupdatedat)
SELECT gen_random_uuid(), rp.role_id, 'graphql.gaia.deletePropertyEntry', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM gaia_role_permissions rp
WHERE rp.permission = 'graphql.gaia.upsertPropertyEntry'
ON CONFLICT (role_id, permission) DO NOTHING;

INSERT INTO gaia_account_permissions (id, account_id, permission, createdat, lastupdatedat)
SELECT gen_random_uuid(), ap.account_id, 'graphql.gaia.deletePropertyEntry', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM gaia_account_permissions ap
WHERE ap.permission = 'graphql.gaia.upsertPropertyEntry'
ON CONFLICT (account_id, permission) DO NOTHING;
