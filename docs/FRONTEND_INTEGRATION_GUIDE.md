# Employee Dashboard Integration Guide

## Overview
This guide explains how to integrate the Employee Dashboard Service with the Project Service to fetch projects and tasks assigned to employees.

## Architecture

### Services Involved
1. **Auth Service** (Port 8081) - Issues JWT tokens with userId
2. **Gateway Service** (Port 8080) - Routes all requests
3. **Employee Dashboard Service** (Port 8084) - BFF (Backend for Frontend)
4. **Project Service** (Port 8082) - .NET service managing projects and tasks

## Authentication Flow

### 1. User Login
When a user logs in through the frontend:

```javascript
// Frontend login request
POST /api/auth/login
{
  "email": "employee@example.com",
  "password": "password123"
}

// Response contains JWT token
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "userId": 123,
  "email": "employee@example.com",
  "role": "EMPLOYEE",
  "firstName": "John"
}
```

**Important**: The frontend receives BOTH the token and the userId. Store both in your frontend state/storage.

### 2. JWT Token Structure
The JWT token contains these claims:
```json
{
  "userId": 123,           // Long (Integer)
  "email": "employee@example.com",
  "role": "EMPLOYEE",
  "firstName": "John",
  "sub": "employee@example.com",  // Subject is email
  "iat": 1699430000,
  "exp": 1699516400
}
```

## API Endpoints

### Base URLs
- **Through Gateway**: `http://localhost:8080`
- **Employee Dashboard Service**: `http://localhost:8084`

### 1. Get User's Projects

**Endpoint**: `GET /api/employee/dashboard/projects`

**Headers**:
```
Authorization: Bearer <jwt_token>
```

**Query Parameters**:
- `includeTasks` (boolean, default: true) - Include tasks in projects
- `page` (integer, default: 1) - Page number
- `pageSize` (integer, default: 20) - Items per page

**Example Request** (via Gateway):
```bash
curl -X GET \
  'http://localhost:8080/api/employee-dashboard/projects?includeTasks=true&page=1&pageSize=20' \
  -H 'Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...'
```

**Example Response**:
```json
[
  {
    "projectId": "550e8400-e29b-41d4-a716-446655440000",
    "customerId": "123e4567-e89b-12d3-a456-426614174000",
    "title": "Vehicle Repair Project",
    "status": "InProgress",
    "createdAt": "2024-01-15T10:30:00Z",
    "updatedAt": "2024-01-20T14:45:00Z",
    "budget": 5000.00,
    "dueDate": "2024-02-15",
    "tasks": [
      {
        "taskId": "660e8400-e29b-41d4-a716-446655440001",
        "title": "Engine Inspection",
        "estimateHours": 8.5,
        "assigneeId": "123",
        "status": "InProgress"
      }
    ],
    "quotes": [],
    "statusHistory": []
  }
]
```

### 2. Get User's Tasks

**Endpoint**: `GET /api/employee/dashboard/tasks`

**Headers**:
```
Authorization: Bearer <jwt_token>
```

**Query Parameters**:
- `status` (string, optional) - Filter by status (e.g., "InProgress", "Completed", "NotStarted")
- `page` (integer, default: 1) - Page number
- `pageSize` (integer, default: 50) - Items per page

**Example Request** (via Gateway):
```bash
curl -X GET \
  'http://localhost:8080/api/employee-dashboard/tasks?status=InProgress&page=1&pageSize=50' \
  -H 'Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...'
```

**Example Response**:
```json
{
  "page": 1,
  "pageSize": 50,
  "total": 15,
  "items": [
    {
      "taskId": "660e8400-e29b-41d4-a716-446655440001",
      "projectId": "550e8400-e29b-41d4-a716-446655440000",
      "title": "Engine Inspection",
      "description": "Complete thorough inspection of engine components",
      "assigneeId": "123",
      "status": "InProgress",
      "estimateHours": 8.5,
      "createdAt": "2024-01-15T10:30:00Z",
      "project": {
        "projectId": "550e8400-e29b-41d4-a716-446655440000",
        "title": "Vehicle Repair Project",
        "status": "InProgress"
      }
    }
  ]
}
```

## Frontend Integration

### React/Vue/Angular Example

```javascript
// 1. Login and Store Token
async function login(email, password) {
  const response = await fetch('http://localhost:8080/api/auth/login', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ email, password })
  });
  
  const data = await response.json();
  
  // Store token and userId
  localStorage.setItem('token', data.token);
  localStorage.setItem('userId', data.userId);
  
  return data;
}

// 2. Fetch User's Projects
async function getMyProjects(includeTasks = true, page = 1, pageSize = 20) {
  const token = localStorage.getItem('token');
  
  const response = await fetch(
    `http://localhost:8080/api/employee-dashboard/projects?includeTasks=${includeTasks}&page=${page}&pageSize=${pageSize}`,
    {
      method: 'GET',
      headers: {
        'Authorization': `Bearer ${token}`,
        'Content-Type': 'application/json'
      }
    }
  );
  
  if (!response.ok) {
    throw new Error('Failed to fetch projects');
  }
  
  return await response.json();
}

// 3. Fetch User's Tasks
async function getMyTasks(status = null, page = 1, pageSize = 50) {
  const token = localStorage.getItem('token');
  
  let url = `http://localhost:8080/api/employee-dashboard/tasks?page=${page}&pageSize=${pageSize}`;
  if (status) {
    url += `&status=${status}`;
  }
  
  const response = await fetch(url, {
    method: 'GET',
    headers: {
      'Authorization': `Bearer ${token}`,
      'Content-Type': 'application/json'
    }
  });
  
  if (!response.ok) {
    throw new Error('Failed to fetch tasks');
  }
  
  return await response.json();
}

// 4. Usage Example
async function loadDashboard() {
  try {
    // Fetch projects with tasks
    const projects = await getMyProjects(true, 1, 20);
    console.log('Projects:', projects);
    
    // Fetch in-progress tasks
    const tasks = await getMyTasks('InProgress', 1, 50);
    console.log('Tasks:', tasks);
    
    // Display in UI...
  } catch (error) {
    console.error('Error loading dashboard:', error);
  }
}
```

### Axios Example

```javascript
import axios from 'axios';

// Configure axios instance
const api = axios.create({
  baseURL: 'http://localhost:8080',
  headers: {
    'Content-Type': 'application/json'
  }
});

// Add auth token to all requests
api.interceptors.request.use(config => {
  const token = localStorage.getItem('token');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// Get user's projects
export const getMyProjects = async (includeTasks = true, page = 1, pageSize = 20) => {
  const response = await api.get('/api/employee-dashboard/projects', {
    params: { includeTasks, page, pageSize }
  });
  return response.data;
};

// Get user's tasks
export const getMyTasks = async (status = null, page = 1, pageSize = 50) => {
  const params = { page, pageSize };
  if (status) params.status = status;
  
  const response = await api.get('/api/employee-dashboard/tasks', { params });
  return response.data;
};
```

## Important Notes

### 1. User ID Extraction
The Employee Dashboard Service automatically extracts the userId from the JWT token. **The frontend does NOT need to pass the userId as a parameter** - it's extracted from the token on the backend.

### 2. User ID Type Conversion
- **Auth Service** stores userId as `Long` (e.g., 123)
- **Project Service** expects userId as `Guid/UUID` (e.g., "00000000-0000-0000-0000-000000000123")
- The conversion happens automatically in the `ProjectServiceClient`

### 3. Authentication Requirements
- All endpoints require a valid JWT token in the Authorization header
- Token must have the "EMPLOYEE" role
- Token must not be expired

### 4. CORS Configuration
The gateway is configured to allow requests from:
- `http://localhost:5173` (default Vite dev server)

If your frontend runs on a different port, update the gateway's CORS configuration.

### 5. Error Handling
Always handle these potential errors:
- `401 Unauthorized` - Invalid or expired token
- `403 Forbidden` - User doesn't have the required role
- `500 Internal Server Error` - Backend service error

## Task Status Values
Common status values for filtering tasks:
- `NotStarted`
- `InProgress`
- `Completed`
- `Blocked`
- `Cancelled`

## Project Status Values
- `Draft`
- `PendingApproval`
- `InProgress`
- `Completed`
- `Cancelled`

## Testing

### Using cURL

```bash
# 1. Login
TOKEN=$(curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"employee@example.com","password":"password123"}' \
  | jq -r '.token')

# 2. Get Projects
curl -X GET "http://localhost:8080/api/employee-dashboard/projects?includeTasks=true&page=1&pageSize=20" \
  -H "Authorization: Bearer $TOKEN"

# 3. Get Tasks
curl -X GET "http://localhost:8080/api/employee-dashboard/tasks?status=InProgress&page=1&pageSize=50" \
  -H "Authorization: Bearer $TOKEN"
```

### Using Postman

1. **Login Request**:
   - Method: POST
   - URL: `http://localhost:8080/api/auth/login`
   - Body (JSON):
     ```json
     {
       "email": "employee@example.com",
       "password": "password123"
     }
     ```
   - Save the `token` from response

2. **Get Projects**:
   - Method: GET
   - URL: `http://localhost:8080/api/employee-dashboard/projects?includeTasks=true&page=1&pageSize=20`
   - Headers:
     - `Authorization`: `Bearer <your_token>`

3. **Get Tasks**:
   - Method: GET
   - URL: `http://localhost:8080/api/employee-dashboard/tasks?status=InProgress&page=1&pageSize=50`
   - Headers:
     - `Authorization`: `Bearer <your_token>`

## Troubleshooting

### "No valid Authorization header found"
- Ensure the Authorization header is included
- Verify the format is `Bearer <token>`, not just `<token>`

### "Could not extract userId from token"
- Token may be invalid or corrupted
- Token may be from a different environment
- Try logging in again to get a fresh token

### Empty Results
- User may not have any assigned projects/tasks
- Check the userId in the JWT token matches database records
- Verify the assigneeId in projects/tasks matches the user's ID

### 401 Unauthorized
- Token has expired (default: 24 hours)
- Token is invalid
- User needs to log in again

### 403 Forbidden
- User doesn't have the EMPLOYEE role
- Check user role in the JWT token

## Development Tips

1. **Token Expiration**: Implement token refresh logic in your frontend
2. **Loading States**: Show loading indicators while fetching data
3. **Error Messages**: Display user-friendly error messages
4. **Pagination**: Implement infinite scroll or pagination UI
5. **Caching**: Consider caching responses to reduce API calls
6. **Real-time Updates**: Consider using WebSockets for real-time task updates

## Next Steps

To extend this integration:
1. Add WebSocket support for real-time updates
2. Implement push notifications for new task assignments
3. Add task filtering and search capabilities
4. Include task comments and attachments
5. Add project progress tracking
