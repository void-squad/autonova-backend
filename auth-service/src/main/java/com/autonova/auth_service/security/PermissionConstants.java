package com.autonova.auth_service.security;

/**
 * Permission Constants for Role-Based Access Control
 * 
 * Access Path:
 * - USER: Guest/Anonymous - Landing page + Service summary only
 * - CUSTOMER: Registered customers - Customer Dashboard access
 * - EMPLOYEE: Company employees - Employee Dashboard access
 * - ADMIN: System administrators - Admin Dashboard + Full system access
 * 
 * These constants define common permission expressions used throughout the application
 * Usage: @PreAuthorize(PermissionConstants.ADMIN_ONLY)
 */
public class PermissionConstants {

    // === Single Role Permissions ===
    public static final String ADMIN_ONLY = "hasRole('ADMIN')";
    public static final String EMPLOYEE_ONLY = "hasRole('EMPLOYEE')";
    public static final String CUSTOMER_ONLY = "hasRole('CUSTOMER')";
    public static final String USER_ONLY = "hasRole('USER')"; // Guest access (rarely used with @PreAuthorize)

    // === Multiple Role Permissions ===
    public static final String ADMIN_OR_EMPLOYEE = "hasAnyRole('ADMIN', 'EMPLOYEE')";
    public static final String ADMIN_OR_CUSTOMER = "hasAnyRole('ADMIN', 'CUSTOMER')";
    public static final String EMPLOYEE_OR_CUSTOMER = "hasAnyRole('EMPLOYEE', 'CUSTOMER')";
    
    // Hierarchical permissions (or higher)
    public static final String EMPLOYEE_OR_HIGHER = "hasAnyRole('ADMIN', 'EMPLOYEE')";
    public static final String CUSTOMER_OR_HIGHER = "hasAnyRole('ADMIN', 'EMPLOYEE', 'CUSTOMER')";
    public static final String REGISTERED_USERS = "hasAnyRole('ADMIN', 'EMPLOYEE', 'CUSTOMER')"; // Any authenticated user

    // === User Management Permissions ===
    public static final String CAN_VIEW_ALL_USERS = "hasRole('ADMIN')"; // Only ADMIN can view all users
    public static final String CAN_CREATE_USER = "permitAll()"; // Open for registration (signup)
    public static final String CAN_UPDATE_ANY_USER = "hasRole('ADMIN')"; // Only ADMIN can update any user
    public static final String CAN_DELETE_USER = "hasRole('ADMIN')"; // Only ADMIN can delete users
    
    // === Self-Service Permissions ===
    // Used with custom security expressions (requires UserSecurityService)
    public static final String CAN_VIEW_OWN_PROFILE = REGISTERED_USERS; // Any registered user
    public static final String CAN_UPDATE_OWN_PROFILE = REGISTERED_USERS; // Any registered user

    // === Dashboard Access Permissions ===
    public static final String CAN_ACCESS_CUSTOMER_DASHBOARD = CUSTOMER_OR_HIGHER;
    public static final String CAN_ACCESS_EMPLOYEE_DASHBOARD = EMPLOYEE_OR_HIGHER;
    public static final String CAN_ACCESS_ADMIN_DASHBOARD = ADMIN_ONLY;

    // === Appointment Management (for future customer-service) ===
    public static final String CAN_BOOK_APPOINTMENT = CUSTOMER_OR_HIGHER; // Customers can book
    public static final String CAN_VIEW_ALL_APPOINTMENTS = EMPLOYEE_OR_HIGHER; // Employees can view all
    public static final String CAN_MANAGE_APPOINTMENTS = EMPLOYEE_OR_HIGHER; // Employees manage appointments
    public static final String CAN_CANCEL_ANY_APPOINTMENT = ADMIN_ONLY; // Only admin can cancel any appointment

    // === Employee Management (for future employee-service) ===
    public static final String CAN_VIEW_EMPLOYEES = EMPLOYEE_OR_HIGHER; // Employees can view other employees
    public static final String CAN_MANAGE_EMPLOYEES = ADMIN_ONLY; // Only admin manages employees
    public static final String CAN_ASSIGN_TASKS = EMPLOYEE_OR_HIGHER; // Employees can assign tasks

    // === Progress Monitoring (for progress-monitoring-service) ===
    public static final String CAN_VIEW_PROGRESS = CUSTOMER_OR_HIGHER; // Customers can track their service progress
    public static final String CAN_UPDATE_PROGRESS = EMPLOYEE_OR_HIGHER; // Employees update service progress

    // === System Administration ===
    public static final String CAN_VIEW_SYSTEM_LOGS = ADMIN_ONLY;
    public static final String CAN_MANAGE_SYSTEM = ADMIN_ONLY;
    public static final String CAN_MANAGE_ROLES = ADMIN_ONLY;

    private PermissionConstants() {
        // Utility class, prevent instantiation
    }
}
