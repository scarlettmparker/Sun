-- V6 add immutable column to gaia_ip_whitelist_entries

ALTER TABLE gaia_ip_whitelist_entries ADD COLUMN IF NOT EXISTS immutable BOOLEAN NOT NULL DEFAULT FALSE;
