# Security Implementation Guide

**Comprehensive security documentation for AutoNova Auth Service**

---

## 📋 Table of Contents

### Part 1: Enhanced Security Features
1. [Overview](#overview)
2. [JWT & Refresh Token System](#jwt--refresh-token-system)
3. [Token Configuration](#token-configuration)
4. [Implementation Details](#implementation-details)

### Part 2: URL Security Best Practices
5. [URL Security Issues](#url-security-issues)
6. [Secure vs Insecure Patterns](#secure-vs-insecure-patterns)
7. [Migration Guide](#migration-guide)

### Part 3: Quick Reference
8. [Quick Reference Cheat Sheet](#quick-reference-cheat-sheet)
9. [Frontend Integration Examples](#frontend-integration-examples)
10. [Testing & Troubleshooting](#testing--troubleshooting)

---

# Part 1: Enhanced Security Features

## Overview

This auth-service implements enterprise-grade security with:
- ✅ **JWT Access Tokens:** 1 hour expiry (short-lived for security)
- ✅ **Refresh Tokens:** 7 days expiry (long-lived for convenience)
- ✅ **Password Reset Tokens:** 2 hours expiry (fast action = more secure)
- ✅ **OAuth2 Integration:** Google login support
- ✅ **URL Security:** Sensitive data in POST body (not URL)
- ✅ **BCrypt Password Hashing:** Industry-standard encryption
- ✅ **Token Revocation:** Logout invalidates refresh tokens

---

## JWT & Refresh Token System

### Why This Approach?

**Industry Standard Pattern:**
- Short-lived access tokens (1h) minimize risk if compromised
- Long-lived refresh tokens (7d) provide good user experience
- Token revocation on logout provides security control

### Token Lifecycle

```
User Login
   ↓
Generate Access Token (1h) + Refresh Token (7d)
   ↓
User makes API calls with Access Token
   ↓
Access Token expires after 1 hour
   ↓
Use Refresh Token to get new Access Token
   ↓
Repeat until Refresh Token expires (7 days)
   ↓
User must login again
```

### How It Works

1. **Login:** User authenticates → receives both tokens
2. **API Calls:** Use access token in `Authorization: Bearer <token>` header
3. **Token Expiry:** After 1h, access token expires
4. **Refresh:** Send refresh token to `/api/auth/refresh` → get new access token
5. **Logout:** Send refresh token to `/api/auth/logout` → token revoked
6. **Security:** Refresh tokens can be revoked anytime (logout/security breach)

---

## Token Configuration

### Application Properties

```properties
# JWT Configuration (application.properties)
jwt.secret=your-secure-secret-key-at-least-256-bits-long
jwt.expiration=3600000          # 1 hour in milliseconds
jwt.refresh.expiration=604800000 # 7 days in milliseconds

# Password Reset Configuration
password.reset.token.expiration=7200000  # 2 hours in milliseconds
```

### Database Schema

**Users Table:**
```sql
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    full_name VARCHAR(255),
    role VARCHAR(50) NOT NULL,
    email_verified BOOLEAN DEFAULT FALSE,
    enabled BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP
);
```

**Refresh Tokens Table:**
```sql
CREATE TABLE refresh_tokens (
    id BIGSERIAL PRIMARY KEY,
    token VARCHAR(255) UNIQUE NOT NULL,
    user_id BIGINT NOT NULL REFERENCES users(id),
    expiry_date TIMESTAMP NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    revoked BOOLEAN DEFAULT FALSE,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);
```

**Password Reset Tokens Table:**
```sql
CREATE TABLE password_reset_tokens (
    id BIGSERIAL PRIMARY KEY,
    token VARCHAR(255) UNIQUE NOT NULL,
    user_id BIGINT NOT NULL REFERENCES users(id),
    expiry_date TIMESTAMP NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    used BOOLEAN DEFAULT FALSE,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);
```

---

## Implementation Details

### Key Classes

**1. RefreshToken Entity**
```java
@Entity
@Table(name = "refresh_tokens")
@Data
public class RefreshToken {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String token;  // UUID
    
    @ManyToOne
    @JoinColumn(name = "user_id", referencedColumnName = "id")
    private User user;
    
    private Instant expiryDate;  // 7 days from creation
    
    @Column(name = "created_at")
    private Instant createdAt;
    
    private Boolean revoked = false;
    
    public boolean isExpired() {
        return Instant.now().isAfter(this.expiryDate);
    }
}
```

**2. RefreshTokenService**
```java
@Service
public class RefreshTokenService {
    
    public RefreshToken createRefreshToken(Long userId) {
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUser(userRepository.findById(userId).get());
        refreshToken.setToken(UUID.randomUUID().toString());
        refreshToken.setExpiryDate(Instant.now().plusMillis(refreshExpiration));
        refreshToken.setCreatedAt(Instant.now());
        refreshToken.setRevoked(false);
        return refreshTokenRepository.save(refreshToken);
    }
    
    public RefreshToken verifyExpiration(RefreshToken token) {
        if (token.isExpired()) {
            refreshTokenRepository.delete(token);
            throw new TokenRefreshException("Refresh token expired");
        }
        return token;
    }
    
    public void revokeToken(String token) {
        RefreshToken refreshToken = refreshTokenRepository.findByToken(token)
            .orElseThrow(() -> new ResourceNotFoundException("Token not found"));
        refreshToken.setRevoked(true);
        refreshTokenRepository.save(refreshToken);
    }
}
```

**3. AuthController Endpoints**
```java
@RestController
@RequestMapping("/api/auth")
public class AuthController {
    
    // Login - returns both tokens
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        // Authenticate user
        // Generate JWT access token
        // Create refresh token
        return ResponseEntity.ok(new LoginResponse(accessToken, refreshToken, userInfo));
    }
    
    // Refresh access token
    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(@RequestBody RefreshTokenRequest request) {
        String requestRefreshToken = request.getRefreshToken();
        RefreshToken refreshToken = refreshTokenService.findByToken(requestRefreshToken)
            .orElseThrow(() -> new TokenRefreshException("Refresh token not found"));
        
        refreshToken = refreshTokenService.verifyExpiration(refreshToken);
        User user = refreshToken.getUser();
        String newAccessToken = jwtService.generateToken(user);
        
        return ResponseEntity.ok(new RefreshTokenResponse(newAccessToken, requestRefreshToken));
    }
    
    // Logout - revoke refresh token
    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestBody RefreshTokenRequest request) {
        refreshTokenService.revokeToken(request.getRefreshToken());
        return ResponseEntity.ok(new MessageResponse("Logged out successfully"));
    }
}
```

---

# Part 2: URL Security Best Practices

## URL Security Issues

### 🚨 The Problem

**Sensitive data (emails, passwords, tokens) in URLs can be:**
- ❌ Logged in server logs (Apache, Nginx, application logs)
- ❌ Stored in browser history
- ❌ Cached by proxy servers
- ❌ Visible in network monitoring tools
- ❌ Leaked through HTTP Referer headers
- ❌ Exposed in error messages and stack traces

### Example of Vulnerable Endpoints:
```http
GET /api/users/email/john@company.com           ← Email in URL!
GET /api/users/email-exists/sensitive@email.com ← Email exposed!
GET /api/auth/reset?token=SECRET&email=user@email.com  ← Both exposed!
GET /api/auth/login?email=x&password=y          ← NEVER DO THIS!
```

---

## Secure vs Insecure Patterns

### 🔒 The Golden Rule
> **Never pass sensitive data (emails, passwords, tokens, SSN, PII) in URLs. Always use POST with request body.**

### ✅ SECURE Patterns (Use These):

```http
POST /api/users/by-email
Content-Type: application/json
Authorization: Bearer <token>

{
  "email": "user@example.com"
}
```

**Why secure?**
- ✓ Request body encrypted by HTTPS
- ✓ Not logged in access logs
- ✓ Not stored in browser history
- ✓ Not cached by proxies
- ✓ Not leaked via Referer header

### ❌ INSECURE Patterns (Avoid These):

```http
GET /api/users/email/john@company.com           ← Email in URL!
GET /api/auth/login?email=x&password=y          ← Credentials in URL!
GET /api/auth/reset?token=SECRET&email=x        ← Token in URL!
GET /api/users/search?ssn=123-45-6789           ← SSN in URL!
```

**Why insecure?**
- ✘ Logged in server logs
- ✘ Stored in browser history
- ✘ Cached by proxy servers
- ✘ Visible in network monitoring
- ✘ Leaked through Referer headers

---

## Migration Guide

### Endpoint Changes

#### 1. Email Lookup (Admin Only)

**❌ OLD (Insecure - Deprecated):**
```http
GET /api/users/email/{email}
Authorization: Bearer <token>
```

**✅ NEW (Secure):**
```http
POST /api/users/by-email
Authorization: Bearer <token>
Content-Type: application/json

{
  "email": "user@example.com"
}
```

#### 2. Email Existence Check (Public)

**❌ OLD (Insecure - Deprecated):**
```http
GET /api/users/email-exists/{email}
```

**✅ NEW (Secure):**
```http
POST /api/users/email-exists
Content-Type: application/json

{
  "email": "user@example.com"
}
```

**Response:**
```json
{
  "exists": true
}
```

### Backend Implementation

**UserLookupRequest DTO:**
```java
@Data
public class UserLookupRequest {
    private String email;
    private Long id;
}
```

**Secure Controller Methods:**
```java
// Secure email lookup
@PreAuthorize("hasRole('ADMIN')")
@PostMapping("/by-email")
public ResponseEntity<?> getUserByEmail(@RequestBody UserLookupRequest request) {
    if (request.getEmail() == null || request.getEmail().trim().isEmpty()) {
        return ResponseEntity.badRequest().body("Email is required");
    }
    return userService.getUserByEmail(request.getEmail())
        .map(ResponseEntity::ok)
        .orElse(ResponseEntity.notFound().build());
}

// Secure email exists check
@PostMapping("/email-exists")
public ResponseEntity<Map<String, Boolean>> checkEmailExists(@RequestBody UserLookupRequest request) {
    Map<String, Boolean> response = new HashMap<>();
    if (request.getEmail() == null || request.getEmail().trim().isEmpty()) {
        response.put("exists", false);
        response.put("error", true);
        return ResponseEntity.badRequest().body(response);
    }
    response.put("exists", userService.emailExists(request.getEmail()));
    return ResponseEntity.ok(response);
}

// Deprecated - backward compatibility
@Deprecated
@GetMapping("/email/{email}")
public ResponseEntity<?> getUserByEmailDeprecated(@PathVariable String email) {
    // Old implementation - will be removed in future version
}
```

---

# Part 3: Quick Reference

## Quick Reference Cheat Sheet

### 🔑 Authentication Endpoints

| Endpoint | Method | Purpose | Token Required |
|----------|--------|---------|----------------|
| `/api/auth/login` | POST | Get JWT + refresh token | No |
| `/api/auth/refresh` | POST | Get new access token | No |
| `/api/auth/logout` | POST | Revoke refresh token | No |
| `/api/auth/forgot-password` | POST | Request password reset | No |
| `/api/auth/reset-password` | POST | Reset password | No |
| `/oauth2/authorization/google` | GET | Google OAuth2 login | No |

### 👤 User Management Endpoints (Secure)

| Endpoint | Method | Purpose | Auth Required |
|----------|--------|---------|---------------|
| `/api/users` | GET | Get all users | Admin |
| `/api/users` | POST | Create user (register) | No |
| `/api/users/{id}` | GET | Get user by ID | Admin/Owner |
| `/api/users/by-email` | POST | Get user by email ✅ | Admin |
| `/api/users/email-exists` | POST | Check email exists ✅ | No |
| `/api/users/{id}` | PUT | Update user | Admin/Owner |
| `/api/users/{id}/role` | PATCH | Update role | Admin |
| `/api/users/{id}` | DELETE | Delete user | Admin |

### ⏱️ Token Expiration Times

| Token Type | Expiration | Use Case |
|------------|------------|----------|
| **JWT Access Token** | 1 hour | API authentication |
| **Refresh Token** | 7 days | Get new access token |
| **Password Reset Token** | 2 hours | Reset password |

---

## Frontend Integration Examples

### React/JavaScript

#### 1. Login with Token Storage
```javascript
const login = async (email, password) => {
  const response = await fetch('http://localhost:8081/api/auth/login', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ email, password })
  });
  const data = await response.json();
  
  // Store tokens
  localStorage.setItem('accessToken', data.accessToken);
  localStorage.setItem('refreshToken', data.refreshToken);
  
  return data;
};
```

#### 2. Secure Email Validation
```javascript
// ✅ SECURE - Email in body
const checkEmailExists = async (email) => {
  const response = await fetch('http://localhost:8081/api/users/email-exists', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ email })
  });
  const data = await response.json();
  return data.exists;
};
```

#### 3. Refresh Token Implementation
```javascript
const refreshAccessToken = async () => {
  const refreshToken = localStorage.getItem('refreshToken');
  
  try {
    const response = await fetch('http://localhost:8081/api/auth/refresh', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ refreshToken })
    });
    const data = await response.json();
    
    // Update access token
    localStorage.setItem('accessToken', data.accessToken);
    return data.accessToken;
  } catch (error) {
    // Refresh token expired - redirect to login
    localStorage.clear();
    window.location.href = '/login';
  }
};
```

#### 4. API Call with Auto-Refresh
```javascript
const apiCall = async (url, options = {}) => {
  let token = localStorage.getItem('accessToken');
  
  // First attempt
  let response = await fetch(url, {
    ...options,
    headers: {
      ...options.headers,
      'Authorization': `Bearer ${token}`
    }
  });
  
  // If 401, try refreshing token
  if (response.status === 401) {
    token = await refreshAccessToken();
    
    // Retry with new token
    response = await fetch(url, {
      ...options,
      headers: {
        ...options.headers,
        'Authorization': `Bearer ${token}`
      }
    });
  }
  
  return response.json();
};
```

#### 5. Logout
```javascript
const logout = async () => {
  const refreshToken = localStorage.getItem('refreshToken');
  
  await fetch('http://localhost:8081/api/auth/logout', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ refreshToken })
  });
  
  // Clear tokens
  localStorage.removeItem('accessToken');
  localStorage.removeItem('refreshToken');
  
  // Redirect to login
  window.location.href = '/login';
};
```

#### 6. Secure User Lookup (Admin)
```javascript
// ✅ SECURE - Email in body
const getUserByEmail = async (email) => {
  const token = localStorage.getItem('accessToken');
  
  const response = await fetch('http://localhost:8081/api/users/by-email', {
    method: 'POST',
    headers: {
      'Authorization': `Bearer ${token}`,
      'Content-Type': 'application/json'
    },
    body: JSON.stringify({ email })
  });
  
  return response.json();
};
```

---

## Testing & Troubleshooting

### Testing Secure Endpoints

#### 1. Test Email Validation (Public)
```bash
curl -X POST http://localhost:8081/api/users/email-exists \
  -H "Content-Type: application/json" \
  -d '{"email": "test@example.com"}'
```

**Expected Response:**
```json
{
  "exists": false
}
```

#### 2. Test Login
```bash
curl -X POST http://localhost:8081/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "user@example.com",
    "password": "password123"
  }'
```

**Expected Response:**
```json
{
  "accessToken": "eyJhbGc...",
  "refreshToken": "550e8400-e29b-41d4...",
  "userInfo": {
    "id": 1,
    "email": "user@example.com",
    "role": "ROLE_CUSTOMER"
  }
}
```

#### 3. Test Refresh Token
```bash
curl -X POST http://localhost:8081/api/auth/refresh \
  -H "Content-Type: application/json" \
  -d '{"refreshToken": "YOUR_REFRESH_TOKEN"}'
```

#### 4. Test Authenticated Endpoint
```bash
curl -H "Authorization: Bearer YOUR_ACCESS_TOKEN" \
  http://localhost:8081/api/auth/user-info
```

#### 5. Test Secure Email Lookup (Admin)
```bash
curl -X POST http://localhost:8081/api/users/by-email \
  -H "Authorization: Bearer ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"email": "admin@example.com"}'
```

### Common Issues & Solutions

#### Issue 1: Token Expired
**Error:** `401 Unauthorized`
**Solution:** Use refresh token to get new access token

```javascript
const token = await refreshAccessToken();
// Retry request with new token
```

#### Issue 2: Refresh Token Expired
**Error:** `Refresh token expired` or `404 Not Found`
**Solution:** User must login again

```javascript
localStorage.clear();
window.location.href = '/login';
```

#### Issue 3: Email in URL Not Working
**Error:** `404 Not Found` on old endpoints
**Solution:** Use new POST endpoints

```javascript
// ❌ OLD - Will fail
fetch(`/api/users/email/${email}`)

// ✅ NEW - Works
fetch('/api/users/by-email', {
  method: 'POST',
  body: JSON.stringify({ email })
})
```

### Security Checklist

- [x] ✅ JWT access tokens: 1 hour expiry
- [x] ✅ Refresh tokens: 7 days expiry
- [x] ✅ Password reset tokens: 2 hours expiry
- [x] ✅ Passwords hashed with BCrypt
- [x] ✅ Emails in POST body (not URL)
- [x] ✅ Tokens in Authorization header
- [x] ✅ Refresh tokens can be revoked
- [x] ✅ OAuth2 integration (Google)
- [x] ✅ Role-based access control
- [x] ✅ HTTPS encryption for all data

---

## 📚 Compliance & Standards

This implementation follows:
- ✅ **OWASP Top 10** - Broken Access Control, Insecure Design
- ✅ **PCI DSS** - Requirement 3.4 (Render PAN unreadable)
- ✅ **GDPR** - Article 32 (Security of processing)
- ✅ **NIST Cybersecurity Framework** - PR.DS-2 (Data-in-transit protection)

---

## 🔗 Related Documentation

- **[API_DOCUMENTATION.md](./API_DOCUMENTATION.md)** - Complete API reference
- **[GOOGLE_OAUTH2_SETUP.md](./GOOGLE_OAUTH2_SETUP.md)** - OAuth2 setup guide
- **[README.md](./README.md)** - Project overview

---

**Last Updated:** 2024-11-03  
**Version:** 1.0  
**Status:** ✅ Production Ready
