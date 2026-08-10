ALTER TABLE gaia_accounts DROP CONSTRAINT IF EXISTS gaia_accounts_status_check;
ALTER TABLE gaia_accounts ADD CONSTRAINT gaia_accounts_status_check
  CHECK (status IN ('ACTIVE', 'SUSPENDED', 'PENDING', 'DEACTIVATED'));
