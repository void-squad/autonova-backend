package com.autonova.auth_service.user.service;

import java.util.List;
import java.util.Optional;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.autonova.auth_service.user.Role;
import com.autonova.auth_service.user.model.User;
import com.autonova.auth_service.user.repository.UserRepository;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // Get all users
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    // Get user by ID
    public Optional<User> getUserById(Long id) {
        return userRepository.findById(id);
    }

    // Get user by email
    public Optional<User> getUserByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    // Create new user
    @Transactional
    public User createUser(User user) {
        // Validate required fields
        if (user.getUserName() == null || user.getUserName().trim().isEmpty()) {
            throw new IllegalArgumentException("Username is required");
        }
        if (user.getEmail() == null || user.getEmail().trim().isEmpty()) {
            throw new IllegalArgumentException("Email is required");
        }
        if (user.getContactOne() == null || user.getContactOne().trim().isEmpty()) {
            throw new IllegalArgumentException("Contact number is required");
        }
        if (user.getPassword() == null || user.getPassword().trim().isEmpty()) {
            throw new IllegalArgumentException("Password is required");
        }

        // Check if email already exists
        if (userRepository.existsByEmail(user.getEmail())) {
            throw new IllegalArgumentException("Email already exists: " + user.getEmail());
        }

        // Set default role if not provided
        if (user.getRole() == null) {
            user.setRole(Role.CUSTOMER);
        }

        // Hash the password before saving
        user.setPassword(passwordEncoder.encode(user.getPassword()));

        return userRepository.save(user);
    }

    // Update user
    @Transactional
    public User updateUser(Long id, User userDetails) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + id));

        // Update basic fields if provided
        if (userDetails.getUserName() != null && !userDetails.getUserName().trim().isEmpty()) {
            user.setUserName(userDetails.getUserName());
        }

        if (userDetails.getEmail() != null && !userDetails.getEmail().equals(user.getEmail())) {
            if (userRepository.existsByEmail(userDetails.getEmail())) {
                throw new IllegalArgumentException("Email already exists: " + userDetails.getEmail());
            }
            user.setEmail(userDetails.getEmail());
        }

        if (userDetails.getContactOne() != null && !userDetails.getContactOne().trim().isEmpty()) {
            user.setContactOne(userDetails.getContactOne());
        }

        // Note: Password changes are NOT allowed through this endpoint
        // Use the dedicated change-password endpoint instead

        // Update optional fields (address and contactTwo)
        if (userDetails.getAddress() != null) {
            user.setAddress(userDetails.getAddress());
        }

        if (userDetails.getContactTwo() != null) {
            user.setContactTwo(userDetails.getContactTwo());
        }

        user.setEnabled(userDetails.isEnabled());

        return userRepository.save(user);
    }

    // Delete user
    @Transactional
    public void deleteUser(Long id) {
        if (!userRepository.existsById(id)) {
            throw new IllegalArgumentException("User not found with id: " + id);
        }
        userRepository.deleteById(id);
    }

    // Update user role - ADMIN only operation
    @Transactional
    public User updateUserRole(Long id, Role newRole) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + id));

        // Validate that the new role is a persisted role
        if (newRole != Role.CUSTOMER && newRole != Role.EMPLOYEE && newRole != Role.ADMIN) {
            throw new IllegalArgumentException("Invalid role. Only CUSTOMER, EMPLOYEE, or ADMIN roles are allowed.");
        }

        user.setRole(newRole);
        return userRepository.save(user);
    }

    // Note: Password changes are handled through the forgot-password/reset-password flow
    // This ensures email verification for all password changes (more secure)

    // Check if user exists
    public boolean userExists(Long id) {
        return userRepository.existsById(id);
    }

    // Check if email exists
    public boolean emailExists(String email) {
        return userRepository.existsByEmail(email);
    }
}
