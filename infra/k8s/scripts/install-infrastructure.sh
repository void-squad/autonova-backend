#!/bin/bash
set -e

# Install Core Infrastructure Components
# This script installs ArgoCD, Nginx Ingress, cert-manager, and External Secrets Operator

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

echo -e "${GREEN}=== Installing Core Infrastructure ===${NC}"
echo

# Install ArgoCD
echo -e "${YELLOW}Installing ArgoCD...${NC}"
kubectl create namespace argocd --dry-run=client -o yaml | kubectl apply -f -
kubectl apply -n argocd -f https://raw.githubusercontent.com/argoproj/argo-cd/stable/manifests/install.yaml

echo "Waiting for ArgoCD to be ready..."
kubectl wait --for=condition=available --timeout=300s deployment/argocd-server -n argocd

echo -e "${GREEN}✓ ArgoCD installed${NC}"

# Get ArgoCD initial admin password
ARGOCD_PASSWORD=$(kubectl -n argocd get secret argocd-initial-admin-secret -o jsonpath="{.data.password}" | base64 -d)
echo -e "${YELLOW}ArgoCD admin password: ${GREEN}$ARGOCD_PASSWORD${NC}"
echo

# Install Nginx Ingress Controller
echo -e "${YELLOW}Installing Nginx Ingress Controller...${NC}"
kubectl apply -f https://raw.githubusercontent.com/kubernetes/ingress-nginx/controller-v1.10.0/deploy/static/provider/cloud/deploy.yaml

echo "Waiting for Ingress Controller to be ready..."
kubectl wait --namespace ingress-nginx \
  --for=condition=ready pod \
  --selector=app.kubernetes.io/component=controller \
  --timeout=300s

echo -e "${GREEN}✓ Nginx Ingress Controller installed${NC}"

# Get Load Balancer IP
echo "Waiting for Load Balancer IP assignment..."
sleep 30
INGRESS_IP=$(kubectl get service ingress-nginx-controller -n ingress-nginx -o jsonpath='{.status.loadBalancer.ingress[0].ip}')
echo -e "${YELLOW}Ingress Load Balancer IP: ${GREEN}$INGRESS_IP${NC}"
echo -e "${YELLOW}Configure your DNS A records to point to this IP${NC}"
echo

# Install cert-manager
echo -e "${YELLOW}Installing cert-manager...${NC}"
kubectl apply -f https://github.com/cert-manager/cert-manager/releases/download/v1.14.0/cert-manager.yaml

echo "Waiting for cert-manager to be ready..."
kubectl wait --for=condition=available --timeout=300s deployment/cert-manager -n cert-manager
kubectl wait --for=condition=available --timeout=300s deployment/cert-manager-webhook -n cert-manager
kubectl wait --for=condition=available --timeout=300s deployment/cert-manager-cainjector -n cert-manager

echo -e "${GREEN}✓ cert-manager installed${NC}"

# Install External Secrets Operator
echo -e "${YELLOW}Installing External Secrets Operator...${NC}"
helm repo add external-secrets https://charts.external-secrets.io || true
helm repo update

kubectl create namespace external-secrets-system --dry-run=client -o yaml | kubectl apply -f -

helm install external-secrets \
  external-secrets/external-secrets \
  -n external-secrets-system \
  --create-namespace \
  --wait || echo "External Secrets may already be installed"

echo -e "${GREEN}✓ External Secrets Operator installed${NC}"

# Install Prometheus Stack (optional)
read -p "Install Prometheus + Grafana for monitoring? (y/n) " -n 1 -r
echo
if [[ $REPLY =~ ^[Yy]$ ]]; then
  echo -e "${YELLOW}Installing Prometheus Stack...${NC}"
  helm repo add prometheus-community https://prometheus-community.github.io/helm-charts || true
  helm repo update
  
  kubectl create namespace monitoring --dry-run=client -o yaml | kubectl apply -f -
  
  helm install prometheus prometheus-community/kube-prometheus-stack \
    --namespace monitoring \
    --set prometheus.prometheusSpec.serviceMonitorSelectorNilUsesHelmValues=false \
    --set grafana.adminPassword='admin123' \
    --set prometheus.prometheusSpec.retention=30d \
    --wait || echo "Prometheus may already be installed"
  
  echo -e "${GREEN}✓ Prometheus Stack installed${NC}"
  echo -e "${YELLOW}Grafana admin password: ${GREEN}admin123${NC}"
fi

echo
echo -e "${GREEN}=== Infrastructure Setup Complete ===${NC}"
echo
echo -e "${YELLOW}Installed components:${NC}"
echo "  ✓ ArgoCD (namespace: argocd)"
echo "  ✓ Nginx Ingress Controller (namespace: ingress-nginx)"
echo "  ✓ cert-manager (namespace: cert-manager)"
echo "  ✓ External Secrets Operator (namespace: external-secrets-system)"
echo
echo -e "${YELLOW}Access ArgoCD UI:${NC}"
echo "  kubectl port-forward svc/argocd-server -n argocd 8080:443"
echo "  https://localhost:8080"
echo "  Username: admin"
echo "  Password: $ARGOCD_PASSWORD"
echo
echo -e "${YELLOW}Next steps:${NC}"
echo "1. Update DNS A records to point to: $INGRESS_IP"
echo "2. Migrate secrets: ./migrate-secrets-to-gcp.sh"
echo "3. Update ArgoCD repository credentials in infra/argocd/repository.yaml"
echo "4. Deploy applications: kubectl apply -f ../argocd/application-production.yaml"
