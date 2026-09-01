-- V41 seeds STUDY blog post type for Jocasta.
INSERT INTO briareus_blog_post_types (name, description, createdat, lastupdatedat)
VALUES ('STUDY', 'Study attempt with questions', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (name) DO NOTHING;
