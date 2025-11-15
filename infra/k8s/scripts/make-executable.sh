#!/bin/bash

# Make all scripts executable

chmod +x create-gke-cluster.sh
chmod +x install-infrastructure.sh
chmod +x migrate-secrets-to-gcp.sh
chmod +x deploy-production.sh

echo "All scripts are now executable"
