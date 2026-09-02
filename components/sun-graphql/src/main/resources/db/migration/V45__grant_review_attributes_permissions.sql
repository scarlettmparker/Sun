-- V45 grants property-set permissions for review attributes and gallery meta anchored on briareus locate.

INSERT INTO gaia_role_permissions (id, role_id, permission, createdat, lastupdatedat)
SELECT gen_random_uuid(), rp.role_id, perm, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM gaia_role_permissions rp
CROSS JOIN (VALUES
  ('graphql.gaia.propertySets'),
  ('graphql.gaia.propertySet'),
  ('graphql.gaia.propertySetSchema'),
  ('graphql.gaia.upsertPropertyEntry'),
  ('graphql.gaia.setProperty'),
  ('graphql.gaia.registerPropertySetSchema')
) AS p(perm)
WHERE rp.permission = 'graphql.briareus.locateBlogPost'
ON CONFLICT (role_id, permission) DO NOTHING;

INSERT INTO gaia_account_permissions (id, account_id, permission, createdat, lastupdatedat)
SELECT gen_random_uuid(), ap.account_id, perm, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM gaia_account_permissions ap
CROSS JOIN (VALUES
  ('graphql.gaia.propertySets'),
  ('graphql.gaia.propertySet'),
  ('graphql.gaia.propertySetSchema'),
  ('graphql.gaia.upsertPropertyEntry'),
  ('graphql.gaia.setProperty'),
  ('graphql.gaia.registerPropertySetSchema')
) AS p(perm)
WHERE ap.permission = 'graphql.briareus.locateBlogPost'
ON CONFLICT (account_id, permission) DO NOTHING;

-- Cerberus gallery permissions for file/gallery attach (reuse Filestore bucket listing pattern)
INSERT INTO gaia_role_permissions (id, role_id, permission, createdat, lastupdatedat)
SELECT gen_random_uuid(), rp.role_id, perm, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM gaia_role_permissions rp
CROSS JOIN (VALUES
  ('graphql.cerberus.listGalleryItems'),
  ('graphql.cerberus.createGalleryItem'),
  ('graphql.cerberus.deleteGalleryItem')
) AS p(perm)
WHERE rp.permission = 'graphql.briareus.locateBlogPost'
ON CONFLICT (role_id, permission) DO NOTHING;

INSERT INTO gaia_account_permissions (id, account_id, permission, createdat, lastupdatedat)
SELECT gen_random_uuid(), ap.account_id, perm, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM gaia_account_permissions ap
CROSS JOIN (VALUES
  ('graphql.cerberus.listGalleryItems'),
  ('graphql.cerberus.createGalleryItem'),
  ('graphql.cerberus.deleteGalleryItem')
) AS p(perm)
WHERE ap.permission = 'graphql.briareus.locateBlogPost'
ON CONFLICT (account_id, permission) DO NOTHING;

-- Dionysus presigned URL permissions for Garage attach
INSERT INTO gaia_role_permissions (id, role_id, permission, createdat, lastupdatedat)
SELECT gen_random_uuid(), rp.role_id, perm, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM gaia_role_permissions rp
CROSS JOIN (VALUES
  ('graphql.dionysus.getPresignedUploadUrl'),
  ('graphql.dionysus.getPresignedDownloadUrl')
) AS p(perm)
WHERE rp.permission = 'graphql.briareus.locateBlogPost'
ON CONFLICT (role_id, permission) DO NOTHING;

INSERT INTO gaia_account_permissions (id, account_id, permission, createdat, lastupdatedat)
SELECT gen_random_uuid(), ap.account_id, perm, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM gaia_account_permissions ap
CROSS JOIN (VALUES
  ('graphql.dionysus.getPresignedUploadUrl'),
  ('graphql.dionysus.getPresignedDownloadUrl')
) AS p(perm)
WHERE ap.permission = 'graphql.briareus.locateBlogPost'
ON CONFLICT (account_id, permission) DO NOTHING;
