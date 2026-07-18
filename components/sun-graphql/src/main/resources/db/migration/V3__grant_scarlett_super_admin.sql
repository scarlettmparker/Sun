-- V3 grants Super Admin to the dev Discord account.

INSERT INTO gaia_account_roles (account_id, role_id)
SELECT a.id, r.id
FROM gaia_accounts a, gaia_roles r
WHERE a.provider = 'discord'
  AND a.provider_id = '1310531013700751441'
  AND r.name = 'Super Admin'
ON CONFLICT (account_id, role_id) DO NOTHING;
