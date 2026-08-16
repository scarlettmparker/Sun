-- V23 creates the BOT_HELP blog post type for command help content.

INSERT INTO briareus_blog_post_types (id, name, description, createdat, lastupdatedat)
VALUES (
  'b9f70000-0000-4000-8000-000000000002',
  'BOT_HELP',
  'Niece-Scarlett bot command help content',
  CURRENT_TIMESTAMP,
  CURRENT_TIMESTAMP
)
ON CONFLICT (name) DO NOTHING;
