-- V52 re-seeds ReactApp themes as configurable (4 total) and ensures reader-level-colours are configurable.
-- Uses WHERE NOT EXISTS for idempotency, then ensures configurable=true and status=ACTIVE.

-- Ensure themes exist and are configurable
INSERT INTO gaia_property_set_entries (id, owner_key, property_set, entry_name, values, configurable, status, createdat, lastupdatedat)
SELECT gen_random_uuid(), 'ReactApp', 'themes', 'sea',
  '{"primary":"#1d4ed8","primary-hover":"#3b82f6","primary-active":"#1e3a8a","secondary":"#ffffff","secondary-hover":"#f5f5f5","accent":"#bfdbfe","accent-hover":"#dbeafe","tertiary":"#6d28d9","tertiary-hover":"#7c3aed"}'::jsonb,
  true, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM gaia_property_set_entries WHERE owner_key='ReactApp' AND property_set='themes' AND entry_name='sea');

INSERT INTO gaia_property_set_entries (id, owner_key, property_set, entry_name, values, configurable, status, createdat, lastupdatedat)
SELECT gen_random_uuid(), 'ReactApp', 'themes', 'nature',
  '{"primary":"#166534","primary-hover":"#15803d","primary-active":"#14532d","secondary":"#ffffff","secondary-hover":"#f5f5f5","accent":"#dcfce7","accent-hover":"#bbf7d0","tertiary":"#ea580c","tertiary-hover":"#fb923c"}'::jsonb,
  true, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM gaia_property_set_entries WHERE owner_key='ReactApp' AND property_set='themes' AND entry_name='nature');

INSERT INTO gaia_property_set_entries (id, owner_key, property_set, entry_name, values, configurable, status, createdat, lastupdatedat)
SELECT gen_random_uuid(), 'ReactApp', 'themes', 'sun',
  '{"primary":"#d97706","primary-hover":"#f59e0b","primary-active":"#b45309","secondary":"#ffffff","secondary-hover":"#f5f5f5","accent":"#fef3c7","accent-hover":"#fde68a","tertiary":"#0d9488","tertiary-hover":"#14b8a6"}'::jsonb,
  true, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM gaia_property_set_entries WHERE owner_key='ReactApp' AND property_set='themes' AND entry_name='sun');

INSERT INTO gaia_property_set_entries (id, owner_key, property_set, entry_name, values, configurable, status, createdat, lastupdatedat)
SELECT gen_random_uuid(), 'ReactApp', 'themes', 'scarlet',
  '{"primary":"#d90429","primary-hover":"#fb3758","primary-active":"#a0031d","secondary":"#ffffff","secondary-hover":"#f5f5f5","accent":"#ffdaad","accent-hover":"#ffe3c2","tertiary":"#d03991","tertiary-hover":"#dc6aad"}'::jsonb,
  true, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM gaia_property_set_entries WHERE owner_key='ReactApp' AND property_set='themes' AND entry_name='scarlet');

UPDATE gaia_property_set_entries SET configurable=true, status='ACTIVE', lastupdatedat=CURRENT_TIMESTAMP
WHERE owner_key='ReactApp' AND property_set='themes' AND entry_name IN ('sea','nature','sun','scarlet');

-- Ensure reader-level-colours are configurable (8 entries)
UPDATE gaia_property_set_entries SET configurable=true, status='ACTIVE'
WHERE owner_key='ReactApp' AND property_set='reader-level-colours';
