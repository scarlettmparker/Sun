-- V35 seeds unique permissions for wiki ingest and hades batch, plus wikipedia/wiktionary intents with 1/s rate limit.

-- graphql.briareus.wikipediaSummary (unique, anchored on listBlogPosts)
INSERT INTO gaia_role_permissions (id, role_id, permission, createdat, lastupdatedat)
SELECT gen_random_uuid(), rp.role_id, 'graphql.briareus.wikipediaSummary', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM gaia_role_permissions rp
WHERE rp.permission = 'graphql.briareus.listBlogPosts'
ON CONFLICT (role_id, permission) DO NOTHING;

INSERT INTO gaia_account_permissions (id, account_id, permission, createdat, lastupdatedat)
SELECT gen_random_uuid(), ap.account_id, 'graphql.briareus.wikipediaSummary', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM gaia_account_permissions ap
WHERE ap.permission = 'graphql.briareus.listBlogPosts'
ON CONFLICT (account_id, permission) DO NOTHING;

-- graphql.briareus.wiktionaryEntry (unique)
INSERT INTO gaia_role_permissions (id, role_id, permission, createdat, lastupdatedat)
SELECT gen_random_uuid(), rp.role_id, 'graphql.briareus.wiktionaryEntry', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM gaia_role_permissions rp
WHERE rp.permission = 'graphql.briareus.listBlogPosts'
ON CONFLICT (role_id, permission) DO NOTHING;

INSERT INTO gaia_account_permissions (id, account_id, permission, createdat, lastupdatedat)
SELECT gen_random_uuid(), ap.account_id, 'graphql.briareus.wiktionaryEntry', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM gaia_account_permissions ap
WHERE ap.permission = 'graphql.briareus.listBlogPosts'
ON CONFLICT (account_id, permission) DO NOTHING;

-- graphql.briareus.ingestBlogFromSource (unique, anchored on createBlogPost if present else listBlogPosts fallback)
INSERT INTO gaia_role_permissions (id, role_id, permission, createdat, lastupdatedat)
SELECT gen_random_uuid(), rp.role_id, 'graphql.briareus.ingestBlogFromSource', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM gaia_role_permissions rp
WHERE rp.permission = 'graphql.briareus.createBlogPost'
ON CONFLICT (role_id, permission) DO NOTHING;

INSERT INTO gaia_account_permissions (id, account_id, permission, createdat, lastupdatedat)
SELECT gen_random_uuid(), ap.account_id, 'graphql.briareus.ingestBlogFromSource', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM gaia_account_permissions ap
WHERE ap.permission = 'graphql.briareus.createBlogPost'
ON CONFLICT (account_id, permission) DO NOTHING;

-- fallback for ingest if no createBlogPost anchor exists (covers current DB where createBlogPost not yet seeded)
INSERT INTO gaia_role_permissions (id, role_id, permission, createdat, lastupdatedat)
SELECT gen_random_uuid(), rp.role_id, 'graphql.briareus.ingestBlogFromSource', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM gaia_role_permissions rp
WHERE rp.permission = 'graphql.briareus.listBlogPosts'
  AND NOT EXISTS (
    SELECT 1 FROM gaia_role_permissions rp2
    WHERE rp2.role_id = rp.role_id AND rp2.permission = 'graphql.briareus.ingestBlogFromSource'
  )
ON CONFLICT (role_id, permission) DO NOTHING;

INSERT INTO gaia_account_permissions (id, account_id, permission, createdat, lastupdatedat)
SELECT gen_random_uuid(), ap.account_id, 'graphql.briareus.ingestBlogFromSource', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM gaia_account_permissions ap
WHERE ap.permission = 'graphql.briareus.listBlogPosts'
  AND NOT EXISTS (
    SELECT 1 FROM gaia_account_permissions ap2
    WHERE ap2.account_id = ap.account_id AND ap2.permission = 'graphql.briareus.ingestBlogFromSource'
  )
ON CONFLICT (account_id, permission) DO NOTHING;

-- graphql.hades.locateReaderTexts (unique, anchored on hades.texts / locateRemoteObjects)
INSERT INTO gaia_role_permissions (id, role_id, permission, createdat, lastupdatedat)
SELECT gen_random_uuid(), rp.role_id, 'graphql.hades.locateReaderTexts', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM gaia_role_permissions rp
WHERE rp.permission = 'graphql.hades.texts'
ON CONFLICT (role_id, permission) DO NOTHING;

INSERT INTO gaia_account_permissions (id, account_id, permission, createdat, lastupdatedat)
SELECT gen_random_uuid(), ap.account_id, 'graphql.hades.locateReaderTexts', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM gaia_account_permissions ap
WHERE ap.permission = 'graphql.hades.texts'
ON CONFLICT (account_id, permission) DO NOTHING;

-- fallback for locateReaderTexts if anchored on texts not present for account
INSERT INTO gaia_role_permissions (id, role_id, permission, createdat, lastupdatedat)
SELECT gen_random_uuid(), rp.role_id, 'graphql.hades.locateReaderTexts', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM gaia_role_permissions rp
WHERE rp.permission = 'graphql.hades.locateRemoteObjects'
  AND NOT EXISTS (
    SELECT 1 FROM gaia_role_permissions rp2
    WHERE rp2.role_id = rp.role_id AND rp2.permission = 'graphql.hades.locateReaderTexts'
  )
ON CONFLICT (role_id, permission) DO NOTHING;

INSERT INTO gaia_account_permissions (id, account_id, permission, createdat, lastupdatedat)
SELECT gen_random_uuid(), ap.account_id, 'graphql.hades.locateReaderTexts', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM gaia_account_permissions ap
WHERE ap.permission = 'graphql.hades.locateRemoteObjects'
  AND NOT EXISTS (
    SELECT 1 FROM gaia_account_permissions ap2
    WHERE ap2.account_id = ap.account_id AND ap2.permission = 'graphql.hades.locateReaderTexts'
  )
ON CONFLICT (account_id, permission) DO NOTHING;

-- wikipedia intent (1/s rate limit for service/ui; not used as bot command yet)
INSERT INTO gaia_property_set_entries (id, owner_key, property_set, entry_name, values, configurable, status, createdat, lastupdatedat)
SELECT gen_random_uuid(), 'NieceScarlett', 'command-intents', 'wikipedia',
       '{"command": "wikipedia", "words": ["wikipedia", "wiki", "lookup", "summarise", "summarize"], "rateLimit": {"capacity": 1, "refillPerSecond": 1.0}}'::jsonb,
       TRUE, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (
  SELECT 1 FROM gaia_property_set_entries
  WHERE owner_key = 'NieceScarlett' AND property_set = 'command-intents' AND entry_name = 'wikipedia'
);

-- wiktionary intent (1/s)
INSERT INTO gaia_property_set_entries (id, owner_key, property_set, entry_name, values, configurable, status, createdat, lastupdatedat)
SELECT gen_random_uuid(), 'NieceScarlett', 'command-intents', 'wiktionary',
       '{"command": "wiktionary", "words": ["wiktionary", "definition", "define", "meaning", "etymology"], "rateLimit": {"capacity": 1, "refillPerSecond": 1.0}}'::jsonb,
       TRUE, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (
  SELECT 1 FROM gaia_property_set_entries
  WHERE owner_key = 'NieceScarlett' AND property_set = 'command-intents' AND entry_name = 'wiktionary'
);
