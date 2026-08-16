-- V19 adds a reload entry to the command-intents propertyset so the flush
-- command has a runtime permission, removing the need for a code constant.

INSERT INTO gaia_property_set_entries (id, owner_key, property_set, entry_name, values,
                                       configurable, status, createdat, lastupdatedat)
SELECT gen_random_uuid(), 'NieceScarlett', 'command-intents', 'reload',
       '{"command": "reload", "permission": "bot.commands.reload"}',
       TRUE, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM gaia_property_set_entries
                  WHERE owner_key = 'NieceScarlett'
                    AND property_set = 'command-intents'
                    AND entry_name = 'reload');
