#!/bin/bash
set -e

# GKE Cluster Creation Script for Autonova
# This script creates a production-ready GKE cluster with all necessary configurations

# Color codes for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Configuration
PROJECT_ID="${GCP_PROJECT_ID:-}"
CLUSTER_NAME="${CLUSTER_NAME:-autonova-cluster}"
REGION="${GCP_REGION:-us-central1}"
NODE_MACHINE_TYPE="${NODE_MACHINE_TYPE:-n1-standard-2}"
MIN_NODES="${MIN_NODES:-2}"
MAX_NODES="${MAX_NODES:-10}"
INITIAL_NODE_COUNT="${INITIAL_NODE_COUNT:-3}"

echo -e "${GREEN}=== Autonova GKE Cluster Setup ===${NC}"
echo

# Validate required variables
if [ -z "$PROJECT_ID" ]; then
  echo -e "${RED}Error: GCP_PROJECT_ID environment variable is required${NC}"
  echo "Usage: export GCP_PROJECT_ID=your-project-id && ./create-gke-cluster.sh"
  exit 1
fi

echo -e "${YELLOW}Configuration:${NC}"
echo "  Project ID: $PROJECT_ID"
echo "  Cluster Name: $CLUSTER_NAME"
echo "  Region: $REGION"
echo "  Machine Type: $NODE_MACHINE_TYPE"
echo "  Node Pool: $MIN_NODES - $MAX_NODES nodes"
echo

read -p "Continue with these settings? (y/n) " -n 1 -r
echo
if [[ ! $REPLY =~ ^[Yy]$ ]]; then
  echo "Aborted."
  exit 1
fi

# Set the active project
echo -e "${YELLOW}Setting active GCP project...${NC}"
gcloud config set project $PROJECT_ID

# Enable required APIs
echo -e "${YELLOW}Enabling required GCP APIs...${NC}"
gcloud services enable container.googleapis.com
gcloud services enable compute.googleapis.com
gcloud services enable secretmanager.googleapis.com
gcloud services enable cloudresourcemanager.googleapis.com

# Create GKE cluster
echo -e "${YELLOW}Creating GKE cluster (this may take 5-10 minutes)...${NC}"
gcloud container clusters create $CLUSTER_NAME \
  --region $REGION \
  --num-nodes $INITIAL_NODE_COUNT \
  --machine-type $NODE_MACHINE_TYPE \
  --enable-autoscaling \
  --min-nodes $MIN_NODES \
  --max-nodes $MAX_NODES \
  --enable-autorepair \
  --enable-autoupgrade \
  --workload-pool=$PROJECT_ID.svc.id.goog \
  --enable-ip-alias \
  --network "default" \
  --subnetwork "default" \
  --addons HorizontalPodAutoscaling,HttpLoadBalancing,GcePersistentDiskCsiDriver \
  --disk-size 100 \
  --disk-type pd-standard \
  --enable-stackdriver-kubernetes \
  --logging=SYSTEM,WORKLOAD \
  --monitoring=SYSTEM

echo -e "${GREEN}✓ GKE cluster created successfully${NC}"

# Get cluster credentials
echo -e "${YELLOW}Getting cluster credentials...${NC}"
gcloud container clusters get-credentials $CLUSTER_NAME --region $REGION

# Verify cluster connection
echo -e "${YELLOW}Verifying cluster connection...${NC}"
kubectl cluster-info

# Create namespace
echo -e "${YELLOW}Creating autonova namespace...${NC}"
kubectl create namespace autonova --dry-run=client -o yaml | kubectl apply -f -

# Create service account for External Secrets
echo -e "${YELLOW}Creating GCP service account for External Secrets...${NC}"
gcloud iam service-accounts create autonova-secrets-sa \
  --display-name="Autonova Secrets Service Account" \
  --project=$PROJECT_ID || echo "Service account may already exist"

# Grant Secret Manager access
echo -e "${YELLOW}Granting Secret Manager access...${NC}"
gcloud projects add-iam-policy-binding $PROJECT_ID \
  --member="serviceAccount:autonova-secrets-sa@$PROJECT_ID.iam.gserviceaccount.com" \
  --role="roles/secretmanager.secretAccessor"

# Bind to K8s Service Account via Workload Identity
echo -e "${YELLOW}Configuring Workload Identity...${NC}"
kubectl create serviceaccount external-secrets -n autonova --dry-run=client -o yaml | kubectl apply -f -

gcloud iam service-accounts add-iam-policy-binding \
  autonova-secrets-sa@$PROJECT_ID.iam.gserviceaccount.com \
  --role roles/iam.workloadIdentityUser \
  --member "serviceAccount:$PROJECT_ID.svc.id.goog[autonova/external-secrets]"

kubectl annotate serviceaccount external-secrets \
  -n autonova \
  iam.gke.io/gcp-service-account=autonova-secrets-sa@$PROJECT_ID.iam.gserviceaccount.com \
  --overwrite

echo -e "${GREEN}✓ Workload Identity configured${NC}"

# Output cluster information
echo
echo -e "${GREEN}=== Cluster Setup Complete ===${NC}"
echo
echo "Cluster Name: $CLUSTER_NAME"
echo "Region: $REGION"
echo "Project: $PROJECT_ID"
echo
echo -e "${YELLOW}Next steps:${NC}"
echo "1. Run ./install-infrastructure.sh to install core infrastructure (ArgoCD, Ingress, etc.)"
echo "2. Migrate secrets to GCP Secret Manager: ./migrate-secrets-to-gcp.sh"
echo "3. Deploy applications via ArgoCD"
echo
echo "To delete this cluster later, run:"
echo "  gcloud container clusters delete $CLUSTER_NAME --region $REGION"
