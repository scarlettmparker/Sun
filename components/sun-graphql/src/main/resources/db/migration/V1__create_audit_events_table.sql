-- V1 audit_events: one immutable row per audited operation.
-- user_id is NULL for anonymous or system events. namespace records the
-- sub-service the operation came from (HADES, GAIA, APOLLO, ...).

CREATE TABLE audit_events (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    created_at       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    correlation_id   UUID NOT NULL,

    user_id          UUID,
    namespace        TEXT,

    event_type       TEXT NOT NULL,          -- e.g. HADES_CREATEANNOTATION
    operation_name   TEXT,                   -- GraphQL field or REST path
    operation_type   TEXT NOT NULL,          -- QUERY | MUTATION | REST
    target_entity    TEXT,
    target_entity_id UUID,
    outcome          TEXT NOT NULL,          -- SUCCESS | FAILURE | UNAUTHORIZED
    error_message    TEXT,

    endpoint         TEXT NOT NULL,
    ip_address       TEXT,
    user_agent       TEXT,
    http_status      INTEGER NOT NULL,
    duration_ms      BIGINT,

    -- masked body/variables as JSON text, safe to store.
    payload_redacted TEXT,

    -- hash chain: row_hash = HMAC(chain_key, canonical_fields || prev_hash)
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
