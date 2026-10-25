-- V60 seeds Free Chess roles and their permission patterns.

INSERT INTO gaia_roles (id, name, description, createdat, lastupdatedat)
VALUES
  ('e175a67f-e12b-4c75-b801-a6da8cea55cd', 'Free Chess User', 'Default Free Chess player', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  ('e2b73635-f453-49b2-a8b7-35885e255cd6', 'Free Chess Admin', 'Free Chess administrator', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (name) DO NOTHING;

-- Free Chess User permissions
INSERT INTO gaia_role_permissions (id, role_id, permission, createdat, lastupdatedat)
SELECT gen_random_uuid(), r.id, p.perm, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM gaia_roles r,
     (VALUES
       ('graphql.medusa.listPieces'),
       ('graphql.medusa.getPiece'),
       ('graphql.medusa.listPieceSets'),
       ('graphql.medusa.getPieceSet'),
       ('graphql.medusa.listBoardLayouts'),
       ('graphql.medusa.getBoardLayout'),
       ('graphql.medusa.auditLogs'),
       ('graphql.gaia.me'),
       ('graphql.gaia.changePassword')
     ) AS p(perm)
WHERE r.name = 'Free Chess User'
ON CONFLICT (role_id, permission) DO NOTHING;

-- Free Chess Admin permissions (superset)
INSERT INTO gaia_role_permissions (id, role_id, permission, createdat, lastupdatedat)
SELECT gen_random_uuid(), r.id, p.perm, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM gaia_roles r,
     (VALUES
       ('graphql.medusa.*'),
       ('graphql.gaia.accounts'),
       ('graphql.gaia.account'),
       ('graphql.gaia.listAccounts'),
       ('graphql.gaia.suspendAccount'),
       ('graphql.gaia.unsuspendAccount'),
       ('graphql.gaia.setAccountPassword'),
       ('graphql.gaia.deleteAccount'),
       ('graphql.gaia.setAccountRoles'),
       ('graphql.gaia.setAccountPermissions'),
       ('graphql.gaia.rolePermissions'),
       ('graphql.gaia.setRolePermissions'),
       ('graphql.audit.*')
     ) AS p(perm)
WHERE r.name = 'Free Chess Admin'
ON CONFLICT (role_id, permission) DO NOTHING;