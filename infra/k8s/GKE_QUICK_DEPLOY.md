# 🚀 10-Minute GKE Deployment Guide

This guide gets AutoNova running on Google Kubernetes Engine in under 10 minutes.

## Prerequisites

- Google Cloud account with billing enabled
- `gcloud` CLI installed
- `kubectl` installed
- All Docker images pushed to Docker Hub or GCR

## Step 1: Setup GCP Project (1 min)

```bash
# Set your project
export PROJECT_ID="your-project-id"
gcloud config set project $PROJECT_ID

# Enable required APIs
gcloud services enable container.googleapis.com compute.googleapis.com
```

## Step 2: Create GKE Cluster (3 mins)

### Option A: Autopilot (Recommended - Fully Managed)
```bash
gcloud container clusters create-auto autonova-cluster \
  --region=us-central1 \
  --async

# Get credentials once ready
gcloud container clusters get-credentials autonova-cluster \
  --region=us-central1
```

### Option B: Standard Cluster (More Control)
```bash
gcloud container clusters create autonova-cluster \
  --zone=us-central1-a \
  --num-nodes=3 \
  --machine-type=e2-standard-4 \
  --disk-size=50 \
  --enable-autoscaling \
  --min-nodes=2 \
  --max-nodes=10 \
  --enable-autorepair \
  --enable-autoupgrade

# Get credentials
gcloud container clusters get-credentials autonova-cluster \
  --zone=us-central1-a
```

**Wait for cluster creation (~3 minutes)**

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

### Method 2: Ingress (Production Ready)

```bash
# Reserve static IP
gcloud compute addresses create autonova-ip --global

# Get the IP
gcloud compute addresses describe autonova-ip --global

# Deploy ingress
kubectl apply -f base/ingress/ingress.yaml

# Wait for ingress (takes 5-10 minutes for SSL cert provisioning)
kubectl get ingress autonova-ingress -n autonova -w

# Get ingress IP
kubectl get ingress autonova-ingress -n autonova
```

**Update your domain DNS:**
```
A record: yourdomain.com -> INGRESS_IP
```

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

## 🔥 Super Fast Deploy (All-in-One Script)

Create this script: `quick-deploy.sh`

```bash
#!/bin/bash
set -e

PROJECT_ID="your-project-id"
CLUSTER_NAME="autonova-cluster"
REGION="us-central1"

echo "🚀 Starting GKE deployment..."

# 1. Setup GCP
gcloud config set project $PROJECT_ID
gcloud services enable container.googleapis.com compute.googleapis.com

# 2. Create cluster (async to save time)
echo "📦 Creating GKE cluster..."
gcloud container clusters create-auto $CLUSTER_NAME \
  --region=$REGION \
  --async

# Wait for cluster
echo "⏳ Waiting for cluster..."
gcloud container clusters list --format="table(name,status)" --filter="name=$CLUSTER_NAME" --region=$REGION
sleep 180  # Wait 3 minutes

# Get credentials
gcloud container clusters get-credentials $CLUSTER_NAME --region=$REGION

# 3. Deploy app
echo "🔐 Creating secrets..."
cd infra/k8s/secrets
./create-secrets-from-env.sh autonova-env autonova

echo "🚢 Deploying services..."
cd ..
kubectl apply -k base/

# 4. Expose services
echo "🌐 Exposing services..."
kubectl expose deployment gateway-service \
  --type=LoadBalancer \
  --name=gateway-lb \
  --port=80 \
  --target-port=8080 \
  -n autonova

kubectl expose deployment web \
  --type=LoadBalancer \
  --name=frontend-lb \
  --port=80 \
  --target-port=5173 \
  -n autonova

# Wait for external IPs
echo "⏳ Waiting for external IPs..."
sleep 60

# Get IPs
GATEWAY_IP=$(kubectl get svc gateway-lb -n autonova -o jsonpath='{.status.loadBalancer.ingress[0].ip}')
FRONTEND_IP=$(kubectl get svc frontend-lb -n autonova -o jsonpath='{.status.loadBalancer.ingress[0].ip}')

echo "✅ Deployment complete!"
echo "🌐 Gateway: http://$GATEWAY_IP"
echo "🎨 Frontend: http://$FRONTEND_IP"
echo "📊 Eureka: http://$GATEWAY_IP/eureka"
```

Run it:
```bash
chmod +x quick-deploy.sh
./quick-deploy.sh
```

## 💰 Cost Optimization

**For Development:**
```bash
# Use smaller nodes
--machine-type=e2-medium
--num-nodes=2

# Use preemptible nodes (cheaper)
--preemptible

# Scale down when not in use
kubectl scale deployment --all --replicas=0 -n autonova

# Scale up when needed
kubectl scale deployment --all --replicas=1 -n autonova
```

**For Production:**
- Use Autopilot (pay-per-pod)
- Enable cluster autoscaling
- Use committed use discounts

## 🧹 Cleanup

```bash
# Delete cluster (stops billing)
gcloud container clusters delete autonova-cluster --region=us-central1

# Delete static IP
gcloud compute addresses delete autonova-ip --global

# Delete load balancers (if using LoadBalancer service type)
kubectl delete svc gateway-lb frontend-lb -n autonova
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

# Check firewall rules
gcloud compute firewall-rules list

# Create firewall rule if needed
gcloud compute firewall-rules create allow-gateway \
  --allow tcp:8080 \
  --source-ranges 0.0.0.0/0
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

## 📊 Monitoring

```bash
# View GKE dashboard
echo "https://console.cloud.google.com/kubernetes/workload?project=$PROJECT_ID"

# View logs in Cloud Logging
echo "https://console.cloud.google.com/logs?project=$PROJECT_ID"

# Check resource usage
kubectl top nodes
kubectl top pods -n autonova
```

## 🔄 CI/CD Integration

Add to your GitHub Actions or GitLab CI:

```yaml
- name: Deploy to GKE
  run: |
    gcloud container clusters get-credentials autonova-cluster --region=us-central1
    kubectl apply -k infra/k8s/base/
    kubectl rollout restart deployment -n autonova
```

## 📚 Next Steps

1. **Setup Domain**: Point your domain to the LoadBalancer IP
2. **Enable HTTPS**: Use Google-managed SSL certificates with Ingress
3. **Setup Monitoring**: Enable GKE monitoring and logging
4. **Configure Backups**: Setup automated database backups
5. **Implement CI/CD**: Automate deployments with Cloud Build
6. **Add Autoscaling**: Configure HPA for pods
7. **Network Policies**: Secure inter-service communication

## 🎯 Production Checklist

- [ ] Use Autopilot or enable autoscaling
- [ ] Configure resource requests/limits
- [ ] Enable workload identity
- [ ] Use Google-managed certificates
- [ ] Setup Cloud Armor for DDoS protection
- [ ] Enable Binary Authorization
- [ ] Configure Cloud CDN for frontend
- [ ] Setup Cloud SQL proxy (if not using Neon)
- [ ] Enable VPC-native cluster
- [ ] Configure Network Policies
- [ ] Setup alerting with Cloud Monitoring
- [ ] Configure log retention policies
