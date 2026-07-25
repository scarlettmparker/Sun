-- V5 ensure the gaia_ip_whitelist_entries table matches the entity.
-- V4 created columns named cidr/created_at/last_updated_at but the JPA entity
-- maps pattern/createdAt/lastUpdatedAt (PhysicalNamingStrategyStandardImpl
-- preserves the field name exactly). Drop the old columns and add the ones
-- Hibernate expects.

ALTER TABLE gaia_ip_whitelist_entries DROP COLUMN IF EXISTS cidr;
ALTER TABLE gaia_ip_whitelist_entries ADD COLUMN IF NOT EXISTS pattern VARCHAR(255) NOT NULL DEFAULT '';
ALTER TABLE gaia_ip_whitelist_entries ADD COLUMN IF NOT EXISTS createdat TIMESTAMP(6);
ALTER TABLE gaia_ip_whitelist_entries ADD COLUMN IF NOT EXISTS lastupdatedat TIMESTAMP(6);
ALTER TABLE gaia_ip_whitelist_entries ADD COLUMN IF NOT EXISTS createdby UUID;
ALTER TABLE gaia_ip_whitelist_entries ADD COLUMN IF NOT EXISTS lastupdatedby UUID;
