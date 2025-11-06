package com.autonova.auth_service.security;

import com.autonova.auth_service.user.Role;
import java.util.Arrays;
import java.util.List;

/**
 * Role Hierarchy Utility for Automobile Service System
 * 
 * Access Path:
 * - USER: Anonymous/Guest users (not saved in DB) - can only view landing page and service summary
 * - CUSTOMER: Registered customers who book automobile services (saved in DB)
 * - EMPLOYEE: Company employees who perform service operations (saved in DB)
 * - ADMIN: System administrators with full access (saved in DB)
 * 
 * Hierarchy (from lowest to highest privilege):
 * USER (guest) -> CUSTOMER -> EMPLOYEE -> ADMIN
 * 
 * Note: Only CUSTOMER, EMPLOYEE, and ADMIN are stored in the database.
 * USER role is for unauthenticated/guest access to public pages.
 */
public class RoleHierarchy {

    // Role hierarchy order (lower index = lower privilege)
    private static final List<Role> HIERARCHY = Arrays.asList(
            Role.USER,          // Guest/Anonymous (not in DB)
            Role.CUSTOMER,      // Registered customers (in DB)
            Role.EMPLOYEE,      // Company employees (in DB)
            Role.ADMIN          // System administrators (in DB)
    );

    /**
     * Get the privilege level of a role (higher number = higher privilege)
     */
    public static int getLevel(Role role) {
        return HIERARCHY.indexOf(role);
    }

    /**
     * Check if a role has equal or higher privilege than another role
     */
    public static boolean hasEqualOrHigherPrivilege(Role userRole, Role requiredRole) {
        return getLevel(userRole) >= getLevel(requiredRole);
    }

    /**
     * Check if a role has higher privilege than another role
     */
    public static boolean hasHigherPrivilege(Role userRole, Role comparisonRole) {
        return getLevel(userRole) > getLevel(comparisonRole);
    }

    /**
     * Get all roles with equal or lower privilege
     */
    public static List<Role> getRolesWithEqualOrLowerPrivilege(Role role) {
        int level = getLevel(role);
        return HIERARCHY.subList(0, level + 1);
    }

    /**
     * Get all roles with higher privilege
     */
    public static List<Role> getRolesWithHigherPrivilege(Role role) {
        int level = getLevel(role);
        if (level == HIERARCHY.size() - 1) {
            return List.of(); // Already highest role
        }
        return HIERARCHY.subList(level + 1, HIERARCHY.size());
    }

    /**
     * Check if role is ADMIN
     */
    public static boolean isAdmin(Role role) {
        return role == Role.ADMIN;
    }

    /**
     * Check if role is EMPLOYEE or higher
     */
    public static boolean isEmployeeOrHigher(Role role) {
        return getLevel(role) >= getLevel(Role.EMPLOYEE);
    }

    /**
     * Check if role is CUSTOMER or higher (registered users)
     */
    public static boolean isCustomerOrHigher(Role role) {
        return getLevel(role) >= getLevel(Role.CUSTOMER);
    }

    /**
     * Check if role is USER (guest/unauthenticated)
     */
    public static boolean isGuest(Role role) {
        return role == Role.USER;
    }

    /**
     * Check if role is saved in database (CUSTOMER, EMPLOYEE, or ADMIN)
     */
    public static boolean isPersistedRole(Role role) {
        return role == Role.CUSTOMER || role == Role.EMPLOYEE || role == Role.ADMIN;
    }
}
