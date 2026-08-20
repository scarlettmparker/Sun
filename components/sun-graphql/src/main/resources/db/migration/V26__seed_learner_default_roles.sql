-- V26 seeds the default learner role (DISCORD) and its default-role mapping.
-- Learner gets all hades/icarus read + create/vote for annotations/comments,
-- plus defineWord/classify, but NOT admin: createText/createSource/archiveText/attachObject,
-- nor gaia admin (accounts, ipWhitelist, tailscale, suspend/unsuspend, property mutations).

-- 1. Learner role
INSERT INTO gaia_roles (id, name, description, createdat, lastupdatedat)
VALUES ('22222222-2222-2222-2222-222222222222', 'learner', 'Default Guided Reader learner - Discord users', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (name) DO NOTHING;

-- 2. Learner permissions - hades (text read, annotation/comment create/vote, defineWord, private notes)
INSERT INTO gaia_role_permissions (id, role_id, permission, createdat, lastupdatedat)
SELECT gen_random_uuid(), r.id, p.perm, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM gaia_roles r,
     (VALUES
       ('graphql.hades.texts'),
       ('graphql.hades.text'),
       ('graphql.hades.classifyTextLevel'),
       ('graphql.hades.defineWord'),
       ('graphql.hades.source'),
       ('graphql.hades.sources'),
       ('graphql.hades.annotations'),
       ('graphql.hades.annotation'),
       ('graphql.hades.comments'),
       ('graphql.hades.readerAccounts'),
       ('graphql.hades.myVote'),
       ('graphql.hades.locateRemoteObjects'),
       ('graphql.hades.createAnnotation'),
       ('graphql.hades.editAnnotation'),
       ('graphql.hades.deleteAnnotation'),
       ('graphql.hades.addComment'),
       ('graphql.hades.editComment'),
       ('graphql.hades.deleteComment'),
       ('graphql.hades.vote'),
       ('graphql.hades.removeVote'),
       -- private notes (future, guarded by same learner perm)
       ('graphql.hades.privateNotes'),
       ('graphql.hades.createPrivateNote'),
       ('graphql.hades.deletePrivateNote'),
       ('graphql.hades.sharePrivateNote'),
       -- viewed / versioning
       ('graphql.hades.viewedTexts'),
       ('graphql.hades.textVersions'),
       ('graphql.hades.markViewed'),
       ('graphql.hades.editText'),
       -- icarus discussion
       ('graphql.icarus.thread'),
       ('graphql.icarus.threads'),
       ('graphql.icarus.threadsFor'),
       ('graphql.icarus.posts'),
       ('graphql.icarus.myVote'),
       ('graphql.icarus.locateRemoteObjects'),
       ('graphql.icarus.createThread'),
       ('graphql.icarus.createPost'),
       ('graphql.icarus.deletePost'),
       ('graphql.icarus.vote'),
       ('graphql.icarus.removeVote')
     ) AS p(perm)
WHERE r.name = 'learner'
ON CONFLICT (role_id, permission) DO NOTHING;

-- Explicitly NOT granted to learner (admin): graphql.hades.createText, createSource, archiveText, attachObject,
-- graphql.gaia.accounts, graphql.gaia.account, graphql.gaia.listAccounts, graphql.gaia.suspendAccount, unsuspendAccount,
-- graphql.gaia.ipWhitelist, graphql.gaia.configuration(s), graphql.gaia.tailscale*, upsertPropertyEntry, setProperty, etc.

-- 3. Property set schema for default roles (GuidedReader / reader-default-roles)
INSERT INTO gaia_property_set_schemas (id, owner_key, name, properties, configurable, status, createdat, lastupdatedat)
VALUES (gen_random_uuid(), 'GuidedReader', 'reader-default-roles',
        '{"DISCORD": {"type": "array", "items": {"type": "string"}}, "default": {"type": "array", "items": {"type": "string"}}}',
        TRUE, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT DO NOTHING;

-- 4. Property set schema for reader settings (hideThreshold, wordCacheTtl)
INSERT INTO gaia_property_set_schemas (id, owner_key, name, properties, configurable, status, createdat, lastupdatedat)
VALUES (gen_random_uuid(), 'GuidedReader', 'reader-settings',
        '{"hideThreshold": {"type": "number"}, "wordCacheTtl": {"type": "string"}}',
        TRUE, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT DO NOTHING;

-- 5. Default roles entry: DISCORD -> ["learner"] (idempotent via NOT EXISTS)
INSERT INTO gaia_property_set_entries (id, owner_key, property_set, entry_name, values, configurable, status, createdat, lastupdatedat)
SELECT gen_random_uuid(), 'GuidedReader', 'reader-default-roles', 'DISCORD',
        '{"roles": ["learner"]}', TRUE, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM gaia_property_set_entries WHERE owner_key='GuidedReader' AND property_set='reader-default-roles' AND entry_name='DISCORD');

-- 6. Default hideThreshold = -3
INSERT INTO gaia_property_set_entries (id, owner_key, property_set, entry_name, values, configurable, status, createdat, lastupdatedat)
SELECT gen_random_uuid(), 'GuidedReader', 'reader-settings', 'hideThreshold',
        '{"value": -3}', TRUE, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM gaia_property_set_entries WHERE owner_key='GuidedReader' AND property_set='reader-settings' AND entry_name='hideThreshold');
