-- V30 blog-as-knowledge graph: tree (parent_id) + GIN indexes for remote_object/tags.

ALTER TABLE briareus_posts ADD COLUMN IF NOT EXISTS parent_id UUID REFERENCES briareus_posts(id) ON DELETE SET NULL;

CREATE INDEX IF NOT EXISTS idx_briareus_posts_parent ON briareus_posts(parent_id);

CREATE INDEX IF NOT EXISTS idx_briareus_posts_remote_object ON briareus_posts USING GIN (remote_object);

CREATE INDEX IF NOT EXISTS idx_briareus_posts_tags ON briareus_posts USING GIN (tags);

DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM pg_constraint WHERE conname = 'chk_briareus_posts_no_self_parent'
  ) THEN
    ALTER TABLE briareus_posts ADD CONSTRAINT chk_briareus_posts_no_self_parent CHECK (parent_id IS NULL OR parent_id <> id);
  END IF;
END $$;
