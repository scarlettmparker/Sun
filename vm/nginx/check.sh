#!/usr/bin/env bash
#
# Validates every site file with `nginx -t` in a throwaway nginx container,
# against stub TLS material. Catches a bad edit before it reaches the VPS.
#
#   ./check.sh

set -euo pipefail

HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
IMAGE="${IMAGE:-nginx:1.27-alpine}"
STAGE="$(mktemp -d)"
trap 'rm -rf "$STAGE"' EXIT

NGX="$STAGE/etc/nginx"
LE="$STAGE/etc/letsencrypt"
mkdir -p "$NGX/conf.d" "$NGX/snippets" "$NGX/sites-enabled" "$LE/live" "$STAGE/var/www/html"

cat >"$NGX/nginx.conf" <<'CONF'
error_log stderr;
pid /tmp/nginx.pid;
events {}
http {
    access_log off;
    include /etc/nginx/conf.d/*.conf;
    include /etc/nginx/sites-enabled/*;
}
CONF

# $connection_upgrade comes from conf.d on the VPS.
cat >"$NGX/conf.d/upgrade-map.conf" <<'CONF'
map $http_upgrade $connection_upgrade {
    default upgrade;
    ''      close;
}
CONF

cp "$HERE/snippets/"*.conf "$NGX/snippets/"

DHPARAM=/tmp/opencode/sun-check-dhparam.pem
if [ ! -f "$DHPARAM" ]; then
  openssl dhparam -dsaparam -out "$DHPARAM" 2048 2>/dev/null
fi
cp "$DHPARAM" "$LE/ssl-dhparams.pem"
: >"$LE/options-ssl-nginx.conf"

while read -r domain live _standby; do
  mkdir -p "$LE/live/$domain"
  openssl req -x509 -newkey rsa:2048 -sha256 -days 1 -nodes \
    -keyout "$LE/live/$domain/privkey.pem" -out "$LE/live/$domain/fullchain.pem" \
    -subj "/CN=$domain" 2>/dev/null
  sed "s/@PORT@/$live/g" "$HERE/sites/$domain.conf" > "$NGX/sites-enabled/$domain"
done < <(grep -vE '^[[:space:]]*(#|$)' "$HERE/sites.conf")

docker run --rm \
  -v "$NGX:/etc/nginx:ro" \
  -v "$LE:/etc/letsencrypt:ro" \
  -v "$STAGE/var/www/html:/var/www/html:ro" \
  "$IMAGE" nginx -t
