-- V31 seeds knowledge hub post types used by Sun entry + DOCS tree.

INSERT INTO briareus_blog_post_types (id, name, description, createdat, lastupdatedat)
VALUES
  (gen_random_uuid(), 'KNOWLEDGE', 'General knowledge note - philosophy, ideas, evergreen', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (gen_random_uuid(), 'REVIEW', 'Music/book/etc. review with rating attributes', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (gen_random_uuid(), 'DOCS', 'In-house documentation (plan, architecture, runbook)', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (name) DO NOTHING;
