-- V32 grants dedicated permissions for blog-as-knowledge tree and edge mutations.

-- children query (paginated, DB-level Pageable)
INSERT INTO gaia_role_permissions (id, role_id, permission, createdat, lastupdatedat)
SELECT gen_random_uuid(), rp.role_id, 'graphql.briareus.children', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM gaia_role_permissions rp
WHERE rp.permission = 'graphql.briareus.listBlogPosts'
ON CONFLICT (role_id, permission) DO NOTHING;

INSERT INTO gaia_account_permissions (id, account_id, permission, createdat, lastupdatedat)
SELECT gen_random_uuid(), ap.account_id, 'graphql.briareus.children', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM gaia_account_permissions ap
WHERE ap.permission = 'graphql.briareus.listBlogPosts'
ON CONFLICT (account_id, permission) DO NOTHING;

-- addRemoteObject / removeRemoteObject (edge mutations, unique per mutation)
INSERT INTO gaia_role_permissions (id, role_id, permission, createdat, lastupdatedat)
SELECT gen_random_uuid(), rp.role_id, p.perm, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM gaia_role_permissions rp,
     (VALUES ('graphql.briareus.addRemoteObject'), ('graphql.briareus.removeRemoteObject')) AS p(perm)
WHERE rp.permission = 'graphql.briareus.createBlogPost'
ON CONFLICT (role_id, permission) DO NOTHING;

INSERT INTO gaia_account_permissions (id, account_id, permission, createdat, lastupdatedat)
SELECT gen_random_uuid(), ap.account_id, p.perm, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM gaia_account_permissions ap,
     (VALUES ('graphql.briareus.addRemoteObject'), ('graphql.briareus.removeRemoteObject')) AS p(perm)
WHERE ap.permission = 'graphql.briareus.createBlogPost'
ON CONFLICT (account_id, permission) DO NOTHING;
