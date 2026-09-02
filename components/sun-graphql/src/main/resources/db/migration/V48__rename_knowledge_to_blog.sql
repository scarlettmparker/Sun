-- V48 renames Knowledge owner to Blog for review attributes.

UPDATE gaia_property_set_schemas SET owner_key = 'Blog' WHERE owner_key = 'Knowledge';
UPDATE gaia_property_set_entries SET owner_key = 'Blog' WHERE owner_key = 'Knowledge';
