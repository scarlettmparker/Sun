-- V36 seeds graphql.briareus.attachedTexts permission anchored on locateBlogPost / listBlogPosts.

INSERT INTO gaia_role_permissions (id, role_id, permission, createdat, lastupdatedat)
SELECT gen_random_uuid(), rp.role_id, 'graphql.briareus.attachedTexts', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM gaia_role_permissions rp
WHERE rp.permission = 'graphql.briareus.locateBlogPost'
ON CONFLICT (role_id, permission) DO NOTHING;

INSERT INTO gaia_account_permissions (id, account_id, permission, createdat, lastupdatedat)
SELECT gen_random_uuid(), ap.account_id, 'graphql.briareus.attachedTexts', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM gaia_account_permissions ap
WHERE ap.permission = 'graphql.briareus.locateBlogPost'
ON CONFLICT (account_id, permission) DO NOTHING;

INSERT INTO gaia_role_permissions (id, role_id, permission, createdat, lastupdatedat)
SELECT gen_random_uuid(), rp.role_id, 'graphql.briareus.attachedTexts', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM gaia_role_permissions rp
WHERE rp.permission = 'graphql.briareus.listBlogPosts'
  AND NOT EXISTS (
    SELECT 1 FROM gaia_role_permissions rp2
    WHERE rp2.role_id = rp.role_id AND rp2.permission = 'graphql.briareus.attachedTexts'
  )
ON CONFLICT (role_id, permission) DO NOTHING;

INSERT INTO gaia_account_permissions (id, account_id, permission, createdat, lastupdatedat)
SELECT gen_random_uuid(), ap.account_id, 'graphql.briareus.attachedTexts', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM gaia_account_permissions ap
WHERE ap.permission = 'graphql.briareus.listBlogPosts'
  AND NOT EXISTS (
    SELECT 1 FROM gaia_account_permissions ap2
    WHERE ap2.account_id = ap.account_id AND ap2.permission = 'graphql.briareus.attachedTexts'
  )
ON CONFLICT (account_id, permission) DO NOTHING;
