#!/usr/bin/env bash
#
# Queues a commit on the local E2E runner and waits for the result.
#
# Usage:
#   ./vm/e2e/trigger.sh Guided-Reader <sha>

set -euo pipefail

REPO="${1:-}"
SHA="${2:-}"
ENV_FILE="${E2E_ENV_FILE:-$HOME/.config/sun-e2e.env}"

if [[ -z "$REPO" || -z "$SHA" ]]; then
  echo "usage: trigger.sh <repo> <sha>" >&2
  exit 2
fi

if [[ -f "$ENV_FILE" ]]; then
  set -a
  # shellcheck disable=SC1090
  source "$ENV_FILE"
  set +a
fi

: "${E2E_RUNNER_URL:?E2E_RUNNER_URL is required}"
: "${E2E_RUNNER_TOKEN:?E2E_RUNNER_TOKEN is required}"

BODY=$(printf '{"repo":"%s","sha":"%s"}' "$REPO" "$SHA")
RESPONSE=$(curl -fsS -X POST "$E2E_RUNNER_URL/runs" \
  -H "Authorization: Bearer $E2E_RUNNER_TOKEN" \
  -H "Content-Type: application/json" \
  -d "$BODY")

RUN_ID=$(printf '%s' "$RESPONSE" | sed -n 's/.*"id":"\([^"]*\)".*/\1/p')
if [[ -z "$RUN_ID" ]]; then
  echo "Runner did not return a run id: $RESPONSE" >&2
  exit 1
fi

echo "Queued $RUN_ID"

while true; do
  STATUS_JSON=$(curl -fsS "$E2E_RUNNER_URL/runs/$RUN_ID" \
    -H "Authorization: Bearer $E2E_RUNNER_TOKEN")
  STATUS=$(printf '%s' "$STATUS_JSON" | sed -n 's/.*"status":"\([^"]*\)".*/\1/p')

  case "$STATUS" in
    passed)
      echo "Passed $RUN_ID"
      exit 0
      ;;
    failed)
      REPORT=$(printf '%s' "$STATUS_JSON" | sed -n 's/.*"reportPath":"\([^"]*\)".*/\1/p')
      ERROR=$(printf '%s' "$STATUS_JSON" | sed -n 's/.*"error":"\([^"]*\)".*/\1/p')
      echo "Failed $RUN_ID: ${ERROR:-unknown error}" >&2
      [[ -n "$REPORT" ]] && echo "Report: $REPORT" >&2
      exit 1
      ;;
  esac

  sleep 2
done
