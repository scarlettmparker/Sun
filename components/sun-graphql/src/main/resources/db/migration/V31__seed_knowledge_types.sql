-- V31 seeds knowledge hub post types used by Sun entry + DOCS tree.

INSERT INTO briareus_blog_post_types (name, description, createdat, lastupdatedat)
VALUES
  ('KNOWLEDGE', 'General knowledge note - philosophy, ideas, evergreen', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  ('REVIEW', 'Music/book/etc. review with rating attributes', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  ('DOCS', 'In-house documentation (plan, architecture, runbook)', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (name) DO NOTHING;
