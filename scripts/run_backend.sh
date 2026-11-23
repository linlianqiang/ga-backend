#!/bin/sh
set -eu
cd "$(dirname "$0")/.."
if [ -f .env ]; then
  set -a
  . ./.env
  set +a
fi
: "${JWT_SECRET:?Set JWT_SECRET (at least 32 bytes) in the environment or .env}"
exec mvn spring-boot:run "$@"
