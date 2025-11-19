package com.autonova.customer.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.server.ResponseStatusException;

import java.sql.SQLException;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Customer Service - GlobalExceptionHandler Unit Tests")
class GlobalExceptionHandlerTest {

    @Mock
    private HttpServletRequest request;

    @InjectMocks
    private GlobalExceptionHandler globalExceptionHandler;

    @BeforeEach
    void setUp() {
        when(request.getRequestURI()).thenReturn("/api/customers");
    }

    @Test
    @DisplayName("Should handle validation exception")
    void handleValidation_WithValidationErrors_ShouldReturnBadRequest() {
        // Given
        FieldError fieldError1 = new FieldError("customer", "email", "must not be null");
        FieldError fieldError2 = new FieldError("customer", "name", "must not be blank");
        
        BindingResult bindingResult = mock(BindingResult.class);
        when(bindingResult.getFieldErrors()).thenReturn(List.of(fieldError1, fieldError2));
        
        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(null, bindingResult);

        // When
        ResponseEntity<ApiError> response = globalExceptionHandler.handleValidation(ex, request);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(400);
        assertThat(response.getBody().message()).isEqualTo("Validation failed");
        assertThat(response.getBody().fieldErrors()).hasSize(2);
        assertThat(response.getBody().path()).isEqualTo("/api/customers");
    }

    @Test
    @DisplayName("Should handle ResponseStatusException with NOT_FOUND")
    void handleResponseStatus_WithNotFound_ShouldReturnNotFound() {
        // Given
        ResponseStatusException ex = new ResponseStatusException(HttpStatus.NOT_FOUND, "Customer not found");

        // When
        ResponseEntity<ApiError> response = globalExceptionHandler.handleResponseStatus(ex, request);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(404);
        assertThat(response.getBody().message()).isEqualTo("Customer not found");
        assertThat(response.getBody().path()).isEqualTo("/api/customers");
    }

    @Test
    @DisplayName("Should handle ResponseStatusException with BAD_REQUEST")
    void handleResponseStatus_WithBadRequest_ShouldReturnBadRequest() {
        // Given
        ResponseStatusException ex = new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid input");

        // When
        ResponseEntity<ApiError> response = globalExceptionHandler.handleResponseStatus(ex, request);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(400);
        assertThat(response.getBody().message()).isEqualTo("Invalid input");
    }

    @Test
    @DisplayName("Should handle DataIntegrityViolationException with duplicate key")
    void handleDataIntegrity_WithDuplicateKey_ShouldReturnConflict() {
        // Given
        SQLException sqlException = new SQLException("Duplicate entry 'test@example.com' for key 'email'");
        DataIntegrityViolationException ex = new DataIntegrityViolationException("could not execute statement", sqlException);

        // When
        ResponseEntity<ApiError> response = globalExceptionHandler.handleDataIntegrity(ex, request);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(409);
        assertThat(response.getBody().message()).contains("Duplicate");
        assertThat(response.getBody().path()).isEqualTo("/api/customers");
    }

    @Test
    @DisplayName("Should handle DataIntegrityViolationException with foreign key constraint")
    void handleDataIntegrity_WithForeignKeyViolation_ShouldReturnConflict() {
        // Given
        SQLException sqlException = new SQLException("Cannot add or update a child row: a foreign key constraint fails");
        DataIntegrityViolationException ex = new DataIntegrityViolationException("constraint violation", sqlException);

        // When
        ResponseEntity<ApiError> response = globalExceptionHandler.handleDataIntegrity(ex, request);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(409);
        assertThat(response.getBody().message()).contains("Related record is missing");
    }

    @Test
    @DisplayName("Should handle DataIntegrityViolationException with generic message")
    void handleDataIntegrity_WithGenericViolation_ShouldReturnConflict() {
        // Given
        DataIntegrityViolationException ex = new DataIntegrityViolationException("Some constraint violation");

        // When
        ResponseEntity<ApiError> response = globalExceptionHandler.handleDataIntegrity(ex, request);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(409);
        assertThat(response.getBody().message()).contains("data integrity constraint");
    }

    @Test
    @DisplayName("Should handle generic Exception")
    void handleGeneric_WithAnyException_ShouldReturnInternalServerError() {
        // Given
        Exception ex = new RuntimeException("Unexpected error occurred");

        // When
        ResponseEntity<ApiError> response = globalExceptionHandler.handleGeneric(ex, request);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(500);
        assertThat(response.getBody().message()).isEqualTo("Unexpected error");
        assertThat(response.getBody().path()).isEqualTo("/api/customers");
    }

    @Test
    @DisplayName("Should handle NullPointerException")
    void handleGeneric_WithNullPointerException_ShouldReturnInternalServerError() {
        // Given
        NullPointerException ex = new NullPointerException("null value encountered");

        // When
        ResponseEntity<ApiError> response = globalExceptionHandler.handleGeneric(ex, request);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(500);
        assertThat(response.getBody().message()).isEqualTo("Unexpected error");
    }

    @Test
    @DisplayName("Should sanitize message when null in DataIntegrityViolationException")
    void handleDataIntegrity_WithNullMessage_ShouldReturnGenericMessage() {
        // Given
        DataIntegrityViolationException ex = new DataIntegrityViolationException("");

        // When
        ResponseEntity<ApiError> response = globalExceptionHandler.handleDataIntegrity(ex, request);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).isEqualTo("Request violates a data integrity constraint");
    }

    @Test
    @DisplayName("Should include timestamp in error response")
    void handleGeneric_ShouldIncludeTimestamp() {
        // Given
        Exception ex = new RuntimeException("Test error");

        // When
        ResponseEntity<ApiError> response = globalExceptionHandler.handleGeneric(ex, request);

        // Then
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().timestamp()).isNotNull();
    }

    @Test
    @DisplayName("Should handle validation with empty field errors")
    void handleValidation_WithEmptyFieldErrors_ShouldReturnEmptyList() {
        // Given
        BindingResult bindingResult = mock(BindingResult.class);
        when(bindingResult.getFieldErrors()).thenReturn(List.of());
        
        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(null, bindingResult);

        // When
        ResponseEntity<ApiError> response = globalExceptionHandler.handleValidation(ex, request);

        // Then
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().fieldErrors()).isEmpty();
    }

    @Test
    @DisplayName("Should handle ResponseStatusException with null reason")
    void handleResponseStatus_WithNullReason_ShouldUseStatusReasonPhrase() {
        // Given
        ResponseStatusException ex = new ResponseStatusException(HttpStatus.BAD_REQUEST, null);

        // When
        ResponseEntity<ApiError> response = globalExceptionHandler.handleResponseStatus(ex, request);

        // Then
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).isEqualTo(HttpStatus.BAD_REQUEST.getReasonPhrase());
    }
}
