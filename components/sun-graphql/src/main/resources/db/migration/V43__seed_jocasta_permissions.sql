-- V43 seeds jocasta graphql permissions anchored on briareus locate.
INSERT INTO gaia_role_permissions (id, role_id, permission, createdat, lastupdatedat)
SELECT gen_random_uuid(), rp.role_id, perm, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM gaia_role_permissions rp
CROSS JOIN (VALUES
  ('graphql.jocasta.listQuestions'),
  ('graphql.jocasta.locateQuestion'),
  ('graphql.jocasta.listAnswers'),
  ('graphql.jocasta.bulkCreateQuestions'),
  ('graphql.jocasta.submitAnswer'),
  ('graphql.jocasta.linkQuestion')
) AS p(perm)
WHERE rp.permission = 'graphql.briareus.locateBlogPost'
ON CONFLICT (role_id, permission) DO NOTHING;

INSERT INTO gaia_account_permissions (id, account_id, permission, createdat, lastupdatedat)
SELECT gen_random_uuid(), ap.account_id, perm, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM gaia_account_permissions ap
CROSS JOIN (VALUES
  ('graphql.jocasta.listQuestions'),
  ('graphql.jocasta.locateQuestion'),
  ('graphql.jocasta.listAnswers'),
  ('graphql.jocasta.bulkCreateQuestions'),
  ('graphql.jocasta.submitAnswer'),
  ('graphql.jocasta.linkQuestion')
) AS p(perm)
WHERE ap.permission = 'graphql.briareus.locateBlogPost'
ON CONFLICT (account_id, permission) DO NOTHING;
