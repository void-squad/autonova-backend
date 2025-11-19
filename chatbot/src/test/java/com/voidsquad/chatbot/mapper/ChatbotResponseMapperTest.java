package com.voidsquad.chatbot.mapper;

import com.voidsquad.chatbot.dto.ChatbotResponseDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ChatbotResponseMapperTest {

    private ChatbotResponseMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new ChatbotResponseMapper();
    }

    @Test
    void toDto_ShouldMapAllFieldsCorrectly() {
        // Arrange
        String response = "Hello, how can I help you?";
        int availableTokens = 1000;
        int usedTokens = 50;

        // Act
        ChatbotResponseDTO dto = mapper.toDto(response, availableTokens, usedTokens);

        // Assert
        assertNotNull(dto);
        assertEquals(response, dto.getResponse());
        assertNotNull(dto.getTokens());
        assertEquals(availableTokens, dto.getTokens().getAvailable());
        assertEquals(usedTokens, dto.getTokens().getUsed());
    }

    @Test
    void toDto_ShouldHandleNullResponse() {
        // Act
        ChatbotResponseDTO dto = mapper.toDto(null, 100, 10);

        // Assert
        assertNotNull(dto);
        assertNull(dto.getResponse());
        assertNotNull(dto.getTokens());
        assertEquals(100, dto.getTokens().getAvailable());
        assertEquals(10, dto.getTokens().getUsed());
    }

    @Test
    void toDto_ShouldHandleEmptyResponse() {
        // Act
        ChatbotResponseDTO dto = mapper.toDto("", 200, 0);

        // Assert
        assertNotNull(dto);
        assertEquals("", dto.getResponse());
        assertNotNull(dto.getTokens());
        assertEquals(200, dto.getTokens().getAvailable());
        assertEquals(0, dto.getTokens().getUsed());
    }

    @Test
    void toDto_ShouldHandleZeroTokens() {
        // Act
        ChatbotResponseDTO dto = mapper.toDto("Test response", 0, 0);

        // Assert
        assertNotNull(dto);
        assertEquals("Test response", dto.getResponse());
        assertNotNull(dto.getTokens());
        assertEquals(0, dto.getTokens().getAvailable());
        assertEquals(0, dto.getTokens().getUsed());
    }

    @Test
    void toDto_ShouldHandleNegativeTokens() {
        // Act
        ChatbotResponseDTO dto = mapper.toDto("Test", -1, -1);

        // Assert
        assertNotNull(dto);
        assertEquals("Test", dto.getResponse());
        assertNotNull(dto.getTokens());
        assertEquals(-1, dto.getTokens().getAvailable());
        assertEquals(-1, dto.getTokens().getUsed());
    }
}
