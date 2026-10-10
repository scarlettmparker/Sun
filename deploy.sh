#!/usr/bin/env bash
set -euo pipefail

VM="${VM:-ubuntu@vps.scarlettparker.co.uk}"
SSH_KEY="${SSH_KEY:-$HOME/.ssh/ubuntu.key}"
SSH="ssh -i $SSH_KEY $VM"
APP_DIR="$(cd "$(dirname "$0")" && pwd)"

DOMAIN="sun.scarlettparker.co.uk"
SERVICE="sun-frontend"
DEPLOY_DIR="/opt/sun/web"
LIVE_PORT=5173
STANDBY_PORT=15173

usage() {
  echo "Usage: deploy.sh <backend|frontend|all> [--skip-build]"
  exit 1
}

[ $# -lt 1 ] && usage
TARGET="$1"
SKIP_BUILD="${2:-}"

build_backend() {
  echo "==> Building sun-graphql JAR..."
  "$APP_DIR/components/sun-graphql/gradlew" -p "$APP_DIR/components" \
    :sun-graphql:bootJar --quiet
  echo "    $(ls -lh "$APP_DIR/components/sun-graphql/build/libs/"*.jar | awk '{print $9, $5}')"
}

build_frontend() {
  echo "==> Building Sun frontend..."
  cd "$APP_DIR"
  npm run build --quiet
  echo "    dist/client/ $(du -sh dist/client | cut -f1)"
  echo "    dist/server/ $(du -sh dist/server | cut -f1)"
}

deploy_permify() {
  echo "==> Deploying permify..."
  VPS_ARCH=$($SSH "uname -m")
  if [ "$VPS_ARCH" = "aarch64" ] || [ "$VPS_ARCH" = "arm64" ]; then
    PERMIFY_ARCH="arm64"
  else
    PERMIFY_ARCH="amd64"
  fi
  PERMIFY_VERSION="v1.7.3"
  TMPDIR=$(mktemp -d)
  PERMIFY_BIN=""
  # Prefer checked-in arch-specific binary (permify for amd64, permify-arm64 for arm64)
  if [ "$PERMIFY_ARCH" = "arm64" ] && [ -f "$APP_DIR/permify/permify-arm64" ]; then
    PERMIFY_BIN="$APP_DIR/permify/permify-arm64"
    echo "    Using checked-in permify-arm64 for VPS (aarch64)"
  elif [ "$PERMIFY_ARCH" = "amd64" ] && [ -f "$APP_DIR/permify/permify" ]; then
    if file "$APP_DIR/permify/permify" | grep -q -E "x86-64|amd64"; then
      PERMIFY_BIN="$APP_DIR/permify/permify"
      echo "    Using checked-in permify for VPS (amd64)"
    fi
  fi
  # Fallback: use generic permify if arch matches, else download to temp
  if [ -z "$PERMIFY_BIN" ]; then
    if [ -f "$APP_DIR/permify/permify" ]; then
      if file "$APP_DIR/permify/permify" | grep -q -E "aarch64|ARM" && [ "$PERMIFY_ARCH" = "arm64" ]; then
        PERMIFY_BIN="$APP_DIR/permify/permify"
      elif file "$APP_DIR/permify/permify" | grep -q -E "x86-64|amd64" && [ "$PERMIFY_ARCH" = "amd64" ]; then
        PERMIFY_BIN="$APP_DIR/permify/permify"
      fi
    fi
  fi
  if [ -z "$PERMIFY_BIN" ]; then
    if [ -f "$APP_DIR/permify/permify" ]; then
      echo "    Repo permify binary arch mismatch (VPS is $VPS_ARCH), downloading $PERMIFY_ARCH to temp..."
    else
      echo "    Permify binary not in repo, downloading $PERMIFY_ARCH to temp..."
    fi
    curl -fsSL "https://github.com/Permify/permify/releases/download/${PERMIFY_VERSION}/permify_${PERMIFY_VERSION#v}_Linux_${PERMIFY_ARCH}.tar.gz" -o "$TMPDIR/permify.tar.gz"
    tar -xzf "$TMPDIR/permify.tar.gz" -C "$TMPDIR"
    PERMIFY_BIN="$TMPDIR/permify"
    chmod +x "$PERMIFY_BIN"
  fi
  # Copy binary, yaml, schema to VM
  $SSH "sudo mkdir -p /opt/permify && sudo chown ubuntu:ubuntu /opt/permify"
  rsync -avz -e "ssh -i $SSH_KEY" "$PERMIFY_BIN" "$VM:/opt/permify/permify"
  $SSH "chmod +x /opt/permify/permify"
  rsync -avz -e "ssh -i $SSH_KEY" "$APP_DIR/permify/permify.yaml" "$APP_DIR/permify/schema.zed" "$VM:/opt/permify/"
  rm -rf "$TMPDIR"
  # Render permify.yaml with actual password from backend .env and install systemd unit
  $SSH bash -s <<'REMOTE'
    set -euo pipefail
    # Expand ${SPRING_DATASOURCE_PASSWORD} in yaml from backend env
    if [ -f /opt/sun/backend/.env ]; then
      set -a; source /opt/sun/backend/.env; set +a
      export SPRING_DATASOURCE_PASSWORD
      # Use envsubst if available, else sed
      if command -v envsubst >/dev/null 2>&1; then
        envsubst < /opt/permify/permify.yaml > /tmp/permify.yaml.tmp && sudo mv /tmp/permify.yaml.tmp /opt/permify/permify.yaml
      else
        sudo sed -i "s|\${SPRING_DATASOURCE_PASSWORD}|${SPRING_DATASOURCE_PASSWORD}|g" /opt/permify/permify.yaml
      fi
    fi
    # Install systemd unit
    sudo tee /etc/systemd/system/permify.service > /dev/null <<'UNIT'
[Unit]
Description=Permify Authorization Service
After=network.target postgresql.service
Requires=postgresql.service

[Service]
Type=simple
ExecStart=/opt/permify/permify serve --config /opt/permify/permify.yaml
Restart=always
RestartSec=5

[Install]
WantedBy=multi-user.target
UNIT
    sudo systemctl daemon-reload
    sudo systemctl enable --now permify
    echo "    Waiting for permify health..."
    for i in $(seq 1 30); do
      if curl -sf http://localhost:3477/healthz > /dev/null 2>&1; then
        echo "    Permify healthy after ${i}s"
        break
      fi
      sleep 1
    done
    # Migrate schema (idempotent)
    echo "    Writing permify schema..."
    SCHEMA_JSON=$(jq -Rs . < /opt/permify/schema.zed)
    curl -sf -X POST http://localhost:3477/v1/tenants/t1/schemas/write -H "content-type: application/json" -d "{\"schema\": $SCHEMA_JSON}" > /dev/null
    echo "    Schema migrated."
REMOTE
  echo "    Permify deployed."
}

setup_vm() {
  echo "==> Setting up VM directories and systemd..."
  $SSH bash -s <<'REMOTE'
    mkdir -p ~/.config/systemd/user
    sudo mkdir -p /opt/sun/backend /opt/sun/web /opt/permify
    sudo chown ubuntu:ubuntu /opt/sun/backend /opt/sun/web /opt/permify
    sudo loginctl enable-linger ubuntu 2>/dev/null || true
REMOTE
  rsync -avz -e "ssh -i $SSH_KEY" \
    "$APP_DIR/vm/systemd/" "$VM:~/.config/systemd/user/"
}

deploy_backend() {
  echo "==> Deploying backend to VM..."
  rsync -avz --delete \
    -e "ssh -i $SSH_KEY" \
    "$APP_DIR/components/sun-graphql/build/libs/sun-graphql-0.0.1-SNAPSHOT.jar" \
    "$VM:/opt/sun/backend/sun-graphql.jar"
  $SSH "systemctl --user daemon-reload && systemctl --user restart sun-backend"
  echo "    Backend restarted."
  deploy_permify
}

deploy_frontend() {
  echo "==> Deploying frontend (zero-downtime blue-green) to VM..."

  # Rsync code
  $SSH "mkdir -p $DEPLOY_DIR/dist/client $DEPLOY_DIR/dist/server"
  rsync -avz --delete \
    -e "ssh -i $SSH_KEY" \
    --exclude node_modules \
    --exclude .env \
    --exclude .env.production \
    "$APP_DIR/dist/client/" "$VM:$DEPLOY_DIR/dist/client/"
  rsync -avz --delete \
    -e "ssh -i $SSH_KEY" \
    "$APP_DIR/dist/server/" "$VM:$DEPLOY_DIR/dist/server/"
  rsync -avz \
    -e "ssh -i $SSH_KEY" \
    "$APP_DIR/server.js" "$APP_DIR/config.js" \
    "$APP_DIR/config.d.ts" "$APP_DIR/css-loader.mjs" \
    "$APP_DIR/package.json" "$APP_DIR/tsconfig.json" \
    "$VM:$DEPLOY_DIR/"
  rsync -avz \
    -e "ssh -i $SSH_KEY" \
    "$APP_DIR/routes/" "$VM:$DEPLOY_DIR/routes/"
  rsync -avz \
    -e "ssh -i $SSH_KEY" \
    "$APP_DIR/messages/" "$VM:$DEPLOY_DIR/messages/"
  rsync -avz \
    -e "ssh -i $SSH_KEY" \
    --exclude node_modules \
    "$APP_DIR/src/" "$VM:$DEPLOY_DIR/src/"
  rsync -avz \
    -e "ssh -i $SSH_KEY" \
    "$APP_DIR/utils/" "$VM:$DEPLOY_DIR/utils/"
  rsync -avz \
    -e "ssh -i $SSH_KEY" \
    "$APP_DIR/.npmrc" "$APP_DIR/package-lock.json" "$VM:$DEPLOY_DIR/" 2>/dev/null || rsync -avz -e "ssh -i $SSH_KEY" "$APP_DIR/.npmrc" "$VM:$DEPLOY_DIR/"

  echo "    Installing production dependencies..."
  $SSH "cd $DEPLOY_DIR && npm install --omit=dev --no-audit --quiet 2>/dev/null"

  # Zero-downtime blue-green swap
  $SSH bash -s <<'SWAP'
set -euo pipefail

LIVE_PORT=5173
STANDBY_PORT=15173
DOMAIN="sun.scarlettparker.co.uk"
SERVICE="sun-frontend"

# Detect which port is currently live from the nginx config
CURRENT=$(grep -oP '127\.0\.0\.1:\K[0-9]+' "/etc/nginx/sites-enabled/$DOMAIN" 2>/dev/null || echo "$LIVE_PORT")
if [ "$CURRENT" = "$LIVE_PORT" ]; then
  NEW_PORT=$STANDBY_PORT
  SWAP_TO="standby"
else
  NEW_PORT=$LIVE_PORT
  SWAP_TO="live"
fi

echo "    Live: $CURRENT -> Deploying to: $NEW_PORT"

# Step 1: Start new instance on standby (old is still serving)
echo "    Starting new instance on port $NEW_PORT..."
systemctl --user start "$SERVICE@$NEW_PORT"

# Step 2: Health-check new instance (up to 30s)
echo "    Health-checking..."
HEALTHY=false
for i in $(seq 1 30); do
  if curl -sf "http://127.0.0.1:$NEW_PORT/" > /dev/null 2>&1; then
    echo "    Healthy after ${i}s"
    HEALTHY=true
    break
  fi
  sleep 1
done

if [ "$HEALTHY" != "true" ]; then
  echo "    HEALTH CHECK FAILED, rolling back"
  systemctl --user stop "$SERVICE@$NEW_PORT" 2>/dev/null || true
  exit 1
fi

# Step 3: Swap nginx to new instance (old is still running — no gap)
echo "    Swapping nginx to port $NEW_PORT..."
sudo ln -sf "/etc/nginx/sites-available/$DOMAIN.$SWAP_TO" "/etc/nginx/sites-enabled/$DOMAIN"
sudo nginx -t 2>&1 && sudo nginx -s reload 2>&1

# Step 4: Stop old instance (no longer receiving traffic)
echo "    Stopping old instance on port $CURRENT..."
systemctl --user stop "$SERVICE@$CURRENT" 2>/dev/null || true

echo "    Deploy complete: $DOMAIN now on port $NEW_PORT"
SWAP

  echo "    Frontend deployed (zero-downtime)."
}

# --skip-build flag
if [ "$SKIP_BUILD" != "--skip-build" ]; then
  case "$TARGET" in
    backend)  build_backend ;;
    frontend) build_frontend ;;
    all)      build_backend; build_frontend ;;
    *)        usage ;;
  esac
fi

setup_vm

case "$TARGET" in
  backend)  deploy_backend ;;
  frontend) deploy_frontend ;;
  all)      deploy_backend; deploy_frontend ;;
  *)        usage ;;
esac

echo "==> Done."
