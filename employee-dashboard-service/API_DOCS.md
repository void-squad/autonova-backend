# Employee Dashboard Service API Documentation

Base URL: `http://localhost:8084/api/dashboard`

## Authentication
All endpoints require authentication via JWT token in the Authorization header:
```
Authorization: Bearer <token>
```

---

## Operational View Endpoints

### Get Operational Dashboard
Aggregates real-time operational data from multiple services.

**Endpoint:** `GET /api/dashboard/operational`

**Headers:**
- `Authorization: Bearer <token>`

**Response:** `200 OK`
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

---

## Analytics Endpoints

### Get Analytics Summary
Fetches analytics summary data.

**Endpoint:** `GET /api/dashboard/analytics/summary`

**Headers:**
- `Authorization: Bearer <token>`

**Response:** `200 OK`
```json
{
  "totalJobs": 150,
  "completedJobs": 120,
  "averageCompletionTime": 45,
  "customerSatisfaction": 4.5,
  "period": "Q3-2025"
}
```

### Save Analytics Report
Saves custom analytics report parameters.

**Endpoint:** `POST /api/dashboard/analytics/save-report`

**Headers:**
- `Authorization: Bearer <token>`
- `Content-Type: application/json`

**Request Body:**
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

**Response:** `200 OK`
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

### Get Saved Reports
Retrieves all saved reports for the authenticated employee.

**Endpoint:** `GET /api/dashboard/analytics/saved-reports`

**Headers:**
- `Authorization: Bearer <token>`

**Response:** `200 OK`
```json
[
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
  },
  {
    "reportId": 2,
    "employeeId": 1,
    "reportName": "October Performance",
    "reportParameters": {
      "dateRange": "October-2025",
      "metric": "performance"
    },
    "createdAt": "2025-11-02T15:45:00"
  }
]
```

---

## Preferences Endpoints

### Get Employee Preferences
Retrieves the authenticated employee's dashboard preferences.

**Endpoint:** `GET /api/dashboard/preferences`

**Headers:**
- `Authorization: Bearer <token>`

**Response:** `200 OK`
```json
{
  "employeeId": 1,
  "defaultView": "operational",
  "theme": "light"
}
```

### Update Employee Preferences
Updates the authenticated employee's dashboard preferences.

**Endpoint:** `PUT /api/dashboard/preferences`

**Headers:**
- `Authorization: Bearer <token>`
- `Content-Type: application/json`

**Request Body:**
```json
{
  "defaultView": "analytical",
  "theme": "dark"
}
```

**Valid Values:**
- `defaultView`: "operational" or "analytical"
- `theme`: "dark" or "light"

**Response:** `200 OK`
```json
{
  "employeeId": 1,
  "defaultView": "analytical",
  "theme": "dark"
}
```

---

## Error Responses

### 400 Bad Request
```json
{
  "timestamp": "2025-11-01T10:30:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Invalid argument: defaultView must be 'operational' or 'analytical'"
}
```

### 401 Unauthorized
```json
{
  "timestamp": "2025-11-01T10:30:00",
  "status": 401,
  "error": "Unauthorized",
  "message": "Full authentication is required to access this resource"
}
```

### 404 Not Found
```json
{
  "timestamp": "2025-11-01T10:30:00",
  "status": 404,
  "error": "Not Found",
  "message": "Resource not found"
}
```

### 500 Internal Server Error
```json
{
  "timestamp": "2025-11-01T10:30:00",
  "status": 500,
  "error": "Internal Server Error",
  "message": "An unexpected error occurred"
}
```

---

## Service Dependencies

The Employee Dashboard Service depends on the following services:

1. **Time Logging Service** (Port 8081)
   - Endpoint: `/api/time-logging/active?employeeId={employeeId}`

2. **Appointment Booking Service** (Port 8082)
   - Endpoint: `/api/appointments/today?employeeId={employeeId}`

3. **Service Tracking Service** (Port 8083)
   - Endpoint: `/api/service-tracking/work-queue?employeeId={employeeId}`

4. **Analytics and Reporting Service** (Port 8085)
   - Endpoint: `/api/analytics/summary?employeeId={employeeId}`

Make sure these services are running and accessible before using the Employee Dashboard Service.

---

## Testing with cURL

### Get Operational View
```bash
curl -X GET http://localhost:8084/api/dashboard/operational \
  -H "Authorization: Bearer <your-token>"
```

### Get Preferences
```bash
curl -X GET http://localhost:8084/api/dashboard/preferences \
  -H "Authorization: Bearer <your-token>"
```

### Update Preferences
```bash
curl -X PUT http://localhost:8084/api/dashboard/preferences \
  -H "Authorization: Bearer <your-token>" \
  -H "Content-Type: application/json" \
  -d '{
    "defaultView": "analytical",
    "theme": "dark"
  }'
```

### Save Analytics Report
```bash
curl -X POST http://localhost:8084/api/dashboard/analytics/save-report \
  -H "Authorization: Bearer <your-token>" \
  -H "Content-Type: application/json" \
  -d '{
    "reportName": "My Q3 Summary",
    "reportParameters": {
      "dateRange": "Q3-2025",
      "jobType": "Repair"
    }
  }'
```
