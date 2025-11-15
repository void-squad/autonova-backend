#!/bin/bash
set -e

# Migrate secrets from .env file to GCP Secret Manager

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

PROJECT_ID="${GCP_PROJECT_ID:-}"
ENV_FILE="${ENV_FILE:-../secrets/.env}"

echo -e "${GREEN}=== Migrating Secrets to GCP Secret Manager ===${NC}"
echo

if [ -z "$PROJECT_ID" ]; then
  echo -e "${RED}Error: GCP_PROJECT_ID environment variable is required${NC}"
  echo "Usage: export GCP_PROJECT_ID=your-project-id && ./migrate-secrets-to-gcp.sh"
  exit 1
fi

if [ ! -f "$ENV_FILE" ]; then
  echo -e "${RED}Error: $ENV_FILE not found${NC}"
  echo "Please ensure the .env file exists at $ENV_FILE"
  exit 1
fi

echo -e "${YELLOW}Reading secrets from: $ENV_FILE${NC}"
echo -e "${YELLOW}Target GCP Project: $PROJECT_ID${NC}"
echo

# Set active project
gcloud config set project $PROJECT_ID

# Read .env file and create secrets
while IFS='=' read -r key value; do
  # Skip comments and empty lines
  if [[ $key =~ ^#.* ]] || [[ -z $key ]]; then
    continue
  fi
  
  # Remove leading/trailing whitespace
  key=$(echo "$key" | xargs)
  value=$(echo "$value" | xargs)
  
  if [[ -n $key ]] && [[ -n $value ]]; then
    secret_name="autonova-$key"
    echo -e "${YELLOW}Creating secret: $secret_name${NC}"
    
    # Create or update secret
    echo -n "$value" | gcloud secrets create "$secret_name" \
      --data-file=- \
      --replication-policy="automatic" \
      2>/dev/null || \
    echo -n "$value" | gcloud secrets versions add "$secret_name" \
      --data-file=- 2>/dev/null || \
    echo -e "${RED}  Failed to create/update $secret_name${NC}"
  fi
done < "$ENV_FILE"

echo
echo -e "${GREEN}✓ Secrets migrated to GCP Secret Manager${NC}"
echo
echo -e "${YELLOW}Verify secrets:${NC}"
echo "  gcloud secrets list --project=$PROJECT_ID"
echo
echo -e "${YELLOW}Next steps:${NC}"
echo "1. Update infra/k8s/overlays/production/external-secrets/secret-store.yaml with your project ID"
echo "2. Deploy External Secret: kubectl apply -f ../overlays/production/external-secrets/"
echo "3. Verify secret sync: kubectl get externalsecret -n autonova"
