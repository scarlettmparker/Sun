-- V41 seeds STUDY blog post type for Jocasta.
INSERT INTO briareus_blog_post_types (id, name, description, createdat, lastupdatedat)
VALUES (gen_random_uuid(), 'STUDY', 'Study attempt with questions', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (name) DO NOTHING;
