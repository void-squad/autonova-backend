package com.autonova.auth_service.user.controller;

import com.autonova.auth_service.security.PermissionConstants;
import com.autonova.auth_service.user.Role;
import com.autonova.auth_service.user.dto.CustomerProfile;
import com.autonova.auth_service.user.dto.UserMapper;
import com.autonova.auth_service.user.dto.UserProfileResponse;
import com.autonova.auth_service.user.dto.UserResponse;
import com.autonova.auth_service.user.service.CustomerServiceClient;
import com.autonova.auth_service.user.service.UserService;
import com.autonova.auth_service.user.model.User;
import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
 * - PUT /api/users/{id}: Only ADMIN, EMPLOYEE, CUSTOMER (update user profile - NO role changes)
 *   - USER role (guest) CANNOT update any user details
 *   - ADMIN can update anyone, EMPLOYEE/CUSTOMER can only update themselves
 *   - Role changes are NOT allowed through this endpoint
 * - PATCH /api/users/{id}/role: ADMIN only (change user role)
 * - DELETE /api/users/{id}: ADMIN only (delete user)
 * - GET /api/users/exists/{id}: Authenticated users (check existence)
 * - GET /api/users/email-exists/{email}: Public (for registration validation)
 */
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;
    private final CustomerServiceClient customerServiceClient;

    public UserController(UserService userService, CustomerServiceClient customerServiceClient) {
        this.userService = userService;
        this.customerServiceClient = customerServiceClient;
    }

    /**
     * GET all users - Only ADMIN can view all users
     */
    @PreAuthorize(PermissionConstants.CAN_VIEW_ALL_USERS)
    @GetMapping
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        List<UserResponse> users = userService.getAllUsers().stream()
                .map(UserMapper::toResponse)
                .toList();
        return ResponseEntity.ok(users);
    }

    /**
     * GET user by ID - ADMIN or the user themselves can view
     */
    @PreAuthorize("@userSecurityService.canViewUser(#id)")
    @GetMapping("/{id}")
    public ResponseEntity<?> getUserById(@PathVariable Long id) {
    return userService.getUserById(id)
        .map(UserMapper::toResponse)
        .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(createErrorResponse("User not found with id: " + id)));
    }

    @PreAuthorize(PermissionConstants.REGISTERED_USERS)
    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(HttpServletRequest request) {
        String authorization = request.getHeader("Authorization");

        return userService.getCurrentUser()
                .map(UserMapper::toResponse)
                .map(user -> {
                    CustomerProfile customer = null;
                    if (authorization != null && !authorization.isBlank()
                            && user.role() == Role.CUSTOMER) {
                        customer = customerServiceClient.getCurrentCustomer(authorization).orElse(null);
                    }
                    return new UserProfileResponse(user, customer);
                })
                .<ResponseEntity<?>>map(ResponseEntity::ok)
        .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(createErrorResponse("Current user not found")));
    }

    /**
     * GET user by email - Only ADMIN
     */
    @PreAuthorize(PermissionConstants.CAN_VIEW_ALL_USERS)
    @GetMapping("/email/{email}")
    public ResponseEntity<?> getUserByEmail(@PathVariable String email) {
        return userService.getUserByEmail(email)
                .map(UserMapper::toResponse)
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
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(UserMapper.toResponse(createdUser));
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
            return ResponseEntity.ok(UserMapper.toResponse(updatedUser));
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
            return ResponseEntity.ok(UserMapper.toResponse(updatedUser));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(createErrorResponse(e.getMessage()));
        }
    }

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
     * Check if email exists - Public (for registration form validation)
     * Note: This is marked as public in SecurityConfig permitAll
     */
    @GetMapping("/email-exists/{email}")
    public ResponseEntity<Map<String, Boolean>> checkEmailExists(@PathVariable String email) {
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
