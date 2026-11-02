-- V66 grants Free Chess users access to rating queries.

INSERT INTO gaia_role_permissions (id, role_id, permission, createdat, lastupdatedat)
SELECT gen_random_uuid(), r.id, p.perm, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM gaia_roles r,
     (VALUES
       ('graphql.medusa.playerRatings'),
       ('graphql.medusa.myRating'),
       ('graphql.medusa.leaderboard')
     ) AS p(perm)
WHERE r.name = 'Free Chess User'
ON CONFLICT (role_id, permission) DO NOTHING;

INSERT INTO gaia_account_permissions (id, account_id, permission, createdat, lastupdatedat)
SELECT gen_random_uuid(), a.id, p.perm, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM gaia_accounts a,
     (VALUES
       ('graphql.medusa.playerRatings'),
       ('graphql.medusa.myRating'),
       ('graphql.medusa.leaderboard')
     ) AS p(perm)
WHERE a.username = 'free-chess-worker'
ON CONFLICT (account_id, permission) DO NOTHING;
