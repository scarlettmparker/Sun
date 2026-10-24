-- V59 grants subtitle permissions.

INSERT INTO gaia_role_permissions (id, role_id, permission, createdat, lastupdatedat)
SELECT gen_random_uuid(), rp.role_id, perm, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM gaia_role_permissions rp
CROSS JOIN (VALUES
  ('graphql.dionysus.subtitles'),
  ('graphql.dionysus.searchSubtitles'),
  ('graphql.dionysus.fetchSubtitle'),
  ('graphql.dionysus.deleteSubtitle')
) AS p(perm)
WHERE rp.permission = 'graphql.dionysus.listKeys'
ON CONFLICT (role_id, permission) DO NOTHING;

INSERT INTO gaia_account_permissions (id, account_id, permission, createdat, lastupdatedat)
SELECT gen_random_uuid(), ap.account_id, perm, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM gaia_account_permissions ap
CROSS JOIN (VALUES
  ('graphql.dionysus.subtitles'),
  ('graphql.dionysus.searchSubtitles'),
  ('graphql.dionysus.fetchSubtitle'),
  ('graphql.dionysus.deleteSubtitle')
) AS p(perm)
WHERE ap.permission = 'graphql.dionysus.listKeys'
ON CONFLICT (account_id, permission) DO NOTHING;
