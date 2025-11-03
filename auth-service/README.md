# AutoNova Auth Service

**Enterprise-grade authentication and user management microservice**

[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.3-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Java](https://img.shields.io/badge/Java-17-orange.svg)](https://www.oracle.com/java/)
[![Security](https://img.shields.io/badge/Security-JWT%20%2B%20OAuth2-blue.svg)](https://jwt.io/)

---

## 🚀 Features

- ✅ **JWT Authentication** - 1 hour access tokens + 7 days refresh tokens
- ✅ **OAuth2 Integration** - Google login with automatic user creation
- ✅ **Password Reset** - Email-based password reset (2-hour token expiry)
- ✅ **Role-Based Access Control** - CUSTOMER, EMPLOYEE, ADMIN roles
- ✅ **Enterprise Security** - Sensitive data in POST body (not URLs)
- ✅ **BCrypt Password Hashing** - Industry-standard encryption
- ✅ **Token Revocation** - Logout invalidates refresh tokens
- ✅ **Email Service** - Gmail SMTP with HTML templates

---

## 📋 Quick Start

### 1. Prerequisites
- Java 17+
- PostgreSQL database (Neon Cloud recommended)
- Maven 3.6+
- Gmail account (for email service)

### 2. Configuration

Create `application-local.properties`:
```properties
# Database
spring.datasource.url=jdbc:postgresql://your-neon-db.neon.tech/user_management_db
spring.datasource.username=your-username
spring.datasource.password=your-password

# JWT
jwt.secret=your-secret-key-at-least-256-bits-long
jwt.expiration=3600000
jwt.refresh.expiration=604800000

# Email
spring.mail.username=your-email@gmail.com
spring.mail.password=your-app-password

# Google OAuth2 (optional)
spring.security.oauth2.client.registration.google.client-id=${GOOGLE_CLIENT_ID}
spring.security.oauth2.client.registration.google.client-secret=${GOOGLE_CLIENT_SECRET}
```

### 3. Run the Service
```bash
# With local profile
.\mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=local

# Or compile and run
.\mvnw.cmd clean install
java -jar target/auth-service-0.0.1-SNAPSHOT.jar
```

Service will run on: **http://localhost:8081**

---

## 📚 Documentation

| Document | Description |
|----------|-------------|
| **[API_DOCUMENTATION.md](./API_DOCUMENTATION.md)** | Complete API reference with all endpoints |
| **[AUTHENTICATION_INTEGRATION_GUIDE.md](./AUTHENTICATION_INTEGRATION_GUIDE.md)** | 🆕 Frontend integration guide (React) |
| **[ROLE_BASED_ACCESS_GUIDE.md](./ROLE_BASED_ACCESS_GUIDE.md)** | 🆕 Role-based access control implementation |
| **[SECURITY_GUIDE.md](./SECURITY_GUIDE.md)** | Security implementation & best practices |
| **[GOOGLE_OAUTH2_SETUP.md](./GOOGLE_OAUTH2_SETUP.md)** | Google OAuth2 setup guide |

---

## 🔑 Key Endpoints

### Authentication
```bash
# Login
POST /api/auth/login
Body: {"email": "user@example.com", "password": "password123"}

# Refresh token
POST /api/auth/refresh
Body: {"refreshToken": "your-refresh-token"}

# Logout
POST /api/auth/logout
Body: {"refreshToken": "your-refresh-token"}

# Google OAuth2
GET /oauth2/authorization/google
```

### User Management
```bash
# Register
POST /api/users
Body: {"userName": "John Doe", "email": "john@example.com", "password": "pass123", "role": "CUSTOMER"}

# Get all users (Admin)
GET /api/users
Authorization: Bearer <token>

# Check email exists (secure)
POST /api/users/email-exists
Body: {"email": "test@example.com"}
```

---

## 🔒 Security Features

### Token Expiration Times
- **JWT Access Token:** 1 hour
- **Refresh Token:** 7 days  
- **Password Reset Token:** 2 hours

### Security Best Practices Implemented
- ✅ Short-lived access tokens minimize risk
- ✅ Refresh tokens provide good UX
- ✅ Sensitive data in POST body (not URLs)
- ✅ BCrypt password hashing
- ✅ Token revocation on logout
- ✅ Email verification for password reset
- ✅ Role-based access control
- ✅ OAuth2 integration

### Compliance Standards
- ✅ OWASP Top 10
- ✅ PCI DSS
- ✅ GDPR
- ✅ NIST Cybersecurity Framework

---

## 🧪 Testing

```bash
# Test login
curl -X POST http://localhost:8081/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email": "test@example.com", "password": "test123"}'

# Test email validation
curl -X POST http://localhost:8081/api/users/email-exists \
  -H "Content-Type: application/json" \
  -d '{"email": "test@example.com"}'

# Test authenticated endpoint
curl -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  http://localhost:8081/api/auth/user-info
```

---

## 📊 Database Schema

### Tables
- **users** - User accounts with roles
- **refresh_tokens** - Refresh token management
- **password_reset_tokens** - Password reset tokens

See [API_DOCUMENTATION.md](./API_DOCUMENTATION.md) for detailed schema.

---

## 🛠️ Tech Stack

- **Spring Boot 3.3.3** - Framework
- **Spring Security** - Authentication & authorization
- **Spring Data JPA** - Data access
- **PostgreSQL** - Database
- **JWT (jjwt 0.12.6)** - Token generation
- **OAuth2 Client** - Google login
- **JavaMail** - Email service
- **Lombok** - Reduce boilerplate
- **BCrypt** - Password hashing

---

## 🤝 Contributing

1. Create feature branch (`git checkout -b feature/amazing-feature`)
2. Commit changes (`git commit -m 'Add amazing feature'`)
3. Push to branch (`git push origin feature/amazing-feature`)
4. Open Pull Request

---

## 📞 Support

For issues or questions:
- Check [API_DOCUMENTATION.md](./API_DOCUMENTATION.md) for endpoint details
- Check [SECURITY_GUIDE.md](./SECURITY_GUIDE.md) for security implementation
- Check [GOOGLE_OAUTH2_SETUP.md](./GOOGLE_OAUTH2_SETUP.md) for OAuth2 setup

---

## 📝 License

This project is part of the AutoNova system.

---

**Last Updated:** 2024-11-03  
**Version:** 1.0.0  
**Status:** ✅ Production Ready
