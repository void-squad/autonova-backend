# Employee Dashboard Service

This microservice manages employee jobs and tasks for the Autonova automobile service system.

## Features

- View all jobs assigned to employees
- Track active jobs
- Start, pause, and stop jobs
- Update job status
- Track job timing and progress

## API Endpoints

### 1. Get All Jobs
```
GET /api/employee/jobs
Query Parameters: employeeId (optional, UUID)
```
Returns all jobs, optionally filtered by employee ID.

### 2. Get Active Jobs
```
GET /api/employee/jobs/active
Query Parameters: employeeId (optional, UUID)
```
Returns all jobs with ACTIVE status.

### 3. Start Job
```
POST /api/employee/jobs/{jobId}/start
Path Parameters: jobId (UUID)
```
Starts a job (changes status from PENDING or PAUSED to IN_PROGRESS).

### 4. Update Job Status
```
PATCH /api/employee/jobs/{jobId}/status
Path Parameters: jobId (UUID)
Request Body:
{
  "status": "COMPLETED",
  "notes": "Optional notes"
}
```
Updates the job status with a custom status value.

### 5. Pause Job
```
POST /api/employee/jobs/{jobId}/pause
Path Parameters: jobId (UUID)
```
Pauses a job (changes status from IN_PROGRESS to PAUSED).

### 6. Stop Job
```
POST /api/employee/jobs/{jobId}/stop
Path Parameters: jobId (UUID)
```
Stops a job (changes status to STOPPED).

### 7. Get Job by ID
```
GET /api/employee/jobs/{jobId}
Path Parameters: jobId (UUID)
```
Returns detailed information about a specific job.

## Job Status Values

- `PENDING` - Job is waiting to be started
- `ACTIVE` - Job is available for work
- `IN_PROGRESS` - Job is currently being worked on
- `PAUSED` - Job work has been temporarily paused
- `COMPLETED` - Job has been successfully completed
- `STOPPED` - Job has been stopped/cancelled

## Technology Stack

- **Framework**: Spring Boot 3.3.3
- **Database**: PostgreSQL
- **ORM**: Spring Data JPA / Hibernate
- **Service Discovery**: Eureka Client
- **Build Tool**: Maven

## Configuration

### Database Configuration
The service connects to PostgreSQL. Update `application.properties` with your database credentials:

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/employee_dashboard_db
spring.datasource.username=postgres
spring.datasource.password=postgres
```

### Server Port
The service runs on port `8084` by default.

### Eureka Configuration
The service registers with Eureka Discovery Server at `http://localhost:8761/eureka/`.

## Running the Service

### Prerequisites
1. Java 17 or higher
2. Maven 3.6+
3. PostgreSQL database
4. Eureka Discovery Service running on port 8761

### Steps

1. **Create the database**:
   ```bash
   psql -U postgres
   CREATE DATABASE employee_dashboard_db;
   ```

2. **Build the project**:
   ```bash
   mvn clean install
   ```

3. **Run the service**:
   ```bash
   ./mvnw spring-boot:run
   ```

   Or run from the JAR:
   ```bash
   java -jar target/employee-dashboard-service-0.0.1-SNAPSHOT.jar
   ```

4. **Verify the service**:
   - Health check: `http://localhost:8084/actuator/health`
   - Eureka dashboard: `http://localhost:8761`

## Testing the APIs

### Example: Get All Jobs
```bash
curl -X GET "http://localhost:8084/api/employee/jobs"
```

### Example: Start a Job
```bash
curl -X POST "http://localhost:8084/api/employee/jobs/{jobId}/start"
```

### Example: Update Job Status
```bash
curl -X PATCH "http://localhost:8084/api/employee/jobs/{jobId}/status" \
  -H "Content-Type: application/json" \
  -d '{"status": "COMPLETED", "notes": "Job finished successfully"}'
```

## Error Handling

The service includes comprehensive error handling with appropriate HTTP status codes:

- `200 OK` - Successful request
- `400 Bad Request` - Invalid request or job state
- `404 Not Found` - Job not found
- `500 Internal Server Error` - Server error

Error responses follow this format:
```json
{
  "timestamp": "2025-10-26T10:30:00",
  "status": 404,
  "error": "Not Found",
  "message": "Job not found with id: {jobId}"
}
```

## Development

### Project Structure
```
employee-dashboard-service/
├── src/
│   ├── main/
│   │   ├── java/com/autonova/employee_dashboard/
│   │   │   ├── controller/        # REST controllers
│   │   │   ├── service/           # Business logic
│   │   │   ├── repository/        # Data access
│   │   │   ├── domain/            # Entities and enums
│   │   │   ├── dto/               # Data transfer objects
│   │   │   └── exception/         # Exception handling
│   │   └── resources/
│   │       ├── application.properties
│   │       └── db/init.sql
│   └── test/
└── pom.xml
```

## Integration with Other Services

This service is designed to work with:
- **Discovery Service** (Eureka) - Service registration and discovery
- **Gateway Service** - API Gateway routing
- **Auth Service** - Authentication and authorization
- **Project Service** - Project management integration

## Future Enhancements

- Add authentication and authorization
- Implement job assignment workflow
- Add time tracking for actual hours worked
- Integrate with notification service
- Add reporting and analytics
- Implement job history and audit logs
