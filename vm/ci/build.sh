#!/usr/bin/env bash
set -euo pipefail

HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
VPS="${VPS:-ubuntu@vps.scarlettparker.co.uk}"
SSH_KEY="${SSH_KEY:-$HOME/.ssh/ubuntu.key}"

# Prints a status line.
#
# @param $@ Message parts.
log() { printf '\033[1;36m==>\033[0m %s\n' "$*"; }

log "build: sun-ci:1"
ssh -i "$SSH_KEY" -o BatchMode=yes "$VPS" "mkdir -p /tmp/sun-ci"
rsync -az -e "ssh -i $SSH_KEY" "$HERE/Dockerfile" "$VPS:/tmp/sun-ci/"
ssh -i "$SSH_KEY" -o BatchMode=yes "$VPS" "sudo docker build -q -t sun-ci:1 /tmp/sun-ci"
log "built: sun-ci:1"
