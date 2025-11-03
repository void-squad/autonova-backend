package com.autonova.auth_service.user.controller;

import com.autonova.auth_service.security.PermissionConstants;
import com.autonova.auth_service.user.Role;
import com.autonova.auth_service.user.dto.UserLookupRequest;
import com.autonova.auth_service.user.service.UserService;
import com.autonova.auth_service.user.model.User;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * User Management Controller with Role-Based Access Control
 * 
 * Access Path:
 * - USER (Guest): Landing page + Service summary (no authentication required)
 * - CUSTOMER: Signup/Login -> Customer Dashboard
 * - EMPLOYEE: Signup/Login -> Employee Dashboard  
 * - ADMIN: Signup/Login -> Admin Dashboard
 * 
 * Note: Only CUSTOMER, EMPLOYEE, and ADMIN roles are stored in database.
 * 
 * Access Rules:
 * - GET /api/users: ADMIN only (view all users)
 * - GET /api/users/{id}: ADMIN or owner (view specific user)
 * - GET /api/users/email/{email}: ADMIN only
 * - POST /api/users: Public (user registration/signup) - configured in SecurityConfig
 * - PUT /api/users/{id}: Only ADMIN, EMPLOYEE, CUSTOMER (update user profile - NO role/password changes)
 *   - USER role (guest) CANNOT update any user details
 *   - ADMIN can update anyone, EMPLOYEE/CUSTOMER can only update themselves
 *   - Role and password changes are NOT allowed through this endpoint
 * - PATCH /api/users/{id}/role: ADMIN only (change user role)
 * - Password changes: Use /api/auth/forgot-password and /api/auth/reset-password (email verification required)
 * - DELETE /api/users/{id}: ADMIN only (delete user)
 * - GET /api/users/exists/{id}: Authenticated users (check existence)
 * - GET /api/users/email-exists/{email}: Public (for registration validation)
 */
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    /**
     * GET all users - Only ADMIN can view all users
     */
    @PreAuthorize(PermissionConstants.CAN_VIEW_ALL_USERS)
    @GetMapping
    public ResponseEntity<List<User>> getAllUsers() {
        List<User> users = userService.getAllUsers();
        return ResponseEntity.ok(users);
    }

    /**
     * GET user by ID - ADMIN or the user themselves can view
     */
    @PreAuthorize("@userSecurityService.canViewUser(#id)")
    @GetMapping("/{id}")
    public ResponseEntity<?> getUserById(@PathVariable Long id) {
        return userService.getUserById(id)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(createErrorResponse("User not found with id: " + id)));
    }

    /**
     * POST - Get user by email (SECURE) - Only ADMIN
     * Uses POST with request body to avoid exposing email in URL/logs
     * Enterprise security best practice: Sensitive data in body, not URL
     */
    @PreAuthorize(PermissionConstants.CAN_VIEW_ALL_USERS)
    @PostMapping("/by-email")
    public ResponseEntity<?> getUserByEmail(@RequestBody UserLookupRequest request) {
        if (request.getEmail() == null || request.getEmail().trim().isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(createErrorResponse("Email is required"));
        }
        return userService.getUserByEmail(request.getEmail())
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(createErrorResponse("User not found")));
    }
    
    /**
     * @deprecated Use POST /api/users/by-email instead (more secure)
     * GET user by email - Only ADMIN
     * WARNING: This endpoint exposes email in URL which can be logged
     * Kept for backward compatibility - will be removed in future version
     */
    @Deprecated
    @PreAuthorize(PermissionConstants.CAN_VIEW_ALL_USERS)
    @GetMapping("/email/{email}")
    public ResponseEntity<?> getUserByEmailDeprecated(@PathVariable String email) {
        return userService.getUserByEmail(email)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(createErrorResponse("User not found with email: " + email)));
    }

    /**
     * POST - Create new user (Signup/Registration)
     * Public endpoint - Anyone can signup
     * Accepts role: CUSTOMER, EMPLOYEE, or ADMIN (only these are saved in DB)
     * No @PreAuthorize needed here
     */
    @PostMapping
    public ResponseEntity<?> createUser(@RequestBody User user) {
        try {
            // Validate that role is one of the persisted roles
            if (user.getRole() != Role.CUSTOMER && 
                user.getRole() != Role.EMPLOYEE && 
                user.getRole() != Role.ADMIN) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(createErrorResponse("Invalid role. Only CUSTOMER, EMPLOYEE, or ADMIN roles are allowed for signup."));
            }
            
            User createdUser = userService.createUser(user);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdUser);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(createErrorResponse(e.getMessage()));
        }
    }

    /**
     * PUT - Update user profile
     * Only persisted roles (ADMIN, EMPLOYEE, CUSTOMER) can update user details
     * USER role (guest) cannot update
     * ADMIN can update anyone, EMPLOYEE and CUSTOMER can only update themselves
     * Note: Role cannot be changed through this endpoint (use PATCH /{id}/role instead)
     */
    @PreAuthorize("@userSecurityService.canModifyUser(#id)")
    @PutMapping("/{id}")
    public ResponseEntity<?> updateUser(@PathVariable Long id, @RequestBody User user) {
        try {
            User updatedUser = userService.updateUser(id, user);
            return ResponseEntity.ok(updatedUser);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(createErrorResponse(e.getMessage()));
        }
    }

    /**
     * PATCH - Update user role - ADMIN only
     * This is the only way to change a user's role
     */
    @PreAuthorize(PermissionConstants.ADMIN_ONLY)
    @PatchMapping("/{id}/role")
    public ResponseEntity<?> updateUserRole(@PathVariable Long id, @RequestBody Map<String, String> roleRequest) {
        try {
            String roleStr = roleRequest.get("role");
            if (roleStr == null || roleStr.trim().isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(createErrorResponse("Role is required"));
            }
            
            Role newRole = Role.valueOf(roleStr.toUpperCase());
            User updatedUser = userService.updateUserRole(id, newRole);
            return ResponseEntity.ok(updatedUser);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(createErrorResponse(e.getMessage()));
        }
    }

    /**
     * Note: Password changes are handled through /api/auth/forgot-password and /api/auth/reset-password
     * This approach ensures email verification for all password changes (more secure)
     * Users don't need to remember their current password - they just verify via email
     */

    /**
     * DELETE user - Only ADMIN can delete users
     */
    @PreAuthorize(PermissionConstants.CAN_DELETE_USER)
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable Long id) {
        try {
            userService.deleteUser(id);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "User deleted successfully");
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(createErrorResponse(e.getMessage()));
        }
    }

    /**
     * Check if user exists - Authenticated users only (registered users)
     */
    @PreAuthorize(PermissionConstants.REGISTERED_USERS)
    @GetMapping("/exists/{id}")
    public ResponseEntity<Map<String, Boolean>> checkUserExists(@PathVariable Long id) {
        Map<String, Boolean> response = new HashMap<>();
        response.put("exists", userService.userExists(id));
        return ResponseEntity.ok(response);
    }

    /**
     * POST - Check if email exists (SECURE) - Public (for registration form validation)
     * Uses POST with request body to avoid exposing email in URL/logs
     * Enterprise security best practice: Sensitive data in body, not URL
     */
    @PostMapping("/email-exists")
    public ResponseEntity<Map<String, Boolean>> checkEmailExists(@RequestBody UserLookupRequest request) {
        Map<String, Boolean> response = new HashMap<>();
        if (request.getEmail() == null || request.getEmail().trim().isEmpty()) {
            response.put("exists", false);
            response.put("error", true);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
        response.put("exists", userService.emailExists(request.getEmail()));
        return ResponseEntity.ok(response);
    }
    
    /**
     * @deprecated Use POST /api/users/email-exists instead (more secure)
     * GET - Check if email exists - Public (for registration form validation)
     * WARNING: This endpoint exposes email in URL which can be logged
     * Kept for backward compatibility - will be removed in future version
     */
    @Deprecated
    @GetMapping("/email-exists/{email}")
    public ResponseEntity<Map<String, Boolean>> checkEmailExistsDeprecated(@PathVariable String email) {
        Map<String, Boolean> response = new HashMap<>();
        response.put("exists", userService.emailExists(email));
        return ResponseEntity.ok(response);
    }

    // Helper method to create error response
    private Map<String, String> createErrorResponse(String message) {
        Map<String, String> error = new HashMap<>();
        error.put("error", message);
        return error;
    }
}
