-- V15 grants the Niece-Scarlett service account access to the WordReference
-- dictionary lookup used by the bot's define command.

INSERT INTO gaia_account_permissions (id, account_id, permission, createdat, lastupdatedat)
SELECT gen_random_uuid(), a.id, 'graphql.hades.defineWord',
       CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM gaia_accounts a
WHERE a.username = 'niece-scarlett'
ON CONFLICT (account_id, permission) DO NOTHING;
