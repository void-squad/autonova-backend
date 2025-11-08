# Time Logging Service

A Spring Boot microservice for managing employee time logs, projects, and tasks in the AutoNova Automobile Service Management System.

## 🎯 Overview

The Time Logging Service is a core microservice in the AutoNova system that enables employees to:

- Log time spent on project tasks
- Track hours worked on service and modification projects
- View project tasks and assignments

This service integrates with the Employee Dashboard and supports real-time tracking of billable hours for automotive service and modification projects.

## ✨ Features

### Time Log Management

- Create, update, and delete time log entries
- Automatic validation of employee-task assignments
- Automatic calculation of task actual hours
- Prevent logging on completed/cancelled projects
- Timestamp tracking with `@CreationTimestamp` and `@UpdateTimestamp`

### Project & Task Tracking

- View projects assigned to employees
- Track tasks by project, employee, or status
- Calculate total hours per project/task
- Support for SERVICE and MODIFICATION project types

### Employee Analytics

- Total hours worked by employee
- Project-specific time tracking
- Employee summary reports

### Data Integrity

- Foreign key constraints
- Business rule validation
- Transaction management
- Global exception handling

## 🛠 Tech Stack

- **Framework**: Spring Boot 3.5.7
- **Language**: Java 17
- **Database**: PostgreSQL 17.5 (Neon Cloud)
- **ORM**: Hibernate 6.6.33 / Spring Data JPA
- **Build Tool**: Maven
- **Additional Libraries**:
  - Lombok (boilerplate reduction)
  - Jakarta Validation (input validation)
  - dotenv-java (environment variable management)
  - HikariCP (connection pooling)

## 🏗 Architecture

### Package Structure

```
com.automobileservice.time_logging_service/
├── config/           # CORS and application configuration
├── controller/       # REST API endpoints
├── dto/             # Data Transfer Objects
│   ├── request/     # Request DTOs
│   └── response/    # Response DTOs
├── entity/          # JPA entities
├── exception/       # Custom exceptions and handlers
├── repository/      # Spring Data JPA repositories
└── service/         # Business logic layer
    └── impl/        # Service implementations
```

## 🚀 Getting Started

### Prerequisites

- Java 17 or higher
- Maven 3.6+
- PostgreSQL database (or use provided Neon cloud instance)
- Git

### Installation

1. **Clone the repository**

   ```bash
   git clone https://github.com/void-squad/autonova-backend.git
   cd autonova-backend/time-logging-service
   ```

2. **Configure environment variables**

   Create/update `../infra/.env` file:

   ```env
   POSTGRES_HOST=host
   POSTGRES_PORT=5432
   POSTGRES_USER=neondb_owner
   POSTGRES_PASSWORD=your_password_here
   PGDATABASE=time_logging_db
   ```

3. **Build the project**

   ```bash
   ./mvnw clean install
   ```

4. **Run the application**

   ```bash
   ./mvnw spring-boot:run
   ```

   The service will start on `http://localhost:8083`

### Initial Setup

On first run, the application will:

1. Connect to PostgreSQL database
2. Create all tables via Hibernate DDL
3. Populate mock data from `data.sql`
4. Start on port 8083

> **Note**: After the first successful run, `data.sql` is disabled to prevent duplicate key errors. To reset data, drop all tables and restart.

## 📚 API Documentation

### Base URL

```
http://localhost:8083/api
```

### Health Check

**Check service health**

```bash
curl http://localhost:8083/actuator/health
```

**Expected Response:**

```json
{ "status": "UP" }
```

---

### Time Log Endpoints

#### 1. Create Time Log

**Endpoint:** `POST /api/time-logs`

**PowerShell:**

```powershell
Invoke-RestMethod -Method POST -Uri "http://localhost:8083/api/time-logs" `
  -ContentType "application/json" `
  -Body '{"projectId":"proj-001","taskId":"task-001","employeeId":"emp-001","hours":2.5,"note":"Completed oil change successfully"}'
```

**Git Bash/Linux:**

```bash
curl -X POST http://localhost:8083/api/time-logs \
  -H "Content-Type: application/json" \
  -d '{
    "projectId": "proj-001",
    "taskId": "task-001",
    "employeeId": "emp-001",
    "hours": 2.5,
    "note": "Completed oil change successfully"
  }'
```

**Request Body:**

```json
{
  "projectId": "proj-001",
  "taskId": "task-001",
  "employeeId": "emp-001",
  "hours": 2.5,
  "note": "Completed oil change successfully"
}
```

**Response (201 Created):**

```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "projectId": "proj-001",
  "projectTitle": "Regular Maintenance - Toyota Camry",
  "taskId": "task-001",
  "taskName": "Oil Change",
  "employeeId": "emp-001",
  "employeeName": "John Doe",
  "hours": 2.5,
  "note": "Completed oil change successfully",
  "loggedAt": "2025-11-01T16:30:00"
}
```

#### 2. Get Time Logs by Employee

**Endpoint:** `GET /api/time-logs/employee/{employeeId}`

```bash
curl http://localhost:8083/api/time-logs/employee/emp-001
```

**Response (200 OK):**

```json
[
  {
    "id": "log-001",
    "projectId": "proj-002",
    "projectTitle": "Custom Exhaust System",
    "taskId": "task-004",
    "taskName": "Remove Old Exhaust",
    "employeeId": "emp-002",
    "employeeName": "Jane Smith",
    "hours": 1.5,
    "note": "Removed old exhaust system, all bolts removed successfully",
    "loggedAt": "2025-11-01T13:00:00"
  }
]
```

#### 3. Get Time Logs by Project

**Endpoint:** `GET /api/time-logs/project/{projectId}`

```bash
curl http://localhost:8083/api/time-logs/project/proj-001
```

#### 4. Get Time Logs by Task

**Endpoint:** `GET /api/time-logs/task/{taskId}`

```bash
curl http://localhost:8083/api/time-logs/task/task-001
```

#### 5. Get Specific Time Log

**Endpoint:** `GET /api/time-logs/{id}`

```bash
curl http://localhost:8083/api/time-logs/log-001
```

#### 6. Update Time Log

**Endpoint:** `PUT /api/time-logs/{id}`

**PowerShell:**

```powershell
Invoke-RestMethod -Method PUT -Uri "http://localhost:8083/api/time-logs/log-001" `
  -ContentType "application/json" `
  -Body '{"projectId":"proj-001","taskId":"task-001","employeeId":"emp-001","hours":3.0,"note":"Updated hours after review"}'
```

#### 7. Delete Time Log

**Endpoint:** `DELETE /api/time-logs/{id}`

```bash
curl -X DELETE http://localhost:8083/api/time-logs/log-001
```

**Response:** `204 No Content`

---

### Analytics Endpoints

#### 8. Get Employee Total Hours

**Endpoint:** `GET /api/time-logs/employee/{employeeId}/total-hours`

```bash
curl http://localhost:8083/api/time-logs/employee/emp-001/total-hours
```

**Response (200 OK):**

```json
5.75
```

#### 9. Get Employee Summary

**Endpoint:** `GET /api/time-logs/employee/{employeeId}/summary`

```bash
curl http://localhost:8083/api/time-logs/employee/emp-001/summary
```

**Response (200 OK):**

```json
{
  "employeeId": "emp-001",
  "employeeName": "John Doe",
  "totalHours": 5.75,
  "hourlyRate": 35.5,
  "totalEarnings": 204.12
}
```

#### 10. Get Employee Hours by Project

**Endpoint:** `GET /api/time-logs/employee/{employeeId}/project/{projectId}`

```bash
curl http://localhost:8083/api/time-logs/employee/emp-001/project/proj-001
```

---

### Project Endpoints

#### 11. Get Projects Assigned to Employee

**Endpoint:** `GET /api/projects/employee/{employeeId}`

```bash
curl http://localhost:8083/api/projects/employee/emp-001
```

**Response (200 OK):**

```json
[
  {
    "id": "proj-001",
    "title": "Regular Maintenance - Toyota Camry",
    "description": "Oil change, brake inspection, tire rotation",
    "projectType": "SERVICE",
    "status": "IN_PROGRESS",
    "priority": "MEDIUM",
    "customerId": "cust-001",
    "customerName": "Alice Johnson",
    "vehicleId": "veh-001",
    "vehicleDetails": "2020 Toyota Camry (ABC-1234)",
    "estimatedCost": 350.0,
    "startDate": "2025-10-28",
    "endDate": "2025-11-28"
  }
]
```

#### 12. Get Active Projects

**Endpoint:** `GET /api/projects/active`

```bash
curl http://localhost:8083/api/projects/active
```

#### 13. Get Specific Project

**Endpoint:** `GET /api/projects/{projectId}`

```bash
curl http://localhost:8083/api/projects/proj-001
```

---

### Task Endpoints

#### 14. Get Tasks by Project

**Endpoint:** `GET /api/tasks/project/{projectId}`

```bash
curl http://localhost:8083/api/tasks/project/proj-001
```

**Response (200 OK):**

```json
[
  {
    "id": "task-001",
    "projectId": "proj-001",
    "taskName": "Oil Change",
    "description": "Replace engine oil and filter",
    "assignedEmployeeId": "emp-001",
    "assignedEmployeeName": "John Doe",
    "estimatedHours": 1.0,
    "actualHours": 0.75,
    "status": "IN_PROGRESS",
    "priority": "MEDIUM",
    "dueDate": "2025-11-02"
  }
]
```

#### 15. Get Tasks Assigned to Employee

**Endpoint:** `GET /api/tasks/employee/{employeeId}`

```bash
curl http://localhost:8083/api/tasks/employee/emp-001
```

#### 16. Get Incomplete Tasks by Employee

**Endpoint:** `GET /api/tasks/employee/{employeeId}/incomplete`

```bash
curl http://localhost:8083/api/tasks/employee/emp-001/incomplete
```

#### 17. Get Specific Task

**Endpoint:** `GET /api/tasks/{taskId}`

```bash
curl http://localhost:8083/api/tasks/task-001
```

---

### Error Responses

#### 404 Not Found

```json
{
  "timestamp": "2025-11-01T16:30:00",
  "status": 404,
  "error": "Not Found",
  "message": "Employee not found with id: emp-999",
  "path": "/api/time-logs",
  "details": null
}
```

#### 400 Bad Request (Business Rule Violation)

```json
{
  "timestamp": "2025-11-01T16:30:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Employee is not assigned to this task",
  "path": "/api/time-logs",
  "details": null
}
```

#### 400 Bad Request (Validation Error)

```json
{
  "timestamp": "2025-11-01T16:30:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "path": "/api/time-logs",
  "details": {
    "hours": "must be greater than 0",
    "employeeId": "must not be blank"
  }
}
```

## 🗄 Database Schema

### Tables

#### 1. users

```sql
CREATE TABLE users (
    id VARCHAR(36) PRIMARY KEY,
    email VARCHAR(255) UNIQUE NOT NULL,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    role VARCHAR(50) NOT NULL, -- 'CUSTOMER', 'EMPLOYEE', 'ADMIN'
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

#### 2. employees

```sql
CREATE TABLE employees (
    user_id VARCHAR(36) PRIMARY KEY,
    employee_code VARCHAR(50) UNIQUE NOT NULL,
    department VARCHAR(100),
    position VARCHAR(100),
    hourly_rate DECIMAL(10, 2),
    hire_date DATE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id)
);
```

#### 3. customers

```sql
CREATE TABLE customers (
    user_id VARCHAR(36) PRIMARY KEY,
    phone VARCHAR(20),
    address TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id)
);
```

#### 4. vehicles

```sql
CREATE TABLE vehicles (
    id VARCHAR(36) PRIMARY KEY,
    customer_id VARCHAR(36) NOT NULL,
    make VARCHAR(100) NOT NULL,
    model VARCHAR(100) NOT NULL,
    year INT NOT NULL,
    license_plate VARCHAR(20) UNIQUE NOT NULL,
    vin VARCHAR(50),
    color VARCHAR(50),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (customer_id) REFERENCES customers(user_id)
);
```

#### 5. projects

```sql
CREATE TABLE projects (
    id VARCHAR(36) PRIMARY KEY,
    customer_id VARCHAR(36) NOT NULL,
    vehicle_id VARCHAR(36) NOT NULL,
    project_type VARCHAR(50) NOT NULL, -- 'SERVICE', 'MODIFICATION'
    title VARCHAR(255) NOT NULL,
    description TEXT,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    priority VARCHAR(20) DEFAULT 'MEDIUM',
    estimated_cost DECIMAL(10, 2),
    start_date DATE,
    end_date DATE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (customer_id) REFERENCES customers(user_id),
    FOREIGN KEY (vehicle_id) REFERENCES vehicles(id)
);
```

#### 6. project_tasks

```sql
CREATE TABLE project_tasks (
    id VARCHAR(36) PRIMARY KEY,
    project_id VARCHAR(36) NOT NULL,
    task_name VARCHAR(255) NOT NULL,
    description TEXT,
    assigned_employee_id VARCHAR(36),
    estimated_hours DECIMAL(5, 2),
    actual_hours DECIMAL(5, 2) DEFAULT 0.0,
    status VARCHAR(50) NOT NULL DEFAULT 'TODO',
    priority VARCHAR(20) DEFAULT 'MEDIUM',
    due_date DATE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (project_id) REFERENCES projects(id),
    FOREIGN KEY (assigned_employee_id) REFERENCES employees(user_id)
);
```

#### 7. time_logs

```sql
CREATE TABLE time_logs (
    id VARCHAR(36) PRIMARY KEY,
    project_id VARCHAR(36) NOT NULL,
    task_id VARCHAR(36) NOT NULL,
    employee_id VARCHAR(36) NOT NULL,
    hours DECIMAL(5, 2) NOT NULL CHECK (hours > 0),
    note TEXT,
    logged_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (project_id) REFERENCES projects(id),
    FOREIGN KEY (task_id) REFERENCES project_tasks(id),
    FOREIGN KEY (employee_id) REFERENCES employees(user_id)
);
```

### Indexes

```sql
CREATE INDEX idx_time_logs_employee ON time_logs(employee_id);
CREATE INDEX idx_time_logs_project ON time_logs(project_id);
CREATE INDEX idx_time_logs_task ON time_logs(task_id);
CREATE INDEX idx_time_logs_logged_at ON time_logs(logged_at);
CREATE INDEX idx_project_tasks_assigned ON project_tasks(assigned_employee_id);
CREATE INDEX idx_projects_customer ON projects(customer_id);
```

## ⚙ Configuration

### application.properties

```properties
# Server Configuration
server.port=8083

# Database Configuration
spring.datasource.url=jdbc:postgresql://${POSTGRES_HOST}:${POSTGRES_PORT}/${PGDATABASE}?sslmode=require
spring.datasource.username=${POSTGRES_USER}
spring.datasource.password=${POSTGRES_PASSWORD}
spring.datasource.driver-class-name=org.postgresql.Driver

# JPA/Hibernate Configuration
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true

# SQL Initialization (disabled after first run)
# spring.sql.init.mode=always
spring.jpa.defer-datasource-initialization=true

# Logging
logging.level.root=INFO
logging.level.com.automobileservice.time_logging_service=DEBUG
logging.level.org.hibernate.SQL=DEBUG

# Error Handling
server.error.include-message=always
server.error.include-binding-errors=always
```

### CORS Configuration

Configured in `WebConfig.java` to allow:

- **Origins**: `http://localhost:3000`, `http://localhost:5173` (React/Vite dev servers)
- **Methods**: GET, POST, PUT, DELETE, OPTIONS
- **Credentials**: Enabled

## 🧪 Testing

### Manual Testing with cURL

**Test complete workflow:**

```bash
# 1. Check health
curl http://localhost:8083/actuator/health

# 2. Get employee's projects
curl http://localhost:8083/api/projects/employee/emp-001

# 3. Get project tasks
curl http://localhost:8083/api/tasks/project/proj-001

# 4. Create time log (PowerShell)
Invoke-RestMethod -Method POST -Uri "http://localhost:8083/api/time-logs" `
  -ContentType "application/json" `
  -Body '{"projectId":"proj-001","taskId":"task-001","employeeId":"emp-001","hours":2.5,"note":"Work completed"}'

# 5. Get employee's time logs
curl http://localhost:8083/api/time-logs/employee/emp-001

# 6. Get employee summary
curl http://localhost:8083/api/time-logs/employee/emp-001/summary
```

### Mock Data

The application includes pre-populated mock data:

- **Users**: 4 (2 employees, 2 customers)
- **Employees**: John Doe (emp-001), Jane Smith (emp-002)
- **Customers**: Alice Johnson (cust-001), Bob Williams (cust-002)
- **Vehicles**: 3 vehicles
- **Projects**: 4 projects (service and modification types)
- **Tasks**: 10 tasks assigned to employees
- **Time Logs**: 4 sample time entries

## 🔧 Troubleshooting

### Issue: Application fails to start with "duplicate key" error

**Solution:** The database already has data. Either:

1. Disable `data.sql` by commenting out `spring.sql.init.mode=always`
2. Drop all tables and restart for fresh data

### Issue: "Employee not found" or "Project not found"

**Solution:** Verify the IDs exist in the database. Use mock data IDs:

- Employees: `emp-001`, `emp-002`
- Projects: `proj-001` to `proj-004`
- Tasks: `task-001` to `task-010`

### Issue: "Employee is not assigned to this task"

**Solution:** Ensure the employee is actually assigned to the task:

- Check `/api/tasks/employee/{employeeId}` to see assigned tasks
- Only log time on tasks assigned to you

### Issue: Cannot log time on project

**Solution:** Check project status:

- Only `PENDING` and `IN_PROGRESS` projects accept time logs
- `COMPLETED` and `CANCELLED` projects are locked

### Issue: CORS errors from frontend

**Solution:** Verify frontend origin is in `WebConfig.java`:

```java
.allowedOrigins("http://localhost:3000", "http://localhost:5173")
```

### Issue: Database connection failed

**Solution:**

1. Check `../infra/.env` file has correct credentials
2. Verify network connectivity to Neon database
3. Check firewall/VPN settings

## 📝 Development Notes

### Current Limitations

- **Authentication**: Currently uses hardcoded employee ID (`emp-001`). Integration with Auth Service pending.
- **Service Discovery**: Eureka integration not yet implemented.
- **API Gateway**: Direct service access, gateway routing pending.

### Future Enhancements

- [ ] JWT authentication integration
- [ ] Kafka/RabbitMQ for event-driven communication
- [ ] Redis caching for frequently accessed data
- [ ] Advanced reporting and analytics
- [ ] File attachments for time log notes
- [ ] Bulk time log imports
- [ ] Automated task completion on hours threshold

## 📄 License

This project is part of the AutoNova Automobile Service Management System developed by the Void Squad team.

## 👥 Contributors

- **Kavindya** - Developer (Time Logging Service)
- **Void Squad Team** - Project Contributors

## 🔗 Related Services

- **Auth Service** - User authentication and authorization
- **Employee Dashboard Service** - Employee management and dashboards
- **Appointment Booking Service** - Customer appointment scheduling
- **Gateway Service** - API Gateway and routing
- **Discovery Service** - Service registry and discovery (Eureka)
