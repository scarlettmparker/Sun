-- V49 adds index for gallery image_path lookup used by blog detail cover handling.

CREATE INDEX IF NOT EXISTS idx_cerberus_gallery_items_image_path
  ON cerberus_gallery_items (image_path);
