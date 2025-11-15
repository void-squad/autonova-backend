# Employee Dashboard BFF - API Testing Guide

## Prerequisites
1. **Auth Service** must be running on port 8081
2. **Employee Dashboard Service** must be running on port 8084
3. You need an employee account in the auth service

## Step 1: Get JWT Token

First, login to get a JWT token from the auth service.

### Request
```bash
curl -X POST http://localhost:8081/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "employee@autonova.com",
    "password": "password123"
  }'
```

### Response
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiJ9...",
  "userId": 1,
  "email": "employee@autonova.com",
  "role": "EMPLOYEE"
}
```

**Copy the `token` value for the next request.**

---

## Step 2: Call Dashboard Endpoint

Use the JWT token to fetch the employee dashboard data.

### Request
```bash
curl -X GET http://localhost:8084/api/employee/dashboard \
  -H "Authorization: Bearer YOUR_JWT_TOKEN_HERE"
```

### Successful Response (200 OK)
```json
{
  "employeeInfo": {
    "userId": 1,
    "name": "Employee User",
    "email": "employee@autonova.com",
    "role": "EMPLOYEE",
    "department": "Service Department"
  },
  "stats": {
    "totalActiveProjects": 5,
    "pendingAppointments": 3,
    "completedTasksThisWeek": 12,
    "totalRevenueThisMonth": 45000.0,
    "totalCustomers": 28
  },
  "recentActivities": [
    {
      "id": "ACT-001",
      "type": "PROJECT_UPDATE",
      "description": "Updated project PRJ-2024-001 progress to 65%",
      "timestamp": "2025-11-07 14:30:00",
      "status": "COMPLETED"
    },
    {
      "id": "ACT-002",
      "type": "APPOINTMENT",
      "description": "Completed appointment with customer John Doe",
      "timestamp": "2025-11-07 11:30:00",
      "status": "COMPLETED"
    },
    {
      "id": "ACT-003",
      "type": "PAYMENT_RECEIVED",
      "description": "Payment received for Invoice #INV-2024-045",
      "timestamp": "2025-11-06 16:30:00",
      "status": "COMPLETED"
    }
  ],
  "upcomingTasks": [
    {
      "id": "TASK-001",
      "title": "Complete vehicle inspection",
      "description": "Inspect vehicle for project PRJ-2024-001",
      "dueDate": "2025-11-09",
      "priority": "HIGH",
      "projectId": "PRJ-2024-001"
    },
    {
      "id": "TASK-002",
      "title": "Customer follow-up call",
      "description": "Follow up with customer regarding service feedback",
      "dueDate": "2025-11-10",
      "priority": "MEDIUM",
      "projectId": "PRJ-2024-003"
    },
    {
      "id": "TASK-003",
      "title": "Submit weekly report",
      "description": "Prepare and submit weekly progress report",
      "dueDate": "2025-11-12",
      "priority": "MEDIUM",
      "projectId": null
    }
  ],
  "activeProjects": [
    {
      "projectId": "PRJ-2024-001",
      "projectName": "Toyota Camry - Full Service",
      "customerName": "John Doe",
      "status": "IN_PROGRESS",
      "startDate": "2025-10-28",
      "expectedCompletionDate": "2025-11-12",
      "progressPercentage": 65
    },
    {
      "projectId": "PRJ-2024-003",
      "projectName": "Honda Accord - Repair",
      "customerName": "Jane Smith",
      "status": "IN_PROGRESS",
      "startDate": "2025-11-02",
      "expectedCompletionDate": "2025-11-17",
      "progressPercentage": 30
    },
    {
      "projectId": "PRJ-2024-005",
      "projectName": "BMW X5 - Paint Job",
      "customerName": "Robert Johnson",
      "status": "PENDING",
      "startDate": "2025-11-09",
      "expectedCompletionDate": "2025-11-27",
      "progressPercentage": 0
    }
  ]
}
```

---

## Error Responses

### 401 Unauthorized - Missing/Invalid Token
```bash
curl -X GET http://localhost:8084/api/employee/dashboard
```

**Response:**
```json
{
  "error": "Unauthorized"
}
```

### 403 Forbidden - Not an Employee
If you try to access with a CUSTOMER role token:

**Response:**
```json
{
  "error": "Forbidden",
  "message": "Access Denied"
}
```

---

## Testing with Postman

### Collection Setup

1. **Create a new request collection** named "Employee Dashboard BFF"

2. **Request 1: Login**
   - Method: `POST`
   - URL: `http://localhost:8081/api/auth/login`
   - Headers:
     - `Content-Type`: `application/json`
   - Body (raw JSON):
     ```json
     {
       "email": "employee@autonova.com",
       "password": "password123"
     }
     ```
   - Save the response token for the next request

3. **Request 2: Get Dashboard**
   - Method: `GET`
   - URL: `http://localhost:8084/api/employee/dashboard`
   - Headers:
     - `Authorization`: `Bearer {{token}}` (use token from step 2)
   - No body required

---

## Health Check

Test if the service is running:

```bash
curl http://localhost:8084/actuator/health
```

**Response:**
```json
{
  "status": "UP"
}
```

---

## Notes

- **Current Data**: The service returns mock data. When other services are implemented, this will return real data.
- **Token Expiry**: JWT tokens expire after 1 hour. Get a new token if you receive 401 errors.
- **Security**: Only users with `EMPLOYEE` role can access the dashboard endpoint.

---

## Frontend Integration

### React/JavaScript Example

```javascript
// Login and get token
const login = async () => {
  const response = await fetch('http://localhost:8081/api/auth/login', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      email: 'employee@autonova.com',
      password: 'password123'
    })
  });
  
  const data = await response.json();
  localStorage.setItem('token', data.token);
  return data.token;
};

// Get dashboard data
const getDashboard = async (token) => {
  const response = await fetch('http://localhost:8084/api/employee/dashboard', {
    method: 'GET',
    headers: {
      'Authorization': `Bearer ${token}`
    }
  });
  
  if (!response.ok) {
    throw new Error('Failed to fetch dashboard data');
  }
  
  return await response.json();
};

// Usage
const token = await login();
const dashboardData = await getDashboard(token);
console.log(dashboardData);
```

---

## Troubleshooting

### Issue: 401 Unauthorized
- **Solution**: Make sure you're including the JWT token in the Authorization header
- **Solution**: Check if the token has expired (tokens last 1 hour)
- **Solution**: Verify the JWT secret matches between auth-service and dashboard-service

### Issue: 403 Forbidden
- **Solution**: Make sure you're logged in as an EMPLOYEE, not a CUSTOMER
- **Solution**: Check the role in the JWT token payload

### Issue: Connection Refused
- **Solution**: Verify auth-service is running on port 8081
- **Solution**: Verify employee-dashboard-service is running on port 8084
- **Solution**: Check firewall settings

---

## Next Steps

When other services are implemented:
1. Update `EmployeeDashboardBFFService.java` to call real services
2. Remove mock data methods
3. Add error handling for service failures
4. Add caching for better performance
5. Implement circuit breakers for resilience
