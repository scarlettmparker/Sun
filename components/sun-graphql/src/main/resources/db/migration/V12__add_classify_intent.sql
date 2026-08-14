-- V12 adds the classify command intent for the Niece-Scarlett bot.

INSERT INTO gaia_property_set_entries (id, owner_key, property_set, entry_name, values,
                                       configurable, status, createdat, lastupdatedat)
SELECT gen_random_uuid(), 'NieceScarlett', 'command-intents', 'classify',
       '{"command": "classify", "words": ["classify", "level", "cefr", "difficulty", "grade", "analyse", "analyze"]}',
       TRUE, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM gaia_property_set_entries
                  WHERE owner_key = 'NieceScarlett'
                    AND property_set = 'command-intents'
                    AND entry_name = 'classify');
