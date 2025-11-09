# Architecture Diagram

## System Overview

```
┌─────────────────────────────────────────────────────────────────────┐
│                            FRONTEND                                  │
│                     (React/Vue/Angular/etc.)                        │
│                                                                      │
│  Stores: JWT Token + User Info after login                          │
│  Calls: http://localhost:8080/api/employee-dashboard/*             │
└──────────────────────┬───────────────────────────────────────────────┘
                       │
                       │ HTTP Request
                       │ Header: Authorization: Bearer <JWT>
                       │
                       ▼
┌─────────────────────────────────────────────────────────────────────┐
│                       GATEWAY SERVICE                                │
│                      (Spring Cloud Gateway)                          │
│                       Port: 8080                                     │
│                                                                      │
│  Routes:                                                             │
│  ┌────────────────────────────────────────────────────────────┐   │
│  │ /api/employee-dashboard/**                                  │   │
│  │   → http://localhost:8084/api/employee/dashboard/**        │   │
│  │                                                             │   │
│  │ /api/projects/**                                            │   │
│  │   → http://localhost:8082/api/projects/**                  │   │
│  │                                                             │   │
│  │ /api/tasks/**                                               │   │
│  │   → http://localhost:8082/api/tasks/**                     │   │
│  └────────────────────────────────────────────────────────────┘   │
└──────────────┬──────────────────────────────────┬────────────────────┘
               │                                  │
               │                                  │
               ▼                                  ▼
┌────────────────────────────────┐   ┌────────────────────────────────┐
│  EMPLOYEE DASHBOARD SERVICE    │   │      PROJECT SERVICE           │
│  (Spring Boot - Java)          │   │      (.NET Core - C#)          │
│  Port: 8084                    │   │      Port: 8082                │
│                                │   │                                │
│  ┌──────────────────────────┐ │   │  ┌──────────────────────────┐ │
│  │ Controller Layer         │ │   │  │ Controller Layer         │ │
│  │ - GET /projects          │ │   │  │ - GET /api/projects      │ │
│  │ - GET /tasks             │ │   │  │ - GET /api/tasks         │ │
│  │                          │ │   │  │                          │ │
│  │ Extracts userId from JWT │ │   │  │ Validates JWT token      │ │
│  └──────────┬───────────────┘ │   │  └──────────┬───────────────┘ │
│             │                  │   │             │                  │
│             ▼                  │   │             ▼                  │
│  ┌──────────────────────────┐ │   │  ┌──────────────────────────┐ │
│  │ JwtService               │ │   │  │ Service Layer            │ │
│  │ - extractUserId(token)   │ │   │  │ - GetProjects()          │ │
│  └──────────┬───────────────┘ │   │  │ - GetTasks()             │ │
│             │                  │   │  └──────────┬───────────────┘ │
│             ▼                  │   │             │                  │
│  ┌──────────────────────────┐ │   │             ▼                  │
│  │ ProjectServiceClient     │ │   │  ┌──────────────────────────┐ │
│  │ - getProjectsByAssignee()│─┼───┼─▶│ Database (PostgreSQL)    │ │
│  │ - getTasksByAssignee()   │ │   │  │ - Projects Table         │ │
│  │   (calls via Gateway)    │ │   │  │ - Tasks Table            │ │
│  └──────────────────────────┘ │   │  └──────────────────────────┘ │
└────────────────────────────────┘   └────────────────────────────────┘
```

## Request Flow Diagram

### 1. Login Flow

```
Frontend                 Gateway              Auth Service
   │                        │                      │
   │  POST /api/auth/login  │                      │
   ├───────────────────────►│                      │
   │                        │  Forward Request     │
   │                        ├─────────────────────►│
   │                        │                      │ Validate
   │                        │                      │ Credentials
   │                        │                      │
   │                        │   JWT Token +        │
   │                        │   User Info          │
   │                        │◄─────────────────────┤
   │   Response:            │                      │
   │   - token              │                      │
   │   - userId             │                      │
   │   - email              │                      │
   │   - role               │                      │
   │◄───────────────────────┤                      │
   │                        │                      │
   │  Store token           │                      │
   │  and userId            │                      │
   │                        │                      │
```

### 2. Get Projects Flow

```
Frontend          Gateway         Employee Dashboard       Gateway         Project Service
   │                 │                    │                   │                    │
   │ GET /projects   │                    │                   │                    │
   │ + JWT Token     │                    │                   │                    │
   ├────────────────►│                    │                   │                    │
   │                 │  Forward Request   │                   │                    │
   │                 ├───────────────────►│                   │                    │
   │                 │                    │                   │                    │
   │                 │                    │ Extract userId    │                    │
   │                 │                    │ from JWT          │                    │
   │                 │                    │                   │                    │
   │                 │                    │ Call via Gateway  │                    │
   │                 │                    │ GET /api/projects │                    │
   │                 │                    │ ?assigneeId=123   │                    │
   │                 │                    │ + JWT Token       │                    │
   │                 │                    ├──────────────────►│                    │
   │                 │                    │                   │  Forward Request   │
   │                 │                    │                   ├───────────────────►│
   │                 │                    │                   │                    │
   │                 │                    │                   │  Query Database    │
   │                 │                    │                   │  WHERE assigneeId  │
   │                 │                    │                   │                    │
   │                 │                    │                   │  Project Data      │
   │                 │                    │                   │◄───────────────────┤
   │                 │                    │  Project Data     │                    │
   │                 │                    │◄──────────────────┤                    │
   │                 │  Project Data      │                   │                    │
   │                 │◄───────────────────┤                   │                    │
   │  Project Data   │                    │                   │                    │
   │◄────────────────┤                    │                   │                    │
   │                 │                    │                   │                    │
   │  Display        │                    │                   │                    │
   │  in UI          │                    │                   │                    │
   │                 │                    │                   │                    │
```

### 3. Get Tasks Flow (Similar to Projects)

```
Frontend          Gateway         Employee Dashboard       Gateway         Project Service
   │                 │                    │                   │                    │
   │ GET /tasks      │                    │                   │                    │
   │ + JWT Token     │                    │                   │                    │
   ├────────────────►│                    │                   │                    │
   │                 ├───────────────────►│                   │                    │
   │                 │                    │ Extract userId    │                    │
   │                 │                    ├──────────────────►│                    │
   │                 │                    │                   ├───────────────────►│
   │                 │                    │                   │  Query Tasks       │
   │                 │                    │                   │◄───────────────────┤
   │                 │                    │◄──────────────────┤                    │
   │                 │◄───────────────────┤                   │                    │
   │◄────────────────┤                    │                   │                    │
   │  Display        │                    │                   │                    │
   │  in UI          │                    │                   │                    │
```

## JWT Token Structure

```
┌─────────────────────────────────────────┐
│          JWT Token Header               │
├─────────────────────────────────────────┤
│ {                                       │
│   "alg": "HS256",                       │
│   "typ": "JWT"                          │
│ }                                       │
└─────────────────────────────────────────┘
               ▼
┌─────────────────────────────────────────┐
│          JWT Token Payload              │
├─────────────────────────────────────────┤
│ {                                       │
│   "userId": 123,         ◄─── Extracted │
│   "email": "...",                       │
│   "role": "EMPLOYEE",                   │
│   "firstName": "John",                  │
│   "sub": "email@...",                   │
│   "iat": 1699430000,                    │
│   "exp": 1699516400                     │
│ }                                       │
└─────────────────────────────────────────┘
               ▼
┌─────────────────────────────────────────┐
│          JWT Token Signature            │
├─────────────────────────────────────────┤
│ HMACSHA256(                             │
│   base64UrlEncode(header) + "." +       │
│   base64UrlEncode(payload),             │
│   secret                                │
│ )                                       │
└─────────────────────────────────────────┘
```

## Data Flow for User ID

```
                    LOGIN
                      │
                      ▼
    ┌───────────────────────────────────┐
    │   Auth Service generates JWT      │
    │   with userId claim               │
    └───────────┬───────────────────────┘
                │
                ▼
    ┌───────────────────────────────────┐
    │   Frontend stores JWT token       │
    │   (doesn't need to extract userId)│
    └───────────┬───────────────────────┘
                │
                ▼
    ┌───────────────────────────────────┐
    │   Frontend sends request          │
    │   with JWT in header              │
    └───────────┬───────────────────────┘
                │
                ▼
    ┌───────────────────────────────────┐
    │   Employee Dashboard extracts     │
    │   userId from JWT automatically   │
    └───────────┬───────────────────────┘
                │
                ▼
    ┌───────────────────────────────────┐
    │   Convert userId (Long → String)  │
    │   123 → "123"                     │
    └───────────┬───────────────────────┘
                │
                ▼
    ┌───────────────────────────────────┐
    │   Pass to Project Service         │
    │   as query parameter              │
    └───────────┬───────────────────────┘
                │
                ▼
    ┌───────────────────────────────────┐
    │   Project Service queries DB      │
    │   WHERE assigneeId = userId       │
    └───────────────────────────────────┘
```

## Technology Stack

```
┌─────────────────────────────────────────┐
│            FRONTEND LAYER               │
│  - React/Vue/Angular/etc.               │
│  - HTTP Client (fetch/axios)            │
│  - JWT Token Storage                    │
└──────────────┬──────────────────────────┘
               │
               ▼
┌─────────────────────────────────────────┐
│         GATEWAY LAYER                   │
│  - Spring Cloud Gateway                 │
│  - Routing & Load Balancing             │
│  - CORS Configuration                   │
└──────────────┬──────────────────────────┘
               │
        ┌──────┴──────┐
        ▼             ▼
┌──────────────┐  ┌──────────────┐
│   BFF LAYER  │  │ SERVICE      │
│              │  │ LAYER        │
│ - Spring     │  │              │
│   Boot       │  │ - .NET Core  │
│ - WebFlux    │  │ - EF Core    │
│ - JWT        │  │ - PostgreSQL │
└──────────────┘  └──────────────┘
```

## Key Takeaways

1. **No Manual User ID Passing**
   - Frontend: Just send JWT token
   - Backend: Extracts userId automatically

2. **Gateway as Central Hub**
   - All requests go through gateway
   - Single entry point
   - Consistent routing

3. **Security**
   - JWT validation on every request
   - Role-based access control
   - Token expiration handling

4. **Reactive Architecture**
   - Non-blocking I/O
   - Better scalability
   - Efficient resource usage
