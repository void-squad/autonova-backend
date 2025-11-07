# Docker Setup Guide - Employee Dashboard BFF Service

## Building the Docker Image

### Basic Build
```bash
docker build -t employee-dashboard-bff:latest .
```

### Build with Custom Tag
```bash
docker build -t employee-dashboard-bff:1.0.0 .
```

## Running the Container

### Using Docker Run

#### Development Mode (with environment variables)
```bash
docker run -d \
  --name employee-dashboard-bff \
  -p 8084:8084 \
  -e JWT_SECRET=your-256-bit-secret-key-here-change-this-in-production \
  -e CUSTOMER_SERVICE_URL=http://customer-service:8081 \
  -e PROJECT_SERVICE_URL=http://project-service:8082 \
  -e PROGRESS_MONITORING_SERVICE_URL=http://progress-monitoring-service:8083 \
  -e PAYMENTS_BILLING_SERVICE_URL=http://payments-billing-service:8085 \
  -e SPRING_PROFILES_ACTIVE=docker \
  employee-dashboard-bff:latest
```

#### Production Mode (with resource limits)
```bash
docker run -d \
  --name employee-dashboard-bff \
  -p 8084:8084 \
  --memory="512m" \
  --cpus="1.0" \
  -e JWT_SECRET=${JWT_SECRET} \
  -e CUSTOMER_SERVICE_URL=${CUSTOMER_SERVICE_URL} \
  -e PROJECT_SERVICE_URL=${PROJECT_SERVICE_URL} \
  -e PROGRESS_MONITORING_SERVICE_URL=${PROGRESS_MONITORING_SERVICE_URL} \
  -e PAYMENTS_BILLING_SERVICE_URL=${PAYMENTS_BILLING_SERVICE_URL} \
  -e SPRING_PROFILES_ACTIVE=production \
  --restart unless-stopped \
  employee-dashboard-bff:latest
```

### Using Docker Compose

#### Start Service
```bash
docker-compose -f docker-compose.test.yml up -d
```

#### View Logs
```bash
docker-compose -f docker-compose.test.yml logs -f employee-dashboard-service
```

#### Stop Service
```bash
docker-compose -f docker-compose.test.yml down
```

#### Rebuild and Start
```bash
docker-compose -f docker-compose.test.yml up -d --build
```

## Container Management

### Check Container Status
```bash
docker ps | grep employee-dashboard-bff
```

### View Container Logs
```bash
docker logs employee-dashboard-bff
```

### Follow Logs in Real-time
```bash
docker logs -f employee-dashboard-bff
```

### Execute Commands Inside Container
```bash
docker exec -it employee-dashboard-bff sh
```

### Check Container Resource Usage
```bash
docker stats employee-dashboard-bff
```

### Inspect Container
```bash
docker inspect employee-dashboard-bff
```

## Health Checks

### Check Health Status
```bash
docker inspect --format='{{json .State.Health}}' employee-dashboard-bff | jq
```

### Manual Health Check
```bash
curl http://localhost:8084/actuator/health
```

## Environment Variables

### Required Variables
- `JWT_SECRET` - Secret key for JWT token validation (must match auth-service)
- `JWT_EXPIRATION` - Token expiration time in milliseconds (default: 86400000 = 24 hours)

### Optional Service URLs (defaults to localhost if not set)
- `CUSTOMER_SERVICE_URL` - Customer service endpoint
- `PROJECT_SERVICE_URL` - Project service endpoint
- `PROGRESS_MONITORING_SERVICE_URL` - Progress monitoring service endpoint
- `PAYMENTS_BILLING_SERVICE_URL` - Payments & billing service endpoint
- `NOTIFICATION_SERVICE_URL` - Notification service endpoint
- `TIME_LOGGING_SERVICE_URL` - Time logging service endpoint
- `APPOINTMENT_BOOKING_SERVICE_URL` - Appointment booking service endpoint

### Spring Boot Configuration
- `SPRING_PROFILES_ACTIVE` - Active Spring profile (default, docker, production)
- `JAVA_OPTS` - JVM options for tuning (set automatically, can override)

## Integration with Main Docker Compose

To integrate with the main `infra/docker-compose.yml`:

```yaml
  employee-dashboard-service:
    build:
      context: ../employee-dashboard-service
      dockerfile: Dockerfile
    container_name: employee-dashboard-bff
    ports:
      - "8084:8084"
    environment:
      - JWT_SECRET=${JWT_SECRET}
      - CUSTOMER_SERVICE_URL=http://customer-service:8081
      - PROJECT_SERVICE_URL=http://project-service:8082
      - PROGRESS_MONITORING_SERVICE_URL=http://progress-monitoring-service:8083
      - PAYMENTS_BILLING_SERVICE_URL=http://payments-billing-service:8085
      - NOTIFICATION_SERVICE_URL=http://notification-service:8086
      - TIME_LOGGING_SERVICE_URL=http://time-logging-service:8087
      - APPOINTMENT_BOOKING_SERVICE_URL=http://appointment-booking-service:8088
      - SPRING_PROFILES_ACTIVE=docker
    depends_on:
      auth-service:
        condition: service_healthy
      discovery-service:
        condition: service_healthy
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8084/actuator/health"]
      interval: 30s
      timeout: 10s
      retries: 3
      start_period: 60s
    networks:
      - autonova-network
```

## Troubleshooting

### Container Won't Start
1. Check logs: `docker logs employee-dashboard-bff`
2. Verify environment variables are set
3. Ensure port 8084 is not already in use
4. Check if JWT_SECRET is properly configured

### Health Check Failing
1. Check if the application is starting: `docker logs employee-dashboard-bff`
2. Verify health endpoint: `curl http://localhost:8084/actuator/health`
3. Increase `start_period` in health check if needed
4. Check memory and CPU limits

### Service Communication Issues
1. Verify service URLs are correct
2. Check if services are on the same Docker network
3. Test connectivity: `docker exec employee-dashboard-bff curl http://customer-service:8081/actuator/health`
4. Review logs for connection errors

### Performance Issues
1. Check resource usage: `docker stats employee-dashboard-bff`
2. Adjust JAVA_OPTS if needed
3. Increase memory limit: `--memory="1g"`
4. Monitor with: `docker exec employee-dashboard-bff jcmd 1 VM.flags`

## Testing the Container

### Quick Test
```bash
# Build
docker build -t employee-dashboard-bff:test .

# Run
docker run -d --name test-bff -p 8084:8084 \
  -e JWT_SECRET=test-secret-key-for-development-only employee-dashboard-bff:test

# Wait for startup
sleep 30

# Test health endpoint
curl http://localhost:8084/actuator/health

# Cleanup
docker stop test-bff && docker rm test-bff
```

### Integration Test with Auth Service
```bash
# Start both services
docker-compose -f docker-compose.test.yml up -d

# Wait for services to be ready
sleep 45

# Register a test employee (via auth-service)
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "test.employee",
    "email": "employee@test.com",
    "password": "Test123!@#",
    "role": "EMPLOYEE"
  }'

# Login to get token
TOKEN=$(curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "test.employee",
    "password": "Test123!@#"
  }' | jq -r '.token')

# Test BFF endpoint
curl -X GET http://localhost:8084/api/employee/dashboard \
  -H "Authorization: Bearer $TOKEN"

# Cleanup
docker-compose -f docker-compose.test.yml down
```

## Production Deployment

### Build for Production
```bash
docker build -t employee-dashboard-bff:1.0.0 -t employee-dashboard-bff:latest .
```

### Push to Registry (if using Docker Hub or private registry)
```bash
# Tag for registry
docker tag employee-dashboard-bff:latest your-registry/employee-dashboard-bff:1.0.0

# Push
docker push your-registry/employee-dashboard-bff:1.0.0
```

### Deploy with Production Settings
```bash
docker run -d \
  --name employee-dashboard-bff \
  -p 8084:8084 \
  --memory="1g" \
  --cpus="2.0" \
  --restart always \
  -e JWT_SECRET=${JWT_SECRET} \
  -e SPRING_PROFILES_ACTIVE=production \
  -e CUSTOMER_SERVICE_URL=${CUSTOMER_SERVICE_URL} \
  -e PROJECT_SERVICE_URL=${PROJECT_SERVICE_URL} \
  -e JAVA_OPTS="-Xms512m -Xmx1g -XX:+UseG1GC -XX:MaxGCPauseMillis=200" \
  employee-dashboard-bff:1.0.0
```

## Monitoring

### Prometheus Metrics (if enabled)
```bash
curl http://localhost:8084/actuator/prometheus
```

### Application Info
```bash
curl http://localhost:8084/actuator/info
```

### Environment Variables
```bash
curl http://localhost:8084/actuator/env
```

## Security Notes

1. **Always change the default JWT_SECRET in production**
2. **Use secrets management** (Docker secrets, Kubernetes secrets, etc.)
3. **Don't expose actuator endpoints** in production without authentication
4. **Run containers with resource limits** to prevent resource exhaustion
5. **Keep base images updated** for security patches
6. **Use non-root user** (already configured in Dockerfile)
