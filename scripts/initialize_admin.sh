#!/bin/bash
set -euo pipefail
cd "$(dirname "$0")/.."
read -r -s -p '设置首个 admin 密码（至少12字符，最多72字节）：' BOOTSTRAP_ADMIN_PASSWORD
printf '\n'
read -r -s -p '再次输入：' confirmation
printf '\n'
if [[ "$BOOTSTRAP_ADMIN_PASSWORD" != "$confirmation" ]]; then
  echo '两次密码不一致' >&2
  exit 1
fi
if [[ ${#BOOTSTRAP_ADMIN_PASSWORD} -lt 12 ]]; then
  echo '密码至少12字符' >&2
  exit 1
fi
export BOOTSTRAP_ADMIN_PASSWORD
exec scripts/run_backend.sh -Dspring-boot.run.arguments='--directory.bootstrap=true'
