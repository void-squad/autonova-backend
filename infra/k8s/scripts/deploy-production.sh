#!/bin/bash
set -e

# Deploy Autonova to production via ArgoCD

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

GITHUB_TOKEN="${GITHUB_TOKEN:-}"
REPO_URL="https://github.com/void-squad/autonova-backend"

echo -e "${GREEN}=== Deploying Autonova to Production ===${NC}"
echo

# Verify ArgoCD is installed
if ! kubectl get namespace argocd &> /dev/null; then
  echo -e "${RED}Error: ArgoCD is not installed${NC}"
  echo "Run ./install-infrastructure.sh first"
  exit 1
fi

# Configure repository credentials
if [ -n "$GITHUB_TOKEN" ]; then
  echo -e "${YELLOW}Configuring Git repository credentials...${NC}"
  kubectl create secret generic autonova-repo \
    -n argocd \
    --from-literal=type=git \
    --from-literal=url=$REPO_URL \
    --from-literal=username=git \
    --from-literal=password=$GITHUB_TOKEN \
    --dry-run=client -o yaml | kubectl apply -f -
  
  kubectl label secret autonova-repo -n argocd argocd.argoproj.io/secret-type=repository --overwrite
  echo -e "${GREEN}✓ Repository credentials configured${NC}"
else
  echo -e "${YELLOW}Warning: GITHUB_TOKEN not set. Using public repository access.${NC}"
  echo "For private repos, set GITHUB_TOKEN environment variable"
fi

# Deploy production application
echo -e "${YELLOW}Deploying production application...${NC}"
kubectl apply -f ../argocd/application-production.yaml

echo "Waiting for application to be created..."
sleep 5

# Check application status
echo -e "${YELLOW}Application status:${NC}"
kubectl get application autonova-production -n argocd

echo
echo -e "${GREEN}=== Deployment Initiated ===${NC}"
echo
echo -e "${YELLOW}Monitor deployment:${NC}"
echo "  kubectl get application autonova-production -n argocd -w"
echo
echo -e "${YELLOW}View application in ArgoCD UI:${NC}"
echo "  kubectl port-forward svc/argocd-server -n argocd 8080:443"
echo "  https://localhost:8080"
echo
echo -e "${YELLOW}Sync application (manual approval required):${NC}"
echo "  argocd app sync autonova-production"
echo "  # Or use the UI"
echo
echo -e "${YELLOW}Check pod status:${NC}"
echo "  kubectl get pods -n autonova -w"
echo
echo -e "${YELLOW}View logs:${NC}"
echo "  kubectl logs -n autonova deployment/gateway-service -f"
