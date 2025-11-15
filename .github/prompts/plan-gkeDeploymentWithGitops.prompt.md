# GKE Deployment Plan with GitOps (ArgoCD)

## Overview

Deploy the Autonova microservices platform to Google Kubernetes Engine with a production-ready GitOps workflow using ArgoCD. The existing Kustomize base and GitHub Actions CI/CD provide a solid foundation. This plan focuses on adding critical production components while maintaining the current Neon PostgreSQL setup.

## Current State Assessment

### ✅ What's Already Working
- **12 Java/Spring Boot microservices** + 1 .NET service + React frontend
- **Complete Kustomize base** with manifests for most services
- **GitHub Actions CI/CD** building multi-arch images (amd64/arm64)
- **Docker Hub registry** with automated image tagging (`:latest`, `:dev-latest`, `:sha-{hash}`)
- **Neon PostgreSQL** cloud-managed database with SSL
- **RabbitMQ** event-driven messaging architecture
- **Spring Cloud Gateway** with Eureka service discovery
- **Multi-stage Dockerfiles** for all services

### ❌ Critical Gaps to Address
1. **No production Kustomize overlay** (only `overlays/dev` exists)
2. **No Ingress configuration** (services are ClusterIP only)
3. **Manual secret management** (`.env` files converted to K8s secrets)
4. **No GitOps automation** (manual kubectl apply)
5. **Missing K8s manifests** for 4 services (time-logging, employee-dashboard, appointment-booking, chatbot)
6. **No persistent storage** for RabbitMQ
7. **No observability stack** (metrics, logging, tracing)
8. **Hardcoded localhost URLs** in gateway configuration

## Deployment Architecture

### Infrastructure Components

```
┌─────────────────────────────────────────────────────────────────┐
│                         GKE Cluster                              │
│                                                                   │
│  ┌────────────────────────────────────────────────────────────┐ │
│  │                    Ingress Layer                            │ │
│  │  - Nginx Ingress Controller / GKE Ingress                  │ │
│  │  - TLS Certificates (cert-manager / Google-managed)        │ │
│  │  - Domain: autonova.example.com                            │ │
│  └────────────────────────────────────────────────────────────┘ │
│                              │                                   │
│  ┌────────────────────────────────────────────────────────────┐ │
│  │                   Gateway Service (8080)                    │ │
│  │  - Spring Cloud Gateway                                     │ │
│  │  - CORS Configuration                                       │ │
│  │  - Route all /api/* to backend services                    │ │
│  └────────────────────────────────────────────────────────────┘ │
│                              │                                   │
│  ┌──────────────────┬────────┴─────────┬─────────────────────┐ │
│  │                  │                  │                     │ │
│  │  Business Logic  │  Infrastructure  │    Frontend        │ │
│  │  Services        │  Services        │                     │ │
│  │                  │                  │                     │ │
│  │ • auth-service   │ • discovery      │ • React SPA        │ │
│  │ • customer       │ • gateway        │   (Nginx)          │ │
│  │ • project        │ • notification   │                     │ │
│  │ • progress-mon.  │ • chatbot        │                     │ │
│  │ • payments       │                  │                     │ │
│  │ • time-logging   │                  │                     │ │
│  │ • employee-dash  │                  │                     │ │
│  │ • appointment    │                  │                     │ │
│  └──────────────────┴──────────────────┴─────────────────────┘ │
│                              │                                   │
│  ┌────────────────────────────────────────────────────────────┐ │
│  │                  Message Broker (RabbitMQ)                  │ │
│  │  - StatefulSet with PersistentVolume                       │ │
│  │  - Event exchanges: project.events, auth.events, etc.      │ │
│  └────────────────────────────────────────────────────────────┘ │
│                                                                   │
└─────────────────────────────────────────────────────────────────┘
                              │
        ┌─────────────────────┼─────────────────────┐
        │                     │                     │
   ┌────▼────┐        ┌───────▼───────┐    ┌───────▼────────┐
   │  Neon   │        │  GCP Secret   │    │  ArgoCD        │
   │  PG DB  │        │  Manager      │    │  (GitOps)      │
   │ (Cloud) │        │  (Secrets)    │    │                │
   └─────────┘        └───────────────┘    └────────────────┘
```

## Implementation Plan

### Phase 1: Foundation Setup (Week 1)

#### 1.1 GKE Cluster Creation
```bash
# Create GKE cluster with Workload Identity
gcloud container clusters create autonova-cluster \
  --region us-central1 \
  --num-nodes 3 \
  --machine-type n1-standard-2 \
  --enable-autoscaling \
  --min-nodes 2 \
  --max-nodes 10 \
  --enable-autorepair \
  --enable-autoupgrade \
  --workload-pool=PROJECT_ID.svc.id.goog \
  --enable-ip-alias \
  --network "default" \
  --subnetwork "default" \
  --addons HorizontalPodAutoscaling,HttpLoadBalancing,GcePersistentDiskCsiDriver
```

#### 1.2 Install Core Infrastructure
```bash
# Install ArgoCD
kubectl create namespace argocd
kubectl apply -n argocd -f https://raw.githubusercontent.com/argoproj/argocd/stable/manifests/install.yaml

# Install Nginx Ingress Controller
kubectl apply -f https://raw.githubusercontent.com/kubernetes/ingress-nginx/controller-v1.10.0/deploy/static/provider/cloud/deploy.yaml

# Install cert-manager for TLS
kubectl apply -f https://github.com/cert-manager/cert-manager/releases/download/v1.14.0/cert-manager.yaml

# Install External Secrets Operator (for GCP Secret Manager)
helm repo add external-secrets https://charts.external-secrets.io
helm install external-secrets \
  external-secrets/external-secrets \
  -n external-secrets-system \
  --create-namespace
```

#### 1.3 Configure GCP Secret Manager Integration
```bash
# Create GCP service account for secret access
gcloud iam service-accounts create autonova-secrets-sa \
  --display-name="Autonova Secrets Service Account"

# Grant Secret Manager access
gcloud projects add-iam-policy-binding PROJECT_ID \
  --member="serviceAccount:autonova-secrets-sa@PROJECT_ID.iam.gserviceaccount.com" \
  --role="roles/secretmanager.secretAccessor"

# Bind to K8s Service Account via Workload Identity
gcloud iam service-accounts add-iam-policy-binding \
  autonova-secrets-sa@PROJECT_ID.iam.gserviceaccount.com \
  --role roles/iam.workloadIdentityUser \
  --member "serviceAccount:PROJECT_ID.svc.id.goog[autonova/external-secrets]"
```

### Phase 2: Kubernetes Manifests Completion (Week 1-2)

#### 2.1 Add Missing Service Manifests
Create K8s manifests for:
- `time-logging-service` (integrate into base kustomization)
- `employee-dashboard-service` 
- `appointment-booking-service`
- `chatbot` service

**Template structure:**
```yaml
# k8s/base/[service-name]/deployment.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: [service-name]
  namespace: autonova
spec:
  replicas: 1
  selector:
    matchLabels:
      app: [service-name]
  template:
    metadata:
      labels:
        app: [service-name]
    spec:
      containers:
      - name: [service-name]
        image: jalinahirushan02/[service-name]:latest
        ports:
        - containerPort: [port]
        env:
        - name: SPRING_PROFILES_ACTIVE
          value: "prod"
        envFrom:
        - secretRef:
            name: autonova-secrets
        resources:
          requests:
            memory: "512Mi"
            cpu: "250m"
          limits:
            memory: "1Gi"
            cpu: "500m"
        livenessProbe:
          httpGet:
            path: /actuator/health/liveness
            port: [port]
          initialDelaySeconds: 90
          periodSeconds: 10
        readinessProbe:
          httpGet:
            path: /actuator/health/readiness
            port: [port]
          initialDelaySeconds: 60
          periodSeconds: 5

---
# k8s/base/[service-name]/service.yaml
apiVersion: v1
kind: Service
metadata:
  name: [service-name]
  namespace: autonova
spec:
  selector:
    app: [service-name]
  ports:
  - port: [port]
    targetPort: [port]
  type: ClusterIP
```

#### 2.2 Update Base Kustomization
```yaml
# k8s/base/kustomization.yaml
apiVersion: kustomize.config.k8s.io/v1beta1
kind: Kustomization

namespace: autonova

resources:
  - namespace.yaml
  
  # Infrastructure
  - discovery-service/deployment.yaml
  - discovery-service/service.yaml
  - rabbitmq/rabbitmq-deployment.yaml
  - rabbitmq/rabbitmq-service.yaml
  - rabbitmq/rabbitmq-pvc.yaml  # NEW: Persistent storage
  
  # Core services
  - auth-service/deployment.yaml
  - auth-service/service.yaml
  - customer-service/deployment.yaml
  - customer-service/service.yaml
  - project-service/deployment.yaml
  - project-service/service.yaml
  - progress-monitoring-service/deployment.yaml
  - progress-monitoring-service/service.yaml
  - notification-service/deployment.yaml
  - notification-service/service.yaml
  - payments-billing-service/deployment.yaml
  - payments-billing-service/service.yaml
  - gateway-service/deployment.yaml
  - gateway-service/service.yaml
  
  # NEW: Previously missing services
  - time-logging-service/deployment.yaml
  - time-logging-service/service.yaml
  - employee-dashboard-service/deployment.yaml
  - employee-dashboard-service/service.yaml
  - appointment-booking-service/deployment.yaml
  - appointment-booking-service/service.yaml
  - chatbot/deployment.yaml
  - chatbot/service.yaml
  
  # Frontend
  - frontend/web-deployment.yaml
  - frontend/web-service.yaml
  
  # Database initialization
  - postgres/init-scripts-configmap.yaml
  - postgres/postgres-init-job.yaml
```

### Phase 3: Production Overlay Creation (Week 2)

#### 3.1 Create Production Overlay Structure
```bash
mkdir -p k8s/overlays/production/patches
mkdir -p k8s/overlays/production/ingress
```

#### 3.2 Production Kustomization
```yaml
# k8s/overlays/production/kustomization.yaml
apiVersion: kustomize.config.k8s.io/v1beta1
kind: Kustomization

namespace: autonova

bases:
  - ../../base

# Production image tags
images:
  - name: jalinahirushan02/auth-service
    newTag: latest
  - name: jalinahirushan02/customer-service
    newTag: latest
  - name: jalinahirushan02/gateway-service
    newTag: latest
  - name: jalinahirushan02/project-service
    newTag: latest
  - name: jalinahirushan02/progress-monitoring-service
    newTag: latest
  - name: jalinahirushan02/notification-service
    newTag: latest
  - name: jalinahirushan02/payments-billing-service
    newTag: latest
  - name: jalinahirushan02/employee-dashboard-service
    newTag: latest
  - name: jalinahirushan02/time-logging-service
    newTag: latest
  - name: jalinahirushan02/appointment-booking-service
    newTag: latest
  - name: jalinahirushan02/chatbot
    newTag: latest
  - name: jalinahirushan02/discovery-service
    newTag: latest
  - name: sachinthalk/autonova-frontend-v1
    newTag: latest

resources:
  - ingress/ingress.yaml
  - ingress/certificate.yaml
  - hpa/gateway-hpa.yaml
  - hpa/auth-hpa.yaml
  - external-secrets/secret-store.yaml
  - external-secrets/external-secret.yaml

patches:
  - path: patches/production-resources.yaml
  - path: patches/production-replicas.yaml
  - path: patches/production-env.yaml
```

#### 3.3 Ingress Configuration
```yaml
# k8s/overlays/production/ingress/ingress.yaml
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: autonova-ingress
  namespace: autonova
  annotations:
    cert-manager.io/cluster-issuer: "letsencrypt-prod"
    nginx.ingress.kubernetes.io/ssl-redirect: "true"
    nginx.ingress.kubernetes.io/proxy-body-size: "50m"
    nginx.ingress.kubernetes.io/proxy-read-timeout: "600"
    nginx.ingress.kubernetes.io/proxy-send-timeout: "600"
spec:
  ingressClassName: nginx
  tls:
  - hosts:
    - autonova.example.com
    - api.autonova.example.com
    secretName: autonova-tls
  rules:
  # API Gateway
  - host: api.autonova.example.com
    http:
      paths:
      - path: /
        pathType: Prefix
        backend:
          service:
            name: gateway-service
            port:
              number: 8080
  
  # Frontend
  - host: autonova.example.com
    http:
      paths:
      - path: /
        pathType: Prefix
        backend:
          service:
            name: web-service
            port:
              number: 80
```

#### 3.4 TLS Certificate
```yaml
# k8s/overlays/production/ingress/certificate.yaml
apiVersion: cert-manager.io/v1
kind: ClusterIssuer
metadata:
  name: letsencrypt-prod
spec:
  acme:
    server: https://acme-v02.api.letsencrypt.org/directory
    email: admin@autonova.example.com
    privateKeySecretRef:
      name: letsencrypt-prod
    solvers:
    - http01:
        ingress:
          class: nginx
```

#### 3.5 HorizontalPodAutoscaler
```yaml
# k8s/overlays/production/hpa/gateway-hpa.yaml
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: gateway-service-hpa
  namespace: autonova
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: gateway-service
  minReplicas: 2
  maxReplicas: 10
  metrics:
  - type: Resource
    resource:
      name: cpu
      target:
        type: Utilization
        averageUtilization: 70
  - type: Resource
    resource:
      name: memory
      target:
        type: Utilization
        averageUtilization: 80
  behavior:
    scaleDown:
      stabilizationWindowSeconds: 300
      policies:
      - type: Percent
        value: 50
        periodSeconds: 60
    scaleUp:
      stabilizationWindowSeconds: 0
      policies:
      - type: Percent
        value: 100
        periodSeconds: 30
      - type: Pods
        value: 2
        periodSeconds: 60
      selectPolicy: Max
```

#### 3.6 Resource Patches
```yaml
# k8s/overlays/production/patches/production-resources.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: gateway-service
spec:
  template:
    spec:
      containers:
      - name: gateway-service
        resources:
          requests:
            memory: "1Gi"
            cpu: "500m"
          limits:
            memory: "2Gi"
            cpu: "1000m"
---
apiVersion: apps/v1
kind: Deployment
metadata:
  name: auth-service
spec:
  template:
    spec:
      containers:
      - name: auth-service
        resources:
          requests:
            memory: "768Mi"
            cpu: "500m"
          limits:
            memory: "1536Mi"
            cpu: "1000m"
# ... repeat for other services with appropriate sizing
```

#### 3.7 Production Environment Overrides
```yaml
# k8s/overlays/production/patches/production-env.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: gateway-service
spec:
  template:
    spec:
      containers:
      - name: gateway-service
        env:
        - name: SPRING_PROFILES_ACTIVE
          value: "prod"
        - name: LOGGING_LEVEL_ROOT
          value: "INFO"
        - name: EUREKA_CLIENT_ENABLED
          value: "true"
        - name: EUREKA_SERVER_URL
          value: "http://discovery-service:8761/eureka"
        - name: AUTH_SERVICE_URL
          value: "http://auth-service:8081"
        - name: CUSTOMER_SERVICE_URL
          value: "http://customer-service:8087"
        - name: PROJECT_SERVICE_URL
          value: "http://project-service:8082"
        - name: PROGRESS_MONITORING_SERVICE_URL
          value: "http://progress-monitoring-service:8086"
        - name: EMPLOYEE_DASHBOARD_SERVICE_URL
          value: "http://employee-dashboard-service:8084"
        - name: PAYMENTS_BILLING_SERVICE_URL
          value: "http://payments-billing-service:8069"
        - name: APPOINTMENT_BOOKING_SERVICE_URL
          value: "http://appointment-booking-service:8088"
        - name: NOTIFICATION_SERVICE_URL
          value: "http://notification-service:8085"
        - name: CHATBOT_SERVICE_URL
          value: "http://chatbot:8097"
```

### Phase 4: Secret Management with GCP Secret Manager (Week 2)

#### 4.1 Migrate Secrets to GCP Secret Manager
```bash
# Create secrets in GCP Secret Manager from existing .env
while IFS='=' read -r key value; do
  if [[ ! $key =~ ^#.* ]] && [[ -n $key ]]; then
    echo "Creating secret: $key"
    echo -n "$value" | gcloud secrets create "autonova-$key" \
      --data-file=- \
      --replication-policy="automatic"
  fi
done < k8s/secrets/.env
```

#### 4.2 External Secrets Configuration
```yaml
# k8s/overlays/production/external-secrets/secret-store.yaml
apiVersion: external-secrets.io/v1beta1
kind: SecretStore
metadata:
  name: gcpsm-secret-store
  namespace: autonova
spec:
  provider:
    gcpsm:
      projectID: "PROJECT_ID"
      auth:
        workloadIdentity:
          clusterLocation: us-central1
          clusterName: autonova-cluster
          serviceAccountRef:
            name: external-secrets
```

```yaml
# k8s/overlays/production/external-secrets/external-secret.yaml
apiVersion: external-secrets.io/v1beta1
kind: ExternalSecret
metadata:
  name: autonova-secrets
  namespace: autonova
spec:
  refreshInterval: 1h
  secretStoreRef:
    name: gcpsm-secret-store
    kind: SecretStore
  target:
    name: autonova-secrets
    creationPolicy: Owner
  data:
  # Database
  - secretKey: POSTGRES_HOST
    remoteRef:
      key: autonova-POSTGRES_HOST
  - secretKey: POSTGRES_PORT
    remoteRef:
      key: autonova-POSTGRES_PORT
  - secretKey: POSTGRES_USER
    remoteRef:
      key: autonova-POSTGRES_USER
  - secretKey: POSTGRES_PASSWORD
    remoteRef:
      key: autonova-POSTGRES_PASSWORD
  - secretKey: PGDATABASE
    remoteRef:
      key: autonova-PGDATABASE
  
  # Service-specific DB passwords
  - secretKey: PROJECTS_DB_PASSWORD
    remoteRef:
      key: autonova-PROJECTS_DB_PASSWORD
  - secretKey: PROGRESS_MONITORING_DB_PASSWORD
    remoteRef:
      key: autonova-PROGRESS_MONITORING_DB_PASSWORD
  - secretKey: EMPLOYEE_DASHBOARD_DB_PASSWORD
    remoteRef:
      key: autonova-EMPLOYEE_DASHBOARD_DB_PASSWORD
  - secretKey: TIME_LOGGING_DB_PASSWORD
    remoteRef:
      key: autonova-TIME_LOGGING_DB_PASSWORD
  - secretKey: USER_MANAGEMENT_DB_PASSWORD
    remoteRef:
      key: autonova-USER_MANAGEMENT_DB_PASSWORD
  - secretKey: NOTIFICATION_DB_PASSWORD
    remoteRef:
      key: autonova-NOTIFICATION_DB_PASSWORD
  - secretKey: VECTOR_DB_PASSWORD
    remoteRef:
      key: autonova-VECTOR_DB_PASSWORD
  - secretKey: PBS_DB_PASSWORD
    remoteRef:
      key: autonova-PBS_DB_PASSWORD
  
  # Auth & Security
  - secretKey: JWT_SECRET
    remoteRef:
      key: autonova-JWT_SECRET
  - secretKey: GOOGLE_CLIENT_ID
    remoteRef:
      key: autonova-GOOGLE_CLIENT_ID
  - secretKey: GOOGLE_CLIENT_SECRET
    remoteRef:
      key: autonova-GOOGLE_CLIENT_SECRET
  - secretKey: EMAIL_USERNAME
    remoteRef:
      key: autonova-EMAIL_USERNAME
  - secretKey: EMAIL_PASSWORD
    remoteRef:
      key: autonova-EMAIL_PASSWORD
  
  # Payments
  - secretKey: STRIPE_API_KEY
    remoteRef:
      key: autonova-STRIPE_API_KEY
  - secretKey: STRIPE_WEBHOOK_SECRET
    remoteRef:
      key: autonova-STRIPE_WEBHOOK_SECRET
  - secretKey: STRIPE_PUBLISHABLE_KEY
    remoteRef:
      key: autonova-STRIPE_PUBLISHABLE_KEY
  
  # AI
  - secretKey: GEMINI_API_KEY
    remoteRef:
      key: autonova-GEMINI_API_KEY
  - secretKey: SPRING_AI_OLLAMA_BASE_URL
    remoteRef:
      key: autonova-SPRING_AI_OLLAMA_BASE_URL
  
  # URLs
  - secretKey: FRONTEND_URL
    remoteRef:
      key: autonova-FRONTEND_URL
```

### Phase 5: ArgoCD GitOps Setup (Week 2-3)

#### 5.1 Access ArgoCD UI
```bash
# Get initial admin password
kubectl -n argocd get secret argocd-initial-admin-secret -o jsonpath="{.data.password}" | base64 -d

# Port forward to access UI
kubectl port-forward svc/argocd-server -n argocd 8080:443

# Access at https://localhost:8080
# Login: admin / <password from above>
```

#### 5.2 Configure Git Repository
```yaml
# argocd/repository.yaml
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
  password: <GITHUB_PAT>  # Use GitHub Personal Access Token
```

Apply:
```bash
kubectl apply -f argocd/repository.yaml
```

#### 5.3 Create ArgoCD Application for Development
```yaml
# argocd/application-dev.yaml
apiVersion: argoproj.io/v1alpha1
kind: Application
metadata:
  name: autonova-dev
  namespace: argocd
  finalizers:
    - resources-finalizer.argocd.argoproj.io
spec:
  project: default
  
  source:
    repoURL: https://github.com/void-squad/autonova-backend
    targetRevision: dev
    path: infra/k8s/overlays/dev
  
  destination:
    server: https://kubernetes.default.svc
    namespace: autonova
  
  syncPolicy:
    automated:
      prune: true      # Remove resources not in Git
      selfHeal: true   # Auto-sync when cluster state drifts
      allowEmpty: false
    syncOptions:
    - CreateNamespace=true
    - PrunePropagationPolicy=foreground
    - PruneLast=true
    retry:
      limit: 5
      backoff:
        duration: 5s
        factor: 2
        maxDuration: 3m
  
  # Health assessment
  ignoreDifferences:
  - group: apps
    kind: Deployment
    jsonPointers:
    - /spec/replicas  # Ignore HPA-managed replicas
```

#### 5.4 Create ArgoCD Application for Production
```yaml
# argocd/application-production.yaml
apiVersion: argoproj.io/v1alpha1
kind: Application
metadata:
  name: autonova-production
  namespace: argocd
  finalizers:
    - resources-finalizer.argocd.argoproj.io
spec:
  project: default
  
  source:
    repoURL: https://github.com/void-squad/autonova-backend
    targetRevision: main
    path: infra/k8s/overlays/production
  
  destination:
    server: https://kubernetes.default.svc
    namespace: autonova
  
  syncPolicy:
    automated:
      prune: false      # Manual approval for deletions
      selfHeal: false   # Manual sync for production safety
      allowEmpty: false
    syncOptions:
    - CreateNamespace=true
    - PrunePropagationPolicy=foreground
    retry:
      limit: 3
      backoff:
        duration: 10s
        factor: 2
        maxDuration: 5m
  
  # Require manual approval
  syncPolicy:
    syncOptions:
    - ApprovalRequired=true
  
  ignoreDifferences:
  - group: apps
    kind: Deployment
    jsonPointers:
    - /spec/replicas
```

Apply:
```bash
kubectl apply -f argocd/application-dev.yaml
kubectl apply -f argocd/application-production.yaml
```

#### 5.5 ArgoCD CLI Setup
```bash
# Install ArgoCD CLI
curl -sSL -o argocd-linux-amd64 https://github.com/argoproj/argo-cd/releases/latest/download/argocd-linux-amd64
sudo install -m 555 argocd-linux-amd64 /usr/local/bin/argocd
rm argocd-linux-amd64

# Login
argocd login localhost:8080 --username admin --password <password>

# View applications
argocd app list

# Sync production manually
argocd app sync autonova-production

# View sync status
argocd app get autonova-production
```

### Phase 6: Rollback Strategy (Week 3)

#### 6.1 ArgoCD Rollback Mechanisms

**1. Automatic Rollback via Sync Windows**
```yaml
# argocd/sync-windows.yaml
apiVersion: argoproj.io/v1alpha1
kind: AppProject
metadata:
  name: autonova-project
  namespace: argocd
spec:
  description: Autonova microservices project
  
  sourceRepos:
  - 'https://github.com/void-squad/autonova-backend'
  
  destinations:
  - namespace: autonova
    server: https://kubernetes.default.svc
  
  # Sync windows - only allow syncs during maintenance windows
  syncWindows:
  - kind: allow
    schedule: '0 2 * * *'  # 2 AM daily
    duration: 2h
    applications:
    - autonova-production
  - kind: deny
    schedule: '0 9-17 * * 1-5'  # Business hours Mon-Fri
    duration: 8h
    applications:
    - autonova-production
```

**2. Manual Rollback via ArgoCD History**
```bash
# View application history
argocd app history autonova-production

# Rollback to specific revision
argocd app rollback autonova-production <REVISION_ID>

# Or use kubectl
kubectl patch application autonova-production -n argocd \
  --type merge \
  --patch '{"spec":{"source":{"targetRevision":"<COMMIT_SHA>"}}}'
```

**3. Progressive Delivery with Argo Rollouts**
```yaml
# argocd/rollout-strategy.yaml
apiVersion: argoproj.io/v1alpha1
kind: Rollout
metadata:
  name: gateway-service
  namespace: autonova
spec:
  replicas: 3
  strategy:
    canary:
      steps:
      - setWeight: 20
      - pause: {duration: 5m}
      - setWeight: 40
      - pause: {duration: 5m}
      - setWeight: 60
      - pause: {duration: 5m}
      - setWeight: 80
      - pause: {duration: 5m}
      # Automatic rollback on failure
      analysis:
        templates:
        - templateName: success-rate
        args:
        - name: service-name
          value: gateway-service
      # Manual approval gate
      - pause: {}
  
  revisionHistoryLimit: 5
  selector:
    matchLabels:
      app: gateway-service
  template:
    metadata:
      labels:
        app: gateway-service
    spec:
      containers:
      - name: gateway-service
        image: jalinahirushan02/gateway-service:latest
        # ... rest of spec
```

**4. Health-based Auto Rollback**
```yaml
# argocd/application-production.yaml (updated)
spec:
  syncPolicy:
    automated:
      prune: false
      selfHeal: false
    retry:
      limit: 3
      backoff:
        duration: 10s
        factor: 2
        maxDuration: 5m
  
  # Health assessment with automatic rollback
  health:
    # Wait for all pods to be ready
    timeoutSeconds: 600
    
  # Resource tracking
  revisionHistoryLimit: 10
  
  # Notifications on failure
  notifications:
    - trigger: on-health-degraded
      destination: slack
    - trigger: on-sync-failed
      destination: email
```

#### 6.2 CI/CD Pipeline Updates for Rollback Support

**Update GitHub Actions to tag images semantically:**
```yaml
# .github/workflows/backend-ci.yml (updated)
- name: Build and push Docker image
  uses: docker/build-push-action@v5
  with:
    context: ./${{ matrix.service }}
    push: true
    tags: |
      jalinahirushan02/${{ matrix.service }}:latest
      jalinahirushan02/${{ matrix.service }}:${{ github.sha }}
      jalinahirushan02/${{ matrix.service }}:v${{ github.run_number }}
    cache-from: type=registry,ref=jalinahirushan02/${{ matrix.service }}:latest
    cache-to: type=inline

# Update ArgoCD image tags automatically
- name: Update image tag in Git
  if: github.ref == 'refs/heads/main'
  run: |
    cd infra/k8s/overlays/production
    kustomize edit set image \
      jalinahirushan02/${{ matrix.service }}=jalinahirushan02/${{ matrix.service }}:v${{ github.run_number }}
    git config user.name "GitHub Actions"
    git config user.email "actions@github.com"
    git add kustomization.yaml
    git commit -m "Update ${{ matrix.service }} to v${{ github.run_number }}"
    git push
```

#### 6.3 Rollback Runbook

**Scenario 1: Immediate Rollback (Service Failure)**
```bash
# 1. View current version
argocd app get autonova-production

# 2. Check history
argocd app history autonova-production

# 3. Rollback to previous working version
argocd app rollback autonova-production <PREVIOUS_REVISION>

# 4. Verify rollback
kubectl get pods -n autonova -w
kubectl logs -n autonova deployment/gateway-service -f
```

**Scenario 2: Selective Service Rollback**
```bash
# Rollback only specific service
kubectl rollout undo deployment/gateway-service -n autonova

# Or to specific revision
kubectl rollout undo deployment/gateway-service -n autonova --to-revision=<NUMBER>

# Check rollout status
kubectl rollout status deployment/gateway-service -n autonova
```

**Scenario 3: Blue-Green Deployment (Zero Downtime)**
```yaml
# Deploy new version alongside old
apiVersion: apps/v1
kind: Deployment
metadata:
  name: gateway-service-blue
spec:
  replicas: 3
  selector:
    matchLabels:
      app: gateway-service
      version: blue
  # ... spec

---
apiVersion: apps/v1
kind: Deployment
metadata:
  name: gateway-service-green
spec:
  replicas: 3
  selector:
    matchLabels:
      app: gateway-service
      version: green
  # ... spec with new image

---
# Service routes to active version
apiVersion: v1
kind: Service
metadata:
  name: gateway-service
spec:
  selector:
    app: gateway-service
    version: blue  # Switch to green when ready
```

**Scenario 4: Database Migration Rollback**
```bash
# For services with database changes, maintain migration scripts
# in version control with rollback capability

# Example: Liquibase/Flyway rollback
kubectl exec -it deployment/auth-service -n autonova -- \
  java -jar app.jar db rollback --count 1

# Or use init Job with rollback script
kubectl apply -f k8s/rollback-jobs/auth-db-rollback-job.yaml
```

### Phase 7: Observability Stack (Week 3-4)

#### 7.1 Install Prometheus Stack
```bash
# Add Prometheus community Helm repo
helm repo add prometheus-community https://prometheus-community.github.io/helm-charts
helm repo update

# Install kube-prometheus-stack (includes Grafana)
helm install prometheus prometheus-community/kube-prometheus-stack \
  --namespace monitoring \
  --create-namespace \
  --set prometheus.prometheusSpec.serviceMonitorSelectorNilUsesHelmValues=false \
  --set grafana.adminPassword='admin123' \
  --set prometheus.prometheusSpec.retention=30d \
  --set prometheus.prometheusSpec.storageSpec.volumeClaimTemplate.spec.resources.requests.storage=50Gi
```

#### 7.2 Configure ServiceMonitors for Spring Boot Services
```yaml
# k8s/overlays/production/monitoring/service-monitor.yaml
apiVersion: monitoring.coreos.com/v1
kind: ServiceMonitor
metadata:
  name: autonova-services
  namespace: autonova
  labels:
    release: prometheus
spec:
  selector:
    matchLabels:
      monitoring: enabled
  endpoints:
  - port: http
    path: /actuator/prometheus
    interval: 30s
    scrapeTimeout: 10s
```

#### 7.3 Add Monitoring Labels to Services
```yaml
# Apply to all service manifests
metadata:
  labels:
    monitoring: enabled
```

#### 7.4 Grafana Dashboards
- Import Spring Boot Dashboard (ID: 12464)
- Import JVM Dashboard (ID: 4701)
- Import RabbitMQ Dashboard (ID: 10991)
- Custom dashboard for business metrics

#### 7.5 Alerting Rules
```yaml
# k8s/overlays/production/monitoring/alerts.yaml
apiVersion: monitoring.coreos.com/v1
kind: PrometheusRule
metadata:
  name: autonova-alerts
  namespace: monitoring
spec:
  groups:
  - name: autonova
    interval: 30s
    rules:
    - alert: HighErrorRate
      expr: rate(http_server_requests_seconds_count{status=~"5.."}[5m]) > 0.05
      for: 5m
      labels:
        severity: critical
      annotations:
        summary: "High error rate detected"
        description: "Service {{ $labels.service }} has error rate above 5%"
    
    - alert: HighMemoryUsage
      expr: container_memory_usage_bytes / container_spec_memory_limit_bytes > 0.9
      for: 5m
      labels:
        severity: warning
      annotations:
        summary: "High memory usage"
        description: "Container {{ $labels.pod }} using over 90% memory"
    
    - alert: ServiceDown
      expr: up{job="autonova-services"} == 0
      for: 2m
      labels:
        severity: critical
      annotations:
        summary: "Service is down"
        description: "Service {{ $labels.service }} is unreachable"
```

### Phase 8: RabbitMQ Persistent Storage (Week 2)

#### 8.1 Create PersistentVolumeClaim
```yaml
# k8s/base/rabbitmq/rabbitmq-pvc.yaml
apiVersion: v1
kind: PersistentVolumeClaim
metadata:
  name: rabbitmq-data
  namespace: autonova
spec:
  accessModes:
    - ReadWriteOnce
  storageClassName: standard-rwo  # GKE standard persistent disk
  resources:
    requests:
      storage: 10Gi
```

#### 8.2 Update RabbitMQ Deployment to StatefulSet
```yaml
# k8s/base/rabbitmq/rabbitmq-statefulset.yaml
apiVersion: apps/v1
kind: StatefulSet
metadata:
  name: rabbitmq
  namespace: autonova
spec:
  serviceName: rabbitmq
  replicas: 1
  selector:
    matchLabels:
      app: rabbitmq
  template:
    metadata:
      labels:
        app: rabbitmq
    spec:
      containers:
      - name: rabbitmq
        image: rabbitmq:4.1.4-management
        ports:
        - containerPort: 5672
          name: amqp
        - containerPort: 15672
          name: management
        env:
        - name: RABBITMQ_DEFAULT_USER
          value: "admin"
        - name: RABBITMQ_DEFAULT_PASS
          valueFrom:
            secretKeyRef:
              name: autonova-secrets
              key: RABBITMQ_PASSWORD
        volumeMounts:
        - name: rabbitmq-data
          mountPath: /var/lib/rabbitmq
        resources:
          requests:
            memory: "512Mi"
            cpu: "250m"
          limits:
            memory: "1Gi"
            cpu: "500m"
  volumeClaimTemplates:
  - metadata:
      name: rabbitmq-data
    spec:
      accessModes: [ "ReadWriteOnce" ]
      storageClassName: standard-rwo
      resources:
        requests:
          storage: 10Gi
```

### Phase 9: Gateway CORS Updates (Week 2)

#### 9.1 Update Gateway Configuration for Production
```yaml
# k8s/overlays/production/patches/gateway-cors.yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: gateway-config
  namespace: autonova
data:
  application-prod.yml: |
    spring:
      cloud:
        gateway:
          globalcors:
            add-to-simple-url-handler-mapping: true
            corsConfigurations:
              "[/**]":
                allowedOrigins:
                  - https://autonova.example.com
                  - https://www.autonova.example.com
                allowedOriginPatterns:
                  - https://*.autonova.example.com
                allowedMethods:
                  - GET
                  - POST
                  - PUT
                  - DELETE
                  - PATCH
                  - OPTIONS
                allowedHeaders: "*"
                allowCredentials: true
                maxAge: 3600
```

Mount as volume in gateway deployment:
```yaml
volumes:
- name: config
  configMap:
    name: gateway-config
containers:
- name: gateway-service
  volumeMounts:
  - name: config
    mountPath: /config
  env:
  - name: SPRING_CONFIG_ADDITIONAL_LOCATION
    value: file:/config/
```

### Phase 10: Deployment Workflow (Week 3)

#### 10.1 Development Flow
```
1. Developer pushes to `dev` branch
   ↓
2. GitHub Actions builds images
   - Tags: :dev-latest, :sha-{hash}
   ↓
3. ArgoCD detects change (auto-sync enabled)
   ↓
4. ArgoCD syncs dev overlay
   ↓
5. New pods deployed automatically
   ↓
6. Developer verifies in dev environment
```

#### 10.2 Production Flow
```
1. Developer creates PR: dev → main
   ↓
2. Code review + approval
   ↓
3. Merge to main branch
   ↓
4. GitHub Actions builds images
   - Tags: :latest, :sha-{hash}, :v{build-number}
   - Updates production kustomization with new image tag
   ↓
5. ArgoCD detects change (manual sync required)
   ↓
6. DevOps reviews changes in ArgoCD UI
   ↓
7. Manual sync triggered via ArgoCD
   - Option: Progressive rollout with Argo Rollouts
   ↓
8. ArgoCD deploys to production
   ↓
9. Health checks validate deployment
   ↓
10. If failure: Auto-rollback or manual rollback
```

#### 10.3 Rollback Flow
```
1. Issue detected (alerts, monitoring, user reports)
   ↓
2. Assess severity
   ↓
3a. Critical: Immediate rollback
    argocd app rollback autonova-production <PREV_REVISION>
   ↓
3b. Non-critical: Investigate and fix forward
   ↓
4. Verify rollback success
   - Check pod status
   - Review logs
   - Test critical paths
   ↓
5. Post-mortem and fix in dev
```

## Testing Strategy

### Pre-deployment Checklist

- [ ] All services have health check endpoints (`/actuator/health`)
- [ ] Database connectivity verified with Neon PostgreSQL
- [ ] RabbitMQ message flows tested
- [ ] JWT authentication working across services
- [ ] OAuth2 Google login functional
- [ ] Stripe webhook endpoint accessible (use Stripe CLI for testing)
- [ ] Email sending configured (Gmail SMTP)
- [ ] All secrets migrated to GCP Secret Manager
- [ ] SSL/TLS certificates generated for domain
- [ ] DNS records pointing to ingress IP
- [ ] Resource limits tuned based on load testing
- [ ] HPA thresholds validated
- [ ] Backup strategy documented
- [ ] Monitoring dashboards configured
- [ ] Alert rules tested
- [ ] Rollback procedure tested in dev

### Load Testing
```bash
# Install k6 for load testing
brew install k6  # or appropriate package manager

# Test gateway endpoint
k6 run --vus 100 --duration 30s load-test.js
```

### Smoke Tests Post-Deployment
```bash
# Health checks
kubectl get pods -n autonova
kubectl top pods -n autonova

# Service accessibility
curl -k https://api.autonova.example.com/actuator/health
curl -k https://autonova.example.com

# ArgoCD sync status
argocd app get autonova-production

# Logs
kubectl logs -n autonova deployment/gateway-service --tail=100
```

## Cost Optimization

### GKE Cluster
- **Autopilot mode** for hands-off management (alternative to standard cluster)
- **Preemptible nodes** for dev environment (70% cost savings)
- **Cluster autoscaling** to scale down during off-hours

### Storage
- **Standard persistent disks** (not SSD) for RabbitMQ data
- **Object lifecycle policies** for logs and backups

### Networking
- **Shared VPC** if multiple projects exist
- **Cloud CDN** for frontend static assets

### Monitoring
- **GCP Cloud Logging/Monitoring** (native, no extra charges) vs Prometheus/Grafana (compute costs)

## Security Hardening

### Network Policies
```yaml
# k8s/overlays/production/security/network-policy.yaml
apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
metadata:
  name: deny-all-ingress
  namespace: autonova
spec:
  podSelector: {}
  policyTypes:
  - Ingress
---
apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
metadata:
  name: allow-gateway-to-services
  namespace: autonova
spec:
  podSelector:
    matchLabels:
      tier: backend
  policyTypes:
  - Ingress
  ingress:
  - from:
    - podSelector:
        matchLabels:
          app: gateway-service
    ports:
    - protocol: TCP
      port: 8080
```

### Pod Security Standards
```yaml
# k8s/overlays/production/security/pod-security.yaml
apiVersion: policy/v1beta1
kind: PodSecurityPolicy
metadata:
  name: restricted
spec:
  privileged: false
  allowPrivilegeEscalation: false
  requiredDropCapabilities:
    - ALL
  runAsUser:
    rule: MustRunAsNonRoot
  seLinux:
    rule: RunAsAny
  fsGroup:
    rule: RunAsAny
  volumes:
    - 'configMap'
    - 'emptyDir'
    - 'projected'
    - 'secret'
    - 'downwardAPI'
    - 'persistentVolumeClaim'
```

### Image Scanning (GitHub Actions)
```yaml
# Add to .github/workflows/backend-ci.yml
- name: Scan Docker image
  uses: aquasecurity/trivy-action@master
  with:
    image-ref: jalinahirushan02/${{ matrix.service }}:${{ github.sha }}
    format: 'sarif'
    output: 'trivy-results.sarif'

- name: Upload scan results
  uses: github/codeql-action/upload-sarif@v2
  with:
    sarif_file: 'trivy-results.sarif'
```

## Disaster Recovery

### Backup Strategy
1. **Database**: Neon PostgreSQL automatic backups (managed by Neon)
2. **RabbitMQ**: Periodic PVC snapshots
3. **Secrets**: GCP Secret Manager versioning
4. **Configurations**: Git repository (infrastructure as code)

### Recovery Procedure
```bash
# 1. Restore GKE cluster
gcloud container clusters create autonova-cluster-recovery \
  --region us-central1 \
  --cluster-version latest

# 2. Deploy infrastructure
kubectl apply -k infra/k8s/overlays/production

# 3. Restore secrets (already in GCP Secret Manager)
# External Secrets Operator will sync automatically

# 4. Restore RabbitMQ data (from PVC snapshot)
gcloud compute disks create rabbitmq-data-restore \
  --source-snapshot rabbitmq-data-snapshot-20250116

# 5. Point DNS to new cluster
# Update DNS A records to new ingress IP

# 6. Verify all services
argocd app sync autonova-production
kubectl get pods -n autonova -w
```

## Timeline Summary

| Week | Phase | Deliverables |
|------|-------|-------------|
| 1 | Foundation | GKE cluster, ArgoCD, Ingress, cert-manager |
| 1-2 | K8s Manifests | Complete all service deployments, production overlay |
| 2 | Secrets & Config | GCP Secret Manager integration, gateway CORS updates |
| 2-3 | GitOps | ArgoCD applications, sync policies, rollback mechanisms |
| 3 | Observability | Prometheus, Grafana, alerting |
| 3-4 | Testing | Load tests, security scanning, smoke tests |
| 4 | Production Go-Live | Deploy to production, monitor, iterate |

## Success Criteria

- [ ] All 13 microservices running in GKE
- [ ] Zero-downtime deployments via rolling updates
- [ ] TLS enabled for all ingress traffic
- [ ] Secrets managed via GCP Secret Manager
- [ ] ArgoCD sync successful for both dev and production
- [ ] Rollback capability tested and documented
- [ ] Monitoring dashboards showing all services
- [ ] Alert notifications working (Slack/Email)
- [ ] Load testing passed (target: 1000 concurrent users)
- [ ] Security scanning integrated in CI/CD
- [ ] Documentation complete for operations team

## Next Steps

1. **Create GCP project** and enable billing
2. **Provision GKE cluster** with Workload Identity
3. **Install core infrastructure** (ArgoCD, Ingress, cert-manager, External Secrets)
4. **Complete missing K8s manifests** for 4 services
5. **Create production overlay** with proper resource limits and HPA
6. **Migrate secrets** to GCP Secret Manager
7. **Configure ArgoCD applications** for dev and production
8. **Update CI/CD pipeline** to trigger ArgoCD syncs
9. **Deploy to dev environment** and validate
10. **Perform load testing** and tune resources
11. **Deploy to production** with manual approval
12. **Set up monitoring** and alerting
13. **Document runbooks** for operations team

## Questions to Resolve

1. **Domain name**: What will be the production domain? (e.g., autonova.example.com)
2. **GCP project**: Existing or new project? Project ID?
3. **Alert destinations**: Slack webhook or email for alerts?
4. **Budget constraints**: What's the monthly budget for GKE infrastructure?
5. **Neon PostgreSQL**: Current connection string and SSL certificate setup?
6. **Stripe webhooks**: Will webhook URL change to production domain?
7. **OAuth2 redirect URIs**: Need to update Google OAuth2 console with production URLs
8. **Email sending limits**: Gmail SMTP limits (consider SendGrid/Mailgun for production?)
9. **Rollout schedule**: When to deploy to production? Maintenance window?
10. **Team access**: Who needs ArgoCD and GKE cluster access?

## References

- [ArgoCD Documentation](https://argo-cd.readthedocs.io/)
- [Kustomize Documentation](https://kustomize.io/)
- [GKE Best Practices](https://cloud.google.com/kubernetes-engine/docs/best-practices)
- [External Secrets Operator](https://external-secrets.io/)
- [Spring Boot Actuator Endpoints](https://docs.spring.io/spring-boot/docs/current/reference/html/actuator.html)
- [Argo Rollouts for Progressive Delivery](https://argoproj.github.io/argo-rollouts/)
