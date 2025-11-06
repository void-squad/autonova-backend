# 🐳 Docker Guide for Auth Service

## 📋 Overview

This guide covers how to build and run the auth-service using Docker. The Dockerfile is optimized for:
- **Small image size** (using Alpine Linux and multi-stage builds)
- **Fast builds** (leveraging Docker layer caching)
- **Security** (non-root user, minimal base image)
- **Production-ready** (health checks, proper signal handling)

---

## 🏗️ Dockerfile Features

### **Multi-Stage Build**
- **Stage 1 (Builder)**: Compiles the application using Maven
- **Stage 2 (Runtime)**: Runs the application with minimal JRE

### **Optimizations**
- ✅ **Layer caching**: Dependencies downloaded separately from source code
- ✅ **Alpine Linux**: Minimal base image (~5MB vs ~100MB)
- ✅ **JRE only**: No development tools in runtime image
- ✅ **Non-root user**: Runs as user `spring` (UID 1001)
- ✅ **Health checks**: Built-in container health monitoring
- ✅ **Signal handling**: Uses `dumb-init` for proper shutdown
- ✅ **JVM tuning**: Container-aware memory settings

### **Security Features**
- 🔒 Non-root user execution
- 🔒 Minimal attack surface (Alpine + JRE only)
- 🔒 No sensitive files included (.dockerignore)
- 🔒 Health checks for reliability

---

## 🚀 Quick Start

### **Option 1: Docker Compose (Recommended for Development)**

```powershell
# Start both database and application
docker-compose up -d

# View logs
docker-compose logs -f auth-service

# Stop services
docker-compose down

# Stop and remove volumes (clean slate)
docker-compose down -v
```

### **Option 2: Docker Build + Run (Manual)**

```powershell
# 1. Build the Docker image
docker build -t autonova/auth-service:latest .

# 2. Run with environment variables
docker run -d \
  -p 8081:8081 \
  -e DB_URL="jdbc:postgresql://your-db-host:5432/user_management_db" \
  -e DB_USERNAME="postgres" \
  -e DB_PASSWORD="your_password" \
  -e JWT_SECRET="your_jwt_secret" \
  -e EMAIL_USERNAME="your-email@gmail.com" \
  -e EMAIL_PASSWORD="your_app_password" \
  -e GOOGLE_CLIENT_ID="your_client_id" \
  -e GOOGLE_CLIENT_SECRET="your_client_secret" \
  --name auth-service \
  autonova/auth-service:latest

# 3. View logs
docker logs -f auth-service

# 4. Stop container
docker stop auth-service

# 5. Remove container
docker rm auth-service
```

---

## 📝 Environment Variables

### **Required Variables**

| Variable | Description | Example |
|----------|-------------|---------|
| `DB_URL` | PostgreSQL connection URL | `jdbc:postgresql://localhost:5432/user_management_db` |
| `DB_USERNAME` | Database username | `postgres` |
| `DB_PASSWORD` | Database password | `your_password` |
| `JWT_SECRET` | JWT signing secret (256-bit) | `base64-encoded-secret` |
| `EMAIL_USERNAME` | Gmail username for sending emails | `your-email@gmail.com` |
| `EMAIL_PASSWORD` | Gmail app password | `your_app_password` |
| `GOOGLE_CLIENT_ID` | Google OAuth2 client ID | `your-client-id` |
| `GOOGLE_CLIENT_SECRET` | Google OAuth2 client secret | `your-client-secret` |

### **Optional Variables**

| Variable | Description | Default |
|----------|-------------|---------|
| `FRONTEND_URL` | Frontend application URL for CORS | `http://localhost:3000` |
| `SPRING_PROFILES_ACTIVE` | Active Spring profile | `default` |
| `JAVA_OPTS` | Additional JVM options | `-XX:+UseContainerSupport ...` |

---

## 🔧 Docker Compose Configuration

### **Using Environment File**

Create a `.env` file in the same directory as `docker-compose.yml`:

```bash
# .env file (NOT committed to Git)
EMAIL_USERNAME=your-email@gmail.com
EMAIL_PASSWORD=your_app_password
GOOGLE_CLIENT_ID=your-client-id
GOOGLE_CLIENT_SECRET=your-client-secret
FRONTEND_URL=http://localhost:3000
```

Then run:
```powershell
docker-compose up -d
```

Docker Compose will automatically load variables from `.env` file.

---

## 📦 Docker Image Details

### **Image Layers**

```
Base: eclipse-temurin:17-jre-alpine (~150MB)
├── dumb-init (~20KB)
├── Non-root user setup
├── Application JAR (~50MB)
└── Total: ~200MB (vs ~500MB with full JDK)
```

### **Image Tags**

```powershell
# Build with version tag
docker build -t autonova/auth-service:1.0.0 .
docker build -t autonova/auth-service:latest .

# Build with Git commit SHA
docker build -t autonova/auth-service:$(git rev-parse --short HEAD) .
```

---

## 🏥 Health Checks

### **Container Health Check**

The Dockerfile includes a health check that:
- Runs every 30 seconds
- Checks `/actuator/health` endpoint
- Retries 3 times before marking unhealthy
- Waits 60 seconds before first check

### **Check Container Health**

```powershell
# View health status
docker ps

# View detailed health logs
docker inspect --format='{{json .State.Health}}' auth-service | ConvertFrom-Json

# Wait for healthy status
docker-compose up -d
docker-compose ps  # Shows health status
```

---

## 🔍 Troubleshooting

### **1. Build Fails**

```powershell
# Clean build (no cache)
docker build --no-cache -t autonova/auth-service:latest .

# Check build logs
docker build -t autonova/auth-service:latest . --progress=plain
```

### **2. Container Won't Start**

```powershell
# View logs
docker logs auth-service

# Interactive shell (debug)
docker run -it --entrypoint sh autonova/auth-service:latest

# Check environment variables
docker exec auth-service env | grep DB_
```

### **3. Database Connection Issues**

```powershell
# Test database connectivity from container
docker exec auth-service ping postgres

# Check if database is ready
docker-compose logs postgres
```

### **4. Out of Memory**

```powershell
# Run with more memory
docker run -m 1g --memory-swap 1g \
  -e JAVA_OPTS="-Xmx512m -Xms256m" \
  autonova/auth-service:latest

# Or in docker-compose.yml:
# deploy:
#   resources:
#     limits:
#       memory: 1G
```

---

## 🚀 Production Deployment

### **1. Build Production Image**

```powershell
# Build optimized production image
docker build \
  --build-arg MAVEN_OPTS="-XX:+TieredCompilation -XX:TieredStopAtLevel=1" \
  -t autonova/auth-service:1.0.0 \
  -t autonova/auth-service:latest \
  .
```

### **2. Push to Container Registry**

```powershell
# Docker Hub
docker login
docker push autonova/auth-service:1.0.0
docker push autonova/auth-service:latest

# AWS ECR
aws ecr get-login-password --region us-east-1 | docker login --username AWS --password-stdin <account>.dkr.ecr.us-east-1.amazonaws.com
docker tag autonova/auth-service:latest <account>.dkr.ecr.us-east-1.amazonaws.com/auth-service:latest
docker push <account>.dkr.ecr.us-east-1.amazonaws.com/auth-service:latest
```

### **3. Deploy to Production**

**Kubernetes Example:**

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: auth-service
spec:
  replicas: 3
  selector:
    matchLabels:
      app: auth-service
  template:
    metadata:
      labels:
        app: auth-service
    spec:
      containers:
      - name: auth-service
        image: autonova/auth-service:1.0.0
        ports:
        - containerPort: 8081
        env:
        - name: DB_URL
          valueFrom:
            secretKeyRef:
              name: auth-secrets
              key: db-url
        - name: DB_PASSWORD
          valueFrom:
            secretKeyRef:
              name: auth-secrets
              key: db-password
        resources:
          requests:
            memory: "512Mi"
            cpu: "500m"
          limits:
            memory: "1Gi"
            cpu: "1000m"
        livenessProbe:
          httpGet:
            path: /actuator/health
            port: 8081
          initialDelaySeconds: 60
          periodSeconds: 30
        readinessProbe:
          httpGet:
            path: /actuator/health
            port: 8081
          initialDelaySeconds: 30
          periodSeconds: 10
```

---

## 🔒 Security Best Practices

### **1. Never Include Secrets in Image**

❌ **DON'T:**
```dockerfile
ENV DB_PASSWORD=mypassword
```

✅ **DO:**
```dockerfile
# Pass at runtime
docker run -e DB_PASSWORD=mypassword ...
```

### **2. Use .dockerignore**

The `.dockerignore` file prevents sensitive files from being copied:
- `application-local.properties`
- `.env` files
- Private keys (`.key`, `.pem`)
- Git history

### **3. Scan for Vulnerabilities**

```powershell
# Scan image for vulnerabilities
docker scan autonova/auth-service:latest

# Or use Trivy
trivy image autonova/auth-service:latest
```

### **4. Use Secrets Management**

- **Docker Secrets** (Docker Swarm)
- **Kubernetes Secrets**
- **AWS Secrets Manager**
- **Azure Key Vault**
- **HashiCorp Vault**

---

## 📊 Monitoring

### **1. View Logs**

```powershell
# Follow logs
docker logs -f auth-service

# Last 100 lines
docker logs --tail 100 auth-service

# Since timestamp
docker logs --since 2025-11-04T00:00:00 auth-service
```

### **2. Resource Usage**

```powershell
# Container stats
docker stats auth-service

# All containers
docker stats
```

### **3. Exec into Container**

```powershell
# Interactive shell
docker exec -it auth-service sh

# Run command
docker exec auth-service cat /app/logs/application.log
```

---

## 🧹 Cleanup

```powershell
# Stop and remove container
docker-compose down

# Remove volumes (WARNING: deletes database data)
docker-compose down -v

# Remove images
docker rmi autonova/auth-service:latest

# Remove unused images
docker image prune -a

# Clean everything (including volumes)
docker system prune -a --volumes
```

---

## 📚 Additional Resources

- [Spring Boot Docker Guide](https://spring.io/guides/topicals/spring-boot-docker/)
- [Docker Best Practices](https://docs.docker.com/develop/dev-best-practices/)
- [Multi-stage Builds](https://docs.docker.com/build/building/multi-stage/)
- [Health Checks](https://docs.docker.com/engine/reference/builder/#healthcheck)

---

## 🎯 Quick Reference

```powershell
# Build
docker build -t autonova/auth-service:latest .

# Run
docker-compose up -d

# Logs
docker-compose logs -f

# Stop
docker-compose down

# Rebuild
docker-compose up -d --build

# Clean
docker-compose down -v
docker system prune -a
```

---

**Status:** ✅ **Production-Ready**  
**Last Updated:** 2024-11-04  
**Docker Version:** 20.10+  
**Image Size:** ~200MB
