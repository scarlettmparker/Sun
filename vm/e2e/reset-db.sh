#!/usr/bin/env bash
#
# Drops the disposable E2E schema so the next backend start recreates it.
#
# Usage:
#   ./vm/e2e/reset-db.sh

set -euo pipefail

SUN_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
COMPOSE="docker compose -f $SUN_ROOT/vm/e2e/compose.e2e.yml"

$COMPOSE up -d db >/dev/null

for _ in $(seq 1 30); do
  if $COMPOSE exec -T db pg_isready -U sun -d sun_e2e >/dev/null 2>&1; then
    break
  fi
  sleep 1
done

$COMPOSE exec -T db psql -v ON_ERROR_STOP=1 -U sun -d sun_e2e <<'SQL'
DROP SCHEMA IF EXISTS public CASCADE;
CREATE SCHEMA public;
GRANT ALL ON SCHEMA public TO sun;
SQL

echo "sun_e2e reset. Start the backend so Flyway recreates the schema."
