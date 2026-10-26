#!/usr/bin/env bash
#
# Truncates the disposable E2E tables so a running backend can be reused.
#
# Usage:
#   ./vm/e2e/truncate-db.sh

set -euo pipefail

SUN_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
COMPOSE="docker compose -f $SUN_ROOT/vm/e2e/compose.e2e.yml"

$COMPOSE exec -T db psql -v ON_ERROR_STOP=1 -U sun -d sun_e2e <<'SQL'
DO $$
DECLARE
  tables text;
BEGIN
  SELECT string_agg(format('%I.%I', schemaname, tablename), ', ')
    INTO tables
    FROM pg_tables
    WHERE schemaname = 'public';

  IF tables IS NOT NULL THEN
    EXECUTE 'TRUNCATE TABLE ' || tables || ' RESTART IDENTITY CASCADE';
  END IF;
END $$;
SQL
