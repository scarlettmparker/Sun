-- V3 grants Super Admin to the dev local account. Linked accounts (same
-- person_id) inherit, so only one grant is needed per person.

INSERT INTO gaia_account_roles (account_id, role_id)
SELECT a.id, r.id
FROM gaia_accounts a, gaia_roles r
WHERE a.username = 'scarlett'
  AND r.name = 'Super Admin'
ON CONFLICT (account_id, role_id) DO NOTHING;
