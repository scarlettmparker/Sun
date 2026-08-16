-- V17 seeds runtime permission and rate-limit config into the command-intents
-- entries so they can be edited without code changes.

UPDATE gaia_property_set_entries
SET values = values::jsonb || '{
  "permission": "bot.commands.classify",
  "rateLimit": { "capacity": 5, "refillPerSecond": 0.25 }
}'::jsonb,
    lastupdatedat = CURRENT_TIMESTAMP
WHERE owner_key = 'NieceScarlett'
  AND property_set = 'command-intents'
  AND entry_name = 'classify';

UPDATE gaia_property_set_entries
SET values = values::jsonb || '{
  "permission": "bot.commands.define",
  "rateLimit": { "capacity": 1, "refillPerSecond": 0.1 }
}'::jsonb,
    lastupdatedat = CURRENT_TIMESTAMP
WHERE owner_key = 'NieceScarlett'
  AND property_set = 'command-intents'
  AND entry_name = 'define';

INSERT INTO gaia_property_set_entries (id, owner_key, property_set, entry_name, values,
                                       configurable, status, createdat, lastupdatedat)
SELECT gen_random_uuid(), 'NieceScarlett', 'command-intents', 'pronounce',
       '{
         "command": "pronounce",
         "permission": "bot.commands.pronounce",
         "rateLimit": { "capacity": 1, "refillPerSecond": 0.1 }
       }'::jsonb,
       TRUE, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM gaia_property_set_entries
                  WHERE owner_key = 'NieceScarlett'
                    AND property_set = 'command-intents'
                    AND entry_name = 'pronounce');
