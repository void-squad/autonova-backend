# Employee Dashboard Service

## Overview
The Employee Dashboard Service is a microservice that aggregates data from multiple services to provide a unified dashboard experience for employees. It supports both operational and analytical views, along with user preferences management.

## Features

### 1. Operational View
**Endpoint:** `GET /api/dashboard/operational`

Aggregates real-time operational data from multiple services:
- Active timer from Time Logging service
- Today's appointments from Appointment Booking service
- Work queue from Service Tracking service

**Response Example:**
```json
{
  "activeTimer": {
    "timerId": 123,
    "employeeId": 1,
    "jobId": "JOB-001",
    "status": "RUNNING",
    "startTime": "2025-11-01T09:00:00",
    "elapsedSeconds": 3600
  },
  "todaysAppointments": [
    {
      "appointmentId": 456,
      "customerName": "John Doe",
      "appointmentTime": "2025-11-01T14:00:00",
      "serviceType": "Oil Change",
      "status": "SCHEDULED"
    }
  ],
  "workQueue": [
    {
      "jobId": 789,
      "jobType": "Repair",
      "priority": "HIGH",
      "status": "IN_PROGRESS",
      "assignedTo": "Employee 1",
      "deadline": "2025-11-02T17:00:00"
    }
  ]
}
```

### 2. Analytical View
**Endpoint:** `GET /api/dashboard/analytics/summary`

Fetches analytics summary from the Analytics and Reporting service.

**Response:** Passes through the analytics service response directly.

### 3. Save Analytics Report
**Endpoint:** `POST /api/dashboard/analytics/save-report`

Saves custom analytics report parameters for later retrieval.

**Request Example:**
```json
{
  "reportName": "My Q3 Job Summary",
  "reportParameters": {
    "dateRange": "Q3-2025",
    "jobType": "Repair",
    "status": "Completed"
  }
}
```

**Response Example:**
```json
{
  "reportId": 1,
  "employeeId": 1,
  "reportName": "My Q3 Job Summary",
  "reportParameters": {
    "dateRange": "Q3-2025",
    "jobType": "Repair",
    "status": "Completed"
  },
  "createdAt": "2025-11-01T10:30:00"
}
```

### 4. Get Saved Reports
**Endpoint:** `GET /api/dashboard/analytics/saved-reports`

Retrieves all saved reports for the authenticated employee.

### 5. Employee Preferences
**Endpoint:** `GET /api/dashboard/preferences`

Retrieves employee's dashboard preferences.

**Response Example:**
```json
{
  "employeeId": 1,
  "defaultView": "operational",
  "theme": "light"
}
```

**Endpoint:** `PUT /api/dashboard/preferences`

Updates employee's dashboard preferences.

**Request Example:**
```json
{
  "defaultView": "analytical",
  "theme": "dark"
}
```

## Database Schema

### EmployeePreferences Table
```sql
employee_id (Primary Key, Foreign Key to Authentication service)
default_view (String: "OPERATIONAL" or "ANALYTICAL")
theme (String: "DARK" or "LIGHT")
created_at (Timestamp)
updated_at (Timestamp)
```

### SavedAnalyticsReports Table
```sql
report_id (Primary Key, Auto-increment)
employee_id (Foreign Key to EmployeePreferences)
report_name (String)
report_parameters (JSONB)
created_at (Timestamp)
updated_at (Timestamp)
```

## Configuration

### Application Properties
The service requires the following configuration in `application.properties`:

```properties
# Server Configuration
server.port=8084

# Database Configuration
spring.datasource.url=jdbc:postgresql://[host]:[port]/employee_dashboard_db
spring.datasource.username=[username]
spring.datasource.password=[password]

# External Service URLs
services.time-logging.url=http://localhost:8081
services.appointment-booking.url=http://localhost:8082
services.service-tracking.url=http://localhost:8083
services.analytics-reporting.url=http://localhost:8085
```

## Dependencies
- Spring Boot 3.5.7
- Spring Data JPA
- Spring Security
- Spring WebFlux (for reactive WebClient)
- PostgreSQL
- Lombok

## Running the Service

### Prerequisites
1. PostgreSQL database running
2. Other dependent services (Time Logging, Appointment Booking, Service Tracking, Analytics) running

### Build
```bash
./mvnw clean install
```

### Run
```bash
./mvnw spring-boot:run
```

### Access
- API Base URL: `http://localhost:8084/api/dashboard`
- Actuator Health: `http://localhost:8084/actuator/health`

## Security
The service uses Spring Security with stateless session management. All endpoints (except actuator) require authentication. The employee ID is extracted from the authentication token.

**Note:** The current implementation uses a placeholder for extracting employee ID from authentication. Update the `extractEmployeeId()` method in controllers based on your actual authentication service implementation.

## Error Handling
The service includes global exception handling for:
- Resource not found (404)
- Invalid arguments (400)
- External service errors (propagated status codes)
- Generic errors (500)

## Testing
Run tests with:
```bash
./mvnw test
```

## Architecture
This service follows the microservices aggregator pattern:
- Acts as a gateway for frontend to access multiple backend services
- Reduces frontend complexity by providing unified APIs
- Manages employee-specific data (preferences and saved reports)
- Uses reactive WebClient for non-blocking inter-service communication

## Future Enhancements
- Add caching for frequently accessed data
- Implement circuit breakers for resilience
- Add rate limiting
- Implement JWT token validation
- Add comprehensive integration tests
- Add API documentation with Swagger/OpenAPI
