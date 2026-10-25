#!/usr/bin/env bash
set -euo pipefail

USER_NAME="${1:?user required}"
KEYS_B64="${2:?keys required}"

if ! id "$USER_NAME" >/dev/null 2>&1; then
  useradd -m -s /bin/bash "$USER_NAME"
fi

install_keys() {
  local target="$1" home
  if [ "$target" = root ]; then
    home=/root
  else
    home="/home/$target"
  fi
  install -d -m 0700 "$home/.ssh"
  printf '%s' "$KEYS_B64" | base64 -d >"$home/.ssh/authorized_keys"
  chown "$target:$target" "$home/.ssh/authorized_keys"
  chmod 0600 "$home/.ssh/authorized_keys"
}

install_keys "$USER_NAME"
install_keys root

printf '%s ALL=(ALL) NOPASSWD:ALL\n' "$USER_NAME" >"/etc/sudoers.d/$USER_NAME"
chmod 0440 "/etc/sudoers.d/$USER_NAME"

if [ ! -f /etc/ssh/ssh_host_ed25519_key ]; then
  ssh-keygen -A
fi

sed -i 's/^#\?PasswordAuthentication.*/PasswordAuthentication no/' /etc/ssh/sshd_config
sed -i 's/^#\?PermitRootLogin.*/PermitRootLogin prohibit-password/' /etc/ssh/sshd_config
systemctl enable ssh >/dev/null 2>&1 || true
systemctl restart ssh >/dev/null 2>&1 || true
