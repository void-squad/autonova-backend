package com.voidsquad.chatbot.mapper;

import com.voidsquad.chatbot.dto.AuthInfoDTO;
import com.voidsquad.chatbot.service.auth.AuthInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class AuthInfoMapperTest {

    private AuthInfoMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new AuthInfoMapper();
    }

    @Test
    void toDto_ShouldMapAllFieldsCorrectly() {
        // Arrange
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", 123L);
        claims.put("email", "test@example.com");
        
        AuthInfo authInfo = AuthInfo.builder()
                .scheme("Bearer")
                .userId(123L)
                .email("test@example.com")
                .role("ADMIN")
                .firstName("John")
                .valid(true)
                .error(null)
                .claims(claims)
                .build();

        // Act
        AuthInfoDTO dto = mapper.toDto(authInfo);

        // Assert
        assertNotNull(dto);
        assertEquals("Bearer", dto.getScheme());
        assertEquals(123L, dto.getUserId());
        assertEquals("test@example.com", dto.getEmail());
        assertEquals("ADMIN", dto.getRole());
        assertEquals("John", dto.getFirstName());
        assertTrue(dto.isValid());
        assertNull(dto.getError());
    }

    @Test
    void toDto_ShouldHandleNullInput() {
        // Act
        AuthInfoDTO dto = mapper.toDto(null);

        // Assert
        assertNull(dto);
    }

    @Test
    void toDto_ShouldHandleInvalidAuthInfo() {
        // Arrange
        AuthInfo authInfo = AuthInfo.builder()
                .scheme("Bearer")
                .valid(false)
                .error("Invalid token")
                .build();

        // Act
        AuthInfoDTO dto = mapper.toDto(authInfo);

        // Assert
        assertNotNull(dto);
        assertEquals("Bearer", dto.getScheme());
        assertNull(dto.getUserId());
        assertNull(dto.getEmail());
        assertNull(dto.getRole());
        assertNull(dto.getFirstName());
        assertFalse(dto.isValid());
        assertEquals("Invalid token", dto.getError());
    }

    @Test
    void toDto_ShouldHandlePartialAuthInfo() {
        // Arrange
        AuthInfo authInfo = AuthInfo.builder()
                .scheme("Bearer")
                .userId(456L)
                .email(null)
                .role("USER")
                .firstName(null)
                .valid(true)
                .build();

        // Act
        AuthInfoDTO dto = mapper.toDto(authInfo);

        // Assert
        assertNotNull(dto);
        assertEquals("Bearer", dto.getScheme());
        assertEquals(456L, dto.getUserId());
        assertNull(dto.getEmail());
        assertEquals("USER", dto.getRole());
        assertNull(dto.getFirstName());
        assertTrue(dto.isValid());
    }

    @Test
    void toDto_ShouldHandleNullScheme() {
        // Arrange
        AuthInfo authInfo = AuthInfo.builder()
                .scheme(null)
                .userId(789L)
                .valid(true)
                .build();

        // Act
        AuthInfoDTO dto = mapper.toDto(authInfo);

        // Assert
        assertNotNull(dto);
        assertNull(dto.getScheme());
        assertEquals(789L, dto.getUserId());
        assertTrue(dto.isValid());
    }
}
