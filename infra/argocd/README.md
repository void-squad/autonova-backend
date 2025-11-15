# ArgoCD Setup

This directory contains ArgoCD application manifests for GitOps deployment.

## Files

- `application-dev.yaml` - ArgoCD Application for dev environment (auto-sync enabled)
- `application-production.yaml` - ArgoCD Application for production environment (manual sync)
- `repository.yaml` - Git repository credentials for ArgoCD

## Prerequisites

1. ArgoCD installed in the cluster:
```bash
kubectl create namespace argocd
kubectl apply -n argocd -f https://raw.githubusercontent.com/argoproj/argo-cd/stable/manifests/install.yaml
```

2. Access ArgoCD UI:
```bash
# Get initial admin password
kubectl -n argocd get secret argocd-initial-admin-secret -o jsonpath="{.data.password}" | base64 -d

# Port forward to access UI
kubectl port-forward svc/argocd-server -n argocd 8080:443

# Access at https://localhost:8080
# Login: admin / <password from above>
```

3. Install ArgoCD CLI (optional):
```bash
curl -sSL -o argocd-linux-amd64 https://github.com/argoproj/argo-cd/releases/latest/download/argocd-linux-amd64
sudo install -m 555 argocd-linux-amd64 /usr/local/bin/argocd
rm argocd-linux-amd64
```

## Setup

1. **Update repository credentials:**
   Edit `repository.yaml` and replace `YOUR_GITHUB_PERSONAL_ACCESS_TOKEN` with your GitHub Personal Access Token.

2. **Apply repository secret:**
```bash
kubectl apply -f argocd/repository.yaml
```

3. **Deploy dev environment:**
```bash
kubectl apply -f argocd/application-dev.yaml
```

4. **Deploy production environment:**
```bash
kubectl apply -f argocd/application-production.yaml
```

## Usage

### View Applications

```bash
# Via CLI
argocd app list

# View specific application
argocd app get autonova-production

# View sync history
argocd app history autonova-production
```

### Sync Production (Manual)

```bash
# Via CLI
argocd app sync autonova-production

# Via UI
# Navigate to the application and click "Sync"
```

### Rollback

```bash
# View history
argocd app history autonova-production

# Rollback to specific revision
argocd app rollback autonova-production <REVISION_ID>

# Or via kubectl
kubectl patch application autonova-production -n argocd \
  --type merge \
  --patch '{"spec":{"source":{"targetRevision":"<COMMIT_SHA>"}}}'
```

### Monitor Deployment

```bash
# Watch application status
argocd app get autonova-production --watch

# View logs
kubectl logs -n argocd deployment/argocd-application-controller -f

# Check pod status
kubectl get pods -n autonova -w
```

## Deployment Flow

### Development
1. Push to `dev` branch
2. GitHub Actions builds images (`:dev-latest`, `:sha-{hash}`)
3. ArgoCD auto-syncs dev overlay
4. New pods deployed automatically

### Production
1. Create PR: dev → main
2. Code review + approval
3. Merge to main
4. GitHub Actions builds images (`:latest`, `:v{build-number}`)
5. ArgoCD detects change (manual sync required)
6. DevOps reviews in ArgoCD UI
7. Manual sync triggered
8. Health checks validate deployment
9. Rollback if issues detected

## Notifications (Optional)

Configure ArgoCD notifications for Slack/Email:

```bash
kubectl apply -n argocd -f https://raw.githubusercontent.com/argoproj/argo-cd/stable/notifications_catalog/install.yaml
```

Update `application-production.yaml` annotations with your Slack channel.

## Troubleshooting

### Application Out of Sync
```bash
argocd app sync autonova-production --prune
```

### Sync Failed
```bash
# View sync details
argocd app get autonova-production

# View detailed logs
kubectl logs -n argocd deployment/argocd-server
```

### Manual Override
```bash
# Temporarily disable auto-sync
argocd app set autonova-dev --sync-policy none

# Re-enable
argocd app set autonova-dev --sync-policy automated
```
