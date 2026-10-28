-- V64 grants Free Chess users the batch player lookup used by lobby and game loaders.

INSERT INTO gaia_role_permissions (id, role_id, permission, createdat, lastupdatedat)
SELECT gen_random_uuid(), r.id, 'graphql.medusa.players',
       CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM gaia_roles r
WHERE r.name = 'Free Chess User'
ON CONFLICT (role_id, permission) DO NOTHING;
