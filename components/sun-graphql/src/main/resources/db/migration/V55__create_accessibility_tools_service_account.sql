-- V55 creates the Accessibility-Tools service account (used by the browser extension
-- via X-Api-Key) and grants the dictionary lookup permission.

INSERT INTO fates_people (id, first_name, last_name, display_name, createdat, lastupdatedat)
VALUES ('1147443c-6b65-4ee6-9ea0-041588e3c96d', 'Accessibility', 'Tools', 'Accessibility Tools',
        CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (id) DO NOTHING;

INSERT INTO gaia_accounts (id, username, password_hash, person_id, status, provider, account_type,
                           createdat, lastupdatedat)
VALUES ('9b11a1d1-0c2b-4bb3-862d-ba355a6c8b5e', 'accessibility-tools', '!',
        '1147443c-6b65-4ee6-9ea0-041588e3c96d', 'ACTIVE', 'local', 'SERVICE',
        CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (id) DO NOTHING;

INSERT INTO gaia_account_permissions (id, account_id, permission, createdat, lastupdatedat)
SELECT gen_random_uuid(), a.id, 'graphql.hades.defineWord',
       CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM gaia_accounts a
WHERE a.username = 'accessibility-tools'
ON CONFLICT (account_id, permission) DO NOTHING;
