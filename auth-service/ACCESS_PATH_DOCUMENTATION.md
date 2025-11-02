# AutoNova Auth Service - Access Path & API Documentation

## 🚀 User Access Flow

### 1. Guest/Anonymous Users (Role: USER - Not Stored in DB)
**Access:** Landing Page + Service Summary (Public Pages)

No authentication required. Users can:
- View landing page
- Browse service summary
- Check available services

---

### 2. Customer Registration & Login Flow

#### Step 1: Signup as Customer
```http
POST /api/users
Content-Type: application/json

{
  "userName": "John Doe",
  "email": "john@example.com",
  "contactOne": "1234567890",
  "password": "securePassword123",
  "role": "CUSTOMER",
  "address": "123 Main St",  // Optional
  "contactTwo": "0987654321"  // Optional
}
```

**Response (201 Created):**
```json
{
  "id": 1,
  "userName": "John Doe",
  "email": "john@example.com",
  "contactOne": "1234567890",
  "role": "CUSTOMER",
  "address": "123 Main St",
  "contactTwo": "0987654321",
  "enabled": true,
  "createdAt": "2024-12-10T10:30:00Z",
  "updatedAt": null
}
```

#### Step 2: Login
```http
POST /api/auth/login
Content-Type: application/json

{
  "email": "john@example.com",
  "password": "securePassword123"
}
```

**Response (200 OK):**
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "userId": 1,
  "userName": "John Doe",
  "email": "john@example.com",
  "role": "CUSTOMER"
}
```

#### Step 3: Access Customer Dashboard
After login, redirect to **Customer Dashboard** with JWT token in Authorization header.

**Dashboard Access:**
```http
GET /api/customer/dashboard
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

---

### 3. Employee Registration & Login Flow

#### Step 1: Signup as Employee
```http
POST /api/users
Content-Type: application/json

{
  "userName": "Jane Smith",
  "email": "jane@autonova.com",
  "contactOne": "5551234567",
  "password": "employeePass456",
  "role": "EMPLOYEE",
  "address": "456 Work Ave"
}
```

**Response (201 Created):**
```json
{
  "id": 2,
  "userName": "Jane Smith",
  "email": "jane@autonova.com",
  "contactOne": "5551234567",
  "role": "EMPLOYEE",
  "address": "456 Work Ave",
  "enabled": true,
  "createdAt": "2024-12-10T11:00:00Z"
}
```

#### Step 2: Login
```http
POST /api/auth/login
Content-Type: application/json

{
  "email": "jane@autonova.com",
  "password": "employeePass456"
}
```

**Response (200 OK):**
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "userId": 2,
  "userName": "Jane Smith",
  "email": "jane@autonova.com",
  "role": "EMPLOYEE"
}
```

#### Step 3: Access Employee Dashboard
After login, redirect to **Employee Dashboard**.

**Dashboard Access:**
```http
GET /api/employee/dashboard
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

---

### 4. Admin Registration & Login Flow

#### Step 1: Signup as Admin
```http
POST /api/users
Content-Type: application/json

{
  "userName": "Admin User",
  "email": "admin@autonova.com",
  "contactOne": "5559876543",
  "password": "adminSecure789",
  "role": "ADMIN"
}
```

**Response (201 Created):**
```json
{
  "id": 3,
  "userName": "Admin User",
  "email": "admin@autonova.com",
  "contactOne": "5559876543",
  "role": "ADMIN",
  "enabled": true,
  "createdAt": "2024-12-10T12:00:00Z"
}
```

#### Step 2: Login
```http
POST /api/auth/login
Content-Type: application/json

{
  "email": "admin@autonova.com",
  "password": "adminSecure789"
}
```

**Response (200 OK):**
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "userId": 3,
  "userName": "Admin User",
  "email": "admin@autonova.com",
  "role": "ADMIN"
}
```

#### Step 3: Access Admin Dashboard
After login, redirect to **Admin Dashboard** with full system access.

**Dashboard Access:**
```http
GET /api/admin/dashboard
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

---

## 🔐 Authentication & Authorization

### JWT Token Structure
The JWT token contains:
- **userId**: User's unique ID
- **email**: User's email
- **role**: User's role (CUSTOMER, EMPLOYEE, or ADMIN)
- **Expiration**: 24 hours (86400000 ms)

### Using JWT Token
Include the token in all authenticated requests:

```http
Authorization: Bearer <your_jwt_token>
```

---

## 📋 Complete API Endpoints

### Public Endpoints (No Authentication Required)

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/auth/login` | User login |
| POST | `/api/users` | User signup (CUSTOMER/EMPLOYEE/ADMIN) |
| GET | `/api/users/email-exists/{email}` | Check if email already exists |

### Protected Endpoints (Authentication Required)

#### User Management

| Method | Endpoint | Required Role | Description |
|--------|----------|---------------|-------------|
| GET | `/api/users` | ADMIN | View all users |
| GET | `/api/users/{id}` | ADMIN or Owner | View specific user |
| GET | `/api/users/email/{email}` | ADMIN | Find user by email |
| PUT | `/api/users/{id}` | ADMIN or Owner | Update user profile |
| DELETE | `/api/users/{id}` | ADMIN | Delete user |
| GET | `/api/users/exists/{id}` | Any registered user | Check if user exists |

---

## 🎭 Role-Based Access Control

### Role Hierarchy (Lowest to Highest)
```
USER (Guest) → CUSTOMER → EMPLOYEE → ADMIN
```

### Permission Matrix

| Feature | CUSTOMER | EMPLOYEE | ADMIN |
|---------|----------|----------|-------|
| View Landing Page | ✅ | ✅ | ✅ |
| Signup/Login | ✅ | ✅ | ✅ |
| View Own Profile | ✅ | ✅ | ✅ |
| Update Own Profile | ✅ | ✅ | ✅ |
| View All Users | ❌ | ❌ | ✅ |
| Delete Any User | ❌ | ❌ | ✅ |
| Access Customer Dashboard | ✅ | ✅ | ✅ |
| Access Employee Dashboard | ❌ | ✅ | ✅ |
| Access Admin Dashboard | ❌ | ❌ | ✅ |

---

## ⚠️ Error Responses

### 400 Bad Request - Invalid Role
```json
{
  "error": "Invalid role. Only CUSTOMER, EMPLOYEE, or ADMIN roles are allowed for signup."
}
```

### 401 Unauthorized - Invalid Credentials
```json
{
  "error": "Invalid email or password"
}
```

### 403 Forbidden - Insufficient Permissions
```json
{
  "error": "Access denied. You don't have permission to access this resource."
}
```

### 404 Not Found - User Not Found
```json
{
  "error": "User not found with id: 123"
}
```

---

## 🧪 Testing with cURL

### 1. Signup as Customer
```bash
curl -X POST http://localhost:8081/api/users \
  -H "Content-Type: application/json" \
  -d '{
    "userName": "Test Customer",
    "email": "customer@test.com",
    "contactOne": "1234567890",
    "password": "password123",
    "role": "CUSTOMER"
  }'
```

### 2. Login
```bash
curl -X POST http://localhost:8081/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "customer@test.com",
    "password": "password123"
  }'
```

### 3. View Own Profile (using token from login)
```bash
curl -X GET http://localhost:8081/api/users/1 \
  -H "Authorization: Bearer <your_token_here>"
```

### 4. Update Own Profile
```bash
curl -X PUT http://localhost:8081/api/users/1 \
  -H "Authorization: Bearer <your_token_here>" \
  -H "Content-Type: application/json" \
  -d '{
    "userName": "Updated Name",
    "address": "New Address 123"
  }'
```

---

## 📝 Important Notes

1. **Only 3 Roles in Database**: CUSTOMER, EMPLOYEE, ADMIN
2. **USER Role**: Reserved for guest/anonymous access (NOT saved in DB)
3. **Password Security**: Passwords are hashed with BCrypt
4. **JWT Expiration**: Tokens expire after 24 hours
5. **Email Uniqueness**: Each email must be unique in the system
6. **Default Role**: If no role specified during user creation internally, CUSTOMER is default (but signup requires explicit role)

---

## 🔄 Frontend Integration

### Recommended Flow

1. **Landing Page** → No authentication
2. **Signup Page** → User selects role (CUSTOMER/EMPLOYEE/ADMIN)
3. **Login Page** → User enters email/password
4. **On Successful Login**:
   - Store JWT token (localStorage/sessionStorage)
   - Decode token to get user role
   - Redirect based on role:
     - CUSTOMER → `/customer-dashboard`
     - EMPLOYEE → `/employee-dashboard`
     - ADMIN → `/admin-dashboard`

### Example Frontend Logic (JavaScript)
```javascript
// After successful login
const response = await fetch('/api/auth/login', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({ email, password })
});

const { token, role } = await response.json();

// Store token
localStorage.setItem('authToken', token);

// Redirect based on role
switch(role) {
  case 'CUSTOMER':
    window.location.href = '/customer-dashboard';
    break;
  case 'EMPLOYEE':
    window.location.href = '/employee-dashboard';
    break;
  case 'ADMIN':
    window.location.href = '/admin-dashboard';
    break;
}
```

---

**Last Updated:** December 2024  
**Auth Service Version:** 1.0  
**Port:** 8081
