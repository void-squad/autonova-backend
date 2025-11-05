# Auth Service - User Management & JWT Authentication API

A complete authentication service with user management CRUD operations and JWT-based authentication for the Autonova backend system.

## Features
- ✅ User Registration (with BCrypt password hashing)
- ✅ JWT Authentication (Login)
- ✅ User CRUD Operations
- ✅ Role-based User Management
- ✅ Secure Password Storage

## Setup

### 1. Create Database in Neon Console
Before running the service, create the database in Neon Console:
- Database name: `user_management_db`
- User: `user_management_service`
- Make sure the user has access to the database

### 2. Run the Service
```bash
cd auth-service
mvnw spring-boot:run
```

The service will run on: `http://localhost:8081`

## API Endpoints

### Base URLs
```
Authentication: http://localhost:8081/api/auth
User Management: http://localhost:8081/api/users
```

---

## 🔐 Authentication Endpoints

### 1. Login (JWT Authentication)
```http
POST /api/auth/login
Content-Type: application/json
```

**Task:** Authenticates user with email and password, returns JWT token

**Request Body:**
```json
{
  "email": "john@example.com",
  "password": "myPassword123"
}
```

**Success Response (200 OK):**
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "type": "Bearer",
  "user": {
    "id": 1,
    "userName": "John Doe",
    "email": "john@example.com",
    "role": "CUSTOMER"
  }
}
```

**Error Response (400 Bad Request):**
```json
{
  "error": "Invalid email or password"
}
```

**Example:**
```bash
curl -X POST http://localhost:8081/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "john@example.com",
    "password": "myPassword123"
  }'
```

**JWT Token Contains:**
- `userId` - User's ID
- `email` - User's email
- `role` - User's role (CUSTOMER, USER, ADMIN, MANAGER)
- `exp` - Token expiration time (24 hours)

**Note:** Save the token and include it in future requests as:
```
Authorization: Bearer <token>
```

---

## 👤 User Management Endpoints

### 1. Get All Users
```http
GET /api/users
```

**Response:**
```json
[
  {
    "id": 1,
    "email": "user@example.com",
    "fullName": "John Doe",
    "role": "USER",
    "enabled": true,
    "createdAt": "2025-11-01T13:30:00Z",
    "updatedAt": null
  }
]
```

### 2. Get User by ID
```http
GET /api/users/{id}
```

**Example:**
```bash
curl http://localhost:8081/api/users/1
```

### 3. Get User by Email
```http
GET /api/users/email/{email}
```

**Example:**
```bash
curl http://localhost:8081/api/users/email/user@example.com
```

### 4. Create New User
```http
POST /api/users
Content-Type: application/json
```

**Required Fields:**
- `userName` - User's full name
- `email` - User's email address (must be unique)
- `contactOne` - Primary contact number
- `password` - User's password
- `role` - User role (default: CUSTOMER)

**Request Body:**
```json
{
  "userName": "Jane Smith",
  "email": "newuser@example.com",
  "contactOne": "+94771234567",
  "password": "securePassword123",
  "role": "CUSTOMER"
}
```

**Example:**
```bash
curl -X POST http://localhost:8081/api/users \
  -H "Content-Type: application/json" \
  -d '{
    "userName": "Test User",
    "email": "test@example.com",
    "contactOne": "+94771234567",
    "password": "myPassword123",
    "role": "CUSTOMER"
  }'
```

**Response:**
```json
{
  "id": 2,
  "userName": "Jane Smith",
  "email": "newuser@example.com",
  "contactOne": "+94771234567",
  "password": "securePassword123",
  "role": "CUSTOMER",
  "address": null,
  "contactTwo": null,
  "enabled": true,
  "createdAt": "2025-11-01T13:35:00Z",
  "updatedAt": null
}
```

### 5. Update User
```http
PUT /api/users/{id}
Content-Type: application/json
```

**Updatable Fields:**
- Basic info: `userName`, `email`, `contactOne`, `password`, `role`
- Additional info: `address`, `contactTwo`

**Request Body (Update basic + additional info):**
```json
{
  "userName": "Updated Name",
  "email": "updated@example.com",
  "contactOne": "+94777654321",
  "password": "newPassword123",
  "role": "ADMIN",
  "address": "123 Main Street, Colombo 00700, Sri Lanka",
  "contactTwo": "+94112345678",
  "enabled": true
}
```

**Example (Update address and second contact):**
```bash
curl -X PUT http://localhost:8081/api/users/1 \
  -H "Content-Type: application/json" \
  -d '{
    "address": "456 New Street, Kandy",
    "contactTwo": "+94112223344"
  }'
```

**Example (Update basic info):**
```bash
curl -X PUT http://localhost:8081/api/users/1 \
  -H "Content-Type: application/json" \
  -d '{
    "userName": "Updated User Name",
    "contactOne": "+94771111111",
    "role": "ADMIN"
  }'
```

### 6. Delete User
```http
DELETE /api/users/{id}
```

**Example:**
```bash
curl -X DELETE http://localhost:8081/api/users/1
```

**Response:**
```json
{
  "success": true,
  "message": "User deleted successfully"
}
```

### 7. Check if User Exists
```http
GET /api/users/exists/{id}
```

**Response:**
```json
{
  "exists": true
}
```

### 8. Check if Email Exists
```http
GET /api/users/email-exists/{email}
```

**Response:**
```json
{
  "exists": false
}
```

## User Roles

Available roles:
- `CUSTOMER` - Customer (default role)
- `USER` - Regular user
- `ADMIN` - Administrator
- `MANAGER` - Manager

## Health Check

Check if the service is running:
```bash
curl http://localhost:8081/actuator/health
```

## 🧪 Complete Testing Workflow

### **Scenario 1: User Registration & Login**

**Step 1: Create a new user (Registration)**
```bash
curl -X POST http://localhost:8081/api/users \
  -H "Content-Type: application/json" \
  -d '{
    "userName": "John Doe",
    "email": "john@example.com",
    "contactOne": "+94771234567",
    "password": "myPassword123",
    "role": "CUSTOMER"
  }'
```

**Expected Response:**
```json
{
  "id": 1,
  "userName": "John Doe",
  "email": "john@example.com",
  "contactOne": "+94771234567",
  "password": "$2a$10$...", // Encrypted password
  "role": "CUSTOMER",
  "address": null,
  "contactTwo": null,
  "enabled": true,
  "createdAt": "2025-11-02T00:00:00Z"
}
```

**Step 2: Login with the created user**
```bash
curl -X POST http://localhost:8081/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "john@example.com",
    "password": "myPassword123"
  }'
```

**Expected Response:**
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9.eyJ1c2VySWQiOjEsImVtYWlsIjoiam9obkBleGFtcGxlLmNvbSIsInJvbGUiOiJDVVNUT01FUiIsInN1YiI6ImpvaG5AZXhhbXBsZS5jb20iLCJpYXQiOjE2OTg4MDAwMDAsImV4cCI6MTY5ODg4NjQwMH0.xyz...",
  "type": "Bearer",
  "user": {
    "id": 1,
    "userName": "John Doe",
    "email": "john@example.com",
    "role": "CUSTOMER"
  }
}
```

**Step 3: Use the JWT token (Future Protected Routes)**
```bash
curl -X GET http://localhost:8081/api/users/1 \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9..."
```

---

### **Scenario 2: Update User Profile**

**Step 1: Update user with address and second contact**
```bash
curl -X PUT http://localhost:8081/api/users/1 \
  -H "Content-Type: application/json" \
  -d '{
    "address": "123 Main Street, Colombo 00700",
    "contactTwo": "+94112345678"
  }'
```

**Step 2: Get updated user details**
```bash
curl http://localhost:8081/api/users/1
```

---

### **Scenario 3: Test Invalid Login**

**Wrong password:**
```bash
curl -X POST http://localhost:8081/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "john@example.com",
    "password": "wrongPassword"
  }'
```

**Expected Response (400 Bad Request):**
```json
{
  "error": "Invalid email or password"
}
```

---

### **Scenario 4: Full CRUD Workflow**
1. **Create a user** → `POST /api/users`
2. **Login** → `POST /api/auth/login` (Get JWT token)
3. **Get all users** → `GET /api/users`
4. **Get user by ID** → `GET /api/users/1`
5. **Update user** → `PUT /api/users/1`
6. **Check email exists** → `GET /api/users/email-exists/john@example.com`
7. **Delete user** → `DELETE /api/users/1`

## Common Issues

### Database Connection Error
If you see database connection errors:
1. Make sure `user_management_db` is created in Neon Console
2. Verify database credentials in `application.properties`
3. Check network connectivity to Neon

### Port Already in Use
If port 8081 is already in use, change it in `application.properties`:
```properties
server.port=8082
```

## Database Schema

The `users` table is automatically created by JPA with the following structure:

```sql
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    user_name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    contact_one VARCHAR(20) NOT NULL,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL DEFAULT 'CUSTOMER',
    address VARCHAR(500),
    contact_two VARCHAR(20),
    enabled BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP
);
```

**Required fields for creating user:**
- `user_name` - User's full name
- `email` - Unique email address
- `contact_one` - Primary contact number
- `password` - User password
- `role` - User role (default: CUSTOMER)

**Optional fields (can be added later via update):**
- `address` - User's address
- `contact_two` - Secondary contact number

---

## 🔒 Security Features

### Password Security
- ✅ Passwords are hashed using **BCrypt** algorithm
- ✅ Passwords are never stored in plain text
- ✅ Passwords are hashed on user creation and updates
- ✅ Password validation uses secure comparison

### JWT Token Security
- ✅ Tokens are signed with HS256 algorithm
- ✅ Secret key is Base64 encoded
- ✅ Token expiration: **24 hours** (86400000 ms)
- ✅ Token contains: userId, email, role
- ✅ Tokens are validated on protected routes

### Current Authentication State
- ⚠️ **Security is currently DISABLED** for testing
- ⚠️ All endpoints are **publicly accessible** 
- 🔄 **Next Step:** Enable Spring Security with JWT filter for protected routes

---

## 📝 Important Notes

1. **Passwords are encrypted:**
   - When you create a user with password `"myPassword123"`
   - It's stored as `"$2a$10$abcdef123456..."` in the database
   - You must use the original password for login

2. **JWT Token Usage:**
   - Token expires after 24 hours
   - Include token in protected requests: `Authorization: Bearer <token>`
   - Token contains user info (don't need to query DB for every request)

3. **Role-Based Access:**
   - Roles: CUSTOMER, USER, ADMIN, MANAGER
   - Default role: CUSTOMER
   - Can be used for future authorization

4. **Email Uniqueness:**
   - Emails must be unique
   - System checks for duplicate emails on registration and update

---

## 🚀 Quick Start Commands

**1. Create a test user:**
```bash
curl -X POST http://localhost:8081/api/users -H "Content-Type: application/json" -d '{"userName":"Test User","email":"test@example.com","contactOne":"+94771234567","password":"test123","role":"CUSTOMER"}'
```

**2. Login:**
```bash
curl -X POST http://localhost:8081/api/auth/login -H "Content-Type: application/json" -d '{"email":"test@example.com","password":"test123"}'
```

**3. Get all users:**
```bash
curl http://localhost:8081/api/users
```

---

## 🎯 API Summary

| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| POST | `/api/auth/login` | Login & get JWT token | No |
| POST | `/api/users` | Create new user | No (for now) |
| GET | `/api/users` | Get all users | No (for now) |
| GET | `/api/users/{id}` | Get user by ID | No (for now) |
| GET | `/api/users/email/{email}` | Get user by email | No (for now) |
| PUT | `/api/users/{id}` | Update user | No (for now) |
| DELETE | `/api/users/{id}` | Delete user | No (for now) |
| GET | `/api/users/exists/{id}` | Check if user exists | No (for now) |
| GET | `/api/users/email-exists/{email}` | Check if email exists | No (for now) |
| GET | `/actuator/health` | Health check | No |

---

**Authentication is ready! 🎉**

Next steps for production:
1. Enable Spring Security with JWT filter
2. Protect endpoints based on roles
3. Add refresh token mechanism
4. Add rate limiting for login attempts
