-- V4 ip_whitelist_entries: one row per allowed IP pattern. Patterns may be:
--   * a CIDR block  (e.g. "192.168.0.0/24")
--   * a glob         (e.g. "192.168.0.*" or "5.*")
--   * a bare IP      (e.g. "10.0.0.1")

CREATE TABLE gaia_ip_whitelist_entries (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    pattern          VARCHAR(255) NOT NULL,
    description      VARCHAR(255),
    enabled          BOOLEAN NOT NULL DEFAULT TRUE,
    created_at       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_updated_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by       UUID,
    last_updated_by  UUID,
    CONSTRAINT gaia_ip_whitelist_entries_pattern_key UNIQUE (pattern)
);

CREATE INDEX idx_ip_whitelist_enabled ON gaia_ip_whitelist_entries (enabled);
