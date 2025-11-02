package com.autonova.auth_service.security;

import com.autonova.auth_service.user.Role;
import com.autonova.auth_service.user.model.User;
import com.autonova.auth_service.user.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * User Security Service
 * 
 * Provides utility methods for checking user permissions and ownership
 * Used in @PreAuthorize expressions like: @userSecurityService.isOwner(#userId)
 */
@Service("userSecurityService")
public class UserSecurityService {

    private final UserRepository userRepository;

    public UserSecurityService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Get the currently authenticated user's ID from security context
     */
    public Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        
        try {
            // The principal is the email (username) from JWT
            String email = authentication.getName();
            Optional<User> user = userRepository.findByEmail(email);
            return user.map(User::getId).orElse(null);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Get the currently authenticated user's email
     */
    public String getCurrentUserEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        return authentication.getName();
    }

    /**
     * Get the currently authenticated user's role
     */
    public Role getCurrentUserRole() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }

        // Check authorities for role
        if (authentication.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_ADMIN"))) {
            return Role.ADMIN;
        } else if (authentication.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_EMPLOYEE"))) {
            return Role.EMPLOYEE;
        } else if (authentication.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_CUSTOMER"))) {
            return Role.CUSTOMER;
        } else if (authentication.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_USER"))) {
            return Role.USER;
        }
        return null;
    }

    /**
     * Check if the current user is the owner of the resource (by user ID)
     */
    public boolean isOwner(Long userId) {
        Long currentUserId = getCurrentUserId();
        return currentUserId != null && currentUserId.equals(userId);
    }

    /**
     * Check if the current user is the owner or has ADMIN role
     */
    public boolean isOwnerOrAdmin(Long userId) {
        return isOwner(userId) || hasRole(Role.ADMIN);
    }

    /**
     * Check if the current user is the owner or has EMPLOYEE or ADMIN role
     */
    public boolean isOwnerOrEmployeeOrAdmin(Long userId) {
        return isOwner(userId) || hasAnyRole(Role.EMPLOYEE, Role.ADMIN);
    }

    /**
     * Check if current user has specific role
     */
    public boolean hasRole(Role role) {
        Role currentRole = getCurrentUserRole();
        return currentRole != null && currentRole == role;
    }

    /**
     * Check if current user has any of the specified roles
     */
    public boolean hasAnyRole(Role... roles) {
        Role currentRole = getCurrentUserRole();
        if (currentRole == null) {
            return false;
        }
        for (Role role : roles) {
            if (currentRole == role) {
                return true;
            }
        }
        return false;
    }

    /**
     * Check if current user has equal or higher privilege than specified role
     */
    public boolean hasEqualOrHigherPrivilege(Role requiredRole) {
        Role currentRole = getCurrentUserRole();
        if (currentRole == null) {
            return false;
        }
        return RoleHierarchy.hasEqualOrHigherPrivilege(currentRole, requiredRole);
    }

    /**
     * Check if current user can modify another user
     * Rules:
     * - Only persisted roles (ADMIN, EMPLOYEE, CUSTOMER) can update user details
     * - USER role (guest) cannot update any user details
     * - ADMIN can modify anyone
     * - EMPLOYEE and CUSTOMER can only modify themselves (own profile)
     */
    public boolean canModifyUser(Long targetUserId) {
        Long currentUserId = getCurrentUserId();
        Role currentRole = getCurrentUserRole();

        if (currentUserId == null || currentRole == null) {
            return false;
        }

        // USER role (guest) cannot update any user details
        if (currentRole == Role.USER) {
            return false;
        }

        // Only ADMIN, EMPLOYEE, or CUSTOMER can proceed
        if (currentRole != Role.ADMIN && 
            currentRole != Role.EMPLOYEE && 
            currentRole != Role.CUSTOMER) {
            return false;
        }

        // Admin can modify anyone
        if (currentRole == Role.ADMIN) {
            return true;
        }

        // EMPLOYEE and CUSTOMER can only modify themselves
        return currentUserId.equals(targetUserId);
    }

    /**
     * Check if current user can delete another user
     * Only ADMIN can delete users
     */
    public boolean canDeleteUser(Long targetUserId) {
        return hasRole(Role.ADMIN);
    }

    /**
     * Check if current user can view another user's details
     * Rules:
     * - ADMIN can view anyone
     * - EMPLOYEE and CUSTOMER can only view themselves
     */
    public boolean canViewUser(Long targetUserId) {
        Long currentUserId = getCurrentUserId();
        Role currentRole = getCurrentUserRole();

        if (currentUserId == null || currentRole == null) {
            return false;
        }

        // Admin can view anyone
        if (currentRole == Role.ADMIN) {
            return true;
        }

        // Users can only view themselves
        return currentUserId.equals(targetUserId);
    }

    /**
     * Check if user has access to Customer Dashboard
     */
    public boolean canAccessCustomerDashboard() {
        Role currentRole = getCurrentUserRole();
        return currentRole != null && RoleHierarchy.hasEqualOrHigherPrivilege(currentRole, Role.CUSTOMER);
    }

    /**
     * Check if user has access to Employee Dashboard
     */
    public boolean canAccessEmployeeDashboard() {
        Role currentRole = getCurrentUserRole();
        return currentRole != null && RoleHierarchy.hasEqualOrHigherPrivilege(currentRole, Role.EMPLOYEE);
    }

    /**
     * Check if user has access to Admin Dashboard
     */
    public boolean canAccessAdminDashboard() {
        return hasRole(Role.ADMIN);
    }
}
