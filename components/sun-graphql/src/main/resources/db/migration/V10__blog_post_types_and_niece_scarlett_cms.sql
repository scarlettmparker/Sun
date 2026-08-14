-- V10: blog post types + language for the blogsite CMS, plus the Niece-Scarlett
-- bot's command-intent and blog-backed FAQ property sets.

CREATE TABLE IF NOT EXISTS briareus_blog_post_types (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name         VARCHAR(255) NOT NULL UNIQUE,
    description  VARCHAR(255),
    createdat    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    lastupdatedat TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by   UUID,
    last_updated_by UUID
);

ALTER TABLE briareus_posts ADD COLUMN IF NOT EXISTS type_id UUID;
ALTER TABLE briareus_posts ADD COLUMN IF NOT EXISTS language VARCHAR(255);

-- BOT_FAQ post type (Niece-Scarlett bot FAQ content).
INSERT INTO briareus_blog_post_types (id, name, description, createdat, lastupdatedat)
VALUES ('b9f70000-0000-4000-8000-000000000001', 'BOT_FAQ',
        'Niece-Scarlett bot FAQ content', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (name) DO NOTHING;

-- English FAQ post.
INSERT INTO briareus_posts (id, title, content, tags, remote_object, type_id, language,
                            createdat, lastupdatedat)
VALUES (
    'b9f70000-0000-4000-8000-000000000002',
    'What is Language Transfer?',
    E'**What is Language Transfer?**\n\nLanguage Transfer is an audio series that teaches the basics of Modern Greek in a natural and easy-to-comprehend manner. It focuses on grammar and teaches useful vocabulary to prepare you for everyday conversations.\n\nIt''s highly encouraged to check it out, as it will help you build a very solid foundation to communicate in Greek.\n\n**The complete series can be found on:**\n\n- [YouTube](https://www.youtube.com/watch?v=dHsgJkV9J30&list=PLeA5t3dWTWvtWkl4oOV8J9SMB7L9N9Ogt)\n- [Soundcloud](https://soundcloud.com/languagetransfer/sets/complete-greek-more-audios)\n- [Transcript (PDF)](https://static1.squarespace.com/static/5c69bfa4f4e531370e74fa44/t/5d03d32873f6f10001a364b5/1560531782855/COMPLETE+GREEK+-+Transcripts_LT.pdf)\n\nThe audio series follows the teacher (Mihalis) as he teaches a student useful grammatical constructions and how to form sentences naturally, allowing you to follow along by putting yourself in the student''s shoes.\n\nMore useful resources can be found in [the resources channel](https://discord.com/channels/350234668680871946/359578025228107776/1132288734738522112), notably in the pins, to help you advance your Greek level after Language Transfer.\n\n*Ανιψιά Σκαρλέτα FAQ*',
    '["bot-faq", "language-transfer"]',
    '["niece-scarlett:command:lt"]',
    'b9f70000-0000-4000-8000-000000000001', 'en', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (id) DO NOTHING;

-- Greek FAQ post (placeholder until authored via the Sun app blog).
INSERT INTO briareus_posts (id, title, content, tags, remote_object, type_id, language,
                            createdat, lastupdatedat)
VALUES (
    'b9f70000-0000-4000-8000-000000000003',
    'Τι είναι το Language Transfer;',
    E'**Τι είναι το Language Transfer;**\n\nΤο ελληνικό περιεχόμενο θα προστεθεί σύντομα. Οι σύνδεσμοι του μαθήματος:\n\n- [YouTube](https://www.youtube.com/watch?v=dHsgJkV9J30&list=PLeA5t3dWTWvtWkl4oOV8J9SMB7L9N9Ogt)\n- [Soundcloud](https://soundcloud.com/languagetransfer/sets/complete-greek-more-audios)\n- [Transcript (PDF)](https://static1.squarespace.com/static/5c69bfa4f4e531370e74fa44/t/5d03d32873f6f10001a364b5/1560531782855/COMPLETE+GREEK+-+Transcripts_LT.pdf)\n\n*Ανιψιά Σκαρλέτα FAQ*',
    '["bot-faq", "language-transfer"]',
    '["niece-scarlett:command:lt"]',
    'b9f70000-0000-4000-8000-000000000001', 'el', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (id) DO NOTHING;

-- Niece-Scarlett property-set schemas.
INSERT INTO gaia_property_set_schemas (id, owner_key, name, properties, configurable, status,
                                       createdat, lastupdatedat)
VALUES
    (gen_random_uuid(), 'NieceScarlett', 'command-intents',
     '{"command": {"type": "string", "required": true}, "words": {"type": "array", "required": true}}',
     TRUE, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (gen_random_uuid(), 'NieceScarlett', 'language-transfer',
     '{"kind": {"type": "string", "required": true}, "content": {"type": "object", "required": false}, "typeName": {"type": "string", "required": false}, "language": {"type": "string", "required": false}, "remoteObject": {"type": "string", "required": false}, "oneOf": [["content", "typeName", "remoteObject"]]}',
     TRUE, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Command intents.
INSERT INTO gaia_property_set_entries (id, owner_key, property_set, entry_name, values,
                                       configurable, status, createdat, lastupdatedat)
VALUES
    (gen_random_uuid(), 'NieceScarlett', 'command-intents', 'texts',
     '{"command": "texts", "words": ["texts", "reader", "reading", "stories", "browse"]}',
     TRUE, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (gen_random_uuid(), 'NieceScarlett', 'command-intents', 'text',
     '{"command": "text", "words": ["text", "find", "search", "locate", "story", "read"]}',
     TRUE, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (gen_random_uuid(), 'NieceScarlett', 'command-intents', 'lt',
     '{"command": "lt", "words": ["language", "transfer", "greek", "course", "learn", "lt"]}',
     TRUE, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Replace the inline FAQ entries with blog-backed lookups.
DELETE FROM gaia_property_set_entries
WHERE owner_key = 'NieceScarlett' AND property_set = 'language-transfer';

INSERT INTO gaia_property_set_entries (id, owner_key, property_set, entry_name, values,
                                       configurable, status, createdat, lastupdatedat)
VALUES
    (gen_random_uuid(), 'NieceScarlett', 'language-transfer', 'faq.en',
     '{"kind": "blog", "typeName": "BOT_FAQ", "language": "en"}',
     TRUE, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (gen_random_uuid(), 'NieceScarlett', 'language-transfer', 'faq.el',
     '{"kind": "blog", "typeName": "BOT_FAQ", "language": "el"}',
     TRUE, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Read-only blog access for the Niece-Scarlett service account.
INSERT INTO gaia_account_permissions (id, account_id, permission, createdat, lastupdatedat)
SELECT gen_random_uuid(), a.id, p.permission, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM gaia_accounts a,
     (VALUES ('graphql.briareus.listBlogPosts'),
             ('graphql.briareus.locateBlogPost'),
             ('graphql.briareus.listByRemoteObjects'),
             ('graphql.briareus.blogPostTypes')) AS p(permission)
WHERE a.username = 'niece-scarlett'
ON CONFLICT (account_id, permission) DO NOTHING;
