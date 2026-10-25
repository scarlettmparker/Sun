#!/usr/bin/env bash
set -euo pipefail

STAMP="$(date -u +%Y%m%d)"
DEST="/var/backups/sun-vps/daily/$STAMP"
install -d -m 0700 "$DEST"

lxc exec artemis -- sudo -u postgres pg_dump -Fc -d sun >"$DEST/artemis-sun.dump"
lxc exec medusa -- sqlite3 /var/lib/free-chess/game.db ".backup '/tmp/game.db'"
lxc file pull medusa/tmp/game.db "$DEST/medusa-game.db"
lxc exec medusa -- rm -f /tmp/game.db

find /var/backups/sun-vps/daily -mindepth 1 -maxdepth 1 -type d -mtime +7 -exec rm -rf {} +
