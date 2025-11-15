# 🚀 10-Minute Azure AKS Deployment Guide

This guide gets AutoNova running on Azure Kubernetes Service in under 10 minutes.

## Prerequisites

- Azure account with active subscription
- `az` CLI installed
- `kubectl` installed
- All Docker images pushed to Docker Hub or Azure Container Registry

## Step 1: Setup Azure (1 min)

```bash
# Login to Azure
az login

# Set subscription (if you have multiple)
az account set --subscription "Your Subscription Name"

# Set variables
export RESOURCE_GROUP="autonova-rg"
export CLUSTER_NAME="autonova-aks"
export LOCATION="eastus"
```

## Step 2: Create AKS Cluster (3 mins)

### Option A: Quick Deployment (Recommended)
```bash
# Create resource group
az group create --name $RESOURCE_GROUP --location $LOCATION

# Create AKS cluster (managed, auto-scaling)
az aks create \
  --resource-group $RESOURCE_GROUP \
  --name $CLUSTER_NAME \
  --node-count 3 \
  --node-vm-size Standard_D2s_v3 \
  --enable-managed-identity \
  --enable-addons monitoring \
  --generate-ssh-keys \
  --network-plugin azure \
  --enable-cluster-autoscaler \
  --min-count 2 \
  --max-count 10

# Get credentials
az aks get-credentials --resource-group $RESOURCE_GROUP --name $CLUSTER_NAME
```

### Option B: Cost-Optimized (Dev/Test)
```bash
# Create resource group
az group create --name $RESOURCE_GROUP --location $LOCATION

# Create smaller cluster for development
az aks create \
  --resource-group $RESOURCE_GROUP \
  --name $CLUSTER_NAME \
  --node-count 2 \
  --node-vm-size Standard_B2s \
  --enable-managed-identity \
  --generate-ssh-keys \
  --tier free

# Get credentials
az aks get-credentials --resource-group $RESOURCE_GROUP --name $CLUSTER_NAME
```

**Wait for cluster creation (~3-4 minutes)**

## Step 3: Deploy Application (5 mins)

### 3.1 Configure Secrets

```bash
cd infra/k8s/secrets

# Ensure .env file has actual values
vim .env

# Create Kubernetes secret
./create-secrets-from-env.sh autonova-env autonova
```

### 3.2 Deploy All Services

```bash
cd ..
kubectl apply -k base/

# Watch pods starting
kubectl get pods -n autonova -w
```

### 3.3 Verify Deployment

```bash
# Check all pods are running
kubectl get pods -n autonova

# Check services
kubectl get svc -n autonova

# Check deployment status
kubectl rollout status deployment -n autonova
```

## Step 4: Expose Services (2 mins)

### Method 1: LoadBalancer (Quick & Easy)

```bash
# Expose gateway
kubectl expose deployment gateway-service \
  --type=LoadBalancer \
  --name=gateway-lb \
  --port=80 \
  --target-port=8080 \
  -n autonova

# Expose frontend
kubectl expose deployment web \
  --type=LoadBalancer \
  --name=frontend-lb \
  --port=80 \
  --target-port=5173 \
  -n autonova

# Get external IPs (takes 1-2 minutes)
kubectl get svc gateway-lb frontend-lb -n autonova -w

# Access your services
export GATEWAY_IP=$(kubectl get svc gateway-lb -n autonova -o jsonpath='{.status.loadBalancer.ingress[0].ip}')
export FRONTEND_IP=$(kubectl get svc frontend-lb -n autonova -o jsonpath='{.status.loadBalancer.ingress[0].ip}')

echo "Gateway: http://$GATEWAY_IP"
echo "Frontend: http://$FRONTEND_IP"
```

### Method 2: Ingress with Application Gateway (Production)

```bash
# Enable application gateway ingress controller
az aks enable-addons \
  --resource-group $RESOURCE_GROUP \
  --name $CLUSTER_NAME \
  --addons ingress-appgw \
  --appgw-name autonova-appgw \
  --appgw-subnet-cidr "10.225.0.0/16"

# Deploy ingress
kubectl apply -f base/ingress/ingress-azure.yaml

# Get ingress IP
kubectl get ingress autonova-ingress -n autonova
```

## 🔥 Super Fast Deploy (All-in-One Script)

Use the automated script:

```bash
cd infra/k8s

# Set your variables
export AZURE_SUBSCRIPTION="Your Subscription Name"
export RESOURCE_GROUP="autonova-rg"
export CLUSTER_NAME="autonova-aks"
export LOCATION="eastus"

# Run deployment
./quick-deploy-aks.sh
```

The script will:
1. ✅ Create resource group
2. ✅ Create AKS cluster
3. ✅ Deploy all services
4. ✅ Expose via LoadBalancer
5. ✅ Show access URLs

## Step 5: Verify Everything Works

```bash
# Check all pods are running
kubectl get pods -n autonova

# Check logs
kubectl logs -n autonova -l app=discovery-service --tail=50
kubectl logs -n autonova -l app=auth-service --tail=50
kubectl logs -n autonova -l app=gateway-service --tail=50

# Test API
curl http://$GATEWAY_IP/actuator/health
curl http://$FRONTEND_IP
```

## 💰 Cost Optimization

### For Development:
```bash
# Use smaller VMs
--node-vm-size Standard_B2s  # ~$30/month per node

# Use spot instances (70-90% cheaper)
az aks nodepool add \
  --resource-group $RESOURCE_GROUP \
  --cluster-name $CLUSTER_NAME \
  --name spotpool \
  --priority Spot \
  --eviction-policy Delete \
  --spot-max-price -1 \
  --node-vm-size Standard_D2s_v3 \
  --node-count 2

# Stop cluster when not in use (saves ~90% of costs)
az aks stop --name $CLUSTER_NAME --resource-group $RESOURCE_GROUP

# Start when needed
az aks start --name $CLUSTER_NAME --resource-group $RESOURCE_GROUP
```

### For Production:
- Use Azure Reserved Instances (40-60% savings)
- Enable cluster autoscaling
- Use Azure Container Registry (ACR) for images
- Enable Azure Monitor for containers

## 🔐 Azure Container Registry (ACR) Integration

### Push images to ACR:
```bash
# Create ACR
az acr create \
  --resource-group $RESOURCE_GROUP \
  --name autonovaacr \
  --sku Basic

# Attach ACR to AKS
az aks update \
  --resource-group $RESOURCE_GROUP \
  --name $CLUSTER_NAME \
  --attach-acr autonovaacr

# Login to ACR
az acr login --name autonovaacr

# Tag and push images
docker tag jalinahirushan02/auth-service:dev-latest autonovaacr.azurecr.io/auth-service:dev-latest
docker push autonovaacr.azurecr.io/auth-service:dev-latest

# Update deployment images to use ACR
kubectl set image deployment/auth-service auth-service=autonovaacr.azurecr.io/auth-service:dev-latest -n autonova
```

## 🧹 Cleanup

```bash
# Delete entire resource group (stops all billing)
az group delete --name $RESOURCE_GROUP --yes --no-wait

# Or delete just the cluster
az aks delete --resource-group $RESOURCE_GROUP --name $CLUSTER_NAME --yes --no-wait
```

## 🐛 Troubleshooting

### Pods not starting?
```bash
# Check events
kubectl get events -n autonova --sort-by='.lastTimestamp'

# Check pod logs
kubectl logs -n autonova <pod-name>

# Describe pod
kubectl describe pod -n autonova <pod-name>
```

### Can't access services?
```bash
# Check service endpoints
kubectl get endpoints -n autonova

# Check load balancer status
az network lb list --resource-group MC_${RESOURCE_GROUP}_${CLUSTER_NAME}_${LOCATION}

# Check NSG rules
az network nsg list --resource-group MC_${RESOURCE_GROUP}_${CLUSTER_NAME}_${LOCATION}
```

### Database connection issues?
```bash
# Test from pod
kubectl run -it --rm debug \
  --image=postgres:17 \
  --restart=Never \
  -n autonova \
  -- psql -h ep-mute-thunder-adhoybp1-pooler.c-2.us-east-1.aws.neon.tech \
       -U neondb_owner -d neondb

# Check secrets
kubectl get secret autonova-env -n autonova -o yaml
```

### Node issues?
```bash
# Check node status
kubectl get nodes
kubectl describe node <node-name>

# Scale node pool
az aks scale \
  --resource-group $RESOURCE_GROUP \
  --name $CLUSTER_NAME \
  --node-count 4

# Upgrade node pool
az aks nodepool upgrade \
  --resource-group $RESOURCE_GROUP \
  --cluster-name $CLUSTER_NAME \
  --name nodepool1 \
  --kubernetes-version 1.28.3
```

## 📊 Monitoring & Management

### Azure Portal Dashboard
```bash
# Open AKS in Azure Portal
echo "https://portal.azure.com/#@/resource/subscriptions/$(az account show --query id -o tsv)/resourceGroups/$RESOURCE_GROUP/providers/Microsoft.ContainerService/managedClusters/$CLUSTER_NAME"
```

### Azure Monitor Integration
```bash
# View logs in Azure Monitor
az monitor log-analytics workspace create \
  --resource-group $RESOURCE_GROUP \
  --workspace-name autonova-logs

# View metrics
kubectl top nodes
kubectl top pods -n autonova

# Azure Container Insights (automatically enabled with --enable-addons monitoring)
# Access via Azure Portal > AKS > Monitoring > Insights
```

### Connect to AKS Dashboard
```bash
# Install dashboard (if not using Azure Portal)
kubectl apply -f https://raw.githubusercontent.com/kubernetes/dashboard/v2.7.0/aio/deploy/recommended.yaml

# Create admin user
kubectl create clusterrolebinding kubernetes-dashboard --clusterrole=cluster-admin --serviceaccount=kube-system:kubernetes-dashboard

# Access dashboard
az aks browse --resource-group $RESOURCE_GROUP --name $CLUSTER_NAME
```

## 🔄 CI/CD Integration

### Azure DevOps Pipeline:
```yaml
trigger:
  - main

pool:
  vmImage: 'ubuntu-latest'

steps:
- task: AzureCLI@2
  inputs:
    azureSubscription: 'Your-Service-Connection'
    scriptType: 'bash'
    scriptLocation: 'inlineScript'
    inlineScript: |
      az aks get-credentials --resource-group $(RESOURCE_GROUP) --name $(CLUSTER_NAME)
      kubectl apply -k infra/k8s/base/
      kubectl rollout restart deployment -n autonova
```

### GitHub Actions:
```yaml
name: Deploy to AKS

on:
  push:
    branches: [ main ]

jobs:
  deploy:
    runs-on: ubuntu-latest
    steps:
      - uses: azure/login@v1
        with:
          creds: ${{ secrets.AZURE_CREDENTIALS }}
      
      - uses: azure/aks-set-context@v1
        with:
          resource-group: 'autonova-rg'
          cluster-name: 'autonova-aks'
      
      - run: |
          kubectl apply -k infra/k8s/base/
          kubectl rollout restart deployment -n autonova
```

## 🎯 Production Checklist

- [ ] Use Azure Reserved Instances or Spot VMs
- [ ] Enable cluster autoscaling
- [ ] Configure resource requests/limits
- [ ] Use Azure Container Registry (ACR)
- [ ] Enable Azure Monitor and Container Insights
- [ ] Configure Azure Key Vault for secrets
- [ ] Set up Azure Application Gateway with WAF
- [ ] Enable Azure Defender for Kubernetes
- [ ] Configure Azure Backup for persistent volumes
- [ ] Set up Virtual Network integration
- [ ] Enable Azure Policy for governance
- [ ] Configure Azure AD integration for RBAC
- [ ] Set up Azure Front Door for global load balancing

## 📚 Next Steps

1. **Custom Domain**: Configure Azure DNS and point to LoadBalancer
2. **SSL/TLS**: Use Azure Application Gateway with SSL termination
3. **Backup**: Enable Azure Backup for AKS
4. **Monitoring**: Configure Azure Monitor alerts
5. **Security**: Enable Azure AD Pod Identity
6. **Scaling**: Configure HPA (Horizontal Pod Autoscaler)
7. **Networking**: Configure Azure CNI for better performance
8. **Storage**: Use Azure Disk or Azure Files for persistent volumes

## 💡 Tips & Best Practices

1. **Use Azure Container Registry** for faster image pulls and better security
2. **Enable Azure Monitor** for comprehensive logging and metrics
3. **Use managed identities** instead of service principals
4. **Configure pod identity** for secure access to Azure resources
5. **Use Azure Key Vault** for managing secrets
6. **Enable cluster autoscaler** to optimize costs
7. **Use availability zones** for high availability
8. **Regular updates**: Keep AKS and node pools updated
9. **Network policies**: Secure inter-pod communication
10. **Resource quotas**: Prevent resource exhaustion

## 📞 Support Resources

- [AKS Documentation](https://docs.microsoft.com/en-us/azure/aks/)
- [Azure CLI Reference](https://docs.microsoft.com/en-us/cli/azure/aks)
- [AKS Best Practices](https://docs.microsoft.com/en-us/azure/aks/best-practices)
- [Azure Support](https://portal.azure.com/#blade/Microsoft_Azure_Support/HelpAndSupportBlade)
