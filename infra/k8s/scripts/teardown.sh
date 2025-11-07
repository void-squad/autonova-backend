#!/usr/bin/env bash
set -euo pipefail

# Delete the autonova namespace and all included resources
kubectl delete namespace autonova || true

echo "Requested deletion of namespace 'autonova'."