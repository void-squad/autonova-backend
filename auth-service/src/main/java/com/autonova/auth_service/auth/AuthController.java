package com.autonova.auth_service.auth;

import com.autonova.auth_service.security.model.RefreshToken;
import com.autonova.auth_service.security.service.PasswordResetService;
import com.autonova.auth_service.security.service.RefreshTokenService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final PasswordResetService passwordResetService;
    private final RefreshTokenService refreshTokenService;

    public AuthController(
            AuthService authService, 
            PasswordResetService passwordResetService,
            RefreshTokenService refreshTokenService) {
        this.authService = authService;
        this.passwordResetService = passwordResetService;
        this.refreshTokenService = refreshTokenService;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        try {
            LoginResponse response = authService.login(request);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * POST /api/auth/forgot-password
     * Request password reset - generates token and sends email
     * Public endpoint - no authentication required
     * 
     * Security Note: Always returns success message even if email doesn't exist
     * to prevent user enumeration attacks (OWASP recommendation)
     */
    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody Map<String, String> request) {
        try {
            String email = request.get("email");
            
            if (email == null || email.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(createErrorResponse("Email address is required"));
            }
            
            // Validate email format
            if (!email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {
                return ResponseEntity.badRequest()
                    .body(createErrorResponse("Please enter a valid email address"));
            }
            
            String token = passwordResetService.generateResetToken(email);
            
            Map<String, Object> response = new HashMap<>();
            
            // Security Best Practice: Always show same success message
            // Don't reveal whether email exists or not (prevents user enumeration)
            if (token == null) {
                // Email doesn't exist in system
                response.put("success", true);
                response.put("message", "If an account exists with this email, you will receive password reset instructions shortly");
                response.put("info", "Please check your email inbox and spam folder");
            } else {
                // Email exists and token generated
                response.put("success", true);
                response.put("message", "Password reset link has been sent to your email");
                response.put("info", "Please check your inbox and follow the instructions to reset your password");
                response.put("token", token); // For development/testing only - remove in production
            }
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            // Log error but show generic message to user
            System.err.println("Error in forgot password: " + e.getMessage());
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Unable to process your request at this time");
            response.put("info", "Please try again later or contact support if the problem persists");
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * POST /api/auth/reset-password
     * Reset password using valid token
     * Public endpoint - no authentication required
     */
    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody Map<String, String> request) {
        try {
            String token = request.get("token");
            String newPassword = request.get("newPassword");
            
            if (token == null || token.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(createErrorResponse("Token is required"));
            }
            
            if (newPassword == null || newPassword.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(createErrorResponse("New password is required"));
            }
            
            passwordResetService.resetPassword(token, newPassword);
            
            Map<String, String> response = new HashMap<>();
            response.put("message", "Password has been reset successfully");
            
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                .body(createErrorResponse(e.getMessage()));
        }
    }

    /**
     * GET /api/auth/validate-reset-token
     * Check if a reset token is valid
     * Public endpoint - no authentication required
     */
    @GetMapping("/validate-reset-token")
    public ResponseEntity<?> validateResetToken(@RequestParam String token) {
        boolean isValid = passwordResetService.validateToken(token);
        
        Map<String, Object> response = new HashMap<>();
        response.put("valid", isValid);
        
        if (isValid) {
            response.put("message", "Token is valid");
        } else {
            response.put("message", "Token is invalid or expired");
        }
        
        return ResponseEntity.ok(response);
    }

    /**
     * POST /api/auth/refresh
     * Generate new access token using refresh token
     * Public endpoint - uses refresh token for authentication
     */
    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(@RequestBody Map<String, String> request) {
        try {
            String refreshToken = request.get("refreshToken");
            
            if (refreshToken == null || refreshToken.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(createErrorResponse("Refresh token is required"));
            }
            
            // Validate and get refresh token from database
            RefreshToken tokenEntity = refreshTokenService.findByToken(refreshToken);
            refreshTokenService.verifyExpiration(tokenEntity);
            
            // Generate new access token
            LoginResponse response = authService.refreshAccessToken(tokenEntity.getUser());
            
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                .body(createErrorResponse(e.getMessage()));
        }
    }

    /**
     * POST /api/auth/logout
     * Revoke refresh token (logout)
     * Public endpoint - requires refresh token
     */
    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestBody Map<String, String> request) {
        try {
            String refreshToken = request.get("refreshToken");
            
            if (refreshToken == null || refreshToken.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(createErrorResponse("Refresh token is required"));
            }
            
            refreshTokenService.revokeToken(refreshToken);
            
            Map<String, String> response = new HashMap<>();
            response.put("message", "Logged out successfully");
            
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                .body(createErrorResponse(e.getMessage()));
        }
    }

    // Helper method to create error response
    private Map<String, String> createErrorResponse(String message) {
        Map<String, String> error = new HashMap<>();
        error.put("error", message);
        return error;
    }
}
