# Role-Based Authorization Utility Summary

## Overview
This document summarizes the implemented role utility classes and role-based access control system for the AutoNova Automobile Service System.

---

## 🎯 Role Hierarchy & Access Path

### Access Path
- **USER (Guest)**: Landing page + Service summary only (no authentication required, NOT saved in database)
- **CUSTOMER**: Signup → Login → Customer Dashboard (saved in database)
- **EMPLOYEE**: Signup → Login → Employee Dashboard (saved in database)
- **ADMIN**: Signup → Login → Admin Dashboard (saved in database)

### Roles (from lowest to highest privilege):
1. **USER** - Guest/Anonymous users (NOT stored in database) - can only view public pages
2. **CUSTOMER** - Registered customers who book automobile services (stored in database)
3. **EMPLOYEE** - Company employees who perform service operations (stored in database)
4. **ADMIN** - System administrators with full access (stored in database)

### Privilege Levels:
```
USER/Guest (Level 0) < CUSTOMER (Level 1) < EMPLOYEE (Level 2) < ADMIN (Level 3)
```

### 📝 Important Notes:
- Only **CUSTOMER**, **EMPLOYEE**, and **ADMIN** roles are saved in the database
- **USER** role is for guest/unauthenticated access to public pages (landing page, service summary)
- During signup, users must select one of the three persisted roles: CUSTOMER, EMPLOYEE, or ADMIN

---

## 📦 Utility Classes Implemented

### 1. **RoleHierarchy.java**
**Purpose:** Provides role comparison and hierarchy checking utilities

**Key Methods:**
- `getLevel(Role role)` - Returns privilege level (0-3)
- `hasEqualOrHigherPrivilege(Role userRole, Role requiredRole)` - Checks if user role meets minimum requirement
- `hasHigherPrivilege(Role userRole, Role comparisonRole)` - Checks strict higher privilege
- `getRolesWithEqualOrLowerPrivilege(Role role)` - Returns all roles below or equal
- `getRolesWithHigherPrivilege(Role role)` - Returns all roles above
- `isAdmin(Role role)` - Quick ADMIN check
- `isManagerOrHigher(Role role)` - Quick MANAGER+ check
- `isUserOrHigher(Role role)` - Quick USER+ check

**Usage Example:**
```java
if (RoleHierarchy.hasEqualOrHigherPrivilege(currentRole, Role.MANAGER)) {
    // User is MANAGER or ADMIN
}
```

---

### 2. **PermissionConstants.java**
**Purpose:** Centralized permission expressions for @PreAuthorize annotations

**Permission Categories:**

#### Single Role Permissions
- `ADMIN_ONLY` - `"hasRole('ADMIN')"`
- `EMPLOYEE_ONLY` - `"hasRole('EMPLOYEE')"`
- `CUSTOMER_ONLY` - `"hasRole('CUSTOMER')"`
- `USER_ONLY` - `"hasRole('USER')"` (rarely used with @PreAuthorize - for guest access)

#### Multiple Role Permissions
- `ADMIN_OR_EMPLOYEE` - `"hasAnyRole('ADMIN', 'EMPLOYEE')"`
- `ADMIN_OR_CUSTOMER` - `"hasAnyRole('ADMIN', 'CUSTOMER')"`
- `EMPLOYEE_OR_CUSTOMER` - `"hasAnyRole('EMPLOYEE', 'CUSTOMER')"`
- `EMPLOYEE_OR_HIGHER` - `"hasAnyRole('ADMIN', 'EMPLOYEE')"`
- `CUSTOMER_OR_HIGHER` - `"hasAnyRole('ADMIN', 'EMPLOYEE', 'CUSTOMER')"`
- `REGISTERED_USERS` - `"hasAnyRole('ADMIN', 'EMPLOYEE', 'CUSTOMER')"` (any authenticated user)

#### User Management Permissions
- `CAN_VIEW_ALL_USERS` - ADMIN only
- `CAN_CREATE_USER` - Public (registration/signup)
- `CAN_UPDATE_ANY_USER` - ADMIN only
- `CAN_DELETE_USER` - ADMIN only
- `CAN_VIEW_OWN_PROFILE` - All registered users (CUSTOMER, EMPLOYEE, ADMIN)
- `CAN_UPDATE_OWN_PROFILE` - All registered users (CUSTOMER, EMPLOYEE, ADMIN)

#### Dashboard Access Permissions
- `CAN_ACCESS_CUSTOMER_DASHBOARD` - CUSTOMER or higher (CUSTOMER, EMPLOYEE, ADMIN)
- `CAN_ACCESS_EMPLOYEE_DASHBOARD` - EMPLOYEE or higher (EMPLOYEE, ADMIN)
- `CAN_ACCESS_ADMIN_DASHBOARD` - ADMIN only

#### Future Service Permissions (for other microservices)
- `CAN_BOOK_APPOINTMENT` - CUSTOMER or higher (customers can book appointments)
- `CAN_VIEW_ALL_APPOINTMENTS` - EMPLOYEE or higher (employees can view all appointments)
- `CAN_MANAGE_APPOINTMENTS` - EMPLOYEE or higher (employees manage appointments)
- `CAN_CANCEL_ANY_APPOINTMENT` - ADMIN only
- `CAN_VIEW_EMPLOYEES` - EMPLOYEE or higher (employees can view other employees)
- `CAN_MANAGE_EMPLOYEES` - ADMIN only (only admin manages employees)
- `CAN_ASSIGN_TASKS` - EMPLOYEE or higher (employees can assign tasks)
- `CAN_VIEW_PROGRESS` - CUSTOMER or higher (customers can track their service progress)
- `CAN_UPDATE_PROGRESS` - EMPLOYEE or higher (employees update service progress)
- `CAN_VIEW_SYSTEM_LOGS` - ADMIN only
- `CAN_MANAGE_SYSTEM` - ADMIN only
- `CAN_MANAGE_ROLES` - ADMIN only

**Usage Example:**
```java
@PreAuthorize(PermissionConstants.ADMIN_ONLY)
@DeleteMapping("/{id}")
public ResponseEntity<?> deleteUser(@PathVariable Long id) {
    // Only ADMIN can execute this
}
```

---

### 3. **UserSecurityService.java**
**Purpose:** Runtime security checks and custom permission logic

**Key Methods:**

#### Current User Information
- `getCurrentUserId()` - Get authenticated user's ID
- `getCurrentUserEmail()` - Get authenticated user's email
- `getCurrentUserRole()` - Get authenticated user's role

#### Ownership Checks
- `isOwner(Long userId)` - Check if current user owns the resource
- `isOwnerOrAdmin(Long userId)` - Owner or ADMIN
- `isOwnerOrManagerOrAdmin(Long userId)` - Owner, MANAGER, or ADMIN

#### Role Checks
- `hasRole(Role role)` - Check specific role
- `hasAnyRole(Role... roles)` - Check multiple roles
- `hasEqualOrHigherPrivilege(Role requiredRole)` - Hierarchy-based check

#### Business Logic Permissions
- `canModifyUser(Long targetUserId)` - Complex modification logic:
  - ADMIN can modify anyone
  - Users can modify themselves
  - MANAGER can modify USER and CUSTOMER (not other MANAGERS or ADMIN)
- `canDeleteUser(Long targetUserId)` - Only ADMIN can delete
- `canViewUser(Long targetUserId)` - ADMIN/MANAGER can view anyone, users can view themselves

**Usage Example:**
```java
@PreAuthorize("@userSecurityService.canModifyUser(#id)")
@PutMapping("/{id}")
public ResponseEntity<?> updateUser(@PathVariable Long id, @RequestBody User user) {
    // Custom business logic for who can modify whom
}
```

---

## 🔒 Applied Access Control Rules

### UserController Endpoints

| Endpoint | Method | Access Rule | Who Can Access |
|----------|--------|-------------|----------------|
| `/api/users` | GET | `CAN_VIEW_ALL_USERS` | ADMIN only |
| `/api/users/{id}` | GET | `canViewUser(#id)` | ADMIN, or self |
| `/api/users/email/{email}` | GET | `CAN_VIEW_ALL_USERS` | ADMIN only |
| `/api/users` | POST | Public (permitAll) | Anyone (signup with CUSTOMER/EMPLOYEE/ADMIN role) |
| `/api/users/{id}` | PUT | `canModifyUser(#id)` | ADMIN, or self |
| `/api/users/{id}` | DELETE | `CAN_DELETE_USER` | ADMIN only |
| `/api/users/exists/{id}` | GET | `REGISTERED_USERS` | All registered users (CUSTOMER, EMPLOYEE, ADMIN) |
| `/api/users/email-exists/{email}` | GET | Public (permitAll) | Anyone (validation for signup) |

### AuthController Endpoints

| Endpoint | Method | Access Rule | Who Can Access |
|----------|--------|-------------|----------------|
| `/api/auth/login` | POST | Public (permitAll) | Anyone |

---

## 🛠️ How to Add New Utilities

### Step 1: Add Permission Constants
Edit `PermissionConstants.java`:
```java
// Add new constant for your permission
public static final String CAN_MANAGE_INVOICES = "hasAnyRole('ADMIN', 'MANAGER')";
```

### Step 2: Add Custom Logic to UserSecurityService
Edit `UserSecurityService.java`:
```java
/**
 * Check if user can approve invoices
 */
public boolean canApproveInvoice(Long invoiceId) {
    Role currentRole = getCurrentUserRole();
    // Your custom logic here
    return hasAnyRole(Role.ADMIN, Role.MANAGER);
}
```

### Step 3: Apply to Controller
```java
@PreAuthorize("@userSecurityService.canApproveInvoice(#invoiceId)")
@PostMapping("/invoices/{invoiceId}/approve")
public ResponseEntity<?> approveInvoice(@PathVariable Long invoiceId) {
    // Your logic
}
```

---

## 🔐 Security Configuration

### JWT Authentication Flow
1. User logs in via `/api/auth/login`
2. JWT token generated with claims: `userId`, `email`, `role`
3. Client sends token in `Authorization: Bearer <token>` header
4. `JwtAuthenticationFilter` validates token and sets Spring Security context
5. `@PreAuthorize` annotations check permissions before method execution

### Public Endpoints (No Authentication Required)
- `POST /api/auth/login` - User login
- `POST /api/users` - User registration
- `GET /api/users/email-exists/{email}` - Email validation
- `/actuator/**` - Health checks

### Protected Endpoints
All other endpoints require valid JWT token with appropriate role

---

## 📝 Testing Examples

### 1. Test as ADMIN
```bash
# Login as ADMIN
curl -X POST http://localhost:8081/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@autonova.com","password":"admin123"}'

# Use returned token
curl -X GET http://localhost:8081/api/users \
  -H "Authorization: Bearer <admin_token>"
```

### 2. Test as CUSTOMER (Limited Access)
```bash
# Signup as CUSTOMER
curl -X POST http://localhost:8081/api/users \
  -H "Content-Type: application/json" \
  -d '{"userName":"John Doe","email":"john@customer.com","contactOne":"1234567890","password":"password123","role":"CUSTOMER"}'

# Login as CUSTOMER
curl -X POST http://localhost:8081/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"john@customer.com","password":"password123"}'

# Try to view all users (should fail - 403 Forbidden)
curl -X GET http://localhost:8081/api/users \
  -H "Authorization: Bearer <customer_token>"

# View own profile (should succeed)
curl -X GET http://localhost:8081/api/users/{customer_id} \
  -H "Authorization: Bearer <customer_token>"

# Update own profile (should succeed)
curl -X PUT http://localhost:8081/api/users/{customer_id} \
  -H "Authorization: Bearer <customer_token>" \
  -H "Content-Type: application/json" \
  -d '{"userName":"John Updated","address":"123 Main St"}'
```

### 3. Test as EMPLOYEE
```bash
# Signup as EMPLOYEE
curl -X POST http://localhost:8081/api/users \
  -H "Content-Type: application/json" \
  -d '{"userName":"Jane Smith","email":"jane@employee.com","contactOne":"0987654321","password":"password123","role":"EMPLOYEE"}'

# Login as EMPLOYEE
curl -X POST http://localhost:8081/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"jane@employee.com","password":"password123"}'

# Try to view all users (should fail - 403 Forbidden, only ADMIN can view all)
curl -X GET http://localhost:8081/api/users \
  -H "Authorization: Bearer <employee_token>"

# View own profile (should succeed)
curl -X GET http://localhost:8081/api/users/{employee_id} \
  -H "Authorization: Bearer <employee_token>"

# Try to delete user (should fail - only ADMIN can delete)
curl -X DELETE http://localhost:8081/api/users/5 \
  -H "Authorization: Bearer <employee_token>"
```

---

## 🚀 Future Enhancements

### Planned Utilities to Add:
1. **Appointment Management Permissions** (for customer-service)
   - `canBookAppointment()` - CUSTOMER or higher
   - `canCancelOwnAppointment(appointmentId)` - Appointment owner
   - `canCancelAnyAppointment(appointmentId)` - ADMIN only
   - `canViewAppointmentHistory()` - CUSTOMER (own) or EMPLOYEE (all)

2. **Vehicle Management Permissions** (for customer-service)
   - `canAddVehicle()` - CUSTOMER or higher
   - `canViewVehicleHistory(vehicleId)` - Vehicle owner or EMPLOYEE
   - `canUpdateVehicleStatus(vehicleId)` - EMPLOYEE or higher

3. **Invoice/Payment Permissions** (for future billing-service)
   - `canGenerateInvoice()` - EMPLOYEE or higher
   - `canApprovePayment(invoiceId)` - ADMIN only
   - `canViewFinancialReports()` - ADMIN only

4. **Progress Monitoring Permissions** (for progress-monitoring-service)
   - `canUpdateServiceProgress(serviceId)` - EMPLOYEE or higher
   - `canViewProgressDashboard()` - CUSTOMER (own services) or EMPLOYEE (all services)

5. **Employee Management Permissions** (for employee-dashboard-service)
   - `canAssignTask(employeeId)` - EMPLOYEE or higher
   - `canViewEmployeePerformance()` - ADMIN only
   - `canManageEmployeeSchedule()` - ADMIN only

---

## 📋 Best Practices

1. **Use PermissionConstants** - Always prefer constants over hardcoded strings
2. **Document Custom Logic** - Add Javadoc comments for complex permission methods
3. **Test Edge Cases** - Test with different roles and ownership scenarios
4. **Keep Hierarchy Consistent** - Don't bypass the role hierarchy without good reason
5. **Log Access Denials** - Add logging in UserSecurityService for audit trails
6. **Centralize Complex Logic** - Put business rules in UserSecurityService, not controllers

---

## 📞 Support

For questions or additions to this role utility system:
1. Review this document
2. Check `PermissionConstants.java` for available permissions
3. Extend `UserSecurityService.java` for custom logic
4. Update controller `@PreAuthorize` annotations
5. Test with different roles using JWT tokens

---

**Last Updated:** December 2024  
**Version:** 1.0  
**System:** AutoNova Automobile Service System - Auth Service
