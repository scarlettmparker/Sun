-- V63 grants Free Chess game query permissions and creates the archive worker account.

INSERT INTO gaia_role_permissions (id, role_id, permission, createdat, lastupdatedat)
SELECT gen_random_uuid(), r.id, p.perm, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM gaia_roles r,
     (VALUES
       ('graphql.medusa.game'),
       ('graphql.medusa.myGames')
     ) AS p(perm)
WHERE r.name = 'Free Chess User'
ON CONFLICT (role_id, permission) DO NOTHING;

INSERT INTO fates_people (id, first_name, last_name, display_name, createdat, lastupdatedat)
VALUES ('c27f63c8-2dd9-4d5e-a5b2-6fbbf4f5c1a1', 'Free', 'Chess Worker',
        'Free Chess Worker', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (id) DO NOTHING;

INSERT INTO gaia_accounts (id, username, password_hash, person_id, status, provider, account_type,
                           createdat, lastupdatedat)
VALUES ('ad5b2f99-7e7d-4d8b-9f74-1f0b6b2f2f4e', 'free-chess-worker', '!',
        'c27f63c8-2dd9-4d5e-a5b2-6fbbf4f5c1a1', 'ACTIVE', 'local', 'SERVICE',
        CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (id) DO NOTHING;

INSERT INTO gaia_account_permissions (id, account_id, permission, createdat, lastupdatedat)
SELECT gen_random_uuid(), a.id, 'graphql.medusa.archiveGame',
       CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM gaia_accounts a
WHERE a.username = 'free-chess-worker'
ON CONFLICT (account_id, permission) DO NOTHING;
