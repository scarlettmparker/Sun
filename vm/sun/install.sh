#!/usr/bin/env bash
set -euo pipefail

SUN_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
BUN="${BUN:-$HOME/.bun/bin/bun}"
BIN_DIR="$HOME/.local/bin"

mkdir -p "$BIN_DIR" "$HOME/.config/systemd/user"

install -m 0755 "$SUN_ROOT/vm/sun/sun-notify" "$BIN_DIR/sun-notify"

chmod +x "$SUN_ROOT/vm/sun/sun-dev"
ln -sfn "$SUN_ROOT/vm/sun/sun-dev" "$BIN_DIR/sun-dev"

units="$("$BUN" "$SUN_ROOT/vm/sun/render-units.ts")"
systemctl --user daemon-reload

for unit in $units; do
  systemctl --user enable --now "$unit"
done

loginctl enable-linger "$USER" >/dev/null 2>&1 || true

echo "enabled units:"
for unit in $units; do
  echo "  $unit"
done
