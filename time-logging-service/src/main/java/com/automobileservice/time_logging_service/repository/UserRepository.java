package com.automobileservice.time_logging_service.repository;

import com.automobileservice.time_logging_service.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, String> {
    
    // Find user by email
    Optional<User> findByEmail(String email);
    
    // Find users by role
    java.util.List<User> findByRole(String role);
}