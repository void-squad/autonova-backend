#!/usr/bin/env bash
set -euo pipefail

# Usage: ./create-secrets-from-env.sh <secret-name> <namespace>
# Example: ./create-secrets-from-env.sh autonova-env autonova

SECRET_NAME=${1:-autonova-env}
NAMESPACE=${2:-autonova}
ENV_FILE=${3:-.env}

if [ ! -f "$ENV_FILE" ]; then
  echo "Env file not found: $ENV_FILE"
  echo "Copy .env.example -> .env and fill in values, then re-run."
  exit 2
fi

# Create namespace if it doesn't exist
kubectl get namespace "$NAMESPACE" >/dev/null 2>&1 || kubectl create namespace "$NAMESPACE"

# Create or update the secret from env file (client-side dry-run + apply)
kubectl -n "$NAMESPACE" create secret generic "$SECRET_NAME" \
  --from-env-file="$ENV_FILE" \
  --dry-run=client -o yaml | kubectl apply -f -

echo "Secret '$SECRET_NAME' applied to namespace '$NAMESPACE'."
