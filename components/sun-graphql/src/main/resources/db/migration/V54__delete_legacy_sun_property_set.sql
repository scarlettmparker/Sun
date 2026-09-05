-- V54 deletes the legacy "sun" property set. The "themes" property set is the
-- only source of truth for ReactApp themes (see V52/V53); the "sun" set held
-- byte-identical copies of the same four entries and shadowed "themes" in
-- readers that preferred it.
DELETE FROM gaia_property_set_entries
WHERE owner_key = 'ReactApp'
  AND property_set = 'sun';
