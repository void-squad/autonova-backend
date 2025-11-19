package com.autonova.auth_service.user.service;

import com.autonova.auth_service.email.EmailService;
import com.autonova.auth_service.security.UserSecurityService;
import com.autonova.auth_service.user.Role;
import com.autonova.auth_service.user.model.User;
import com.autonova.auth_service.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService Unit Tests")
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private UserSecurityService userSecurityService;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private UserService userService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail("test@example.com");
        testUser.setUserName("Test User");
        testUser.setFirstName("Test");
        testUser.setLastName("User");
        testUser.setContactOne("+1234567890");
        testUser.setPassword("hashedPassword");
        testUser.setRole(Role.CUSTOMER);
        testUser.setEnabled(true);
    }

    @Test
    @DisplayName("Should get all users")
    void getAllUsers_ShouldReturnAllUsers() {
        // Given
        List<User> users = Arrays.asList(testUser, new User());
        when(userRepository.findAll()).thenReturn(users);

        // When
        List<User> result = userService.getAllUsers();

        // Then
        assertThat(result).hasSize(2);
        verify(userRepository).findAll();
    }

    @Test
    @DisplayName("Should get user by ID when exists")
    void getUserById_WhenExists_ShouldReturnUser() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        // When
        Optional<User> result = userService.getUserById(1L);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getEmail()).isEqualTo("test@example.com");
        verify(userRepository).findById(1L);
    }

    @Test
    @DisplayName("Should return empty when user not found by ID")
    void getUserById_WhenNotExists_ShouldReturnEmpty() {
        // Given
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        // When
        Optional<User> result = userService.getUserById(999L);

        // Then
        assertThat(result).isEmpty();
        verify(userRepository).findById(999L);
    }

    @Test
    @DisplayName("Should get user by email when exists")
    void getUserByEmail_WhenExists_ShouldReturnUser() {
        // Given
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));

        // When
        Optional<User> result = userService.getUserByEmail("test@example.com");

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getUserName()).isEqualTo("Test User");
        verify(userRepository).findByEmail("test@example.com");
    }

    @Test
    @DisplayName("Should get current user when authenticated")
    void getCurrentUser_WhenAuthenticated_ShouldReturnUser() {
        // Given
        when(userSecurityService.getCurrentUserEmail()).thenReturn("test@example.com");
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));

        // When
        Optional<User> result = userService.getCurrentUser();

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getEmail()).isEqualTo("test@example.com");
        verify(userSecurityService).getCurrentUserEmail();
        verify(userRepository).findByEmail("test@example.com");
    }

    @Test
    @DisplayName("Should return empty when no current user")
    void getCurrentUser_WhenNotAuthenticated_ShouldReturnEmpty() {
        // Given
        when(userSecurityService.getCurrentUserEmail()).thenReturn(null);

        // When
        Optional<User> result = userService.getCurrentUser();

        // Then
        assertThat(result).isEmpty();
        verify(userSecurityService).getCurrentUserEmail();
        verify(userRepository, never()).findByEmail(anyString());
    }

    @Test
    @DisplayName("Should create user with valid data")
    void createUser_WithValidData_ShouldCreateUser() {
        // Given
        User newUser = new User();
        newUser.setEmail("new@example.com");
        newUser.setUserName("New User");
        newUser.setContactOne("+9876543210");
        newUser.setPassword("plainPassword");
        newUser.setFirstName("First");
        newUser.setLastName("Last");

        when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
        when(passwordEncoder.encode("plainPassword")).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(newUser);
        doNothing().when(emailService).sendWelcomeEmail(anyString(), anyString());

        // When
        User result = userService.createUser(newUser);

        // Then
        assertThat(result).isNotNull();
        verify(userRepository).existsByEmail("new@example.com");
        verify(passwordEncoder).encode("plainPassword");
        verify(userRepository).save(any(User.class));
        verify(emailService).sendWelcomeEmail("new@example.com", "New User");
    }

    @Test
    @DisplayName("Should set default role when not provided")
    void createUser_WithoutRole_ShouldSetDefaultRole() {
        // Given
        User newUser = new User();
        newUser.setEmail("new@example.com");
        newUser.setUserName("New User");
        newUser.setContactOne("+9876543210");
        newUser.setPassword("plainPassword");
        // No role set

        when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User savedUser = invocation.getArgument(0);
            assertThat(savedUser.getRole()).isEqualTo(Role.CUSTOMER);
            return savedUser;
        });
        doNothing().when(emailService).sendWelcomeEmail(anyString(), anyString());

        // When
        userService.createUser(newUser);

        // Then
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("Should throw exception when username is missing")
    void createUser_WithoutUsername_ShouldThrowException() {
        // Given
        User newUser = new User();
        newUser.setEmail("new@example.com");
        newUser.setContactOne("+9876543210");
        newUser.setPassword("plainPassword");
        // No username

        // When & Then
        assertThatThrownBy(() -> userService.createUser(newUser))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Username is required");

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Should throw exception when email is missing")
    void createUser_WithoutEmail_ShouldThrowException() {
        // Given
        User newUser = new User();
        newUser.setUserName("New User");
        newUser.setContactOne("+9876543210");
        newUser.setPassword("plainPassword");
        // No email

        // When & Then
        assertThatThrownBy(() -> userService.createUser(newUser))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Email is required");

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Should throw exception when contact is missing")
    void createUser_WithoutContact_ShouldThrowException() {
        // Given
        User newUser = new User();
        newUser.setUserName("New User");
        newUser.setEmail("new@example.com");
        newUser.setPassword("plainPassword");
        // No contact

        // When & Then
        assertThatThrownBy(() -> userService.createUser(newUser))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Contact number is required");

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Should throw exception when password is missing")
    void createUser_WithoutPassword_ShouldThrowException() {
        // Given
        User newUser = new User();
        newUser.setUserName("New User");
        newUser.setEmail("new@example.com");
        newUser.setContactOne("+9876543210");
        // No password

        // When & Then
        assertThatThrownBy(() -> userService.createUser(newUser))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Password is required");

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Should throw exception when email already exists")
    void createUser_WithDuplicateEmail_ShouldThrowException() {
        // Given
        User newUser = new User();
        newUser.setEmail("existing@example.com");
        newUser.setUserName("New User");
        newUser.setContactOne("+9876543210");
        newUser.setPassword("plainPassword");

        when(userRepository.existsByEmail("existing@example.com")).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> userService.createUser(newUser))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Email already exists");

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Should update user with valid data")
    void updateUser_WithValidData_ShouldUpdateUser() {
        // Given
        User updates = new User();
        updates.setUserName("Updated Name");
        updates.setContactOne("+1111111111");
        updates.setFirstName("UpdatedFirst");
        updates.setLastName("UpdatedLast");
        updates.setAddress("New Address");
        updates.setContactTwo("+2222222222");
        updates.setEnabled(true);

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        User result = userService.updateUser(1L, updates);

        // Then
        assertThat(result).isNotNull();
        verify(userRepository).findById(1L);
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("Should throw exception when updating non-existent user")
    void updateUser_WhenUserNotFound_ShouldThrowException() {
        // Given
        User updates = new User();
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> userService.updateUser(999L, updates))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("User not found");

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Should throw exception when updating email to existing one")
    void updateUser_WithDuplicateEmail_ShouldThrowException() {
        // Given
        User updates = new User();
        updates.setEmail("existing@example.com");

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.existsByEmail("existing@example.com")).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> userService.updateUser(1L, updates))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Email already exists");

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Should delete user when exists")
    void deleteUser_WhenExists_ShouldDeleteUser() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        doNothing().when(userRepository).delete(testUser);

        // When
        userService.deleteUser(1L);

        // Then
        verify(userRepository).findById(1L);
        verify(userRepository).delete(testUser);
    }

    @Test
    @DisplayName("Should throw exception when deleting non-existent user")
    void deleteUser_WhenNotExists_ShouldThrowException() {
        // Given
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> userService.deleteUser(999L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("User not found");

        verify(userRepository, never()).delete(any(User.class));
    }

    @Test
    @DisplayName("Should update user role")
    void updateUserRole_WithValidRole_ShouldUpdateRole() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        User result = userService.updateUserRole(1L, Role.EMPLOYEE);

        // Then
        assertThat(result).isNotNull();
        verify(userRepository).findById(1L);
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("Should throw exception when updating to invalid role")
    void updateUserRole_WithInvalidRole_ShouldThrowException() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        // When & Then
        assertThatThrownBy(() -> userService.updateUserRole(1L, Role.USER))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid role");

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Should check if user exists by ID")
    void userExists_WhenExists_ShouldReturnTrue() {
        // Given
        when(userRepository.existsById(1L)).thenReturn(true);

        // When
        boolean result = userService.userExists(1L);

        // Then
        assertThat(result).isTrue();
        verify(userRepository).existsById(1L);
    }

    @Test
    @DisplayName("Should check if user does not exist by ID")
    void userExists_WhenNotExists_ShouldReturnFalse() {
        // Given
        when(userRepository.existsById(999L)).thenReturn(false);

        // When
        boolean result = userService.userExists(999L);

        // Then
        assertThat(result).isFalse();
        verify(userRepository).existsById(999L);
    }

    @Test
    @DisplayName("Should check if email exists")
    void emailExists_WhenExists_ShouldReturnTrue() {
        // Given
        when(userRepository.existsByEmail("test@example.com")).thenReturn(true);

        // When
        boolean result = userService.emailExists("test@example.com");

        // Then
        assertThat(result).isTrue();
        verify(userRepository).existsByEmail("test@example.com");
    }

    @Test
    @DisplayName("Should check if email does not exist")
    void emailExists_WhenNotExists_ShouldReturnFalse() {
        // Given
        when(userRepository.existsByEmail("nonexistent@example.com")).thenReturn(false);

        // When
        boolean result = userService.emailExists("nonexistent@example.com");

        // Then
        assertThat(result).isFalse();
        verify(userRepository).existsByEmail("nonexistent@example.com");
    }
}
