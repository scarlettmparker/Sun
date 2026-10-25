#!/usr/bin/env bash
set -euo pipefail

SELF="$(readlink -f "${BASH_SOURCE[0]}")"
HERE="$(dirname "$SELF")"
SUN_ROOT="$(cd "$HERE/../.." && pwd)"
CONF="$HERE/containers.conf"
FILES="$HERE/files"

VPS="${VPS:-ubuntu@vps.scarlettparker.co.uk}"
SSH_KEY="${SSH_KEY:-$HOME/.ssh/ubuntu.key}"
SSH=(ssh -i "$SSH_KEY" -o BatchMode=yes -o ConnectTimeout=10)
BUN="${BUN:-$HOME/.bun/bin/bun}"
REMOTE_TMP=/tmp/sun-vps

# Prints a status line.
#
# @param $@ Message parts.
log() { printf '\033[1;36m==>\033[0m %s\n' "$*"; }
# Prints a warning to stderr.
#
# @param $@ Message parts.
warn() { printf '\033[1;33mwarn:\033[0m %s\n' "$*" >&2; }

# Runs a command on the VPS over SSH.
#
# @param $@ Command and arguments.
remote() { "${SSH[@]}" "$VPS" "$@"; }

# Prints the non-comment container manifest rows.
#
# @return One row per line on stdout.
containers() { grep -vE '^[[:space:]]*(#|$)' "$CONF"; }

# Copies helper files to the VPS staging directory.
upload_files() {
  remote "mkdir -p $REMOTE_TMP/files"
  rsync -az -e "ssh -i $SSH_KEY" "$FILES/" "$VPS:$REMOTE_TMP/files/"
}

# Creates and enables a 4 GiB swapfile on the VPS.
ensure_swap() {
  log "host: swap"
  remote 'sudo bash -s' <<'REMOTE'
set -euo pipefail
if ! swapon --show | grep -q /swapfile; then
  if [ ! -f /swapfile ]; then
    fallocate -l 4G /swapfile || dd if=/dev/zero of=/swapfile bs=1M count=4096
    chmod 600 /swapfile
    mkswap /swapfile >/dev/null
  fi
  swapon /swapfile
fi
grep -q '^/swapfile ' /etc/fstab || echo '/swapfile none swap sw 0 0' >>/etc/fstab
sysctl -qw vm.swappiness=10
REMOTE
}

# Sets the LXD DHCP range and opens the container SSH ports.
ensure_host_network() {
  log "host: DHCP range and firewall"
  remote 'sudo bash -s' <<'REMOTE'
set -euo pipefail
lxc network set lxdbr0 ipv4.dhcp.ranges 10.89.20.2-10.89.20.199
lxc network set lxdbr0 raw.dnsmasq "" 2>/dev/null || true
for port in 2201 2202 2203; do
  ufw allow "$port"/tcp >/dev/null 2>&1 || true
  iptables -C INPUT -p tcp -m tcp --dport "$port" -m state --state NEW -j ACCEPT 2>/dev/null \
    || iptables -I INPUT -p tcp -m tcp --dport "$port" -m state --state NEW -j ACCEPT
  ip6tables -C INPUT -p tcp -m tcp --dport "$port" -j ACCEPT 2>/dev/null \
    || ip6tables -I INPUT -p tcp -m tcp --dport "$port" -j ACCEPT
done
if command -v netfilter-persistent >/dev/null 2>&1; then
  netfilter-persistent save >/dev/null
fi
REMOTE
}

# Creates a container and removes any compute limits.
#
# @param $1 Container name.
# @param $2 Image alias.
ensure_container() {
  local name="$1" image="$2"
  if ! remote "sudo lxc info '$name' >/dev/null 2>&1"; then
    log "container: create $name"
    remote "sudo lxc init '$image' '$name' --storage default"
  fi
  for key in limits.cpu limits.cpu.allowance limits.memory limits.memory.enforce limits.memory.swap; do
    remote "sudo lxc config unset '$name' $key >/dev/null 2>&1 || true"
  done
  remote "sudo lxc config set '$name' boot.autostart true"
}

# Starts a container and waits for systemd.
#
# @param $1 Container name.
start_container() {
  local name="$1" state
  remote "sudo lxc start '$name'" >/dev/null 2>&1 || true
  for _ in $(seq 1 60); do
    state="$(remote "sudo lxc exec '$name' -- systemctl is-system-running 2>/dev/null || true" | tr -d '\r')"
    case "$state" in running | degraded) return 0 ;; esac
    sleep 1
  done
  warn "$name did not finish booting"
  return 1
}

# Writes a static network configuration and waits for the address.
#
# @param $1 Container name.
# @param $2 IPv4 address.
configure_network() {
  local name="$1" ip="$2"
  remote "sudo lxc exec '$name' -- bash -s" <<REMOTE
set -euo pipefail
cat >/etc/systemd/network/10-sun-vps.network <<EOF
[Match]
Name=eth0

[Network]
Address=$ip/24
Gateway=10.89.20.1
DNS=10.89.20.1
DNS=1.1.1.1

[Link]
RequiredForOnline=no
EOF
rm -f /etc/systemd/network/10-cloud-init-eth0.network /etc/systemd/network/eth0.network
touch /etc/cloud/cloud-init.disabled
systemctl mask systemd-networkd-wait-online.service >/dev/null 2>&1 || true
systemctl restart systemd-networkd
REMOTE
  for _ in $(seq 1 30); do
    if remote "sudo lxc exec '$name' -- ip -4 -o addr show eth0" | grep -q "$ip/24"; then
      return 0
    fi
    sleep 1
  done
  warn "$name did not get $ip"
  return 1
}

# Installs Debian packages in a container.
#
# @param $1 Container name.
# @param $2 Space-separated package list.
install_packages() {
  local name="$1" packages="$2"
  log "guest: $name packages"
  remote "sudo lxc exec '$name' -- env DEBIAN_FRONTEND=noninteractive apt-get update -qq"
  remote "sudo lxc exec '$name' -- env DEBIAN_FRONTEND=noninteractive apt-get install -y -qq -o Dpkg::Options::=--force-confold $packages"
}

# Copies the host Bun binary into a container.
#
# @param $1 Container name.
install_bun() {
  local name="$1"
  if remote "sudo lxc exec '$name' -- test -x /usr/local/bin/bun"; then
    return 0
  fi
  remote "sudo lxc file push -q -p --mode 0755 /home/ubuntu/.bun/bin/bun '$name/usr/local/bin/bun'"
}

# Copies the host Temurin 25 runtime into a container.
#
# @param $1 Container name.
install_temurin() {
  local name="$1"
  if remote "sudo lxc exec '$name' -- test -x /opt/java/temurin-25/bin/java"; then
    return 0
  fi
  remote "sudo lxc exec '$name' -- rm -rf /opt/java/temurin-25"
  remote "sudo lxc exec '$name' -- mkdir -p /opt/java"
  remote "sudo lxc file push -q -r /opt/java/temurin-25 '$name/opt/java/'"
  remote "sudo lxc exec '$name' -- ln -sfn /opt/java/temurin-25/bin/java /usr/local/bin/java"
}

# Creates the container SSH user and installs authorized keys.
#
# @param $1 Container name.
# @param $2 SSH user name.
setup_user() {
  local name="$1" user="$2" keys_b64
  keys_b64="$(base64 -w0 "$FILES/authorized_keys")"
  remote "sudo lxc file push -q -p --mode 0755 $REMOTE_TMP/files/setup-container.sh '$name/tmp/setup-container.sh'"
  remote "sudo lxc exec '$name' -- bash /tmp/setup-container.sh '$user' '$keys_b64'"
}

# Applies the small-box Postgres tuning.
#
# @param $1 Container name.
configure_postgres() {
  local name="$1" pgdir
  log "guest: $name postgres tuning"
  pgdir="$(remote "sudo lxc exec '$name' -- bash -c 'ls -d /etc/postgresql/*/main | head -n1'" | tr -d '\r')"
  remote "sudo lxc exec '$name' -- mkdir -p '$pgdir/conf.d'"
  remote "sudo lxc file push -q -p --mode 0644 $REMOTE_TMP/files/postgres-tuning.conf '$name$pgdir/conf.d/99-sun-vps.conf'"
  remote "sudo lxc exec '$name' -- systemctl restart postgresql"
}

# Applies the journal disk limits.
#
# @param $1 Container name.
configure_journald() {
  local name="$1"
  remote "sudo lxc exec '$name' -- mkdir -p /etc/systemd/journald.conf.d"
  remote "sudo lxc file push -q -p --mode 0644 $REMOTE_TMP/files/journald-sun.conf '$name/etc/systemd/journald.conf.d/99-sun.conf'"
  remote "sudo lxc exec '$name' -- systemctl restart systemd-journald"
}

# Applies the Redis memory limit.
#
# @param $1 Container name.
configure_redis() {
  local name="$1"
  remote "sudo lxc exec '$name' -- bash -s" <<'REMOTE'
set -euo pipefail
grep -q '^# sun-vps' /etc/redis/redis.conf || printf '\n# sun-vps\nmaxmemory 32mb\nmaxmemory-policy volatile-lru\n' >>/etc/redis/redis.conf
systemctl restart redis-server
REMOTE
}

# Adds a container host key to the CI known_hosts file.
#
# @param $1 Container name.
# @param $2 Container IPv4 address.
# @param $3 Host SSH port.
ssh_host_keys() {
  local name="$1" ip="$2" port="$3" key host
  key="$(remote "sudo lxc exec '$name' -- cat /etc/ssh/ssh_host_ed25519_key.pub" | tr -d '\r')"
  for host in "$ip" "[vps.scarlettparker.co.uk]:$port"; do
    remote "sudo bash -c 'grep -qF \"$host \" /opt/woodpecker/keys/known_hosts || echo \"$host $key\" >>/opt/woodpecker/keys/known_hosts'"
  done
}

# Provisions one container from its manifest row.
#
# @param $1 Container name.
# @param $2 Reserved IPv4 address.
# @param $3 Host SSH port.
# @param $4 Image alias.
provision_one() {
  local name="$1" ip="$2" port="$3" image="$4"
  log "container: $name"
  ensure_container "$name" "$image"
  start_container "$name" "$ip"
  configure_network "$name" "$ip"
  install_packages "$name" "openssh-server ca-certificates curl jq rsync unzip"
  setup_user "$name" "$name"
  configure_journald "$name"
  install_bun "$name"

  case "$name" in
    artemis)
      install_packages "$name" "postgresql redis-server sqlite3"
      install_temurin "$name"
      configure_postgres "$name"
      configure_redis "$name"
      ;;
    medusa)
      install_packages "$name" "sqlite3"
      ;;
  esac

  if ! remote "sudo lxc config device show '$name' | grep -q '^ssh:'"; then
    remote "sudo lxc config device add '$name' ssh proxy listen=tcp:0.0.0.0:$port connect=tcp:127.0.0.1:22"
  fi
  ssh_host_keys "$name" "$ip" "$port"
}

# Applies the full VPS container configuration.
provision() {
  local rows
  mapfile -t rows < <(containers)
  upload_files
  ensure_swap
  ensure_host_network
  for row in "${rows[@]}"; do
    read -r name ip port image <<<"$row"
    ensure_container "$name" "$image"
  done
  for row in "${rows[@]}"; do
    read -r name ip port image <<<"$row"
    provision_one "$name" "$ip" "$port" "$image"
  done
  remote 'sudo bash -s' <<'REMOTE'
set -euo pipefail
for entry in "10.89.20.201 cassini" "10.89.20.202 artemis" "10.89.20.203 medusa"; do
  grep -q " ${entry#* }$" /etc/hosts || echo "$entry" >>/etc/hosts
done
install -d /etc/systemd/journald.conf.d
install -m 0644 /tmp/sun-vps/files/journald-sun.conf /etc/systemd/journald.conf.d/99-sun.conf
systemctl restart systemd-journald
install -m 0755 /tmp/sun-vps/files/backup-daily.sh /usr/local/bin/sun-vps-backup
install -m 0644 /tmp/sun-vps/files/sun-vps-backup.service /etc/systemd/system/sun-vps-backup.service
install -m 0644 /tmp/sun-vps/files/sun-vps-backup.timer /etc/systemd/system/sun-vps-backup.timer
systemctl daemon-reload
systemctl enable --now sun-vps-backup.timer
REMOTE
  status
}

# Prints SSH aliases for the containers.
ssh_config() {
  cat <<EOF
Host cassini
  HostName 10.89.20.201
  User cassini
  ProxyJump vps
  IdentityFile $SSH_KEY

Host artemis
  HostName 10.89.20.202
  User artemis
  ProxyJump vps
  IdentityFile $SSH_KEY

Host medusa
  HostName 10.89.20.203
  User medusa
  ProxyJump vps
  IdentityFile $SSH_KEY

Host cassini-port
  HostName vps.scarlettparker.co.uk
  User cassini
  Port 2201
  IdentityFile $SSH_KEY

Host artemis-port
  HostName vps.scarlettparker.co.uk
  User artemis
  Port 2202
  IdentityFile $SSH_KEY

Host medusa-port
  HostName vps.scarlettparker.co.uk
  User medusa
  Port 2203
  IdentityFile $SSH_KEY
EOF
}

# Shows container state and SSH reachability.
status() {
  local name ip
  log "containers"
  for name in cassini artemis medusa; do
    remote "sudo lxc list '$name' --format compact"
  done
  log "ssh"
  for name in cassini artemis medusa; do
    case "$name" in
      cassini) ip=10.89.20.201 ;;
      artemis) ip=10.89.20.202 ;;
      medusa) ip=10.89.20.203 ;;
    esac
    printf '  %s: ' "$name"
    if ssh -i "$SSH_KEY" -o BatchMode=yes -o ConnectTimeout=5 -o StrictHostKeyChecking=accept-new \
      -o "ProxyCommand=ssh -i $SSH_KEY -W %h:%p ubuntu@vps.scarlettparker.co.uk" \
      "$name@$ip" true >/dev/null 2>&1; then
      echo ok
    else
      echo unreachable
    fi
  done
}

# Extracts a host, port, or database from the local JDBC URL.
#
# @param $1 One of host, port, or db.
pg_url_part() {
  local url
  url="$(envval SPRING_DATASOURCE_URL)"
  case "$1" in
    host) sed -E 's#^jdbc:postgresql://([^:/]+).*#\1#' <<<"$url" ;;
    port) sed -E 's#^jdbc:postgresql://[^:/]+:([0-9]+)/.*#\1#' <<<"$url" ;;
    db) sed -E 's#^jdbc:postgresql://[^/]+/([^?]+).*#\1#' <<<"$url" ;;
  esac
}

# Reads one key from the local Sun backend env.
#
# @param $1 Environment key.
envval() {
  grep -E "^$1=" "$SUN_ROOT/components/sun-graphql/src/main/resources/.env" 2>/dev/null | head -n1 | cut -d= -f2- || true
}

# Archives the VPS database, app directories, and nginx configuration.
backup() {
  local ts path tmp
  ts="$(date -u +%Y%m%dT%H%M%SZ)"
  path="/var/backups/sun-vps/$ts"
  log "backup: $path"
  remote "sudo mkdir -p '$path'"
  remote "mkdir -p $REMOTE_TMP"
  remote "sudo bash -c 'sudo -u postgres pg_dump -Fc -d sun >$path/vps-sun.dump'"
  remote "sudo bash -c 'tar -czf \"$path/host-opt.tgz\" --exclude=node_modules /opt/sun /opt/guided-reader /opt/permify 2>/dev/null || true'"
  remote "sudo bash -c 'tar -czf \"$path/host-config.tgz\" /etc/nginx /etc/letsencrypt /home/ubuntu/.config/systemd/user 2>/dev/null || true'"
  tmp="$(mktemp -d)"
  PGPASSWORD="$(envval SPRING_DATASOURCE_PASSWORD)" pg_dump \
    -h "$(pg_url_part host)" -p "$(pg_url_part port)" \
    -U "$(envval SPRING_DATASOURCE_USERNAME)" -d "$(pg_url_part db)" \
    -Fc -f "$tmp/local-sun.dump"
  rsync -az -e "ssh -i $SSH_KEY" "$tmp/local-sun.dump" "$VPS:$REMOTE_TMP/local-sun.dump.tmp"
  remote "sudo mv '$REMOTE_TMP/local-sun.dump.tmp' '$path/local-sun.dump'"
  rm -rf "$tmp"
  remote "sudo bash -c 'sha256sum $path/* >$path/SHA256SUMS'"
  log "backup complete: $path"
}

# Replaces the artemis database with the local Sun database and resets the hi@ password.
db_import() {
  local tmp pw new_pw hash sql role_sql
  tmp="$(mktemp -d)"
  pw="$(envval SPRING_DATASOURCE_PASSWORD)"
  [ -n "$pw" ] || { warn "no SPRING_DATASOURCE_PASSWORD in the Sun env"; return 1; }

  log "dump: local sun"
  PGPASSWORD="$pw" pg_dump \
    -h "$(pg_url_part host)" -p "$(pg_url_part port)" \
    -U "$(envval SPRING_DATASOURCE_USERNAME)" -d "$(pg_url_part db)" \
    -Fc -f "$tmp/sun.dump"

  log "restore: artemis sun"
  rsync -az -e "ssh -i $SSH_KEY" "$tmp/sun.dump" "$VPS:$REMOTE_TMP/sun.dump"
  remote "sudo lxc exec 'artemis' -- rm -f /tmp/sun.dump /tmp/role.sql /tmp/password.sql"
  remote "sudo lxc file push -q -p --mode 0644 $REMOTE_TMP/sun.dump 'artemis/tmp/sun.dump'"

  role_sql="$tmp/role.sql"
  {
    printf 'DO $$\nBEGIN\n'
    printf "  IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'sun') THEN\n"
    printf "    CREATE ROLE sun LOGIN PASSWORD '%s';\n" "${pw//\'/\'\'}"
    printf '  ELSE\n'
    printf "    ALTER ROLE sun WITH LOGIN PASSWORD '%s';\n" "${pw//\'/\'\'}"
    printf '  END IF;\nEND\n$$;\n'
  } >"$role_sql"
  rsync -az -e "ssh -i $SSH_KEY" "$role_sql" "$VPS:$REMOTE_TMP/role.sql"
  remote "sudo lxc file push -q -p --mode 0644 $REMOTE_TMP/role.sql 'artemis/tmp/role.sql'"
  remote "sudo lxc exec 'artemis' -- sudo -u postgres psql -v ON_ERROR_STOP=1 -f /tmp/role.sql"
  remote "sudo lxc exec 'artemis' -- sudo -u postgres psql -v ON_ERROR_STOP=1 -c \"SELECT pg_terminate_backend(pid) FROM pg_stat_activity WHERE datname='sun'\""
  remote "sudo lxc exec 'artemis' -- sudo -u postgres dropdb --if-exists sun"
  remote "sudo lxc exec 'artemis' -- sudo -u postgres createdb -O sun sun"
  remote "sudo lxc exec 'artemis' -- sudo -u postgres pg_restore --no-owner --role=sun -d sun /tmp/sun.dump"
  remote "sudo lxc exec 'artemis' -- rm -f /tmp/sun.dump /tmp/role.sql"

  new_pw="$(openssl rand -hex 16)"
  hash="$(PW="$new_pw" "$BUN" -e 'console.log(await Bun.password.hash(process.env.PW, { algorithm: "bcrypt", cost: 10 }))')"
  sql="$tmp/password.sql"
  printf "UPDATE gaia_accounts SET password_hash = '%s', lastupdatedat = now() WHERE username = 'hi@scarlettparker.co.uk';\n" "${hash//\'/\'\'}" >"$sql"
  rsync -az -e "ssh -i $SSH_KEY" "$sql" "$VPS:$REMOTE_TMP/password.sql"
  remote "sudo lxc file push -q -p --mode 0644 $REMOTE_TMP/password.sql 'artemis/tmp/password.sql'"
  remote "sudo lxc exec 'artemis' -- sudo -u postgres psql -v ON_ERROR_STOP=1 -d sun -f /tmp/password.sql"
  remote "sudo lxc exec 'artemis' -- rm -f /tmp/password.sql"

  mkdir -p "$HOME/.local/share/sun-vps"
  umask 077
  printf 'username=hi@scarlettparker.co.uk\npassword=%s\n' "$new_pw" >"$HOME/.local/share/sun-vps/hi-password"
  rm -rf "$tmp"
  log "hi@scarlettparker.co.uk password: $new_pw"
}

case "${1:-}" in
  provision | apply) provision ;;
  backup) backup ;;
  db-import) db_import ;;
  status) status ;;
  ssh-config) ssh_config ;;
  shell)
    name="${2:?container required}"
    "${SSH[@]}" -t "$VPS" "sudo lxc exec '$name' -- bash -l"
    ;;
  destroy)
    name="${2:?container required}"
    remote "sudo lxc delete --force '$name'"
    ;;
  *)
    echo "usage: vps.sh <provision|backup|db-import|status|ssh-config|shell <name>|destroy <name>>" >&2
    exit 1
    ;;
esac
