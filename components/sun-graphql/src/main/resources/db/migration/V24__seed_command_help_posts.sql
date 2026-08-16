-- V24 seeds BOT_HELP posts for each command.

INSERT INTO briareus_posts (id, title, content, tags, remote_object, type_id, language, createdat, lastupdatedat)
VALUES (
  'b9f70000-0000-4000-8000-000000000010',
  'texts',
  E'**List reader texts**\n\nFilter and sort your reading list with natural language.\n\n' ||
  E'**Filters:**\n' ||
  E'- `level a1` or `level a1 or b2` - CEFR level (A1, A2, B1, B2, C1, C2)\n' ||
  E'- `title has summer` - search by title\n' ||
  E'- `language greek` - filter by language\n\n' ||
  E'**Sorting:**\n' ||
  E'- `sort alphabetically` or `sort by title`\n' ||
  E'- `sort newest` - most recent first\n' ||
  E'- `sort by difficulty` - CEFR level ascending\n\n' ||
  E'**Pagination:** `page 2` or `σελίδα 2`\n\n' ||
  E'**Examples:**\n' ||
  E'- `ns texts level a1 sort by title`\n' ||
  E'- `ns texts language greek page 2`\n' ||
  E'- `ns texts level b1 about summer`',
  '["bot-help"]',
  '["niece-scarlett:command_help:texts"]',
  'b9f70000-0000-4000-8000-000000000002',
  'en',
  CURRENT_TIMESTAMP,
  CURRENT_TIMESTAMP
)
ON CONFLICT (id) DO NOTHING;

INSERT INTO briareus_posts (id, title, content, tags, remote_object, type_id, language, createdat, lastupdatedat)
VALUES (
  'b9f70000-0000-4000-8000-000000000011',
  'classify',
  E'**Predict the CEFR level of a text**\n\n' ||
  E'Three ways to use it:\n' ||
  E'- **Inline:** `ns classify some text here`\n' ||
  E'- **Reply:** reply to any message with `ns classify`\n' ||
  E'- **Slash:** `/classify text:some text here`\n\n' ||
  E'Returns the estimated CEFR level (A1-C2) with confidence scores.',
  '["bot-help"]',
  '["niece-scarlett:command_help:classify"]',
  'b9f70000-0000-4000-8000-000000000002',
  'en',
  CURRENT_TIMESTAMP,
  CURRENT_TIMESTAMP
)
ON CONFLICT (id) DO NOTHING;

INSERT INTO briareus_posts (id, title, content, tags, remote_object, type_id, language, createdat, lastupdatedat)
VALUES (
  'b9f70000-0000-4000-8000-000000000012',
  'define',
  E'**Define a Greek word from WordReference**\n\n' ||
  E'`ns define <word>` looks up the word and shows translations.\n\n' ||
  E'**Scope options** (comma-separated):\n' ||
  E'- `with examples` - include usage examples\n' ||
  E'- `all translations` - show every translation\n' ||
  E'- `compounds` - include compound forms\n' ||
  E'- `related` - include related words\n\n' ||
  E'**Example:** `ns define γεια with examples, related`',
  '["bot-help"]',
  '["niece-scarlett:command_help:define"]',
  'b9f70000-0000-4000-8000-000000000002',
  'en',
  CURRENT_TIMESTAMP,
  CURRENT_TIMESTAMP
)
ON CONFLICT (id) DO NOTHING;

INSERT INTO briareus_posts (id, title, content, tags, remote_object, type_id, language, createdat, lastupdatedat)
VALUES (
  'b9f70000-0000-4000-8000-000000000013',
  'pronounce',
  E'**Play the Greek pronunciation of a word**\n\n' ||
  E'`ns pronounce <word>` fetches the pronunciation from Forvo and sends it as an audio clip.\n\n' ||
  E'If no pronunciation is available, you will be told.',
  '["bot-help"]',
  '["niece-scarlett:command_help:pronounce"]',
  'b9f70000-0000-4000-8000-000000000002',
  'en',
  CURRENT_TIMESTAMP,
  CURRENT_TIMESTAMP
)
ON CONFLICT (id) DO NOTHING;

INSERT INTO briareus_posts (id, title, content, tags, remote_object, type_id, language, createdat, lastupdatedat)
VALUES (
  'b9f70000-0000-4000-8000-000000000014',
  'lt',
  E'**Explain language transfer**\n\n' ||
  E'Language Transfer is a free audio course for learning Greek (and other languages).\n\n' ||
  E'Use `/lt` or `ns lt` to see the FAQ.',
  '["bot-help"]',
  '["niece-scarlett:command_help:lt"]',
  'b9f70000-0000-4000-8000-000000000002',
  'en',
  CURRENT_TIMESTAMP,
  CURRENT_TIMESTAMP
)
ON CONFLICT (id) DO NOTHING;

INSERT INTO briareus_posts (id, title, content, tags, remote_object, type_id, language, createdat, lastupdatedat)
VALUES (
  'b9f70000-0000-4000-8000-000000000015',
  'text',
  E'**Locate and read a text**\n\n' ||
  E'`ns text <id or title search>` opens a view for the matching text.',
  '["bot-help"]',
  '["niece-scarlett:command_help:text"]',
  'b9f70000-0000-4000-8000-000000000002',
  'en',
  CURRENT_TIMESTAMP,
  CURRENT_TIMESTAMP
)
ON CONFLICT (id) DO NOTHING;

INSERT INTO briareus_posts (id, title, content, tags, remote_object, type_id, language, createdat, lastupdatedat)
VALUES (
  'b9f70000-0000-4000-8000-000000000016',
  'reload',
  E'**Flush the cached runtime command config**\n\n' ||
  E'Reloads the command-intents property set from the database.',
  '["bot-help"]',
  '["niece-scarlett:command_help:reload"]',
  'b9f70000-0000-4000-8000-000000000002',
  'en',
  CURRENT_TIMESTAMP,
  CURRENT_TIMESTAMP
)
ON CONFLICT (id) DO NOTHING;
