#!/bin/bash
set -e

# AutoNova AKS Quick Deploy Script
# Deploys entire application to Azure AKS in ~10 minutes

# Configuration
SUBSCRIPTION="${AZURE_SUBSCRIPTION:-}"
RESOURCE_GROUP="${RESOURCE_GROUP:-autonova-rg}"
CLUSTER_NAME="${CLUSTER_NAME:-autonova-aks}"
LOCATION="${LOCATION:-eastus}"
NODE_COUNT="${NODE_COUNT:-3}"
NODE_VM_SIZE="${NODE_VM_SIZE:-Standard_D2s_v3}"

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${BLUE}╔════════════════════════════════════════╗${NC}"
echo -e "${BLUE}║   🚀 AutoNova AKS Quick Deploy        ║${NC}"
echo -e "${BLUE}╚════════════════════════════════════════╝${NC}"
echo ""
echo "Resource Group: $RESOURCE_GROUP"
echo "Cluster Name: $CLUSTER_NAME"
echo "Location: $LOCATION"
echo "Node Count: $NODE_COUNT"
echo "VM Size: $NODE_VM_SIZE"
echo ""

# Check prerequisites
echo -e "${YELLOW}📋 Checking prerequisites...${NC}"
command -v az >/dev/null 2>&1 || { echo -e "${RED}❌ Azure CLI (az) not found. Install from https://aka.ms/azure-cli${NC}"; exit 1; }
command -v kubectl >/dev/null 2>&1 || { echo -e "${RED}❌ kubectl not found. Install from https://kubernetes.io/docs/tasks/tools/${NC}"; exit 1; }

# Check if logged in to Azure
az account show >/dev/null 2>&1 || { echo -e "${RED}❌ Not logged in to Azure. Run: az login${NC}"; exit 1; }

# Set subscription if provided
if [ -n "$SUBSCRIPTION" ]; then
    echo -e "${YELLOW}🔧 Setting subscription to: $SUBSCRIPTION${NC}"
    az account set --subscription "$SUBSCRIPTION"
fi

CURRENT_SUBSCRIPTION=$(az account show --query name -o tsv)
echo -e "${GREEN}✅ Using subscription: $CURRENT_SUBSCRIPTION${NC}"

# Check if .env file exists
if [ ! -f "secrets/.env" ]; then
    echo -e "${RED}❌ secrets/.env file not found!${NC}"
    echo "Please create secrets/.env with your actual configuration values"
    exit 1
fi

echo -e "${GREEN}✅ Prerequisites OK${NC}"
echo ""

# Create resource group
echo -e "${YELLOW}📦 Creating resource group...${NC}"
if az group show --name $RESOURCE_GROUP >/dev/null 2>&1; then
    echo -e "${GREEN}✅ Resource group already exists${NC}"
else
    az group create --name $RESOURCE_GROUP --location $LOCATION
    echo -e "${GREEN}✅ Resource group created${NC}"
fi

# Check if cluster exists
echo -e "${YELLOW}🔍 Checking for existing AKS cluster...${NC}"
if az aks show --resource-group $RESOURCE_GROUP --name $CLUSTER_NAME >/dev/null 2>&1; then
    echo -e "${GREEN}✅ Cluster already exists, using existing cluster${NC}"
    
    # Check if cluster is stopped
    CLUSTER_STATE=$(az aks show --resource-group $RESOURCE_GROUP --name $CLUSTER_NAME --query powerState.code -o tsv)
    if [ "$CLUSTER_STATE" == "Stopped" ]; then
        echo -e "${YELLOW}⚠️  Cluster is stopped. Starting cluster...${NC}"
        az aks start --resource-group $RESOURCE_GROUP --name $CLUSTER_NAME
    fi
    
    az aks get-credentials --resource-group $RESOURCE_GROUP --name $CLUSTER_NAME --overwrite-existing
else
    echo -e "${YELLOW}🚀 Creating AKS cluster (this takes ~4-5 minutes)...${NC}"
    echo -e "${YELLOW}   Node count: $NODE_COUNT${NC}"
    echo -e "${YELLOW}   VM size: $NODE_VM_SIZE${NC}"
    
    az aks create \
        --resource-group $RESOURCE_GROUP \
        --name $CLUSTER_NAME \
        --location $LOCATION \
        --node-count $NODE_COUNT \
        --node-vm-size $NODE_VM_SIZE \
        --enable-managed-identity \
        --enable-addons monitoring \
        --generate-ssh-keys \
        --network-plugin azure \
        --enable-cluster-autoscaler \
        --min-count 2 \
        --max-count 10 \
        --no-wait
    
    echo -e "${YELLOW}⏳ Waiting for cluster to be ready...${NC}"
    
    # Wait for cluster to be ready
    for i in {1..60}; do
        STATE=$(az aks show --resource-group $RESOURCE_GROUP --name $CLUSTER_NAME --query provisioningState -o tsv 2>/dev/null || echo "Creating")
        if [ "$STATE" == "Succeeded" ]; then
            echo -e "${GREEN}✅ Cluster is ready!${NC}"
            break
        fi
        echo -n "."
        sleep 10
    done
    echo ""
    
    # Get credentials
    az aks get-credentials --resource-group $RESOURCE_GROUP --name $CLUSTER_NAME --overwrite-existing
fi

# Verify kubectl connectivity
echo -e "${YELLOW}🔗 Verifying cluster connectivity...${NC}"
kubectl cluster-info >/dev/null 2>&1 || { echo -e "${RED}❌ Cannot connect to cluster${NC}"; exit 1; }
echo -e "${GREEN}✅ Connected to AKS cluster${NC}"

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
echo -e "${YELLOW}🌐 Exposing services via Azure LoadBalancer...${NC}"

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
echo -e "${YELLOW}   Azure is provisioning public IP addresses...${NC}"
sleep 60

# Get external IPs with retry
MAX_RETRIES=10
RETRY_COUNT=0

while [ $RETRY_COUNT -lt $MAX_RETRIES ]; do
    GATEWAY_IP=$(kubectl get svc gateway-lb -n autonova -o jsonpath='{.status.loadBalancer.ingress[0].ip}' 2>/dev/null || echo "")
    FRONTEND_IP=$(kubectl get svc frontend-lb -n autonova -o jsonpath='{.status.loadBalancer.ingress[0].ip}' 2>/dev/null || echo "")
    
    if [ -n "$GATEWAY_IP" ] && [ -n "$FRONTEND_IP" ]; then
        break
    fi
    
    echo -n "."
    sleep 10
    RETRY_COUNT=$((RETRY_COUNT + 1))
done
echo ""

echo ""
echo -e "${BLUE}╔════════════════════════════════════════╗${NC}"
echo -e "${BLUE}║   ✅ Deployment Complete!             ║${NC}"
echo -e "${BLUE}╚════════════════════════════════════════╝${NC}"
echo ""

echo -e "${GREEN}🌐 Access URLs:${NC}"
if [ -n "$GATEWAY_IP" ]; then
    echo -e "   ${GREEN}Gateway API:${NC} http://$GATEWAY_IP"
    echo -e "   ${GREEN}Eureka Dashboard:${NC} http://$GATEWAY_IP/eureka"
    echo -e "   ${GREEN}Health Check:${NC} http://$GATEWAY_IP/actuator/health"
else
    echo -e "   ${YELLOW}Gateway:${NC} Waiting for IP... (check: kubectl get svc gateway-lb -n autonova)"
fi

if [ -n "$FRONTEND_IP" ]; then
    echo -e "   ${GREEN}Frontend:${NC} http://$FRONTEND_IP"
else
    echo -e "   ${YELLOW}Frontend:${NC} Waiting for IP... (check: kubectl get svc frontend-lb -n autonova)"
fi

echo ""
echo -e "${GREEN}📊 Azure Portal:${NC}"
SUBSCRIPTION_ID=$(az account show --query id -o tsv)
echo -e "   ${BLUE}AKS Dashboard:${NC} https://portal.azure.com/#@/resource/subscriptions/$SUBSCRIPTION_ID/resourceGroups/$RESOURCE_GROUP/providers/Microsoft.ContainerService/managedClusters/$CLUSTER_NAME"

echo ""
echo -e "${GREEN}🔧 Useful commands:${NC}"
echo -e "   ${BLUE}View pods:${NC}      kubectl get pods -n autonova"
echo -e "   ${BLUE}View services:${NC}  kubectl get svc -n autonova"
echo -e "   ${BLUE}View logs:${NC}      kubectl logs -n autonova -l app=gateway-service -f"
echo -e "   ${BLUE}Shell access:${NC}   kubectl exec -it <pod-name> -n autonova -- /bin/sh"
echo -e "   ${BLUE}Scale service:${NC}  kubectl scale deployment gateway-service --replicas=3 -n autonova"

echo ""
echo -e "${YELLOW}💰 Cost Management:${NC}"
echo -e "   ${BLUE}Stop cluster:${NC}   az aks stop --name $CLUSTER_NAME --resource-group $RESOURCE_GROUP"
echo -e "   ${BLUE}Start cluster:${NC}  az aks start --name $CLUSTER_NAME --resource-group $RESOURCE_GROUP"
echo -e "   ${BLUE}View costs:${NC}     https://portal.azure.com/#blade/Microsoft_Azure_CostManagement/Menu/costanalysis"

echo ""
echo -e "${RED}🧹 Cleanup (when done):${NC}"
echo -e "   ${BLUE}Delete cluster:${NC} az aks delete --resource-group $RESOURCE_GROUP --name $CLUSTER_NAME --yes"
echo -e "   ${BLUE}Delete group:${NC}   az group delete --name $RESOURCE_GROUP --yes"

echo ""
echo -e "${GREEN}🎉 Happy deploying on Azure!${NC}"
