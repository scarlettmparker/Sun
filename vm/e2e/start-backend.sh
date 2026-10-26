#!/usr/bin/env bash
#
# Runs the Sun GraphQL backend against the disposable E2E database.
#
# Usage:
#   ./vm/e2e/start-backend.sh
#
# Environment:
#   E2E_BACKEND_JAR    Backend jar, default the local bootJar output.
#   E2E_BACKEND_PORT   Backend port, default 8085.
#   E2E_JWT_SECRET     JWT signing secret shared by the E2E apps.

set -euo pipefail

SUN_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
JAR="${E2E_BACKEND_JAR:-$SUN_ROOT/components/sun-graphql/build/libs/sun-graphql-0.0.1-SNAPSHOT.jar}"
BACKEND_PORT="${E2E_BACKEND_PORT:-8085}"
JWT_SECRET="${E2E_JWT_SECRET:-test-secret-key-for-functional-tests-only-very-long-secure-key-1234567890ABCDEF}"

if [[ ! -f "$JAR" ]]; then
  echo "Missing $JAR" >&2
  echo "Build it with: ./components/sun-graphql/gradlew -p components :sun-graphql:bootJar" >&2
  exit 1
fi

export SPRING_DATASOURCE_URL="jdbc:postgresql://127.0.0.1:5436/sun_e2e"
export SPRING_DATASOURCE_USERNAME=sun
export SPRING_DATASOURCE_PASSWORD=sun
export SPRING_FLYWAY_ENABLED=false
export JWT_SECRET="$JWT_SECRET"
export REDIS_HOST=127.0.0.1
export REDIS_PORT=6379
export APP_BASE_URL="http://127.0.0.1:5200"

export S3_ENDPOINT=http://127.0.0.1:3900
export S3_PUBLIC_ENDPOINT=http://127.0.0.1:3900
export GARAGE_SECRET_KEY=test
export AWS_ACCESS_KEY_ID=test
export AWS_SECRET_ACCESS_KEY=test
export EMAIL_CLIENT_ID=test
export EMAIL_CLIENT_SECRET=test
export EMAIL_REFRESH_TOKEN=test
export EMAIL_ADDRESS=test@example.com
export DISCORD_CLIENT_ID=test
export DISCORD_CLIENT_SECRET=test
export GOOGLE_CLIENT_ID=test
export GOOGLE_CLIENT_SECRET=test
export PROWLARR_API_KEY=test
export OPENSUBTITLES_API_KEY=test
export OPENSUBTITLES_USERNAME=test
export OPENSUBTITLES_PASSWORD=test
export AUDIT_ENABLED=false
export RATE_LIMIT_ENABLED=false
export IP_WHITELIST_ENABLED=false
export CEFR_INFERENCE_ENABLED=false

exec java -jar "$JAR" \
  --server.port="$BACKEND_PORT" \
  --app.bypass-permissions=true \
  --logging.level.root=WARN \
  --logging.level.org.hibernate=WARN \
  --logging.level.org.hibernate.SQL=WARN \
  --logging.level.com.zaxxer.hikari=WARN
