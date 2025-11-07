#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR=$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)
SECRETS_SCRIPT="$ROOT_DIR/secrets/create-secrets-from-env.sh"
ENV_FILE="$ROOT_DIR/secrets/.env"

if [ ! -f "$ENV_FILE" ]; then
  echo "Please copy secrets/.env.example to secrets/.env and fill values before running."
  exit 1
fi

# Create secrets then apply kustomize overlay for dev
$SECRETS_SCRIPT autonova-env autonova "$ENV_FILE"

kubectl apply -k $ROOT_DIR/overlays/dev

echo "Applied overlays/dev to namespace 'autonova'"
