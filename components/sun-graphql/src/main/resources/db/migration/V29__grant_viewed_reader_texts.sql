
-- 1. Learner role (Discord default)
INSERT INTO gaia_role_permissions (id, role_id, permission, createdat, lastupdatedat)
SELECT gen_random_uuid(), r.id, 'graphql.hades.viewedReaderTexts', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM gaia_roles r
WHERE r.name = 'learner'
ON CONFLICT (role_id, permission) DO NOTHING;

-- 2. Any other role that already holds viewedTexts (custom / future roles)
INSERT INTO gaia_role_permissions (id, role_id, permission, createdat, lastupdatedat)
SELECT gen_random_uuid(), rp.role_id, 'graphql.hades.viewedReaderTexts', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM gaia_role_permissions rp
WHERE rp.permission = 'graphql.hades.viewedTexts'
ON CONFLICT (role_id, permission) DO NOTHING;

-- 3. Any account that holds viewedTexts directly
INSERT INTO gaia_account_permissions (id, account_id, permission, createdat, lastupdatedat)
SELECT gen_random_uuid(), ap.account_id, 'graphql.hades.viewedReaderTexts', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM gaia_account_permissions ap
WHERE ap.permission = 'graphql.hades.viewedTexts'
ON CONFLICT (account_id, permission) DO NOTHING;
