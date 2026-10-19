#!/usr/bin/env bash
#
# sisyphus: self-contained Incus container running the Sun backend stack.
# Tunables live in sisyphus.conf.
#   ./sisyphus.sh provision              box: storage, network, limits
#   ./sisyphus.sh deploy [--skip-build]  build, push, restart the backend
#   ./sisyphus.sh status | shell | stop | start | destroy

set -euo pipefail

SELF="$(readlink -f "${BASH_SOURCE[0]}")"
VM_DIR="$(dirname "$SELF")"
REPO="$(cd "$VM_DIR/../.." && pwd)"
FILES="$VM_DIR/files"
BACKEND_ENV="$REPO/components/sun-graphql/src/main/resources/.env"

# shellcheck source=sisyphus.conf
. "$VM_DIR/sisyphus.conf"

incus() { sudo incus "$@"; }
ic() { sudo incus exec "$NAME" -- "$@"; }

# Incus has no device-exists predicate so read the YAML listing.
device_exists() { incus config device show "$NAME" 2>/dev/null | grep -q "^$1:"; }

log() { printf '\033[1;36m==>\033[0m %s\n' "$*"; }
warn() { printf '\033[1;33mwarn:\033[0m %s\n' "$*" >&2; }

# Read one KEY=value from the backend .env.
envval() { grep -E "^$1=" "$BACKEND_ENV" 2>/dev/null | head -n1 | cut -d= -f2- || true; }

# ── storage ──────────────────────────────────────────────────────────────────

# Sparse loop image + ext4, mounted at POOL_DIR and registered as an Incus pool.
# Safe to re-run.
ensure_storage() {
  log "storage: $POOL_IMG ($STORAGE_SIZE) -> $POOL_DIR"
  sudo mkdir -p "$POOL_DIR"
  if [ ! -f "$POOL_IMG" ]; then
    sudo truncate -s "$STORAGE_SIZE" "$POOL_IMG"
    sudo mkfs.ext4 -q -F "$POOL_IMG"
  fi
  if ! mountpoint -q "$POOL_DIR"; then
    sudo mount -o loop "$POOL_IMG" "$POOL_DIR"
    sudo rmdir "$POOL_DIR/lost+found" 2>/dev/null || true
  fi
  grep -q "$POOL_DIR" /etc/fstab \
    || echo "$POOL_IMG $POOL_DIR ext4 loop,defaults,nofail 0 0" | sudo tee -a /etc/fstab >/dev/null
  incus storage show "$POOL" >/dev/null 2>&1 || incus storage create "$POOL" dir source="$POOL_DIR"
}

# ── network ──────────────────────────────────────────────────────────────────

# Managed bridge, shared with cassini. dnsmasq stays off because pihole already
# owns host :53 and :67.
ensure_bridge() {
  if ! incus network show "$BRIDGE" >/dev/null 2>&1; then
    log "network: creating bridge $BRIDGE (dns/dhcp off, host owns :53/:67)"
    incus network create "$BRIDGE" --type=bridge \
      ipv4.address="$BRIDGE_CIDR" \
      ipv4.nat=true \
      ipv4.dhcp=false \
      ipv6.address=none \
      dns.mode=none
  fi
}

# Let the LAN reach the published backend port.
ensure_ufw() {
  log "firewall: LAN -> :$HOST_PORT"
  sudo ufw allow "$HOST_PORT"/tcp >/dev/null 2>&1 || true
}

# Points HOST_ALIAS at loopback so the browser on windcatcher can find it.
ensure_hosts() {
  grep -qE "[[:space:]]$HOST_ALIAS\$" /etc/hosts \
    || echo "127.0.0.1 $HOST_ALIAS" | sudo tee -a /etc/hosts >/dev/null
}

# ── container ────────────────────────────────────────────────────────────────

# Wait for systemd inside the container. "degraded" still counts as up, since
# containers usually have some dead unit we don't care about.
wait_boot() {
  local i state
  for i in $(seq 1 60); do
    state="$(ic systemctl is-system-running 2>/dev/null || true)"
    case "$state" in running | degraded) return 0 ;; esac
    sleep 1
  done
  warn "container did not report a running systemd within 60s"
}

# Launches the instance if it doesn't exist, attaches its NIC and proxy device,
# then pins the limits. Idempotent.
ensure_container() {
  if ! incus info "$NAME" >/dev/null 2>&1; then
    log "container: launching $NAME from $IMAGE on pool $POOL"
    incus launch "$IMAGE" "$NAME" --storage "$POOL"
  fi
  incus start "$NAME" 2>/dev/null || true
  wait_boot

  device_exists eth0 \
    || incus config device add "$NAME" eth0 nic network="$BRIDGE" name=eth0

  device_exists api \
    || incus config device add "$NAME" api proxy \
         listen=tcp:0.0.0.0:"$HOST_PORT" connect=tcp:127.0.0.1:"$CONTAINER_PORT"

  incus config set "$NAME" boot.autostart true
  incus config set "$NAME" limits.cpu "$LIMITS_CPU"
  incus config set "$NAME" limits.memory "$MEMORY"
  incus config set "$NAME" limits.memory.enforce hard
  incus config set "$NAME" limits.memory.swap true
}

# Push the static NIC config and set the hostname.
configure_guest() {
  log "guest: hostname, static NIC"
  ic hostnamectl set-hostname "$NAME" >/dev/null 2>&1 || true

  sudo incus file push -q -p --mode 0644 \
    "$FILES/10-sisyphus.network" "$NAME/etc/systemd/network/10-sisyphus.network"

  ic systemctl restart systemd-networkd >/dev/null 2>&1 || true
  ic systemctl daemon-reload >/dev/null 2>&1 || true
  ic systemctl mask systemd-networkd-wait-online.service >/dev/null 2>&1 || true
}

# JRE and data services. dpkg gates each package so re-runs are cheap.
install_packages() {
  local missing=() pkg
  for pkg in openjdk-25-jre-headless postgresql redis-server; do
    ic dpkg -s "$pkg" >/dev/null 2>&1 || missing+=("$pkg")
  done
  [ "${#missing[@]}" -eq 0 ] && return 0
  log "guest: installing ${missing[*]}"
  ic env DEBIAN_FRONTEND=noninteractive apt-get update -qq
  ic env DEBIAN_FRONTEND=noninteractive apt-get install -y -qq \
    -o Dpkg::Options::=--force-confold "${missing[@]}"
}

# Small-box tuning plus the shared sun role and database.
configure_postgres() {
  local pg_conf pw sql
  pg_conf="$(ic bash -c 'ls -d /etc/postgresql/*/main' | head -n1)"
  [ -n "$pg_conf" ] || { warn "no postgres config dir found"; return 1; }

  log "guest: postgres tuning"
  ic mkdir -p "$pg_conf/conf.d"
  sudo incus file push -q -p --mode 0644 \
    "$FILES/postgres-tuning.conf" "$NAME$pg_conf/conf.d/99-sisyphus.conf"
  ic systemctl restart postgresql

  pw="$(envval SPRING_DATASOURCE_PASSWORD)"
  [ -n "$pw" ] || { warn "SPRING_DATASOURCE_PASSWORD missing from $BACKEND_ENV"; return 1; }

  log "guest: sun role + database"
  mkdir -p /tmp/opencode
  sql=/tmp/opencode/sisyphus-db.sql
  umask 077
  DB_PASSWORD="${pw//\'/\'\'}" envsubst '${DB_PASSWORD}' \
    < "$FILES/init-db.sql" > "$sql"
  sudo incus exec "$NAME" -- rm -f /tmp/init-db.sql
  sudo incus file push -q -p --mode 0600 --uid 0 --gid 0 "$sql" "$NAME/tmp/init-db.sql"
  ic chown postgres:postgres /tmp/init-db.sql
  ic su postgres -c 'psql -v ON_ERROR_STOP=1 -q -f /tmp/init-db.sql'
  sudo incus exec "$NAME" -- rm -f /tmp/init-db.sql
  rm -f "$sql"
}

# Caps the box's swap usage.
tune_swap() {
  local cg="/sys/fs/cgroup/lxc.payload.$NAME" bytes
  case "$SWAP_LIMIT" in
    *[Gg]) bytes=$(( ${SWAP_LIMIT%[Gg]} * 1024 * 1024 * 1024 )) ;;
    *[Mm]) bytes=$(( ${SWAP_LIMIT%[Mm]} * 1024 * 1024 )) ;;
    *)     bytes="$SWAP_LIMIT" ;;
  esac
  if echo "$bytes" | sudo tee "$cg/memory.swap.max" >/dev/null 2>&1; then
    log "memory: swap capped at $SWAP_LIMIT"
  else
    warn "memory: no memory.swap.max at $cg, skipping"
  fi
}

# Full box setup. Each step checks before it acts, so running it twice is safe.
provision() {
  ensure_storage
  ensure_bridge
  ensure_container
  ensure_ufw
  ensure_hosts
  configure_guest
  install_packages
  configure_postgres
  tune_swap
  log "provisioned. backend will be published at http://$HOST_ALIAS:$HOST_PORT/graphql"
}

# ── misc ─────────────────────────────────────────────────────────────────────

# Container list, limits and a liveness probe on the published port.
status() {
  incus list "$NAME" 2>/dev/null || true
  log "limits"
  incus config get "$NAME" limits.cpu 2>/dev/null || true
  incus config get "$NAME" limits.memory 2>/dev/null || true
  log "http"
  curl -s -o /dev/null -w 'http %{http_code} in %{time_total}s\n' \
    "http://$HOST_ALIAS:$HOST_PORT/graphql" || warn "no response yet"
}

case "${1:-}" in
  provision) provision ;;
  status) status ;;
  shell) shift; sudo incus exec "$NAME" -- bash -l ;;
  stop) incus stop "$NAME" ;;
  start) incus start "$NAME" ;;
  destroy)
    incus delete --force "$NAME" 2>/dev/null || true
    log "deleted $NAME (pool + loop image left in place)"
    ;;
  *) grep -E '^# ' "$SELF" | head -n 5; exit 1 ;;
esac
