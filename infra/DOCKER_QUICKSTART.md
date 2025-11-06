# 🚀 Quick Start Guide - Running Auth Service with Docker

## ✅ Pre-Flight Checklist

Before running `docker-compose up`, ensure:

- [ ] **Docker Desktop is RUNNING**
  - Look for Docker icon in system tray
  - Should show "Docker Desktop is running" (green)
  - Test: `docker ps` should work without errors

- [ ] **Environment file configured**
  - File: `infra/.env` exists
  - Contains all required variables (see below)

- [ ] **In correct directory**
  - Current directory: `autonova-backend/infra/`
  - Command: `cd infra` if not already there

---

## 🎯 Step-by-Step Instructions

### **Step 1: Start Docker Desktop**

```powershell
# Check if Docker is running
docker ps

# If you see an error, start Docker Desktop from Windows Start Menu
# Wait 30-60 seconds for it to fully start
```

### **Step 2: Navigate to infra folder**

```powershell
cd C:\Users\"Lasitha Hasaranga"\OneDrive\Desktop\automobile-service-system\autonova-backend\infra
```

### **Step 3: Verify .env file**

```powershell
# Check if .env exists
Test-Path .env

# Should return: True
```

### **Step 4: Build and start services**

```powershell
# Build and start all services
docker-compose up --build -d

# Or start only auth-service
docker-compose up --build -d auth-service
```

### **Step 5: Verify services are running**

```powershell
# Check running containers
docker-compose ps

# Expected output:
# NAME                    STATUS              PORTS
# autonova-auth-service   Up (healthy)        0.0.0.0:8082->8081/tcp
# autonova-rabbitmq       Up (healthy)        0.0.0.0:5672->5672/tcp, 0.0.0.0:15672->15672/tcp
```

### **Step 6: Check logs**

```powershell
# View auth-service logs
docker-compose logs -f auth-service

# Should see:
# "Started AuthServiceApplication"
# "Tomcat started on port 8081"
```

### **Step 7: Test the service**

```powershell
# Test health endpoint
Invoke-WebRequest -Uri "http://localhost:8082/actuator/health"

# Expected response: {"status":"UP"}
```

---

## 🔧 Troubleshooting

### **Error: "unable to get image" or "pipe/dockerDesktopLinuxEngine"**

**Problem:** Docker Desktop is not running

**Solution:**
1. Open Docker Desktop from Windows Start Menu
2. Wait for "Docker Desktop is running" message
3. Try again: `docker ps`

---

### **Warning: "The PGHOST variable is not set"**

**Problem:** Harmless warning - variables are set inside container

**Solution:** Already fixed in `.env` file with:
```bash
PGHOST=${POSTGRES_HOST}
PGPORT=${POSTGRES_PORT}
```

---

### **Error: "network autonova-network not found"**

**Problem:** Docker network doesn't exist yet

**Solution:**
```powershell
docker network create autonova-network
```
Or just run `docker-compose up` - it will create automatically.

---

### **Error: "port is already allocated"**

**Problem:** Port 8082 is already in use

**Solution:**
```powershell
# Check what's using port 8082
netstat -ano | findstr :8082

# Stop the process or change the port in docker-compose.yml
```

---

### **Container exits immediately**

**Problem:** Check logs for errors

**Solution:**
```powershell
# View all logs
docker-compose logs

# View specific service
docker-compose logs auth-service

# View with timestamps
docker-compose logs -t auth-service
```

---

## 📊 Common Commands

```powershell
# Start services
docker-compose up -d

# Stop services
docker-compose stop

# Stop and remove containers
docker-compose down

# Rebuild specific service
docker-compose up --build -d auth-service

# View logs (follow mode)
docker-compose logs -f auth-service

# View all container status
docker-compose ps

# Restart a service
docker-compose restart auth-service

# Execute command in container
docker-compose exec auth-service sh

# Remove all containers and volumes
docker-compose down -v
```

---

## 🌐 Service URLs

Once running, access services at:

| Service | URL | Credentials |
|---------|-----|-------------|
| Auth Service API | http://localhost:8082 | - |
| Health Check | http://localhost:8082/actuator/health | - |
| RabbitMQ Management | http://localhost:15672 | guest / guest |
| Project Service | http://localhost:8081 | - |

---

## 📝 Environment Variables Required

Your `infra/.env` file should contain:

```bash
# Database
POSTGRES_HOST=...
POSTGRES_PORT=5432
POSTGRES_USER=...
POSTGRES_PASSWORD=...
PGDATABASE=...
USER_MANAGEMENT_DB_PASSWORD=...

# Auth Service
JWT_SECRET=...
EMAIL_USERNAME=...
EMAIL_PASSWORD=...
GOOGLE_CLIENT_ID=...
GOOGLE_CLIENT_SECRET=...
FRONTEND_URL=http://localhost:5173
```

---

## ✅ Success Indicators

You'll know everything is working when:

1. ✅ `docker-compose ps` shows "Up (healthy)" status
2. ✅ `docker-compose logs auth-service` shows "Started AuthServiceApplication"
3. ✅ Health endpoint returns: `{"status":"UP"}`
4. ✅ No error messages in logs
5. ✅ Can register/login via API

---

## 🎉 Next Steps

After services are running:

1. **Test API endpoints** using Postman or curl
2. **Check RabbitMQ management** at http://localhost:15672
3. **Connect frontend** to http://localhost:8082
4. **View logs** to monitor activity

---

**Last Updated:** 2025-11-04  
**Status:** ✅ Ready for Docker deployment
