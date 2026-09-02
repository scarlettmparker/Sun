-- V44 seeds Knowledge:review-attributes schema + gallery cover-image schema.
-- Review attributes: rating 0-100 mandatory, ratingMax, listenCount, mood[], tags[], linkedPostIds[], year, format.
-- Cover-image metadata: describes if a gallery image is the cover for its review (via KeyDetail metadata).

INSERT INTO gaia_property_set_schemas (id, owner_key, name, properties, configurable, status, createdat, lastupdatedat)
VALUES (gen_random_uuid(), 'Knowledge', 'review-attributes',
  '{
    "rating": {"type":"number","required":true},
    "ratingMax": {"type":"number"},
    "listenCount": {"type":"number"},
    "mood": {"type":"array","items":{"type":"string"}},
    "tags": {"type":"array","items":{"type":"string"}},
    "linkedPostIds": {"type":"array","items":{"type":"string"}},
    "year": {"type":"number"},
    "format": {"type":"string"}
  }',
  true, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT DO NOTHING;

INSERT INTO gaia_property_set_schemas (id, owner_key, name, properties, configurable, status, createdat, lastupdatedat)
VALUES (gen_random_uuid(), 'Knowledge', 'review-gallery-meta',
  '{
    "coverImageKey": {"type":"string"},
    "coverImageRemoteObject": {"type":"string"}
  }',
  true, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT DO NOTHING;
