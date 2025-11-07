# Employee Dashboard Service - BFF (Backend For Frontend)# Employee Dashboard Service



**Secure API aggregator for employee dashboard frontend**## Overview

The Employee Dashboard Service is a microservice that aggregates data from multiple services to provide a unified dashboard experience for employees. It supports both operational and analytical views, along with user preferences management.

[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.7-brightgreen.svg)](https://spring.io/projects/spring-boot)

[![Java](https://img.shields.io/badge/Java-17-orange.svg)](https://www.oracle.com/java/)## Features

[![Security](https://img.shields.io/badge/Security-JWT-blue.svg)](https://jwt.io/)

[![Architecture](https://img.shields.io/badge/Pattern-BFF-purple.svg)](https://samnewman.io/patterns/architectural/bff/)### 1. Operational View

**Endpoint:** `GET /api/dashboard/operational`

---

Aggregates real-time operational data from multiple services:

## 🎯 Overview- Active timer from Time Logging service

- Today's appointments from Appointment Booking service

This service implements the **Backend For Frontend (BFF)** pattern, providing a single, optimized API endpoint for the employee dashboard frontend. It aggregates data from multiple microservices and handles authentication/authorization.- Work queue from Service Tracking service



### Why BFF?**Response Example:**

```json

- ✅ **Single API Call** - Frontend makes one request instead of multiple{

- ✅ **Reduced Network Overhead** - Aggregation happens on the backend  "activeTimer": {

- ✅ **Frontend Optimized** - Response structure matches UI requirements exactly    "timerId": 123,

- ✅ **Centralized Security** - JWT validation in one place    "employeeId": 1,

- ✅ **Simplified Frontend** - No need to orchestrate multiple API calls    "jobId": "JOB-001",

- ✅ **Better Performance** - Server-side aggregation is faster than client-side    "status": "RUNNING",

    "startTime": "2025-11-01T09:00:00",

---    "elapsedSeconds": 3600

  },

## 🔐 Security  "todaysAppointments": [

    {

### JWT Authentication      "appointmentId": 456,

- **Required**: Valid JWT token from auth-service      "customerName": "John Doe",

- **Header**: `Authorization: Bearer <token>`      "appointmentTime": "2025-11-01T14:00:00",

- **Role**: Only `EMPLOYEE` role can access dashboard endpoints      "serviceType": "Oil Change",

- **Token Validation**: JWT signature and expiration checked      "status": "SCHEDULED"

    }

### Endpoint Security  ],

```java  "workQueue": [

GET /api/employee/dashboard    {

- Required Role: EMPLOYEE      "jobId": 789,

- Returns: Aggregated dashboard data      "jobType": "Repair",

```      "priority": "HIGH",

      "status": "IN_PROGRESS",

---      "assignedTo": "Employee 1",

      "deadline": "2025-11-02T17:00:00"

## 📋 API Documentation    }

  ]

### Get Employee Dashboard}

**Endpoint:** `GET /api/employee/dashboard````



**Headers:**### 2. Analytical View

```**Endpoint:** `GET /api/dashboard/analytics/summary`

Authorization: Bearer <jwt_token>

```Fetches analytics summary from the Analytics and Reporting service.



**Response:****Response:** Passes through the analytics service response directly.

```json

{### 3. Save Analytics Report

  "employeeInfo": {**Endpoint:** `POST /api/dashboard/analytics/save-report`

    "userId": 1,

    "name": "Employee User",Saves custom analytics report parameters for later retrieval.

    "email": "employee@autonova.com",

    "role": "EMPLOYEE",**Request Example:**

    "department": "Service Department"```json

  },{

  "stats": {  "reportName": "My Q3 Job Summary",

    "totalActiveProjects": 5,  "reportParameters": {

    "pendingAppointments": 3,    "dateRange": "Q3-2025",

    "completedTasksThisWeek": 12,    "jobType": "Repair",

    "totalRevenueThisMonth": 45000.00,    "status": "Completed"

    "totalCustomers": 28  }

  },}

  "recentActivities": [```

    {

      "id": "ACT-001",**Response Example:**

      "type": "PROJECT_UPDATE",```json

      "description": "Updated project PRJ-2024-001 progress to 65%",{

      "timestamp": "2025-11-07 14:30:00",  "reportId": 1,

      "status": "COMPLETED"  "employeeId": 1,

    }  "reportName": "My Q3 Job Summary",

  ],  "reportParameters": {

  "upcomingTasks": [    "dateRange": "Q3-2025",

    {    "jobType": "Repair",

      "id": "TASK-001",    "status": "Completed"

      "title": "Complete vehicle inspection",  },

      "description": "Inspect vehicle for project PRJ-2024-001",  "createdAt": "2025-11-01T10:30:00"

      "dueDate": "2025-11-09",}

      "priority": "HIGH",```

      "projectId": "PRJ-2024-001"

    }### 4. Get Saved Reports

  ],**Endpoint:** `GET /api/dashboard/analytics/saved-reports`

  "activeProjects": [

    {Retrieves all saved reports for the authenticated employee.

      "projectId": "PRJ-2024-001",

      "projectName": "Toyota Camry - Full Service",### 5. Employee Preferences

      "customerName": "John Doe",**Endpoint:** `GET /api/dashboard/preferences`

      "status": "IN_PROGRESS",

      "startDate": "2025-10-28",Retrieves employee's dashboard preferences.

      "expectedCompletionDate": "2025-11-12",

      "progressPercentage": 65**Response Example:**

    }```json

  ]{

}  "employeeId": 1,

```  "defaultView": "operational",

  "theme": "light"

**Status Codes:**}

- `200 OK` - Success```

- `401 Unauthorized` - Invalid or missing JWT token

- `403 Forbidden` - User is not an EMPLOYEE**Endpoint:** `PUT /api/dashboard/preferences`

- `500 Internal Server Error` - Server error

Updates employee's dashboard preferences.

---

**Request Example:**

## 🚀 Getting Started```json

{

### Prerequisites  "defaultView": "analytical",

- Java 17+  "theme": "dark"

- Maven 3.6+}

- Auth-service running (for JWT validation)```



### Configuration## Database Schema



**application.properties:**### EmployeePreferences Table

```properties```sql

spring.application.name=employee-dashboard-serviceemployee_id (Primary Key, Foreign Key to Authentication service)

server.port=8084default_view (String: "OPERATIONAL" or "ANALYTICAL")

theme (String: "DARK" or "LIGHT")

# JWT Configuration (must match auth-service)created_at (Timestamp)

jwt.secret=YXV0b25vdmEtc2VjcmV0LWtleS1mb3Itand0LWF1dGhlbnRpY2F0aW9uLXNlcnZpY2UtMjAyNQ==updated_at (Timestamp)

```

# External Service URLs

services.gateway.url=http://localhost:8080### SavedAnalyticsReports Table

services.customer.url=http://localhost:8085```sql

services.appointment.url=http://localhost:8086report_id (Primary Key, Auto-increment)

services.project.url=http://localhost:8087employee_id (Foreign Key to EmployeePreferences)

```report_name (String)

report_parameters (JSONB)

### Running the Servicecreated_at (Timestamp)

updated_at (Timestamp)

```bash```

# Clean and build

mvn clean install## Configuration



# Run### Application Properties

mvn spring-boot:runThe service requires the following configuration in `application.properties`:



# Or run JAR```properties

java -jar target/employee-dashboard-service-0.0.1-SNAPSHOT.jar# Server Configuration

```server.port=8084



---# Database Configuration

spring.datasource.url=jdbc:postgresql://[host]:[port]/employee_dashboard_db

## 🧪 Testingspring.datasource.username=[username]

spring.datasource.password=[password]

### Using cURL

# External Service URLs

**1. Get JWT Token from Auth Service:**services.time-logging.url=http://localhost:8081

```bashservices.appointment-booking.url=http://localhost:8082

curl -X POST http://localhost:8081/api/auth/login \services.service-tracking.url=http://localhost:8083

  -H "Content-Type: application/json" \services.analytics-reporting.url=http://localhost:8085

  -d '{```

    "email": "employee@autonova.com",

    "password": "password123"## Dependencies

  }'- Spring Boot 3.5.7

```- Spring Data JPA

- Spring Security

**2. Call Dashboard Endpoint:**- Spring WebFlux (for reactive WebClient)

```bash- PostgreSQL

curl -X GET http://localhost:8084/api/employee/dashboard \- Lombok

  -H "Authorization: Bearer <your_jwt_token>"

```## Running the Service



### Using Postman### Prerequisites

1. PostgreSQL database running

1. **Login to get token:**2. Other dependent services (Time Logging, Appointment Booking, Service Tracking, Analytics) running

   - Method: POST

   - URL: `http://localhost:8081/api/auth/login`### Build

   - Body (JSON):```bash

     ```json./mvnw clean install

     {```

       "email": "employee@autonova.com",

       "password": "password123"### Run

     }```bash

     ```./mvnw spring-boot:run

```

2. **Get Dashboard:**

   - Method: GET### Access

   - URL: `http://localhost:8084/api/employee/dashboard`- API Base URL: `http://localhost:8084/api/dashboard`

   - Headers:- Actuator Health: `http://localhost:8084/actuator/health`

     - Key: `Authorization`

     - Value: `Bearer <token_from_step_1>`## Security

The service uses Spring Security with stateless session management. All endpoints (except actuator) require authentication. The employee ID is extracted from the authentication token.

---

**Note:** The current implementation uses a placeholder for extracting employee ID from authentication. Update the `extractEmployeeId()` method in controllers based on your actual authentication service implementation.

## 🏗️ Architecture

## Error Handling

### Current State (Mock Data)The service includes global exception handling for:

```- Resource not found (404)

Frontend → Employee Dashboard BFF → Mock Data- Invalid arguments (400)

                ↓- External service errors (propagated status codes)

           JWT Validation- Generic errors (500)

           (Auth Service)

```## Testing

Run tests with:

### Future State (When Services Are Ready)```bash

```./mvnw test

Frontend → Employee Dashboard BFF → Project Service```

                ↓                 → Customer Service

           JWT Validation        → Appointment Service## Architecture

           (Auth Service)        → Payment ServiceThis service follows the microservices aggregator pattern:

                                 → Progress Monitoring Service- Acts as a gateway for frontend to access multiple backend services

```- Reduces frontend complexity by providing unified APIs

- Manages employee-specific data (preferences and saved reports)

### BFF Service Structure- Uses reactive WebClient for non-blocking inter-service communication

```

src/## Future Enhancements

├── config/- Add caching for frequently accessed data

│   ├── SecurityConfig.java          # JWT security configuration- Implement circuit breakers for resilience

│   └── WebClientConfig.java         # HTTP client for service calls- Add rate limiting

├── controller/- Implement JWT token validation

│   └── EmployeeDashboardBFFController.java  # Main API endpoint- Add comprehensive integration tests

├── dto/- Add API documentation with Swagger/OpenAPI

│   └── EmployeeDashboardResponse.java       # Response DTOs
├── security/
│   ├── JwtService.java              # JWT validation
│   └── JwtAuthenticationFilter.java # Request filter
└── service/
    └── EmployeeDashboardBFFService.java     # Data aggregation logic
```

---

## 🔄 Integration with Other Services

### When Services Become Available

The BFF service is designed to easily integrate with other microservices. Here's how to add real service calls:

**Example - Integrating Project Service:**

```java
@Service
public class EmployeeDashboardBFFService {
    
    private final WebClient.Builder webClientBuilder;
    
    @Value("${services.project.url}")
    private String projectServiceUrl;
    
    private List<ProjectSummary> getActiveProjects(Long userId) {
        WebClient webClient = webClientBuilder
                .baseUrl(projectServiceUrl)
                .build();
        
        return webClient.get()
                .uri("/api/projects/employee/{userId}", userId)
                .retrieve()
                .bodyToFlux(ProjectSummary.class)
                .collectList()
                .block();
    }
}
```

### Service Integration Checklist

- [ ] **Customer Service** - Get customer data
- [ ] **Appointment Service** - Get pending appointments
- [ ] **Project Service** - Get active projects
- [ ] **Payment Service** - Get revenue stats
- [ ] **Progress Monitoring** - Get task completion data

---

## 📊 Monitoring

### Health Check
```bash
curl http://localhost:8084/actuator/health
```

### Actuator Endpoints
- `/actuator/health` - Service health status
- `/actuator/info` - Service information
- `/actuator/metrics` - Performance metrics

---

## 🔧 Configuration Options

| Property | Default | Description |
|----------|---------|-------------|
| `server.port` | 8084 | Service port |
| `jwt.secret` | - | JWT signing key (must match auth-service) |
| `webclient.timeout.connection` | 5000 | Connection timeout (ms) |
| `webclient.timeout.read` | 10000 | Read timeout (ms) |
| `services.*.url` | - | URLs of downstream services |

---

## 🚨 Error Handling

| Status Code | Meaning | Resolution |
|-------------|---------|------------|
| 401 | Missing/Invalid JWT | Get new token from auth-service |
| 403 | Not an EMPLOYEE | Use employee credentials |
| 500 | Server Error | Check logs for details |

---

## 📝 Future Enhancements

1. ✅ **Mock Data** - Currently returns mock data
2. ⏳ **Real Service Integration** - Integrate when services are ready
3. ⏳ **Caching** - Add Redis for response caching
4. ⏳ **Rate Limiting** - Prevent API abuse
5. ⏳ **Parallel Service Calls** - Use CompletableFuture for better performance
6. ⏳ **Circuit Breaker** - Handle service failures gracefully
7. ⏳ **Request Tracing** - Add distributed tracing

---

## 🤝 Contributing

When implementing new service integrations:

1. Update the `EmployeeDashboardBFFService` class
2. Replace mock data with real service calls
3. Add error handling for service failures
4. Update this README with new endpoints
5. Add unit and integration tests

---

## 📖 Related Documentation

- [Auth Service README](../auth-service/README.md)
- [API Gateway Setup](../gateway-service/README.md)
- [Project Service README](../project-service/README.md)

---

## 📞 Support

For issues or questions, contact the development team or create an issue in the repository.

---

**Note:** This service currently returns mock data. As other microservices are implemented, the BFF will be updated to call real endpoints while maintaining the same API contract with the frontend.
