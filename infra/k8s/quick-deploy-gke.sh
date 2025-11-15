#!/bin/bash
set -e

# AutoNova GKE Quick Deploy Script
# Deploys entire application to GKE in ~10 minutes

# Configuration
PROJECT_ID="${GCP_PROJECT_ID:-your-project-id}"
CLUSTER_NAME="${CLUSTER_NAME:-autonova-cluster}"
REGION="${GCP_REGION:-us-central1}"
ZONE="${GCP_ZONE:-us-central1-a}"
MACHINE_TYPE="${MACHINE_TYPE:-e2-standard-4}"
NUM_NODES="${NUM_NODES:-3}"

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo -e "${GREEN}🚀 AutoNova GKE Quick Deploy${NC}"
echo "=================================="
echo "Project: $PROJECT_ID"
echo "Cluster: $CLUSTER_NAME"
echo "Region: $REGION"
echo ""

# Check prerequisites
echo -e "${YELLOW}📋 Checking prerequisites...${NC}"
command -v gcloud >/dev/null 2>&1 || { echo -e "${RED}❌ gcloud CLI not found. Install from https://cloud.google.com/sdk${NC}"; exit 1; }
command -v kubectl >/dev/null 2>&1 || { echo -e "${RED}❌ kubectl not found. Install from https://kubernetes.io/docs/tasks/tools/${NC}"; exit 1; }

# Check if .env file exists
if [ ! -f "secrets/.env" ]; then
    echo -e "${RED}❌ secrets/.env file not found!${NC}"
    echo "Please create secrets/.env with your actual configuration values"
    exit 1
fi

echo -e "${GREEN}✅ Prerequisites OK${NC}"
echo ""

# Setup GCP
echo -e "${YELLOW}☁️  Setting up GCP project...${NC}"
gcloud config set project $PROJECT_ID

echo -e "${YELLOW}🔧 Enabling required APIs...${NC}"
gcloud services enable container.googleapis.com compute.googleapis.com --quiet

# Check if cluster exists
echo -e "${YELLOW}🔍 Checking for existing cluster...${NC}"
if gcloud container clusters describe $CLUSTER_NAME --region=$REGION >/dev/null 2>&1; then
    echo -e "${GREEN}✅ Cluster already exists, using existing cluster${NC}"
    gcloud container clusters get-credentials $CLUSTER_NAME --region=$REGION
else
    echo -e "${YELLOW}📦 Creating GKE Autopilot cluster (this takes ~3 minutes)...${NC}"
    
    # Autopilot is faster and simpler
    gcloud container clusters create-auto $CLUSTER_NAME \
        --region=$REGION \
        --async
    
    echo -e "${YELLOW}⏳ Waiting for cluster to be ready...${NC}"
    
    # Wait for cluster to be running
    for i in {1..60}; do
        STATUS=$(gcloud container clusters describe $CLUSTER_NAME --region=$REGION --format="value(status)" 2>/dev/null || echo "PROVISIONING")
        if [ "$STATUS" == "RUNNING" ]; then
            echo -e "${GREEN}✅ Cluster is ready!${NC}"
            break
        fi
        echo -n "."
        sleep 5
    done
    echo ""
    
    # Get credentials
    gcloud container clusters get-credentials $CLUSTER_NAME --region=$REGION
fi

# Deploy application
echo ""
echo -e "${YELLOW}🔐 Creating Kubernetes secrets...${NC}"
cd secrets
./create-secrets-from-env.sh autonova-env autonova
cd ..

echo -e "${YELLOW}🚢 Deploying all services...${NC}"
kubectl apply -k base/

echo -e "${YELLOW}⏳ Waiting for pods to start (this may take 2-3 minutes)...${NC}"
sleep 30

# Show status
echo ""
echo -e "${GREEN}📊 Current deployment status:${NC}"
kubectl get pods -n autonova

# Expose services
echo ""
echo -e "${YELLOW}🌐 Exposing services via LoadBalancer...${NC}"

# Check if services already exist
if kubectl get svc gateway-lb -n autonova >/dev/null 2>&1; then
    echo "Gateway LoadBalancer already exists"
else
    kubectl expose deployment gateway-service \
        --type=LoadBalancer \
        --name=gateway-lb \
        --port=80 \
        --target-port=8080 \
        -n autonova
fi

if kubectl get svc frontend-lb -n autonova >/dev/null 2>&1; then
    echo "Frontend LoadBalancer already exists"
else
    kubectl expose deployment web \
        --type=LoadBalancer \
        --name=frontend-lb \
        --port=80 \
        --target-port=5173 \
        -n autonova
fi

echo -e "${YELLOW}⏳ Waiting for external IPs (this takes 1-2 minutes)...${NC}"
sleep 60

# Get external IPs
GATEWAY_IP=$(kubectl get svc gateway-lb -n autonova -o jsonpath='{.status.loadBalancer.ingress[0].ip}' 2>/dev/null || echo "pending")
FRONTEND_IP=$(kubectl get svc frontend-lb -n autonova -o jsonpath='{.status.loadBalancer.ingress[0].ip}' 2>/dev/null || echo "pending")

echo ""
echo -e "${GREEN}✅ Deployment complete!${NC}"
echo "=================================="
echo ""
echo -e "${GREEN}🌐 Access URLs:${NC}"
if [ "$GATEWAY_IP" != "pending" ]; then
    echo "   Gateway API: http://$GATEWAY_IP"
    echo "   Eureka Dashboard: http://$GATEWAY_IP/eureka"
else
    echo "   Gateway: Waiting for IP... (check with: kubectl get svc gateway-lb -n autonova)"
fi

if [ "$FRONTEND_IP" != "pending" ]; then
    echo "   Frontend: http://$FRONTEND_IP"
else
    echo "   Frontend: Waiting for IP... (check with: kubectl get svc frontend-lb -n autonova)"
fi

echo ""
echo -e "${GREEN}📊 Useful commands:${NC}"
echo "   View pods:      kubectl get pods -n autonova"
echo "   View services:  kubectl get svc -n autonova"
echo "   View logs:      kubectl logs -n autonova -l app=gateway-service -f"
echo "   Shell access:   kubectl exec -it <pod-name> -n autonova -- /bin/sh"
echo ""
echo -e "${GREEN}🧹 Cleanup (when done):${NC}"
echo "   Delete cluster: gcloud container clusters delete $CLUSTER_NAME --region=$REGION"
echo "   Delete services: kubectl delete svc gateway-lb frontend-lb -n autonova"
echo ""
echo -e "${GREEN}🎉 Happy deploying!${NC}"
