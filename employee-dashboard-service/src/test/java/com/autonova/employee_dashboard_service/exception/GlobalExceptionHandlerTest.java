package com.autonova.employee_dashboard_service.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("GlobalExceptionHandler Unit Tests")
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler exceptionHandler = new GlobalExceptionHandler();

    @Test
    @DisplayName("Should handle ResourceNotFoundException and return 404")
    void shouldHandleResourceNotFoundException() {
        // Given
        ResourceNotFoundException exception = new ResourceNotFoundException("Resource not found");

        // When
        ResponseEntity<Map<String, Object>> response = exceptionHandler.handleResourceNotFoundException(exception);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertNotNull(response.getBody());
        assertThat(response.getBody().get("status")).isEqualTo(404);
        assertThat(response.getBody().get("message")).isEqualTo("Resource not found");
        assertThat(response.getBody().get("error")).isEqualTo("Not Found");
        assertThat(response.getBody()).containsKey("timestamp");
    }

    @Test
    @DisplayName("Should handle IllegalArgumentException and return 400")
    void shouldHandleIllegalArgumentException() {
        // Given
        IllegalArgumentException exception = new IllegalArgumentException("Invalid argument");

        // When
        ResponseEntity<Map<String, Object>> response = exceptionHandler.handleIllegalArgumentException(exception);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertNotNull(response.getBody());
        assertThat(response.getBody().get("status")).isEqualTo(400);
        assertThat(response.getBody().get("message")).isEqualTo("Invalid argument");
        assertThat(response.getBody().get("error")).isEqualTo("Bad Request");
    }

    @Test
    @DisplayName("Should handle WebClientResponseException and return appropriate status")
    void shouldHandleWebClientResponseException() {
        // Given
        WebClientResponseException exception = mock(WebClientResponseException.class);
        when(exception.getStatusCode()).thenReturn(HttpStatus.SERVICE_UNAVAILABLE);
        when(exception.getMessage()).thenReturn("Service unavailable");

        // When
        ResponseEntity<Map<String, Object>> response = exceptionHandler.handleWebClientResponseException(exception);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertNotNull(response.getBody());
        assertThat(response.getBody().get("status")).isEqualTo(503);
        assertThat(response.getBody().get("message")).asString()
                .contains("Error communicating with external service");
    }

    @Test
    @DisplayName("Should handle generic Exception and return 500")
    void shouldHandleGenericException() {
        // Given
        Exception exception = new Exception("Unexpected error");

        // When
        ResponseEntity<Map<String, Object>> response = exceptionHandler.handleGenericException(exception);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertNotNull(response.getBody());
        assertThat(response.getBody().get("status")).isEqualTo(500);
        assertThat(response.getBody().get("message")).isEqualTo("An unexpected error occurred");
        assertThat(response.getBody().get("error")).isEqualTo("Internal Server Error");
    }

    @Test
    @DisplayName("Should include timestamp in all error responses")
    void shouldIncludeTimestampInAllErrorResponses() {
        // Given
        Exception exception = new Exception("Test error");

        // When
        ResponseEntity<Map<String, Object>> response = exceptionHandler.handleGenericException(exception);

        // Then
        assertNotNull(response.getBody());
        assertThat(response.getBody()).containsKey("timestamp");
        assertThat(response.getBody().get("timestamp")).isNotNull();
    }

    @Test
    @DisplayName("Should handle multiple ResourceNotFoundException consistently")
    void shouldHandleMultipleResourceNotFoundExceptionsConsistently() {
        // Given
        ResourceNotFoundException exception1 = new ResourceNotFoundException("User not found");
        ResourceNotFoundException exception2 = new ResourceNotFoundException("Project not found");

        // When
        ResponseEntity<Map<String, Object>> response1 = exceptionHandler.handleResourceNotFoundException(exception1);
        ResponseEntity<Map<String, Object>> response2 = exceptionHandler.handleResourceNotFoundException(exception2);

        // Then
        assertThat(response1.getStatusCode()).isEqualTo(response2.getStatusCode());
        assertThat(response1.getBody().get("status")).isEqualTo(response2.getBody().get("status"));
        assertThat(response1.getBody().get("message")).isNotEqualTo(response2.getBody().get("message"));
    }

    @Test
    @DisplayName("Should preserve exception message in response")
    void shouldPreserveExceptionMessageInResponse() {
        // Given
        String customMessage = "Custom error message for testing";
        IllegalArgumentException exception = new IllegalArgumentException(customMessage);

        // When
        ResponseEntity<Map<String, Object>> response = exceptionHandler.handleIllegalArgumentException(exception);

        // Then
        assertThat(response.getBody().get("message")).isEqualTo(customMessage);
    }
}
