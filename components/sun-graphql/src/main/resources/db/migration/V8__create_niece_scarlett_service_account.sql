-- V8 creates the Niece-Scarlett service account (used by the Niece-Scarlett
-- Discord bot via X-Api-Key) and grants the permissions it needs: reading
-- reader texts and editing the language-transfer property set.

INSERT INTO fates_people (id, first_name, last_name, display_name, createdat, lastupdatedat)
VALUES ('9f7f0000-0000-4000-8000-000000000001', 'Niece', 'Scarlett', 'Niece Scarlett',
        CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO gaia_accounts (id, username, password_hash, person_id, status, provider, account_type,
                           createdat, lastupdatedat)
VALUES ('9f7f0000-0000-4000-8000-000000000002', 'niece-scarlett', '!',
        '9f7f0000-0000-4000-8000-000000000001', 'ACTIVE', 'local', 'SERVICE',
        CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO gaia_account_permissions (id, account_id, permission, createdat, lastupdatedat)
SELECT gen_random_uuid(), a.id, p.permission, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM gaia_accounts a,
     (VALUES ('graphql.hades.texts'),
             ('graphql.hades.text'),
             ('graphql.hades.sources'),
             ('graphql.gaia.upsertPropertyEntry'),
             ('graphql.gaia.setProperty')) AS p(permission)
WHERE a.username = 'niece-scarlett'
ON CONFLICT (account_id, permission) DO NOTHING;
