# GKE Setup Quick Start Guide

**Last Updated:** November 16, 2025  
**Time to Complete:** ~30 minutes  
**Prerequisites:** gcloud CLI, kubectl, helm installed

---

## 🚀 Quick Setup (30 Minutes)

### Step 0: Pre-Flight Checklist

**Before you begin, gather:**
- [ ] GCP Project ID (create one at console.cloud.google.com)
- [ ] Domain name (e.g., `autonova.example.com`) or use IP for testing
- [ ] GitHub Personal Access Token (Settings → Developer settings → Personal access tokens)
- [ ] All secrets from your current `.env` files

**Install required tools:**
```bash
# Install gcloud (if not installed)
curl https://sdk.cloud.google.com | bash
exec -l $SHELL

# Install kubectl
gcloud components install kubectl

# Install helm
curl https://raw.githubusercontent.com/helm/helm/main/scripts/get-helm-3 | bash

# Install ArgoCD CLI (optional but recommended)
curl -sSL -o argocd-linux-amd64 https://github.com/argoproj/argo-cd/releases/latest/download/argocd-linux-amd64
sudo install -m 555 argocd-linux-amd64 /usr/local/bin/argocd
rm argocd-linux-amd64
```

---

## Step 1: Configure GCP Project (2 minutes)

```bash
# Login to GCP
gcloud auth login

# Set your project ID
export GCP_PROJECT_ID="your-project-id-here"
gcloud config set project $GCP_PROJECT_ID

# Enable billing (required)
# Go to: https://console.cloud.google.com/billing
# Link your project to a billing account

# Verify project is set
gcloud config get-value project
```

---

## Step 2: Create GKE Cluster (8-10 minutes)

```bash
# Navigate to scripts directory
cd /files_partition/Projects/EAD/autonova/autonova-backend/infra/k8s/scripts

# Make scripts executable
chmod +x *.sh

# Create the cluster (this takes ~8 minutes)
./create-gke-cluster.sh
```

**What this does:**
- Creates a 3-node GKE cluster with autoscaling (2-10 nodes)
- Enables Workload Identity for secure secret management
- Creates service accounts for External Secrets Operator
- Configures cluster with proper addons

**Expected output:**
```
✓ GKE cluster created successfully
✓ Workload Identity configured

Cluster Name: autonova-cluster
Region: us-central1
Project: your-project-id
```

**Verify cluster:**
```bash
kubectl cluster-info
kubectl get nodes
```

---

## Step 3: Install Core Infrastructure (5 minutes)

```bash
# Still in the scripts directory
./install-infrastructure.sh
```

**When prompted "Install Prometheus + Grafana for monitoring?"**
- Type `y` if you want full monitoring (recommended)
- Type `n` to skip (you can add later)

**What this installs:**
- ArgoCD (GitOps deployment tool)
- Nginx Ingress Controller (routing traffic)
- cert-manager (TLS certificates)
- External Secrets Operator (secret management)
- Prometheus + Grafana (optional monitoring)

**IMPORTANT - Save these values:**
```
ArgoCD admin password: [SAVE THIS]
Ingress Load Balancer IP: [SAVE THIS]
```

---

## Step 4: Configure DNS (2 minutes or skip for now)

**Option A: Use your domain (recommended for production)**
```bash
# Add A records in your DNS provider:
# Replace <INGRESS_IP> with the IP from Step 3

A    autonova.example.com         → <INGRESS_IP>
A    api.autonova.example.com     → <INGRESS_IP>
A    www.autonova.example.com     → <INGRESS_IP>
```

**Option B: Use nip.io for testing (quick setup)**
```bash
# Get your ingress IP
INGRESS_IP=$(kubectl get service ingress-nginx-controller -n ingress-nginx -o jsonpath='{.status.loadBalancer.ingress[0].ip}')

# Your domains will be:
# - autonova.$INGRESS_IP.nip.io
# - api.$INGRESS_IP.nip.io
# - www.$INGRESS_IP.nip.io

echo "Frontend: http://autonova.$INGRESS_IP.nip.io"
echo "API: http://api.$INGRESS_IP.nip.io"
```

**Update ingress files with your domain:**
```bash
# Go back to root directory
cd /files_partition/Projects/EAD/autonova/autonova-backend

# Option A: Replace with your domain
find infra/k8s/overlays/production -name "*.yaml" -type f -exec sed -i 's/autonova\.example\.com/your-actual-domain.com/g' {} +

# Option B: Use nip.io (for testing)
INGRESS_IP=$(kubectl get service ingress-nginx-controller -n ingress-nginx -o jsonpath='{.status.loadBalancer.ingress[0].ip}')
find infra/k8s/overlays/production -name "*.yaml" -type f -exec sed -i "s/autonova\.example\.com/autonova.$INGRESS_IP.nip.io/g" {} +
find infra/k8s/overlays/production -name "*.yaml" -type f -exec sed -i "s/api\.autonova\.example\.com/api.$INGRESS_IP.nip.io/g" {} +
```

---

## Step 5: Prepare Secrets (5 minutes)

**Create secrets file:**
```bash
cd infra/k8s/secrets
cp .env.example .env
nano .env  # or use your preferred editor
```

**Required secrets (get from your existing .env files):**
```bash
# Database (Neon PostgreSQL)
POSTGRES_HOST=your-neon-host.aws.neon.tech
POSTGRES_PORT=5432
POSTGRES_USER=neondb_owner
POSTGRES_PASSWORD=your-db-password
PGDATABASE=neondb

# Service-specific DB passwords
PROJECTS_DB_PASSWORD=xxx
PROGRESS_MONITORING_DB_PASSWORD=xxx
EMPLOYEE_DASHBOARD_DB_PASSWORD=xxx
TIME_LOGGING_DB_PASSWORD=xxx
USER_MANAGEMENT_DB_PASSWORD=xxx
NOTIFICATION_DB_PASSWORD=xxx
VECTOR_DB_PASSWORD=xxx
PBS_DB_PASSWORD=xxx

# Auth & Security
JWT_SECRET=your-256-bit-secret
GOOGLE_CLIENT_ID=your-client-id.apps.googleusercontent.com
GOOGLE_CLIENT_SECRET=your-client-secret
EMAIL_USERNAME=your-email@gmail.com
EMAIL_PASSWORD=your-app-password

# Payments (Stripe)
STRIPE_API_KEY=sk_live_xxx
STRIPE_WEBHOOK_SECRET=whsec_xxx
STRIPE_PUBLISHABLE_KEY=pk_live_xxx

# AI
GEMINI_API_KEY=your-gemini-key
SPRING_AI_OLLAMA_BASE_URL=http://ollama:11434

# RabbitMQ
RABBITMQ_PASSWORD=your-rabbitmq-password

# URLs
FRONTEND_URL=https://autonova.your-domain.com
```

**Migrate secrets to GCP Secret Manager:**
```bash
cd ../scripts
./migrate-secrets-to-gcp.sh
```

**Verify secrets were created:**
```bash
gcloud secrets list --filter="name:autonova-*" | head -10
```

---

## Step 6: Update GCP Project ID in Configs (1 minute)

```bash
cd /files_partition/Projects/EAD/autonova/autonova-backend

# Update secret store with your project ID
sed -i "s/YOUR_GCP_PROJECT_ID/$GCP_PROJECT_ID/g" \
  infra/k8s/overlays/production/external-secrets/secret-store.yaml

# Verify the change
grep "projectID" infra/k8s/overlays/production/external-secrets/secret-store.yaml
```

---

## Step 7: Configure ArgoCD Repository Access (2 minutes)

```bash
# Set your GitHub token
export GITHUB_TOKEN="your_github_personal_access_token"

# Update repository secret
cat > /tmp/argocd-repo.yaml <<EOF
apiVersion: v1
kind: Secret
metadata:
  name: autonova-repo
  namespace: argocd
  labels:
    argocd.argoproj.io/secret-type: repository
stringData:
  type: git
  url: https://github.com/void-squad/autonova-backend
  username: git
  password: $GITHUB_TOKEN
EOF

kubectl apply -f /tmp/argocd-repo.yaml
rm /tmp/argocd-repo.yaml
```

---

## Step 8: Deploy to Production (3 minutes)

```bash
cd /files_partition/Projects/EAD/autonova/autonova-backend/infra/k8s/scripts
./deploy-production.sh
```

**Access ArgoCD UI:**
```bash
# In a new terminal, port forward
kubectl port-forward svc/argocd-server -n argocd 8080:443

# Open browser to: https://localhost:8080
# Username: admin
# Password: [from Step 3]
```

**Sync the application:**

**Option A: Via ArgoCD UI**
1. Login to ArgoCD at https://localhost:8080
2. Click on `autonova-production` application
3. Click "SYNC" button
4. Select "SYNCHRONIZE"
5. Watch deployment progress

**Option B: Via CLI**
```bash
argocd login localhost:8080 --username admin --password <your-password> --insecure
argocd app sync autonova-production
argocd app get autonova-production --watch
```

---

## Step 9: Monitor Deployment (5 minutes)

**Watch pods starting:**
```bash
kubectl get pods -n autonova -w
```

**Expected output (after 3-5 minutes):**
```
NAME                                      READY   STATUS    RESTARTS
auth-service-xxx                          1/1     Running   0
customer-service-xxx                      1/1     Running   0
gateway-service-xxx                       1/1     Running   0
project-service-xxx                       1/1     Running   0
progress-monitoring-service-xxx           1/1     Running   0
notification-service-xxx                  1/1     Running   0
payments-billing-service-xxx              1/1     Running   0
employee-dashboard-service-xxx            1/1     Running   0
time-logging-service-xxx                  1/1     Running   0
appointment-booking-service-xxx           1/1     Running   0
chatbot-xxx                               1/1     Running   0
discovery-service-xxx                     1/1     Running   0
rabbitmq-0                                1/1     Running   0
web-deployment-xxx                        1/1     Running   0
```

**Check service logs:**
```bash
# Check gateway
kubectl logs -n autonova deployment/gateway-service --tail=50

# Check auth service
kubectl logs -n autonova deployment/auth-service --tail=50
```

**Common issues:**
```bash
# If pods are in ImagePullBackOff
kubectl describe pod -n autonova <pod-name>
# Check if Docker Hub images are accessible

# If pods are in CrashLoopBackOff
kubectl logs -n autonova <pod-name>
# Check for database connection issues or missing secrets
```

---

## Step 10: Verify Deployment (2 minutes)

**Test health endpoints:**
```bash
# Get ingress IP
INGRESS_IP=$(kubectl get service ingress-nginx-controller -n ingress-nginx -o jsonpath='{.status.loadBalancer.ingress[0].ip}')

# Test API Gateway (adjust domain if needed)
curl -k https://api.autonova.$INGRESS_IP.nip.io/actuator/health

# Test Frontend
curl -k https://autonova.$INGRESS_IP.nip.io

# Or use your actual domain
curl -k https://api.your-domain.com/actuator/health
```

**Check all services:**
```bash
kubectl get all -n autonova
kubectl get ingress -n autonova
kubectl get secrets -n autonova
```

**Access the application:**
```bash
# Get the URL
echo "Frontend: https://autonova.$INGRESS_IP.nip.io"
echo "API: https://api.$INGRESS_IP.nip.io"

# Or with your domain
echo "Frontend: https://autonova.your-domain.com"
echo "API: https://api.your-domain.com"
```

---

## 🎯 You're Live!

Your Autonova application is now running on GKE with:
- ✅ GitOps deployment via ArgoCD
- ✅ Auto-scaling enabled (HPA)
- ✅ Secrets managed via GCP Secret Manager
- ✅ TLS certificates (Let's Encrypt)
- ✅ Monitoring with Prometheus + Grafana
- ✅ Rollback capability

---

## 📊 Next Steps

### Access Monitoring (if installed)

**Grafana:**
```bash
kubectl port-forward -n monitoring svc/prometheus-grafana 3000:80

# Open: http://localhost:3000
# Username: admin
# Password: admin123
```

**Import Dashboards:**
- Spring Boot: ID 12464
- JVM Metrics: ID 4701
- RabbitMQ: ID 10991

### Configure Alerts

**Slack notifications (optional):**
```bash
# Create Slack webhook
# Update ArgoCD application annotations with webhook URL
kubectl edit application autonova-production -n argocd
```

### Setup CI/CD

**Push changes trigger automatic builds:**
```bash
# Make a change
git checkout -b feature/test
# ... make changes ...
git commit -m "test change"
git push origin feature/test

# Merge to main → images built → ArgoCD detects change → manual sync
```

---

## 🔧 Troubleshooting

### Pods not starting?

```bash
# Check pod events
kubectl describe pod -n autonova <pod-name>

# Check logs
kubectl logs -n autonova <pod-name>

# Check secrets
kubectl get externalsecret -n autonova
kubectl describe externalsecret autonova-secrets -n autonova
```

### Database connection issues?

```bash
# Test database connectivity
kubectl run -it --rm psql --image=postgres:17 --restart=Never -- \
  psql "postgresql://user:pass@host/db?sslmode=require"

# Check secret values
kubectl get secret autonova-env -n autonova -o jsonpath='{.data.POSTGRES_HOST}' | base64 -d
```

### Ingress not working?

```bash
# Check ingress
kubectl get ingress -n autonova
kubectl describe ingress autonova-ingress -n autonova

# Check cert-manager
kubectl get certificate -n autonova
kubectl describe certificate autonova-tls -n autonova

# Check ingress controller logs
kubectl logs -n ingress-nginx deployment/ingress-nginx-controller
```

### ArgoCD application out of sync?

```bash
# Force sync
argocd app sync autonova-production --prune

# Check diff
argocd app diff autonova-production
```

---

## 🔄 Making Updates

### Deploy new version:
```bash
# 1. Make changes in code
# 2. Commit and push to dev branch
git add .
git commit -m "your changes"
git push origin dev

# 3. Create PR: dev → main
# 4. Merge PR → GitHub Actions builds images
# 5. ArgoCD detects change
# 6. Manual sync in ArgoCD UI or:
argocd app sync autonova-production
```

### Rollback if needed:
```bash
# View history
argocd app history autonova-production

# Rollback
argocd app rollback autonova-production <REVISION>
```

---

## 💰 Cost Monitoring

```bash
# Check cluster cost
gcloud billing accounts list
gcloud alpha billing accounts describe ACCOUNT_ID

# Optimize costs
# - Use preemptible nodes for dev
# - Scale down during off-hours
# - Enable cluster autoscaler (already configured)
```

**Estimated monthly cost:** ~$327/month
- GKE cluster: ~$220
- Load balancer: ~$20
- Storage: ~$50
- Networking: ~$12
- Secrets/Logging: ~$25

---

## 📚 Additional Resources

- **Rollback Runbook:** `docs/ROLLBACK_RUNBOOK.md`
- **Full Deployment Plan:** `plan-gkeDeploymentWithGitops.prompt.md`
- **ArgoCD Docs:** `infra/argocd/README.md`
- **Scripts Docs:** `infra/k8s/scripts/README.md`

---

## 🆘 Need Help?

**Common commands:**
```bash
# Check everything
kubectl get all -n autonova

# Tail logs
kubectl logs -n autonova deployment/gateway-service -f

# Restart service
kubectl rollout restart deployment/gateway-service -n autonova

# Scale manually
kubectl scale deployment/gateway-service -n autonova --replicas=3

# Delete and redeploy
argocd app delete autonova-production
kubectl apply -f infra/argocd/application-production.yaml
```

**Emergency rollback:**
```bash
argocd app rollback autonova-production
```

---

**Setup Complete! 🚀**

Your Autonova platform is now running on production-grade GKE infrastructure with GitOps, auto-scaling, monitoring, and automated rollback capabilities.
