-- V50 adds the nature theme and renames the greek theme to sea.
-- Nature uses forest greens for the primary range (darker for higher contrast
-- against white, same hue family) and an autumn orange for the tertiary range.

-- Rename greek -> sea (idempotent)
UPDATE gaia_property_set_entries
SET entry_name = 'sea',
    lastupdatedat = CURRENT_TIMESTAMP
WHERE owner_key = 'ReactApp'
  AND property_set = 'themes'
  AND entry_name = 'greek';

-- Insert nature theme (forest green primary, autumn orange tertiary)
-- Uses ON CONFLICT to be re-runnable; values are the CSS custom property map
-- applied by @sun/themes (primary -> --primary etc).
INSERT INTO gaia_property_set_entries (id, owner_key, property_set, entry_name, values, configurable, status, createdat, lastupdatedat)
VALUES (
  gen_random_uuid(),
  'ReactApp',
  'themes',
  'nature',
  '{
    "primary": "#166534",
    "primary-hover": "#15803d",
    "primary-active": "#14532d",
    "secondary": "#ffffff",
    "secondary-hover": "#f5f5f5",
    "accent": "#dcfce7",
    "accent-hover": "#bbf7d0",
    "tertiary": "#ea580c",
    "tertiary-hover": "#fb923c"
  }',
  true,
  'ACTIVE',
  CURRENT_TIMESTAMP,
  CURRENT_TIMESTAMP
)
ON CONFLICT (owner_key, property_set, entry_name) DO UPDATE
SET values = EXCLUDED.values,
    lastupdatedat = CURRENT_TIMESTAMP;
