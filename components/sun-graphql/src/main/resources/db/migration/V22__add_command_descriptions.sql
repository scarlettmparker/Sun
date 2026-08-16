-- V22 adds a description field to every command-intents entry so the help
-- command can render command listings without loading the bot registry.

UPDATE gaia_property_set_entries
SET values = values || '{"description": "Predict the CEFR level of a text"}'::jsonb,
    lastupdatedat = CURRENT_TIMESTAMP
WHERE owner_key = 'NieceScarlett'
  AND property_set = 'command-intents'
  AND entry_name = 'classify';

UPDATE gaia_property_set_entries
SET values = values || '{"description": "Define a Greek word from WordReference"}'::jsonb,
    lastupdatedat = CURRENT_TIMESTAMP
WHERE owner_key = 'NieceScarlett'
  AND property_set = 'command-intents'
  AND entry_name = 'define';

UPDATE gaia_property_set_entries
SET values = values || '{"description": "Explain language transfer"}'::jsonb,
    lastupdatedat = CURRENT_TIMESTAMP
WHERE owner_key = 'NieceScarlett'
  AND property_set = 'command-intents'
  AND entry_name = 'lt';

UPDATE gaia_property_set_entries
SET values = values || '{"description": "Play the Greek pronunciation of a word"}'::jsonb,
    lastupdatedat = CURRENT_TIMESTAMP
WHERE owner_key = 'NieceScarlett'
  AND property_set = 'command-intents'
  AND entry_name = 'pronounce';

UPDATE gaia_property_set_entries
SET values = values || '{"description": "Flush the cached runtime command config"}'::jsonb,
    lastupdatedat = CURRENT_TIMESTAMP
WHERE owner_key = 'NieceScarlett'
  AND property_set = 'command-intents'
  AND entry_name = 'reload';

UPDATE gaia_property_set_entries
SET values = values || '{"description": "Locate and read a text"}'::jsonb,
    lastupdatedat = CURRENT_TIMESTAMP
WHERE owner_key = 'NieceScarlett'
  AND property_set = 'command-intents'
  AND entry_name = 'text';

UPDATE gaia_property_set_entries
SET values = values || '{"description": "List reader texts"}'::jsonb,
    lastupdatedat = CURRENT_TIMESTAMP
WHERE owner_key = 'NieceScarlett'
  AND property_set = 'command-intents'
  AND entry_name = 'texts';
