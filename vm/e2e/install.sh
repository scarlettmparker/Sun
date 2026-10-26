#!/usr/bin/env bash
#
# Installs and starts the local E2E runner as a systemd user service.
#
# Usage:
#   ./vm/e2e/install.sh

set -euo pipefail

SUN_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
CONFIG_DIR="${XDG_CONFIG_HOME:-$HOME/.config}"
ENV_FILE="$CONFIG_DIR/sun-e2e.env"
UNIT_DIR="$CONFIG_DIR/systemd/user"
UNIT="$UNIT_DIR/sun-e2e-runner.service"
BUN="${BUN:-$HOME/.bun/bin/bun}"

mkdir -p "$CONFIG_DIR" "$UNIT_DIR"

if [[ ! -f "$ENV_FILE" ]]; then
  umask 077
  {
    printf 'E2E_RUNNER_PORT=8090\n'
    printf 'E2E_RUNNER_TOKEN=%s\n' "$(openssl rand -hex 32)"
  } > "$ENV_FILE"
fi

cat > "$UNIT" <<EOF
[Unit]
Description=Sun local E2E runner
After=network-online.target docker.service
Wants=network-online.target

[Service]
Type=simple
WorkingDirectory=$SUN_ROOT
EnvironmentFile=$ENV_FILE
ExecStart=$BUN $SUN_ROOT/vm/e2e/runner.ts
Restart=always
RestartSec=5

[Install]
WantedBy=default.target
EOF

systemctl --user daemon-reload
systemctl --user enable --now sun-e2e-runner.service

echo "Runner installed."
echo "Token file: $ENV_FILE"
echo "Health: http://127.0.0.1:$(sed -n 's/^E2E_RUNNER_PORT=//p' "$ENV_FILE")/health"
