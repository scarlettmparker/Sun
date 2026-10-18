-- V56 rebuilds audit_events. Each row records one operation, the caller's
-- denormalised identity, a masked payload, and a hash chained over the row
-- before it.

DROP TRIGGER IF EXISTS audit_no_update ON audit_events;
DROP TRIGGER IF EXISTS audit_no_delete ON audit_events;
DROP FUNCTION IF EXISTS block_audit_mutation();
DROP TABLE IF EXISTS audit_events;

CREATE TABLE audit_events (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    created_at       TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    correlation_id   UUID NOT NULL,

    -- identity is denormalised so the trail survives renames and deletions.
    user_id          UUID,
    user_name        TEXT,
    user_roles       TEXT,                   -- JSON array of role names
    namespace        TEXT,                   -- sub-service, e.g. HADES

    event_type       TEXT NOT NULL,          -- e.g. HADES_CREATEANNOTATION
    operation_name   TEXT,                   -- GraphQL field or REST path
    operation_type   TEXT NOT NULL,          -- QUERY, MUTATION or REST
    target_entity    TEXT,
    target_entity_id UUID,
    outcome          TEXT NOT NULL,          -- SUCCESS or FAILURE
    error_message    TEXT,

    endpoint         TEXT NOT NULL,
    ip_address       TEXT,
    user_agent       TEXT,
    http_status      INTEGER NOT NULL,
    duration_ms      BIGINT,

    -- masked body as JSON text.
    payload_redacted   TEXT,
    -- {kekId, iv, wrappedDek, ciphertext} for retained sensitive fields.
    payload_encrypted  TEXT,

    -- prior state for edits, NULL for creates.
    payload_before            TEXT,
    payload_before_encrypted  TEXT,

    -- row_hash = HMAC(chain_key, canonical_fields || prev_hash)
    prev_hash        TEXT,
    row_hash         TEXT NOT NULL
);

CREATE INDEX idx_audit_events_time        ON audit_events (created_at DESC);
CREATE INDEX idx_audit_events_correlation ON audit_events (correlation_id);
CREATE INDEX idx_audit_events_user        ON audit_events (user_id, created_at DESC);
CREATE INDEX idx_audit_events_target      ON audit_events (target_entity, target_entity_id);
CREATE INDEX idx_audit_events_namespace   ON audit_events (namespace, created_at DESC);

-- Append-only: triggers reject UPDATE and DELETE so the trail cannot be edited.
CREATE OR REPLACE FUNCTION block_audit_mutation() RETURNS trigger AS $$
BEGIN
    RAISE EXCEPTION 'audit_events is append-only: % not permitted on row %', TG_OP, OLD.id;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER audit_no_update BEFORE UPDATE ON audit_events
    FOR EACH ROW EXECUTE FUNCTION block_audit_mutation();

CREATE TRIGGER audit_no_delete BEFORE DELETE ON audit_events
    FOR EACH ROW EXECUTE FUNCTION block_audit_mutation();
