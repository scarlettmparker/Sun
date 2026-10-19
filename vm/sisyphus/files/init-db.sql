-- Bootstrap the shared sun role and database. Rendered by sisyphus.sh with
-- DB_PASSWORD from the repo backend .env, then run once as the postgres user.

DO $$
BEGIN
  IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'sun') THEN
    CREATE ROLE sun LOGIN PASSWORD '${DB_PASSWORD}';
  ELSE
    ALTER ROLE sun WITH LOGIN PASSWORD '${DB_PASSWORD}';
  END IF;
END
$$;

SELECT 'CREATE DATABASE sun OWNER sun'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'sun')\gexec
