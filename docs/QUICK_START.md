# Employee Dashboard - Quick Start Guide

## Problem Solved
✅ Call Project Service endpoints from Employee Dashboard Service through the Gateway  
✅ Extract userId from JWT token automatically  
✅ Handle authentication and authorization properly  

## Solution Overview

### New Endpoints Added to Employee Dashboard Service

#### 1. Get User's Projects
```
GET /api/employee/dashboard/projects
```
- **Headers**: `Authorization: Bearer <token>`
- **Query Params**: 
  - `includeTasks` (default: true)
  - `page` (default: 1)
  - `pageSize` (default: 20)

#### 2. Get User's Tasks
```
GET /api/employee/dashboard/tasks
```
- **Headers**: `Authorization: Bearer <token>`
- **Query Params**:
  - `status` (optional, e.g., "InProgress")
  - `page` (default: 1)
  - `pageSize` (default: 50)

## How It Works

### Backend Flow
```
Frontend (with JWT token)
    ↓
Gateway (http://localhost:8080)
    ↓
Employee Dashboard Service (http://localhost:8084)
    ↓ (extracts userId from JWT)
    ↓ (calls via Gateway)
Gateway (http://localhost:8080)
    ↓
Project Service (http://localhost:8082)
    ↓
Returns data back up the chain
```

### User ID Extraction
The backend automatically extracts the userId from the JWT token:

```java
String token = authHeader.substring(7); // Remove "Bearer "
Long userId = jwtService.extractUserId(token);  // Extract from JWT
String userIdString = userId.toString();  // Convert to String
```

**You do NOT need to pass userId as a parameter!**

## Frontend Usage

### Simple Fetch Example

```javascript
// After login, you have the JWT token
const token = localStorage.getItem('token');

// 1. Get Projects
const projects = await fetch(
  'http://localhost:8080/api/employee-dashboard/projects?includeTasks=true&page=1&pageSize=20',
  {
    method: 'GET',
    headers: {
      'Authorization': `Bearer ${token}`
    }
  }
).then(res => res.json());

// 2. Get Tasks
const tasks = await fetch(
  'http://localhost:8080/api/employee-dashboard/tasks?status=InProgress&page=1&pageSize=50',
  {
    method: 'GET',
    headers: {
      'Authorization': `Bearer ${token}`
    }
  }
).then(res => res.json());
```

### Axios Example

```javascript
import axios from 'axios';

// Setup axios with base URL and token
const api = axios.create({
  baseURL: 'http://localhost:8080',
});

api.interceptors.request.use(config => {
  const token = localStorage.getItem('token');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// Get projects
const projects = await api.get('/api/employee-dashboard/projects', {
  params: { includeTasks: true, page: 1, pageSize: 20 }
});

// Get tasks
const tasks = await api.get('/api/employee-dashboard/tasks', {
  params: { status: 'InProgress', page: 1, pageSize: 50 }
});
```

## Files Created/Modified

### New Files
1. `ProjectServiceClient.java` - Client to call Project Service
2. `ProjectDto.java` - DTO for project data
3. `ProjectListResponse.java` - DTO for project list response
4. `TaskDto.java` - DTO for task data
5. `TaskListResponse.java` - DTO for task list response
6. `FRONTEND_INTEGRATION_GUIDE.md` - Detailed integration guide

### Modified Files
1. `EmployeeDashboardBFFController.java` - Added new endpoints
2. `gateway-service/application.yml` - Fixed routing

## Testing

### Using cURL

```bash
# 1. Login
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"employee@example.com","password":"password"}' \
  | jq -r '.token')

# 2. Get Projects
curl -X GET \
  "http://localhost:8080/api/employee-dashboard/projects?includeTasks=true" \
  -H "Authorization: Bearer $TOKEN"

# 3. Get Tasks
curl -X GET \
  "http://localhost:8080/api/employee-dashboard/tasks?status=InProgress" \
  -H "Authorization: Bearer $TOKEN"
```

### Using Postman

1. **Login** - POST to `http://localhost:8080/api/auth/login`
2. **Copy the token** from the response
3. **Get Projects** - GET to `http://localhost:8080/api/employee-dashboard/projects?includeTasks=true`
   - Add header: `Authorization: Bearer <your_token>`
4. **Get Tasks** - GET to `http://localhost:8080/api/employee-dashboard/tasks?status=InProgress`
   - Add header: `Authorization: Bearer <your_token>`

## Key Points

✅ **All requests go through the Gateway** (port 8080)  
✅ **JWT token is required** in Authorization header  
✅ **userId is extracted automatically** from the token  
✅ **No need to pass userId** as a parameter  
✅ **Frontend only needs to store and send the token**  

## Common Status Values

### Task Status
- `NotStarted`
- `InProgress`
- `Completed`
- `Blocked`
- `Cancelled`

### Project Status
- `Draft`
- `PendingApproval`
- `InProgress`
- `Completed`
- `Cancelled`

## Troubleshooting

### 401 Unauthorized
- Token is missing or invalid
- Token has expired
- Solution: Login again to get a fresh token

### 403 Forbidden
- User doesn't have EMPLOYEE role
- Solution: Check user role in the system

### Empty Results
- User has no assigned projects/tasks
- This is normal if no assignments exist

### Token Extraction Errors
- Token format must be: `Bearer <token>`
- Don't send just `<token>` without "Bearer" prefix

## Next Steps

1. Integrate these endpoints into your frontend
2. Handle loading states and errors
3. Implement pagination UI
4. Add filtering and search features
5. Consider caching for better performance

## Need More Help?

See `FRONTEND_INTEGRATION_GUIDE.md` for detailed examples and advanced usage.
