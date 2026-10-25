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
mkdir -p "$STAGE/snippets" "$STAGE/sites" "$STAGE/maintenance"

cp "$HERE/snippets/"*.conf "$STAGE/snippets/"
cp "$HERE/maintenance/"*.html "$STAGE/maintenance/"

while read -r domain live standby host; do
  host="${host:-127.0.0.1}"
  sed "s/@HOST@/$host/g; s/@PORT@/$live/g" "$HERE/sites/$domain.conf" > "$STAGE/sites/$domain.live"
  sed "s/@HOST@/$host/g; s/@PORT@/$standby/g" "$HERE/sites/$domain.conf" > "$STAGE/sites/$domain.standby"
done < <(grep -vE '^[[:space:]]*(#|$)' "$HERE/sites.conf")

rsync -az --delete -e "ssh -i $SSH_KEY" "$STAGE/" "$VM:$REMOTE/"

$SSH bash -s <<'REMOTE'
set -euo pipefail
sudo install -d -m 0755 /etc/nginx/snippets
sudo install -m 0644 /tmp/sun-nginx/snippets/*.conf /etc/nginx/snippets/
sudo install -d -m 0755 /var/www/maintenance
sudo install -m 0644 /tmp/sun-nginx/maintenance/*.html /var/www/maintenance/
for f in /etc/nginx/sites-available/*.live /etc/nginx/sites-available/*.standby; do
  [ -e "$f" ] || continue
  [ -e "$f.pre-sun" ] || sudo cp "$f" "$f.pre-sun"
done
for f in /tmp/sun-nginx/sites/*.live /tmp/sun-nginx/sites/*.standby; do
  [ -e "$f" ] || continue
  sudo install -m 0644 "$f" "/etc/nginx/sites-available/$(basename "$f")"
done
for f in /etc/nginx/sites-enabled/*; do
  [ -e "$f" ] || continue
  target="$(readlink -f "$f")"
  if grep -q 'sun-maintenance.conf' "$target"; then
    continue
  fi
  sudo sed -i --follow-symlinks '/^[[:space:]]*server[[:space:]]*{[[:space:]]*$/a\    include /etc/nginx/snippets/sun-maintenance.conf;' "$target"
done
sudo nginx -t
sudo systemctl reload nginx
echo "installed: $(ls /tmp/sun-nginx/sites)"
REMOTE
