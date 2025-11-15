# Rollback Runbook

This runbook provides detailed procedures for rolling back deployments in various scenarios.

## Table of Contents

1. [Quick Reference](#quick-reference)
2. [Scenario 1: Immediate Rollback (Service Failure)](#scenario-1-immediate-rollback-service-failure)
3. [Scenario 2: Selective Service Rollback](#scenario-2-selective-service-rollback)
4. [Scenario 3: Gradual Rollback with Monitoring](#scenario-3-gradual-rollback-with-monitoring)
5. [Scenario 4: Database Migration Rollback](#scenario-4-database-migration-rollback)
6. [Scenario 5: Configuration Rollback](#scenario-5-configuration-rollback)
7. [Post-Rollback Procedures](#post-rollback-procedures)
8. [Prevention & Best Practices](#prevention--best-practices)

## Quick Reference

### ArgoCD Rollback Commands
```bash
# View deployment history
argocd app history autonova-production

# Rollback to previous version
argocd app rollback autonova-production <REVISION_ID>

# Rollback to specific Git commit
kubectl patch application autonova-production -n argocd \
  --type merge \
  --patch '{"spec":{"source":{"targetRevision":"<COMMIT_SHA>"}}}'
```

### Kubernetes Rollback Commands
```bash
# Rollback specific deployment
kubectl rollout undo deployment/<service-name> -n autonova

# Rollback to specific revision
kubectl rollout undo deployment/<service-name> -n autonova --to-revision=<NUMBER>

# Check rollout status
kubectl rollout status deployment/<service-name> -n autonova

# View rollout history
kubectl rollout history deployment/<service-name> -n autonova
```

## Scenario 1: Immediate Rollback (Service Failure)

**When to use:** Critical service outage, high error rates (>10%), or complete service failure.

**Time to Complete:** 2-5 minutes

### Symptoms
- Service health checks failing
- 5xx errors > 10% of traffic
- Complete service unavailability
- Critical alerts firing

### Procedure

#### Step 1: Assess the Situation
```bash
# Check pod status
kubectl get pods -n autonova -l app=<service-name>

# Check recent logs
kubectl logs -n autonova deployment/<service-name> --tail=100

# Check application status in ArgoCD
argocd app get autonova-production

# Check recent events
kubectl get events -n autonova --sort-by='.lastTimestamp' | head -20
```

#### Step 2: Initiate Rollback via ArgoCD
```bash
# View deployment history (shows last 10 deployments)
argocd app history autonova-production

# Identify the last working revision (usually REVISION-1 from current)
# Note the revision ID

# Rollback to previous working version
argocd app rollback autonova-production <PREVIOUS_REVISION_ID>

# Alternatively, rollback via Git commit
argocd app set autonova-production --revision <WORKING_COMMIT_SHA>
argocd app sync autonova-production
```

#### Step 3: Monitor Rollback Progress
```bash
# Watch pods rolling back
kubectl get pods -n autonova -l app=<service-name> -w

# Watch deployment status
kubectl rollout status deployment/<service-name> -n autonova

# Monitor ArgoCD sync status
argocd app get autonova-production --watch

# Check health after rollback
kubectl get pods -n autonova -l app=<service-name>
kubectl logs -n autonova deployment/<service-name> --tail=50
```

#### Step 4: Verify Service Recovery
```bash
# Test health endpoint
kubectl run -it --rm debug --image=curlimages/curl --restart=Never -- \
  curl http://<service-name>:PORT/actuator/health

# Check error rates (via Prometheus/Grafana)
# Navigate to Grafana → Autonova Dashboard → Error Rates

# Test critical user paths
curl -k https://api.autonova.example.com/<critical-endpoint>
```

#### Step 5: Communicate Status
```
- Notify team via Slack/Email about rollback completion
- Update incident ticket with rollback details
- Schedule post-mortem meeting
```

---

## Scenario 2: Selective Service Rollback

**When to use:** Only one or few services are affected, others are stable.

**Time to Complete:** 3-7 minutes

### Procedure

#### Step 1: Identify Affected Service
```bash
# List all services
kubectl get deployments -n autonova

# Check which services were recently updated
kubectl get deployments -n autonova -o json | \
  jq -r '.items[] | select(.metadata.annotations."deployment.kubernetes.io/revision" != null) | 
  "\(.metadata.name): \(.metadata.annotations."deployment.kubernetes.io/revision")"'
```

#### Step 2: Rollback Single Service
```bash
# Get rollout history for the service
kubectl rollout history deployment/<service-name> -n autonova

# View specific revision details
kubectl rollout history deployment/<service-name> -n autonova --revision=<NUMBER>

# Rollback to previous revision
kubectl rollout undo deployment/<service-name> -n autonova

# Or rollback to specific revision
kubectl rollout undo deployment/<service-name> -n autonova --to-revision=<NUMBER>
```

#### Step 3: Update ArgoCD to Match
```bash
# ArgoCD will detect drift and show out-of-sync
# Option 1: Accept the manual rollback
argocd app set autonova-production --sync-policy none

# Option 2: Update Git to match current state
# Edit infra/k8s/overlays/production/kustomization.yaml
# Change the image tag for the specific service back to previous version
# Commit and push changes
```

#### Step 4: Verify and Monitor
```bash
# Check pod status
kubectl get pods -n autonova -l app=<service-name>

# Tail logs
kubectl logs -n autonova deployment/<service-name> -f

# Check dependent services
kubectl logs -n autonova deployment/gateway-service | grep <service-name>
```

---

## Scenario 3: Gradual Rollback with Monitoring

**When to use:** Non-critical issues, want to validate rollback progressively.

**Time to Complete:** 10-20 minutes

### Procedure

#### Step 1: Scale Down New Version
```bash
# Reduce replicas of problematic deployment
kubectl scale deployment/<service-name> -n autonova --replicas=1

# Monitor traffic and errors
# Check if issues decrease with reduced traffic to new version
```

#### Step 2: Deploy Old Version Alongside (Blue-Green)
```bash
# Create a copy of the deployment with old image
kubectl get deployment/<service-name> -n autonova -o yaml > /tmp/old-deployment.yaml

# Edit the file:
# - Change name to <service-name>-old
# - Change image tag to previous version
# - Change selector label: version: old
# - Set replicas to 2

kubectl apply -f /tmp/old-deployment.yaml

# Verify old version is running
kubectl get pods -n autonova -l app=<service-name>,version=old
```

#### Step 3: Shift Traffic Gradually
```bash
# Update service selector to route to old version
kubectl patch service/<service-name> -n autonova -p '{"spec":{"selector":{"version":"old"}}}'

# Monitor for 5-10 minutes
# Check error rates, latency, logs
```

#### Step 4: Complete Rollback
```bash
# If old version is stable, remove new version
kubectl delete deployment/<service-name> -n autonova

# Rename old deployment back
kubectl get deployment/<service-name>-old -n autonova -o yaml | \
  sed 's/<service-name>-old/<service-name>/g' | \
  sed 's/version: old/version: stable/g' | \
  kubectl apply -f -

# Delete the temporary old deployment
kubectl delete deployment/<service-name>-old -n autonova

# Update service selector
kubectl patch service/<service-name> -n autonova -p '{"spec":{"selector":{"app":"<service-name>"}}}'
```

---

## Scenario 4: Database Migration Rollback

**When to use:** Database schema changes causing issues.

**Time to Complete:** 5-15 minutes

### Procedure

#### Step 1: Assess Database State
```bash
# Connect to database (Neon PostgreSQL)
psql "postgresql://<username>:<password>@<host>/neondb?sslmode=require"

# Check migration history (if using Flyway/Liquibase)
SELECT * FROM flyway_schema_history ORDER BY installed_rank DESC LIMIT 10;
# OR
SELECT * FROM databasechangelog ORDER BY dateexecuted DESC LIMIT 10;
```

#### Step 2: Stop Application Access
```bash
# Scale down the service to prevent new writes
kubectl scale deployment/<service-name> -n autonova --replicas=0

# Verify no pods running
kubectl get pods -n autonova -l app=<service-name>
```

#### Step 3: Rollback Database Migration

**For Flyway:**
```bash
# Exec into a pod with database access
kubectl run -it --rm db-rollback --image=jalinahirushan02/<service-name>:previous-version \
  --restart=Never -n autonova -- /bin/sh

# Inside the pod
java -jar app.jar db rollback --count 1

# Or use Flyway CLI
flyway -url=jdbc:postgresql://<host>/neondb -user=<user> -password=<password> undo
```

**For Liquibase:**
```sql
-- Manual rollback via SQL
-- Revert schema changes manually based on changeset
-- Example:
DROP TABLE IF EXISTS new_table;
ALTER TABLE existing_table DROP COLUMN new_column;

-- Update changelog
DELETE FROM databasechangelog WHERE id = '<changeset-id>';
```

**Manual Rollback:**
```sql
-- If no migration tool, manually revert schema
-- Use backups if available
pg_restore --clean --if-exists -d neondb backup.dump

-- Or execute rollback SQL scripts
\i rollback-20250116.sql
```

#### Step 4: Deploy Previous Application Version
```bash
# Rollback application deployment
kubectl rollout undo deployment/<service-name> -n autonova

# Scale back up
kubectl scale deployment/<service-name> -n autonova --replicas=2

# Monitor startup
kubectl logs -n autonova deployment/<service-name> -f
```

#### Step 5: Verify Data Integrity
```sql
-- Run validation queries
SELECT COUNT(*) FROM critical_table;
SELECT * FROM audit_log ORDER BY timestamp DESC LIMIT 10;

-- Test application functionality
-- Run integration tests
```

---

## Scenario 5: Configuration Rollback

**When to use:** ConfigMap or Secret changes causing issues.

**Time to Complete:** 3-5 minutes

### Procedure

#### Step 1: Identify Configuration Changes
```bash
# View recent ConfigMap/Secret changes
kubectl get configmap -n autonova -o yaml | grep -A 5 "creationTimestamp"
kubectl get secret -n autonova -o yaml | grep -A 5 "creationTimestamp"

# Check ConfigMap history (if using Git)
git log --oneline --follow infra/k8s/overlays/production/patches/gateway-cors.yaml
```

#### Step 2: Rollback Configuration via Git
```bash
# Find previous working commit
git log --oneline -- infra/k8s/overlays/production/patches/<config-file>.yaml

# Revert to previous version
git revert <commit-sha>
git push origin main

# Sync via ArgoCD
argocd app sync autonova-production
```

#### Step 3: Manual Configuration Rollback (Emergency)
```bash
# Edit ConfigMap directly
kubectl edit configmap/<config-name> -n autonova

# Or replace with backup
kubectl apply -f backup/<config-name>.yaml

# Rollout restart to pick up new config
kubectl rollout restart deployment/<service-name> -n autonova
```

#### Step 4: Rollback External Secrets
```bash
# If using GCP Secret Manager
gcloud secrets versions list autonova-<SECRET_KEY>

# Disable current version
gcloud secrets versions disable <VERSION> --secret autonova-<SECRET_KEY>

# Enable previous version
gcloud secrets versions enable <PREVIOUS_VERSION> --secret autonova-<SECRET_KEY>

# Force External Secrets resync
kubectl delete externalsecret autonova-secrets -n autonova
kubectl apply -f infra/k8s/overlays/production/external-secrets/external-secret.yaml
```

---

## Post-Rollback Procedures

### 1. Verify System Health
```bash
# Check all pods
kubectl get pods -n autonova

# Check services
kubectl get svc -n autonova

# Test critical endpoints
./scripts/smoke-tests.sh

# Review metrics
# Open Grafana → Autonova Dashboard
# Check: Error rates, latency, throughput
```

### 2. Document the Rollback
```markdown
**Incident Report: [Date] Rollback**

- **Time:** [Start] - [End]
- **Trigger:** [What caused the rollback]
- **Affected Services:** [List services]
- **Rollback Method:** [ArgoCD/kubectl/manual]
- **Revision Rolled Back:** [From version X to version Y]
- **Impact:** [User impact, duration]
- **Root Cause:** [Brief description]
- **Prevention:** [How to prevent in future]
```

### 3. Update Monitoring
```bash
# Add annotation to Grafana
# Mark the rollback time on dashboards
curl -X POST https://grafana.example.com/api/annotations \
  -H "Authorization: Bearer $GRAFANA_API_KEY" \
  -H "Content-Type: application/json" \
  -d '{
    "time": '$(date +%s000)',
    "text": "Rollback: <service-name> to v<version>",
    "tags": ["rollback", "production"]
  }'
```

### 4. Schedule Post-Mortem
- Schedule within 24-48 hours
- Invite: DevOps, Backend team, Product
- Agenda: Timeline, root cause, action items
- Output: Incident report, prevention tasks

---

## Prevention & Best Practices

### 1. Pre-Deployment Checklist
- [ ] All tests passing (unit, integration, e2e)
- [ ] Code reviewed by at least 2 developers
- [ ] Database migrations tested in staging
- [ ] Load testing completed
- [ ] Rollback plan documented
- [ ] Monitoring dashboards ready
- [ ] On-call engineer notified

### 2. Deployment Best Practices
- Deploy during low-traffic hours
- Use canary deployments (Argo Rollouts)
- Monitor for 30 minutes post-deployment
- Keep previous version readily accessible
- Test rollback procedure in staging first

### 3. Database Migration Best Practices
- Always make migrations backward-compatible
- Separate schema changes from code changes
- Test migrations against production-like data
- Keep rollback scripts alongside migration scripts
- Backup database before migrations

### 4. Monitoring & Alerting
- Set up alerts for:
  - Error rate > 5%
  - Latency p95 > 2s
  - Pod restarts > 3 in 15 min
  - Health check failures
- Configure PagerDuty/Opsgenie for critical alerts
- Use Grafana dashboards for real-time monitoring

### 5. Testing Strategy
```bash
# Automated rollback testing (in staging)
./scripts/test-rollback.sh

# Contents:
# 1. Deploy new version
# 2. Introduce failure
# 3. Trigger rollback
# 4. Verify system recovery
# 5. Measure rollback time
```

### 6. Communication Protocol
- **Before deployment:** Notify team in #deployments channel
- **During rollback:** Update incident channel with status
- **After rollback:** Send all-clear message
- **Post-mortem:** Share findings and action items

---

## Emergency Contacts

| Role | Contact | Availability |
|------|---------|--------------|
| DevOps Lead | [Name] | 24/7 |
| Backend Lead | [Name] | Business hours |
| Database Admin | [Name] | On-call |
| Product Manager | [Name] | Business hours |

---

## Useful Commands Cheatsheet

```bash
# ArgoCD
argocd app list
argocd app get autonova-production
argocd app history autonova-production
argocd app rollback autonova-production <REV>
argocd app sync autonova-production

# Kubernetes
kubectl get pods -n autonova
kubectl get deployments -n autonova
kubectl rollout history deployment/<name> -n autonova
kubectl rollout undo deployment/<name> -n autonova
kubectl rollout status deployment/<name> -n autonova
kubectl logs -n autonova deployment/<name> -f
kubectl describe pod/<pod-name> -n autonova
kubectl get events -n autonova --sort-by='.lastTimestamp'

# Debugging
kubectl run -it --rm debug --image=curlimages/curl --restart=Never -- sh
kubectl exec -it <pod-name> -n autonova -- /bin/sh
kubectl port-forward deployment/<name> -n autonova 8080:8080

# Database
psql "postgresql://<user>:<pass>@<host>/neondb?sslmode=require"
```

---

## Related Documentation

- [Deployment Plan](./DEPLOYMENT_PLAN.md)
- [ArgoCD Documentation](../infra/argocd/README.md)
- [Monitoring Setup](../infra/k8s/overlays/production/monitoring/)
- [Infrastructure Scripts](../infra/k8s/scripts/README.md)
