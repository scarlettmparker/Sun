-- V16 adds the define command intent for the Niece-Scarlett bot.

INSERT INTO gaia_property_set_entries (id, owner_key, property_set, entry_name, values,
                                       configurable, status, createdat, lastupdatedat)
SELECT gen_random_uuid(), 'NieceScarlett', 'command-intents', 'define',
       '{"command": "define", "words": ["define", "definition", "meaning", "mean", "dictionary", "explain"]}',
       TRUE, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM gaia_property_set_entries
                  WHERE owner_key = 'NieceScarlett'
                    AND property_set = 'command-intents'
                    AND entry_name = 'define');
