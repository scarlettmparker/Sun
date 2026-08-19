-- V25 seeds a BOT_HELP post for the annotations command.

INSERT INTO briareus_posts (id, title, content, tags, remote_object, type_id, language, createdat, lastupdatedat)
VALUES (
  'b9f70000-0000-4000-8000-000000000017',
  'annotations',
  E'**List annotations for a text**\n\n' ||
  E'`ns annotations for <text title or id>` shows all annotations for a text.\n\n' ||
  E'**Sorting:**\n' ||
  E'- `sort by newest` or `sort by created` - most recent first\n' ||
  E'- `sort by updated` - recently edited first\n' ||
  E'- `sort by popular` or `sort by votes` - most upvoted first\n\n' ||
  E'**Include hidden:** add `include hidden` to show hidden annotations.\n\n' ||
  E'**Examples:**\n' ||
  E'- `ns annotations for κείμενο 1`\n' ||
  E'- `ns annotations for text (uuid) sort by newest`\n' ||
  E'- `ns annotations for (uuid) include hidden`',
  '["bot-help"]',
  '["niece-scarlett:command_help:annotations"]',
  'b9f70000-0000-4000-8000-000000000002',
  'en',
  CURRENT_TIMESTAMP,
  CURRENT_TIMESTAMP
)
ON CONFLICT (id) DO NOTHING;
