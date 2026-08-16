-- V18 adds per-channel rate-limit overrides for define and restricts classify
-- to a single channel via capacity-0 global + channel override.

-- define: global 1/min, three channels 1/10s
UPDATE gaia_property_set_entries
SET values = (values::jsonb - 'channelRateLimits') || '{
  "rateLimit": {
    "capacity": 1,
    "refillPerSecond": 0.0167,
    "channels": {
      "354924532479295498": { "capacity": 1, "refillPerSecond": 0.1 },
      "477473581626949642": { "capacity": 1, "refillPerSecond": 0.1 },
      "798363574358769684": { "capacity": 1, "refillPerSecond": 0.1 }
    }
  }
}'::jsonb,
    lastupdatedat = CURRENT_TIMESTAMP
WHERE owner_key = 'NieceScarlett'
  AND property_set = 'command-intents'
  AND entry_name = 'define';

-- classify: capacity 0 globally (disabled), 1/min only in one channel
UPDATE gaia_property_set_entries
SET values = (values::jsonb - 'channelRateLimits') || '{
  "rateLimit": {
    "capacity": 0,
    "refillPerSecond": 0.0167,
    "channels": {
      "354924532479295498": { "capacity": 1, "refillPerSecond": 0.0167 }
    }
  }
}'::jsonb,
    lastupdatedat = CURRENT_TIMESTAMP
WHERE owner_key = 'NieceScarlett'
  AND property_set = 'command-intents'
  AND entry_name = 'classify';
