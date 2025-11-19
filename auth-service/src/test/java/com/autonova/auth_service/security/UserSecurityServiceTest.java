package com.autonova.auth_service.security;

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
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Collection;
import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserSecurityService Unit Tests")
class UserSecurityServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private Authentication authentication;

    @Mock
    private SecurityContext securityContext;

    @InjectMocks
    private UserSecurityService userSecurityService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail("test@example.com");
        testUser.setRole(Role.CUSTOMER);
        
        SecurityContextHolder.setContext(securityContext);
    }

    @SuppressWarnings("unchecked")
    private Collection<GrantedAuthority> createAuthority(String role) {
        return (Collection<GrantedAuthority>) (Collection<?>) Collections.singletonList(new SimpleGrantedAuthority(role));
    }

    @Test
    @DisplayName("Should get current user ID when authenticated")
    void getCurrentUserId_WhenAuthenticated_ShouldReturnUserId() {
        // Given
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn("test@example.com");
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));

        // When
        Long userId = userSecurityService.getCurrentUserId();

        // Then
        assertThat(userId).isEqualTo(1L);
    }

    @Test
    @DisplayName("Should return null when not authenticated")
    void getCurrentUserId_WhenNotAuthenticated_ShouldReturnNull() {
        // Given
        when(securityContext.getAuthentication()).thenReturn(null);

        // When
        Long userId = userSecurityService.getCurrentUserId();

        // Then
        assertThat(userId).isNull();
    }

    @Test
    @DisplayName("Should return null when authentication is not authenticated")
    void getCurrentUserId_WhenAuthenticationNotAuthenticated_ShouldReturnNull() {
        // Given
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(false);

        // When
        Long userId = userSecurityService.getCurrentUserId();

        // Then
        assertThat(userId).isNull();
    }

    @Test
    @DisplayName("Should return null when user not found")
    void getCurrentUserId_WhenUserNotFound_ShouldReturnNull() {
        // Given
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn("test@example.com");
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.empty());

        // When
        Long userId = userSecurityService.getCurrentUserId();

        // Then
        assertThat(userId).isNull();
    }

    @Test
    @DisplayName("Should get current user email when authenticated")
    void getCurrentUserEmail_WhenAuthenticated_ShouldReturnEmail() {
        // Given
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn("test@example.com");

        // When
        String email = userSecurityService.getCurrentUserEmail();

        // Then
        assertThat(email).isEqualTo("test@example.com");
    }

    @Test
    @DisplayName("Should return null email when not authenticated")
    void getCurrentUserEmail_WhenNotAuthenticated_ShouldReturnNull() {
        // Given
        when(securityContext.getAuthentication()).thenReturn(null);

        // When
        String email = userSecurityService.getCurrentUserEmail();

        // Then
        assertThat(email).isNull();
    }

    @Test
    @DisplayName("Should get current user role as ADMIN")
    void getCurrentUserRole_WhenAdmin_ShouldReturnAdminRole() {
        // Given
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        doReturn(createAuthority("ROLE_ADMIN")).when(authentication).getAuthorities();

        // When
        Role role = userSecurityService.getCurrentUserRole();

        // Then
        assertThat(role).isEqualTo(Role.ADMIN);
    }

    @Test
    @DisplayName("Should get current user role as EMPLOYEE")
    void getCurrentUserRole_WhenEmployee_ShouldReturnEmployeeRole() {
        // Given
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        doReturn(createAuthority("ROLE_EMPLOYEE")).when(authentication).getAuthorities();

        // When
        Role role = userSecurityService.getCurrentUserRole();

        // Then
        assertThat(role).isEqualTo(Role.EMPLOYEE);
    }

    @Test
    @DisplayName("Should get current user role as CUSTOMER")
    void getCurrentUserRole_WhenCustomer_ShouldReturnCustomerRole() {
        // Given
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        doReturn(createAuthority("ROLE_CUSTOMER")).when(authentication).getAuthorities();

        // When
        Role role = userSecurityService.getCurrentUserRole();

        // Then
        assertThat(role).isEqualTo(Role.CUSTOMER);
    }

    @Test
    @DisplayName("Should get current user role as USER")
    void getCurrentUserRole_WhenUser_ShouldReturnUserRole() {
        // Given
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        doReturn(createAuthority("ROLE_USER")).when(authentication).getAuthorities();

        // When
        Role role = userSecurityService.getCurrentUserRole();

        // Then
        assertThat(role).isEqualTo(Role.USER);
    }

    @Test
    @DisplayName("Should return null role when not authenticated")
    void getCurrentUserRole_WhenNotAuthenticated_ShouldReturnNull() {
        // Given
        when(securityContext.getAuthentication()).thenReturn(null);

        // When
        Role role = userSecurityService.getCurrentUserRole();

        // Then
        assertThat(role).isNull();
    }

    @Test
    @DisplayName("Should verify owner when user IDs match")
    void isOwner_WhenUserIdsMatch_ShouldReturnTrue() {
        // Given
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn("test@example.com");
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));

        // When
        boolean isOwner = userSecurityService.isOwner(1L);

        // Then
        assertThat(isOwner).isTrue();
    }

    @Test
    @DisplayName("Should verify not owner when user IDs do not match")
    void isOwner_WhenUserIdsDoNotMatch_ShouldReturnFalse() {
        // Given
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn("test@example.com");
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));

        // When
        boolean isOwner = userSecurityService.isOwner(999L);

        // Then
        assertThat(isOwner).isFalse();
    }

    @Test
    @DisplayName("Should verify owner or admin when user is admin")
    void isOwnerOrAdmin_WhenAdmin_ShouldReturnTrue() {
        // Given
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        doReturn(createAuthority("ROLE_ADMIN")).when(authentication).getAuthorities();

        // When
        boolean result = userSecurityService.isOwnerOrAdmin(999L);

        // Then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("Should check if user has specific role")
    void hasRole_WhenUserHasRole_ShouldReturnTrue() {
        // Given
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        doReturn(createAuthority("ROLE_ADMIN")).when(authentication).getAuthorities();

        // When
        boolean hasRole = userSecurityService.hasRole(Role.ADMIN);

        // Then
        assertThat(hasRole).isTrue();
    }

    @Test
    @DisplayName("Should check if user does not have specific role")
    void hasRole_WhenUserDoesNotHaveRole_ShouldReturnFalse() {
        // Given
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        doReturn(createAuthority("ROLE_CUSTOMER")).when(authentication).getAuthorities();

        // When
        boolean hasRole = userSecurityService.hasRole(Role.ADMIN);

        // Then
        assertThat(hasRole).isFalse();
    }

    @Test
    @DisplayName("Should check if user has any of the specified roles")
    void hasAnyRole_WhenUserHasOneOfRoles_ShouldReturnTrue() {
        // Given
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        doReturn(createAuthority("ROLE_EMPLOYEE")).when(authentication).getAuthorities();

        // When
        boolean hasAnyRole = userSecurityService.hasAnyRole(Role.EMPLOYEE, Role.ADMIN);

        // Then
        assertThat(hasAnyRole).isTrue();
    }

    @Test
    @DisplayName("Should return false when user has none of the specified roles")
    void hasAnyRole_WhenUserHasNoneOfRoles_ShouldReturnFalse() {
        // Given
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        doReturn(createAuthority("ROLE_CUSTOMER")).when(authentication).getAuthorities();

        // When
        boolean hasAnyRole = userSecurityService.hasAnyRole(Role.EMPLOYEE, Role.ADMIN);

        // Then
        assertThat(hasAnyRole).isFalse();
    }

    @Test
    @DisplayName("Should verify admin can modify any user")
    void canModifyUser_WhenAdmin_ShouldReturnTrue() {
        // Given
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn("admin@example.com");
        doReturn(createAuthority("ROLE_ADMIN")).when(authentication).getAuthorities();
        
        User adminUser = new User();
        adminUser.setId(2L);
        adminUser.setEmail("admin@example.com");
        adminUser.setRole(Role.ADMIN);
        when(userRepository.findByEmail("admin@example.com")).thenReturn(Optional.of(adminUser));

        // When
        boolean canModify = userSecurityService.canModifyUser(999L);

        // Then
        assertThat(canModify).isTrue();
    }

    @Test
    @DisplayName("Should verify customer can modify own profile")
    void canModifyUser_WhenCustomerModifiesOwn_ShouldReturnTrue() {
        // Given
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn("test@example.com");
        doReturn(createAuthority("ROLE_CUSTOMER")).when(authentication).getAuthorities();
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));

        // When
        boolean canModify = userSecurityService.canModifyUser(1L);

        // Then
        assertThat(canModify).isTrue();
    }

    @Test
    @DisplayName("Should verify customer cannot modify other user")
    void canModifyUser_WhenCustomerModifiesOther_ShouldReturnFalse() {
        // Given
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn("test@example.com");
        doReturn(createAuthority("ROLE_CUSTOMER")).when(authentication).getAuthorities();
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));

        // When
        boolean canModify = userSecurityService.canModifyUser(999L);

        // Then
        assertThat(canModify).isFalse();
    }

    @Test
    @DisplayName("Should verify only admin can delete user")
    void canDeleteUser_WhenAdmin_ShouldReturnTrue() {
        // Given
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        doReturn(createAuthority("ROLE_ADMIN")).when(authentication).getAuthorities();

        // When
        boolean canDelete = userSecurityService.canDeleteUser(1L);

        // Then
        assertThat(canDelete).isTrue();
    }

    @Test
    @DisplayName("Should verify non-admin cannot delete user")
    void canDeleteUser_WhenNotAdmin_ShouldReturnFalse() {
        // Given
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        doReturn(createAuthority("ROLE_CUSTOMER")).when(authentication).getAuthorities();

        // When
        boolean canDelete = userSecurityService.canDeleteUser(1L);

        // Then
        assertThat(canDelete).isFalse();
    }

    @Test
    @DisplayName("Should verify admin can view any user")
    void canViewUser_WhenAdmin_ShouldReturnTrue() {
        // Given
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn("admin@example.com");
        doReturn(createAuthority("ROLE_ADMIN")).when(authentication).getAuthorities();
        
        User adminUser = new User();
        adminUser.setId(2L);
        adminUser.setEmail("admin@example.com");
        adminUser.setRole(Role.ADMIN);
        when(userRepository.findByEmail("admin@example.com")).thenReturn(Optional.of(adminUser));

        // When
        boolean canView = userSecurityService.canViewUser(999L);

        // Then
        assertThat(canView).isTrue();
    }

    @Test
    @DisplayName("Should verify user can view own profile")
    void canViewUser_WhenViewingOwn_ShouldReturnTrue() {
        // Given
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn("test@example.com");
        doReturn(createAuthority("ROLE_CUSTOMER")).when(authentication).getAuthorities();
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));

        // When
        boolean canView = userSecurityService.canViewUser(1L);

        // Then
        assertThat(canView).isTrue();
    }

    @Test
    @DisplayName("Should verify user cannot view other user")
    void canViewUser_WhenViewingOther_ShouldReturnFalse() {
        // Given
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn("test@example.com");
        doReturn(createAuthority("ROLE_CUSTOMER")).when(authentication).getAuthorities();
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));

        // When
        boolean canView = userSecurityService.canViewUser(999L);

        // Then
        assertThat(canView).isFalse();
    }

    @Test
    @DisplayName("Should verify admin can access admin dashboard")
    void canAccessAdminDashboard_WhenAdmin_ShouldReturnTrue() {
        // Given
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        doReturn(createAuthority("ROLE_ADMIN")).when(authentication).getAuthorities();

        // When
        boolean canAccess = userSecurityService.canAccessAdminDashboard();

        // Then
        assertThat(canAccess).isTrue();
    }

    @Test
    @DisplayName("Should verify non-admin cannot access admin dashboard")
    void canAccessAdminDashboard_WhenNotAdmin_ShouldReturnFalse() {
        // Given
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        doReturn(createAuthority("ROLE_CUSTOMER")).when(authentication).getAuthorities();

        // When
        boolean canAccess = userSecurityService.canAccessAdminDashboard();

        // Then
        assertThat(canAccess).isFalse();
    }
}
