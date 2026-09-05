-- V53 adds the high-contrast (light) theme.

-- Insert contrast theme (idempotent without requiring a unique constraint
-- on (owner_key, property_set, entry_name)).
INSERT INTO gaia_property_set_entries (id, owner_key, property_set, entry_name, values, configurable, status, createdat, lastupdatedat)
SELECT gen_random_uuid(), 'ReactApp', 'themes', 'contrast',
  '{
    "primary": "#111111",
    "primary-hover": "#222222",
    "primary-active": "#000000",
    "secondary": "#f5f5f5",
    "secondary-hover": "#e8e8e8",
    "accent": "#ffd600",
    "accent-hover": "#ffc400",
    "tertiary": "#111111",
    "tertiary-hover": "#333333",
    "muted": "#595959",
    "muted-foreground": "#737373",
    "very-visible": "1",
    "visible": ".8",
    "somewhat-visible": ".6",
    "barely-visible": ".4"
  }'::jsonb,
  true, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (
  SELECT 1 FROM gaia_property_set_entries
  WHERE owner_key = 'ReactApp' AND property_set = 'themes' AND entry_name = 'contrast'
);

UPDATE gaia_property_set_entries
SET values = '{
    "primary": "#111111",
    "primary-hover": "#222222",
    "primary-active": "#000000",
    "secondary": "#f5f5f5",
    "secondary-hover": "#e8e8e8",
    "accent": "#ffd600",
    "accent-hover": "#ffc400",
    "tertiary": "#111111",
    "tertiary-hover": "#333333",
    "muted": "#595959",
    "muted-foreground": "#737373",
    "very-visible": "1",
    "visible": ".8",
    "somewhat-visible": ".6",
    "barely-visible": ".4"
  }'::jsonb,
    configurable = true,
    status = 'ACTIVE',
    lastupdatedat = CURRENT_TIMESTAMP
WHERE owner_key = 'ReactApp' AND property_set = 'themes' AND entry_name = 'contrast';
