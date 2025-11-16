# Employee Dashboard BFF - Quick Start Guide

## 🚀 What is This?

This is a **Backend For Frontend (BFF)** service that provides a single secure API endpoint for the employee dashboard. It aggregates data from multiple microservices and is secured with JWT authentication requiring EMPLOYEE role.

## ⚡ Quick Start

### Prerequisites
- Java 17+
- Maven 3.9+
- Docker (optional)
- Auth service running on port 8080

### Running Locally

```bash
# 1. Set JWT secret (must match auth-service)
export JWT_SECRET="your-256-bit-secret-key-here-change-this-in-production"

# 2. Run the service
./mvnw spring-boot:run

# Service will start on http://localhost:8084
```

### Running with Docker

```bash
# Build image
docker build -t employee-dashboard-bff:latest .

# Run container
docker run -d \
  --name employee-dashboard-bff \
  -p 8084:8084 \
  -e JWT_SECRET=your-256-bit-secret-key-here-change-this-in-production \
  employee-dashboard-bff:latest

# Check logs
docker logs -f employee-dashboard-bff
```

## 🔑 Getting a JWT Token

### 1. Register an Employee
```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "john.employee",
    "email": "john@company.com",
    "password": "SecurePass123!",
    "role": "EMPLOYEE"
  }'
```

### 2. Login
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "john.employee",
    "password": "SecurePass123!"
  }'
```

Save the `token` from the response.

## 📡 Using the BFF Endpoint

### Get Dashboard Data
```bash
curl -X GET http://localhost:8084/api/employee/dashboard \
  -H "Authorization: Bearer YOUR_TOKEN_HERE"
```

### Example Response
```json
{
  "userId": 1,
  "username": "john.employee",
  "role": "EMPLOYEE",
  "customerMetrics": {
    "totalCustomers": 156,
    "activeProjects": 23,
    "pendingRequests": 8
  },
  "activeProjects": [
    {
      "id": 1,
      "name": "Vehicle Service - Toyota Camry",
      "status": "IN_PROGRESS",
      "progress": 65,
      "dueDate": "2025-11-15",
      "assignedTo": "John Doe"
    }
  ],
  "recentActivities": [...],
  "pendingTasksCount": 5,
  "teamStats": {...},
  "notifications": {...}
}
```

## 🔒 Security

### Required
- Valid JWT token in `Authorization: Bearer <token>` header
- User must have `EMPLOYEE` role
- Token must not be expired

### Common Errors

#### 403 Forbidden
```json
{
  "error": "Access Denied",
  "message": "Insufficient permissions"
}
```
**Solution**: User must have EMPLOYEE role

#### 401 Unauthorized
```json
{
  "error": "Unauthorized",
  "message": "Invalid or expired token"
}
```
**Solution**: Get a new token or check if JWT_SECRET matches

## 🏗️ Current State

### Mock Data ✅
Currently returns **mock data** since backend services aren't implemented yet. This allows:
- Frontend development to proceed
- API contract testing
- Security flow validation

### When Services Are Ready
Simply update the client services to call real endpoints instead of returning mocks. No frontend changes needed!

## 🔧 Configuration

### Environment Variables

| Variable | Required | Default | Description |
|----------|----------|---------|-------------|
| `JWT_SECRET` | ✅ Yes | - | Must match auth-service |
| `JWT_EXPIRATION` | No | 86400000 | Token expiration (24h) |
| `CUSTOMER_SERVICE_URL` | No | http://localhost:8081 | Customer service endpoint |
| `PROJECT_SERVICE_URL` | No | http://localhost:8082 | Project service endpoint |
| `PROGRESS_MONITORING_SERVICE_URL` | No | http://localhost:8083 | Progress service endpoint |
| `PAYMENTS_BILLING_SERVICE_URL` | No | http://localhost:8069 | Payments service endpoint |

## 🩺 Health Check

```bash
curl http://localhost:8084/actuator/health
```

Expected response:
```json
{
  "status": "UP"
}
```

## 📚 Documentation

- **README.md** - Complete service documentation
- **API_TESTING.md** - Detailed testing guide with examples
- **DOCKER_GUIDE.md** - Comprehensive Docker instructions
- **BFF_IMPLEMENTATION_SUMMARY.md** - Implementation details

## 🐛 Troubleshooting

### Service won't start
```bash
# Check if port 8084 is in use
lsof -i :8084

# Check logs
./mvnw spring-boot:run --debug
```

### JWT validation fails
1. Ensure JWT_SECRET matches between services
2. Check token expiration date
3. Verify token format: `Bearer <token>`, not just `<token>`

### Docker container unhealthy
```bash
# Check container logs
docker logs employee-dashboard-bff

# Exec into container
docker exec -it employee-dashboard-bff bash

# Test health endpoint from inside container
curl http://localhost:8084/actuator/health
```

## 🎯 Testing Checklist

- [ ] Service starts successfully
- [ ] Health endpoint responds
- [ ] Can get JWT token from auth-service
- [ ] Dashboard endpoint requires authentication
- [ ] Non-EMPLOYEE roles get 403
- [ ] EMPLOYEE role gets dashboard data
- [ ] Mock data structure is correct
- [ ] CORS works for frontend

## 🔄 Next Steps

1. **Frontend Integration**: Use this endpoint in your React/Angular app
2. **Service Integration**: When services are ready, update client services
3. **Caching**: Add Redis for response caching
4. **Monitoring**: Set up Prometheus + Grafana
5. **Production**: Add rate limiting, API keys, etc.

## 📞 Need Help?

Check these files for more information:
- Full documentation: `README.md`
- Testing guide: `API_TESTING.md`
- Docker deployment: `DOCKER_GUIDE.md`
- Implementation details: `BFF_IMPLEMENTATION_SUMMARY.md`

## ✅ Success!

If you can:
1. Start the service ✅
2. Get a JWT token ✅
3. Call the dashboard endpoint ✅
4. See mock data returned ✅

**You're all set! 🎉** The BFF is ready for frontend integration.
