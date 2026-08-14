-- V13 adds the natural-language query schema to the texts command intent.
-- The property-set entries store free-form JSON, so the query config lives
-- inside the entry's values.

UPDATE gaia_property_set_entries
SET values = values::jsonb || '{
  "query": {
    "fields": {
      "title": { "aliases": ["title", "name"] },
      "level": {
        "aliases": ["level", "levels", "difficulty"],
        "defaultOperator": "IN",
        "values": {
          "a1": "A1", "a2": "A2", "b1": "B1", "b2": "B2", "c1": "C1", "c2": "C2",
          "α1": "A1", "α2": "A2", "β1": "B1", "β2": "B2", "γ1": "C1", "γ2": "C2"
        }
      },
      "language": { "aliases": ["language", "lang"] },
      "createdAt": { "aliases": ["created at", "created"] },
      "updatedAt": { "aliases": ["updated at", "updated"] }
    },
    "sort": {
      "fields": {
        "title": { "aliases": ["title", "name", "alphabetical", "alphabetically"] },
        "level": { "aliases": ["level", "difficulty"] },
        "language": { "aliases": ["language", "lang"] },
        "createdAt": {
          "aliases": ["created", "created at", "newest"],
          "aliasDirections": { "newest": "DESC" }
        },
        "updatedAt": {
          "aliases": ["updated", "updated at", "recently updated"],
          "aliasDirections": { "recently updated": "DESC" }
        }
      },
      "default": { "by": "level", "dir": "ASC" }
    },
    "defaultSearchField": "title",
    "pageAliases": ["page", "σελίδα"]
  }
}'::jsonb,
    lastupdatedat = CURRENT_TIMESTAMP
WHERE owner_key = 'NieceScarlett'
  AND property_set = 'command-intents'
  AND entry_name = 'texts';

INSERT INTO gaia_property_set_entries (id, owner_key, property_set, entry_name, values,
                                       configurable, status, createdat, lastupdatedat)
SELECT gen_random_uuid(), 'NieceScarlett', 'command-intents', 'texts',
       '{
         "command": "texts",
         "words": ["texts", "reader", "reading", "stories", "browse", "library"],
         "query": {
           "fields": {
             "title": { "aliases": ["title", "name"] },
             "level": {
               "aliases": ["level", "levels", "difficulty"],
               "defaultOperator": "IN",
               "values": {
                 "a1": "A1", "a2": "A2", "b1": "B1", "b2": "B2", "c1": "C1", "c2": "C2",
                 "α1": "A1", "α2": "A2", "β1": "B1", "β2": "B2", "γ1": "C1", "γ2": "C2"
               }
             },
             "language": { "aliases": ["language", "lang"] },
             "createdAt": { "aliases": ["created at", "created"] },
             "updatedAt": { "aliases": ["updated at", "updated"] }
           },
           "sort": {
             "fields": {
               "title": { "aliases": ["title", "name", "alphabetical", "alphabetically"] },
               "level": { "aliases": ["level", "difficulty"] },
               "language": { "aliases": ["language", "lang"] },
               "createdAt": {
                 "aliases": ["created", "created at", "newest"],
                 "aliasDirections": { "newest": "DESC" }
               },
               "updatedAt": {
                 "aliases": ["updated", "updated at", "recently updated"],
                 "aliasDirections": { "recently updated": "DESC" }
               }
             },
             "default": { "by": "level", "dir": "ASC" }
           },
           "defaultSearchField": "title",
           "pageAliases": ["page", "σελίδα"]
         }
       }'::jsonb,
       TRUE, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM gaia_property_set_entries
                  WHERE owner_key = 'NieceScarlett'
                    AND property_set = 'command-intents'
                    AND entry_name = 'texts');
