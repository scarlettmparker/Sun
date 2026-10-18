#!/usr/bin/env bash
#
# Installs the Sun edge nginx config on the VPS: the shared snippets plus each
# domain's site file, rendered into .live/.standby for the blue-green swap.
#
#   ./install.sh
#
# Idempotent. Validates with nginx -t before reloading, so a bad edit is a
# no-op on the running server.

set -euo pipefail

VM="${VM:-ubuntu@vps.scarlettparker.co.uk}"
SSH_KEY="${SSH_KEY:-$HOME/.ssh/ubuntu.key}"
SSH="ssh -i $SSH_KEY $VM"
HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REMOTE=/tmp/sun-nginx

STAGE="$(mktemp -d)"
trap 'rm -rf "$STAGE"' EXIT
mkdir -p "$STAGE/snippets" "$STAGE/sites"

cp "$HERE/snippets/"*.conf "$STAGE/snippets/"

while read -r domain live standby; do
  sed "s/@PORT@/$live/g" "$HERE/sites/$domain.conf" > "$STAGE/sites/$domain.live"
  sed "s/@PORT@/$standby/g" "$HERE/sites/$domain.conf" > "$STAGE/sites/$domain.standby"
done < <(grep -vE '^[[:space:]]*(#|$)' "$HERE/sites.conf")

rsync -az --delete -e "ssh -i $SSH_KEY" "$STAGE/" "$VM:$REMOTE/"

$SSH bash -s <<'REMOTE'
set -euo pipefail
sudo install -d -m 0755 /etc/nginx/snippets
sudo install -m 0644 /tmp/sun-nginx/snippets/*.conf /etc/nginx/snippets/
for f in /etc/nginx/sites-available/*.live /etc/nginx/sites-available/*.standby; do
  [ -e "$f" ] || continue
  [ -e "$f.pre-sun" ] || sudo cp "$f" "$f.pre-sun"
done
for f in /tmp/sun-nginx/sites/*.live /tmp/sun-nginx/sites/*.standby; do
  [ -e "$f" ] || continue
  sudo install -m 0644 "$f" "/etc/nginx/sites-available/$(basename "$f")"
done
sudo nginx -t
sudo systemctl reload nginx
echo "installed: $(ls /tmp/sun-nginx/sites)"
REMOTE
