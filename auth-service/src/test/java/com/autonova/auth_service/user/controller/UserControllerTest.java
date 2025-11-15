package com.autonova.auth_service.user.controller;

import com.autonova.auth_service.user.Role;
import com.autonova.auth_service.user.dto.CustomerProfile;
import com.autonova.auth_service.user.dto.UserResponse;
import com.autonova.auth_service.user.model.User;
import com.autonova.auth_service.user.service.CustomerServiceClient;
import com.autonova.auth_service.user.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.Instant;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("User Controller Unit Tests")
class UserControllerTest {

    @Mock
    private UserService userService;

    @Mock
    private CustomerServiceClient customerServiceClient;

    @Mock
    private HttpServletRequest httpServletRequest;

    @InjectMocks
    private UserController userController;

    private User testUser;
    private User adminUser;
    private List<User> userList;

    @BeforeEach
    void setUp() {
        // Setup test customer user
        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail("customer@example.com");
        testUser.setPassword("encodedPassword");
        testUser.setRole(Role.CUSTOMER);
        testUser.setUserName("Test Customer");
        testUser.setFirstName("Test");
        testUser.setLastName("Customer");
        testUser.setContactOne("+1234567890");
        testUser.setEnabled(true);

        // Setup admin user
        adminUser = new User();
        adminUser.setId(2L);
        adminUser.setEmail("admin@example.com");
        adminUser.setPassword("encodedPassword");
        adminUser.setRole(Role.ADMIN);
        adminUser.setUserName("Admin User");
        adminUser.setEnabled(true);

        // Setup user list
        User employeeUser = new User();
        employeeUser.setId(3L);
        employeeUser.setEmail("employee@example.com");
        employeeUser.setRole(Role.EMPLOYEE);
        employeeUser.setUserName("Employee User");
        employeeUser.setEnabled(true);

        userList = Arrays.asList(testUser, adminUser, employeeUser);
    }

    @Test
    @DisplayName("Should return all users when admin requests")
    void testGetAllUsers_AsAdmin_ShouldReturnAllUsers() {
        // Given
        when(userService.getAllUsers()).thenReturn(userList);

        // When
        ResponseEntity<List<UserResponse>> response = userController.getAllUsers(null, null, null);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(3);
        verify(userService, times(1)).getAllUsers();
    }

    @Test
    @DisplayName("Should filter users by role")
    void testGetAllUsers_WithRoleFilter_ShouldReturnFilteredUsers() {
        // Given
        when(userService.getAllUsers()).thenReturn(userList);

        // When
        ResponseEntity<List<UserResponse>> response = userController.getAllUsers(null, "CUSTOMER", null);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(1);
        assertThat(response.getBody().get(0).role()).isEqualTo(Role.CUSTOMER);
    }

    @Test
    @DisplayName("Should return user statistics for admin")
    void testGetUserStats_AsAdmin_ShouldReturnStats() {
        // Given
        when(userService.getAllUsers()).thenReturn(userList);

        // When
        ResponseEntity<Map<String, Object>> response = userController.getUserStats();

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> stats = response.getBody();
        assertThat(stats).isNotNull();
        assertThat(stats.get("totalUsers")).isEqualTo(3);
        assertThat(stats.get("activeUsers")).isEqualTo(3L);
        assertThat(stats.get("customers")).isEqualTo(1L);
        assertThat(stats.get("employees")).isEqualTo(1L);
        assertThat(stats.get("admins")).isEqualTo(1L);
    }

    @Test
    @DisplayName("Should return user by ID when found")
    void testGetUserById_WithValidId_ShouldReturnUser() {
        // Given
        when(userService.getUserById(1L)).thenReturn(Optional.of(testUser));

        // When
        ResponseEntity<?> response = userController.getUserById(1L);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isInstanceOf(UserResponse.class);
        UserResponse body = (UserResponse) response.getBody();
        assertThat(body.id()).isEqualTo(1L);
        assertThat(body.email()).isEqualTo("customer@example.com");
        verify(userService, times(1)).getUserById(1L);
    }

    @Test
    @DisplayName("Should return 404 when user not found by ID")
    void testGetUserById_WithInvalidId_ShouldReturn404() {
        // Given
        when(userService.getUserById(999L)).thenReturn(Optional.empty());

        // When
        ResponseEntity<?> response = userController.getUserById(999L);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        @SuppressWarnings("unchecked")
        Map<String, String> body = (Map<String, String>) response.getBody();
        assertThat(body.get("error")).contains("User not found");
    }

    @Test
    @DisplayName("Should successfully create new customer user")
    void testCreateUser_WithValidData_ShouldCreateUser() {
        // Given
        User newUser = new User();
        newUser.setEmail("newuser@example.com");
        newUser.setPassword("Password123!");
        newUser.setRole(Role.CUSTOMER);
        newUser.setUserName("New User");

        when(userService.createUser(any(User.class))).thenReturn(testUser);

        // When
        ResponseEntity<?> response = userController.createUser(newUser);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isInstanceOf(UserResponse.class);
        verify(userService, times(1)).createUser(any(User.class));
    }

    @Test
    @DisplayName("Should return bad request when creating user with duplicate email")
    void testCreateUser_WithDuplicateEmail_ShouldReturnBadRequest() {
        // Given
        User newUser = new User();
        newUser.setEmail("customer@example.com");
        newUser.setPassword("Password123!");
        newUser.setRole(Role.CUSTOMER);

        when(userService.createUser(any(User.class)))
                .thenThrow(new IllegalArgumentException("Email already exists"));

        // When
        ResponseEntity<?> response = userController.createUser(newUser);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        @SuppressWarnings("unchecked")
        Map<String, String> body = (Map<String, String>) response.getBody();
        assertThat(body.get("error")).isEqualTo("Email already exists");
    }

    @Test
    @DisplayName("Should successfully update user profile")
    void testUpdateUser_WithValidData_ShouldUpdateUser() {
        // Given
        User updatedUser = new User();
        updatedUser.setUserName("Updated Name");
        updatedUser.setContactOne("+9876543210");

        when(userService.updateUser(eq(1L), any(User.class))).thenReturn(testUser);

        // When
        ResponseEntity<?> response = userController.updateUser(1L, updatedUser);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isInstanceOf(UserResponse.class);
        verify(userService, times(1)).updateUser(eq(1L), any(User.class));
    }

    @Test
    @DisplayName("Should successfully update user role as admin")
    void testUpdateUserRole_AsAdmin_ShouldUpdateRole() {
        // Given
        Map<String, String> roleRequest = new HashMap<>();
        roleRequest.put("role", "EMPLOYEE");

        User updatedUser = new User();
        updatedUser.setId(1L);
        updatedUser.setRole(Role.EMPLOYEE);
        updatedUser.setEmail("customer@example.com");

        when(userService.updateUserRole(1L, Role.EMPLOYEE)).thenReturn(updatedUser);

        // When
        ResponseEntity<?> response = userController.updateUserRole(1L, roleRequest);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isInstanceOf(UserResponse.class);
        verify(userService, times(1)).updateUserRole(1L, Role.EMPLOYEE);
    }

    @Test
    @DisplayName("Should successfully delete user as admin")
    void testDeleteUser_AsAdmin_ShouldDeleteUser() {
        // Given
        doNothing().when(userService).deleteUser(1L);

        // When
        ResponseEntity<?> response = userController.deleteUser(1L);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertThat(body.get("success")).isEqualTo(true);
        assertThat(body.get("message")).asString().contains("deleted successfully");
        verify(userService, times(1)).deleteUser(1L);
    }

    @Test
    @DisplayName("Should check if user exists by ID")
    void testCheckUserExists_WithValidId_ShouldReturnTrue() {
        // Given
        when(userService.userExists(1L)).thenReturn(true);

        // When
        ResponseEntity<Map<String, Boolean>> response = userController.checkUserExists(1L);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().get("exists")).isTrue();
        verify(userService, times(1)).userExists(1L);
    }

    @Test
    @DisplayName("Should check if email exists")
    void testCheckEmailExists_WithExistingEmail_ShouldReturnTrue() {
        // Given
        com.autonova.auth_service.user.dto.UserLookupRequest request = 
            new com.autonova.auth_service.user.dto.UserLookupRequest();
        request.setEmail("customer@example.com");
        when(userService.emailExists("customer@example.com")).thenReturn(true);

        // When
        ResponseEntity<Map<String, Boolean>> response = userController.checkEmailExists(request);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().get("exists")).isTrue();
        verify(userService, times(1)).emailExists("customer@example.com");
    }
}
