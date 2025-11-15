package com.voidsquad.chatbot.mapper;

import com.voidsquad.chatbot.dto.ChatbotResponseDTO;
import org.springframework.stereotype.Component;

/**
 * Simple mapper that builds ChatbotResponseDTO from raw response string and token counts.
 */
@Component
public class ChatbotResponseMapper {

    public ChatbotResponseDTO toDto(String response, int availableTokens, int usedTokens) {
        return ChatbotResponseDTO.builder()
                .response(response)
                .tokens(ChatbotResponseDTO.Tokens.builder()
                        .available(availableTokens)
                        .used(usedTokens)
                        .build())
                .build();
    }
}
