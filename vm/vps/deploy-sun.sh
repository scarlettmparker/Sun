#!/usr/bin/env bash
set -euo pipefail

SELF="$(readlink -f "${BASH_SOURCE[0]}")"
HERE="$(dirname "$SELF")"
SUN_ROOT="$(cd "$HERE/../.." && pwd)"
VPS="${VPS:-ubuntu@vps.scarlettparker.co.uk}"
SSH_KEY="${SSH_KEY:-$HOME/.ssh/ubuntu.key}"
ARTEMIS=artemis
REMOTE_TMP=/tmp/sun-vps
BACKEND_ENV="$SUN_ROOT/components/sun-graphql/src/main/resources/.env"
FRONTEND_ENV="$SUN_ROOT/.env"
FREE_CHESS_ENV="${FREE_CHESS_ENV:-$SUN_ROOT/../Free-Chess/.env}"
JAR="$SUN_ROOT/components/sun-graphql/build/libs/sun-graphql-0.0.1-SNAPSHOT.jar"
BUN="${BUN:-$HOME/.bun/bin/bun}"
# Prints a status line.
#
# @param $@ Message parts.
log() { printf '\033[1;36m==>\033[0m %s\n' "$*"; }

# Prints a warning to stderr.
#
# @param $@ Message parts.
warn() { printf '\033[1;33mwarn:\033[0m %s\n' "$*" >&2; }

# Runs a command on the VPS.
#
# @param $@ Command and arguments.
remote() { ssh -i "$SSH_KEY" -o BatchMode=yes "$VPS" "$@"; }

# Reads one key from an env file.
#
# @param $1 Key name.
# @param $2 Env file path.
envval() { grep -E "^$1=" "$2" 2>/dev/null | head -n1 | cut -d= -f2- || true; }
# Builds the Sun frontend and backend JAR.
build() {
  local log
  log="$(mktemp "${TMPDIR:-/tmp}/sun-build.XXXXXX.log")"
  log "build: Sun frontend"
  if ! (cd "$SUN_ROOT" && "$BUN" run build >"$log" 2>&1); then
    cat "$log" >&2
    rm -f "$log"
    return 1
  fi
  rm -f "$log"
  log "build: sun-graphql"
  "$SUN_ROOT/components/sun-graphql/gradlew" -p "$SUN_ROOT/components" :sun-graphql:bootJar --quiet
}
# Hashes the production dependency inputs.
#
# @return The combined digest on stdout.
deps_hash() {
  sha256sum "$SUN_ROOT/package.json" "$SUN_ROOT/bun.lock" | sha256sum | cut -d' ' -f1
}

# Builds production node_modules on the ARM64 VPS, reusing a cached tree.
build_deps() {
  local hash dir
  hash="$(deps_hash)"
  dir="/home/ubuntu/sun-vps-deps/$hash"
  if remote "test -d '$dir/node_modules'"; then
    log "deps: cached ($hash)"
    return 0
  fi
  log "deps: ARM64 node_modules on the VPS"
  remote "rm -rf '$dir' && mkdir -p '$dir'"
  rsync -az -e "ssh -i $SSH_KEY" \
    "$SUN_ROOT/package.json" "$SUN_ROOT/bun.lock" "$SUN_ROOT/bunfig.toml" \
    "$VPS:$dir/"
  remote "cd '$dir' && /home/ubuntu/.bun/bin/bun install --production --frozen-lockfile --no-progress"
}
# Copies an env file and applies VPS overrides.
#
# @param $1 Output path.
# @param $2 Source env path.
render_env() {
  local out="$1" file="$2"
  umask 077
  cp "$file" "$out"
  sed -i \
    -e 's|^GRAPHQL_ENDPOINT=.*|GRAPHQL_ENDPOINT=http://127.0.0.1:8083/graphql|' \
    -e 's|^SPRING_DATASOURCE_URL=.*|SPRING_DATASOURCE_URL=jdbc:postgresql://127.0.0.1:5432/sun|' \
    -e 's|^SPRING_DATASOURCE_USERNAME=.*|SPRING_DATASOURCE_USERNAME=sun|' \
    -e 's|^SERVER_HOST=.*|SERVER_HOST=0.0.0.0|' \
    -e 's|^SERVER_PORT=.*|SERVER_PORT=5173|' \
    -e 's|^VITE_SERVER_BASE=.*|VITE_SERVER_BASE=https://sun.scarlettparker.co.uk|' \
    -e 's|^ALLOWED_HOSTS=.*|ALLOWED_HOSTS=sun.scarlettparker.co.uk,localhost|' \
    -e 's|^DISCORD_REDIRECT_URI=.*|DISCORD_REDIRECT_URI=https://reader.scarlettparker.co.uk/auth/discord/callback|' \
    -e 's|^APP_BASE_URL=.*|APP_BASE_URL=https://sun.scarlettparker.co.uk|' \
    -e 's|^PERMIFY_ENABLED=.*|PERMIFY_ENABLED=true|' \
    -e 's|^AUDIT_ENABLED=.*|AUDIT_ENABLED=true|' \
    -e 's|^RATE_LIMIT_ENABLED=.*|RATE_LIMIT_ENABLED=true|' \
    -e 's|^IP_WHITELIST_ENABLED=.*|IP_WHITELIST_ENABLED=false|' \
    -e 's|^CEFR_INFERENCE_ENABLED=.*|CEFR_INFERENCE_ENABLED=false|' \
    "$out"
}
# Renders the backend env and wires the Free Chess client secret.
#
# @param $1 Output path.
render_backend_env() {
  local out="$1" secret
  render_env "$out" "$BACKEND_ENV"
  secret="$(envval CSRF_SECRET "$FREE_CHESS_ENV")"
  if grep -q '^FREE_CHESS_CLIENT_SECRET=' "$out"; then
    sed -i "s|^FREE_CHESS_CLIENT_SECRET=.*|FREE_CHESS_CLIENT_SECRET=$secret|" "$out"
  else
    printf 'FREE_CHESS_CLIENT_SECRET=%s\n' "$secret" >>"$out"
  fi
  if grep -q '^IP_WHITELIST_ENABLED=' "$out"; then
    sed -i 's|^IP_WHITELIST_ENABLED=.*|IP_WHITELIST_ENABLED=false|' "$out"
  else
    printf 'IP_WHITELIST_ENABLED=false\n' >>"$out"
  fi
}
# Renders the frontend env.
#
# @param $1 Output path.
render_frontend_env() {
  local out="$1"
  render_env "$out" "$FRONTEND_ENV"
  grep -q '^APP_BASE_URL=' "$out" || printf 'APP_BASE_URL=https://sun.scarlettparker.co.uk\n' >>"$out"
}
# Copies a file into an artemis path.
#
# @param $1 Local path.
# @param $2 Container path.
push_file() {
  local local_path="$1" remote_path="$2"
  remote "mkdir -p $REMOTE_TMP"
  rsync -az -e "ssh -i $SSH_KEY" "$local_path" "$VPS:$REMOTE_TMP/"
  remote "sudo lxc exec '$ARTEMIS' -- rm -f '$remote_path'"
  remote "sudo lxc file push -q -p --mode 0644 '$REMOTE_TMP/$(basename "$local_path")' '$ARTEMIS$remote_path'"
}
# Installs Permify, its config, unit, and schema.
deploy_permify() {
  local arch=arm64 version=v1.7.3 tmp schema_json
  if [ ! -f "$BACKEND_ENV" ]; then
    log "permify: no local env, keeping remote config"
    return 0
  fi
  log "permify: binary and config"
  remote "sudo mkdir -p /tmp/sun-vps"
  if ! remote "test -x /tmp/sun-vps/permify"; then
    remote "curl -fsSL https://github.com/Permify/permify/releases/download/$version/permify_${version#v}_Linux_$arch.tar.gz -o /tmp/sun-vps/permify.tgz"
    remote "tar -xzf /tmp/sun-vps/permify.tgz -C /tmp/sun-vps permify && chmod +x /tmp/sun-vps/permify"
  fi
  remote "sudo lxc exec '$ARTEMIS' -- rm -f /opt/permify/permify /opt/permify/permify.yaml /opt/permify/schema.zed"
  remote "sudo lxc exec '$ARTEMIS' -- mkdir -p /opt/permify"
  remote "sudo lxc file push -q -p --mode 0755 /tmp/sun-vps/permify '$ARTEMIS/opt/permify/permify'"

  tmp="$(mktemp)"
  SPRING_DATASOURCE_PASSWORD="$(envval SPRING_DATASOURCE_PASSWORD "$BACKEND_ENV")" \
    envsubst '${SPRING_DATASOURCE_PASSWORD}' <"$SUN_ROOT/permify/permify.yaml" >"$tmp"
  push_file "$tmp" "/opt/permify/permify.yaml"
  rm -f "$tmp"
  push_file "$SUN_ROOT/permify/schema.zed" "/opt/permify/schema.zed"
  push_file "$HERE/files/permify.service" "/etc/systemd/system/permify.service"

  remote "sudo lxc exec '$ARTEMIS' -- systemctl daemon-reload"
  remote "sudo lxc exec '$ARTEMIS' -- systemctl enable permify"
  remote "sudo lxc exec '$ARTEMIS' -- systemctl restart permify"
  for _ in $(seq 1 30); do
    remote "sudo lxc exec '$ARTEMIS' -- curl -sf http://127.0.0.1:3477/healthz >/dev/null" && break
    sleep 1
  done

  schema_json="$(mktemp)"
  python3 -c 'import json,sys; print(json.dumps({"schema": open(sys.argv[1]).read()}))' \
    "$SUN_ROOT/permify/schema.zed" >"$schema_json"
  push_file "$schema_json" "/tmp/permify-schema.json"
  remote "sudo lxc exec '$ARTEMIS' -- curl -sf -X POST http://127.0.0.1:3477/v1/tenants/t1/schemas/write -H 'content-type: application/json' -d @/tmp/permify-schema.json >/dev/null"
  rm -f "$schema_json"
}
# Deploys the backend JAR, env, unit, and waits for health.
deploy_backend() {
  local tmp
  log "backend: jar, env, unit"
  remote "sudo lxc exec '$ARTEMIS' -- mkdir -p /opt/sun/backend"
  push_file "$JAR" "/opt/sun/backend/sun-graphql.jar"
  if [ -f "$BACKEND_ENV" ]; then
    tmp="$(mktemp)"
    render_backend_env "$tmp"
    push_file "$tmp" "/opt/sun/backend/.env"
    rm -f "$tmp"
  fi
  push_file "$HERE/files/sun-backend.service" "/etc/systemd/system/sun-backend.service"
  remote "sudo lxc exec '$ARTEMIS' -- chmod 0600 /opt/sun/backend/.env"
  remote "sudo lxc exec '$ARTEMIS' -- systemctl daemon-reload"
  remote "sudo lxc exec '$ARTEMIS' -- systemctl enable sun-backend"
  remote "sudo lxc exec '$ARTEMIS' -- systemctl restart sun-backend"
  for _ in $(seq 1 120); do
    if remote "sudo lxc exec '$ARTEMIS' -- curl -sf http://127.0.0.1:8083/graphql/schema >/dev/null"; then
      log "backend: healthy"
      return 0
    fi
    sleep 1
  done
  remote "sudo lxc exec '$ARTEMIS' -- journalctl -u sun-backend -n 80 --no-pager" || true
  return 1
}
# Deploys the frontend bundle, dependencies, env, unit, and waits for health.
deploy_frontend() {
  local tmp app_tar
  log "frontend: bundle and dependencies"
  app_tar="$(mktemp -d)/sun-web.tgz"
  tar -czf "$app_tar" -C "$SUN_ROOT" \
    dist/client dist/server server.js config.js config.d.ts css-plugin.ts \
    package.json tsconfig.json bunfig.toml bun.lock .bun-version \
    routes messages src utils vm/sun

  remote "sudo lxc exec '$ARTEMIS' -- mkdir -p /opt/sun/web"
  remote "sudo lxc exec '$ARTEMIS' -- bash -c 'find /opt/sun/web -mindepth 1 -maxdepth 1 ! -name node_modules ! -name .env.production -exec rm -rf {} +'"
  remote "mkdir -p $REMOTE_TMP"
  rsync -az -e "ssh -i $SSH_KEY" "$app_tar" "$VPS:$REMOTE_TMP/sun-web.tgz"
  remote "sudo lxc exec '$ARTEMIS' -- rm -f /tmp/sun-web.tgz"
  remote "sudo lxc file push -q -p --mode 0644 '$REMOTE_TMP/sun-web.tgz' '$ARTEMIS/tmp/sun-web.tgz'"
  remote "sudo lxc exec '$ARTEMIS' -- tar -xzf /tmp/sun-web.tgz -C /opt/sun/web"

  remote "sudo mkdir -p /opt/sun/web/node_modules"
  remote "sudo rsync -az --delete -e 'ssh -i /opt/woodpecker/keys/ci-deploy -p 2202 -o StrictHostKeyChecking=no -o UserKnownHostsFile=/dev/null' /home/ubuntu/sun-vps-deps/$(deps_hash)/node_modules/ root@127.0.0.1:/opt/sun/web/node_modules/"

  if [ -f "$FRONTEND_ENV" ]; then
    tmp="$(mktemp)"
    render_frontend_env "$tmp"
    push_file "$tmp" "/opt/sun/web/.env.production"
    rm -f "$tmp"
  fi
  remote "sudo lxc exec '$ARTEMIS' -- bash -c 'test ! -f /opt/sun/web/.env.production || chmod 0600 /opt/sun/web/.env.production'"
  push_file "$HERE/files/sun-frontend.service" "/etc/systemd/system/sun-frontend.service"
  remote "sudo lxc exec '$ARTEMIS' -- systemctl daemon-reload"
  remote "sudo lxc exec '$ARTEMIS' -- systemctl enable sun-frontend"
  remote "sudo lxc exec '$ARTEMIS' -- systemctl restart sun-frontend"
  for _ in $(seq 1 60); do
    if remote "sudo lxc exec '$ARTEMIS' -- curl -sf http://127.0.0.1:5173/ >/dev/null"; then
      log "frontend: healthy"
      return 0
    fi
    sleep 1
  done
  remote "sudo lxc exec '$ARTEMIS' -- journalctl -u sun-frontend -n 80 --no-pager" || true
  return 1
}

case "${1:-all}" in
  build) build ;;
  deps) build_deps ;;
  permify) deploy_permify ;;
  backend) deploy_backend ;;
  frontend) deploy_frontend ;;
  all)
    if [ "${2:-}" != "--skip-build" ]; then
      build
    fi
    build_deps
    deploy_permify
    deploy_backend
    deploy_frontend
    ;;
  *)
    echo "usage: deploy-sun.sh <all|build|deps|permify|backend|frontend> [--skip-build]" >&2
    exit 1
    ;;
esac
