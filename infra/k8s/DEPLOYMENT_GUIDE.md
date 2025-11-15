# Kubernetes Deployment Guide for AutoNova

This guide provides comprehensive instructions for deploying the AutoNova microservices system to Kubernetes.

## 📋 Overview

The K8s manifests have been updated with all services and proper configurations. All services now have correct environment variables pulled from secrets.

### Services Included

1. **discovery-service** (Port 8761) - Eureka Service Registry
2. **auth-service** (Port 8081) - Authentication & Authorization
3. **customer-service** (Port 8087) - Customer Management
4. **gateway-service** (Port 8080) - API Gateway
5. **project-service** (Port 8082) - Project Management (.NET)
6. **progress-monitoring** (Port 8086) - Progress Tracking
7. **notification-service** (Port 8085) - Notifications
8. **payments-billing-service** (Port 8069) - Payments & Billing
9. **time-logging-service** (Port 8083) - Time Tracking
10. **employee-dashboard-service** (Port 8084) - Employee BFF
11. **appointment-booking-service** (Port 8088) - Appointments
12. **chatbot** (Port 8097) - AI Chatbot
13. **frontend** (Port 5173) - React Frontend

### Infrastructure Components

- **PostgreSQL** - Cloud-hosted Neon database
- **RabbitMQ** - Message broker
- **Postgres Init Job** - Database initialization

## 🔐 Prerequisites

### 1. Kubernetes Cluster

Ensure you have a running Kubernetes cluster. Options include:
- Minikube (local development)
- Docker Desktop Kubernetes
- GKE, EKS, AKS (production)
- Kind (Kubernetes in Docker)

```bash
# Verify kubectl is configured
kubectl cluster-info
kubectl get nodes
```

### 2. Container Images

All services need to be built and pushed to the Docker registry:

```bash
# Example for auth-service
cd auth-service
docker build -t jalinahirushan02/auth-service:dev-latest .
docker push jalinahirushan02/auth-service:dev-latest
```

Required images:
- `jalinahirushan02/auth-service:dev-latest`
- `jalinahirushan02/customer-service:dev-latest`
- `jalinahirushan02/gateway-service:dev-latest`
- `jalinahirushan02/discovery-service:dev-latest`
- `jalinahirushan02/project-service:dev-latest`
- `jalinahirushan02/progress-monitoring-service:dev-latest`
- `jalinahirushan02/notification-service:dev-latest`
- `jalinahirushan02/payments-billing-service:dev-latest`
- `jalinahirushan02/time-logging-service:dev-latest`
- `jalinahirushan02/employee-dashboard-service:dev-latest`
- `jalinahirushan02/appointment-booking-service:dev-latest`
- `jalinahirushan02/chatbot:dev-latest`
- `sachinthalk/autonova-frontend-v1:dev-latest`

### 3. Database Setup

The system uses a cloud-hosted PostgreSQL database (Neon). Ensure:
- PostgreSQL instance is accessible
- All required databases are created
- Service users have proper permissions

Required databases:
- `user_management_db`
- `customer_service`
- `projects_db`
- `progress`
- `notifications_db`
- `payments_billing_db`
- `time_logging_db`
- `vector_db` (for chatbot)

## 🚀 Deployment Steps

### Step 1: Configure Secrets

1. Navigate to the secrets directory:
```bash
cd infra/k8s/secrets
```

2. Copy the example environment file:
```bash
cp .env.example .env
```

3. Edit `.env` and fill in all the actual values:

```bash
# CRITICAL: Update these values with your actual credentials
vim .env
```

Key values to update:
- **POSTGRES_PASSWORD** - Your Neon database password
- **USER_MANAGEMENT_DB_PASSWORD** - Auth service DB password
- **CUSTOMER_SERVICE_DB_PASSWORD** - Customer service DB password
- **PROJECTS_DB_PASSWORD** - Project service DB password
- **PROGRESS_MONITORING_DB_PASSWORD** - Progress monitoring DB password
- **NOTIFICATION_DB_PASSWORD** - Notification service DB password
- **PBS_DB_PASSWORD** - Payments/billing DB password
- **TIME_LOGGING_DB_PASSWORD** - Time logging DB password
- **VECTOR_DB_PASSWORD** - Chatbot vector DB password
- **JWT_SECRET** - JWT signing key (keep the default or change)
- **EMAIL_USERNAME** - Gmail address for sending emails
- **EMAIL_PASSWORD** - Gmail app password (not regular password)
- **GOOGLE_CLIENT_ID** - Google OAuth client ID
- **GOOGLE_CLIENT_SECRET** - Google OAuth client secret
- **STRIPE_API_KEY** - Stripe API key
- **STRIPE_WEBHOOK_SECRET** - Stripe webhook secret
- **GEMINI_API_KEY** - Google Gemini AI API key

### Step 2: Create Kubernetes Secret

Run the provided script to create the secret:

```bash
cd infra/k8s/secrets
./create-secrets-from-env.sh autonova-env autonova
```

Verify the secret was created:
```bash
kubectl get secret autonova-env -n autonova
kubectl describe secret autonova-env -n autonova
```

### Step 3: Deploy Infrastructure Components

First, deploy RabbitMQ and initialize the database:

```bash
cd infra/k8s

# Apply using kustomize
kubectl apply -k base/

# Or apply individually
kubectl apply -f base/namespace.yaml
kubectl apply -f base/rabbitmq/
kubectl apply -f base/postgres/
```

Wait for infrastructure to be ready:
```bash
# Watch the postgres-init job
kubectl get jobs -n autonova -w

# Check RabbitMQ
kubectl get pods -n autonova -l app=rabbitmq
```

### Step 4: Deploy Application Services

Deploy all services using kustomize:

```bash
# Deploy all services at once
kubectl apply -k base/

# Or use the dev overlay
kubectl apply -k overlays/dev/
```

### Step 5: Verify Deployment

Check the status of all pods:

```bash
kubectl get pods -n autonova
kubectl get services -n autonova
```

Check individual service logs:
```bash
# Example: Check auth-service logs
kubectl logs -n autonova -l app=auth-service -f

# Check discovery-service
kubectl logs -n autonova -l app=discovery-service -f
```

Wait for all services to be ready:
```bash
kubectl wait --for=condition=ready pod -l app.kubernetes.io/managed-by=kustomize -n autonova --timeout=300s
```

### Step 6: Access the Services

#### Port Forwarding (for local development)

Forward the gateway to access all services:
```bash
kubectl port-forward -n autonova svc/gateway-service 8080:8080
```

Forward the frontend:
```bash
kubectl port-forward -n autonova svc/web 5173:5173
```

Access services:
- Gateway: http://localhost:8080
- Frontend: http://localhost:5173
- Eureka Dashboard: http://localhost:8761 (if port-forwarded)
- RabbitMQ Management: http://localhost:15672 (if port-forwarded)

#### LoadBalancer / Ingress (for production)

For production deployments, configure an Ingress or LoadBalancer:

```yaml
# Example ingress.yaml
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: autonova-ingress
  namespace: autonova
spec:
  rules:
    - host: api.autonova.com
      http:
        paths:
          - path: /
            pathType: Prefix
            backend:
              service:
                name: gateway-service
                port:
                  number: 8080
    - host: app.autonova.com
      http:
        paths:
          - path: /
            pathType: Prefix
            backend:
              service:
                name: web
                port:
                  number: 5173
```

## 🔧 Configuration Management

### Environment Variables

All services pull configuration from the `autonova-env` secret. Key configurations:

#### Database URLs
- Services connect to cloud PostgreSQL (Neon)
- Connection strings are built from secret values
- SSL mode is set to `require` for cloud databases

#### Service Discovery
- All services register with Eureka at `http://discovery-service:8761/eureka/`
- Services use Kubernetes DNS for inter-service communication

#### Message Broker
- RabbitMQ runs in-cluster at `rabbitmq:5672`
- Exchange: `autonova.events`
- Each service has specific queues and routing keys

#### External Services
- Stripe for payments
- Google OAuth for authentication
- Gemini AI for chatbot
- Gmail SMTP for emails

### Updating Secrets

To update secrets after deployment:

```bash
# Method 1: Re-run the script
cd infra/k8s/secrets
vim .env  # Update values
./create-secrets-from-env.sh autonova-env autonova

# Restart pods to pick up new secrets
kubectl rollout restart deployment -n autonova

# Method 2: Edit directly (not recommended)
kubectl edit secret autonova-env -n autonova
```

## 📊 Monitoring & Troubleshooting

### Health Checks

Check service health:
```bash
# Get all pod statuses
kubectl get pods -n autonova

# Describe a specific pod
kubectl describe pod <pod-name> -n autonova

# Check readiness probe status
kubectl get events -n autonova --sort-by='.lastTimestamp'
```

### Logs

View logs for troubleshooting:
```bash
# Tail logs for a service
kubectl logs -n autonova -l app=auth-service -f --tail=100

# Get logs from all replicas
kubectl logs -n autonova -l app=gateway-service --all-containers=true

# Get previous logs (for crashed containers)
kubectl logs -n autonova <pod-name> --previous
```

### Common Issues

#### 1. Pods stuck in CrashLoopBackOff

**Symptoms:** Pods repeatedly restart
**Causes:** Database connection issues, missing secrets, wrong credentials

```bash
# Check pod logs
kubectl logs -n autonova <pod-name>

# Check secret exists
kubectl get secret autonova-env -n autonova

# Verify database connectivity
kubectl run -it --rm debug --image=postgres:17 --restart=Never -n autonova -- \
  psql -h <POSTGRES_HOST> -U <POSTGRES_USER> -d postgres
```

#### 2. ImagePullBackOff

**Symptoms:** Cannot pull container images
**Causes:** Images not pushed to registry, wrong image names/tags

```bash
# Check image pull status
kubectl describe pod <pod-name> -n autonova

# Verify image exists
docker pull jalinahirushan02/auth-service:dev-latest
```

#### 3. Service Not Responding

**Symptoms:** Service endpoints not accessible
**Causes:** Wrong ports, service not registered with Eureka, network issues

```bash
# Check service endpoints
kubectl get endpoints -n autonova

# Check if service registered with Eureka
kubectl port-forward -n autonova svc/discovery-service 8761:8761
# Then visit http://localhost:8761

# Test service connectivity
kubectl run -it --rm debug --image=curlimages/curl --restart=Never -n autonova -- \
  curl http://auth-service:8081/actuator/health
```

#### 4. Database Connection Errors

**Symptoms:** Services fail to connect to PostgreSQL
**Causes:** Wrong credentials, network issues, SSL mode mismatch

```bash
# Check database credentials in secret
kubectl get secret autonova-env -n autonova -o jsonpath='{.data.POSTGRES_PASSWORD}' | base64 -d

# Test database connection from within cluster
kubectl run -it --rm psql --image=postgres:17 --restart=Never -n autonova --env="PGPASSWORD=<password>" -- \
  psql -h <POSTGRES_HOST> -U <POSTGRES_USER> -d user_management_db
```

## 🔄 Updates & Rollbacks

### Rolling Updates

Update a service image:
```bash
# Update image tag in kustomization.yaml
cd infra/k8s/overlays/dev
vim patches/image-tags.yaml

# Apply changes
kubectl apply -k .

# Or set image directly
kubectl set image deployment/auth-service auth-service=jalinahirushan02/auth-service:v2.0.0 -n autonova

# Watch rollout status
kubectl rollout status deployment/auth-service -n autonova
```

### Rollback

Rollback to previous version:
```bash
# View rollout history
kubectl rollout history deployment/auth-service -n autonova

# Rollback to previous version
kubectl rollout undo deployment/auth-service -n autonova

# Rollback to specific revision
kubectl rollout undo deployment/auth-service --to-revision=2 -n autonova
```

## 🧹 Cleanup

To remove all deployed resources:

```bash
# Delete all resources
kubectl delete -k base/

# Or delete the namespace (removes everything)
kubectl delete namespace autonova

# Remove secrets
kubectl delete secret autonova-env -n autonova
```

## 📝 Service Dependencies

Startup order (enforced by depends_on in definitions):
1. Infrastructure: RabbitMQ, PostgreSQL Init Job
2. Discovery Service (Eureka)
3. Auth Service
4. Other Application Services (can start in parallel)
5. Gateway Service (routes to all services)
6. Frontend

## 🔗 Useful Commands

```bash
# Get all resources in namespace
kubectl get all -n autonova

# Scale a deployment
kubectl scale deployment auth-service --replicas=3 -n autonova

# Execute command in pod
kubectl exec -it <pod-name> -n autonova -- /bin/sh

# Copy files to/from pod
kubectl cp <local-file> autonova/<pod-name>:/path/in/container
kubectl cp autonova/<pod-name>:/path/in/container <local-file>

# View resource usage
kubectl top pods -n autonova
kubectl top nodes

# Get YAML of running resource
kubectl get deployment auth-service -n autonova -o yaml

# Edit resource in place
kubectl edit deployment auth-service -n autonova
```

## 📚 Additional Resources

- [Kubernetes Documentation](https://kubernetes.io/docs/)
- [Kustomize Documentation](https://kustomize.io/)
- [Spring Cloud Kubernetes](https://spring.io/projects/spring-cloud-kubernetes)
- [Eureka Documentation](https://github.com/Netflix/eureka/wiki)

## 🆘 Support

For issues or questions:
1. Check pod logs: `kubectl logs -n autonova <pod-name>`
2. Check events: `kubectl get events -n autonova`
3. Verify secrets: `kubectl describe secret autonova-env -n autonova`
4. Review application.yml/properties in each service

## 🎯 Production Considerations

Before deploying to production:

1. **Resource Limits**: Update resource requests/limits in overlays
2. **Horizontal Pod Autoscaling**: Configure HPA for services
3. **Persistent Volumes**: Use PVCs for RabbitMQ data
4. **TLS/SSL**: Configure TLS for all external endpoints
5. **Network Policies**: Implement network segmentation
6. **RBAC**: Configure proper role-based access control
7. **Monitoring**: Set up Prometheus + Grafana
8. **Logging**: Configure centralized logging (ELK, Loki)
9. **Backup**: Regular database and configuration backups
10. **Secrets Management**: Use external secret management (Vault, AWS Secrets Manager)
