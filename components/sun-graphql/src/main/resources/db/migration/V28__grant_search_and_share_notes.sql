-- Grants learner the search and bulk share permissions for private notes.

INSERT INTO gaia_role_permissions (id, role_id, permission, createdat, lastupdatedat)
SELECT gen_random_uuid(), r.id, p.perm, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM gaia_roles r,
     (VALUES
       ('graphql.hades.searchReaderAccounts'),
       ('graphql.hades.shareNotes')
     ) AS p(perm)
WHERE r.name = 'learner'
ON CONFLICT (role_id, permission) DO NOTHING;
