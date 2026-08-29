-- V34 assigns scarlett as owner (created_by) for all blogs except help/faq.
DO $$
DECLARE
  scarlett_id UUID;
BEGIN
  SELECT id INTO scarlett_id FROM gaia_accounts WHERE username = 'scarlett' LIMIT 1;
  IF scarlett_id IS NOT NULL THEN
    UPDATE briareus_posts
    SET created_by = scarlett_id,
        last_updated_by = scarlett_id
    WHERE type_id NOT IN (
      SELECT id FROM briareus_blog_post_types WHERE name IN ('BOT_FAQ', 'BOT_HELP')
    )
    AND (created_by IS NULL OR created_by <> scarlett_id);
  END IF;
END $$;
