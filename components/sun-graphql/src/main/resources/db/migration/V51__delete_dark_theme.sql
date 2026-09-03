-- V51 deletes the dark theme (replaced by sea/nature).
DELETE FROM gaia_property_set_entries
WHERE owner_key = 'ReactApp'
  AND property_set = 'themes'
  AND entry_name = 'dark';
