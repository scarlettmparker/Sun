#!/usr/bin/env bash
#
# Queues a commit on the local E2E runner and waits for the result.
#
# Usage:
#   ./trigger.sh <repo> <sha>

set -euo pipefail

REPO="${1:-}"
SHA="${2:-}"

if [[ -z "$REPO" || -z "$SHA" ]]; then
  echo "usage: trigger.sh <repo> <sha>" >&2
  exit 2
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

LOG_FILE=$(mktemp)
trap 'rm -f "$LOG_FILE"' EXIT
OFFSET=0

while true; do
  STATUS_JSON=$(curl -fsS "$E2E_RUNNER_URL/runs/$RUN_ID" \
    -H "Authorization: Bearer $E2E_RUNNER_TOKEN")
  STATUS=$(printf '%s' "$STATUS_JSON" | sed -n 's/.*"status":"\([^"]*\)".*/\1/p')

  curl -fsS "$E2E_RUNNER_URL/runs/$RUN_ID/log" \
    -H "Authorization: Bearer $E2E_RUNNER_TOKEN" \
    -o "$LOG_FILE" || true

  if [[ -f "$LOG_FILE" ]]; then
    LEN=$(wc -c < "$LOG_FILE")
    if (( LEN > OFFSET )); then
      tail -c +$((OFFSET + 1)) "$LOG_FILE"
      OFFSET=$LEN
    fi
  fi

  case "$STATUS" in
    passed)
      echo "Passed $RUN_ID"
      exit 0
      ;;
    failed)
      REPORT=$(printf '%s' "$STATUS_JSON" | sed -n 's/.*"reportPath":"\([^"]*\)".*/\1/p')
      ERROR=$(printf '%s' "$STATUS_JSON" | sed -n 's/.*"error":"\([^"]*\)".*/\1/p')
      if (( OFFSET == 0 )); then
        curl -fsS "$E2E_RUNNER_URL/runs/$RUN_ID/log?name=build" \
          -H "Authorization: Bearer $E2E_RUNNER_TOKEN" || true
      fi
      echo "Failed $RUN_ID: ${ERROR:-unknown error}" >&2
      [[ -n "$REPORT" ]] && echo "Report: $REPORT" >&2
      exit 1
      ;;
  esac

  sleep 2
done
