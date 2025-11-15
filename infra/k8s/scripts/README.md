# Deployment Scripts

## Local Development Scripts

- `./apply-dev.sh` — Creates the `autonova-env` secret from `secrets/.env` and applies `overlays/dev`
- `./teardown.sh` — Deletes the `autonova` namespace and all resources in it

Before running apply-dev.sh:

```bash
cd infra/k8s
cp secrets/.env.example secrets/.env
# edit secrets/.env with real values
./scripts/apply-dev.sh
```

## GKE Production Deployment Scripts

### Prerequisites
- `gcloud` CLI installed and authenticated
- `kubectl` installed  
- `helm` installed
- Active GCP project with billing enabled
- GitHub Personal Access Token (for private repos)

### Quick Start

**1. Setup Environment**
```bash
export GCP_PROJECT_ID="your-gcp-project-id"
export GITHUB_TOKEN="your-github-pat"
chmod +x *.sh
```

**2. Create GKE Cluster** (~5-10 minutes)
```bash
./create-gke-cluster.sh
```

**3. Install Infrastructure** (~3-5 minutes)
```bash
./install-infrastructure.sh
```

**4. Migrate Secrets**
```bash
./migrate-secrets-to-gcp.sh
```

**5. Deploy Production**
```bash
./deploy-production.sh
```

### Access ArgoCD
```bash
kubectl port-forward svc/argocd-server -n argocd 8080:443
# https://localhost:8080
```

### Monitor & Rollback
```bash
# Monitor
kubectl get application autonova-production -n argocd -w
kubectl get pods -n autonova -w

# Rollback
argocd app history autonova-production
argocd app rollback autonova-production <REVISION>
```

For full documentation, see the deployment plan in the plan file.
