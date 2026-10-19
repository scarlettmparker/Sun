#!/usr/bin/env bash
#
# sisyphus: self-contained Incus container running the Sun backend stack.
# Tunables live in sisyphus.conf.
#   ./sisyphus.sh provision              box: storage, network, limits
#   ./sisyphus.sh deploy [--skip-build]  build, push, restart the backend
#   ./sisyphus.sh bootstrap [--force]    (re)load the Hibernate base schema
#   ./sisyphus.sh gen-schema             regenerate files/base-schema.sql
#   ./sisyphus.sh status | shell | stop | start | autostart | destroy

set -euo pipefail

SELF="$(readlink -f "${BASH_SOURCE[0]}")"
VM_DIR="$(dirname "$SELF")"
REPO="$(cd "$VM_DIR/../.." && pwd)"
FILES="$VM_DIR/files"
BACKEND_ENV="$REPO/components/sun-graphql/src/main/resources/.env"
JAR="$REPO/components/sun-graphql/build/libs/sun-graphql-0.0.1-SNAPSHOT.jar"
BASE_SCHEMA="$FILES/base-schema.sql"

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

  incus config set "$NAME" boot.autostart "$AUTOSTART"
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
  for pkg in openjdk-25-jre-headless openjdk-25-jdk-headless postgresql redis-server; do
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
  ( umask 077
    DB_PASSWORD="${pw//\'/\'\'}" envsubst '${DB_PASSWORD}' \
      < "$FILES/init-db.sql" > "$sql" )
  sudo incus exec "$NAME" -- rm -f /tmp/init-db.sql
  sudo incus file push -q -p --mode 0600 --uid 0 --gid 0 "$sql" "$NAME/tmp/init-db.sql"
  ic chown postgres:postgres /tmp/init-db.sql
  ic su postgres -c 'psql -v ON_ERROR_STOP=1 -q -f /tmp/init-db.sql'
  sudo incus exec "$NAME" -- rm -f /tmp/init-db.sql
  rm -f "$sql"
}

# ── data ─────────────────────────────────────────────────────────────────────

# Applies the Hibernate base schema once. Flyway cannot create the base tables
# itself: its seed migrations reference Hibernate-owned tables (gaia_accounts,
# ...) and columns (createdat). files/base-schema.sql is generated from the
# entities by gen-schema; Flyway then layers its migrations, which are
# idempotent for the tables Hibernate already owns.
bootstrap_db() {
  local pw pwpg pgpass
  [ -f "$BASE_SCHEMA" ] || { warn "missing $BASE_SCHEMA; run gen-schema"; return 1; }
  if ic test -f "$APP_DIR/.db-bootstrapped"; then
    log "db: already bootstrapped"
    return 0
  fi
  log "db: applying Hibernate base schema"
  ic mkdir -p "$APP_DIR"
  ic systemctl stop sun-backend permify >/dev/null 2>&1 || true

  pw="$(envval SPRING_DATASOURCE_PASSWORD)"
  [ -n "$pw" ] || { warn "SPRING_DATASOURCE_PASSWORD missing from $BACKEND_ENV"; return 1; }
  pwpg="${pw//\\/\\\\}"; pwpg="${pwpg//:/\\:}"
  pgpass=/tmp/opencode/pgpass-bootstrap
  ( umask 077
    printf '127.0.0.1:5432:sun:sun:%s\n' "$pwpg" > "$pgpass" )

  ic su postgres -c "psql -tAc \"select pg_terminate_backend(pid) from pg_stat_activity where datname='sun'\"" >/dev/null 2>&1 || true
  ic su postgres -c 'dropdb --if-exists sun; createdb -O sun sun'

  sudo incus exec "$NAME" -- rm -f /tmp/base-schema.sql /root/.pgpass
  sudo incus file push -q -p --mode 0600 --uid 0 --gid 0 \
    "$BASE_SCHEMA" "$NAME/tmp/base-schema.sql"
  sudo incus file push -q -p --mode 0600 --uid 0 --gid 0 "$pgpass" "$NAME/root/.pgpass"
  ic bash -c 'PGPASSFILE=/root/.pgpass psql -q -v ON_ERROR_STOP=1 -h 127.0.0.1 -U sun -d sun -f /tmp/base-schema.sql >/dev/null'
  sudo incus exec "$NAME" -- rm -f /tmp/base-schema.sql /root/.pgpass
  rm -f "$pgpass"

  ic touch "$APP_DIR/.db-bootstrapped"
  if ic test -f /etc/systemd/system/sun-backend.service; then
    ic systemctl reset-failed sun-backend >/dev/null 2>&1 || true
    ic systemctl start sun-backend >/dev/null 2>&1 || true
  fi
  log "db: bootstrapped"
}

# ── permify ──────────────────────────────────────────────────────────────────

# Installs the release binary once, renders its config with the DB password and
# (re)starts the unit.
# Bounds Redis; it only holds JWT sessions and revocation keys.
configure_redis() {
  local conf=/etc/redis/redis.conf
  ic test -f "$conf" || { warn "no redis config at $conf"; return 0; }
  if ! ic grep -q '^# sisyphus tuning' "$conf"; then
    ic bash -c "printf '\n# sisyphus tuning\nmaxmemory 64mb\nmaxmemory-policy volatile-lru\n' >> '$conf'"
  fi
  ic systemctl restart redis-server
}

install_permify() {
  local arch tarball yaml pw tmp
  arch="$(ic uname -m)"
  case "$arch" in x86_64) arch=amd64 ;; aarch64) arch=arm64 ;; esac
  mkdir -p /tmp/opencode

  if ! ic test -x "$PERMIFY_DIR/permify"; then
    log "guest: permify $PERMIFY_VERSION ($arch)"
    tarball="/tmp/opencode/permify-${PERMIFY_VERSION}-${arch}.tar.gz"
    [ -f "$tarball" ] || curl -fsSL \
      "https://github.com/Permify/permify/releases/download/${PERMIFY_VERSION}/permify_${PERMIFY_VERSION#v}_Linux_${arch}.tar.gz" \
      -o "$tarball"
    tmp="$(mktemp -d)"
    tar -xzf "$tarball" -C "$tmp" permify
    ic mkdir -p "$PERMIFY_DIR"
    sudo incus exec "$NAME" -- rm -f "$PERMIFY_DIR/permify"
    sudo incus file push -q -p --mode 0755 --uid 0 --gid 0 \
      "$tmp/permify" "$NAME$PERMIFY_DIR/permify"
    rm -rf "$tmp"
  fi

  pw="$(envval SPRING_DATASOURCE_PASSWORD)"
  yaml=/tmp/opencode/permify.yaml
  ( umask 077
    SPRING_DATASOURCE_PASSWORD="$pw" envsubst '${SPRING_DATASOURCE_PASSWORD}' \
      < "$REPO/permify/permify.yaml" > "$yaml" )
  sudo incus exec "$NAME" -- rm -f "$PERMIFY_DIR/permify.yaml"
  sudo incus file push -q -p --mode 0600 --uid 0 --gid 0 \
    "$yaml" "$NAME$PERMIFY_DIR/permify.yaml"
  rm -f "$yaml"

  sudo incus file push -q -p --mode 0644 --uid 0 --gid 0 \
    "$FILES/permify.service" "$NAME/etc/systemd/system/permify.service"
  ic systemctl daemon-reload
  ic systemctl enable permify >/dev/null 2>&1 || true
  ic systemctl restart permify
}

# Blocks until Permify answers on its health endpoint.
wait_permify() {
  local addr="${CONTAINER_IP%/*}" i
  for i in $(seq 1 30); do
    curl -sf "http://$addr:3477/healthz" >/dev/null 2>&1 && return 0
    sleep 1
  done
  warn "permify did not become healthy within 30s"
  return 1
}

# Writes the repo schema so Permify is primed even when the backend runs with
# its own schema push disabled (AUTH=off).
migrate_permify_schema() {
  local addr="${CONTAINER_IP%/*}" body
  body="$(python3 -c 'import json,sys; print(json.dumps({"schema": open(sys.argv[1]).read()}))' \
    "$REPO/permify/schema.zed")"
  curl -sf -X POST "http://$addr:3477/v1/tenants/t1/schemas/write" \
    -H 'content-type: application/json' -d "$body" >/dev/null
  log "permify: schema migrated"
}

# ── backend ──────────────────────────────────────────────────────────────────

# Renders the container .env from the repo .env, pinning the datasource to the
# local box and forcing the toggles that match AUTH.
render_env() {
  local out=/tmp/opencode/sisyphus.env rate audit ip
  if [ "$AUTH" = "on" ]; then
    rate=true; audit=true; ip=true
  else
    rate=false; audit=false; ip=false
  fi
  mkdir -p /tmp/opencode
  ( umask 077
    sed -e 's|^SPRING_DATASOURCE_URL=.*|SPRING_DATASOURCE_URL=jdbc:postgresql://127.0.0.1:5432/sun?reWriteBatchedInserts=true|' \
        -e 's|^SPRING_DATASOURCE_USERNAME=.*|SPRING_DATASOURCE_USERNAME=sun|' \
        -e "s|^RATE_LIMIT_ENABLED=.*|RATE_LIMIT_ENABLED=$rate|" \
        -e "s|^AUDIT_ENABLED=.*|AUDIT_ENABLED=$audit|" \
        -e 's|^CEFR_INFERENCE_ENABLED=.*|CEFR_INFERENCE_ENABLED=false|' \
        "$BACKEND_ENV" > "$out"
    {
      echo "AUTH=$AUTH"
      echo "IP_WHITELIST_ENABLED=$ip"
      echo "JVM_XMS=$JVM_XMS"
      echo "JVM_XMX=$JVM_XMX"
      echo "JVM_METASPACE=$JVM_METASPACE"
      echo "JVM_CPUS=$JVM_CPUS"
      echo "JVM_GC=$JVM_GC"
      echo "JVM_CDS=$JVM_CDS"
      echo "NMT=$NMT"
      echo "COMPACT_HEADERS=$COMPACT_HEADERS"
      echo "VIRTUAL_THREADS=$VIRTUAL_THREADS"
      if [ "${MODULES:-full}" = "reader" ]; then
        for module in APOLLO BRIAREUS CERBERUS DIONYSUS ECHO JOCASTA NARCISSUS; do
          echo "${module}_ENABLED=false"
        done
      fi
      if [ "${LAZY_INIT:-off}" = "on" ]; then
        echo "SPRING_MAIN_LAZY_INITIALIZATION=true"
      fi
    } >> "$out"
  )
}

build_backend() {
  log "build: sun-graphql bootJar"
  "$REPO/components/sun-graphql/gradlew" -p "$REPO/components" :sun-graphql:bootJar --quiet
  [ -f "$JAR" ] || { warn "jar missing at $JAR"; return 1; }
  log "build: $(du -h "$JAR" | cut -f1)"
}

# Blocks until the schema endpoint answers.
wait_backend() {
  local addr="${CONTAINER_IP%/*}" i
  for i in $(seq 1 90); do
    curl -sf "http://$addr:8083/graphql/schema" >/dev/null 2>&1 && return 0
    sleep 1
  done
  warn "backend did not answer within 90s"
  return 1
}

# Pushes the jar, rendered env, launcher and unit. No service changes.
push_backend_files() {
  log "push: jar + env + unit"
  ic mkdir -p "$APP_DIR"

  sudo incus exec "$NAME" -- rm -f "$APP_DIR/sun-graphql.jar"
  sudo incus file push -q -p --mode 0644 --uid 0 --gid 0 \
    "$JAR" "$NAME$APP_DIR/sun-graphql.jar"

  render_env
  sudo incus exec "$NAME" -- rm -f "$APP_DIR/.env"
  sudo incus file push -q -p --mode 0600 --uid 0 --gid 0 \
    /tmp/opencode/sisyphus.env "$NAME$APP_DIR/.env"
  rm -f /tmp/opencode/sisyphus.env

  sudo incus file push -q -p --mode 0755 --uid 0 --gid 0 \
    "$FILES/start.sh" "$NAME$APP_DIR/start.sh"
  sudo incus file push -q -p --mode 0644 --uid 0 --gid 0 \
    "$FILES/sun-backend.service" "$NAME/etc/systemd/system/sun-backend.service"

  ic systemctl daemon-reload
}

deploy_backend() {
  push_backend_files
  ic systemctl enable sun-backend >/dev/null 2>&1 || true
  ic systemctl reset-failed sun-backend >/dev/null 2>&1 || true
  ic systemctl restart sun-backend
  wait_backend
  log "backend up at http://$HOST_ALIAS:$HOST_PORT/graphql"
}

# Regenerates files/base-schema.sql by asking Hibernate to emit its DDL into a
# scratch database on the box. Run after entity changes; commit the result.
gen_schema() {
  local scratch=sun_base i
  [ -f "$JAR" ] || { warn "jar missing at $JAR; run deploy first"; return 1; }
  log "schema: generating from $(basename "$JAR")"
  push_backend_files
  ic systemctl stop sun-backend >/dev/null 2>&1 || true

  ic su postgres -c "dropdb --if-exists $scratch; createdb -O sun $scratch"
  sudo incus exec "$NAME" -- bash -c "
    set -a; . $APP_DIR/.env; set +a
    cd $APP_DIR
    nohup java -Xms256m -Xmx\${JVM_XMX:-1g} -jar sun-graphql.jar \
      --server.port=0 \
      --spring.datasource.url=jdbc:postgresql://127.0.0.1:5432/$scratch \
      --spring.flyway.enabled=false \
      --spring.jpa.hibernate.ddl-auto=create \
      --app.bypass-permissions=true --permify.enabled=false \
      >/tmp/gen-schema.log 2>&1 &
    echo \"schema: hibernate running (pid \$!)\""

  for i in $(seq 1 90); do
    if [ "$(ic su postgres -c "psql -tAc \"select count(*) from pg_tables where schemaname='public' and tablename='gaia_accounts'\" $scratch 2>/dev/null" | tr -d '[:space:]')" = "1" ]; then
      break
    fi
    sleep 2
  done
  ic pkill -f 'sun-graphql.jar' >/dev/null 2>&1 || true
  sleep 1

  ic su postgres -c "pg_dump --schema-only --no-owner --no-acl $scratch" \
    | {
        printf '%s\n' '-- sisyphus base schema: the DDL Hibernate generates for every entity.'
        printf '%s\n' '-- Generated by `sisyphus.sh gen-schema`; do not edit by hand.'
        printf '%s\n' '-- Flyway migrations are applied on top of this at first boot.'
        printf '%s\n' '--'
        cat
      } > "$BASE_SCHEMA"
  ic su postgres -c "dropdb --if-exists $scratch"

  [ "$(grep -c 'CREATE TABLE public\.' "$BASE_SCHEMA")" -gt 0 ] \
    || { warn "schema export produced no tables; see /tmp/gen-schema.log"; return 1; }
  log "schema: wrote $BASE_SCHEMA ($(grep -c 'CREATE TABLE public\.' "$BASE_SCHEMA") tables)"

  if ic test -f /etc/systemd/system/sun-backend.service; then
    ic systemctl reset-failed sun-backend >/dev/null 2>&1 || true
    ic systemctl start sun-backend >/dev/null 2>&1 || true
  fi
}

deploy() {
  [ "${1:-}" = "--skip-build" ] || build_backend
  deploy_backend
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
  if [ "$AUTH" = "on" ]; then
    configure_redis
  else
    ic systemctl disable --now redis-server >/dev/null 2>&1 || true
  fi
  bootstrap_db
  if [ "$AUTH" = "on" ]; then
    install_permify
    wait_permify
    migrate_permify_schema
  else
    ic systemctl disable --now permify >/dev/null 2>&1 || true
  fi
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
  log "permify"
  curl -s -o /dev/null -w 'healthz %{http_code}\n' \
    "http://${CONTAINER_IP%/*}:3477/healthz" || warn "no response yet"
  log "backend"
  ic systemctl --no-pager --lines=0 status sun-backend 2>&1 | head -n 4 || true
  log "http"
  curl -s -o /dev/null -w 'http %{http_code} in %{time_total}s\n' \
    "http://$HOST_ALIAS:$HOST_PORT/graphql" || warn "no response yet"
}

case "${1:-}" in
  provision) provision ;;
  deploy) shift; deploy "${1:-}" ;;
  bootstrap)
    ic rm -f "$APP_DIR/.db-bootstrapped" >/dev/null 2>&1 || true
    bootstrap_db
    ;;
  gen-schema) gen_schema ;;
  status) status ;;
  shell) shift; sudo incus exec "$NAME" -- bash -l ;;
  stop) incus stop "$NAME" ;;
  start) incus start "$NAME" ;;
  autostart)
    incus config set "$NAME" boot.autostart "$AUTOSTART"
    log "boot.autostart=$AUTOSTART"
    ;;
  destroy)
    incus delete --force "$NAME" 2>/dev/null || true
    log "deleted $NAME (pool + loop image left in place)"
    ;;
  *) grep -E '^# ' "$SELF" | head -n 5; exit 1 ;;
esac
